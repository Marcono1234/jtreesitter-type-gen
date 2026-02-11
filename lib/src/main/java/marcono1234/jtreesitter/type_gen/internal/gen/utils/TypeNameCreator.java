package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import marcono1234.jtreesitter.type_gen.CodeGenConfig;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Class for creating own to-be-generated Java type names.
 *
 * <p>This is separate from {@link CodeGenHelper} to allow creation of type names to already happen at an
 * earlier stage, before the actual code generation happens.
 */
public class TypeNameCreator {
    private final CodeGenConfig config;

    @Nullable // null when `Optional<T>` instead of `@Nullable T` should be generated
    private final AnnotationSpec nullableAnnotation;
    private final ClassName nonEmptyClassName;
    private final AnnotationSpec nonEmptyAnnotation;

    public TypeNameCreator(CodeGenConfig config) {
        this.config = config;

        nullableAnnotation = this.config.nullableAnnotationTypeName()
            .map(name -> AnnotationSpec.builder(CodeGenHelper.createClassName(name)).build())
            .orElse(null);
        nonEmptyClassName = createOwnClassName(this.config.nonEmptyTypeName());
        nonEmptyAnnotation = AnnotationSpec.builder(nonEmptyClassName).build();
    }

    /**
     * Creates a JavaPoet class name for the given name, in the generated package.
     */
    public ClassName createOwnClassName(String name, String... nestedNames) {
        return ClassName.get(config.packageName(), name, nestedNames);
    }

    /**
     * Creates a child class name for the given enclosing type.
     *
     * <p>Depending on the {@linkplain CodeGenConfig#childTypeAsTopLevel() config} and the {@code mustBeTopLevel}
     * result the child might actually be generated as top-level type being a sibling of the given enclosing type.
     */
    public ClassName createChildClassName(ClassName enclosingType, String childName, BooleanSupplier mustBeTopLevel) {
        if (enclosingType.enclosingClassName() != null) {
            throw new IllegalArgumentException("Not a top-level type: " + enclosingType);
        }

        boolean asTopLevel = switch (config.childTypeAsTopLevel()) {
            case NEVER -> false;
            case ALWAYS -> true;
            case AS_NEEDED -> mustBeTopLevel.getAsBoolean();
        };

        if (asTopLevel) {
            // Create top-level class name
            // TODO: Maybe don't use '$' here but instead for example '_'; '$' might confuse IDEs during debugging
            //   or when looking up source code, because they assume the name refers to a nested class
            return enclosingType.peerClass(enclosingType.simpleName() + "$" + childName);
        } else {
            return enclosingType.nestedClass(childName);
        }
    }

    /**
     * Gets the annotation representing {@code @Nullable}, or {@code null} if {@link Optional} should be used instead
     * of {@code @Nullable}.
     *
     * @see #getReturnOptionalType(TypeName)
     */
    public @Nullable AnnotationSpec getNullableAnnotation() {
        return nullableAnnotation;
    }

    /**
     * Creates a type name indicating that the type is optional. Depending on the config either by
     * wrapping the type in {@link Optional}, or by annotating it with {@code @Nullable},
     * see {@link #getNullableAnnotation()}.
     *
     * <p>Only intended for single optional types; not for {@code List<T>} or similar.
     */
    public TypeName getReturnOptionalType(TypeName type) {
        if (nullableAnnotation != null) {
            return type.annotated(nullableAnnotation);
        } else {
            return ParameterizedTypeName.get(ClassName.get(Optional.class), type);
        }
    }

    /**
     * Class name for the {@code @NonEmpty} annotation.
     */
    public ClassName getNonEmptyTypeName() {
        return nonEmptyClassName;
    }

    /**
     * Creates a new type name annotated with {@code @NonEmpty}.
     */
    public TypeName annotatedNonEmpty(TypeName type) {
        return type.annotated(nonEmptyAnnotation);
    }
}
