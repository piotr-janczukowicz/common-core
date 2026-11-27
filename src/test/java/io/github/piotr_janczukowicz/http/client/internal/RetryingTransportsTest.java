package io.github.piotr_janczukowicz.http.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
class RetryingTransportsTest {

    @Test
    void create_shouldPauseWithTheGivenSleeper_whenNoRandomSourceIsGiven() throws Exception {
        final AtomicInteger pauses = new AtomicInteger();
        final ScriptedTransport scripted = ScriptedTransport.replying(
                new HttpResponse(429, HttpHeaders.of("Retry-After", "1"), HttpBody.absent()),
                new HttpResponse(200, HttpHeaders.empty(), HttpBody.absent()));
        final HttpRequest request = new HttpRequest(
                HttpMethod.GET,
                ParsedUrl.of(URI.create("https://api.example.test/user")),
                HttpHeaders.empty(),
                HttpBody.absent());

        final HttpResponse response;
        try (RetryingHttpTransport transport = RetryingTransports.create(
                scripted.transport(),
                RetryConfiguration.defaults(),
                RateLimitDetector.defaults(),
                duration -> pauses.incrementAndGet(),
                Clock.systemUTC())) {
            response = transport.execute(request);
        }

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(pauses).hasValue(1);
    }

    @Test
    void constructor_shouldRejectInstantiation_whenCalledViaReflection() throws Exception {
        final Constructor<RetryingTransports> constructor = RetryingTransports.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
