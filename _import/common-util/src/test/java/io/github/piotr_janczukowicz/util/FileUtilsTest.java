package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class FileUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(FileUtils.class);
    }

    @Test
    void readString_shouldReturnContents_whenResourceExists() {
        final Class<?> type = FileUtilsTest.class;
        final String fileName = "sample.txt";

        final String text = FileUtils.readString(type, fileName);

        assertThat(text).isEqualTo("zażółć ✓");
    }

    @Test
    void readBytes_shouldReturnContents_whenResourceExists() {
        final Class<?> type = FileUtilsTest.class;
        final String fileName = "sample.txt";

        final byte[] bytes = FileUtils.readBytes(type, fileName);

        assertThat(bytes).isEqualTo("zażółć ✓".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readString_shouldThrow_whenTypeNull() {
        final Class<?> type = null;
        final String fileName = "sample.txt";

        final Throwable thrown = catchThrowable(() -> FileUtils.readString(type, fileName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("type must not be null");
    }

    @Test
    void readString_shouldThrow_whenFileNameBlank() {
        final Class<?> type = FileUtilsTest.class;
        final String fileName = " ";

        final Throwable thrown = catchThrowable(() -> FileUtils.readString(type, fileName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("fileName must not be blank");
    }

    @Test
    void readString_shouldNameResourceWithoutPackage_whenAbsoluteResourceMissing() {
        final Class<?> type = FileUtilsTest.class;
        final String fileName = "/missing.txt";

        final Throwable thrown = catchThrowable(() -> FileUtils.readString(type, fileName));

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing classpath resource /missing.txt");
    }

    @Test
    void readString_shouldThrow_whenResourceMissing() {
        final Class<?> type = FileUtilsTest.class;
        final String fileName = "missing.json";

        final Throwable thrown = catchThrowable(() -> FileUtils.readString(type, fileName));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("missing.json");
    }

    @Test
    void readBytes_shouldThrow_whenReadFails() throws IOException {
        final InputStream stream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read");
            }
        };
        final Class<?> type = typeWithResource(stream);
        final String fileName = "any.bin";

        final Throwable thrown = catchThrowable(() -> FileUtils.readBytes(type, fileName));

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }

    @Test
    void readBytes_shouldThrow_whenCloseFails() throws IOException {
        final InputStream stream = new ByteArrayInputStream(new byte[] {1}) {
            @Override
            public void close() throws IOException {
                throw new IOException("close");
            }
        };
        final Class<?> type = typeWithResource(stream);
        final String fileName = "any.bin";

        final Throwable thrown = catchThrowable(() -> FileUtils.readBytes(type, fileName));

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCauseInstanceOf(IOException.class);
    }

    private static Class<?> typeWithResource(final InputStream stream) throws IOException {
        final byte[] bytecode;
        try (InputStream in = FileUtils.class.getResourceAsStream("FileUtils.class")) {
            bytecode = Objects.requireNonNull(in).readAllBytes();
        }
        return new ResourceClassLoader(stream, bytecode).definedType();
    }

    private static final class ResourceClassLoader extends ClassLoader {

        private final InputStream stream;
        private final byte[] bytecode;

        private ResourceClassLoader(final InputStream stream, final byte[] bytecode) {
            super(FileUtils.class.getClassLoader());
            this.stream = stream;
            this.bytecode = bytecode;
        }

        private Class<?> definedType() {
            return defineClass(FileUtils.class.getName(), bytecode, 0, bytecode.length);
        }

        @Override
        public InputStream getResourceAsStream(final String name) {
            return stream;
        }
    }
}
