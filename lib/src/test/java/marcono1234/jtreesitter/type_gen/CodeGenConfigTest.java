package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.CodeGenConfig.GeneratedAnnotationConfig;
import marcono1234.jtreesitter.type_gen.NameGenerator.TokenNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenConfigTest {
    private static final String DEFAULT_PACKAGE_NAME = "org.example";
    private static final String DEFAULT_NON_EMPTY_NAME = "NonEmpty";
    private static final CodeGenConfig.ChildTypeAsTopLevel DEFAULT_CHILD_AS_TOP_LEVEL = CodeGenConfig.ChildTypeAsTopLevel.AS_NEEDED;
    private static final NameGenerator DEFAULT_NAME_GENERATOR = new NameGenerator.DefaultNameGenerator(TokenNameGenerator.AUTOMATIC);
    private static final boolean DEFAULT_GENERATE_FIND_NODES_METHODS = true;

    @Test
    void packageName() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            "-invalid",
            Optional.empty(),
            Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Not a valid package name: -invalid", e.getMessage());
    }

    @Test
    void nullMarkedPackageAnnotationTypeName() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.of(TypeName.JSPECIFY_NULLMARKED_ANNOTATION),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Cannot use null-marked annotation when nullable annotation is not specified", e.getMessage());
    }

    @Test
    void nonEmptyTypeName() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            "-invalid",
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Not a valid type name: -invalid", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            // Should also fail for qualified name because unqualified name is expected
            "qualified.Name",
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Non-qualified type name must not contain '.': qualified.Name", e.getMessage());
    }

    @Nested
    class GeneratedAnnotationConfigTest {
        @Test
        void new_Valid() {
            assertDoesNotThrow(() -> {
                new GeneratedAnnotationConfig(GeneratedAnnotationConfig.GeneratedAnnotationType.JAVAX_GENERATED, Optional.empty(), Optional.empty());
                new GeneratedAnnotationConfig(GeneratedAnnotationConfig.GeneratedAnnotationType.JAVAX_GENERATED, Optional.of(Instant.now()), Optional.of("some text"));

                new GeneratedAnnotationConfig(
                    new GeneratedAnnotationConfig.GeneratedAnnotationType(
                        new TypeName("example", "Ann"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                    ),
                    Optional.empty(),
                    Optional.empty()
                );

                // With time
                new GeneratedAnnotationConfig(
                    new GeneratedAnnotationConfig.GeneratedAnnotationType(
                        new TypeName("example", "Ann"),
                        Optional.empty(),
                        Optional.of("time"),
                        Optional.empty()
                    ),
                    Optional.of(Instant.now()),
                    Optional.empty()
                );

                // With comment
                new GeneratedAnnotationConfig(
                    new GeneratedAnnotationConfig.GeneratedAnnotationType(
                        new TypeName("example", "Ann"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("comment")
                    ),
                    Optional.empty(),
                    Optional.of("some text")
                );
            });
        }

        @Test
        void new_Invalid() {
            // Without time element
            var e = assertThrows(IllegalArgumentException.class, () -> new GeneratedAnnotationConfig(
                new GeneratedAnnotationConfig.GeneratedAnnotationType(
                    new TypeName("example", "Ann"),
                    Optional.of("generator"),
                    Optional.empty(),
                    Optional.of("comment")
                ),
                Optional.of(Instant.now()),
                Optional.of("some text")
            ));
            assertEquals("Cannot specify generationTime when annotation type has no date element", e.getMessage());

            // Without comment element
            e = assertThrows(IllegalArgumentException.class, () -> new GeneratedAnnotationConfig(
                new GeneratedAnnotationConfig.GeneratedAnnotationType(
                    new TypeName("example", "Ann"),
                    Optional.of("generator"),
                    Optional.of("time"),
                    Optional.empty()
                ),
                Optional.of(Instant.now()),
                Optional.of("some text")
            ));
            assertEquals("Cannot specify additionalInformation when annotation type has no comments element", e.getMessage());
        }
    }

    @Nested
    class BuilderTest {
        @Test
        void defaults() {
            var config = CodeGenConfig.builder(DEFAULT_PACKAGE_NAME).build();
            assertEquals(DEFAULT_PACKAGE_NAME, config.packageName());
            assertEquals(Optional.of(TypeName.JSPECIFY_NULLABLE_ANNOTATION), config.nullableAnnotationTypeName());
            assertEquals(Optional.of(TypeName.JSPECIFY_NULLMARKED_ANNOTATION), config.nullMarkedPackageAnnotationTypeName());
            assertEquals("NonEmpty", config.nonEmptyTypeName());
            assertEquals(CodeGenConfig.ChildTypeAsTopLevel.AS_NEEDED, config.childTypeAsTopLevel());
            assertEquals(Optional.empty(), config.typedNodeSuperinterface());
            assertEquals(CodeGenConfig.validatingNameGenerator(CodeGenConfig.Builder.DEFAULT_NAME_GENERATOR), config.nameGenerator());
            assertTrue(config.generateFindNodesMethods());
            assertEquals(Optional.empty(), config.typedQueryNameGenerator());
            assertEquals(Optional.empty(), config.customMethodsProvider());

            var defaultGeneratedAnnotation = new GeneratedAnnotationConfig(GeneratedAnnotationConfig.GeneratedAnnotationType.JAVAX_GENERATED, Optional.empty(), Optional.empty());
            assertEquals(Optional.of(defaultGeneratedAnnotation), config.generatedAnnotationConfig());
        }

        @Test
        void custom() {
            var packageName = "com.mypackage";
            var nonEmptyTypeName = "MyNonEmpty";
            var childTypeAsTopLevel = CodeGenConfig.ChildTypeAsTopLevel.ALWAYS;
            var typedNodeSuperinterface = new TypeName("example", "Super");
            var nameGenerator = new NameGenerator.DefaultNameGenerator(TokenNameGenerator.AUTOMATIC) {
                @Override
                public boolean equals(Object obj) {
                    // Explicitly check for reference equality, to make sure assertion below only passes if this custom name generator is used
                    return this == obj;
                }
            };
            var generateFindNodesMethods = false;
            var typedQueryNameGenerator = TypedQueryNameGenerator.createDefault(nameGenerator);
            var customMethodsProvider = new CustomMethodsProvider() { };
            var generatedAnnotationConfig = new GeneratedAnnotationConfig(GeneratedAnnotationConfig.GeneratedAnnotationType.JAVAX_GENERATED, Optional.of(Instant.EPOCH), Optional.of("some text"));

            var config = CodeGenConfig.builder(packageName)
                .usingOptional()
                .nonEmptyTypeName(nonEmptyTypeName)
                .childTypeAsTopLevel(childTypeAsTopLevel)
                .typedNodeSuperinterface(typedNodeSuperinterface)
                .nameGenerator(nameGenerator)
                .generateFindNodesMethods(generateFindNodesMethods)
                .typedQueryNameGenerator(typedQueryNameGenerator)
                .customMethodsProvider(customMethodsProvider)
                .generatedAnnotationConfig(generatedAnnotationConfig)
                .build();

            assertEquals(packageName, config.packageName());
            assertEquals(Optional.empty(), config.nullableAnnotationTypeName());
            assertEquals(Optional.empty(), config.nullMarkedPackageAnnotationTypeName());
            assertEquals(nonEmptyTypeName, config.nonEmptyTypeName());
            assertEquals(childTypeAsTopLevel, config.childTypeAsTopLevel());
            assertEquals(Optional.of(typedNodeSuperinterface), config.typedNodeSuperinterface());
            assertEquals(CodeGenConfig.validatingNameGenerator(nameGenerator), config.nameGenerator());
            assertEquals(generateFindNodesMethods, config.generateFindNodesMethods());
            assertEquals(CodeGenConfig.validatingTypedQueryNameGenerator(typedQueryNameGenerator), config.typedQueryNameGenerator().orElseThrow());
            assertSame(customMethodsProvider, config.customMethodsProvider().orElseThrow());
            assertEquals(Optional.of(generatedAnnotationConfig), config.generatedAnnotationConfig());
        }

        @Test
        void custom_NullableAnnotation() {
            var config = CodeGenConfig.builder(DEFAULT_PACKAGE_NAME)
                .usingOptional()
                .build();
            assertEquals(Optional.empty(), config.nullableAnnotationTypeName());
            assertEquals(Optional.empty(), config.nullMarkedPackageAnnotationTypeName());

            var nullableTypeName = new TypeName("example", "MyNullable");
            config = CodeGenConfig.builder(DEFAULT_PACKAGE_NAME)
                .usingNullable(nullableTypeName)
                .build();
            assertEquals(Optional.of(nullableTypeName), config.nullableAnnotationTypeName());
            assertEquals(Optional.empty(), config.nullMarkedPackageAnnotationTypeName());

            var nullMarkedTypeName = new TypeName("example", "MyNullMarked");
            config = CodeGenConfig.builder(DEFAULT_PACKAGE_NAME)
                .usingNullable(nullableTypeName, nullMarkedTypeName)
                .build();
            assertEquals(Optional.of(nullableTypeName), config.nullableAnnotationTypeName());
            assertEquals(Optional.of(nullMarkedTypeName), config.nullMarkedPackageAnnotationTypeName());
        }

        @Test
        void custom_NoGeneratedAnnotation() {
            var config = CodeGenConfig.builder(DEFAULT_PACKAGE_NAME)
                .build();
            assertTrue(config.generatedAnnotationConfig().isPresent());

            config = CodeGenConfig.builder(DEFAULT_PACKAGE_NAME)
                .withoutGeneratedAnnotation()
                .build();
            assertEquals(Optional.empty(), config.generatedAnnotationConfig());
        }

        @Test
        void apply() {
            var nonEmptyTypeName = "MyNonEmpty";
            var config = CodeGenConfig.builder(DEFAULT_PACKAGE_NAME)
                .apply(b -> b.nonEmptyTypeName(nonEmptyTypeName))
                .build();
            assertEquals(nonEmptyTypeName, config.nonEmptyTypeName());
        }
    }
}
