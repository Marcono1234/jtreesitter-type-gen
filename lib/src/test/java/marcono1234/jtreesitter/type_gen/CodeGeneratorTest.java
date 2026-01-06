package marcono1234.jtreesitter.type_gen;

import com.palantir.javapoet.JavaFile;
import io.github.ascopes.jct.compilers.JctCompilers;
import io.github.ascopes.jct.workspaces.PathStrategy;
import io.github.ascopes.jct.workspaces.Workspaces;
import marcono1234.jtreesitter.type_gen.CodeGenConfig.GeneratedAnnotationConfig.GeneratedAnnotationType;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageProviderConfig;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageVersion;
import marcono1234.jtreesitter.type_gen.NameGenerator.TokenNameGenerator;
import org.jspecify.annotations.NonNull;
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

    private static final String DEFAULT_PACKAGE_NAME = "org.example";
    private static final String DEFAULT_NON_EMPTY_NAME = "NonEmpty";
    private static final CodeGenConfig.ChildTypeAsTopLevel DEFAULT_CHILD_AS_TOP_LEVEL = CodeGenConfig.ChildTypeAsTopLevel.AS_NEEDED;
    private static final NameGenerator DEFAULT_NAME_GENERATOR = NameGenerator.createDefault(TokenNameGenerator.AUTOMATIC);
    private static final boolean DEFAULT_GENERATE_FIND_NODES_METHODS = true;

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

        String packageName = baseFileName.contains("(default-package)") ? "" : DEFAULT_PACKAGE_NAME;
        boolean nullableAsOptional = baseFileName.contains("(nullable=Optional)");
        boolean nullMarked = baseFileName.contains("(nullmarked)");

        var childAsTopLevel = baseFileName.contains("(child-top-level)") ? CodeGenConfig.ChildTypeAsTopLevel.ALWAYS
            : CodeGenConfig.ChildTypeAsTopLevel.AS_NEEDED;

        var typedNodeSuperinterface = baseFileName.contains("(typed-node-superinterface)") ? TypeName.fromQualifiedName(DUMMY_TYPED_NODE_SUPERINTERFACE)
            : null;

        var tokenNameGenerator = baseFileName.contains("(token-name-generator)") ? TokenNameGenerator.fromMapping(Map.of("", Map.of("", Map.of("<", "LEFT", ">", "RIGHT"))), true)
            : TokenNameGenerator.AUTOMATIC;
        var nameGenerator = NameGenerator.createDefault(tokenNameGenerator);

        boolean findNodesMethods = !baseFileName.contains("(no-findNodes)");

        var typedQueryNameGenerator = baseFileName.contains("(typed-query)") ? TypedQueryNameGenerator.createDefault(nameGenerator)
            : null;

        var config = new CodeGenConfig(
            packageName,
            nullableAsOptional ? Optional.empty() : Optional.of(TypeName.JSPECIFY_NULLABLE_ANNOTATION),
            nullMarked ? Optional.of(TypeName.JSPECIFY_NULLMARKED_ANNOTATION) : Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            childAsTopLevel,
            Optional.ofNullable(typedNodeSuperinterface),
            nameGenerator,
            findNodesMethods,
            Optional.ofNullable(typedQueryNameGenerator),
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
        List<JavaFileSource> actualGeneratedFiles = new ArrayList<>();
        StringBuilder actualContent = new StringBuilder();
        var codeWriter = new CodeGenerator.JavaCodeWriter() {
            private void appendFileContentSeparator() {
                actualContent.append("\n\n/* ");
                actualContent.append("=".repeat(20));
                actualContent.append(" */ \n\n");
            }

            @Override
            public void write(@NonNull JavaFile javaCode) throws CodeGenException {
                actualGeneratedFiles.add(new JavaFileSource(javaCode));
                try {
                    javaCode.writeTo(actualContent);
                } catch (IOException e) {
                    throw new CodeGenException("Failed writing code", e);
                }
                appendFileContentSeparator();
            }

            @Override
            public void writePackageInfo(@NonNull String packageName, @NonNull String content) {
                actualGeneratedFiles.add(new JavaFileSource(packageName, "package-info.java", content));
                actualContent.append(content);
                appendFileContentSeparator();
            }
        };
        new CodeGenerator(config).generate(nodeTypesFile, languageConfig, codeWriter, VERSION_INFO);

        if (UPDATE_EXPECTED) {
            Files.writeString(expectedFile, actualContent.toString());
        }

        compileCode(actualGeneratedFiles, config, rootNode != null, languageProvider != null);

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

    private static final String DUMMY_TYPED_NODE_SUPERINTERFACE = "org.example.lang.TypedNodeSuper";
    private static final String DUMMY_LANGUAGE_PROVIDER_NAME = "org.example.lang.LangProvider";

    private static String[] createFilePath(String packageName, String fileName) {
        List<String> filePath = new ArrayList<>(Arrays.asList(packageName.split("\\.")));
        filePath.add(fileName);
        return filePath.toArray(String[]::new);
    }

    private static String[] classNameToSourcePath(String name) {
        var pathPieces = name.split("\\.");
        // Append ".java" to class name
        pathPieces[pathPieces.length - 1] += ".java";
        return pathPieces;
    }

    private static String[] classNameToCompiledPath(String name) {
        var pathPieces = name.split("\\.");
        // Append ".class" to class name
        pathPieces[pathPieces.length - 1] += ".class";
        return pathPieces;
    }

    private record JavaFileSource(
        String packageName,
        String fileName,
        String source
    ) {
        private static String getSource(JavaFile javaFile) {
            var sourceCode = new StringBuilder();
            try {
                javaFile.writeTo(sourceCode);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return sourceCode.toString();
        }

        JavaFileSource(JavaFile javaFile) {
            this(javaFile.packageName(), javaFile.typeSpec().name() + ".java", getSource(javaFile));
        }
    }

    private static void compileCode(List<JavaFileSource> javaFiles, CodeGenConfig codeGenConfig, boolean hasRootNode, boolean usesLanguageProvider) {
        var compiler = JctCompilers.newPlatformCompiler();
        try (var workspace = Workspaces.newWorkspace(PathStrategy.RAM_DIRECTORIES)) {
            var sourcePath = workspace.createSourcePathPackage();

            for (var javaFile : javaFiles) {
                var filePath = createFilePath(javaFile.packageName(), javaFile.fileName());
                sourcePath.createFile(filePath)
                    .withContents(javaFile.source());
            }

            boolean hasTypedNodeSuperinterface = codeGenConfig.typedNodeSuperinterface().isPresent();
            if (hasTypedNodeSuperinterface) {
                // Create a dummy "typed node superinterface"
                sourcePath.createFile(classNameToSourcePath(DUMMY_TYPED_NODE_SUPERINTERFACE))
                    .withContents("""
                        package org.example.lang;
                        
                        import io.github.treesitter.jtreesitter.Node;
                        
                        public interface TypedNodeSuper {
                            // Can define `getNode` in the superinterface already so that `TypedNode` effectively overrides it
                            Node getNode();
                        
                            // Can define custom default methods
                            default int getChildCount() {
                                return getNode().getChildCount();
                            }
                        }
                        """);
            }

            if (usesLanguageProvider) {
                // Create a dummy "language provider" class
                sourcePath.createFile(classNameToSourcePath(DUMMY_LANGUAGE_PROVIDER_NAME))
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
            }

            // Note: `-Xlint:all` might lead to JDK version specific warnings, but Gradle JVM toolchain should make sure
            // specific JDK version is used
            compiler.addCompilerOptions("-Xlint:all").showWarnings(true);
            var compilation = compiler.compile(workspace);

            assertThatCompilation(compilation)
                .isSuccessfulWithoutWarnings();

            String packageName = codeGenConfig.packageName();
            assertThatCompilation(compilation)
                .classOutputPackages()
                .fileExists(createFilePath(packageName, codeGenConfig.nonEmptyTypeName() + ".class"))
                .isNotEmptyFile();
            assertThatCompilation(compilation)
                .classOutputPackages()
                .fileExists(createFilePath(packageName, "TypedNode.class"))
                .isNotEmptyFile();

            if (hasRootNode) {
                assertThatCompilation(compilation)
                    .classOutputPackages()
                    .fileExists(createFilePath(packageName, "TypedTree.class"))
                    .isNotEmptyFile();
            }

            if (hasTypedNodeSuperinterface) {
                assertThatCompilation(compilation)
                    .classOutputPackages()
                    .fileExists(classNameToCompiledPath(DUMMY_TYPED_NODE_SUPERINTERFACE))
                    .isNotEmptyFile();
            }

            if (usesLanguageProvider) {
                assertThatCompilation(compilation)
                    .classOutputPackages()
                    .fileExists(classNameToCompiledPath(DUMMY_LANGUAGE_PROVIDER_NAME))
                    .isNotEmptyFile();
            }
        }
    }

    /** Code writer which throws assertion errors when any of its methods are called. */
    private static class ThrowingCodeWriter implements CodeGenerator.JavaCodeWriter {
        private final AtomicBoolean wasCalled = new AtomicBoolean(false);

        private void fail() {
            // Set that code writer was called, in case AssertionError thrown below is discarded somewhere
            wasCalled.set(true);
            throw new AssertionError("should not be called");
        }

        @Override
        public void write(@NonNull JavaFile javaCode) {
            fail();
        }

        @Override
        public void writePackageInfo(@NonNull String packageName, @NonNull String content) {
            fail();
        }

        public void verifyNotCalled() {
            if (wasCalled.get()) {
                throw new AssertionError("has been called, and original assertion error has been discarded");
            }
        }
    }

    @Test
    void testError_NonExistentNodeTypesFile(@TempDir Path tempDir) {
        var config = new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
            Optional.of(GENERATED_ANNOTATION_CONFIG)
        );
        var codeGenerator = new CodeGenerator(config);

        var codeWriter = new ThrowingCodeWriter();
        Path nodeTypesFile = tempDir.resolve("does-not-exist.json");

        var languageConfig = new LanguageConfig(Optional.empty(), Map.of(), Optional.empty(), Optional.empty());
        var e = assertThrows(CodeGenException.class, () -> codeGenerator.generate(nodeTypesFile, languageConfig, codeWriter, VERSION_INFO));
        assertEquals("Failed reading node types file: " + nodeTypesFile, e.getMessage());
        codeWriter.verifyNotCalled();
    }

    @Test
    void testError_FileAsOutputDir(@TempDir Path tempDir) throws Exception {
        var config = new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
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
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
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

        String rootNode = "does-not-exist";

        var languageConfig = new LanguageConfig(Optional.of(rootNode), Map.of(), Optional.empty(), Optional.empty());
        var codeWriter = new ThrowingCodeWriter();
        var e = assertThrows(CodeGenException.class, () -> codeGenerator.generate(new StringReader(nodeTypesJson), languageConfig, codeWriter, VERSION_INFO));
        assertEquals("Root node type '%s' not found".formatted(rootNode), e.getMessage());
        codeWriter.verifyNotCalled();
    }

    /**
     * Tests behavior when both {@code node-types.json} (with {@code "root": true}) and the user specifies
     * a root type, even if they are the same.
     */
    @Test
    void testError_JsonRootAndUserRoot() {
        var config = new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
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

        String rootNode = "my_node";

        var languageConfig = new LanguageConfig(Optional.of(rootNode), Map.of(), Optional.empty(), Optional.empty());
        var codeWriter = new ThrowingCodeWriter();
        var e = assertThrows(CodeGenException.class, () -> codeGenerator.generate(new StringReader(nodeTypesJson), languageConfig, codeWriter, VERSION_INFO));
        assertEquals(
            "Should not explicitly specify root node type when 'node-types.json' already specifies root node ('my_node')",
            e.getMessage()
        );
        codeWriter.verifyNotCalled();
    }

    /**
     * Tests behavior when an unknown node type is referenced. tree-sitter seems to have a bug where aliases can cause
     * this, see https://github.com/tree-sitter/tree-sitter/issues/1654.
     */
    @Test
    void testError_UnknownReferencedNodeType() {
        var config = new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            DEFAULT_GENERATE_FIND_NODES_METHODS,
            Optional.empty(),
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

        var languageConfig = new LanguageConfig(Optional.empty(), Map.of(), Optional.empty(), Optional.empty());
        var codeWriter = new ThrowingCodeWriter();
        var e = assertThrows(NoSuchElementException.class, () -> codeGenerator.generate(new StringReader(nodeTypesJson), languageConfig, codeWriter, VERSION_INFO));
        assertEquals(
            "Unknown type name: as_pattern_target\nPotential tree-sitter bug https://github.com/tree-sitter/tree-sitter/issues/1654",
            e.getMessage()
        );
        codeWriter.verifyNotCalled();
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
