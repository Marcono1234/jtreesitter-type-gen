package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;

/**
 * For a node type name gets information about the corresponding generated Java type.
 */
@FunctionalInterface
public interface NodeTypeLookup {
    @Nullable GenNodeType getNodeTypeNullable(String typeName);

    default GenNodeType getNodeType(String typeName) throws NoSuchElementException {
        var nodeType = getNodeTypeNullable(typeName);
        if (nodeType == null) {
            throw new NoSuchElementException("Unknown type name: " + typeName + "\nPotential tree-sitter bug https://github.com/tree-sitter/tree-sitter/issues/1654");
        }
        return nodeType;
    }
}
