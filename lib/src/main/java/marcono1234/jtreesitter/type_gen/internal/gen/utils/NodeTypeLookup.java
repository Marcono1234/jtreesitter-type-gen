package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;

import java.util.NoSuchElementException;

/**
 * For a node type name gets information about the corresponding generated Java type.
 */
@FunctionalInterface
public interface NodeTypeLookup {
    GenNodeType getNodeType(String typeName) throws NoSuchElementException;
}
