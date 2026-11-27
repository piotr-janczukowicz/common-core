package io.github.piotr_janczukowicz.http.client.authentication;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Bearer token from a supplier, read on each request, whose rejected token may be renewed after a 401 as the predicate
 * decides; {@code toString} does not include secrets.
 */
final class RenewableBearerAuthenticationProvider implements AuthenticationProvider {

    private final Supplier<String> tokens;
    private final BearerAuthenticationCache cache = new BearerAuthenticationCache();
    private final Predicate<String> canRenew;

    RenewableBearerAuthenticationProvider(final Supplier<String> tokens, final Predicate<String> canRenew) {
        this.tokens = requireNonNull(tokens, "tokens");
        this.canRenew = requireNonNull(canRenew, "canRenew");
    }

    @Override
    public Optional<Authentication> authentication() {
        return Optional.of(cache.forToken(tokens.get()));
    }

    @Override
    public boolean canRenew(final Authentication rejected) {
        return canRenew.test(rejected.value());
    }

    @Override
    public String toString() {
        return "AuthenticationProviders.bearer(supplier, predicate)";
    }
}
