package io.github.piotr_janczukowicz.http.client.internal;

import static io.github.piotr_janczukowicz.http.HttpHeaderNames.ACCEPT;
import static io.github.piotr_janczukowicz.http.HttpHeaderNames.ETAG;
import static io.github.piotr_janczukowicz.http.HttpHeaderNames.IF_NONE_MATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.http.HttpBody;
import io.github.piotr_janczukowicz.http.HttpHeaders;
import io.github.piotr_janczukowicz.http.HttpMethod;
import io.github.piotr_janczukowicz.http.HttpRequest;
import io.github.piotr_janczukowicz.http.HttpResponse;
import io.github.piotr_janczukowicz.http.transport.ConditionalCacheConfiguration;
import io.github.piotr_janczukowicz.http.transport.HttpTransport;
import io.github.piotr_janczukowicz.http.transport.HttpTransportConfiguration;
import io.github.piotr_janczukowicz.http.transport.RateLimitDetector;
import io.github.piotr_janczukowicz.http.transport.RetryConfiguration;
import io.github.piotr_janczukowicz.http.transport.RetryingHttpTransport;
import io.github.piotr_janczukowicz.http.uri.ParsedUrl;
import io.github.piotr_janczukowicz.test.http.ScriptedTransport;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class ClientStackTest {

    private static final String URL = "https://api.example.test/user";
    private static final String ETAG_VALUE = "W/\"user-1\"";
    private static final String VERSION = "X-Version";

    @Test
    void assemble_shouldRevalidateTheCachedResponse_whenTheVaryingHeaderIsTheSame() throws Exception {
        final ScriptedTransport scripted = ScriptedTransport.replying(cacheable(), notModified());
        try (HttpTransport stack = stack(scripted, Duration.ZERO, ConditionalCacheConfiguration.defaults())) {
            stack.execute(get(URL, HttpHeaders.of(VERSION, "1")));
            stack.execute(get(URL, HttpHeaders.of(VERSION, "1")));
        }

        assertThat(scripted.request(1).headers().get(IF_NONE_MATCH)).contains(ETAG_VALUE);
    }

    @Test
    void assemble_shouldNotShareCachedResponses_whenAVaryingHeaderDiffers() throws Exception {
        final ScriptedTransport scripted = ScriptedTransport.replying(cacheable(), cacheable());
        try (HttpTransport stack = stack(scripted, Duration.ZERO, ConditionalCacheConfiguration.defaults())) {
            stack.execute(get(URL, HttpHeaders.of(VERSION, "1")));
            stack.execute(get(URL, HttpHeaders.of(VERSION, "2")));
        }

        assertThat(scripted.request(1).headers().get(IF_NONE_MATCH)).isEmpty();
    }

    @Test
    void assemble_shouldKeepAtMostTheConfiguredEntries_whenTheCacheIsSmall() throws Exception {
        final ScriptedTransport scripted = ScriptedTransport.replying(cacheable(), cacheable(), cacheable());
        final ConditionalCacheConfiguration cache =
                ConditionalCacheConfiguration.builder().maxEntries(1).build();
        try (HttpTransport stack = stack(scripted, Duration.ZERO, cache)) {
            stack.execute(get(URL + "?a", HttpHeaders.empty()));
            stack.execute(get(URL + "?b", HttpHeaders.empty()));
            stack.execute(get(URL + "?a", HttpHeaders.empty()));
        }

        assertThat(scripted.request(2).headers().get(IF_NONE_MATCH)).isEmpty();
    }

    @Test
    void assemble_shouldPaceRequestsThatChangeState_whenAMutationIntervalIsSet() throws Exception {
        final ScriptedTransport scripted =
                ScriptedTransport.replying(new HttpResponse(200, HttpHeaders.empty(), HttpBody.absent()));
        try (HttpTransport stack = stack(scripted, Duration.ofMillis(200), ConditionalCacheConfiguration.defaults())) {
            stack.execute(request(HttpMethod.POST));
            stack.execute(request(HttpMethod.POST));
        }

        final List<Long> starts = scripted.startNanos();
        assertThat(Duration.ofNanos(starts.get(1) - starts.get(0))).isGreaterThanOrEqualTo(Duration.ofMillis(150));
    }

    @Test
    void assemble_shouldPassTheRetrySettingsAndTheDetectorToTheRetryLayer_whenBuilt() {
        final AtomicReference<RetryConfiguration> seenRetry = new AtomicReference<>();
        final AtomicReference<RateLimitDetector> seenDetector = new AtomicReference<>();
        final RetryConfiguration retry =
                RetryConfiguration.builder().maxRateLimitAttempts(3).build();
        final RateLimitDetector detector = (response, clock) -> Optional.empty();
        final ScriptedTransport scripted = ScriptedTransport.replying();

        final HttpTransport stack = ClientStack.assemble(
                scripted.transport(),
                HttpTransportConfiguration.defaults(),
                retry,
                detector,
                Duration.ZERO,
                ConditionalCacheConfiguration.defaults(),
                Set.of(ACCEPT),
                (delegate, configuration, rateLimitDetector) -> {
                    seenRetry.set(configuration);
                    seenDetector.set(rateLimitDetector);
                    return delegate;
                });
        stack.close();

        assertSoftly(softly -> {
            softly.assertThat(seenRetry.get()).isSameAs(retry);
            softly.assertThat(seenDetector.get()).isSameAs(detector);
            softly.assertThat(scripted.isClosed()).isTrue();
        });
    }

    @Test
    void assemble_shouldCloseTheBaseAndRethrow_whenTheRetryLayerRefuses() {
        final ScriptedTransport scripted = ScriptedTransport.replying();

        final Throwable thrown =
                catchThrowable(() -> assembleWith(scripted, Set.of(ACCEPT), (delegate, configuration, detector) -> {
                    throw new IllegalStateException("refused");
                }));

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("refused");
            softly.assertThat(scripted.isClosed()).isTrue();
        });
    }

    @Test
    void assemble_shouldCloseTheBaseAndRethrow_whenAVaryingHeaderIsBlank() {
        final ScriptedTransport scripted = ScriptedTransport.replying();

        final Throwable thrown = catchThrowable(() -> assembleWith(scripted, Set.of(" "), RetryingHttpTransport::new));

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(scripted.isClosed()).isTrue();
        });
    }

    @Test
    void assemble_shouldCloseTheBaseAndRethrow_whenTheRetryLayerIsNull() {
        final ScriptedTransport scripted = ScriptedTransport.replying();

        final Throwable thrown = catchThrowable(() -> assembleWith(scripted, Set.of(ACCEPT), null));

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(NullPointerException.class);
            softly.assertThat(scripted.isClosed()).isTrue();
        });
    }

    @Test
    void assemble_shouldCloseTheBaseAndRethrow_whenTheTransportIsNull() {
        final ScriptedTransport scripted = ScriptedTransport.replying();

        final Throwable thrown = catchThrowable(() -> ClientStack.assemble(
                scripted.transport(),
                null,
                RetryConfiguration.defaults(),
                RateLimitDetector.defaults(),
                Duration.ZERO,
                ConditionalCacheConfiguration.defaults(),
                Set.of(ACCEPT),
                RetryingHttpTransport::new));

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(NullPointerException.class);
            softly.assertThat(scripted.isClosed()).isTrue();
        });
    }

    @Test
    void assemble_shouldThrowNullPointerException_whenTheBaseIsNull() {
        final Throwable thrown = catchThrowable(() -> ClientStack.assemble(
                null,
                HttpTransportConfiguration.defaults(),
                RetryConfiguration.defaults(),
                RateLimitDetector.defaults(),
                Duration.ZERO,
                ConditionalCacheConfiguration.defaults(),
                Set.of(ACCEPT),
                RetryingHttpTransport::new));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("base");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50, 100})
    void requireClientConcurrency_shouldAccept_whenTheLimitIsFromOneToOneHundred(final int limit) {
        final HttpTransportConfiguration transport = HttpTransportConfiguration.builder()
                .maxConcurrentRequests(limit)
                .build();

        final Throwable thrown = catchThrowable(() -> ClientStack.requireClientConcurrency(transport));

        assertThat(thrown).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {101, Integer.MAX_VALUE})
    void requireClientConcurrency_shouldThrow_whenTheLimitIsAboveOneHundred(final int limit) {
        final HttpTransportConfiguration transport = HttpTransportConfiguration.builder()
                .maxConcurrentRequests(limit)
                .build();

        final Throwable thrown = catchThrowable(() -> ClientStack.requireClientConcurrency(transport));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxConcurrentRequests");
    }

    @Test
    void requireClientConcurrency_shouldThrowNullPointerException_whenTransportIsNull() {
        final Throwable thrown = catchThrowable(() -> ClientStack.requireClientConcurrency(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("transport");
    }

    @Test
    void constructor_shouldRejectInstantiation_whenCalledViaReflection() throws Exception {
        final Constructor<ClientStack> constructor = ClientStack.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    private static HttpTransport stack(
            final ScriptedTransport scripted,
            final Duration mutationInterval,
            final ConditionalCacheConfiguration cache) {
        return ClientStack.assemble(
                scripted.transport(),
                HttpTransportConfiguration.defaults(),
                RetryConfiguration.defaults(),
                RateLimitDetector.defaults(),
                mutationInterval,
                cache,
                Set.of(ACCEPT, VERSION),
                RetryingHttpTransport::new);
    }

    private static HttpTransport assembleWith(
            final ScriptedTransport scripted,
            final Set<String> varyingHeaders,
            final ClientStack.RetryLayer retryLayer) {
        return ClientStack.assemble(
                scripted.transport(),
                HttpTransportConfiguration.defaults(),
                RetryConfiguration.defaults(),
                RateLimitDetector.defaults(),
                Duration.ZERO,
                ConditionalCacheConfiguration.defaults(),
                varyingHeaders,
                retryLayer);
    }

    private static HttpRequest get(final String url, final HttpHeaders headers) {
        return new HttpRequest(HttpMethod.GET, ParsedUrl.of(URI.create(url)), headers, HttpBody.absent());
    }

    private static HttpRequest request(final HttpMethod method) {
        return new HttpRequest(method, ParsedUrl.of(URI.create(URL)), HttpHeaders.empty(), HttpBody.absent());
    }

    private static HttpResponse cacheable() {
        return new HttpResponse(
                200, HttpHeaders.of(ETAG, ETAG_VALUE), HttpBody.of("body".getBytes(StandardCharsets.UTF_8)));
    }

    private static HttpResponse notModified() {
        return new HttpResponse(304, HttpHeaders.of(ETAG, ETAG_VALUE), HttpBody.absent());
    }
}
