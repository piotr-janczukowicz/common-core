package io.github.piotr_janczukowicz.http.client;

import static io.github.piotr_janczukowicz.http.HttpHeaderNames.AUTHORIZATION;
import static io.github.piotr_janczukowicz.http.HttpHeaderNames.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.http.HttpBody;
import io.github.piotr_janczukowicz.http.HttpHeaders;
import io.github.piotr_janczukowicz.http.HttpMethod;
import io.github.piotr_janczukowicz.http.HttpRequest;
import io.github.piotr_janczukowicz.http.HttpResponse;
import io.github.piotr_janczukowicz.http.HttpStatus;
import io.github.piotr_janczukowicz.http.client.authentication.Authentication;
import io.github.piotr_janczukowicz.http.client.authentication.AuthenticationProvider;
import io.github.piotr_janczukowicz.http.transport.HttpTransport;
import io.github.piotr_janczukowicz.test.http.HttpTransportMocks;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class HttpClientHeaderCredentialTest {

    private static final URI BASE = URI.create("https://gitlab.example.test/");
    private static final String JOB_TOKEN = "JOB-TOKEN";

    private HttpTransport transport;
    private final AtomicInteger asked = new AtomicInteger();
    private final AtomicInteger renewals = new AtomicInteger();

    @BeforeEach
    void setUpTransport() {
        transport = HttpTransportMocks.mock();
    }

    @Test
    void execute_shouldSendCredentialInItsOwnHeaderOnly_whenProviderUsesHeaderCredential() throws IOException {
        when(transport.execute(any())).thenReturn(ok());

        try (HttpClient client = client(provider(false), RedirectPolicy.defaults())) {
            client.execute(HttpCall.get("/api/v4/projects"));
        }

        final HttpRequest sent = sent();
        assertSoftly(softly -> {
            softly.assertThat(sent.headers().get(JOB_TOKEN)).contains("job-1");
            softly.assertThat(sent.headers().get(AUTHORIZATION)).isEmpty();
            softly.assertThat(sent.credentialId()).isEqualTo("job:1");
        });
    }

    @Test
    void execute_shouldNotAskForCredentials_whenCallSetsItsOwnCredentialHeader() throws IOException {
        when(transport.execute(any())).thenReturn(ok());
        final HttpCall call = HttpCall.get("/api/v4/projects").withHeader(JOB_TOKEN, "job-own");

        try (HttpClient client = client(provider(false), RedirectPolicy.defaults())) {
            client.execute(call);
        }

        final HttpRequest sent = sent();
        assertSoftly(softly -> {
            softly.assertThat(asked).hasValue(0);
            softly.assertThat(sent.headers().get(JOB_TOKEN)).contains("job-own");
            softly.assertThat(sent.credentialId()).isNotEqualTo("job:1");
        });
    }

    @Test
    void credentialKey_shouldDifferFromAnonymousAndFromClient_whenCallSetsItsOwnCredentialHeader() throws IOException {
        when(transport.execute(any())).thenReturn(ok());
        final HttpCall own = HttpCall.get("/api/v4/projects").withHeader(JOB_TOKEN, "job-own");

        try (HttpClient client = client(provider(false), RedirectPolicy.defaults())) {
            client.execute(own);
            client.execute(HttpCall.get("/api/v4/projects"));
        }

        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(transport, times(2)).execute(captor.capture());
        final String ownKey = captor.getAllValues().get(0).credentialKey();
        final String clientKey = captor.getAllValues().get(1).credentialKey();
        assertSoftly(softly -> {
            softly.assertThat(ownKey).isNotEqualTo("anonymous").isNotEqualTo(clientKey);
            softly.assertThat(ownKey).doesNotContain("job-own");
            softly.assertThat(clientKey).isEqualTo("id:job:1");
        });
    }

    @Test
    void execute_shouldSendOnlyTheCallsAuthorization_whenHeaderClientCallSetsAuthorization() throws IOException {
        when(transport.execute(any())).thenReturn(ok());
        final HttpCall call = HttpCall.get("/api/v4/projects").withHeader(AUTHORIZATION, "Bearer own");

        try (HttpClient client = client(provider(false), RedirectPolicy.defaults())) {
            client.execute(call);
        }

        final HttpRequest sent = sent();
        assertSoftly(softly -> {
            softly.assertThat(asked).hasValue(0);
            softly.assertThat(sent.headers().get(AUTHORIZATION)).contains("Bearer own");
            softly.assertThat(sent.headers().get(JOB_TOKEN)).isEmpty();
        });
    }

    @Test
    void execute_shouldRenewThroughTheCredentialHeader_whenProviderRenewsAfter401() throws IOException {
        when(transport.execute(any())).thenReturn(unauthorized()).thenReturn(ok());

        try (HttpClient client = client(provider(true), RedirectPolicy.defaults())) {
            final HttpResponse response = client.execute(HttpCall.get("/api/v4/projects"));

            assertThat(response.statusCode()).isEqualTo(200);
        }

        assertThat(renewals).hasValue(1);
        verify(transport, times(2)).execute(any());
    }

    @Test
    void execute_shouldNotRenew_whenProviderDoesNotRenewHeaderCredential() throws IOException {
        when(transport.execute(any())).thenReturn(unauthorized());

        final Throwable thrown;
        try (HttpClient client = client(provider(false), RedirectPolicy.defaults())) {
            thrown = catchThrowable(() -> client.execute(HttpCall.get("/api/v4/projects")));
        }

        assertThat(thrown).isInstanceOf(HttpException.class);
        assertThat(renewals).hasValue(0);
        verify(transport, times(1)).execute(any());
    }

    @Test
    void execute_shouldThrow_whenAuthenticationNamesAnotherHeaderThanTheProviderDeclares() {
        final AuthenticationProvider drifting = new AuthenticationProvider() {
            @Override
            public Optional<Authentication> authentication() {
                return Optional.of(Authentication.ofHeader("PRIVATE-TOKEN", "glpat-secret"));
            }

            @Override
            public String credentialHeaderName() {
                return JOB_TOKEN;
            }
        };

        final Throwable thrown;
        try (HttpClient client = client(drifting, RedirectPolicy.defaults())) {
            thrown = catchThrowable(() -> client.execute(HttpCall.get("/api/v4/projects")));
        }

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageNotContaining("glpat-secret");
    }

    @Test
    void build_shouldRefuse_whenRedirectPolicyAllowsTheCredentialHeaderAcrossOrigins() {
        final RedirectPolicy leaking = new RedirectPolicy(5, Set.of(HttpMethod.GET), Set.of("Accept", "job-token"));

        final Throwable thrown =
                catchThrowable(() -> client(provider(false), leaking).close());

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(JOB_TOKEN);
    }

    @Test
    void execute_shouldDropCredentialHeader_whenRedirectLeavesTheOrigin() throws IOException {
        when(transport.execute(any()))
                .thenReturn(new HttpResponse(
                        HttpStatus.FOUND.code(), HttpHeaders.of(LOCATION, "https://elsewhere.example.test/x"), null))
                .thenReturn(ok());

        try (HttpClient client = client(provider(false), RedirectPolicy.defaults())) {
            client.execute(HttpCall.get("/api/v4/projects/1/jobs/artifacts"));
        }

        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(transport, times(2)).execute(captor.capture());
        assertSoftly(softly -> {
            softly.assertThat(captor.getAllValues().get(0).headers().get(JOB_TOKEN))
                    .contains("job-1");
            softly.assertThat(captor.getAllValues().get(1).headers().get(JOB_TOKEN))
                    .isEmpty();
        });
    }

    private HttpRequest sent() throws IOException {
        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(transport).execute(captor.capture());
        return captor.getValue();
    }

    private AuthenticationProvider provider(final boolean isRenewable) {
        return new AuthenticationProvider() {
            @Override
            public Optional<Authentication> authentication() {
                asked.incrementAndGet();
                return Optional.of(Authentication.ofHeader(JOB_TOKEN, "job-1").withIdentity("job:1"));
            }

            @Override
            public String credentialHeaderName() {
                return JOB_TOKEN;
            }

            @Override
            public boolean isRenewableAfterRejection(final Authentication rejected) {
                if (isRenewable) {
                    renewals.incrementAndGet();
                }
                return isRenewable;
            }
        };
    }

    private HttpClient client(final AuthenticationProvider auth, final RedirectPolicy redirectPolicy) {
        return HttpClient.builder(
                        transport, HttpClientConfiguration.builder(BASE).build())
                .auth(auth)
                .redirectPolicy(redirectPolicy)
                .build();
    }

    private static HttpResponse unauthorized() {
        return new HttpResponse(HttpStatus.UNAUTHORIZED.code(), HttpHeaders.empty(), HttpBody.absent());
    }

    private static HttpResponse ok() {
        return new HttpResponse(HttpStatus.OK.code(), HttpHeaders.empty(), null);
    }
}
