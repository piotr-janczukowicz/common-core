package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.json.PresenceUnion;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class PresenceUnionModuleTest {

    private static final JsonMapper MAPPER = JsonMappers.snakeCase().build();
    private static final String ACTIONS_LOG = """
            {"path": "a.yml", "start_line": 1}""";

    @PresenceUnion(
            keys = {
                @PresenceUnion.Key(name = "blob_url", type = Commit.class),
                @PresenceUnion.Key(name = "issue_url", type = Issue.class),
                @PresenceUnion.Key(name = "comment_url", type = Comment.class)
            },
            fallback = ActionsLog.class,
            fallbackRequires = {"path", "start_line"},
            unknown = LocationUnknown.class)
    @JsonSubTypes({
        @JsonSubTypes.Type(Commit.class),
        @JsonSubTypes.Type(Issue.class),
        @JsonSubTypes.Type(Comment.class),
        @JsonSubTypes.Type(ActionsLog.class)
    })
    sealed interface Location permits Commit, Issue, Comment, ActionsLog, LocationUnknown {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record Commit(
            String path,
            int startLine,
            String blobUrl,
            @Nullable String htmlUrl) implements Location {
        Commit {
            requireNonNull(path, "path");
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record Issue(@Nullable String issueUrl, @Nullable String htmlUrl) implements Location {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record Comment(@Nullable String commentUrl, @Nullable String htmlUrl) implements Location {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record ActionsLog(@Nullable String path, int startLine) implements Location {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record LocationUnknown(@JsonValue Map<String, Object> properties) implements Location {}

    record Alert(@Nullable Location location, @Nullable List<Location> locations) {}

    @PresenceUnion(
            keys = {@PresenceUnion.Key(name = "issue_url", type = Issue.class)},
            fallback = ActionsLog.class,
            fallbackRequires = "path")
    interface Sent {}

    @PresenceUnion(keys = {@PresenceUnion.Key(name = "issue_url", type = Issue.class)})
    interface Keyed {}

    @PresenceUnion(
            keys = {
                @PresenceUnion.Key(name = "issue_url", type = Issue.class),
                @PresenceUnion.Key(name = "issue_url", type = Comment.class)
            })
    interface Twice {}

    @Test
    void readValue_shouldGiveTheBranchOfTheKey_whenTheObjectHasOneKey() {
        final Location commit = MAPPER.readValue("""
                {"path": "p", "start_line": 1, "blob_url": "b", "html_url": "h"}""", Location.class);
        final Location issue = MAPPER.readValue("""
                {"html_url": "h", "issue_url": "i"}""", Location.class);
        final Location comment = MAPPER.readValue("""
                {"comment_url": "c", "html_url": "h"}""", Location.class);

        assertThat(List.of(commit, issue, comment))
                .containsExactly(new Commit("p", 1, "b", "h"), new Issue("i", "h"), new Comment("c", "h"));
    }

    @Test
    void readValue_shouldGiveTheBranchOfTheFirstKey_whenTheObjectHasTwoKeys() {
        final Location issueFirst = MAPPER.readValue("""
                {"issue_url": "i", "comment_url": "c"}""", Location.class);
        final Location commentFirst = MAPPER.readValue("""
                {"comment_url": "c", "issue_url": "i"}""", Location.class);

        assertThat(List.of(issueFirst, commentFirst)).containsExactly(new Issue("i", null), new Comment("c", null));
    }

    @Test
    void readValue_shouldGiveTheBranchOfTheKey_whenTheKeyIsNull() {
        final Location found = MAPPER.readValue("""
                {"comment_url": null}""", Location.class);

        assertThat(found).isEqualTo(new Comment(null, null));
    }

    @Test
    void readValue_shouldGiveTheBranchOfTheKey_whenAnEarlierPropertyHoldsAnObject() {
        final Location found = MAPPER.readValue("""
                {"extra": {"issue_url": "nested", "list": [1, {"a": 2}]}, "comment_url": "c"}""", Location.class);

        assertThat(found).isEqualTo(new Comment("c", null));
    }

    @Test
    void readValue_shouldGiveTheFallback_whenTheObjectHasEveryNameItRequires() {
        final Location found = MAPPER.readValue(ACTIONS_LOG, Location.class);

        assertThat(found).isEqualTo(new ActionsLog("a.yml", 1));
    }

    @Test
    void readValue_shouldGiveTheFallback_whenASharedPropertyIsBesideItsNames() {
        final Location found = MAPPER.readValue("""
                {"html_url": "h", "path": "a.yml", "start_line": 1}""", Location.class);

        assertThat(found).isEqualTo(new ActionsLog("a.yml", 1));
    }

    @Test
    void readValue_shouldGiveTheFallback_whenARequiredNameIsNull() {
        final Location found = MAPPER.readValue("""
                {"path": null, "start_line": 1}""", Location.class);

        assertThat(found).isEqualTo(new ActionsLog(null, 1));
    }

    @Test
    void readValue_shouldGiveTheUnknown_whenTheFallbackMissesARequiredName() {
        final Location found = MAPPER.readValue("""
                {"path": "a.yml", "html_url": null}""", Location.class);

        final Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("path", "a.yml");
        expected.put("html_url", null);
        assertThat(found).isEqualTo(new LocationUnknown(expected));
    }

    @Test
    void readValue_shouldGiveTheUnknown_whenTheObjectIsEmpty() {
        final Location found = MAPPER.readValue("{}", Location.class);

        assertThat(found).isEqualTo(new LocationUnknown(Map.of()));
    }

    @Test
    void readValue_shouldRefuse_whenTheBranchOfTheKeyCannotReadTheObject() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("""
                {"blob_url": "b", "path": null, "start_line": 1}""", Location.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("path");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "\"s\"", "1", "true"})
    void readValue_shouldRefuse_whenTheValueIsNoObject(final String json) {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue(json, Location.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("Location takes an object");
    }

    @Test
    void readValue_shouldGiveNull_whenTheValueIsNull() {
        final Location found = MAPPER.readValue("null", Location.class);

        assertThat(found).isNull();
    }

    @Test
    void readValue_shouldRefuse_whenNoRuleMatchesAndTheUnionHasNoUnknown() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("""
                {"start_line": 1}""", Sent.class));

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining(
                        "Sent matches no branch: the object has none of its keys and not every property of ActionsLog");
    }

    @Test
    void readValue_shouldGiveTheFallback_whenTheUnionHasNoUnknown() {
        final Object found = MAPPER.readValue("""
                {"path": "a.yml"}""", Sent.class);

        assertThat(found).isEqualTo(new ActionsLog("a.yml", 0));
    }

    @Test
    void readValue_shouldRefuse_whenNoKeyMatchesAndTheUnionHasNoFallback() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue(ACTIONS_LOG, Keyed.class));

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("Keyed matches no branch: the object has none of its keys")
                .hasMessageNotContaining("property of");
    }

    @Test
    void readValue_shouldRefuse_whenTheUnionNamesAKeyTwice() {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue("{}", Twice.class));

        assertThat(thrown).hasStackTraceContaining("names the key issue_url twice");
    }

    @Test
    void readValue_shouldReadTheUnion_whenItIsAComponentOrAListItem() {
        final Alert found = MAPPER.readValue("""
                {"location": {"issue_url": "i"}, "locations": [{"path": "a.yml", "start_line": 2}, {"x": 1}, null]}""", Alert.class);

        assertSoftly(softly -> {
            softly.assertThat(found.location()).isEqualTo(new Issue("i", null));
            softly.assertThat(found.locations())
                    .containsExactly(new ActionsLog("a.yml", 2), new LocationUnknown(Map.of("x", 1)), null);
        });
    }

    @Test
    void readValue_shouldReadTheUnion_whenItIsTheItemTypeOfARootList() {
        final List<Location> found = MAPPER.readValue("""
                [{"comment_url": "c"}, {}]""", new TypeReference<List<Location>>() {});

        assertThat(found).containsExactly(new Comment("c", null), new LocationUnknown(Map.of()));
    }

    @Test
    void readValue_shouldReadABranchAsItself_whenTheBranchIsRead() {
        final ActionsLog actionsLog = MAPPER.readValue("""
                {"start_line": 3}""", ActionsLog.class);
        final LocationUnknown unknown = MAPPER.readValue("""
                {"issue_url": "i"}""", LocationUnknown.class);

        assertSoftly(softly -> {
            softly.assertThat(actionsLog).isEqualTo(new ActionsLog(null, 3));
            softly.assertThat(unknown).isEqualTo(new LocationUnknown(Map.of("issue_url", "i")));
        });
    }

    @Test
    void writeValueAsString_shouldWriteTheBranchAsIt_whenABranchOrTheUnknownIsWritten() {
        final Alert alert = new Alert(
                new Issue("i", null), List.of(new ActionsLog("a.yml", 1), new LocationUnknown(Map.of("x", 1))));

        final String written = MAPPER.writeValueAsString(alert);

        final Alert readBack = MAPPER.readValue(written, Alert.class);
        assertSoftly(softly -> {
            softly.assertThat(readBack).isEqualTo(alert);
            softly.assertThat(written).doesNotContain("@type").contains("{\"x\":1}");
        });
    }

    @Test
    void hasDeserializerFor_shouldTellAPresenceUnion_whenTheClassCarriesTheAnnotation() {
        final ValueUnionModule.ValueUnionDeserializers deserializers = new ValueUnionModule.ValueUnionDeserializers();

        final boolean found = deserializers.hasDeserializerFor(MAPPER.deserializationConfig(), Location.class);

        assertThat(found).isTrue();
    }

    @Test
    void hasDeserializerFor_shouldTellABranch_whenOnlyItsPresenceUnionCarriesTheAnnotation() {
        final ValueUnionModule.ValueUnionDeserializers deserializers = new ValueUnionModule.ValueUnionDeserializers();

        final boolean found = deserializers.hasDeserializerFor(MAPPER.deserializationConfig(), Issue.class);

        assertThat(found).isFalse();
    }
}
