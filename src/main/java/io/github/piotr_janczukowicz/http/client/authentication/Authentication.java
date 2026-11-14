package io.github.piotr_janczukowicz.http.client.authentication;

import static io.github.piotr_janczukowicz.http.HttpHeaderNames.AUTHORIZATION;
import static io.github.piotr_janczukowicz.http.HttpHeaderNames.COOKIE;
import static io.github.piotr_janczukowicz.http.HttpHeaderNames.PROXY_AUTHORIZATION;
import static io.github.piotr_janczukowicz.http.HttpHeaderNames.SET_COOKIE;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.http.Digests;
import io.github.piotr_janczukowicz.http.HttpHeaders;
import io.github.piotr_janczukowicz.util.EncodingUtils;
import io.github.piotr_janczukowicz.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Credentials of an HTTP request, sent as the {@code Authorization} header: a scheme and the credential that follows
 * it. The credential is the token of a {@code Bearer} header and the Base64 text of a {@code Basic} one. It is a
 * secret, so {@link #toString()} never shows it.
 *
 * <p>It also names the principal the credential belongs to, its {@code identity}. The identity is never a secret: the
 * rate-limit gate and the response cache key their state by it, so that a limit that was hit survives a rotation of the
 * token. Unless a provider names the principal with {@link #withIdentity(String)}, it is a keyed digest of the
 * credential ({@link Digests#keyedSha256Hex(String)}): the same credential has the same identity for as long as the
 * process lives, and a different one in the next process, so an identity that reaches a log or an event cannot be
 * matched against guessed credentials. Do not store it or compare it across processes.
 *
 * <p>A credential of type {@link Type#HEADER} is not sent as {@code Authorization}: it is the raw value of a header of
 * its own, such as GitLab's {@code JOB-TOKEN}. It has the same guarantees as the others: it is never shown by
 * {@link #toString()}, it names its principal with {@code identity}, and the client never sends it to another origin.
 *
 * @param type scheme of the header; {@code null} is refused with a {@link NullPointerException}
 * @param value the raw credential after the scheme (the whole header value for {@link Type#HEADER}), which is the
 *     secret itself: it must not be logged, printed or put in an exception message; {@link #identity()} is the loggable
 *     key of the principal. Blank is refused with an {@link IllegalArgumentException}
 * @param identity name of the principal, which must be unique per principal (two principals never share one), stable
 *     across credential rotation where the principal stays the same, and never a secret. The rate-limit gate and the
 *     conditional cache key their state by it, so a secret would leak into those keys, and a shared name would share
 *     limits and cached responses between principals. Blank is refused with an {@link IllegalArgumentException}
 * @param headerName name of the header that carries the credential: {@code Authorization} for {@link Type#BASIC} and
 *     {@link Type#BEARER}, and a header of its own, which is not another credential channel ({@code Authorization},
 *     {@code Proxy-Authorization}, {@code Cookie}, {@code Set-Cookie}), for {@link Type#HEADER}
 */
public record Authentication(Type type, String value, String identity, String headerName) {

    /** Scheme name of HTTP Basic authentication, RFC 7617. */
    public static final String BASIC = "Basic";

    /** Scheme name of bearer token authentication, RFC 6750. */
    public static final String BEARER = "Bearer";

    private static final String VALUE_PARAMETER = "value";

    private static final Set<String> CREDENTIAL_CHANNELS = Stream.of(
                    AUTHORIZATION, PROXY_AUTHORIZATION, COOKIE, SET_COOKIE)
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    /** Authentication scheme. */
    public enum Type {
        /** HTTP Basic authentication: Base64 of {@code user:password}. */
        BASIC(Authentication.BASIC),
        /** Bearer token. */
        BEARER(Authentication.BEARER),
        /** The raw value of a header of its own, with no scheme. */
        HEADER("");

        private final String scheme;

        Type(final String scheme) {
            this.scheme = scheme;
        }

        /**
         * Scheme name as it appears in the header.
         *
         * @return {@code Basic} or {@code Bearer}; empty for {@link #HEADER}, which has none
         */
        public String scheme() {
            return scheme;
        }
    }

    /**
     * Credentials sent as the {@code Authorization} header.
     *
     * @param type scheme of the header; {@link Type#HEADER} is refused, because its header is not {@code Authorization}
     * @param value the raw credential after the scheme
     * @param identity name of the principal
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code value} or {@code identity} is blank, or {@code type} is
     *     {@link Type#HEADER}
     */
    public Authentication(final Type type, final String value, final String identity) {
        this(type, value, identity, AUTHORIZATION);
    }

    public Authentication {
        requireNonNull(type, "type");
        requireNotBlank(value, VALUE_PARAMETER);
        requireNotBlank(identity, "identity");
        requireNotBlank(headerName, "headerName");
        if (type == Type.HEADER) {
            requireOwnHeaderName(headerName);
        } else if (!AUTHORIZATION.equalsIgnoreCase(headerName)) {
            throw new IllegalArgumentException("a " + type.scheme() + " credential is sent as " + AUTHORIZATION);
        }
    }

    /**
     * A credential that is the raw value of a header of its own, such as {@code JOB-TOKEN}. Its identity is a keyed
     * digest of the value, as for the other types; name the principal with {@link #withIdentity(String)} when the
     * credential rotates.
     *
     * @param headerName name of the header; it must be a valid header name and not another credential channel
     *     ({@code Authorization}, {@code Proxy-Authorization}, {@code Cookie}, {@code Set-Cookie})
     * @param value the raw header value, a secret
     * @return the credential
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code headerName} is not allowed, or {@code value} is blank or has a
     *     character that a header value cannot hold; the message never contains {@code value}
     */
    public static Authentication ofHeader(final String headerName, final String value) {
        requireNonNull(headerName, "headerName");
        requireNotBlank(value, VALUE_PARAMETER);
        requireOwnHeaderName(headerName);
        HttpHeaders.of(headerName, value);
        return new Authentication(Type.HEADER, value, Digests.keyedSha256Hex(value), headerName);
    }

    /**
     * Basic authentication for {@code user} and {@code password}: the credential is the Base64 encoding of
     * {@code user:password} as UTF-8.
     *
     * @param user username, or a client ID; it must not be blank or contain {@code :}
     * @param password password, or a client secret; it must not be blank
     * @return authentication of type {@link Type#BASIC}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code user} is blank or contains {@code :}, or {@code password} is blank;
     *     the message never contains the password
     */
    public static Authentication ofBasic(final String user, final String password) {
        requireNotBlank(user, "user");
        if (user.indexOf(':') >= 0) {
            throw new IllegalArgumentException("user must not contain ':'");
        }
        requireNotBlank(password, "password");
        final String encoded = EncodingUtils.encodeBase64((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        return of(Type.BASIC, encoded);
    }

    /**
     * Bearer authentication.
     *
     * @param token token, not including the {@code Bearer} prefix
     * @return authentication of type {@link Type#BEARER}
     * @throws NullPointerException if {@code token} is {@code null}
     * @throws IllegalArgumentException if {@code token} is blank
     */
    public static Authentication ofBearer(final String token) {
        return of(Type.BEARER, requireNotBlank(token, "token"));
    }

    /**
     * Reads the value of an {@code Authorization} header, as {@link #headerValue()} writes it.
     *
     * @param headerValue value of the header, such as {@code Bearer abc}
     * @return the authentication with the default identity, or empty if the scheme is neither {@code Basic} nor
     *     {@code Bearer}, or no credential follows it
     * @throws NullPointerException if {@code headerValue} is {@code null}
     */
    public static Optional<Authentication> parse(final String headerValue) {
        requireNonNull(headerValue, "headerValue");
        final int space = headerValue.indexOf(' ');
        if (space < 0) {
            return Optional.empty();
        }
        final String credential = StringUtils.stripToNull(headerValue.substring(space + 1));
        if (credential == null) {
            return Optional.empty();
        }
        final String scheme = headerValue.substring(0, space).toLowerCase(Locale.ROOT);
        if (scheme.equals(BASIC.toLowerCase(Locale.ROOT))) {
            return Optional.of(of(Type.BASIC, credential));
        }
        if (scheme.equals(BEARER.toLowerCase(Locale.ROOT))) {
            return Optional.of(of(Type.BEARER, credential));
        }
        return Optional.empty();
    }

    /**
     * The same credential, attributed to the principal {@code identity}, for a provider whose credential rotates while
     * the principal stays the same, such as an installation token. The identity must be unique per principal, stable
     * across the rotation, and never a secret: the rate-limit gate and the conditional cache key their state by it.
     *
     * @param identity name of the principal, never the credential itself
     * @return authentication with that identity
     * @throws NullPointerException if {@code identity} is {@code null}
     * @throws IllegalArgumentException if {@code identity} is blank
     */
    public Authentication withIdentity(final String identity) {
        return new Authentication(type, value, identity, headerName);
    }

    /**
     * Value of the header: the scheme, a space and the credential; for {@link Type#HEADER} the raw value.
     *
     * @return for example {@code Bearer abc}, or {@code abc} for a header credential; it holds the secret, so keep it
     *     out of logs
     */
    public String headerValue() {
        return type == Type.HEADER ? value : type.scheme() + " " + value;
    }

    /**
     * Shows the scheme, or the header name of a header credential, never the value.
     *
     * @return {@code Authentication[Bearer]}, {@code Authentication[Basic]} or, for example,
     *     {@code Authentication[JOB-TOKEN]}
     */
    @Override
    public String toString() {
        return "Authentication[" + (type == Type.HEADER ? headerName : type.scheme()) + "]";
    }

    private static void requireOwnHeaderName(final String headerName) {
        if (CREDENTIAL_CHANNELS.contains(headerName.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("headerName must not be " + headerName);
        }
    }

    private static Authentication of(final Type type, final String value) {
        return new Authentication(type, value, Digests.keyedSha256Hex(requireNotBlank(value, VALUE_PARAMETER)));
    }
}
