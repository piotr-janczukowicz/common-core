package io.github.piotr_janczukowicz.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;

import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class VariablesTest {

    enum State {
        OPEN("OPEN_STATE");

        private final String wire;

        State(final String wire) {
            this.wire = wire;
        }

        String wireValue() {
            return wire;
        }
    }

    record Input(String id) {
        Map<String, Object> toVariable() {
            return Map.of("id", id);
        }
    }

    @Test
    void build_shouldKeepTheJsonTypesInTheOrderOfTheCalls_whenEveryKindIsAdded() {
        final Variables variables = Variables.builder();

        final Map<String, Object> built = variables
                .value("count", 3)
                .value("draft", true)
                .value("ids", List.of("a", "b"))
                .value("state", State.OPEN, State::wireValue)
                .value("since", LocalDate.parse("2026-01-02"), Object::toString)
                .values("states", List.of(State.OPEN, State.OPEN), State::wireValue)
                .input("input", new Input("x"), Input::toVariable)
                .inputs("inputs", List.of(new Input("y"), new Input("z")), Input::toVariable)
                .clearable("after", OptionalNull.of("c"))
                .clearable("mapped", OptionalNull.of(State.OPEN), State::wireValue)
                .build();

        assertThat(built)
                .containsExactly(
                        entry("count", 3),
                        entry("draft", true),
                        entry("ids", List.of("a", "b")),
                        entry("state", "OPEN_STATE"),
                        entry("since", "2026-01-02"),
                        entry("states", List.of("OPEN_STATE", "OPEN_STATE")),
                        entry("input", Map.of("id", "x")),
                        entry("inputs", List.of(Map.of("id", "y"), Map.of("id", "z"))),
                        entry("after", "c"),
                        entry("mapped", "OPEN_STATE"));
    }

    @Test
    void build_shouldLeaveOutEveryNullValue_whenNothingIsSet() {
        final State noState = null;
        final List<State> noStates = null;
        final Input noInput = null;
        final List<Input> noInputs = null;
        final OptionalNull<String> noText = null;
        final OptionalNull<State> noWrappedState = null;

        final Map<String, Object> built = Variables.builder()
                .value("a", null)
                .value("b", noState, State::wireValue)
                .values("c", noStates, State::wireValue)
                .input("d", noInput, Input::toVariable)
                .inputs("e", noInputs, Input::toVariable)
                .clearable("f", noText)
                .clearable("g", noWrappedState, State::wireValue)
                .build();

        assertThat(built).isEmpty();
    }

    @Test
    void clearable_shouldWriteJsonNull_whenTheValueIsExplicitlyNull() {
        final Map<String, Object> built = Variables.builder()
                .clearable("after", OptionalNull.ofNull())
                .clearable("state", OptionalNull.<State>ofNull(), State::wireValue)
                .build();

        assertThat(built).containsExactly(entry("after", null), entry("state", null));
    }

    @Test
    void build_shouldKeepAnEmptyList_whenAListIsEmpty() {
        final Map<String, Object> built = Variables.builder()
                .value("ids", List.of())
                .values("states", List.<State>of(), State::wireValue)
                .inputs("inputs", List.<Input>of(), Input::toVariable)
                .build();

        assertThat(built)
                .containsExactly(entry("ids", List.of()), entry("states", List.of()), entry("inputs", List.of()));
    }

    @Test
    void build_shouldReplaceTheValue_whenANameIsAddedTwice() {
        final Map<String, Object> built =
                Variables.builder().value("a", 1).value("a", 2).build();

        assertThat(built).containsExactly(entry("a", 2));
    }

    @Test
    void build_shouldReturnAnUnmodifiableMap_whenCalled() {
        final Map<String, Object> built = Variables.builder().value("a", 1).build();

        final Throwable thrown = catchThrowable(() -> built.put("b", 2));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void build_shouldNotChange_whenTheBuilderIsUsedAfterwards() {
        final Variables variables = Variables.builder().value("a", 1);
        final Map<String, Object> built = variables.build();

        variables.value("b", 2);

        assertThat(built).containsExactly(entry("a", 1));
    }

    @Test
    void value_shouldRejectANullName_whenTheNameIsMissing() {
        final Variables variables = Variables.builder();

        final Throwable thrown = catchThrowable(() -> variables.value(null, 1));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void value_shouldRejectANullMapper_whenTheValueIsNull() {
        final Variables variables = Variables.builder();

        final Throwable thrown = catchThrowable(() -> variables.value("a", null, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("mapper");
    }

    @Test
    void value_shouldRejectANullResult_whenTheMapperReturnsNull() {
        final Variables variables = Variables.builder();

        final Throwable thrown = catchThrowable(() -> variables.value("state", State.OPEN, state -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("state");
    }

    @Test
    void values_shouldRejectANullItem_whenTheListHoldsOne() {
        final Variables variables = Variables.builder();
        final List<State> values = Arrays.asList(State.OPEN, null);

        final Throwable thrown = catchThrowable(() -> variables.values("states", values, State::wireValue));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("states");
    }

    @Test
    void values_shouldRejectANullResult_whenTheMapperReturnsNull() {
        final Variables variables = Variables.builder();
        final List<State> values = List.of(State.OPEN);

        final Throwable thrown = catchThrowable(() -> variables.values("states", values, state -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("states");
    }

    @Test
    void input_shouldRejectANullResult_whenTheMapperReturnsNull() {
        final Variables variables = Variables.builder();
        final Input value = new Input("x");

        final Throwable thrown = catchThrowable(() -> variables.input("input", value, input -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("input");
    }

    @Test
    void input_shouldRejectANullMapper_whenTheValueIsNull() {
        final Variables variables = Variables.builder();

        final Throwable thrown = catchThrowable(() -> variables.input("input", null, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("toVariable");
    }

    @Test
    void inputs_shouldRejectANullItem_whenTheListHoldsOne() {
        final Variables variables = Variables.builder();
        final List<Input> values = Arrays.asList(new Input("x"), null);

        final Throwable thrown = catchThrowable(() -> variables.inputs("inputs", values, Input::toVariable));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("inputs");
    }

    @Test
    void inputs_shouldRejectANullResult_whenTheMapperReturnsNull() {
        final Variables variables = Variables.builder();
        final List<Input> values = List.of(new Input("x"));

        final Throwable thrown = catchThrowable(() -> variables.inputs("inputs", values, input -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("inputs");
    }

    @Test
    void clearable_shouldRejectANullResult_whenTheMapperReturnsNull() {
        final Variables variables = Variables.builder();
        final OptionalNull<State> value = OptionalNull.of(State.OPEN);

        final Throwable thrown = catchThrowable(() -> variables.clearable("state", value, state -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("state");
    }

    @Test
    void clearable_shouldRejectANullName_whenTheNameIsMissing() {
        final Variables variables = Variables.builder();
        final OptionalNull<String> value = OptionalNull.ofNull();

        final Throwable thrown = catchThrowable(() -> variables.clearable(null, value));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("name");
    }

    @Test
    void input_shouldKeepANullMapValue_whenTheMapHoldsOne() {
        final Map<String, Object> nested = new HashMap<>();
        nested.put("afterId", null);

        final Map<String, Object> built =
                Variables.builder().input("input", nested, map -> map).build();

        assertThat(built).containsExactly(entry("input", nested));
    }
}
