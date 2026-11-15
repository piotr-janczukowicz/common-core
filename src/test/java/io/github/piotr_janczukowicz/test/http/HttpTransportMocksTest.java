package io.github.piotr_janczukowicz.test.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.http.HttpBody;
import io.github.piotr_janczukowicz.http.HttpHeaders;
import io.github.piotr_janczukowicz.http.HttpMethod;
import io.github.piotr_janczukowicz.http.HttpRequest;
import io.github.piotr_janczukowicz.http.transport.HttpTransport;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.exceptions.base.MockitoAssertionError;

@UnitTest
class HttpTransportMocksTest {

    @Test
    void captureSingleRequest_shouldReturnTheRequest_whenExactlyOneWasSent() throws IOException {
        final HttpTransport transport = HttpTransportMocks.mock();
        final HttpRequest request = new HttpRequest(
                HttpMethod.GET, URI.create("https://api.example.test/items"), HttpHeaders.empty(), HttpBody.absent());
        transport.execute(request);

        final HttpRequest captured = HttpTransportMocks.captureSingleRequest(transport);

        assertThat(captured).isSameAs(request);
    }

    @Test
    void captureSingleRequest_shouldFail_whenNoRequestWasSent() {
        final HttpTransport transport = HttpTransportMocks.mock();

        final Throwable thrown = catchThrowable(() -> HttpTransportMocks.captureSingleRequest(transport));

        assertThat(thrown).isInstanceOf(MockitoAssertionError.class);
    }

    @Test
    void captureSingleRequest_shouldFail_whenTwoRequestsWereSent() throws IOException {
        final HttpTransport transport = HttpTransportMocks.mock();
        transport.execute(new HttpRequest(
                HttpMethod.GET, URI.create("https://api.example.test/a"), HttpHeaders.empty(), HttpBody.absent()));
        transport.execute(new HttpRequest(
                HttpMethod.GET, URI.create("https://api.example.test/b"), HttpHeaders.empty(), HttpBody.absent()));

        final Throwable thrown = catchThrowable(() -> HttpTransportMocks.captureSingleRequest(transport));

        assertThat(thrown).isInstanceOf(AssertionError.class);
    }

    @Test
    void constructor_shouldThrow_whenCalledThroughReflection() throws Exception {
        final Constructor<HttpTransportMocks> constructor = HttpTransportMocks.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
