package io.github.piotr_janczukowicz.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a presence union: an object union whose branch is picked by the names the object has. The Jackson
 * {@code ValueUnionModule} reads it in one pass over the object's top-level names, in document order:
 *
 * <ul>
 *   <li>the first name that is one of the {@link #keys()} picks that key's branch, which then reads the whole object (a
 *       branch that cannot read it refuses the value; the next rule is not tried);
 *   <li>with no such name, the {@link #fallback()} when the object has every name of {@link #fallbackRequires()};
 *   <li>else the {@link #unknown()} branch, which keeps the object as it came; without it the value is refused.
 * </ul>
 *
 * <p>A name counts when it is present, also with the value {@code null}. A value that is not an object is refused, and
 * a JSON {@code null} never reaches the union: Jackson reads it as {@code null}. Names the keys do not list (properties
 * several branches share) never decide. The lookup is by the annotated class only; this annotation is not inherited, so
 * a branch that implements the union reads as itself. Writing needs nothing: each branch writes itself.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PresenceUnion {

    /**
     * The names that each pick one branch: every property only that branch declares.
     *
     * @return the keys, each name at most once
     */
    Key[] keys();

    /**
     * The branch of an object that has none of the {@link #keys()} names but every name of {@link #fallbackRequires()}.
     *
     * @return the branch class, or {@link Void} for none
     */
    Class<?> fallback() default Void.class;

    /**
     * The names an object must have to be read as the {@link #fallback()}.
     *
     * @return the names, none by default
     */
    String[] fallbackRequires() default {};

    /**
     * The branch of an object no other rule picks, which keeps the object as it came.
     *
     * @return the branch class, or {@link Void} to refuse such an object
     */
    Class<?> unknown() default Void.class;

    /** One name that picks one branch. */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Key {

        /**
         * The JSON name of the property.
         *
         * @return the name
         */
        String name();

        /**
         * The branch an object with this name is read as.
         *
         * @return the branch class
         */
        Class<?> type();
    }
}
