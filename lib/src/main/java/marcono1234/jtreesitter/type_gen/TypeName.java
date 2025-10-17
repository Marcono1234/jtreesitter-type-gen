package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.internal.JavaNameValidator;
import org.jspecify.annotations.Nullable;

/**
 * A Java type name, consisting of <i>package name</i> and <i>simple name</i>.
 *
 * @param name simple name of the type; for nested classes the names are separated by {@code $}
 */
public record TypeName(String packageName, String name) {

    /**
     * Type name of the {@code org.jspecify.annotations.Nullable} annotation.
     */
    // Exists as constant here because jtreesitter uses this annotation, so users can benefit from it transitively
    public static final TypeName JSPECIFY_NULLABLE_ANNOTATION = TypeName.fromClass(Nullable.class);

    public TypeName {
        JavaNameValidator.checkPackageName(packageName);
        JavaNameValidator.checkTypeName(name, false);
    }

    /**
     * Creates a type name from a qualified name. For example {@code org.example.MyClass$Nested}.
     */
    public static TypeName fromQualifiedName(String name) {
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("Missing package name for: " + name);
        }
        return new TypeName(name.substring(0, lastDotIndex), name.substring(lastDotIndex + 1));
    }

    /**
     * Creates a type name from a class object.
     */
    public static TypeName fromClass(Class<?> c) {
        return fromQualifiedName(c.getName());
    }
}
