package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.internal.JavaNameValidator;

import javax.annotation.processing.Generated;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * General code generation config, not specific to a {@code node-types.json} file or a language.
 *
 * @param packageName
 *      Java package name the generated code should use; will create the corresponding subdirectories.
 *      in the output directory
 * @param nullableAnnotationTypeName
 *      Qualified name of the {@code @Nullable} annotation to use in the generated code, can for example
 *      be {@link TypeName#JSPECIFY_NULLABLE_ANNOTATION}.<br>
 *      If empty, no annotation is used and instead {@link Optional} is used in the generated code for
 *      nullable values.
 * @param nonEmptyTypeName
 *      Simple type name of an annotation type to be generated and used for methods which return non-empty
 *      collections. The annotation is mainly intended for documentation purposes. For example {@code NonEmpty}.
 * @param childTypeAsTopLevel
 *      Whether to generate Java types for node children as top-level classes.
 * @param nameGenerator
 *      Determines the names for fields, methods, classes, ... in the generated code.
 * @param generatedAnnotationConfig
 *      Configuration for {@code @Generated} annotations placed on generated classes; empty if such annotations
 *      should be not added.
 */
// Note: Internally this library uses `@Nullable`, but externally it uses `Optional`
public record CodeGenConfig(
    // TODO: Maybe should put package name as CodeGenerator#generate argument instead of config value since it is likely
    //   related to the `node-types.json`, e.g. the package name contains the language name
    String packageName,
    // TODO: For Nullable annotation maybe allow configuring whether to put on method, or on type
    Optional<TypeName> nullableAnnotationTypeName,
    // TODO: Maybe make 'non empty' annotation optional as well, in case users don't want it?
    String nonEmptyTypeName,
    ChildTypeAsTopLevel childTypeAsTopLevel,
    NameGenerator nameGenerator,
    Optional<GeneratedAnnotationConfig> generatedAnnotationConfig
) {
    public CodeGenConfig {
        JavaNameValidator.checkPackageName(packageName);
        Objects.requireNonNull(nullableAnnotationTypeName);
        JavaNameValidator.checkTypeName(nonEmptyTypeName, false);
        Objects.requireNonNull(childTypeAsTopLevel);
        nameGenerator = validatingNameGenerator(nameGenerator);
        Objects.requireNonNull(generatedAnnotationConfig);
    }

    /**
     * Whether to generate a top-level or a nested Java class for a node child type.
     */
    public enum ChildTypeAsTopLevel {
        /**
         * Never generate top-level Java classes for node child types, instead always generate nested Java classes for them.
         *
         * <p><b>Warning:</b> This can lead to "cyclic inheritance" compilation errors.
         */
        NEVER,
        /**
         * Always generate top-level Java classes for node child types.
         */
        ALWAYS,
        /**
         * Generate top-level Java classes for node child types as needed, to avoid "cyclic inheritance" compilation errors.
         * If not needed for a specific node child type, generate a nested Java class instead.
         */
        AS_NEEDED
    }

    /*
     * Note: `@Generated` is in the `javax.annotation.processing` package, which is actually for annotation processing,
     * but maybe it is fine nonetheless to use the annotation, even though code generation here is not doing annotation
     * processing
     */
    /**
     * Configuration for the {@code @Generated} annotation placed on the code.
     *
     * @param annotationType
     *      information about the annotation type, such as qualified type name and annotation element names
     * @param generationTime
     *      fixed time when the code was generated, useful for reproducible builds; if empty the current time is used
     * @param additionalInformation
     *      additional information to include in the {@code @Generated} annotation, can for example be the origin / version
     *      of the {@code node-types.json} file
     */
    public record GeneratedAnnotationConfig(GeneratedAnnotationType annotationType, Optional<Instant> generationTime, Optional<String> additionalInformation) {
        public GeneratedAnnotationConfig {
            Objects.requireNonNull(annotationType);
            Objects.requireNonNull(generationTime);
            Objects.requireNonNull(additionalInformation);
        }

        /**
         * Information about the annotation type to place on generated classes. {@link #JAVAX_GENERATED} can be used
         * in most cases, unless you want to specify a custom one.
         *
         * @param typeName
         *      qualified name of the annotation type, for example {@code com.example.Generated}
         * @param generatorElementName
         *      name of annotation type element of type {@code String} for storing the qualified name of the code
         *      generator; empty if not supported by this annotation type
         * @param dateElementName
         *      name of annotation type element of type {@code String} for storing the date when the code was
         *      generated; empty if not supported by this annotation type
         * @param commentsElementName
         *      name of annotation type element of type {@code String} for storing additional comments in the
         *      annotation; empty if not supported by this annotation type
         */
        // For use case of custom `@Generated`, see https://github.com/projectlombok/lombok/issues/1014
        public record GeneratedAnnotationType(TypeName typeName, Optional<String> generatorElementName, Optional<String> dateElementName, Optional<String> commentsElementName) {
            public GeneratedAnnotationType {
                Objects.requireNonNull(typeName);
                Objects.requireNonNull(generatorElementName);
                Objects.requireNonNull(dateElementName);
                Objects.requireNonNull(commentsElementName);
            }

            /**
             * Type {@link Generated javax.annotation.processing.Generated}
             */
            public static final GeneratedAnnotationType JAVAX_GENERATED = new GeneratedAnnotationType(TypeName.fromClass(Generated.class), Optional.of("value"), Optional.of("date"), Optional.of("comments"));
        }
    }

    private NameGenerator validatingNameGenerator(NameGenerator nameGenerator) {
        Objects.requireNonNull(nameGenerator);
        return new NameGenerator() {
            private String validateTypeName(String name) {
                JavaNameValidator.checkTypeName(name, false);
                return name;
            }

            private String validateMemberName(String name) {
                JavaNameValidator.checkMemberName(name);
                return name;
            }

            public String generateJavaTypeName(String typeName) {
                return validateTypeName(nameGenerator.generateJavaTypeName(typeName));
            }

            public String generateTypeNameConstant(String typeName) {
                return validateMemberName(nameGenerator.generateTypeNameConstant(typeName));
            }

            public String generateTypeIdConstant(String typeName) {
                return validateMemberName(nameGenerator.generateTypeIdConstant(typeName));
            }

            public String generateChildrenTypesName(String parentTypeName, List<String> childrenTypeNames) {
                return validateTypeName(nameGenerator.generateChildrenTypesName(parentTypeName, childrenTypeNames));
            }

            public String generateChildrenTokenTypeName(String parentTypeName, List<String> tokenChildrenTypeNames) {
                return validateTypeName(nameGenerator.generateChildrenTokenTypeName(parentTypeName, tokenChildrenTypeNames));
            }

            public String generateChildrenTokenName(String parentTypeName, String tokenType, int index) {
                return validateMemberName(nameGenerator.generateChildrenTokenName(parentTypeName, tokenType, index));
            }

            public String generateChildrenGetterName(String parentTypeName, List<String> childrenTypeNames, boolean multiple, boolean required) {
                return validateMemberName(nameGenerator.generateChildrenGetterName(parentTypeName, childrenTypeNames, multiple, required));
            }

            public String generateFieldNameConstant(String parentTypeName, String fieldName) {
                return validateMemberName(nameGenerator.generateFieldNameConstant(parentTypeName, fieldName));
            }

            public String generateFieldIdConstant(String parentTypeName, String fieldName) {
                return validateMemberName(nameGenerator.generateFieldIdConstant(parentTypeName, fieldName));
            }

            public String generateFieldTypesName(String parentTypeName, String fieldName, List<String> fieldTypesNames) {
                return validateTypeName(nameGenerator.generateFieldTypesName(parentTypeName, fieldName, fieldTypesNames));
            }

            public String generateFieldTokenTypeName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames) {
                return validateTypeName(nameGenerator.generateFieldTokenTypeName(parentTypeName, fieldName, tokenFieldTypesNames));
            }

            public String generateFieldTokenName(String parentTypeName, String fieldName, String tokenType, int index) {
                return validateMemberName(nameGenerator.generateFieldTokenName(parentTypeName, fieldName, tokenType, index));
            }

            public String generateFieldGetterName(String parentTypeName, String fieldName, List<String> fieldTypesNames, boolean multiple, boolean required) {
                return validateMemberName(nameGenerator.generateFieldGetterName(parentTypeName, fieldName, fieldTypesNames, multiple, required));
            }

            public Optional<String> generateNonNamedChildrenGetter(String parentTypeName, boolean hasNamedChildren, boolean hasFields) {
                return nameGenerator.generateNonNamedChildrenGetter(parentTypeName, hasNamedChildren, hasFields)
                    .map(this::validateMemberName);
            }
        };
    }
}
