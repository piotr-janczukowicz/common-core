package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@UnitTest
class JsonMappersTest {

    @Test
    void defaults_shouldUseSnakeCase_whenDecoding() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record User(String nodeId) {}

        final User user = mapper.readValue(FileUtils.readString(JsonMappersTest.class, "node-id.json"), User.class);

        assertThat(user.nodeId()).isEqualTo("abc");
    }

    @Test
    void defaults_shouldIgnoreUnknownProperties_whenDecoding() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record User(String login) {}

        final User user = mapper.readValue(FileUtils.readString(JsonMappersTest.class, "extra-field.json"), User.class);

        assertThat(user.login()).isEqualTo("octocat");
    }

    @Test
    void defaults_shouldRejectDuplicateKeys_whenDecoding() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Typed(String type) {}

        final Throwable thrown = catchThrowable(() -> mapper.readValue("{\"type\":\"a\",\"type\":\"b\"}", Typed.class));

        assertThat(thrown).hasMessageContaining("Duplicate");
    }

    @Test
    void defaults_shouldRejectFractionalNumber_whenTargetIsInteger() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Count(int total) {}

        final Throwable thrown = catchThrowable(() -> mapper.readValue("{\"total\": 1.9}", Count.class));

        assertThat(thrown).isInstanceOf(DatabindException.class);
    }

    @Test
    void defaults_shouldBindMissingPrimitiveToZero_whenPropertyAbsent() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Id(long id) {}

        final Id decoded = mapper.readValue("{}", Id.class);

        assertThat(decoded.id()).isZero();
    }

    @Test
    void defaults_shouldCoerceJsonNumberToString_whenTargetIsString() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Count(String total) {}

        final Count count = mapper.readValue("{\"total\": 42}", Count.class);

        assertThat(count.total()).isEqualTo("42");
    }

    @Test
    void defaults_shouldCoerceJsonBooleanToString_whenTargetIsString() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Flag(String enabled) {}

        final Flag flag = mapper.readValue("{\"enabled\": true}", Flag.class);

        assertThat(flag.enabled()).isEqualTo("true");
    }

    @Test
    void defaults_shouldCoerceJsonStringToStringList_whenTargetIsList() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Labels(List<String> labels) {}

        final Labels labels = mapper.readValue("{\"labels\":\"bug\"}", Labels.class);

        assertThat(labels.labels()).containsExactly("bug");
    }

    @Test
    void defaults_shouldRegisterOptionalNull_whenDecodingAndEncoding() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Patch(OptionalNull<String> displayName) {}

        final Patch decoded = mapper.readValue("{\"display_name\":null}", Patch.class);
        final String encoded = mapper.writeValueAsString(new Patch(OptionalNull.of("x")));

        assertSoftly(softly -> {
            softly.assertThat(decoded.displayName()).isEqualTo(OptionalNull.ofNull());
            softly.assertThat(encoded).isEqualTo("{\"display_name\":\"x\"}");
        });
    }

    @Test
    void defaults_shouldDecodeListAsImmutable_whenTargetIsList() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Names(List<String> names) {}
        final Names decoded = mapper.readValue("{\"names\":[\"a\",\"b\"]}", Names.class);

        final Throwable thrown = catchThrowable(() -> decoded.names().add("c"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaults_shouldDecodeSetAsImmutable_whenTargetIsSet() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Tags(Set<String> tags) {}
        final Tags decoded = mapper.readValue("{\"tags\":[\"a\",\"b\"]}", Tags.class);

        final Throwable thrown = catchThrowable(() -> decoded.tags().add("c"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaults_shouldDecodeMapAsImmutable_whenTargetIsMap() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Labels(Map<String, String> labels) {}
        final Labels decoded = mapper.readValue("{\"labels\":{\"a\":\"1\"}}", Labels.class);

        final Throwable thrown = catchThrowable(() -> decoded.labels().put("b", "2"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void defaults_shouldKeepNullAndNullElements_whenCollectionsAreNullable() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Names(@Nullable List<String> names, @Nullable List<String> missing) {}

        final Names decoded = mapper.readValue("{\"names\":[\"a\",null]}", Names.class);

        assertSoftly(softly -> {
            softly.assertThat(decoded.names()).containsExactly("a", null);
            softly.assertThat(decoded.missing()).isNull();
        });
    }

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws Exception {
        final Constructor<JsonMappers> constructor = JsonMappers.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    sealed interface Shape permits Circle {}

    record Circle(int radius) implements Shape {}

    @Test
    void defaults_shouldReadMissingExternalTypeIdValueAsNull_whenTheDiscriminatorIsPresentWithoutTheValue() {
        final JsonMapper mapper = JsonMappers.snakeCase().build();
        record Holder(
                @JsonTypeInfo(
                        use = JsonTypeInfo.Id.NAME,
                        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                        property = "kind")
                @JsonSubTypes(@JsonSubTypes.Type(value = Circle.class, name = "circle"))
                @Nullable Shape shape,

                String kind) {}

        final Holder withValue = mapper.readValue("{\"kind\":\"circle\",\"shape\":{\"radius\":2}}", Holder.class);
        final Holder withoutValue = mapper.readValue("{\"kind\":\"circle\"}", Holder.class);

        assertSoftly(softly -> {
            softly.assertThat(withValue.shape()).isEqualTo(new Circle(2));
            softly.assertThat(withoutValue.shape()).isNull();
            softly.assertThat(withoutValue.kind()).isEqualTo("circle");
        });
    }

    @Test
    void modulesOf_shouldReturnTheModuleOfEachProviderInOrder_whenProvidersAreGiven() {
        final SimpleModule first = new SimpleModule("first");
        final SimpleModule second = new SimpleModule("second");

        final List<JacksonModule> modules = JsonMappers.modulesOf(List.of(first, second), provider -> provider);

        assertThat(modules).containsExactly(first, second);
    }

    @Test
    void modulesOf_shouldReturnAnUnmodifiableList_whenProvidersAreGiven() {
        final List<JacksonModule> modules =
                JsonMappers.modulesOf(List.of(new SimpleModule("first")), provider -> provider);

        final Throwable thrown = catchThrowable(() -> modules.add(new SimpleModule("late")));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void modulesOf_shouldThrowNullPointerException_whenAnArgumentOrAModuleIsNull() {
        final List<SimpleModule> providers = List.of(new SimpleModule("only"));
        final List<SimpleModule> none = null;

        final Throwable noProviders = catchThrowable(() -> JsonMappers.modulesOf(none, provider -> provider));
        final Throwable noFunction = catchThrowable(() -> JsonMappers.modulesOf(providers, null));
        final Throwable noModule = catchThrowable(() -> JsonMappers.modulesOf(providers, provider -> null));

        assertSoftly(softly -> {
            softly.assertThat(noProviders)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("providers");
            softly.assertThat(noFunction)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("toModule");
            softly.assertThat(noModule).isInstanceOf(NullPointerException.class).hasMessageContaining("module");
        });
    }
}
