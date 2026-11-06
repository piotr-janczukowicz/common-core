package io.github.piotr_janczukowicz.testsupport.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Locale;
import org.junit.jupiter.api.Test;

@UnitTest
class ModelArchitectureRulesTest {

    private static final String PACKAGE = "io.github.piotr_janczukowicz.testsupport.architecture";

    @JsonSubTypes({@JsonSubTypes.Type(RegisteredUnion.A.class), @JsonSubTypes.Type(RegisteredUnion.B.class)})
    sealed interface RegisteredUnion {
        record A() implements RegisteredUnion {}

        record B() implements RegisteredUnion {}
    }

    @JsonSubTypes({@JsonSubTypes.Type(MissingUnion.A.class)})
    sealed interface MissingUnion {
        record A() implements MissingUnion {}

        record B() implements MissingUnion {}
    }

    @JsonSubTypes({@JsonSubTypes.Type(OpenUnion.A.class)})
    interface OpenUnion {
        record A() implements OpenUnion {}
    }

    enum PairedFallback {
        KNOWN,
        UNRECOGNIZED;

        public String wireValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    enum NoWireValue {
        KNOWN
    }

    enum RenamedFallback {
        KNOWN,
        UNRECOGNIZED;

        public String wireValue() {
            return "other";
        }
    }

    private static JavaClasses classes(final Class<?>... types) {
        return new ClassFileImporter().importClasses(types);
    }

    @Test
    void jsonUnionsRegisterTheirPermittedSubtypes_shouldPass_whenEveryPermittedSubtypeIsRegistered() {
        final JavaClasses union = classes(RegisteredUnion.class);

        final EvaluationResult result = ModelArchitectureRules.jsonUnionsRegisterTheirPermittedSubtypes(PACKAGE)
                .evaluate(union);

        assertThat(result.hasViolation()).isFalse();
    }

    @Test
    void jsonUnionsRegisterTheirPermittedSubtypes_shouldFail_whenAPermittedSubtypeIsNotRegistered() {
        final JavaClasses union = classes(MissingUnion.class);

        final EvaluationResult result = ModelArchitectureRules.jsonUnionsRegisterTheirPermittedSubtypes(PACKAGE)
                .evaluate(union);

        assertThat(result.getFailureReport().getDetails()).anyMatch(detail -> detail.contains("does not register"));
    }

    @Test
    void jsonUnionsRegisterTheirPermittedSubtypes_shouldFail_whenTheUnionIsNotSealed() {
        final JavaClasses union = classes(OpenUnion.class);

        final EvaluationResult result = ModelArchitectureRules.jsonUnionsRegisterTheirPermittedSubtypes(PACKAGE)
                .evaluate(union);

        assertThat(result.getFailureReport().getDetails()).anyMatch(detail -> detail.contains("is not sealed"));
    }

    @Test
    void enumFallbackPairsWithItsWireValue_shouldPass_whenFallbackUsesItsOwnName() {
        final JavaClasses fallback = classes(PairedFallback.class);

        final EvaluationResult result = ModelArchitectureRules.enumFallbackPairsWithItsWireValue(PACKAGE)
                .evaluate(fallback);

        assertThat(result.hasViolation()).isFalse();
    }

    @Test
    void enumFallbackPairsWithItsWireValue_shouldFail_whenFallbackHasAnotherWireValue() {
        final JavaClasses fallback = classes(RenamedFallback.class);

        final EvaluationResult result = ModelArchitectureRules.enumFallbackPairsWithItsWireValue(PACKAGE)
                .evaluate(fallback);

        assertThat(result.getFailureReport().getDetails())
                .anyMatch(detail -> detail.contains("UNRECOGNIZED must have the wire value unrecognized"));
    }

    @Test
    void productionHasNoTestClasses_shouldFail_whenAClassIsNamedLikeATest() {
        final JavaClasses tests = classes(ModelArchitectureRulesTest.class);

        final EvaluationResult result =
                ModelArchitectureRules.productionHasNoTestClasses().evaluate(tests);

        assertThat(result.hasViolation()).isTrue();
    }

    @Test
    void wireValue_shouldReturnTheConstantName_whenTheEnumHasNoWireValueMethod() {
        final Object constant = NoWireValue.KNOWN;

        final String wire = ModelArchitectureRules.wireValue(NoWireValue.class, constant);

        assertThat(wire).isEqualTo("KNOWN");
    }

    @Test
    void wireValue_shouldCallTheMethod_whenTheEnumHasOne() {
        final Object constant = PairedFallback.UNRECOGNIZED;

        final String wire = ModelArchitectureRules.wireValue(PairedFallback.class, constant);

        assertThat(wire).isEqualTo("unrecognized");
    }
}
