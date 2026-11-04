package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireAtLeast;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stream helpers.
 *
 * <p>{@link #readAllBounded(InputStream, long)} reads a stream without letting it grow past a byte limit. A body of
 * known length is read straight into one buffer sized from the announced length, up to 8 MiB. A body of unknown length
 * is read into a list of chunks and copied once into the result. The result is always one {@code byte[]}, so the
 * effective limit never exceeds {@link #MAX_BUFFER_BYTES} (the largest array the JVM reliably allocates) even when a
 * larger limit is passed; a longer stream then fails with {@link BoundedReadLimitException} instead of an
 * {@link OutOfMemoryError}.
 *
 * <p>{@link #copyBounded(InputStream, OutputStream, long)} copies a stream to another one under the same kind of limit,
 * without buffering it.
 *
 * <p>{@link #closeSuppressing(AutoCloseable, Throwable)} closes a resource while keeping the failure that caused the
 * cleanup.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class IoUtils {

    /** Largest stream {@link #readAllBounded(InputStream, long)} buffers: {@code Integer.MAX_VALUE - 8} bytes. */
    public static final long MAX_BUFFER_BYTES = Integer.MAX_VALUE - 8L;

    private static final int BUFFER_SIZE = 8192;
    private static final int NO_SIZE_HINT = -1;
    // A stream of unknown length (typically gzip) is read into chunks and copied once into the result, instead of
    // doubling one buffer and trimming it at the end.
    private static final int FIRST_CHUNK_BYTES = 8192;
    private static final int NEXT_CHUNK_BYTES = 32 * 1024;
    // The announced length is a hint: trust it for the first allocation only up to this much, and let a stream that
    // really is larger grow the buffer, so a header alone cannot force a huge allocation.
    private static final long MAX_HINT_BYTES = 8L * 1024 * 1024;

    private IoUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads {@code stream} to its end, with no size hint.
     *
     * <p>See {@link #readAllBounded(InputStream, long, long)}; this is the same call with an unknown length.
     *
     * @param stream stream to drain; not closed
     * @param maxBytes most bytes accepted, zero or positive; capped at {@link #MAX_BUFFER_BYTES}
     * @return all bytes read
     * @throws NullPointerException if {@code stream} is {@code null}
     * @throws IllegalArgumentException if {@code maxBytes} is negative
     * @throws BoundedReadLimitException if more than the effective limit of bytes is available
     * @throws IOException if reading fails
     */
    public static byte[] readAllBounded(final InputStream stream, final long maxBytes) throws IOException {
        return readAllBounded(stream, maxBytes, NO_SIZE_HINT);
    }

    /**
     * Reads {@code stream} to its end, sizing the buffer from {@code expectedLength} so a stream of up to 8 MiB is read
     * without regrowing or copying.
     *
     * <p>The hint is only a hint: a longer stream still grows the buffer and a shorter one is trimmed. Without a
     * positive hint the stream is read into chunks and copied once into the result. The first allocation is capped at 8
     * MiB whatever the hint says, because an announced length proves nothing about what the sender will deliver; a
     * larger stream grows the buffer as it arrives. {@code stream} is not closed.
     *
     * @param stream stream to drain; not closed
     * @param maxBytes most bytes accepted, zero or positive; capped at {@link #MAX_BUFFER_BYTES}
     * @param expectedLength announced length, or a negative or zero value when unknown; bounded by the limit
     * @return all bytes read
     * @throws NullPointerException if {@code stream} is {@code null}
     * @throws IllegalArgumentException if {@code maxBytes} is negative
     * @throws BoundedReadLimitException if more than the effective limit of bytes is available
     * @throws IOException if reading fails
     */
    public static byte[] readAllBounded(final InputStream stream, final long maxBytes, final long expectedLength)
            throws IOException {
        requireNonNull(stream, "stream");
        requireAtLeast(maxBytes, 0L, "maxBytes");

        final long limit = Math.min(maxBytes, MAX_BUFFER_BYTES);
        return expectedLength > 0 ? readSized(stream, limit, expectedLength) : readChunked(stream, limit);
    }

    /**
     * Copies {@code source} to {@code target} until the end of {@code source}, without holding more than one buffer in
     * memory and without letting the copy pass a byte limit.
     *
     * <p>The check runs before each chunk is written, so at most {@code maxBytes} bytes reach {@code target}. When the
     * stream is longer, {@link BoundedReadLimitException} is thrown and {@code target} holds the bytes written so far.
     * Neither stream is closed or flushed.
     *
     * @param source stream to drain
     * @param target stream to write to
     * @param maxBytes most bytes accepted, zero or positive
     * @return number of bytes copied
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code maxBytes} is negative
     * @throws BoundedReadLimitException if {@code source} holds more than {@code maxBytes} bytes
     * @throws IOException if reading or writing fails
     */
    public static long copyBounded(final InputStream source, final OutputStream target, final long maxBytes)
            throws IOException {
        requireNonNull(source, "source");
        requireNonNull(target, "target");
        requireAtLeast(maxBytes, 0L, "maxBytes");

        final byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read = source.read(buffer);
        while (read >= 0) {
            total += read;
            if (total > maxBytes) {
                throw new BoundedReadLimitException(maxBytes);
            }
            target.write(buffer, 0, read);
            read = source.read(buffer);
        }
        return total;
    }

    /**
     * Closes {@code resource}. An exception thrown by {@link AutoCloseable#close()} is added to {@code failure} as a
     * suppressed exception instead of being thrown, so the original failure stays the one that propagates.
     *
     * <p>When {@code close()} succeeds, {@code failure} is left unchanged. When {@code close()} throws {@code failure}
     * itself, nothing is added, since a throwable cannot suppress itself. When {@code close()} is interrupted, the
     * interrupt flag of the current thread is restored. This method never throws because of the close; an {@link Error}
     * thrown by {@code close()} is not caught.
     *
     * @param resource resource to close; a {@code null} value is rejected, not ignored
     * @param failure failure that caused the cleanup; receives any close exception as suppressed
     * @throws NullPointerException if {@code resource} or {@code failure} is {@code null}
     */
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.DoNotUseThreads"}) // Restores the interrupt flag.
    public static void closeSuppressing(final AutoCloseable resource, final Throwable failure) {
        requireNonNull(resource, "resource");
        requireNonNull(failure, "failure");

        try {
            resource.close();
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            suppress(failure, interrupted);
        } catch (final Exception closeFailure) {
            suppress(failure, closeFailure);
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals") // A throwable cannot suppress itself; identity is the point.
    private static void suppress(final Throwable failure, final Exception closeFailure) {
        if (closeFailure != failure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static byte[] readChunked(final InputStream stream, final long limit) throws IOException {
        final List<byte[]> full = new ArrayList<>();
        byte[] chunk = new byte[0];
        int filled = 0;
        int total = 0;
        boolean isEnd = false;
        while (!isEnd) {
            final long remaining = limit - total;
            if (remaining == 0) {
                rejectExcess(stream, limit);
                isEnd = true;
            } else if (filled == chunk.length) {
                // Full chunk: a single probe byte tells "exactly full" (no new chunk) from "more data" (next chunk).
                final int next = stream.read();
                isEnd = next < 0;
                if (!isEnd) {
                    chunk = nextChunk(full, chunk, total, remaining);
                    chunk[0] = (byte) next;
                    filled = 1;
                    total++;
                }
            } else {
                final int read = stream.read(chunk, filled, (int) Math.min(chunk.length - (long) filled, remaining));
                isEnd = read < 0;
                if (!isEnd) {
                    filled += read;
                    total += read;
                }
            }
        }
        return join(full, chunk, filled, total);
    }

    // At the limit: one more byte is an overflow, the end of the stream is an exact fit.
    private static void rejectExcess(final InputStream stream, final long limit) throws IOException {
        if (stream.read() >= 0) {
            throw new BoundedReadLimitException(limit);
        }
    }

    private static byte[] join(final List<byte[]> full, final byte[] last, final int filled, final int total) {
        return full.isEmpty() ? Arrays.copyOf(last, filled) : concatenate(full, last, filled, total);
    }

    private static byte[] concatenate(final List<byte[]> full, final byte[] last, final int filled, final int total) {
        final byte[] result = new byte[total];
        int offset = 0;
        for (final byte[] part : full) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        System.arraycopy(last, 0, result, offset, filled);
        return result;
    }

    // Moves the full chunk to the list and starts the next one. Each new chunk is as large as everything read so far,
    // between FIRST_CHUNK_BYTES and NEXT_CHUNK_BYTES, so a body just past one chunk does not pay for a large, mostly
    // empty chunk. A chunk is never longer than what may still be read.
    private static byte[] nextChunk(
            final List<byte[]> full, final byte[] filledChunk, final int total, final long remaining) {
        if (filledChunk.length > 0) {
            full.add(filledChunk);
        }
        final int size = total == 0 ? FIRST_CHUNK_BYTES : Math.min(total, NEXT_CHUNK_BYTES);
        return new byte[(int) Math.min(size, remaining)];
    }

    private static byte[] readSized(final InputStream stream, final long limit, final long expectedLength)
            throws IOException {
        final long initial = Math.min(Math.min(expectedLength, MAX_HINT_BYTES), limit);
        byte[] buffer = new byte[(int) initial];
        int total = 0;
        boolean isEnd = false;
        while (!isEnd) {
            if (total == buffer.length) {
                // Full buffer: a single probe byte tells "exactly full" (no copy) from "more data" (grow or reject).
                final int next = stream.read();
                isEnd = next < 0;
                if (!isEnd) {
                    buffer = grow(buffer, total, limit);
                    buffer[total] = (byte) next;
                    total++;
                }
            } else {
                final int read = stream.read(buffer, total, buffer.length - total);
                isEnd = read < 0;
                if (!isEnd) {
                    total += read;
                }
            }
        }
        return total == buffer.length ? buffer : Arrays.copyOf(buffer, total);
    }

    // Math.clamp would throw for a limit below BUFFER_SIZE (its minimum would exceed its maximum); here the limit wins.
    @SuppressWarnings({"java:S6885", "MathClampMigration"})
    private static byte[] grow(final byte[] buffer, final int total, final long limit) throws IOException {
        if (total >= limit) {
            throw new BoundedReadLimitException(limit);
        }
        return Arrays.copyOf(buffer, (int) Math.min(limit, Math.max(total * 2L, BUFFER_SIZE)));
    }
}
