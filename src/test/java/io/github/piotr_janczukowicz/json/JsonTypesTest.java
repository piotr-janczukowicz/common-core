package io.github.piotr_janczukowicz.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonTypesTest {

    @Test
    void of_shouldEqualSameClass_whenCalledTwice() {
        final JsonType<String> first = JsonTypes.of(String.class);

        final JsonType<String> second = JsonTypes.of(String.class);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void of_shouldBuildParameterizedType_whenRawAndArgumentProvided() {
        final JsonType<List<String>> type = JsonTypes.of(List.class, String.class);

        final JsonType<?> raw = JsonTypes.of(List.class);

        assertSoftly(softly -> {
            softly.assertThat(type.toString()).contains("List").contains("String");
            softly.assertThat(type).isNotEqualTo(raw);
        });
    }

    @ParameterizedTest
    @ValueSource(classes = {String.class, Map.class})
    void of_shouldThrow_whenRawTypeDoesNotHaveExactlyOneTypeParameter(final Class<?> rawType) {
        final Throwable thrown = catchThrowable(() -> JsonTypes.of(rawType, Integer.class));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expected exactly 1");
    }

    @Test
    void listOf_shouldEqualJdkParameterizedType_whenSameRawAndArgument() throws Exception {
        final Type jdkType = Holder.class.getDeclaredField("names").getGenericType();

        final Type ours = JsonTypes.listOf(String.class).javaType();

        assertSoftly(softly -> {
            softly.assertThat(ours).isEqualTo(jdkType);
            softly.assertThat(jdkType).isEqualTo(ours);
            softly.assertThat(ours.hashCode()).isEqualTo(jdkType.hashCode());
        });
    }

    @Test
    void of_shouldThrow_whenRawTypeIsNull() {
        final Throwable thrown = catchThrowable(() -> JsonTypes.of(null, String.class));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("rawType");
    }

    @Test
    void of_shouldThrow_whenTypeArgumentIsNull() {
        final Throwable thrown = catchThrowable(() -> JsonTypes.of(List.class, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("typeArgument");
    }

    @Test
    void listOf_shouldThrow_whenElementTypeIsNull() {
        final Throwable thrown = catchThrowable(() -> JsonTypes.listOf(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("elementType");
    }

    @Test
    void mapOf_shouldThrow_whenValueTypeIsNull() {
        final Throwable thrown = catchThrowable(() -> JsonTypes.mapOf(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("valueType");
    }

    @Test
    void listOf_shouldEqualSameElementType_whenCalledTwice() {
        final JsonType<List<String>> first = JsonTypes.listOf(String.class);

        final JsonType<List<String>> second = JsonTypes.listOf(String.class);

        assertSoftly(softly -> {
            softly.assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
            softly.assertThat(first.javaType()).isEqualTo(second.javaType()).isEqualTo(first.javaType());
            softly.assertThat(first.javaType().hashCode())
                    .isEqualTo(second.javaType().hashCode());
        });
    }

    @Test
    void listOf_shouldNotEqual_whenOtherIsNullOrNotAType() {
        final JsonType<List<String>> type = JsonTypes.listOf(String.class);

        assertSoftly(softly -> {
            softly.assertThat(type).isEqualTo(type).isNotEqualTo(null).isNotEqualTo("list");
            softly.assertThat(type.javaType()).isNotEqualTo(String.class);
        });
    }

    @Test
    void listOf_shouldNotEqual_whenElementTypeDiffers() {
        final JsonType<List<String>> strings = JsonTypes.listOf(String.class);

        final JsonType<List<Integer>> integers = JsonTypes.listOf(Integer.class);

        assertThat(strings).isNotEqualTo(integers);
    }

    @Test
    void listOf_shouldNotEqual_whenMapTypeGiven() {
        final JsonType<List<String>> list = JsonTypes.listOf(String.class);

        final Type map = JsonTypes.mapOf(String.class).javaType();

        assertThat(list.javaType()).isNotEqualTo(map);
    }

    @Test
    void listOf_shouldNotEqual_whenOwnerTypeIsSet() {
        final JsonType<List<String>> list = JsonTypes.listOf(String.class);

        final Type owned = ownedListOfString();

        assertThat(list.javaType()).isNotEqualTo(owned);
    }

    @Test
    void listOf_shouldHaveNoOwnerType() {
        final JsonType<List<String>> list = JsonTypes.listOf(String.class);

        final Type owner = ((ParameterizedType) list.javaType()).getOwnerType();

        assertThat(owner).isNull();
    }

    @Test
    void javaType_shouldPrintTypeName_whenList() {
        final Type listType = JsonTypes.listOf(String.class).javaType();

        assertThat(listType).hasToString("java.util.List<java.lang.String>");
    }

    @Test
    void javaType_shouldPrintTypeName_whenMap() {
        final Type mapType = JsonTypes.mapOf(Integer.class).javaType();

        assertThat(mapType).hasToString("java.util.Map<java.lang.String, java.lang.Integer>");
    }

    @Test
    void mapOf_shouldDescribeStringKeys() {
        final JsonType<Map<String, Integer>> type = JsonTypes.mapOf(Integer.class);

        final String description = type.toString();

        assertSoftly(softly -> {
            softly.assertThat(description).contains("Map");
            softly.assertThat(description).contains("String");
            softly.assertThat(description).contains("Integer");
        });
    }

    @Test
    void of_shouldThrow_whenClassIsNull() {
        final Throwable thrown = catchThrowable(() -> JsonTypes.of(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
    }

    @Test
    void constructor_shouldRejectInstantiation_whenCalledViaReflection() throws Exception {
        final Constructor<JsonTypes> constructor = JsonTypes.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    private static ParameterizedType ownedListOfString() {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {String.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return Map.class;
            }
        };
    }

    private static final class Holder {
        @SuppressWarnings("unused")
        private List<String> names;
    }
}
