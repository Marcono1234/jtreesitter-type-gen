package marcono1234.jtreesitter.type_gen.cli;

import marcono1234.jtreesitter.type_gen.*;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageProviderConfig;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageVersion;
import marcono1234.jtreesitter.type_gen.cli.converter.*;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;

// Suppress warnings for fields assigned using reflection by picocli
@SuppressWarnings({"FieldMayBeFinal", "NotNullFieldNotInitialized"})
@CommandLine.Command(
    // Replace default name "<main class>" which might be a bit irritating in usage help
    name = "",
    versionProvider = CommandGenerate.VersionProvider.class,
    mixinStandardHelpOptions = true,
    header = {
        "Generates a type-safe API for your Tree-sitter grammar to use with Java Tree-sitter (jtreesitter)",
        ""
    },
    description = {
        "",
        "Uses the 'node-types.json' file of a Tree-sitter grammar to generate a type-safe Java API around Java Tree-sitter (jtreesitter).",
        "The minimum required options are the path to the 'node-types.json' file ('" + CommandGenerate.OPTION_NODE_TYPES + "'),"
        + " the Java package to use for the generated source code ('" + CommandGenerate.OPTION_PACKAGE + "') and the"
        + " output directory where to place the generated source files ('" + CommandGenerate.OPTION_OUTPUT_DIR + "').",
        "",
        "Example:",
        "  java -jar cli.jar " + CommandGenerate.OPTION_NODE_TYPES + "=node-types.json " + CommandGenerate.OPTION_PACKAGE + "=com.example " + CommandGenerate.OPTION_OUTPUT_DIR + "=generated-src",
        "",
        "The other options can be used to further tweak the generated code.",
        ""
    }
)
class CommandGenerate implements Callable<Void> {
    static final String OPTION_NODE_TYPES = "--node-types";
    static final String OPTION_OUTPUT_DIR = "--output-dir";
    static final String OPTION_PACKAGE = "--package";

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Option(
        names = {OPTION_NODE_TYPES},
        description = {
            "Path to the 'node-types.json' file",
            "This file is generated when running 'tree-sitter generate'.",
            "The official tree-sitter repositories under the https://github.com/tree-sitter/ organization usually contain this under 'src/node-types.json'.",
            "WARNING: Don't use a 'node-types.json' file from untrusted sources, it could lead to arbitrary code execution.",
        },
        required = true
    )
    private Path nodeTypesFile;

    @CommandLine.Option(
        names = {"-o", OPTION_OUTPUT_DIR},
        description = "Directory where the generated source code files will be written (should not include the package name)",
        required = true
    )
    private Path outputDir;

    @CommandLine.Option(
        names = {"-p", OPTION_PACKAGE},
        description = "Java package name to use for the generated code",
        required = true
    )
    private String packageName;

    @CommandLine.Option(
        names = {"--root-node"},
        description = {
            "Name of the root node type",
            "This is usually the first 'rules' entry in the 'grammar.js' file. If provided, additional code will be"
            + " generated to go from a jtreesitter 'Tree' to a 'typed tree'.",
            "Starting with tree-sitter 0.24.0 this information is available in the 'node-types.json' file (as"
            + " '\"root\": true'), and must not be specified using this option here.",
        }
    )
    @Nullable
    private String rootNodeType;

    @CommandLine.ArgGroup(
        exclusive = false
    )
    @Nullable
    private LanguageOptions languageOptions;

    // Separate arg group so that any of the non-required options here require that `languageProvider` is specified,
    // since they all depend on a `Language` instance being available
    private static class LanguageOptions {
        @CommandLine.Option(
            names = {"--language-provider"},
            description = {
                "Name of method or field to obtain a Language instance",
                "Fully qualified name of a static field (e.g. 'com.example.MyClass#field') or no-args method"
                + " (e.g. 'com.example.MyClass#method()') which provides a tree-sitter 'Language' object for the language"
                + " represented by the 'node-types.json' file. If provided, additional code will be generated, exposing"
                + " for example numeric node and field IDs, and validating at runtime that the names in the 'node-types.json'"
                + " file actually match the loaded language."
            },
            converter = LanguageProviderConverter.class,
            required = true
        )
        private LanguageProviderConfig languageProvider;

        @CommandLine.Option(
            names = {"--expected-language-version"},
            description = {
                "Expected version of the language",
                "Version number in the format '<major>.<minor>.<patch>' which the language should have ('patch' version"
                + " deviations are permitted). When the expected version is specified, additional validation code is"
                + " generated which ensures that the loaded language is compatible with the generated code."
                + " Without this validation incompatibilities could otherwise cause exceptions or incorrect behavior.",
                "The language / grammar version is specified as 'metadata.version' in the 'tree-sitter.json' file"
                + " of a grammar.",
            },
            converter = LanguageVersionConverter.class
        )
        @Nullable
        private LanguageVersion languageVersion;
    }

    @CommandLine.Option(
        names = {"--nullable-annotation"},
        description = {
            "Qualified name of external @Nullable annotation to use",
            "If value '-' is used or if not specified, 'java.util.Optional' will be used for all optional values in"
            + " the generated code.",
        },
        converter = DisableableTypeNameConverter.class
    )
    private DisableableArg<TypeName> nullableAnnotationTypeName = DisableableArg.disabled();

    @CommandLine.Option(
        names = {"--non-empty-annotation-name"},
        description = {
            "Simple name to use for the generated @NonEmpty annotation",
            "The generated code will use that annotation for all collection values which will always contain at least"
            + " one element.",
        }
    )
    @Nullable
    private String nonEmptyAnnotationSimpleName;

    @CommandLine.Option(
        names = {"--child-type-as-top-level"},
        description = {
            "Whether to generate top-level Java classes for child types",
            "Using 'never' can lead to compilation errors.",
        },
        converter = ChildAsTopLevelConverter.class,
        defaultValue = "as-needed"
    )
    private CodeGenConfig.ChildTypeAsTopLevel childTypeAsTopLevel;

    @SuppressWarnings("DefaultAnnotationParam") // make `exclusive = true` explicit
    @CommandLine.ArgGroup(
        heading = "@Generated options\n",
        exclusive = true
    )
    @Nullable
    private GeneratedAnnotationOptions generatedAnnotationOptions;

    private static class GeneratedAnnotationOptions {
        @CommandLine.Option(
            names = {"--generated-annotation"},
            description = {
                "Qualified name of external @Generated annotation to use",
                "If value '-' is used, no @Generated annotation will be added.",
                "If not specified, the annotation type 'javax.annotation.processing.Generated' will be used.",
            },
            converter = DisableableTypeNameConverter.class
        )
        @Nullable
        private DisableableArg<TypeName> generatedAnnotationTypeName;

        // Add this as subgroup, so that EITHER custom annotation type can be specified OR javax.Generated can be customized
        @CommandLine.ArgGroup(
            heading = "@javax.*.Generated options\n",
            multiplicity = "1",
            exclusive = false
        )
        @Nullable
        private JavaxGeneratedOptions javaxGeneratedOptions;

        private static class JavaxGeneratedOptions {
            @CommandLine.Option(
                names = {"--generated-time"},
                description = {
                    "Fixed time to use as value for the @Generated annotation",
                    "Can be useful for reproducible builds. If not specified the current time is used.",
                    "The time value has to be in ISO-8601 instant format, e.g. '2025-09-28T16:15:30Z'."
                }
            )
            @Nullable
            private Instant generatedTime;

            @CommandLine.Option(
                names = {"--generated-comment"},
                description = {
                    "Additional comment for the @Generated annotation",
                    "This can for example be origin or version information about the 'node-types.json' file.",
                }
            )
            @Nullable
            private String generatedComment;
        }
    }

    private Optional<CodeGenConfig.GeneratedAnnotationConfig> getGeneratedAnnotationConfig() {
        // Either not annotation options specified, or no custom annotation type specified
        if (generatedAnnotationOptions == null || generatedAnnotationOptions.generatedAnnotationTypeName == null) {
            var annotationType = CodeGenConfig.GeneratedAnnotationConfig.GeneratedAnnotationType.JAVAX_GENERATED;
            Optional<Instant> generatedTime = Optional.empty();
            Optional<String> generatedComment = Optional.empty();

            if (generatedAnnotationOptions != null) {
                var options = generatedAnnotationOptions.javaxGeneratedOptions;
                if (options != null) {
                    generatedTime = Optional.ofNullable(options.generatedTime);
                    generatedComment = Optional.ofNullable(options.generatedComment);
                }
            }

            return Optional.of(new CodeGenConfig.GeneratedAnnotationConfig(
                annotationType,
                generatedTime,
                generatedComment
            ));
        }
        // @Generated annotation explicitly disabled
        else if (generatedAnnotationOptions.generatedAnnotationTypeName.isDisabled()) {
            return Optional.empty();
        }
        // Custom annotation type specified
        else {
            var annotationType = new CodeGenConfig.GeneratedAnnotationConfig.GeneratedAnnotationType(
                generatedAnnotationOptions.generatedAnnotationTypeName.getEnabledValue(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            );
            return Optional.of(new CodeGenConfig.GeneratedAnnotationConfig(
                annotationType,
                Optional.empty(),
                Optional.empty()
            ));
        }
    }

    private LanguageConfig getLanguageConfig() {
        Optional<LanguageProviderConfig> languageProviderConfig = Optional.empty();
        Optional<LanguageVersion> expectedLanguageVersion = Optional.empty();

        if (languageOptions != null) {
            languageProviderConfig = Optional.of(languageOptions.languageProvider);
            expectedLanguageVersion = Optional.ofNullable(languageOptions.languageVersion);
        }

        return new LanguageConfig(
            Optional.ofNullable(rootNodeType),
            languageProviderConfig,
            expectedLanguageVersion
        );
    }

    @Override
    public Void call() throws Exception {
        var commandLine = commandSpec.commandLine();

        var codeGenConfig = new CodeGenConfig(
            packageName,
            nullableAnnotationTypeName.asOptional(),
            nonEmptyAnnotationSimpleName != null ? nonEmptyAnnotationSimpleName : "NonEmpty",
            childTypeAsTopLevel,
            NameGenerator.DEFAULT,
            getGeneratedAnnotationConfig()
        );

        var codeGenerator = new CodeGenerator(codeGenConfig);
        var languageConfig = getLanguageConfig();
        codeGenerator.generate(nodeTypesFile, languageConfig, outputDir);

        commandLine.getOut().println("[SUCCESS] Successfully generated code in directory: " + outputDir);

        return null;
    }

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            var versionInfo = CodeGenerator.version();
            return new String[] {
                versionInfo.gitRepository(),
                versionInfo.version(),
                versionInfo.gitCommitId(),
                // Possibly a bit misleading calling this "minimum" in case there are breaking changes in newer versions,
                // but calling it "recommended" or similar might lead to users sticking to old versions which might contain
                // bugs, which is not ideal either
                "Minimum jtreesitter version: " + versionInfo.jtreesitterVersion(),
            };
        }
    }
}
