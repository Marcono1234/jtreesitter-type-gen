package marcono1234.jtreesitter.type_gen;

import com.palantir.javapoet.JavaFile;
import marcono1234.jtreesitter.type_gen.internal.gen.GenJavaType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenRegularNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenSupertypeNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.common_classes.*;
import marcono1234.jtreesitter.type_gen.internal.gen.typed_query.TypedQueryGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.LanguageUtilsConfigData;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.NodeTypeLookup;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.NodeType;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Main class for code generation.
 */
public class CodeGenerator {
    private static final JsonMapper verboseJsonMapper = JsonMapper.builder()
        // Enhance exceptions for easier troubleshooting; the JSON files are not expected to contain sensitive information
        // which must not be leaked
        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        .build();

    private final CodeGenConfig config;

    public CodeGenerator(CodeGenConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Performs the code generation.
     *
     * <h4>Security</h4>
     * Only use {@code node-types.json} from trusted sources, for example from one of the official tree-sitter
     * repositories. Using an untrusted file could for example lead to code injection or remote code execution,
     * possibly even during code generation.
     *
     * @param nodeTypesFile path to the {@code node-types.json} file
     * @param languageConfig language / {@code node-types.json} specific configuration
     * @param outputDir directory where the generated source code files are written; is created if it does not exist yet
     * @throws CodeGenException if code generation failed
     */
    // Note: Internally this library uses `@Nullable`, but externally it uses `Optional`
    public void generate(Path nodeTypesFile, LanguageConfig languageConfig, Path outputDir) throws CodeGenException {
        Objects.requireNonNull(nodeTypesFile);
        Objects.requireNonNull(languageConfig);
        Objects.requireNonNull(outputDir);

        if (!Files.isRegularFile(nodeTypesFile)) {
            throw new CodeGenException("Node types file does not exist: " + nodeTypesFile);
        }

        if (Files.exists(outputDir) && !Files.isDirectory(outputDir)) {
            throw new CodeGenException("Output dir is not a directory: " + outputDir);
        }
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new CodeGenException("Failed creating output dir", e);
        }

        JavaCodeWriter codeWriter = javaCode -> {
            try {
                javaCode.writeTo(outputDir);
            } catch (Exception e) {
                throw new CodeGenException("Failed to write class '%s' to dir %s".formatted(javaCode.typeSpec().name(), outputDir), e);
            }
        };

        try {
            generate(nodeTypesFile, languageConfig, codeWriter, version());
        } catch (RuntimeException e) {
            throw new CodeGenException("Failed generating code", e);
        }
    }

    // Visible for testing
    interface JavaCodeWriter {
        void write(JavaFile javaCode) throws CodeGenException;
    }

    // Visible for testing
    void generate(Path nodeTypesFile, LanguageConfig languageConfig, JavaCodeWriter codeWriter, Version versionInfo) throws CodeGenException {
        List<NodeType> nodeTypes;
        try {
            nodeTypes = verboseJsonMapper.readValue(nodeTypesFile.toFile(), new TypeReference<>() {});
        } catch (JacksonException e) {
            throw new CodeGenException("Failed reading node types file: " + nodeTypesFile, e);
        }
        generate(nodeTypes, languageConfig, codeWriter, versionInfo);
    }

    // Visible for testing
    void generate(Reader nodeTypesReader, LanguageConfig languageConfig, JavaCodeWriter codeWriter, Version versionInfo) throws CodeGenException {
        List<NodeType> nodeTypes;
        try {
            nodeTypes = verboseJsonMapper.readValue(nodeTypesReader, new TypeReference<>() {});
        } catch (JacksonException e) {
            throw new CodeGenException("Failed reading node types", e);
        }
        generate(nodeTypes, languageConfig, codeWriter, versionInfo);
    }

    private void generate(List<NodeType> nodeTypes, LanguageConfig languageConfig, JavaCodeWriter codeWriter, Version versionInfo) throws CodeGenException {
        SequencedMap<String, NodeType> nodeTypesByName = new LinkedHashMap<>();
        for (NodeType nodeType : nodeTypes) {
            if (nodeType.named) {
                String name = nodeType.type;
                var existing = nodeTypesByName.put(name, nodeType);
                if (existing != null) {
                    throw new CodeGenException("Duplicate node type name: " + name);
                }
            }
        }

        var nodeGens = determineGenElements(nodeTypes, languageConfig.rootNodeTypeName().orElse(null), config.nameGenerator(), languageConfig.fallbackNodeTypeMapping());
        var languageUtilsConfigData = languageConfig.languageProviderConfig()
            .map(languageProvider -> new LanguageUtilsConfigData(
                languageProvider,
                // Retrieving expectedLanguageVersion is nested here because it can only be present if a `languageProvider` exists
                languageConfig.expectedLanguageVersion().orElse(null)
            ))
            .orElse(null);
        CodeGenHelper codeGenHelper = new CodeGenHelper(config, versionInfo, languageUtilsConfigData);

        var languageUtilsConfig = codeGenHelper.languageUtilsConfig();
        if (languageUtilsConfig != null) {
            codeWriter.write(new LanguageUtilsGenerator(codeGenHelper, languageUtilsConfig).generateCode());
        }

        codeWriter.write(new NodeUtilsGenerator(codeGenHelper).generateCode());
        codeWriter.write(new TypedNodeInterfaceGenerator(codeGenHelper).generateCode(
            config.typedNodeSuperinterface().map(CodeGenHelper::createClassName).orElse(null),
            nodeGens.nodeTypes(),
            nodeGens.typedNodeSubtypes()
        ));
        codeWriter.write(new NonEmptyAnnotationGenerator(codeGenHelper).generateCode());

        for (var nodeGen : nodeGens.nodeTypes()) {
            for (var javaCode : nodeGen.generateJavaCode(codeGenHelper)) {
                codeWriter.write(javaCode);
            }
        }

        if (nodeGens.rootNode != null) {
            codeWriter.write(new TypedTreeClassGenerator(codeGenHelper).generateCode(nodeGens.rootNode));
        }

        var typedQueryNameGenerator = config.typedQueryNameGenerator().orElse(null);
        if (typedQueryNameGenerator != null) {
            var javaFiles = new TypedQueryGenerator(codeGenHelper, typedQueryNameGenerator).generateCode(nodeGens.nodeTypes);
            for (var javaFile : javaFiles) {
                codeWriter.write(javaFile);
            }
        }
    }

    private SequencedMap<String, GenSupertypeNodeType> createSupertypeGens(List<NodeType> supertypes, NameGenerator nameGenerator) throws CodeGenException {
        SequencedMap<String, GenSupertypeNodeType> supertypeGens = new LinkedHashMap<>();
        for (var supertype : supertypes) {
            String typeName = supertype.type;
            var interfaceGen = GenSupertypeNodeType.create(supertype, nameGenerator);
            supertypeGens.put(typeName, interfaceGen);
        }

        return supertypeGens;
    }

    /**
     * @param nodeTypes
     *      All node types for which code should be generated
     * @param typedNodeSubtypes
     *      All subtypes of the generated {@code TypedNode} interface; includes {@code nodeTypes} as well as
     *      additional nested classes for example for node children
     * @param rootNode
     *      Root node in the parse tree; one of {@code nodeTypes}; {@code null} if root node is not specified
     */
    private record GenElements(List<GenNodeType> nodeTypes, List<GenJavaType> typedNodeSubtypes, @Nullable GenNodeType rootNode) {
    }

    private GenElements determineGenElements(List<NodeType> nodeTypes, @Nullable String rootNodeTypeCustom, NameGenerator nameGenerator, Map<String, String> fallbackNodeTypeMapping) throws CodeGenException {
        SequencedSet<String> allTypesNames = new LinkedHashSet<>();
        List<NodeType> supertypes = new ArrayList<>();

        SequencedMap<String, GenRegularNodeType> regularNodeGens = new LinkedHashMap<>();
        String rootNodeTypeJson = null;

        for (var nodeType : nodeTypes) {
            String typeName = nodeType.type;

            if (!nodeType.named) {
                // TODO: Are any of these actually permitted / possible? But even if so, should non-named node type
                //   be ignored nonetheless?
                if (nodeType.subtypes != null) {
                    throw new CodeGenException("Non-named node type '%s' should not have subtypes".formatted(typeName));
                }
                if (nodeType.children != null) {
                    throw new CodeGenException("Non-named node type '%s' should not have children".formatted(typeName));
                }
                if (nodeType.fields != null && !nodeType.fields.isEmpty()) {
                    throw new CodeGenException("Non-named node type '%s' should not have fields".formatted(typeName));
                }
                if (nodeType.root) {
                    throw new CodeGenException("Non-named node type '%s' should not be root node".formatted(typeName));
                }

                continue;
            }

            if (!allTypesNames.add(typeName)) {
                throw new CodeGenException("Duplicate node type name: " + typeName);
            }

            if (nodeType.root) {
                if (rootNodeTypeCustom != null) {
                    throw new CodeGenException("Should not explicitly specify root node type when 'node-types.json' already specifies root node ('%s')".formatted(typeName));
                }
                if (rootNodeTypeJson != null) {
                    throw new CodeGenException("Only a single root node type is supported; found node types: %s, %s".formatted(rootNodeTypeJson, typeName));
                }
                rootNodeTypeJson = typeName;
            }

            if (nodeType.subtypes != null) {
                // Handle supertypes later, once all types they refer to have been resolved
                supertypes.add(nodeType);
                continue;
            }

            regularNodeGens.put(typeName, GenRegularNodeType.create(nodeType, nameGenerator));
        }

        var supertypeGens = createSupertypeGens(supertypes, nameGenerator);

        NodeTypeLookup nodeTypeLookup = new NodeTypeLookup() {
            private GenNodeType getNodeType(String typeName, boolean canUseFallbackTypeName) {
                var nodeGen = regularNodeGens.get(typeName);
                if (nodeGen != null) {
                    return nodeGen;
                }

                var supertypeGen = supertypeGens.get(typeName);
                if (supertypeGen != null) {
                    return supertypeGen;
                }

                // Maybe tree-sitter bug: `alias` in grammar does not allow determining original type name in some cases, see https://github.com/tree-sitter/tree-sitter/issues/1654
                // For now allow users to specify a fallback mapping so that they can work around the issue without having to adjust the generated `node-types.json`
                String fallbackTypeName = fallbackNodeTypeMapping.get(typeName);
                if (fallbackTypeName != null) {
                    // Prevent infinite recursion when type names map between each other
                    if (!canUseFallbackTypeName) {
                        throw new IllegalStateException("Node type mapping to '" + typeName + "' should directly map to target node type, not to another fallback mapping");
                    }

                    return getNodeType(fallbackTypeName, false);
                }
                throw new NoSuchElementException("Unknown type name: " + typeName + "\nPotential tree-sitter bug https://github.com/tree-sitter/tree-sitter/issues/1654");
            }

            @Override
            public GenNodeType getNodeType(String typeName) throws NoSuchElementException {
                return getNodeType(typeName, true);
            }
        };

        // Populate subtypes now, once all types have been resolved, to support for example supertype referring to other supertype
        for (var supertypeGen : supertypeGens.values()) {
            supertypeGen.populateSubtypes(nodeTypeLookup);
        }

        List<GenJavaType> typedNodeSubtypes = new ArrayList<>();
        typedNodeSubtypes.addAll(regularNodeGens.values());
        typedNodeSubtypes.addAll(supertypeGens.values());

        // Populate children and fields now, once all types have been resolved
        for (var nodeTypeGen : regularNodeGens.values()) {
            nodeTypeGen.populateChildrenAndFields(nodeTypeLookup, nameGenerator, typedNodeSubtypes::add);
        }

        var allNodeTypeGens = concat(regularNodeGens.values(), supertypeGens.values());
        String rootNodeType = rootNodeTypeCustom != null ? rootNodeTypeCustom : rootNodeTypeJson;
        GenNodeType rootNode = rootNodeType == null
            ? null
            : allNodeTypeGens.stream().filter(n -> n.getTypeName().equals(rootNodeType)).findFirst().orElse(null);
        if (rootNode == null) {
            // If root node type was specified by user, verify that it was actually found
            if (rootNodeTypeCustom != null) {
                throw new CodeGenException("Root node type '%s' not found".formatted(rootNodeTypeCustom));
            } else if (rootNodeTypeJson != null) {
                // Should not happen since root node was specified in JSON, so type gen class should exist
                throw new AssertionError("Failed to find type gen class for root node '%s'".formatted(rootNodeTypeJson));
            }
        }

        return new GenElements(allNodeTypeGens, typedNodeSubtypes, rootNode);
    }

    @SafeVarargs
    private static <T> List<T> concat(Collection<? extends T>... collections) {
        int size = 0;
        for (var c : collections) {
            size += c.size();
        }
        List<T> list = new ArrayList<>(size);
        for (var c : collections) {
            list.addAll(c);
        }
        return list;
    }

    /**
     * {@return version information about this code generator}
     */
    public static Version version() {
        return Version.DEFAULT;
    }

    /**
     * Version information about the code generator.
     *
     * @see CodeGenerator#version()
     */
    public static final class Version {
        static final Version DEFAULT;

        static {
            var properties = new Properties();
            try (InputStream versionPropertiesStream = Version.class.getResourceAsStream("version.properties")) {
                if (versionPropertiesStream == null) {
                    throw new IllegalStateException("Version properties file does not exist");
                }
                properties.load(new InputStreamReader(versionPropertiesStream, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read version properties file", e);
            }

            String version = properties.getProperty("version");
            String gitRepoUrl = properties.getProperty("repository");
            String gitCommitId = properties.getProperty("commit");
            String jtreesitterVersion = properties.getProperty("jtreesitter");
            DEFAULT = new Version(version, gitRepoUrl, gitCommitId, jtreesitterVersion);
        }

        private final String version;
        private final String gitRepoUrl;
        private final String gitCommitId;
        private final String jtreesitterVersion;

        // Visible for testing
        Version(String version, String gitRepoUrl, String gitCommitId, String jtreesitterVersion) {
            this.version = version;
            this.gitRepoUrl = gitRepoUrl;
            this.gitCommitId = gitCommitId;
            this.jtreesitterVersion = jtreesitterVersion;
        }

        /**
         * Version number of the code generator.
         */
        public String version() {
            return version;
        }

        /**
         * URL of the Git repository which contains the code generator source.
         */
        public String gitRepository() {
            return gitRepoUrl;
        }

        /**
         * Git commit ID of the code generator, in {@link #gitRepository()}.
         * More accurate than {@link #version()}, especially when using snapshot versions.
         */
        public String gitCommitId() {
            return gitCommitId;
        }

        /**
         * Version number of jtreesitter ({@code io.github.tree-sitter:jtreesitter}) for which this code was generated.
         * Using a different version can lead to compilation errors for the generated code.
         */
        public String jtreesitterVersion() {
            return jtreesitterVersion;
        }

        @Override
        public String toString() {
            return "Version[" +
                "version=" + version + ", " +
                "commitId=" + gitCommitId + ']';
        }
    }
}
