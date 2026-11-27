package io.github.piotr_janczukowicz.http.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.http.HttpBody;
import io.github.piotr_janczukowicz.http.HttpHeaders;
import io.github.piotr_janczukowicz.http.HttpMethod;
import io.github.piotr_janczukowicz.http.HttpRequest;
import io.github.piotr_janczukowicz.http.HttpResponse;
import io.github.piotr_janczukowicz.http.internal.inaccessible.InaccessibleTypes;
import io.github.piotr_janczukowicz.http.transport.RateLimitDetector;
import io.github.piotr_janczukowicz.http.transport.RetryConfiguration;
import io.github.piotr_janczukowicz.http.transport.RetryingHttpTransport;
import io.github.piotr_janczukowicz.http.uri.ParsedUrl;
import io.github.piotr_janczukowicz.test.http.ScriptedTransport;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Clock;
import java.util.Arrays;
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
    void create_shouldRegisterTheFactoryItself_whenTheTransportClassIsNotYetInitialized() throws Exception {
        try (IsolatedClassLoader loader = new IsolatedClassLoader()) {
            final Class<?> access = loader.loadClass(RetryingTransportAccess.class.getName());
            final Method create = Arrays.stream(access.getMethods())
                    .filter(method -> "create".equals(method.getName()))
                    .findFirst()
                    .orElseThrow();

            final Throwable thrown = catchThrowable(() -> create.invoke(null, new Object[create.getParameterCount()]));

            // the transport constructor rejects the null arguments, so the call reached a registered factory
            assertThat(thrown)
                    .cause()
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("configuration must not be null");
        }
    }

    @Test
    void initialize_shouldNotThrow_whenTheClassIsAccessible() {
        final Throwable thrown = catchThrowable(() -> RetryingTransportAccess.initialize(RetryingHttpTransport.class));

        assertThat(thrown).isNull();
    }

    @Test
    void register_shouldThrowNullPointerException_whenFactoryIsNull() {
        final Throwable thrown = catchThrowable(() -> RetryingTransportAccess.register(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("factory");
    }

    @Test
    void initialize_shouldThrowIllegalStateException_whenTheClassIsNotAccessible() {
        final Class<?> hidden = InaccessibleTypes.packagePrivate();

        final Throwable thrown = catchThrowable(() -> RetryingTransportAccess.initialize(hidden));

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(hidden.getName())
                .hasCauseInstanceOf(IllegalAccessException.class);
    }

    @Test
    void constructor_shouldRejectInstantiation_whenCalledViaReflection() throws Exception {
        final Constructor<RetryingTransportAccess> constructor = RetryingTransportAccess.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    /** Defines this library's classes anew, so that their static state starts empty. */
    private static final class IsolatedClassLoader extends URLClassLoader {

        private static final String LIBRARY_PACKAGE = "io.github.piotr_janczukowicz.";

        private IsolatedClassLoader() {
            super(
                    new URL[] {
                        RetryingTransportAccess.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                    },
                    RetryingTransportAccessTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(LIBRARY_PACKAGE)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                final Class<?> loaded = findLoadedClass(name);
                return loaded != null ? loaded : findClass(name);
            }
        }
    }
}
