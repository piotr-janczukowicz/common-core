package io.github.piotr_janczukowicz.http.internal.inaccessible;

public final class InaccessibleTypes {

    private InaccessibleTypes() {}

    public static Class<?> packagePrivate() {
        return PackagePrivateType.class;
    }
}
