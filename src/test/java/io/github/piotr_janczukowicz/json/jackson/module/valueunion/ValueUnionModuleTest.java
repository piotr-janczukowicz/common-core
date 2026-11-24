package io.github.piotr_janczukowicz.json.jackson.module.valueunion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.json.ValueUnion;
import io.github.piotr_janczukowicz.json.jackson.JsonMappers;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class ValueUnionModuleTest {

    private static final JsonMapper MAPPER = JsonMappers.snakeCase().build();

    @ValueUnion(
            string = Scalar.Text.class,
            integer = Scalar.IntegerValue.class,
            number = Scalar.DecimalValue.class,
            flag = Scalar.Flag.class,
            array = Scalar.Texts.class)
    sealed interface Scalar {

        record Text(@JsonValue String value) implements Scalar {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Text {}
        }

        record IntegerValue(@JsonValue long value) implements Scalar {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public IntegerValue {}
        }

        record DecimalValue(@JsonValue BigDecimal value) implements Scalar {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public DecimalValue {}
        }

        record Flag(@JsonValue boolean value) implements Scalar {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Flag {}
        }

        record Texts(@JsonValue List<String> values) implements Scalar {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Texts {
                values = List.copyOf(values);
            }
        }
    }

    @ValueUnion(string = Received.Text.class, integer = Received.IntegerValue.class, unknown = ReceivedUnknown.class)
    sealed interface Received permits Received.Text, Received.IntegerValue, ReceivedUnknown {

        record Text(@JsonValue String value) implements Received {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Text {}
        }

        record IntegerValue(@JsonValue long value) implements Received {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public IntegerValue {}
        }
    }

    record ReceivedUnknown(@JsonValue Object value) implements Received {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        ReceivedUnknown {}
    }

    @ValueUnion(string = Rule.Text.class, array = Rule.Values.class, object = RuleDetails.class)
    sealed interface Rule permits Rule.Text, Rule.Values, RuleDetails {

        record Text(@JsonValue String value) implements Rule {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Text {}
        }

        record Values(@JsonValue List<Object> values) implements Rule {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Values {}
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({@JsonSubTypes.Type(IfAnyMatch.class), @JsonSubTypes.Type(IfNoneMatch.class)})
    sealed interface RuleDetails extends Rule permits IfAnyMatch, IfNoneMatch {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record IfAnyMatch(List<String> ifAnyMatch) implements RuleDetails {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record IfNoneMatch(List<String> ifNoneMatch) implements RuleDetails {}

    @ValueUnion(flag = Named.Flag.class, object = Named.Fields.class)
    sealed interface Named {

        record Flag(@JsonValue boolean value) implements Named {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Flag {}
        }

        record Fields(String name) implements Named {}
    }

    @ValueUnion(unknown = Opaque.Kept.class)
    sealed interface Opaque {

        record Kept(@JsonValue Object value) implements Opaque {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public Kept {}
        }
    }

    @ValueUnion
    interface Empty {}

    @ValueUnion(integer = Whole.IntegerValue.class)
    sealed interface Whole {

        record IntegerValue(@JsonValue long value) implements Whole {
            @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
            public IntegerValue {}
        }
    }

    record Holder(@Nullable Scalar value, @Nullable List<Scalar> values) {}

    @Test
    void decode_shouldGiveTheStringBranch_whenTheJsonIsAString() {
        final Scalar value = MAPPER.readValue("\"high\"", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.Text("high"));
    }

    @Test
    void decode_shouldGiveTheIntegerBranch_whenTheIntegerFitsALong() {
        final Scalar value = MAPPER.readValue("3000000000", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.IntegerValue(3_000_000_000L));
    }

    @Test
    void decode_shouldGiveTheNumberBranch_whenTheIntegerIsBeyondALong() {
        final Scalar value = MAPPER.readValue("92233720368547758070", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.DecimalValue(new BigDecimal("92233720368547758070")));
    }

    @Test
    void decode_shouldGiveTheNumberBranch_whenTheJsonIsAFraction() {
        final Scalar value = MAPPER.readValue("1.5", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.DecimalValue(new BigDecimal("1.5")));
    }

    @Test
    void decode_shouldKeepTheScale_whenTheFractionIsWhole() {
        final Scalar value = MAPPER.readValue("2.0", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.DecimalValue(new BigDecimal("2.0")));
    }

    @Test
    void decode_shouldGivePlainNotation_whenTheNumberHasAnExponent() {
        final Scalar value = MAPPER.readValue("1e10", Scalar.class);

        assertSoftly(softly -> {
            softly.assertThat(value).isEqualTo(new Scalar.DecimalValue(new BigDecimal("10000000000")));
            softly.assertThat(MAPPER.writeValueAsString(value)).isEqualTo("10000000000");
        });
    }

    @Test
    void decode_shouldReadTheNumberExactly_whenADoubleWouldRoundIt() {
        final Scalar value = MAPPER.readValue("0.12345678901234567890123", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.DecimalValue(new BigDecimal("0.12345678901234567890123")));
    }

    @Test
    void decode_shouldRefuse_whenTheExponentIsOutOfRange() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("1e999999999", Scalar.class));

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("number exponent is out of range");
    }

    @Test
    void decode_shouldRefuse_whenTheExponentIsBelowTheRange() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("1e-999999999", Scalar.class));

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("number exponent is out of range");
    }

    @Test
    void decode_shouldGiveTheFlagBranch_whenTheJsonIsABoolean() {
        final Scalar value = MAPPER.readValue("false", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.Flag(false));
    }

    @Test
    void decode_shouldGiveTheArrayBranch_whenTheJsonIsAnArrayOfStrings() {
        final Scalar value = MAPPER.readValue("[\"a\",\"b\"]", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.Texts(List.of("a", "b")));
    }

    @Test
    void decode_shouldGiveAnEmptyList_whenTheArrayIsEmpty() {
        final Scalar value = MAPPER.readValue("[]", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.Texts(List.of()));
    }

    @Test
    void decode_shouldRefuse_whenAnItemOfAStringListIsANumber() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("[1]", Scalar.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("Scalar list items are strings");
    }

    @Test
    void decode_shouldRefuse_whenAnItemOfAStringListIsNull() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("[\"a\",null]", Scalar.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("Scalar list items are strings");
    }

    @Test
    void decode_shouldRefuseWithTheTakenTokens_whenNoBranchTakesTheToken() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("{}", Scalar.class));

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("Scalar takes a string, an integer, a number, a boolean or an array");
    }

    @Test
    void decode_shouldRefuseAFraction_whenTheUnionTakesIntegersOnly() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("2.0", Whole.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("Whole takes an integer");
    }

    @Test
    void decode_shouldRefuseAnyToken_whenTheUnionHasNoBranch() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("1", Empty.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("Empty takes no value");
    }

    @Test
    void decode_shouldGiveTheUnknownBranch_whenNoOtherBranchTakesABoolean() {
        final Received value = MAPPER.readValue("true", Received.class);

        assertThat(value).isEqualTo(new ReceivedUnknown(true));
    }

    @Test
    void decode_shouldGiveTheUnknownBranch_whenNoOtherBranchTakesAFraction() {
        final Received value = MAPPER.readValue("1.5", Received.class);

        assertThat(value).isEqualTo(new ReceivedUnknown(1.5));
    }

    @Test
    void decode_shouldGiveTheUnknownBranch_whenNoOtherBranchTakesAnArray() {
        final Received value = MAPPER.readValue("[1,\"a\"]", Received.class);

        assertThat(value).isEqualTo(new ReceivedUnknown(List.of(1, "a")));
    }

    @Test
    void decode_shouldGiveTheUnknownBranch_whenTheIntegerIsBeyondALongAndNoNumberBranchExists() {
        final Received value = MAPPER.readValue("92233720368547758070", Received.class);

        assertThat(value).isInstanceOf(ReceivedUnknown.class);
    }

    @Test
    void decode_shouldGiveTheIntegerBranch_whenTheUnionHasAnUnknownBranch() {
        final Received value = MAPPER.readValue("7", Received.class);

        assertThat(value).isEqualTo(new Received.IntegerValue(7));
    }

    @Test
    void encode_shouldWriteTheUnknownValueAsItCame_whenDecodedFromAnObject() {
        final String json = "{\"a\":1,\"b\":[true,null]}";

        final Received value = MAPPER.readValue(json, Received.class);

        assertSoftly(softly -> {
            softly.assertThat(value).isEqualTo(new ReceivedUnknown(Map.of("a", 1, "b", Arrays.asList(true, null))));
            softly.assertThat(MAPPER.writeValueAsString(value)).isEqualTo(json);
        });
    }

    @Test
    void decode_shouldKeepEveryToken_whenTheUnionHasOnlyAnUnknownBranch() {
        final Opaque value = MAPPER.readValue("\"a\"", Opaque.class);

        assertThat(value).isEqualTo(new Opaque.Kept("a"));
    }

    @Test
    void decode_shouldReadTheObjectUnion_whenTheObjectBranchIsADeductionUnion() {
        final Rule value = MAPPER.readValue("{\"if_none_match\":[\"*.env\"]}", Rule.class);

        assertThat(value).isEqualTo(new IfNoneMatch(List.of("*.env")));
    }

    @Test
    void decode_shouldReadAPlainRecord_whenTheObjectBranchIsARecord() {
        final Named value = MAPPER.readValue("{\"name\":\"n\"}", Named.class);

        assertThat(value).isEqualTo(new Named.Fields("n"));
    }

    @Test
    void decode_shouldReadAnyItem_whenTheArrayBranchHoldsObjects() {
        final Rule value = MAPPER.readValue("[1,\"a\",{\"k\":true}]", Rule.class);

        assertThat(value).isEqualTo(new Rule.Values(List.of(1, "a", Map.of("k", true))));
    }

    @Test
    void decode_shouldReadTheBranchAsItself_whenARecordBranchIsReadDirectly() {
        final IfAnyMatch value = MAPPER.readValue("{\"if_any_match\":[\"a\"]}", IfAnyMatch.class);

        assertThat(value).isEqualTo(new IfAnyMatch(List.of("a")));
    }

    @Test
    void decode_shouldReadTheBranchAsItself_whenAWrapperBranchIsReadDirectly() {
        final Scalar.Texts value = MAPPER.readValue("[\"a\"]", Scalar.Texts.class);

        assertThat(value).isEqualTo(new Scalar.Texts(List.of("a")));
    }

    @Test
    void decode_shouldGiveNull_whenTheJsonIsNull() {
        final Holder holder = MAPPER.readValue("{\"value\":null,\"values\":[\"a\",null,1]}", Holder.class);

        assertSoftly(softly -> {
            softly.assertThat(holder.value()).isNull();
            softly.assertThat(holder.values()).containsExactly(new Scalar.Text("a"), null, new Scalar.IntegerValue(1));
        });
    }

    @Test
    void encode_shouldWriteEachBranchInItsOwnForm_whenInARecord() {
        final Holder holder = new Holder(
                new Scalar.DecimalValue(new BigDecimal("2.0")),
                List.of(new Scalar.Text("a"), new Scalar.Flag(true), new Scalar.Texts(List.of("x"))));

        final String json = MAPPER.writeValueAsString(holder);

        assertSoftly(softly -> {
            softly.assertThat(json).isEqualTo("{\"value\":2.0,\"values\":[\"a\",true,[\"x\"]]}");
            softly.assertThat(MAPPER.readValue(json, Holder.class)).isEqualTo(holder);
        });
    }

    @Test
    void setupModule_shouldReadAValueUnion_whenAddedToAPlainMapper() {
        final JsonMapper mapper =
                JsonMapper.builder().addModule(new ValueUnionModule()).build();

        final Scalar value = mapper.readValue("\"a\"", Scalar.class);

        assertThat(value).isEqualTo(new Scalar.Text("a"));
    }

    @Test
    void version_shouldNameTheModule() {
        final ValueUnionModule module = new ValueUnionModule();

        final String artifact = module.version().getArtifactId();

        assertSoftly(softly -> {
            softly.assertThat(artifact).isEqualTo("common-json-value-union");
            softly.assertThat(module.getModuleName()).isEqualTo("ValueUnion");
        });
    }

    @Test
    void hasDeserializerFor_shouldTellAValueUnion_whenTheClassCarriesTheAnnotation() {
        final ValueUnionModule.ValueUnionDeserializers deserializers = new ValueUnionModule.ValueUnionDeserializers();

        final boolean found = deserializers.hasDeserializerFor(MAPPER.deserializationConfig(), Scalar.class);

        assertThat(found).isTrue();
    }

    @Test
    void hasDeserializerFor_shouldTellABranch_whenOnlyItsUnionCarriesTheAnnotation() {
        final ValueUnionModule.ValueUnionDeserializers deserializers = new ValueUnionModule.ValueUnionDeserializers();

        final boolean found = deserializers.hasDeserializerFor(MAPPER.deserializationConfig(), Scalar.Text.class);

        assertThat(found).isFalse();
    }
}
