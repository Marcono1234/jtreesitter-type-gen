package marcono1234.jtreesitter.type_gen;

import java.util.List;
import java.util.Objects;

/**
 * A Java type variable to be emitted in the generated code.
 *
 * @param name
 *      name of the type variable
 * @param bounds
 *      bounds of the type variable, can be empty
 */
public record JavaTypeVariable(String name, List<JavaType> bounds) {
    public JavaTypeVariable {
        Objects.requireNonNull(name);
        Objects.requireNonNull(bounds);
    }
}
