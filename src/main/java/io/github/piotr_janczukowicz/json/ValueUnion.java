package io.github.piotr_janczukowicz.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a value union: a type whose JSON value is one of several JSON token kinds, each read into its own branch class.
 * The Jackson {@code ValueUnionModule} reads it: it looks at the first token of the value and binds the value to the
 * class named for that token, which reads itself (a record with a delegating creator, a plain record, or an object
 * union).
 *
 * <ul>
 *   <li>a JSON string goes to {@link #string()};
 *   <li>a JSON integer goes to {@link #integer()} when it fits a {@code long} and the class is given, else to
 *       {@link #number()};
 *   <li>any other JSON number goes to {@link #number()}, never to {@link #integer()};
 *   <li>a JSON boolean goes to {@link #flag()};
 *   <li>a JSON array goes to {@link #array()}: when that class is created from a collection of strings, every item must
 *       be a JSON string;
 *   <li>a JSON object goes to {@link #object()};
 *   <li>a token whose class is not given goes to {@link #unknown()}, which keeps the value as it came; without it the
 *       value is refused.
 * </ul>
 *
 * <p>A JSON {@code null} never reaches the union: Jackson reads it as {@code null}. The lookup is by the annotated
 * class only; this annotation is not inherited, so a branch that implements the union reads as itself. {@link Void}
 * (the default) means the union has no such branch.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ValueUnion {

    /**
     * The branch of a JSON string.
     *
     * @return the branch class, or {@link Void} for none
     */
    Class<?> string() default Void.class;

    /**
     * The branch of a JSON integer that fits a {@code long}.
     *
     * @return the branch class, or {@link Void} for none
     */
    Class<?> integer() default Void.class;

    /**
     * The branch of a JSON number that has a fraction or an exponent, and of an integer the integer branch does not
     * take.
     *
     * @return the branch class, or {@link Void} for none
     */
    Class<?> number() default Void.class;

    /**
     * The branch of a JSON boolean.
     *
     * @return the branch class, or {@link Void} for none
     */
    Class<?> flag() default Void.class;

    /**
     * The branch of a JSON array.
     *
     * @return the branch class, or {@link Void} for none
     */
    Class<?> array() default Void.class;

    /**
     * The branch of a JSON object.
     *
     * @return the branch class, or {@link Void} for none
     */
    Class<?> object() default Void.class;

    /**
     * The branch of a token no other branch takes, which keeps the value as it came.
     *
     * @return the branch class, or {@link Void} to refuse such a token
     */
    Class<?> unknown() default Void.class;
}
