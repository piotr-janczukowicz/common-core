package io.github.piotr_janczukowicz.http.client.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.http.HttpHeaderNames;
import io.github.piotr_janczukowicz.http.client.authentication.Authentication.Type;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AuthenticationProvidersHeaderTest {

    @Test
    void credentialHeaderName_shouldBeAuthorization_whenProviderDoesNotOverrideIt() {
        assertSoftly(softly -> {
            softly.assertThat(AuthenticationProviders.bearer("t").credentialHeaderName())
                    .isEqualTo(HttpHeaderNames.AUTHORIZATION);
            softly.assertThat(AuthenticationProviders.basic("u", "p").credentialHeaderName())
                    .isEqualTo(HttpHeaderNames.AUTHORIZATION);
            softly.assertThat(AuthenticationProviders.none().credentialHeaderName())
                    .isEqualTo(HttpHeaderNames.AUTHORIZATION);
        });
    }

    @Test
    void header_shouldProvideRawCredentialUnderItsHeaderName_whenTokenGiven() {
        final AuthenticationProvider provider = AuthenticationProviders.header("JOB-TOKEN", "glcbt-abc");

        final Authentication authentication = provider.authentication().orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(provider.credentialHeaderName()).isEqualTo("JOB-TOKEN");
            softly.assertThat(authentication.type()).isEqualTo(Type.HEADER);
            softly.assertThat(authentication.headerName()).isEqualTo("JOB-TOKEN");
            softly.assertThat(authentication.headerValue()).isEqualTo("glcbt-abc");
        });
    }

    @Test
    void header_shouldNotRenew_whenRejected() {
        final AuthenticationProvider provider = AuthenticationProviders.header("JOB-TOKEN", "glcbt-abc");

        assertThat(provider.isRenewableAfterRejection(provider.authentication().orElseThrow()))
                .isFalse();
    }

    @Test
    void header_shouldRefuse_whenNameOrTokenInvalid() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> AuthenticationProviders.header("Authorization", "t")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> AuthenticationProviders.header("JOB-TOKEN", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void header_shouldNotShowTokenInToString_whenPrinted() {
        final AuthenticationProvider provider = AuthenticationProviders.header("JOB-TOKEN", "glcbt-abc");

        assertThat(provider.toString()).doesNotContain("glcbt-abc");
    }
}
