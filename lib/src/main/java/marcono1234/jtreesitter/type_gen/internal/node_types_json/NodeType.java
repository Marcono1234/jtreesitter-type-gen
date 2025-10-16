package marcono1234.jtreesitter.type_gen.internal.node_types_json;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class NodeType extends Type {
    /** If absent, can be either {@code null} or empty */
    @Nullable
    public Map<String, ChildType> fields;

    @Nullable
    public ChildType children;

    @Nullable
    public List<Type> subtypes;

    /**
     * Whether the node type is a root node; since tree-sitter 0.24.0, see
     * https://github.com/tree-sitter/tree-sitter/pull/3615
     */
    public boolean root = false;
}
