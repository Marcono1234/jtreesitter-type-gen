package marcono1234.jtreesitter.type_gen.internal.node_types_json;

@SuppressWarnings("NotNullFieldNotInitialized")
public class Type {
    public String type;
    public boolean named;
    /**
     * Whether the node type is an extra node; since tree-sitter 0.25.0, see
     * https://github.com/tree-sitter/tree-sitter/pull/4116
     */
    public boolean extra = false;
}
