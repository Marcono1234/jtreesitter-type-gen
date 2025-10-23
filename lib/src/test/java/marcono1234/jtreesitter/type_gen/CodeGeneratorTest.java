package marcono1234.jtreesitter.type_gen;

import com.palantir.javapoet.JavaFile;
import io.github.ascopes.jct.compilers.JctCompilers;
import io.github.ascopes.jct.workspaces.PathStrategy;
import io.github.ascopes.jct.workspaces.Workspaces;
import marcono1234.jtreesitter.type_gen.CodeGenConfig.GeneratedAnnotationConfig.GeneratedAnnotationType;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageProviderConfig;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.github.ascopes.jct.assertions.JctAssertions.assertThatCompilation;
import static org.junit.jupiter.api.Assertions.*;

class CodeGeneratorTest {
    private static final boolean UPDATE_EXPECTED = System.getProperty("test-update-expected") != null;
    private static final String JSON_EXTENSION = ".json";
    private static final String EXPECTED_JAVA_EXTENSION = ".expected.java";

    // Use fixed version to avoid test output changes for every version change / Git commit
    private static final CodeGenerator.Version VERSION_INFO = new CodeGenerator.Version(
        "0.0.0",
        "https://github.com/Marcono1234/jtreesitter-type-gen",
        "0".repeat(40),
        "0.0.0"
    );
    private static final CodeGenConfig.GeneratedAnnotationConfig GENERATED_ANNOTATION_CONFIG = new CodeGenConfig.GeneratedAnnotationConfig(
        GeneratedAnnotationType.JAVAX_GENERATED,
        // Use fixed time for deterministic generated code
        Optional.of(Instant.EPOCH),
        Optional.of("custom comment")
    );

    static Stream<Arguments> nodeTypesFiles() throws IOException {
        Path resourcesDir = Path.of("src/test/resources/code-gen");
        try (var files = Files.list(resourcesDir)) {
            for (var file : (Iterable<Path>) files::iterator) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(JSON_EXTENSION) && !fileName.endsWith(EXPECTED_JAVA_EXTENSION)) {
                    fail("Unsupported file: " + file);
                }

                if (fileName.endsWith(EXPECTED_JAVA_EXTENSION)) {
                    String baseName = fileName.substring(0, fileName.length() - EXPECTED_JAVA_EXTENSION.length());
                    if (!Files.exists(file.resolveSibling(baseName + JSON_EXTENSION))) {
                        fail("Missing '%s' file for: %s".formatted(JSON_EXTENSION, file));
                    }
                }
            }
        }

        List<Arguments> arguments;
        try (var files = Files.list(resourcesDir)) {
            // Collect to List first because underlying `Files#list` stream will be closed in this `try` already
            arguments = files
                .filter(f -> f.getFileName().toString().endsWith(JSON_EXTENSION))
                .map(f -> Arguments.of(f.getFileName().toString(), f))
                .toList();
        }

        if (arguments.isEmpty()) {
            throw new IllegalStateException("Did not find test resources in: " + resourcesDir);
        }
        return arguments.stream();
    }

    @AfterAll
    static void checkUpdatedExpected() {
        // Fail if expected was updated, to avoid accidentally always updating expected, and tests always 'succeeding'
        if (UPDATE_EXPECTED) {
            fail("Updated expected output");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nodeTypesFiles")
    void testGeneration(String fileName, Path nodeTypesFile) throws Exception {
        if (!fileName.endsWith(JSON_EXTENSION)) {
            fail("Bad file name: " + nodeTypesFile);
        }

        String baseFileName = fileName.substring(0, fileName.length() - JSON_EXTENSION.length());
        Path expectedFile = nodeTypesFile.resolveSibling(baseFileName + EXPECTED_JAVA_EXTENSION);

        String packageName = baseFileName.contains("(default-package)") ? "" : "org.example";
        boolean nullableAsOptional = baseFileName.contains("(nullable=Optional)");

        var childAsTopLevel = baseFileName.contains("(child-top-level)") ? CodeGenConfig.ChildTypeAsTopLevel.ALWAYS
            : CodeGenConfig.ChildTypeAsTopLevel.AS_NEEDED;

        var config = new CodeGenConfig(
            packageName,
            nullableAsOptional ? Optional.empty() : Optional.of(TypeName.JSPECIFY_NULLABLE_ANNOTATION),
            "NonEmpty",
            childAsTopLevel,
            NameGenerator.DEFAULT,
            Optional.of(GENERATED_ANNOTATION_CONFIG)
        );

        String rootNode = null;
        var rootNodeMatcher = Pattern.compile("\\(root=(.+?)\\)").matcher(baseFileName);
        if (rootNodeMatcher.find()) {
            rootNode = rootNodeMatcher.group(1);
        }

        Map<String, String> fallbackNodeTypeMapping = Map.of();
        var nodeTypeMappingMatcher = Pattern.compile("\\(type-mapping=(.+?)\\)").matcher(baseFileName);
        if (nodeTypeMappingMatcher.find()) {
            String[] mapping = nodeTypeMappingMatcher.group(1).split("=", -1);
            assertEquals(2, mapping.length);
            fallbackNodeTypeMapping = Map.of(mapping[0], mapping[1]);
        }

        LanguageProviderConfig languageProvider = null;
        var languageProviderMatcher = Pattern.compile("\\(lang-provider=(.+?(\\(\\))?)\\)").matcher(baseFileName);
        if (languageProviderMatcher.find()) {
            String memberName = languageProviderMatcher.group(1);
            languageProvider = LanguageProviderConfig.fromString(DUMMY_LANGUAGE_PROVIDER_NAME + "#" + memberName);
        }

        LanguageVersion languageVersion = null;
        var languageVersionMatcher = Pattern.compile("\\(lang-version=(.+?)\\)").matcher(baseFileName);
        if (languageVersionMatcher.find()) {
            languageVersion = LanguageVersion.fromString(languageVersionMatcher.group(1));
        }

        var languageConfig = new LanguageConfig(Optional.ofNullable(rootNode), fallbackNodeTypeMapping, Optional.ofNullable(languageProvider), Optional.ofNullable(languageVersion));

        /*
         * TODO: Maybe change the test setup here and write the expected Java code as separate files instead of concatenating it
         *  in one large file
         *  That might be better for the Git repository size (since Git stores snapshots, not diffs)
         *  And also makes it easier to troubleshoot compilation errors because latest versions of java-compiler-testing
         *  do not include the failing code snippet in the error message anymore, and only using the affected line number
         *  is difficult due to the concatenation performed here; see also https://github.com/ascopes/java-compiler-testing/issues/720#issuecomment-2351004590
         *  (could consider raising this as feature request though, to include minimal context in the compilation error messages again)
         */
        List<JavaFile> actualGeneratedFiles = new ArrayList<>();
        StringBuilder actualContent = new StringBuilder();
        CodeGenerator.JavaCodeWriter codeWriter = javaCode -> {
            actualGeneratedFiles.add(javaCode);
            try {
                javaCode.writeTo(actualContent);
            } catch (IOException e) {
                throw new CodeGenException("Failed writing code", e);
            }

            actualContent.append("\n\n/* ");
            actualContent.append("=".repeat(20));
            actualContent.append(" */ \n\n");
        };
        new CodeGenerator(config).generate(nodeTypesFile, languageConfig, codeWriter, VERSION_INFO);

        if (UPDATE_EXPECTED) {
            Files.writeString(expectedFile, actualContent.toString());
        }

        compileCode(actualGeneratedFiles, config, rootNode != null);

        if (UPDATE_EXPECTED) {
            throw new TestAbortedException("Expected file was updated");
        }

        String expectedContent;
        if (Files.isRegularFile(expectedFile)) {
            expectedContent = Files.readString(expectedFile);
        } else {
            throw (Exception) fail("Missing expected file: " + expectedFile);
        }

        assertEquals(expectedContent, actualContent.toString());
    }

    private static String[] createExpectedFilePath(List<String> pathPrefix, String fileName) {
        List<String> filePath = new ArrayList<>(pathPrefix);
        filePath.add(fileName);
        return filePath.toArray(String[]::new);
    }

    private static final String DUMMY_LANGUAGE_PROVIDER_NAME = "org.example.lang.LangProvider";
    private static void compileCode(List<JavaFile> javaFiles, CodeGenConfig codeGenConfig, boolean hasRootNode) throws Exception {
        var compiler = JctCompilers.newPlatformCompiler();
        try (var workspace = Workspaces.newWorkspace(PathStrategy.RAM_DIRECTORIES)) {
            var sourcePath = workspace.createSourcePathPackage();

            for (var javaFile : javaFiles) {
                var sourceCode = new StringBuilder();
                javaFile.writeTo(sourceCode);

                var fragments = new ArrayList<>(Arrays.asList(javaFile.packageName().split("\\.")));
                fragments.add(javaFile.typeSpec().name() + ".java");
                sourcePath.createFile(fragments)
                    .withContents(sourceCode.toString());
            }

            // Create a dummy "language provider" class
            var langProviderFragments = DUMMY_LANGUAGE_PROVIDER_NAME.split("\\.");
            // Append ".java" to class name
            langProviderFragments[langProviderFragments.length - 1] += ".java";
            sourcePath.createFile(langProviderFragments)
                .withContents("""
                    package org.example.lang;
                    
                    import io.github.treesitter.jtreesitter.Language;
                    
                    public class LangProvider {
                        public static final Language field = null;
                    
                        public static Language method() {
                            return null;
                        }
                    
                        public static Language methodException() throws Exception {
                            return null;
                        }
                    
                        public static Language methodThrowable() throws Throwable {
                            return null;
                        }
                    }
                    """);

            // Note: `-Xlint:all` might lead to JDK version specific warnings, but Gradle JVM toolchain should make sure
            // specific JDK version is used
            compiler.addCompilerOptions("-Xlint:all").showWarnings(true);
            var compilation = compiler.compile(workspace);

            assertThatCompilation(compilation)
                .isSuccessfulWithoutWarnings();

            List<String> expectedDirectory = Arrays.asList(codeGenConfig.packageName().split("\\."));
            assertThatCompilation(compilation)
                .classOutputPackages()
                .fileExists(createExpectedFilePath(expectedDirectory, codeGenConfig.nonEmptyTypeName() + ".class"))
                .isNotEmptyFile();
            assertThatCompilation(compilation)
                .classOutputPackages()
                .fileExists(createExpectedFilePath(expectedDirectory, "TypedNode.class"))
                .isNotEmptyFile();

            if (hasRootNode) {
                assertThatCompilation(compilation)
                    .classOutputPackages()
                    .fileExists(createExpectedFilePath(expectedDirectory, "TypedTree.class"))
                    .isNotEmptyFile();
            }
        }
    }

    @Test
    void testError_NonExistentNodeTypesFile(@TempDir Path tempDir) {
        var config = new CodeGenConfig(
            "org.example",
            Optional.empty(),
            "NonEmpty",
            CodeGenConfig.ChildTypeAsTopLevel.NEVER,
            NameGenerator.DEFAULT,
            Optional.of(GENERATED_ANNOTATION_CONFIG)
        );
        var codeGenerator = new CodeGenerator(config);

        var calledCodeWriter = new AtomicBoolean(false);
        CodeGenerator.JavaCodeWriter codeWriter = _ -> {
            // Set that code writer was called, in case AssertionError thrown below is discarded somewhere
            calledCodeWriter.set(true);

            throw new AssertionError("should not be called");
        };

        Path nodeTypesFile = tempDir.resolve("does-not-exist.json");

        var languageConfig = new LanguageConfig(Optional.empty(), Map.of(), Optional.empty(), Optional.empty());
        var e = assertThrows(CodeGenException.class, () -> codeGenerator.generate(nodeTypesFile, languageConfig, codeWriter, VERSION_INFO));
        assertEquals("Failed reading node types file: " + nodeTypesFile, e.getMessage());
        assertFalse(calledCodeWriter.get());
    }

    @Test
    void testError_FileAsOutputDir(@TempDir Path tempDir) throws Exception {
        var config = new CodeGenConfig(
            "org.example",
            Optional.empty(),
            "NonEmpty",
            CodeGenConfig.ChildTypeAsTopLevel.NEVER,
            NameGenerator.DEFAULT,
            Optional.of(GENERATED_ANNOTATION_CONFIG)
        );
        var codeGenerator = new CodeGenerator(config);

        var nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "my_node",
                "named": true
              }
            ]
            """);

        Path file = tempDir.resolve("some-file.txt");
        Files.createFile(file);

        var languageConfig = new LanguageConfig(Optional.empty(), Map.of(), Optional.empty(), Optional.empty());
        var e = assertThrows(CodeGenException.class, () -> codeGenerator.generate(nodeTypesFile, languageConfig, file));
        assertEquals("Output dir is not a directory: " + file, e.getMessage());
    }

    @Test
    void testError_UnknownRootType() {
        var config = new CodeGenConfig(
            "org.example",
            Optional.empty(),
            "NonEmpty",
            CodeGenConfig.ChildTypeAsTopLevel.NEVER,
            NameGenerator.DEFAULT,
            Optional.of(GENERATED_ANNOTATION_CONFIG)
        );
        var codeGenerator = new CodeGenerator(config);

       var nodeTypesJson = """
            [
              {
                "type": "my_node",
                "named": true
              }
            ]
            """;

        CodeGenerator.JavaCodeWriter codeWriter = _ -> {
            // ignored
        };

        String rootNode = "does-not-exist";

        var languageConfig = new LanguageConfig(Optional.of(rootNode), Map.of(), Optional.empty(), Optional.empty());
        var e = assertThrows(CodeGenException.class, () -> codeGenerator.generate(new StringReader(nodeTypesJson), languageConfig, codeWriter, VERSION_INFO));
        assertEquals("Root node type '%s' not found".formatted(rootNode), e.getMessage());
    }

    /**
     * Tests behavior when both {@code node-types.json} (with {@code "root": true}) and the user specifies
     * a root type, even if they are the same.
     */
    @Test
    void testError_JsonRootAndUserRoot() {
        var config = new CodeGenConfig(
            "org.example",
            Optional.empty(),
            "NonEmpty",
            CodeGenConfig.ChildTypeAsTopLevel.NEVER,
            NameGenerator.DEFAULT,
            Optional.of(GENERATED_ANNOTATION_CONFIG)
        );
        var codeGenerator = new CodeGenerator(config);

        var nodeTypesJson = """
            [
              {
                "type": "my_node",
                "named": true,
                "root": true
              }
            ]
            """;

        CodeGenerator.JavaCodeWriter codeWriter = _ -> {
            // ignored
        };

        String rootNode = "my_node";

        var languageConfig = new LanguageConfig(Optional.of(rootNode), Map.of(), Optional.empty(), Optional.empty());
        var e = assertThrows(CodeGenException.class, () -> codeGenerator.generate(new StringReader(nodeTypesJson), languageConfig, codeWriter, VERSION_INFO));
        assertEquals(
            "Should not explicitly specify root node type when 'node-types.json' already specifies root node ('my_node')",
            e.getMessage()
        );
    }

    /**
     * Tests behavior when an unknown type is referenced. tree-sitter seems to have a bug where aliases can cause
     * this, see https://github.com/tree-sitter/tree-sitter/issues/1654.
     */
    @Test
    void testError_UnknownReferencedType() {
        var config = new CodeGenConfig(
            "org.example",
            Optional.empty(),
            "NonEmpty",
            CodeGenConfig.ChildTypeAsTopLevel.NEVER,
            NameGenerator.DEFAULT,
            Optional.of(GENERATED_ANNOTATION_CONFIG)
        );
        var codeGenerator = new CodeGenerator(config);

        // Snippet is from Python node-types.json, but any node-types.json which references unknown type should reproduce this
        var nodeTypesJson = """
            [
              {
                "type": "as_pattern",
                "named": true,
                "fields": {
                  "alias": {
                    "multiple": false,
                    "required": false,
                    "types": [
                      {
                        "type": "as_pattern_target",
                        "named": true
                      }
                    ]
                  }
                }
              }
            ]
            """;

        CodeGenerator.JavaCodeWriter codeWriter = _ -> {
            // ignored
        };

        var languageConfig = new LanguageConfig(Optional.empty(), Map.of(), Optional.empty(), Optional.empty());
        var e = assertThrows(NoSuchElementException.class, () -> codeGenerator.generate(new StringReader(nodeTypesJson), languageConfig, codeWriter, VERSION_INFO));
        assertEquals(
            "Unknown type name: as_pattern_target\nPotential tree-sitter bug https://github.com/tree-sitter/tree-sitter/issues/1654",
            e.getMessage()
        );
    }

    @Test
    void testVersion() {
        var versionInfo = CodeGenerator.version();

        String version = versionInfo.version();
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"), "Unexpected version: " + version);

        String commitId = versionInfo.gitCommitId();
        assertTrue(commitId.matches("[0-9a-f]{40}"), "Unexpected commit ID: " + commitId);

        String jtreesitterVersion = versionInfo.jtreesitterVersion();
        assertTrue(jtreesitterVersion.matches("\\d+\\.\\d+\\.\\d+"), "Unexpected version: " + jtreesitterVersion);
    }
}
