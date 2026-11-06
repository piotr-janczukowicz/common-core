package io.github.piotr_janczukowicz.testsupport.architecture;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import java.util.Arrays;

/**
 * The classes of a module, imported once and shared by every architecture and audit test of it. {@code importPackages}
 * scans the module path on each call, so a test class that imports per rule pays for the scan again and again; this
 * holder imports lazily on the first {@link #all()} and hands out filtered views after that.
 */
public final class ImportedClasses {

    private final String rootPackage;
    private final ImportOption importOption;
    private JavaClasses all;

    /**
     * @param rootPackage the package whose classes (and sub-packages) are imported
     * @param importOption which class files to skip, such as {@code location -> !location.contains("Builder.class")}
     */
    public ImportedClasses(final String rootPackage, final ImportOption importOption) {
        this.rootPackage = requireNotBlank(rootPackage, "rootPackage");
        this.importOption = requireNonNull(importOption, "importOption");
    }

    /** Imports every class below {@code rootPackage}. */
    public ImportedClasses(final String rootPackage) {
        this(rootPackage, location -> true);
    }

    /** Every imported class: production, tests and the classes of test-support jars. */
    public synchronized JavaClasses all() {
        if (all == null) {
            all = new ClassFileImporter().withImportOption(importOption).importPackages(rootPackage);
        }
        return all;
    }

    /** The classes that are not test classes and reside in one of the packages (or below them). */
    public JavaClasses production(final String... packages) {
        return all().that(inPackages(packages).and(outsideTests()));
    }

    public static DescribedPredicate<JavaClass> inPackages(final String... packages) {
        return DescribedPredicate.describe(
                "reside in " + String.join(", ", packages),
                javaClass -> Arrays.stream(packages)
                        .anyMatch(prefix -> javaClass.getPackageName().equals(prefix)
                                || javaClass.getPackageName().startsWith(prefix + ".")));
    }

    public static DescribedPredicate<JavaClass> outsideTests() {
        return DescribedPredicate.describe(
                "do not come from a test location",
                javaClass -> javaClass
                        .getSource()
                        .map(source ->
                                ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.includes(Location.of(source.getUri())))
                        .orElse(true));
    }

    public static DescribedPredicate<JavaClass> outsideTestSupport() {
        return DescribedPredicate.describe(
                "do not come from common-test-support",
                javaClass -> javaClass
                        .getSource()
                        .map(source -> {
                            final Location location = Location.of(source.getUri());
                            return !location.contains("/common-test-support/") && !location.contains("-tests.jar");
                        })
                        .orElse(true));
    }
}
