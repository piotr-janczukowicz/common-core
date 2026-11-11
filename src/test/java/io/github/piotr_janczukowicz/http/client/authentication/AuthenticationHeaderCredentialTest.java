package io.github.piotr_janczukowicz.http.client.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.http.Digests;
import io.github.piotr_janczukowicz.http.HttpHeaderNames;
import io.github.piotr_janczukowicz.http.client.authentication.Authentication.Type;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class AuthenticationHeaderCredentialTest {

    @Test
    void ofHeader_shouldHoldNameAndRawValue_whenGiven() {
        final Authentication authentication = Authentication.ofHeader("JOB-TOKEN", "glcbt-abc");

        assertSoftly(softly -> {
            softly.assertThat(authentication.type()).isEqualTo(Type.HEADER);
            softly.assertThat(authentication.headerName()).isEqualTo("JOB-TOKEN");
            softly.assertThat(authentication.value()).isEqualTo("glcbt-abc");
            softly.assertThat(authentication.headerValue()).isEqualTo("glcbt-abc");
        });
    }

    @Test
    void ofHeader_shouldDefaultIdentityToKeyedDigest_whenNoIdentityGiven() {
        final Authentication authentication = Authentication.ofHeader("JOB-TOKEN", "glcbt-abc");

        assertThat(authentication.identity()).isEqualTo(Digests.keyedSha256Hex("glcbt-abc"));
    }

    @Test
    void constructor_shouldRefuse_whenSchemeCredentialNamesAnotherHeader() {
        final Throwable thrown = catchThrowable(() -> new Authentication(Type.BEARER, "t", "id", "JOB-TOKEN"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("t\"");
    }

    @Test
    void constructor_shouldRefuse_whenHeaderCredentialIsGivenWithoutAHeaderName() {
        final Throwable thrown = catchThrowable(() -> new Authentication(Type.HEADER, "secret-value", "id"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("secret-value");
    }

    @Test
    void headerName_shouldBeAuthorization_whenBearerOrBasic() {
        assertSoftly(softly -> {
            softly.assertThat(Authentication.ofBearer("t").headerName()).isEqualTo(HttpHeaderNames.AUTHORIZATION);
            softly.assertThat(Authentication.ofBasic("u", "p").headerName()).isEqualTo(HttpHeaderNames.AUTHORIZATION);
        });
    }

    @Test
    void constructor_shouldDefaultToAuthorization_whenCalledWithThreeArguments() {
        final Authentication authentication = new Authentication(Type.BEARER, "t", "id");

        assertThat(authentication.headerName()).isEqualTo(HttpHeaderNames.AUTHORIZATION);
    }

    @Test
    void withIdentity_shouldKeepHeaderName_whenHeaderCredential() {
        final Authentication named =
                Authentication.ofHeader("JOB-TOKEN", "glcbt-abc").withIdentity("job:42");

        assertSoftly(softly -> {
            softly.assertThat(named.headerName()).isEqualTo("JOB-TOKEN");
            softly.assertThat(named.type()).isEqualTo(Type.HEADER);
            softly.assertThat(named.identity()).isEqualTo("job:42");
            softly.assertThat(named.headerValue()).isEqualTo("glcbt-abc");
        });
    }

    @Test
    void toString_shouldShowHeaderNameAndNeverValue_whenHeaderCredential() {
        final String text = Authentication.ofHeader("JOB-TOKEN", "glcbt-abc").toString();

        assertThat(text).isEqualTo("Authentication[JOB-TOKEN]").doesNotContain("glcbt-abc");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Authorization", "authorization", "Proxy-Authorization", "Cookie", "Set-Cookie"})
    void ofHeader_shouldRefuse_whenNameIsAnotherCredentialChannel(final String name) {
        final Throwable thrown = catchThrowable(() -> Authentication.ofHeader(name, "secret-value"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("headerName must not be " + name)
                .hasMessageNotContaining("secret-value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "JOB TOKEN", "JOB:TOKEN", "JOB-TOKEN\r\nX: y", "TÖKEN"})
    void ofHeader_shouldRefuse_whenNameIsNotAValidHeaderName(final String name) {
        final Throwable thrown = catchThrowable(() -> Authentication.ofHeader(name, "secret-value"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not an RFC 9110 token")
                .hasMessageNotContaining("secret-value");
    }

    @Test
    void ofHeader_shouldRefuse_whenValueBlankOrNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> Authentication.ofHeader("JOB-TOKEN", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> Authentication.ofHeader("JOB-TOKEN", null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> Authentication.ofHeader(null, "v")))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void ofHeader_shouldRefuse_whenValueHoldsLineBreak() {
        final Throwable thrown = catchThrowable(() -> Authentication.ofHeader("JOB-TOKEN", "abc\r\nX: y"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("abc");
    }

    @Test
    void parse_shouldStayAuthorizationOnly_whenValueIsRaw() {
        final Optional<Authentication> parsed = Authentication.parse("glcbt-abc");

        assertThat(parsed).isEmpty();
    }
}
