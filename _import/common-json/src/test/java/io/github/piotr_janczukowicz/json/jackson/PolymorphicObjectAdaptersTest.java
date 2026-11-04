package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

class PolymorphicObjectAdaptersTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws Exception {
        final Constructor<PolymorphicObjectAdapters> constructor =
                PolymorphicObjectAdapters.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deserialize_shouldPickCircle_whenRadiusPresent() {
        final JsonMapper mapper = mapper();

        final Shape circle = mapper.readValue("{\"radius\":2}", Shape.class);

        assertThat(circle).isEqualTo(new Circle(new CircleBody(2)));
    }

    @Test
    void deserialize_shouldPickSquare_whenSidePresent() {
        final JsonMapper mapper = mapper();

        final Shape square = mapper.readValue("{\"side\":3}", Shape.class);

        assertThat(square).isEqualTo(new Square(new SquareBody(3)));
    }

    @Test
    void deserialize_shouldPickFirstBranch_whenSeveralFieldsArePresent() {
        final JsonMapper mapper = mapper();

        final Shape shape = mapper.readValue("{\"side\":3,\"radius\":2}", Shape.class);

        assertThat(shape).isInstanceOf(Circle.class);
    }

    @Test
    void deserialize_shouldThrowWithTheGivenMessage_whenNoBranchFieldIsPresent() {
        final JsonMapper mapper = mapper();

        final Throwable thrown = catchThrowable(() -> mapper.readValue("{\"radius\":null}", Shape.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("unknown shape");
    }

    @Test
    void serialize_shouldWriteTheUnwrappedObject_whenUnionIsWritten() {
        final JsonMapper mapper = mapper();

        final byte[] json = mapper.writeValueAsBytes(new Square(new SquareBody(3)));
        final Object written = mapper.readValue(json, Object.class);

        assertThat(written).isEqualTo(Map.of("side", 3));
    }

    private static JsonMapper mapper() {
        final SimpleModule module = new SimpleModule();
        PolymorphicObjectAdapters.register(
                module,
                Shape.class,
                Shape::body,
                "unknown shape",
                List.of(
                        PolymorphicObjectAdapters.branch("radius", CircleBody.class, Circle::new),
                        PolymorphicObjectAdapters.branch("side", SquareBody.class, Square::new)));
        return JsonMapper.builder().addModule(module).build();
    }

    private sealed interface Shape permits Circle, Square {
        Object body();
    }

    private record Circle(CircleBody body) implements Shape {}

    private record Square(SquareBody body) implements Shape {}

    private record CircleBody(int radius) {}

    private record SquareBody(int side) {}
}
