package io.github.piotr_janczukowicz.http.uri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class QueryParametersTest {

    enum Order {
        CREATED("created_at");

        private final String wire;

        Order(final String wire) {
            this.wire = wire;
        }

        String wireValue() {
            return wire;
        }
    }

    @Test
    void build_shouldKeepTheOrderOfTheCalls_whenSeveralKindsAreAdded() {
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, String> query = parameters
                .text("state", "opened")
                .value("confidential", true)
                .value("created_after", Instant.parse("2026-01-01T00:00:00Z"))
                .wire("order_by", Order.CREATED, Order::wireValue)
                .build();

        assertThat(query)
                .containsExactly(
                        Map.entry("state", "opened"),
                        Map.entry("confidential", "true"),
                        Map.entry("created_after", "2026-01-01T00:00:00Z"),
                        Map.entry("order_by", "created_at"));
    }

    @Test
    void build_shouldLeaveOutEveryNullValue_whenNothingIsSet() {
        final QueryParameters parameters = QueryParameters.builder();
        final Order noOrder = null;
        final List<Order> noOrders = null;

        final Map<String, String> query = parameters
                .text("a", null)
                .value("b", null)
                .wire("c", noOrder, Order::wireValue)
                .list("d", null)
                .wireList("e", noOrders, Order::wireValue)
                .map("f", null)
                .build();

        assertThat(query).isEmpty();
    }

    @Test
    void build_shouldJoinListsWithCommas_whenListsAreAdded() {
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, String> query = parameters
                .list("labels", List.of("a", "b"))
                .list("ids", List.of(1L, 2L))
                .wireList("order", List.of(Order.CREATED, Order.CREATED), Order::wireValue)
                .build();

        assertThat(query)
                .containsExactly(
                        Map.entry("labels", "a,b"),
                        Map.entry("ids", "1,2"),
                        Map.entry("order", "created_at,created_at"));
    }

    @Test
    void build_shouldWriteNameBracketKey_whenAMapIsAdded() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("a", 1);
        values.put("b", "x");
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, String> query =
                parameters.map("custom_attributes", values).build();

        assertThat(query)
                .containsExactly(Map.entry("custom_attributes[a]", "1"), Map.entry("custom_attributes[b]", "x"));
    }

    @Test
    void all_shouldAddAndReplace_whenGivenEntries() {
        final QueryParameters parameters = QueryParameters.builder().text("a", "1");

        final Map<String, String> query = parameters.all(Map.of("a", "2")).build();

        assertThat(query).containsExactly(Map.entry("a", "2"));
    }

    @Test
    void all_shouldRejectNull_whenParametersAreMissing() {
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.all(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void build_shouldReturnAnUnmodifiableMap_whenCalled() {
        final Map<String, String> query =
                QueryParameters.builder().text("a", "1").build();

        final Throwable thrown = catchThrowable(() -> query.put("b", "2"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void text_shouldRejectABlankName_whenTheNameIsEmpty() {
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.text(" ", "x"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void list_shouldRejectANullItem_whenTheListHoldsOne() {
        final QueryParameters parameters = QueryParameters.builder();
        final List<String> values = Arrays.asList("a", null);

        final Throwable thrown = catchThrowable(() -> parameters.list("labels", values));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("labels");
    }

    @Test
    void wire_shouldRejectANullWireValue_whenTheFunctionReturnsNull() {
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.wire("order_by", Order.CREATED, order -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("order_by");
    }

    @Test
    void wireList_shouldRejectANullWireValue_whenTheFunctionReturnsNull() {
        final QueryParameters parameters = QueryParameters.builder();
        final List<Order> values = List.of(Order.CREATED);

        final Throwable thrown = catchThrowable(() -> parameters.wireList("order", values, order -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("order");
    }

    @Test
    void wireList_shouldRejectANullItem_whenTheListHoldsOne() {
        final QueryParameters parameters = QueryParameters.builder();
        final List<Order> values = Arrays.asList(Order.CREATED, null);

        final Throwable thrown = catchThrowable(() -> parameters.wireList("order", values, Order::wireValue));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("order");
    }

    @Test
    void map_shouldLeaveOutAnEntryWithANullValue_whenTheMapHoldsOne() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("a", 1);
        values.put("b", null);
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, String> query = parameters.map("attrs", values).build();

        assertThat(query).containsExactly(Map.entry("attrs[a]", "1"));
    }

    @Test
    void map_shouldRejectANullKey_whenTheMapHoldsOne() {
        final Map<String, Object> values = new HashMap<>();
        values.put(null, 1);
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.map("attrs", values));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("attrs");
    }

    @Test
    void all_shouldLeaveOutNullValues_whenAnEntryHoldsOne() {
        final Map<String, String> entries = new HashMap<>();
        entries.put("a", null);
        entries.put("b", "2");

        final Map<String, String> query = QueryParameters.builder().all(entries).build();

        assertThat(query).containsExactly(Map.entry("b", "2"));
    }

    @Test
    void all_shouldRejectABlankName_whenAnEntryHoldsOne() {
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.all(Map.of(" ", "1")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void list_shouldLeaveOutTheParameter_whenTheListIsEmpty() {
        final Map<String, String> query =
                QueryParameters.builder().list("a", List.of()).build();

        assertThat(query).isEmpty();
    }

    @Test
    void wireList_shouldLeaveOutTheParameter_whenTheListIsEmpty() {
        final Map<String, String> query = QueryParameters.builder()
                .wireList("a", List.of(), String::length)
                .build();

        assertThat(query).isEmpty();
    }

    @Test
    void wire_shouldRejectANullFunction_whenTheValueIsNull() {
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.wire("a", null, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildValues_shouldRepeatEachItem_whenARepeatedListIsAdded() {
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, List<String>> query = parameters
                .text("state", "opened")
                .repeated("owner[]", List.of("a", "b"))
                .wireRepeated("order", List.of(Order.CREATED), Order::wireValue)
                .list("labels", List.of("x", "y"))
                .buildValues();

        assertThat(query)
                .containsExactly(
                        Map.entry("state", List.of("opened")),
                        Map.entry("owner[]", List.of("a", "b")),
                        Map.entry("order", List.of("created_at")),
                        Map.entry("labels", List.of("x,y")));
    }

    @Test
    void buildValues_shouldLeaveOutTheParameter_whenTheRepeatedListIsNullOrEmpty() {
        final QueryParameters parameters = QueryParameters.builder();
        final List<Order> noOrders = null;

        final Map<String, List<String>> query = parameters
                .repeated("a", null)
                .repeated("b", List.of())
                .wireRepeated("c", noOrders, Order::wireValue)
                .wireRepeated("d", List.of(), Order::wireValue)
                .buildValues();

        assertThat(query).isEmpty();
    }

    @Test
    void repeated_shouldSkipNullItems_whenTheListHoldsThem() {
        final List<Long> values = Arrays.asList(1L, null, 2L);
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, List<String>> query =
                parameters.repeated("ids", values).buildValues();

        assertThat(query).containsExactly(Map.entry("ids", List.of("1", "2")));
    }

    @Test
    void repeated_shouldLeaveOutTheParameter_whenEveryItemIsNull() {
        final List<String> values = Arrays.asList(null, null);
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, List<String>> query =
                parameters.repeated("ids", values).buildValues();

        assertThat(query).isEmpty();
    }

    @Test
    void wireRepeated_shouldSkipNullItems_whenTheListHoldsThem() {
        final List<Order> values = Arrays.asList(null, Order.CREATED);
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, List<String>> query =
                parameters.wireRepeated("order", values, Order::wireValue).buildValues();

        assertThat(query).containsExactly(Map.entry("order", List.of("created_at")));
    }

    @Test
    void repeated_shouldReplaceTheEarlierValue_whenTheNameIsAddedTwice() {
        final QueryParameters parameters = QueryParameters.builder().text("a", "1");

        final Map<String, List<String>> query =
                parameters.repeated("a", List.of("2", "3")).buildValues();

        assertThat(query).containsExactly(Map.entry("a", List.of("2", "3")));
    }

    @Test
    void repeated_shouldRejectABlankName_whenTheNameIsBlank() {
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.repeated(" ", List.of("x")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void wireRepeated_shouldRejectANullFunction_whenTheValuesAreNull() {
        final QueryParameters parameters = QueryParameters.builder();

        final Throwable thrown = catchThrowable(() -> parameters.wireRepeated("a", null, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("wireValue");
    }

    @Test
    void wireRepeated_shouldRejectANullWireValue_whenTheFunctionReturnsNull() {
        final QueryParameters parameters = QueryParameters.builder();
        final List<Order> values = List.of(Order.CREATED);

        final Throwable thrown = catchThrowable(() -> parameters.wireRepeated("order", values, order -> null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("order");
    }

    @Test
    void build_shouldRefuseTheRepeatedParameter_whenItHoldsSeveralValues() {
        final QueryParameters parameters = QueryParameters.builder().repeated("owner[]", List.of("a", "b"));

        final Throwable thrown = catchThrowable(parameters::build);

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("owner[]")
                .hasMessageContaining("buildValues()");
    }

    @Test
    void build_shouldHoldEverySingleValue_whenARepeatedParameterWasReplaced() {
        final QueryParameters parameters = QueryParameters.builder()
                .text("a", "1")
                .repeated("b", List.of("x", "y"))
                .text("b", "z");

        final Map<String, String> query = parameters.build();

        assertThat(query).containsExactly(Map.entry("a", "1"), Map.entry("b", "z"));
    }

    @Test
    void build_shouldHoldTheOneValue_whenARepeatedParameterHoldsOne() {
        final QueryParameters parameters = QueryParameters.builder();

        final Map<String, String> query =
                parameters.repeated("owner[]", List.of("a")).build();

        assertThat(query).containsExactly(Map.entry("owner[]", "a"));
    }

    @Test
    void buildValues_shouldReturnAnUnmodifiableMap_whenCalled() {
        final Map<String, List<String>> query =
                QueryParameters.builder().text("a", "1").buildValues();

        final Throwable thrown = catchThrowable(() -> query.put("b", List.of("2")));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void buildValues_shouldReturnUnmodifiableLists_whenARepeatedListIsAdded() {
        final List<String> values = new ArrayList<>(List.of("a", "b"));
        final List<String> owners = QueryParameters.builder()
                .repeated("owner[]", values)
                .buildValues()
                .get("owner[]");

        final Throwable thrown = catchThrowable(() -> owners.add("c"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void buildValues_shouldKeepTheBuiltValues_whenTheGivenListChangesAfterwards() {
        final List<String> values = new ArrayList<>(List.of("a", "b"));
        final QueryParameters parameters = QueryParameters.builder().repeated("owner[]", values);
        values.add("c");

        final Map<String, List<String>> query = parameters.buildValues();

        assertThat(query).containsExactly(Map.entry("owner[]", List.of("a", "b")));
    }
}
