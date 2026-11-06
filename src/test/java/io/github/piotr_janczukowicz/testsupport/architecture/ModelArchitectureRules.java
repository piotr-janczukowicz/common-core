package io.github.piotr_janczukowicz.testsupport.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Rules for the model types (records, sealed unions, enums) that every module generated from an API description shares.
 */
public final class ModelArchitectureRules {

    private static final String UNRECOGNIZED = "UNRECOGNIZED";
    private static final String UNRECOGNIZED_WIRE = "unrecognized";

    private ModelArchitectureRules() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * A type annotated with {@code @JsonSubTypes} is a sealed union that registers exactly its permitted subtypes. The
     * {@code defaultImpl} of its {@code @JsonTypeInfo} counts as registered, and a permitted subtype whose name
     * contains {@code Unknown} may be built by a custom deserializer instead.
     */
    public static ArchRule jsonUnionsRegisterTheirPermittedSubtypes(final String packageName) {
        return classes()
                .that()
                .resideInAPackage(packageName + "..")
                .and()
                .areAnnotatedWith(JsonSubTypes.class)
                .should(new ArchCondition<>("register exactly the permitted subtypes of a sealed union") {
                    @Override
                    public void check(final JavaClass clazz, final ConditionEvents events) {
                        checkUnion(clazz, events);
                    }
                });
    }

    /**
     * The constant {@code UNRECOGNIZED} and the wire value {@code unrecognized} belong together: neither stands for
     * anything else.
     */
    public static ArchRule enumFallbackPairsWithItsWireValue(final String packageName) {
        return classes()
                .that()
                .resideInAPackage(packageName + "..")
                .and()
                .areEnums()
                .should(new ArchCondition<>("name the fallback UNRECOGNIZED with the wire value unrecognized") {
                    @Override
                    public void check(final JavaClass clazz, final ConditionEvents events) {
                        checkEnum(clazz, events);
                    }
                });
    }

    /**
     * Production code has no class that looks like a test: a name ending in {@code Test} or starting with {@code Test}.
     */
    public static ArchRule productionHasNoTestClasses() {
        return noClasses()
                .should()
                .haveSimpleNameEndingWith("Test")
                .orShould()
                .haveSimpleNameStartingWith("Test")
                .orShould()
                .resideInAPackage("..test..");
    }

    /** The wire value of an enum constant: its {@code wireValue()} when it has one, else its name. */
    public static String wireValue(final Class<?> type, final Object constant) {
        try {
            return String.valueOf(type.getMethod("wireValue").invoke(constant));
        } catch (NoSuchMethodException e) {
            return ((Enum<?>) constant).name();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName() + " has no readable wireValue()", e);
        }
    }

    private static void checkUnion(final JavaClass clazz, final ConditionEvents events) {
        final Class<?> union = clazz.reflect();
        if (!union.isSealed()) {
            events.add(SimpleConditionEvent.violated(clazz, union.getName() + " is not sealed"));
            return;
        }
        final Set<Class<?>> registered = Arrays.stream(
                        union.getAnnotation(JsonSubTypes.class).value())
                .map(JsonSubTypes.Type::value)
                .collect(Collectors.toSet());
        final Set<Class<?>> permitted = Set.of(union.getPermittedSubclasses());
        final JsonTypeInfo info = union.getAnnotation(JsonTypeInfo.class);
        final Class<?> fallback =
                info == null || info.defaultImpl() == JsonTypeInfo.class || info.defaultImpl() == Void.class
                        ? null
                        : info.defaultImpl();
        final Set<Class<?>> expected = new HashSet<>(registered);
        if (fallback != null) {
            expected.add(fallback);
        }
        final Set<Class<?>> unregistered = sortedByName(permitted);
        unregistered.removeAll(expected);
        unregistered.removeIf(type -> type.getSimpleName().contains("Unknown"));
        final Set<Class<?>> unpermitted = sortedByName(expected);
        unpermitted.removeAll(permitted);
        if (!unregistered.isEmpty()) {
            events.add(SimpleConditionEvent.violated(
                    clazz, union.getName() + " permits but does not register " + unregistered));
        }
        if (!unpermitted.isEmpty()) {
            events.add(SimpleConditionEvent.violated(
                    clazz, union.getName() + " registers or defaults to a type it does not permit " + unpermitted));
        }
    }

    private static void checkEnum(final JavaClass clazz, final ConditionEvents events) {
        final Class<?> type = clazz.reflect();
        for (final Object constant : type.getEnumConstants()) {
            final String name = ((Enum<?>) constant).name();
            final String wire = wireValue(type, constant);
            if (UNRECOGNIZED.equals(name) && !UNRECOGNIZED_WIRE.equals(wire)) {
                events.add(SimpleConditionEvent.violated(
                        clazz, clazz.getName() + ".UNRECOGNIZED must have the wire value unrecognized"));
            }
            if (UNRECOGNIZED_WIRE.equals(wire) && !UNRECOGNIZED.equals(name)) {
                events.add(SimpleConditionEvent.violated(
                        clazz,
                        clazz.getName() + "." + name
                                + " has the wire value unrecognized, which belongs to the constant UNRECOGNIZED"));
            }
        }
    }

    private static Set<Class<?>> sortedByName(final Set<Class<?>> types) {
        final Set<Class<?>> sorted = new TreeSet<>(Comparator.comparing(Class::getName));
        sorted.addAll(types);
        return sorted;
    }
}
