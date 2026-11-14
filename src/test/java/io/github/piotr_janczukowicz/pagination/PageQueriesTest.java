package io.github.piotr_janczukowicz.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PageQueriesTest {

    private static final PaginationConfiguration CONFIGURATION =
            PaginationConfiguration.builder().perPage(50).build();

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        final Constructor<PageQueries> constructor = PageQueries.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void boundPerPage_shouldReturnExplicitSize_whenWithinTheMaximum() {
        final int perPage = PageQueries.boundPerPage(30, CONFIGURATION, 100);

        assertThat(perPage).isEqualTo(30);
    }

    @Test
    void boundPerPage_shouldReturnConfiguredSize_whenNoneIsExplicit() {
        final int perPage = PageQueries.boundPerPage(null, CONFIGURATION, 100);

        assertThat(perPage).isEqualTo(50);
    }

    @Test
    void boundPerPage_shouldReturnTheMaximum_whenConfiguredSizeIsAboveIt() {
        final int perPage = PageQueries.boundPerPage(null, CONFIGURATION, 20);

        assertThat(perPage).isEqualTo(20);
    }

    @Test
    void boundPerPage_shouldThrow_whenExplicitSizeIsAboveTheMaximum() {
        final Throwable thrown = catchThrowable(() -> PageQueries.boundPerPage(101, CONFIGURATION, 100));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("perPage must be at most 100");
    }

    @Test
    void boundPerPage_shouldThrowNullPointerException_whenConfigurationIsNull() {
        final Throwable thrown = catchThrowable(() -> PageQueries.boundPerPage(null, null, 100));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void withParameters_shouldKeepBaseOrderAndReplaceSameName_whenParametersAreGiven() {
        final Map<String, List<String>> base = new LinkedHashMap<>();
        base.put("state", List.of("open"));
        base.put("page", List.of("1"));
        final Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("page", "2");
        parameters.put("per_page", "30");

        final Map<String, List<String>> query = PageQueries.withParameters(base, parameters);

        assertSoftly(softly -> {
            softly.assertThat(query.keySet()).containsExactly("state", "page", "per_page");
            softly.assertThat(query.get("page")).containsExactly("2");
            softly.assertThat(query.get("per_page")).containsExactly("30");
            softly.assertThat(base.get("page")).containsExactly("1");
            softly.assertThat(catchThrowable(() -> query.put("x", List.of())))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void withParameters_shouldThrowNullPointerException_whenParametersIsNull() {
        final Throwable thrown = catchThrowable(() -> PageQueries.withParameters(Map.of(), null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
