package io.github.piotr_janczukowicz.http.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.http.uri.ParsedUrl;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import okhttp3.HttpUrl;

/**
 * Gives the OkHttp transport the OkHttp form of a {@link ParsedUrl} without making it part of the public API.
 *
 * <p>This package is not exported from the module.
 */
public final class ParsedUrlAccess {

    private static final AtomicReference<Function<ParsedUrl, HttpUrl>> READER = new AtomicReference<>();

    private ParsedUrlAccess() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers the function that reads the OkHttp form of a {@link ParsedUrl}; called once by {@link ParsedUrl}.
     *
     * @param function reads the OkHttp form
     * @throws NullPointerException if {@code function} is {@code null}
     */
    public static void register(final Function<ParsedUrl, HttpUrl> function) {
        READER.set(requireNonNull(function, "function"));
    }

    /**
     * The OkHttp form of {@code url}.
     *
     * @param url parsed URL
     * @return parsed OkHttp URL
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public static HttpUrl okHttpUrl(final ParsedUrl url) {
        return READER.get().apply(requireNonNull(url, "url"));
    }
}
