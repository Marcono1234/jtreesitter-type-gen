package marcono1234.jtreesitter.type_gen;

import com.palantir.javapoet.TypeName;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.JavaTypeImpl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a Java type, used for code generation.
 *
 * <p>Unlike {@link marcono1234.jtreesitter.type_gen.TypeName} the type can also be a primitive type,
 * an array, a parameterized type, ....
 */
sealed public interface JavaType permits JavaTypeImpl {
    /**
     * Obtains a {@code JavaType} from Java reflection type, for example {@code String.class}.
     */
    static JavaType fromType(Type type) {
        Objects.requireNonNull(type);
        return new JavaTypeImpl(TypeName.get(type));
    }

    /**
     * Obtains a {@code JavaType} from a string representation.
     *
     * <p>The string representation matches closely how types are written in Java source code, but there are
     * some differences and limitations:
     * <ul>
     * <li>type names must be fully qualified, e.g. {@code java.lang.String} instead of just {@code String}<br>
     * (if the name is not qualified, it might be erroneously considered a type variable, and it can lead to missing
     * import statements in the generated code)
     * <li>nested type names are separated by a {@code '$'} from the enclosing name, e.g. {@code java.util.Map$Entry}
     * <li>type variables must start with an upper case letter
     * <li>annotations do not support element values, e.g. {@code @MyAnnotation(value = 1)} is not supported
     * <li>annotations must be placed in front of the package name, e.g. {@code @com.example.Nullable java.lang.String}<br>
     * (this differs from Java source code where they must be placed <i>after</i> the package name, e.g. {@code java.lang.@c.e.Nullable String})
     * <li>{@code void} is not allowed, instead the API consuming a {@code JavaType} expects an empty {@link Optional} for that
     * <li>whitespace parsing is strict, if a space is expected then it must be present and it must be exactly one space
     * </ul>
     *
     * <h4>Examples</h4>
     * <ul>
     * <li>{@code int}
     * <li>{@code java.lang.String}
     * <li>{@code java.util.Map$Entry}
     * <li>{@code java.util.Map<java.lang.Integer, @com.example.NonNull java.lang.String>}
     * <li>{@code java.util.List<? extends T>}
     * <li>{@code @example.A int[] @example.B []}
     * </ul>
     */
    static JavaType fromTypeString(String typeString) {
        Objects.requireNonNull(typeString);
        return JavaTypeImpl.fromTypeString(typeString);
    }
    
    // TODO: Should this also allow users to directly provide a JavaPoet `TypeName`? That would be quite convenient for
    //   users because it makes building types, especially annotated, parameterized and wildcard types easy
    //   However, currently it is an implementation details that JavaPoet is used for code generation
    //   Could maybe expose this as 'experimental' method? Though users could also use the string representation
}
