package marcono1234.jtreesitter.type_gen.cli.json_configs;

import com.fasterxml.jackson.annotation.JsonProperty;
import marcono1234.jtreesitter.type_gen.CustomJavadocProvider;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * JSON config for custom Javadoc to be added to the generated code.
 *
 * @see marcono1234.jtreesitter.type_gen.CustomJavadocProvider
 */
public class CustomJavadocConfig {
    public static class NodeTypeConfig {
        @JsonProperty("javadoc")
        @Nullable
        public String javadoc;

        @JsonProperty("children")
        @Nullable
        public ChildrenConfig children;

        @JsonProperty("fields")
        @Nullable
        public SequencedMap<String, ChildrenConfig> fields;
    }

    public static class ChildrenConfig {
        @JsonProperty("getter-javadoc")
        @Nullable
        public String getter;

        @JsonProperty("interface-javadoc")
        @Nullable
        public String type;

        // For non-field children these token configs are currently effectively unused, see `NameGenerator#generateChildrenTokenTypeName`

        @JsonProperty("token-class-javadoc")
        @Nullable
        public String tokenClass;

        /** Map from token type to its custom Javadoc */
        @JsonProperty("tokens-javadoc")
        @Nullable
        public SequencedMap<String, String> tokens;
    }

    @JsonProperty("typed-tree-javadoc")
    @Nullable
    public String typedTree;

    @JsonProperty("typed-node-javadoc")
    @Nullable
    public String typedNode;

    @JsonProperty("node-types")
    @Nullable
    public Map<String, NodeTypeConfig> nodeTypes;

    public CustomJavadocProvider asJavadocProvider() {
        return new CustomJavadocProvider() {
            @Override
            public Optional<String> forTypedTree(TypeNameLookup typeNameLookup) {
                return Optional.ofNullable(typedTree);
            }

            @Override
            public Optional<String> forTypedNode(TypeNameLookup typeNameLookup) {
                return Optional.ofNullable(typedNode);
            }

            private Optional<NodeTypeConfig> nodeTypeConfig(String nodeType) {
                if (nodeTypes == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(nodeTypes.get(nodeType));
            }

            @Override
            public Optional<String> forNodeType(String nodeType, TypeNameLookup typeNameLookup) {
                return nodeTypeConfig(nodeType)
                    .map(c -> c.javadoc);
            }

            private Optional<ChildrenConfig> childrenConfig(String nodeType) {
                return nodeTypeConfig(nodeType)
                    .map(n -> n.children);
            }

            @Override
            public Optional<String> forNodeChildrenGetter(String parentNodeType, List<String> childrenNodeTypes, TypeNameLookup typeNameLookup) {
                return childrenConfig(parentNodeType)
                    .map(c -> c.getter);
            }

            @Override
            public Optional<String> forNodeChildrenInterface(String parentNodeType, List<String> childrenNodeTypes, TypeNameLookup typeNameLookup) {
                return childrenConfig(parentNodeType)
                    .map(c -> c.type);
            }

            @Override
            public Optional<String> forNodeChildrenTokenClass(String parentNodeType, List<String> tokenTypesNames, TypeNameLookup typeNameLookup) {
                return childrenConfig(parentNodeType)
                    .map(c -> c.tokenClass);
            }

            @Override
            public Optional<String> forNodeChildrenToken(String parentNodeType, String tokenType, TypeNameLookup typeNameLookup) {
                return childrenConfig(parentNodeType)
                    .map(c -> c.tokens)
                    .map(t -> t.get(tokenType));
            }

            private Optional<ChildrenConfig> fieldConfig(String nodeType, String fieldName) {
                return nodeTypeConfig(nodeType)
                    .map(c -> c.fields)
                    .map(f -> f.get(fieldName));
            }

            @Override
            public Optional<String> forNodeFieldGetter(String parentNodeType, String fieldName, TypeNameLookup typeNameLookup) {
                return fieldConfig(parentNodeType, fieldName)
                    .map(f -> f.getter);
            }

            @Override
            public Optional<String> forNodeFieldInterface(String parentNodeType, String fieldName, TypeNameLookup typeNameLookup) {
                return fieldConfig(parentNodeType, fieldName)
                    .map(f -> f.type);
            }

            @Override
            public Optional<String> forNodeFieldTokenClass(String parentNodeType, String fieldName, List<String> tokenTypesNames, TypeNameLookup typeNameLookup) {
                return fieldConfig(parentNodeType, fieldName)
                    .map(f -> f.tokenClass);
            }

            @Override
            public Optional<String> forNodeFieldToken(String parentNodeType, String fieldName, String tokenType, TypeNameLookup typeNameLookup) {
                return fieldConfig(parentNodeType, fieldName)
                    .map(f -> f.tokens)
                    .map(t -> t.get(tokenType));
            }
        };
    }

    public static CustomJavadocConfig readFromFile(Path file) throws JacksonException {
        return ObjectMappers.verboseJsonMapper.readValue(file, CustomJavadocConfig.class);
    }
}
