package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.json.JsonTestCodec;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.type.TypeReference;

class StringOrObjectAdaptersTest {

    private static final JsonCodec CODEC = JsonTestCodec.with(module -> {
        StringOrObjectAdapters.register(
                module, Label.class, Label::name, Label::details, Label::ofName, Label::ofDetails, LabelDetails.class);
        StringOrObjectAdapters.register(
                module,
                Assignee.class,
                Assignee::login,
                Assignee::details,
                Assignee::ofLogin,
                Assignee::ofDetails,
                AssigneeDetails.class);
        StringOrObjectAdapters.register(
                module,
                TypeValue.class,
                TypeValue::name,
                TypeValue::details,
                TypeValue::ofName,
                TypeValue::ofDetails,
                TypeValueDetails.class);
        StringOrObjectAdapters.register(
                module,
                Payload.class,
                Payload::text,
                Payload::object,
                Payload::ofText,
                Payload::ofObject,
                new TypeReference<>() {});
    });

    @ParameterizedTest
    @MethodSource("cases")
    @SuppressWarnings("unused")
    void decode_shouldReturnValue_whenJsonMatches(
            final Class<?> type, final Object expected, final String json, final Object wire) {
        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        final Object decoded = CODEC.decode(bytes, type);

        assertThat(decoded).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("cases")
    @SuppressWarnings("unused")
    void encode_shouldWriteWireForm_whenValueProvided(
            final Class<?> type, final Object value, final String json, final Object wire) {
        final byte[] encoded = CODEC.encode(value);

        final Object decoded = CODEC.decode(encoded, Object.class);

        assertThat(decoded).isEqualTo(wire);
    }

    @Test
    void encode_shouldFail_whenUnionHasNeitherScalarNorDetails() {
        final Label label = new Label(null, null);

        final Throwable thrown = catchThrowable(() -> CODEC.encode(label));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("details");
    }

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws Exception {
        final Constructor<StringOrObjectAdapters> constructor = StringOrObjectAdapters.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    private static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(Label.class, Label.ofName("bug"), "\"bug\"", "bug"),
                Arguments.of(
                        Label.class,
                        Label.ofDetails(new LabelDetails("bug", null)),
                        "{\"name\":\"bug\"}",
                        Map.of("name", "bug")),
                Arguments.of(Assignee.class, Assignee.ofLogin("octocat"), "\"octocat\"", "octocat"),
                Arguments.of(
                        Assignee.class,
                        Assignee.ofDetails(new AssigneeDetails("octocat", null)),
                        "{\"login\":\"octocat\"}",
                        Map.of("login", "octocat")),
                Arguments.of(TypeValue.class, TypeValue.ofName("Bug"), "\"Bug\"", "Bug"),
                Arguments.of(
                        TypeValue.class,
                        TypeValue.ofDetails(new TypeValueDetails(OptionalNull.of("Bug"), null)),
                        "{\"value\":\"Bug\"}",
                        Map.of("value", "Bug")),
                Arguments.of(Payload.class, Payload.ofText("deploy"), "\"deploy\"", "deploy"),
                Arguments.of(
                        Payload.class,
                        Payload.ofObject(Map.of("env", "prod")),
                        "{\"env\":\"prod\"}",
                        Map.of("env", "prod")));
    }

    record Label(@Nullable String name, @Nullable LabelDetails details) {

        static Label ofName(final String name) {
            return new Label(name, null);
        }

        static Label ofDetails(final LabelDetails details) {
            return new Label(null, details);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record LabelDetails(String name, @Nullable String color) {}

    record Assignee(@Nullable String login, @Nullable AssigneeDetails details) {

        static Assignee ofLogin(final String login) {
            return new Assignee(login, null);
        }

        static Assignee ofDetails(final AssigneeDetails details) {
            return new Assignee(null, details);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AssigneeDetails(String login, @Nullable String url) {}

    record TypeValue(@Nullable String name, @Nullable TypeValueDetails details) {

        static TypeValue ofName(final String name) {
            return new TypeValue(name, null);
        }

        static TypeValue ofDetails(final TypeValueDetails details) {
            return new TypeValue(null, details);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TypeValueDetails(
            @Nullable OptionalNull<String> value, @Nullable String description) {}

    record Payload(@Nullable String text, @Nullable Map<String, Object> object) {

        static Payload ofText(final String text) {
            return new Payload(text, null);
        }

        static Payload ofObject(final Map<String, Object> object) {
            return new Payload(null, object);
        }
    }
}
