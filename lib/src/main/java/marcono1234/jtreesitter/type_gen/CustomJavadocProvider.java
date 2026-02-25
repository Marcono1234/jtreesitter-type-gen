package marcono1234.jtreesitter.type_gen;

import java.util.List;
import java.util.Optional;

/**
 * Provides custom Javadoc text for the generated classes.
 *
 * <p>The Javadoc text is appended to the standard generated Javadoc. The text should use the traditional
 * Javadoc syntax, that is the syntax inside a <code>/** ... &ast;/</code> comment; Markdown syntax is not supported.
 * The Javadoc text will be added as is and no additional processing will be performed. It should therefore use line
 * breaks to wrap long lines, and use qualified type names when referring to Java types in {@code {@link ...}} tags
 * and similar.
 *
 * <p>The provider methods all receive a {@link TypeNameLookup} argument. This can be useful for generating Javadoc
 * {@code {@link ...}} tags referring to other generated classes.
 *
 * <p>The default implementation of the provider methods returns an empty {@link Optional}, therefore not
 * generating any custom Javadoc.
 *
 * <p><b>Important:</b> Do not include untrusted content in the Javadoc text; in the worst case it could allow an
 * attacker to inject arbitrary Java code in the generated code. Trying to prevent this by checking for a closing
 * <code>&ast;/</code> is error-prone because there are ways to circumvent such checks, for example by using
 * Unicode escapes.
 */
public interface CustomJavadocProvider {
    /**
     * For a node type name looks up the name of the generated Java type.
     */
    interface TypeNameLookup {
        /**
         * For a node type name returns the name of the generated Java type.
         *
         * @param nodeType name of the node type, as defined in the Tree-sitter grammar
         * @return Java type name of the generated class, or an empty {@link Optional} if the node type is unknown
         * @see TypeName#qualifiedSourceName()
         */
        Optional<TypeName> getTypeName(String nodeType);
    }

    /**
     * Custom Javadoc for the generated {@code TypedTree} class.
     *
     * <p>{@code TypedTree} is a type-safe wrapper around a jtreesitter {@code Tree}. The class is only generated if
     * the grammar root node type is specified.
     */
    default Optional<String> forTypedTree(TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the generated {@code TypedNode} interface.
     *
     * <p>{@code TypedNode} is the base type for all typed node classes. Those classes wrap the underlying
     * jtreesitter {@code Node} objects.
     */
    default Optional<String> forTypedNode(TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the generated class or interface for the given node type.
     *
     * @param nodeType name of the node type, as defined in the Tree-sitter grammar
     */
    default Optional<String> forNodeType(String nodeType, TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the getter method which retrieves the children of the given node type.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param childrenNodeTypes
     *      names of the children node types
     * @see NameGenerator#generateChildrenGetterName(String, List, boolean, boolean)
     */
    default Optional<String> forNodeChildrenGetter(String parentNodeType, List<String> childrenNodeTypes, TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the generated interface for the children types of the given node type.
     *
     * <p>That interface for the children is generated as common superinterface when multiple possible children types
     * are defined by the Tree-sitter grammar, see also {@link NameGenerator#generateChildrenTypesName(String, List)}.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param childrenNodeTypes
     *      names of the children node types
     */
    // TODO: Not sure if this is really worth it, since that Java type does not directly correspond to something
    //   in the grammar, and therefore documenting it might not be useful?
    default Optional<String> forNodeChildrenInterface(String parentNodeType, List<String> childrenNodeTypes, TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the generated class for the 'token' children types (= non-named types) of the given node type.
     *
     * <p>See {@link NameGenerator#generateChildrenTokenTypeName(String, List)} for details.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param tokenTypesNames
     *      names of all token node types
     */
    // TODO: Not sure if this is really worth it, since that Java type does not directly correspond to something
    //   in the grammar, and therefore documenting it might not be useful?
    default Optional<String> forNodeChildrenTokenClass(String parentNodeType, List<String> tokenTypesNames, TypeNameLookup typeNameLookup) {
        throw new AssertionError("currently unused; see also NameGenerator");
    }

    /**
     * Custom Javadoc for the constant representing a specific 'token' children type (= non-named type) of the given node type.
     *
     * <p>See {@link NameGenerator#generateChildrenTokenName(String, String, int)} for details.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param tokenType
     *      name of the token node type
     */
    default Optional<String> forNodeChildrenToken(String parentNodeType, String tokenType, TypeNameLookup typeNameLookup) {
        throw new AssertionError("currently unused; see also NameGenerator");
    }

    /**
     * Custom Javadoc for the getter method which retrieves the nodes of the given node type field.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param fieldName
     *      name of the field
     * @see NameGenerator#generateFieldGetterName(String, String, boolean, boolean)
     */
    default Optional<String> forNodeFieldGetter(String parentNodeType, String fieldName, TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the generated interface for the field types of the given node type field.
     *
     * <p>That interface for the field is generated as common superinterface when multiple possible field types
     * are defined by the Tree-sitter grammar, see also {@link NameGenerator#generateFieldTypesName(String, String)}.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param fieldName
     *      name of the field
     */
    // TODO: Not sure if this is really worth it, since that Java type does not directly correspond to something
    //   in the grammar, and therefore documenting it might not be useful?
    default Optional<String> forNodeFieldInterface(String parentNodeType, String fieldName, TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the generated class for the 'token' field types (= non-named types) of the given node type field.
     *
     * <p>See {@link NameGenerator#generateFieldTokenTypeName(String, String, List)} for details.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param fieldName
     *      name of the field
     * @param tokenTypesNames
     *      names of all token node types
     */
    // TODO: Not sure if this is really worth it, since that Java type does not directly correspond to something
    //   in the grammar, and therefore documenting it might not be useful?
    default Optional<String> forNodeFieldTokenClass(String parentNodeType, String fieldName, List<String> tokenTypesNames, TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }

    /**
     * Custom Javadoc for the constant representing a specific 'token' field type (= non-named type) of the given node type field.
     *
     * <p>See {@link NameGenerator#generateFieldTokenName(String, String, String, int)} for details.
     *
     * @param parentNodeType
     *      name of the parent node type, as defined in the Tree-sitter grammar
     * @param fieldName
     *      name of the field
     * @param tokenType
     *      name of the token node type
     */
    default Optional<String> forNodeFieldToken(String parentNodeType, String fieldName, String tokenType, TypeNameLookup typeNameLookup) {
        return Optional.empty();
    }
}
