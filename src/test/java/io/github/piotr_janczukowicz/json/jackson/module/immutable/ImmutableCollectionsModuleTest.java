package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;

class ImmutableCollectionsModuleTest {

    private static final ObjectMapper MAPPER =
            JsonMapper.builder().addModule(new ImmutableCollectionsModule()).build();

    @Test
    void readValue_shouldReturnTypedList_whenJsonArrayOfObjects() {
        final List<Item> items = MAPPER.readValue("[{\"name\":\"a\"}]", new TypeReference<>() {});

        assertThat(items).containsExactly(new Item("a"));
    }

    @Test
    void readValue_shouldReturnUnmodifiableList_whenJsonArray() {
        final List<Item> items = MAPPER.readValue("[{\"name\":\"a\"}]", new TypeReference<>() {});

        final Throwable thrown = catchThrowable(() -> items.add(new Item("b")));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readValue_shouldReturnTypedSet_whenJsonArrayOfObjects() {
        final Set<Item> items = MAPPER.readValue("[{\"name\":\"a\"},{\"name\":\"a\"}]", new TypeReference<>() {});

        assertThat(items).containsExactly(new Item("a"));
    }

    @Test
    void readValue_shouldReturnUnmodifiableSet_whenJsonArray() {
        final Set<String> tags = MAPPER.readValue("[\"a\"]", new TypeReference<>() {});

        final Throwable thrown = catchThrowable(() -> tags.add("b"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readValue_shouldReturnUnmodifiableSet_whenElementsAreEnums() {
        final Set<Level> levels = MAPPER.readValue("[\"LOW\"]", new TypeReference<>() {});

        final Throwable thrown = catchThrowable(() -> levels.add(Level.HIGH));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readValue_shouldReturnTypedMap_whenJsonObjectOfObjects() {
        final Map<String, Item> items = MAPPER.readValue("{\"k\":{\"name\":\"a\"}}", new TypeReference<>() {});

        assertThat(items).containsExactly(Map.entry("k", new Item("a")));
    }

    @Test
    void readValue_shouldReturnUnmodifiableMap_whenJsonObject() {
        final Map<String, Integer> counts = MAPPER.readValue("{\"a\":1}", new TypeReference<>() {});

        final Throwable thrown = catchThrowable(() -> counts.put("b", 2));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readValue_shouldKeepNullElements_whenListContainsNull() {
        final List<Item> items = MAPPER.readValue("[null,{\"name\":\"a\"}]", new TypeReference<>() {});

        assertThat(items).containsExactly(null, new Item("a"));
    }

    @Test
    void readValue_shouldKeepNullElements_whenSetContainsNull() {
        final Set<String> tags = MAPPER.readValue("[\"a\",null]", new TypeReference<>() {});

        assertThat(tags).containsExactlyInAnyOrder("a", null);
    }

    @Test
    void readValue_shouldKeepNullValues_whenMapContainsNull() {
        final Map<String, Integer> counts = MAPPER.readValue("{\"a\":1,\"b\":null}", new TypeReference<>() {});

        assertThat(counts).hasSize(2).containsEntry("a", 1).containsEntry("b", null);
    }

    @Test
    void readValue_shouldReturnNullCollections_whenFieldsAreNull() {
        final String json = "{\"items\":null,\"tags\":null,\"counts\":null,\"values\":null}";

        final Holder holder = MAPPER.readValue(json, Holder.class);

        assertSoftly(softly -> {
            softly.assertThat(holder.items()).isNull();
            softly.assertThat(holder.tags()).isNull();
            softly.assertThat(holder.counts()).isNull();
            softly.assertThat(holder.values()).isNull();
        });
    }

    @Test
    void readValue_shouldReturnNullCollections_whenFieldsAreAbsent() {
        final Holder holder = MAPPER.readValue("{}", Holder.class);

        assertSoftly(softly -> {
            softly.assertThat(holder.items()).isNull();
            softly.assertThat(holder.tags()).isNull();
            softly.assertThat(holder.counts()).isNull();
            softly.assertThat(holder.values()).isNull();
        });
    }

    @Test
    void readValue_shouldReturnUnmodifiableList_whenTypeIsCollection() {
        final Holder holder = MAPPER.readValue("{\"values\":[\"a\"]}", Holder.class);
        final Collection<String> values = holder.values();

        final Throwable thrown = catchThrowable(() -> values.add("b"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readValue_shouldKeepConcreteType_whenTypeIsArrayList() {
        final Holder holder = MAPPER.readValue("{\"mutable\":[\"a\"]}", Holder.class);

        holder.mutable().add("b");

        assertThat(holder.mutable()).containsExactly("a", "b");
    }

    @Test
    void readValue_shouldKeepConcreteType_whenTypeIsTreeSet() {
        final TreeSet<String> tags = MAPPER.readValue("[\"a\"]", new TypeReference<>() {});

        tags.add("b");

        assertThat(tags).containsExactly("a", "b");
    }

    @Test
    void readValue_shouldKeepConcreteType_whenTypeIsTreeMap() {
        final TreeMap<String, Integer> counts = MAPPER.readValue("{\"a\":1}", new TypeReference<>() {});

        counts.put("b", 2);

        assertThat(counts).containsOnlyKeys("a", "b");
    }

    @Test
    void getEmptyValue_shouldReturnEmptyList() {
        final ImmutableListDeserializer deserializer = new ImmutableListDeserializer(delegateFor(List.class));

        final Object value = deserializer.getEmptyValue(MAPPER._deserializationContext());

        assertThat(value).isEqualTo(List.of());
    }

    @Test
    void getEmptyValue_shouldReturnEmptySet() {
        final ImmutableSetDeserializer deserializer = new ImmutableSetDeserializer(delegateFor(Set.class));

        final Object value = deserializer.getEmptyValue(MAPPER._deserializationContext());

        assertThat(value).isEqualTo(Set.of());
    }

    @Test
    void getEmptyValue_shouldReturnEmptyMap() {
        final ImmutableMapDeserializer deserializer = new ImmutableMapDeserializer(delegateFor(Map.class));

        final Object value = deserializer.getEmptyValue(MAPPER._deserializationContext());

        assertThat(value).isEqualTo(Map.of());
    }

    private static ValueDeserializer<?> delegateFor(final Class<?> rawClass) {
        final JavaType type = MAPPER.getTypeFactory().constructType(rawClass);
        return MAPPER._deserializationContext().findRootValueDeserializer(type);
    }

    enum Level {
        LOW,
        HIGH
    }

    record Item(String name) {}

    record Holder(
            List<Item> items,
            Set<String> tags,
            Map<String, Integer> counts,
            Collection<String> values,
            ArrayList<String> mutable) {}
}
