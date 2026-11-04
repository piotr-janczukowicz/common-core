package io.github.piotr_janczukowicz.openapi.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.RecordComponent;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogAnnotationsTest {

    @CatalogSchema({"components/schemas/simple-user", "operations/users/get/200"})
    record SampleCatalogRecord(
            @CatalogSimpleXor("string|integer") @SuppressSchemaTypeMismatch("documented as number")
            Object value) {}

    @Test
    void catalogSchema_shouldBeRuntimeAnnotationOnTypes_whenInspected() {
        final Retention retention = CatalogSchema.class.getAnnotation(Retention.class);
        final Target target = CatalogSchema.class.getAnnotation(Target.class);

        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(target.value()).containsExactly(ElementType.TYPE);
    }

    @Test
    void catalogSimpleXor_shouldBeRuntimeAnnotationOnComponentsFieldsAndMethods_whenInspected() {
        final Retention retention = CatalogSimpleXor.class.getAnnotation(Retention.class);
        final Target target = CatalogSimpleXor.class.getAnnotation(Target.class);

        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(target.value())
                .containsExactlyInAnyOrder(ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD);
    }

    @Test
    void suppressSchemaTypeMismatch_shouldBeRuntimeAnnotationOnComponentsFieldsAndMethods_whenInspected() {
        final Retention retention = SuppressSchemaTypeMismatch.class.getAnnotation(Retention.class);
        final Target target = SuppressSchemaTypeMismatch.class.getAnnotation(Target.class);

        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(target.value())
                .containsExactlyInAnyOrder(ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD);
    }

    @Test
    void annotations_shouldBeReadableByReflection_whenPlacedOnRecord() {
        final CatalogSchema schema = SampleCatalogRecord.class.getAnnotation(CatalogSchema.class);
        final RecordComponent component = SampleCatalogRecord.class.getRecordComponents()[0];

        assertThat(List.of(schema.value()))
                .containsExactly("components/schemas/simple-user", "operations/users/get/200");
        assertThat(component.getAnnotation(CatalogSimpleXor.class).value()).isEqualTo("string|integer");
        assertThat(component.getAnnotation(SuppressSchemaTypeMismatch.class).value())
                .isEqualTo("documented as number");
    }
}
