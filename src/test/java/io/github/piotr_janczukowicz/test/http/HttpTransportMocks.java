package io.github.piotr_janczukowicz.test.http;

import io.github.piotr_janczukowicz.http.HttpRequest;
import io.github.piotr_janczukowicz.http.transport.ConcurrencyLimitingHttpTransport;
import io.github.piotr_janczukowicz.http.transport.HttpTransport;
import java.io.IOException;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public final class HttpTransportMocks {

    private HttpTransportMocks() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static HttpTransport mock() {
        return mock(Answers.RETURNS_DEFAULTS);
    }

    public static HttpTransport mock(final Answer<?> answer) {
        return Mockito.mock(ConcurrencyLimitingHttpTransport.class, answer);
    }

    /**
     * The request that {@code transport} was asked to send, after a test used a client built on it. It also verifies
     * that exactly one request was sent.
     *
     * @param transport a mock made by {@link #mock()}
     * @return the one request that reached the transport
     * @throws IOException never in practice: {@code HttpTransport.execute} declares it
     * @throws org.mockito.exceptions.base.MockitoAssertionError if no request, or more than one, reached the transport
     */
    public static HttpRequest captureSingleRequest(final HttpTransport transport) throws IOException {
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(transport).execute(captor.capture());
        return captor.getValue();
    }
}
