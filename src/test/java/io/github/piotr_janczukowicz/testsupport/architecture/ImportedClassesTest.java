package io.github.piotr_janczukowicz.testsupport.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ImportedClassesTest {

    private static final String PACKAGE = "io.github.piotr_janczukowicz.testsupport.architecture";

    @Test
    void all_shouldReturnTheSameImport_whenCalledTwice() {
        final ImportedClasses imported = new ImportedClasses(PACKAGE);

        final JavaClasses first = imported.all();
        final JavaClasses second = imported.all();

        assertThat(second).isSameAs(first);
    }

    @Test
    void all_shouldSkipClassFiles_whenTheImportOptionExcludesThem() {
        final ImportedClasses imported = new ImportedClasses(PACKAGE, location -> !location.contains("Rules"));

        final JavaClasses all = imported.all();

        assertThat(all.contain(ModelArchitectureRules.class)).isFalse();
        assertThat(all.contain(ImportedClasses.class)).isTrue();
    }

    @Test
    void production_shouldKeepOnlyThePackages_whenPackagesAreGiven() {
        final ImportedClasses imported = new ImportedClasses("io.github.piotr_janczukowicz");

        final JavaClasses production = imported.production("io.github.piotr_janczukowicz.pagination");

        assertThat(production.stream().map(JavaClass::getPackageName).distinct())
                .containsOnly("io.github.piotr_janczukowicz.pagination");
    }

    @Test
    void inPackages_shouldNotMatchASiblingPackage_whenItOnlySharesThePrefix() {
        final ImportedClasses imported = new ImportedClasses(PACKAGE);

        final JavaClasses matching =
                imported.all().that(ImportedClasses.inPackages("io.github.piotr_janczukowicz.testsupport.arch"));

        assertThat(matching).isEmpty();
    }
}
