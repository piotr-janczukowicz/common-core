package io.github.piotr_janczukowicz.http.client.authentication;

import java.util.Optional;

final class HeaderAuthenticationProvider implements AuthenticationProvider {

    private final Authentication authentication;

    HeaderAuthenticationProvider(final String headerName, final String token) {
        this.authentication = Authentication.ofHeader(headerName, token);
    }

    @Override
    public Optional<Authentication> authentication() {
        return Optional.of(authentication);
    }

    @Override
    public String credentialHeaderName() {
        return authentication.headerName();
    }

    @Override
    public String toString() {
        return "AuthenticationProviders.header(" + authentication.headerName() + ")";
    }
}
