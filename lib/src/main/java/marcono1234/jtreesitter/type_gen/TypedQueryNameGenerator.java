package marcono1234.jtreesitter.type_gen;

import javax.lang.model.SourceVersion;
import java.util.List;
import java.util.Objects;

import static marcono1234.jtreesitter.type_gen.internal.JavaNameGeneratorHelper.*;

/**
 * {@summary Generates names for generated source code elements (classes, fields, methods ...) of the
 * 'typed query' code, based on the information in {@code node-types.json}.}
 * Implementations do not have to use all arguments provided to the generation methods if they can create
 * unique and meaningful names without them. A default implementation is available through {@link #createDefault(NameGenerator)}.
 *
 * <p><b>Important:</b> Implementations must make sure to create valid Java identifier names, and must not
 * produce conflicting names, for example by using a specific prefix or suffix to avoid conflicts.
 *
 * @see javax.lang.model.SourceVersion#isName(CharSequence, SourceVersion)
 */
// Usage note: Ideally all name generation methods are only called once for a set of certain arguments, and if the same
// name is needed in another part of the code generation (e.g. to call a generated method or refer to it from Javadoc),
// then the previously generated name is passed there instead of calling the name generator a second time
public interface TypedQueryNameGenerator {
    /**
     * For a node type generates the name of the Java class representing that node type in the
     * 'typed query' builder API.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_node",  // @highlight substring="my_node"
     *     "named": true
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class QueryNodeMyNode {  // @highlight substring="QueryNodeMyNode"
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param typeName node type name
     * @return Java class name
     */
    String generateBuilderClassName(String typeName);

    /**
     * For a node type generates the name of the Java builder method representing that node type in the
     * 'typed query' builder API.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_node",  // @highlight substring="my_node"
     *     "named": true
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class Builder {
     *     public QueryNodeMyNode myNode() {  // @highlight substring="myNode"
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param typeName node type name
     * @return Java method name
     * @see #generateBuilderClassName(String)
     */
    String generateBuilderMethodName(String typeName);

    /**
     * For a node type which has a supertype generates the name of the Java method which allows obtaining
     * a builder object for that node type as subtype of the supertype.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_supernode",  // @highlight substring="my_supernode"
     *     "named": true,
     *     "subtypes": [
     *       {
     *         "type": "my_node",  // @highlight substring="my_node"
     *         "named": true
     *       }
     *     ]
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class QueryNodeMyNode {
     *     public QueryNode asSubtypeOfMySupernode() {  // @highlight substring="asSubtypeOfMySupernode"
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param typeName node type name
     * @param supertypeName node supertype name
     * @return Java method name
     */
    String generateAsSubtypeMethodName(String typeName, String supertypeName);

    /**
     * For the field of a node type generates the name of a Java method for specifying additional match requirements
     * of the 'typed query' for that field.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_node",
     *     "named": true,
     *     "fields": {
     *       "custom": {  // @highlight substring="custom"
     *         "multiple": false,
     *         "required": false,
     *         "types": [
     *           {
     *             "type": "field_type_a",
     *             "named": true
     *           }
     *         ]
     *       }
     *     }
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class QueryNodeMyNode {
     *     public QueryNodeMyNode withFieldCustom(QueryNode field) {  // @highlight substring="withFieldCustom"
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param fieldName name of the field
     * @return Java method name
     * @see #generateWithoutFieldMethodName(String, String)
     */
    String generateWithFieldMethodName(String parentTypeName, String fieldName);

    /**
     * For the field of a node type generates the name of a Java method for specifying for the 'typed query' that
     * the field should not be present.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_node",
     *     "named": true,
     *     "fields": {
     *       "custom": {  // @highlight substring="custom"
     *         "multiple": false,
     *         "required": false,
     *         "types": [
     *           {
     *             "type": "field_type_a",
     *             "named": true
     *           }
     *         ]
     *       }
     *     }
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class QueryNodeMyNode {
     *     public QueryNodeMyNode withoutFieldCustom() {  // @highlight substring="withoutFieldCustom"
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param fieldName name of the field
     * @return Java method name
     * @see #generateWithFieldMethodName(String, String)
     */
    String generateWithoutFieldMethodName(String parentTypeName, String fieldName);

    /**
     * For the non-named field types of a node type generates the name of a Java method which converts the
     * token enum constants to the corresponding 'typed query' builder objects.
     * See also {@link NameGenerator#generateFieldTokenTypeName(String, String, List)}.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_node",
     *     "named": true,
     *     "fields": {
     *       "operator": {  // @highlight substring="operator"
     *         "multiple": false,
     *         "required": true,
     *         "types": [
     *           {
     *             "type": "+",  // @highlight substring="+"
     *             "named": false
     *           },
     *           {
     *             "type": "-",  // @highlight substring="-"
     *             "named": false
     *           }
     *         ]
     *       }
     *     }
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class QueryNodeMyNode {
     *     public static QueryNode fieldTokenOperator(TokenType tokenEnum) {  // @highlight substring="fieldTokenOperator"
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type declaring the field
     * @param fieldName name of the field
     * @param tokenFieldTypesNames
     *      names of all token node types for the field; depending on the use case they can potentially be used to
     *      deduce a name
     * @return Java method name
     * @see NameGenerator#generateFieldTokenTypeName(String, String, List)
     */
    String generateFieldTokenMethodName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames);

    /**
     * Same as {@link #generateFieldTokenMethodName(String, String, List)}, except for non-field children.
     *
     * <p><b>Note:</b> Currently this method is effectively unused. tree-sitter does not list non-named
     * children in the {@code node-types.json} file at the moment.
     */
    default String generateChildTokenMethodName(String parentTypeName, List<String> tokenChildrenTypesNames) {
        throw new AssertionError("currently unused");
    }

    /**
     * Creates a default 'typed query' name generator, which is suitable for most use cases.
     * The name generator is based partially on the given {@link NameGenerator}.
     */
    static TypedQueryNameGenerator createDefault(NameGenerator nameGenerator) {
        Objects.requireNonNull(nameGenerator);

        // Dedicated record class to have useful `toString` and `equals`
        record DefaultTypedQueryNameGenerator(NameGenerator nameGenerator) implements TypedQueryNameGenerator {
            @Override
            public String generateBuilderClassName(String typeName) {
                // Prefix the name with a "Q" (plus the prefix (if any) of the general nameGenerator)
                // for conciseness and to tell them apart from non-query classes
                return "Q" + nameGenerator.generateJavaTypeName(typeName);
            }

            @Override
            public String generateBuilderMethodName(String typeName) {
                // Just lowercase the first char so that the type name is suitable as method name;
                // this assumes that the Java type name starts with a unique prefix
                return lowerFirstChar(nameGenerator.generateJavaTypeName(typeName));
            }

            @Override
            public String generateAsSubtypeMethodName(String typeName, String supertypeName) {
                return "asSubtypeOf" + nameGenerator.generateJavaTypeName(supertypeName);
            }

            private static String fieldNameAsSuffix(String fieldName) {
                return upperFirstChar(convertSnakeToCamelCase(fieldName));
            }

            @Override
            public String generateWithFieldMethodName(String parentTypeName, String fieldName) {
                return "withField" + fieldNameAsSuffix(fieldName);
            }

            @Override
            public String generateWithoutFieldMethodName(String parentTypeName, String fieldName) {
                return "withoutField" + fieldNameAsSuffix(fieldName);
            }

            @Override
            public String generateFieldTokenMethodName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames) {
                return "fieldToken" + fieldNameAsSuffix(fieldName);
            }

            @Override
            public String generateChildTokenMethodName(String parentTypeName, List<String> tokenChildrenTypesNames) {
                return "childToken";
            }
        }

        return new DefaultTypedQueryNameGenerator(nameGenerator);
    }
}
