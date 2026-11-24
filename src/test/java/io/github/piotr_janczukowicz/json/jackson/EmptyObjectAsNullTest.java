package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.json.EmptyObjectAsNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class EmptyObjectAsNullTest {

    private static final JsonMapper MAPPER = JsonMappers.snakeCase().build();

    record User(String login, @Nullable Integer id) {}

    record Label(@EmptyObjectAsNull @Nullable String name) {}

    record Commit(
            String sha,
            @EmptyObjectAsNull @Nullable User author,
            @Nullable User committer) {}

    @Test
    void read_shouldReturnNull_whenValueIsAnEmptyObject() {
        final Commit commit = MAPPER.readValue("{\"sha\":\"a\",\"author\":{}}", Commit.class);

        assertThat(commit.author()).isNull();
    }

    @Test
    void read_shouldReturnNull_whenValueIsJsonNull() {
        final Commit commit = MAPPER.readValue("{\"sha\":\"a\",\"author\":null}", Commit.class);

        assertThat(commit.author()).isNull();
    }

    @Test
    void read_shouldReturnNull_whenPropertyIsMissing() {
        final Commit commit = MAPPER.readValue("{\"sha\":\"a\"}", Commit.class);

        assertThat(commit.author()).isNull();
    }

    @Test
    void read_shouldReturnTheDeclaredType_whenValueIsAnObjectWithProperties() {
        final Commit commit = MAPPER.readValue("{\"sha\":\"a\",\"author\":{\"login\":\"x\",\"id\":3}}", Commit.class);

        assertThat(commit.author()).isEqualTo(new User("x", 3));
    }

    @Test
    void read_shouldRefuseTheValue_whenItIsAnArray() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("{\"sha\":\"a\",\"author\":[]}", Commit.class));

        assertThat(thrown).isInstanceOf(DatabindException.class);
    }

    @Test
    void read_shouldRefuseTheValue_whenItIsAScalar() {
        final Throwable thrown =
                catchThrowable(() -> MAPPER.readValue("{\"sha\":\"a\",\"author\":\"x\"}", Commit.class));

        assertThat(thrown).isInstanceOf(DatabindException.class);
    }

    @Test
    void read_shouldKeepTheEmptyObjectAsARecord_whenPropertyIsNotMarked() {
        final Commit commit = MAPPER.readValue("{\"sha\":\"a\",\"committer\":{}}", Commit.class);

        assertThat(commit.committer()).isEqualTo(new User(null, null));
    }

    @Test
    void write_shouldWriteNull_whenValueIsNull() {
        assertThat(MAPPER.writeValueAsString(new Commit("a", null, null))).contains("\"author\":null");
    }

    @Test
    void module_shouldReportItsVersionThroughTheIntrospector() {
        assertThat(new ValueUnionModule.EmptyObjectAsNullIntrospector().version())
                .isEqualTo(new ValueUnionModule().version());
    }

    @Test
    void read_shouldReadTheDeclaredType_whenValueIsNotAnObject() {
        assertThat(MAPPER.readValue("{\"name\":\"x\"}", Label.class).name()).isEqualTo("x");
    }
}
