package marcono1234.jtreesitter.type_gen.cli;

import marcono1234.jtreesitter.type_gen.CodeGenerator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link Main} class of the CLI.
 */
class MainTest {
    /**
     * Runs {@link Main} with the given {@code args} and performs assertions on the output.
     */
    private static void assertMainResult(List<String> args, int expectedExitCode, Consumer<String> stdOutVerifier, Consumer<String> stdErrVerifier) {
        StringWriter stdOut = new StringWriter();
        PrintWriter stdOutPrint = new PrintWriter(stdOut);
        StringWriter stdErr = new StringWriter();
        PrintWriter stdErrPrint = new PrintWriter(stdErr);

        int exitCode = Main.main(args.toArray(String[]::new), stdOutPrint, stdErrPrint);
        stdOutPrint.close();
        stdErrPrint.close();

        // Normalize whitespace and line breaks
        String stdOutStr = stdOut.toString().strip().replaceAll("\\R", "\n");
        String stdErrStr = stdErr.toString().strip().replaceAll("\\R", "\n");

        assertEquals(
            expectedExitCode,
            exitCode,
            "Unexpected exit code " + exitCode + "\nstd-out:\n" + stdOutStr.indent(2) + "std-err:\n" + stdErrStr.indent(2)
        );
        stdOutVerifier.accept(stdOutStr);
        stdErrVerifier.accept(stdErrStr);
    }

    private static void assertContains(String actual, String expected) {
        assertTrue(actual.contains(expected), "'" + actual + "' does not contain '" + expected + "'");
    }

    @Test
    void versionCommand() {
        var expectedVersionInfo = CodeGenerator.version();
        assertMainResult(
            List.of("--version"),
            CommandLine.ExitCode.OK,
            stdOut -> {
                assertContains(stdOut, expectedVersionInfo.gitRepository());
                assertContains(stdOut, expectedVersionInfo.version());
                assertContains(stdOut, expectedVersionInfo.gitCommitId());
            },
            stdErr -> assertEquals("", stdErr)
        );
    }

    @Test
    void helpCommand() {
        assertMainResult(
            List.of("--help"),
            CommandLine.ExitCode.OK,
            // Check for the prefix of the custom command header text
            stdOut -> assertContains(stdOut, "Generates a type-safe API for "),
            stdErr -> assertEquals("", stdErr)
        );
    }

    /** Creates an OS-agnostic path, for assertions */
    private static String canonicalizePath(Path path) {
        StringJoiner joiner = new StringJoiner("/");
        // Join individual path pieces
        path.forEach(p -> joiner.add(p.toString()));
        return joiner.toString();
    }

    private static void assertFiles(Path directory, List<String> expectedFiles) throws IOException {
        List<String> files = new ArrayList<>();
        try (Stream<Path> filesStream = Files.walk(directory)) {
            filesStream
                .filter(f -> !Files.isDirectory(f))
                .map(directory::relativize)
                .map(MainTest::canonicalizePath)
                .forEach(files::add);
        }
        // Sort to get consistent order, independent of file walk order
        files.sort(null);
        assertEquals(expectedFiles, files);
    }

    /*
     * Note: These code generation tests mainly check that the CLI options have an effect, they don't check the
     * exact code generation output or whether the generated source code can be compiled.
     * That is covered by the other unit and integration tests.
     */

    @Test
    void generateCommand(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              },
              {
                "type": "second",
                "named": true,
                "root": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString()
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeSecond.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java", "com/example/TypedTree.java"));
    }

    @Test
    void generateCommand_DefaultPackage(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                // Empty package ('default' package)
                "--package", "",
                "--output-dir", outputDir.toString()
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("NodeFirst.java", "NodeUtils.java", "NonEmpty.java", "TypedNode.java"));
    }

    @Test
    void generateCommand_RootNode(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              },
              {
                "type": "second",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--root-node", "second"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeSecond.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java", "com/example/TypedTree.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/TypedTree.java")), "public NodeSecond getRootNode() {");
    }

    @Test
    void generateCommand_RootNode_NonExistent(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              },
              {
                "type": "second",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // Node type with this name does not exist
                "--root-node", "third"
            ),
            CommandLine.ExitCode.SOFTWARE,
            stdOut -> assertEquals("", stdOut),
            stdErr -> {
                assertContains(stdErr, "[ERROR] Code generation failed");
                assertContains(stdErr, "CodeGenException: Root node type 'third' not found");
            }
        );

        assertFiles(outputDir, List.of());
    }

    @Test
    void generateCommand_FallbackNodeTypeMapping(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "as_pattern",
                "named": true,
                "fields": {
                  "alias": {
                    "multiple": false,
                    "required": true,
                    "types": [
                      {
                        "type": "as_pattern_target",
                        "named": true
                      }
                    ]
                  }
                }
              },
              {
                "type": "my_node",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--fallback-node-type-mapping", "as_pattern_target=my_node"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeAsPattern.java", "com/example/NodeMyNode.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        // Should contain getter method for field 'alias' which has mapped type 'my_node' as result
        assertContains(Files.readString(outputDir.resolve("com/example/NodeAsPattern.java")), "public NodeMyNode getFieldAlias() {");
    }

    @Test
    void generateCommand_LanguageProvider_Method(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--language-provider", "com.example.MyClass#method()"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/LanguageUtils.java", "com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/LanguageUtils.java")), "MyClass.method()");
    }

    @Test
    void generateCommand_LanguageProvider_Field(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--language-provider", "com.example.MyClass#field"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/LanguageUtils.java", "com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/LanguageUtils.java")), "MyClass.field");
    }

    @Test
    void generateCommand_ExpectedLanguageVersion(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--language-provider", "com.example.MyClass#field",
                "--expected-language-version", "1.2.3"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/LanguageUtils.java", "com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        String fileContent = Files.readString(outputDir.resolve("com/example/LanguageUtils.java"));
        assertContains(fileContent, "checkLanguageVersion();");
        assertContains(fileContent, "int expectedMajor = 1;");
        assertContains(fileContent, "int expectedMinor = 2;");
        assertContains(fileContent, "int expectedPatch = 3;");
    }

    /** Tests using {@code --expected-language-version} without specifying {@code --language-provider} */
    @Test
    void generateCommand_ExpectedLanguageVersion_NoLanguageProvider(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // Does not specify `--language-provider`
                "--expected-language-version", "1.2.3"
            ),
            CommandLine.ExitCode.USAGE,
            stdOut -> assertEquals("", stdOut),
            stdErr -> assertContains(
                stdErr,
                // Note: This exact message depends on picocli implementation details
                "Error: Missing required argument(s): --language-provider=<languageProvider>"
            )
        );

        assertFalse(Files.exists(outputDir));
    }

    @Test
    void generateCommand_NullableAnnotation(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--nullable-annotation", "com.example.MyNullable"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/NodeFirst.java")), "@MyNullable");
    }

    @Test
    void generateCommand_NullMarkedAnnotation(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--nullable-annotation", "com.example.MyNullable",
                "--nullmarked-package-annotation", "com.example.MyNullMarked"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java", "com/example/package-info.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/NodeFirst.java")), "@MyNullable");
        assertContains(Files.readString(outputDir.resolve("com/example/package-info.java")), "@MyNullMarked");
    }

    @Test
    void generateCommand_NullMarkedAnnotation_JSpecify_Implicit(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // JSpecify @Nullable
                "--nullable-annotation", Nullable.class.getName()
                // JSpecify's @NullMarked should be implied
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java", "com/example/package-info.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/NodeFirst.java")), "@" + Nullable.class.getSimpleName());
        assertContains(Files.readString(outputDir.resolve("com/example/package-info.java")), "@" + NullMarked.class.getSimpleName());
    }

    @Test
    void generateCommand_NullMarkedAnnotation_JSpecify_None(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // JSpecify @Nullable; normally implies @NullMarked
                "--nullable-annotation", Nullable.class.getName(),
                // Do not emit JSpecify @NullMarked annotation
                "--nullmarked-package-annotation", "-"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/NodeFirst.java")), "@" + Nullable.class.getSimpleName());
    }

    /** Tests using {@code --nullmarked-package-annotation} without specifying {@code --nullable-annotation} */
    @Test
    void generateCommand_NullMarkedAnnotation_NoNullable(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // Does not specify `--nullable-annotation`
                "--nullmarked-package-annotation", "com.example.MyNullMarked"
            ),
            CommandLine.ExitCode.USAGE,
            stdOut -> assertEquals("", stdOut),
            stdErr -> assertContains(
                stdErr,
                // Note: This exact message depends on picocli implementation details
                "Missing required argument(s): --nullable-annotation=<nullableAnnotationTypeName>"
            )
        );

        assertFalse(Files.exists(outputDir));
    }

    @Test
    void generateCommand_NonEmptyAnnotation(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--non-empty-annotation-name", "MyNonEmpty"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/MyNonEmpty.java", "com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/TypedNode.java"));
    }

    @Test
    void generateCommand_ChildTypeAsTopLevel(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              },
              {
                "type": "second",
                "named": true
              },
              {
                "type": "third",
                "named": true,
                "fields": {},
                "children": {
                  "multiple": false,
                  "required": false,
                  "types": [
                    {
                      "type": "first",
                      "named": true
                    },
                    {
                      "type": "second",
                      "named": true
                    }
                  ]
                }
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--child-type-as-top-level", "always"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeSecond.java", "com/example/NodeThird$Child.java", "com/example/NodeThird.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
    }

    @Test
    void generateCommand_TypedNodeSuperinterface(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--typed-node-superinterface", "com.example.TypedNodeSuper"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/TypedNode.java")), "interface TypedNode extends TypedNodeSuper");
    }

    @Test
    void generateCommand_TokenNameMapping(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
             [
              {
                "type": "my_node",
                "named": true,
                "fields": {
                  "my_field": {
                    "multiple": false,
                    "required": true,
                    "types": [
                      {
                        "type": "<",
                        "named": false
                      },
                      {
                        "type": ">",
                        "named": false
                      }
                    ]
                  }
                }
              }
            ]
            """);

        Path tokenNameMappingFile = tempDir.resolve("token-name-mapping.json");
        // Note: For '>' this uses a fallback mapping ("") for the field name
        Files.writeString(tokenNameMappingFile, """
            {
              "my_node": {
                "my_field": {
                  "<": "LEFT"
                },
                "": {
                  ">": "RIGHT"
                }
              }
            }
            """);

        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--token-name-mapping", tokenNameMappingFile.toString()
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeMyNode.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        String fileContent = Files.readString(outputDir.resolve("com/example/NodeMyNode.java"));
        assertContains(fileContent, "LEFT(\"<\")");
        assertContains(fileContent, "RIGHT(\">\")");
    }

    @Test
    void generateCommand_TokenNameMapping_NonExhaustive(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
             [
              {
                "type": "my_node",
                "named": true,
                "fields": {
                  "my_field": {
                    "multiple": false,
                    "required": true,
                    "types": [
                      {
                        "type": "<",
                        "named": false
                      },
                      {
                        "type": ">",
                        "named": false
                      }
                    ]
                  }
                }
              }
            ]
            """);

        Path tokenNameMappingFile = tempDir.resolve("token-name-mapping.json");
        // Mapping does not contain token type '>'
        Files.writeString(tokenNameMappingFile, """
            {
              "my_node": {
                "my_field": {
                  "<": "LEFT"
                }
              }
            }
            """);

        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--token-name-mapping", tokenNameMappingFile.toString()
            ),
            CommandLine.ExitCode.SOFTWARE,
            stdOut -> assertEquals("", stdOut),
            stdErr -> {
                assertContains(stdErr, "[ERROR] Code generation failed");
                assertContains(stdErr, "Token type not mapped: type = my_node, field = my_field, token = >");
            }
        );

        assertFiles(outputDir, List.of());
    }

    @Test
    void generateCommand_TypedQuery(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--generate-typed-query"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals(
                "[WARNING] Generation of the 'typed query' code is currently experimental. Feedback is appreciated!"
                + "\n[SUCCESS] Successfully generated code in directory: " + outputDir,
                stdOut
            ),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/QNodeFirst.java", "com/example/TypedNode.java", "com/example/TypedQuery.java"));
    }

    @Test
    void generateCommand_GeneratedAnnotation_Custom(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--generated-annotation", "com.example.MyGenerated"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/NodeFirst.java")), "@MyGenerated");
    }

    @Test
    void generateCommand_GeneratedAnnotation_None(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // Do not emit any @Generated annotation
                "--generated-annotation", "-"
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertFalse(Files.readString(outputDir.resolve("com/example/NodeFirst.java")).contains("@Generated"));
    }

    @Test
    void generateCommand_GeneratedAnnotation_Time(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        String time = "2025-09-28T16:15:30Z";
        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--generated-time", time
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/NodeFirst.java")), "date = \"" + time + "\"");
    }

    @Test
    void generateCommand_GeneratedAnnotation_Comment(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        String comment = "custom comment";
        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                "--generated-comment", comment
            ),
            CommandLine.ExitCode.OK,
            stdOut -> assertEquals("[SUCCESS] Successfully generated code in directory: " + outputDir, stdOut),
            stdErr -> assertEquals("", stdErr)
        );

        assertFiles(outputDir, List.of("com/example/NodeFirst.java", "com/example/NodeUtils.java", "com/example/NonEmpty.java", "com/example/TypedNode.java"));
        assertContains(Files.readString(outputDir.resolve("com/example/NodeFirst.java")), comment);
    }

    /** Tests using conflicting {@code @Generated} annotation options */
    @Test
    void generateCommand_GeneratedAnnotation_Conflict(@TempDir Path tempDir) throws IOException {
        Path nodeTypesFile = tempDir.resolve("node-types.json");
        Files.writeString(nodeTypesFile, """
            [
              {
                "type": "first",
                "named": true
              }
            ]
            """);
        Path outputDir = tempDir.resolve("output");

        String time = "2025-09-28T16:15:30Z";
        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // Conflicting options
                "--generated-annotation", "com.example.MyGenerated",
                "--generated-time", time
            ),
            CommandLine.ExitCode.USAGE,
            stdOut -> assertEquals("", stdOut),
            stdErr -> assertContains(
                stdErr,
                // Note: This exact message depends on picocli implementation details
                "Error: --generated-annotation=<generatedAnnotationTypeName> and ([--generated-time=<generatedTime>] [--generated-comment=<generatedComment>]) are mutually exclusive (specify only one)"
            )
        );
        assertFalse(Files.exists(outputDir));


        assertMainResult(
            List.of(
                "--node-types", nodeTypesFile.toString(),
                "--package", "com.example",
                "--output-dir", outputDir.toString(),
                // Conflicting options
                "--generated-annotation", "-",
                "--generated-time", time
            ),
            CommandLine.ExitCode.USAGE,
            stdOut -> assertEquals("", stdOut),
            stdErr -> assertContains(
                stdErr,
                // Note: This exact message depends on picocli implementation details
                "Error: --generated-annotation=<generatedAnnotationTypeName> and ([--generated-time=<generatedTime>] [--generated-comment=<generatedComment>]) are mutually exclusive (specify only one)"
            )
        );
        assertFalse(Files.exists(outputDir));
    }
}
