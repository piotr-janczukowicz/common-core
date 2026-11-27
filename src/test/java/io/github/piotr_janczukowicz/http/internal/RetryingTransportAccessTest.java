package io.github.piotr_janczukowicz.http.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.http.HttpBody;
import io.github.piotr_janczukowicz.http.HttpHeaders;
import io.github.piotr_janczukowicz.http.HttpMethod;
import io.github.piotr_janczukowicz.http.HttpRequest;
import io.github.piotr_janczukowicz.http.HttpResponse;
import io.github.piotr_janczukowicz.http.transport.RateLimitDetector;
import io.github.piotr_janczukowicz.http.transport.RetryConfiguration;
import io.github.piotr_janczukowicz.http.transport.RetryingHttpTransport;
import io.github.piotr_janczukowicz.http.uri.ParsedUrl;
import io.github.piotr_janczukowicz.test.http.ScriptedTransport;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

@UnitTest
class RetryingTransportAccessTest {

    @Test
    void create_shouldBuildATransportThatUsesTheGivenSleeper_whenTheResponseIsRateLimited() throws Exception {
        final AtomicInteger pauses = new AtomicInteger();
        final ScriptedTransport scripted = ScriptedTransport.replying(
                new HttpResponse(429, HttpHeaders.of("Retry-After", "1"), HttpBody.absent()),
                new HttpResponse(200, HttpHeaders.empty(), HttpBody.absent()));
        final HttpRequest request = new HttpRequest(
                HttpMethod.GET,
                ParsedUrl.of(URI.create("https://api.example.test/user")),
                HttpHeaders.empty(),
                HttpBody.absent());
        final RetryConfiguration configuration = RetryConfiguration.builder()
                .rateLimitJitter(java.time.Duration.ZERO)
                .build();

        final HttpResponse response;
        try (RetryingHttpTransport transport = RetryingTransportAccess.create(
                scripted.transport(),
                configuration,
                RateLimitDetector.defaults(),
                duration -> pauses.incrementAndGet(),
                Clock.systemUTC(),
                () -> 0L)) {
            response = transport.execute(request);
        }

        assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(200);
            softly.assertThat(pauses).hasValue(1);
        });
    }

    @Test
    void register_shouldThrowNullPointerException_whenFactoryIsNull() {
        final Throwable thrown = catchThrowable(() -> RetryingTransportAccess.register(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("factory");
    }

    @Test
    void load_shouldThrowIllegalStateException_whenTheClassDoesNotExist() {
        final Throwable thrown = catchThrowable(() -> RetryingTransportAccess.load("no.such.Type"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("no.such.Type");
    }

    @Test
    void constructor_shouldRejectInstantiation_whenCalledViaReflection() throws Exception {
        final Constructor<RetryingTransportAccess> constructor = RetryingTransportAccess.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
