package io.github.piotr_janczukowicz.http;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

/** Checks of the {@code User-Agent} header value a client sends. */
public final class HttpUserAgents {

    private static final String USER_AGENT_PARAMETER = "userAgent";
    private static final char FIRST_VISIBLE = ' ';
    private static final char LAST_VISIBLE = '~';

    private HttpUserAgents() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks that {@code userAgent} can be sent as a header value without surprises: not blank, and made of visible
     * ASCII characters and spaces only, so it cannot carry a line break that would split the header or a character that
     * servers read inconsistently.
     *
     * @param userAgent value for the {@code User-Agent} header
     * @return {@code userAgent}
     * @throws NullPointerException if {@code userAgent} is {@code null}
     * @throws IllegalArgumentException if it is blank or holds a control character or a character outside ASCII
     */
    public static String requireValid(final String userAgent) {
        requireNotBlank(userAgent, USER_AGENT_PARAMETER);
        for (int i = 0; i < userAgent.length(); i++) {
            final char c = userAgent.charAt(i);
            if (c < FIRST_VISIBLE || c > LAST_VISIBLE) {
                throw new IllegalArgumentException("userAgent must contain visible ASCII characters only");
            }
        }
        return userAgent;
    }
}
