package marcono1234.jtreesitter.type_gen;

import javax.lang.model.SourceVersion;
import java.util.*;
import java.util.stream.Stream;

import static marcono1234.jtreesitter.type_gen.internal.JavaNameGeneratorHelper.*;

/**
 * {@summary Generates names for generated source code elements (classes, fields, methods ...) based on the
 * information in {@code node-types.json}.}
 * Implementations do not have to use all arguments provided to the generation methods if they can create
 * unique and meaningful names without them. A default implementation is available through {@link DefaultNameGenerator}.
 *
 * <p><b>Important:</b> Implementations must make sure to create valid Java identifier names, and must not
 * produce conflicting names, for example by using a specific prefix or suffix to avoid conflicts.
 *
 * @see javax.lang.model.SourceVersion#isName(CharSequence, SourceVersion)
 */
// Usage note: Ideally all name generation methods are only called once for a set of certain arguments, and if the same
// name is needed in another part of the code generation (e.g. to call a generated method or refer to it from Javadoc),
// then the previously generated name is passed there instead of calling the name generator a second time
public interface NameGenerator {
    /**
     * For a node type generates the name of the Java class or interface.
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
     * class NodeMyNode implements TypedNode {  // @highlight substring="NodeMyNode"
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param typeName node type name
     * @return Java class name
     */
    String generateJavaTypeName(String typeName);

    /**
     * For a node type generates the name of the Java constant field storing the type name.
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
     * class NodeMyNode implements TypedNode {
     *     public static final String NODE_NAME = "my_node";  // @highlight substring="NODE_NAME"
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param typeName
     *      node type name; can be ignored since the constant name does not have to be unique
     *      across different node types
     * @return Java constant field name
     */
    String generateTypeNameConstant(String typeName);

    /**
     * For a node type generates the name of the Java constant field storing the numeric type ID.
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
     * class NodeMyNode implements TypedNode {
     *     public static final int NODE_ID = 0;  // @highlight substring="NODE_ID" @replace substring="0" replacement="..."
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param typeName
     *      node type name; can be ignored since the constant name does not have to be unique
     *      across different node types
     * @return Java constant field name
     * @see #generateTypeNameConstant(String)
     */
    String generateTypeIdConstant(String typeName);

    /**
     * For the children of a node type generates the name of the Java children interface.
     * This children interface is normally generated as nested class, so the name does not have to be
     * unique across different node types.
     *
     * <p>This children interface is only generated when multiple child types exist. If only a single
     * child type exists the getter method directly uses that type as return type, without additional child interface.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_node",
     *     "named": true,
     *     "children": {
     *       "multiple": false,
     *       "required": false,
     *       "types": [
     *         {
     *           "type": "child_type_a",  // @highlight substring="child_type_a"
     *           "named": true
     *         },
     *         {
     *           "type": "child_type_b",  // @highlight substring="child_type_b"
     *           "named": true
     *         }
     *       ]
     *     }
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class NodeMyNode implements TypedNode {
     *     interface Child extends TypedNode {  // @highlight substring="Child"
     *          //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param childrenTypesNames
     *      names of the children types; can be ignored because the class name does not have to be
     *      unique across different node types
     * @return Java interface name
     * @see #generateChildrenGetterName(String, List, boolean, boolean)
     */
    String generateChildrenTypesName(String parentTypeName, List<String> childrenTypesNames);

    /**
     * Same as {@link #generateFieldTokenTypeName(String, String, List)}, except for non-field children.
     *
     * <p><b>Note:</b> Currently this method is effectively unused. tree-sitter does not list non-named
     * children in the {@code node-types.json} file at the moment. As workaround an unspecific method for obtaining
     * non-named children is generated, see {@link #generateNonNamedChildrenGetterName(String, boolean, boolean)}.
     */
    default String generateChildrenTokenTypeName(String parentTypeName, List<String> tokenChildrenTypesNames) {
        throw new AssertionError("currently unused");
    }

    /**
     * Same as {@link #generateFieldTokenName(String, String, String, int)}, except for non-field children.
     *
     * <p><b>Note:</b> Currently this method is effectively unused. tree-sitter does not list non-named
     * children in the {@code node-types.json} file at the moment. As workaround an unspecific method for obtaining
     * non-named children is generated, see {@link #generateNonNamedChildrenGetterName(String, boolean, boolean)}.
     */
    default String generateChildrenTokenName(String parentTypeName, String tokenType, int index) {
        throw new AssertionError("currently unused");
    }

    /**
     * For the children of a node type generates the name of the getter method for obtaining the children from
     * the parent node.
     *
     * <h4>Example</h4>
     * {@snippet lang=json :
     * [
     *   {
     *     "type": "my_node",
     *     "named": true,
     *     "children": {
     *       "multiple": false,  // @highlight substring='"multiple": false'
     *       "required": false,  // @highlight substring='"required": false'
     *       "types": [
     *         {
     *           "type": "child_type_a",  // @highlight substring="child_type_a"
     *           "named": true
     *         },
     *         {
     *           "type": "child_type_b",  // @highlight substring="child_type_b"
     *           "named": true
     *         }
     *       ]
     *     }
     *   }
     * ]
     * }
     * For the {@code node-types.json} above, this method would generate for example:
     * {@snippet lang=java :
     * class NodeMyNode implements TypedNode {
     *     interface Child extends TypedNode {
     *          //...  // @replace substring="//" replacement=""
     *     }
     *
     *      public Child getChild() {  // @highlight substring="getChild"
     *          //...  // @replace substring="//" replacement=""
     *      }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param childrenTypesNames
     *      names of the children types; can be ignored because a node has only one group of children, so at
     *      most one getter method is generated
     * @param multiple whether the getter method may return more than one child
     * @param required whether the getter method returns at least one child
     * @return Java method name
     * @see #generateChildrenTypesName(String, List)
     */
    String generateChildrenGetterName(String parentTypeName, List<String> childrenTypesNames, boolean multiple, boolean required);

    /**
     * For the field of a node type generates the name of the Java constant field storing the name of the field.
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
     *         "required": true,
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
     * class NodeMyNode implements TypedNode {
     *     public static final String FIELD_CUSTOM = null;  // @highlight substring="FIELD_CUSTOM" @replace substring="null" replacement="..."
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param fieldName name of the field
     * @return Java constant field name
     */
    String generateFieldNameConstant(String parentTypeName, String fieldName);

    /**
     * For the field of a node type generates the name of the Java constant field storing the numeric ID of the field.
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
     *         "required": true,
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
     * class NodeMyNode implements TypedNode {
     *     public static final int FIELD_CUSTOM_ID = 0;  // @highlight substring="FIELD_CUSTOM_ID" @replace substring="0" replacement="..."
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param fieldName name of the field
     * @return Java constant field name
     * @see #generateFieldNameConstant(String, String)
     */
    String generateFieldIdConstant(String parentTypeName, String fieldName);

    /**
     * For the fields of a node type generates the name of the Java fields interface.
     * This fields interface is normally generated as nested class, so the name does not have to be
     * unique across different node types.
     *
     * <p>This fields interface is only generated when multiple field types exist. If only a single
     * field type exists the getter method directly uses that type as return type, without additional fields interface.
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
     *         "required": true,
     *         "types": [
     *           {
     *             "type": "field_type_a",
     *             "named": true
     *           },
     *           {
     *             "type": "field_type_b",
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
     * class NodeMyNode implements TypedNode {
     *     interface CustomField extends TypedNode {  // @highlight substring="CustomField"
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param fieldName name of the field
     * @return Java interface name
     * @see #generateFieldGetterName(String, String, boolean, boolean)
     */
    String generateFieldTypesName(String parentTypeName, String fieldName);

    /**
     * For the non-named field types of a node type generates the name of the Java class representing those field types.
     *
     * <p>Non-named node types are types with {@code "named": false} in the {@code node-types.json} file; these often
     * keywords or operator tokens such as {@code '+'}. These node types are referred to as "token" by the code
     * generator. The following code is generated for them:
     * <ul>
     *     <li>a class wrapping the underlying {@code Node} of these tokens (this name generator method here)
     *     <li>a nested enum which contains all possible token node types specified in the grammar (currently this name
     *     cannot be configured)
     *     <li>the enum constants representing the token node types ({@link #generateFieldTokenName(String, String, String, int)})
     * </ul>
     *
     * <p><b>Important:</b> This method should for the same field not generate the same Java class name as
     * {@link #generateFieldTypesName(String, String)}. Otherwise a name conflict can occur when a field has
     * named and non-named node types as values, in which case an additional Java superinterface with the name obtained
     * from {@code generateFieldTypesName} is generated.
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
     * class NodeMyNode implements TypedNode {
     *     class CustomFieldTokenType implements TypedNode {  // @highlight substring="CustomFieldTokenType"
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
     * @return Java class name
     */
    String generateFieldTokenTypeName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames);

    /**
     * For a non-named node type which is the potential value of a field, generates the token enum constant name.
     * See {@link #generateFieldTokenTypeName(String, String, List)} for more information.
     *
     * <p>The token name should either be chosen based on the context of this field, or it should be generic without
     * implying any behavior. For example for the token {@code '*'} it should prefer "ASTERISK" instead of "MULTIPLY",
     * because "MULTIPLY" implies what behavior this token has, even though it could mean something completely different
     * such as "wildcard".
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
     *             "type": "-",
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
     * class NodeMyNode implements TypedNode {
     *     class CustomFieldTokenType implements TypedNode {
     *         enum TokenType {
     *             PLUS("+"),  // @highlight substring="PLUS"
     *             MINUS("-"),
     *
     *             //...  // @replace substring="//" replacement=""
     *         }
     *
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type declaring the field
     * @param fieldName name of the field
     * @param tokenType name of the token node type
     * @param index
     *      index of the node type within the non-named types for the field; can be used to deduce a unique name in
     *      case this is not possible based on the other information
     * @return Java enum constant name
     */
    String generateFieldTokenName(String parentTypeName, String fieldName, String tokenType, int index);

    /**
     * For the field of a node type generates the name of the getter method for obtaining the field nodes from
     * the parent node.
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
     *         "required": true,
     *         "types": [
     *           {
     *             "type": "field_type_a",
     *             "named": true
     *           },
     *           {
     *             "type": "field_type_b",
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
     * class NodeMyNode implements TypedNode {
     *     interface CustomField extends TypedNode {
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     public CustomField getFieldCustom() {  // @highlight substring="getFieldCustom"
     *         //...  // @replace substring="//" replacement=""
     *     }
     *
     *     //...  // @replace substring="//" replacement=""
     * }
     * }
     *
     * @param parentTypeName name of the parent node type
     * @param fieldName name of the field
     * @param multiple whether the getter method may return more than one field node
     * @param required whether the getter method returns at least one field node
     * @return Java method name
     * @see #generateFieldTypesName(String, String)
     */
    String generateFieldGetterName(String parentTypeName, String fieldName, boolean multiple, boolean required);

    /**
     * For the non-named children of a node type generates the name of the getter method for obtaining the children
     * from the parent node.
     *
     * <p><b>Note:</b> Whether a node has any useful or even any non-named children at all depends on the node type
     * specified in the grammar. This information is currently not available in the {@code node-types.json} file.
     *
     * @param parentTypeName name of the parent node type
     * @param hasNamedChildren
     *      whether the parent also has named node type children; can be used to determine if this getter should be
     *      generated or not (by returning an empty {@code Optional})
     * @param hasFields
     *      whether the parent also has fields; can be used to determine if this getter should be generated or not
     *      (by returning an empty {@code Optional})
     * @return Java method name, or empty {@code Optional} if no getter should be generated
     */
    // TODO: Should this also allow customizing the Javadoc of the generated method? Currently it is quite generic
    Optional<String> generateNonNamedChildrenGetterName(String parentTypeName, boolean hasNamedChildren, boolean hasFields);

    /**
     * Generates for a non-named node type a "token" name, see {@link NameGenerator#generateFieldTokenName(String, String, String, int)}
     * for more details.
     *
     * <p>This interface is only used by {@link DefaultNameGenerator}; it is not needed when implementing
     * a custom {@link NameGenerator}.
     */
    interface TokenNameGenerator {
        /**
         * Name generator which chooses names automatically. In many cases the generated names will be pretty
         * generic and not that useful because the generator cannot know the meaning of the token and therefore
         * cannot choose a useful name.
         */
        TokenNameGenerator AUTOMATIC = fromMapping(Map.of(), false);

        /**
         * Generates the token name for a non-named node type of non-field children.
         * See {@link NameGenerator#generateChildrenTokenName(String, String, int)} for a detailed description.
         */
        default String generateChildrenTokenName(String parentTypeName, String tokenType, int index) {
            // See `NameGenerator#generateChildrenTokenName`
            throw new AssertionError("currently unused");
        }

        /**
         * Generates the token name for a non-named node type of a field child.
         * See {@link NameGenerator#generateFieldTokenName(String, String, String, int)} for a detailed description.
         */
        String generateFieldTokenName(String parentTypeName, String fieldName, String tokenType, int index);

        /**
         * Creates a token name generator based on the given mapping.
         *
         * @param tokenNameMapping
         *      Map in the form <code>Map&lt;<i>parentType</i>, Map&lt;<i>fieldName</i>, Map&lt;<i>tokenType</i>, <i>tokenName</i>&gt;&gt;&gt;</code>.
         *      That means, if a node type {@code MyNode} has a field {@code myField} which has a token of type {@code !=},
         *      then a map like the following could provide a mapping for it, giving the token the name "NOT_EQUALS":
         *      {@snippet lang=java :
         *      Map.of("MyNode", Map.of("myField", Map.of("!=", "NOT_EQUALS")))
         *      }
         *      The special key {@code ""} (empty string) can be used as fallback to match any node type respectively
         *      field name. This can be used to provide a mapping regardless of which node type or which of its fields
         *      uses the token. For example this gives all tokens of type {@code !=} the name "NOT_EQUALS":
         *      {@snippet lang = java:
         *      Map.of("", Map.of("", Map.of("!=", "NOT_EQUALS")))
         *      }
         * @param exhaustive
         *      Whether the mapping is expected to be exhaustive. If {@code true} but no mapping is present for a
         *      token, an exception is thrown. If {@code false} a token name is automatically generated instead.
         */
        static TokenNameGenerator fromMapping(Map<String, Map<String, Map<String, String>>> tokenNameMapping, boolean exhaustive) {
            Objects.requireNonNull(tokenNameMapping);

            return new TokenNameGenerator() {
                private String generateDefaultTokenName(String tokenType, int index) {
                    // Note: Avoid any default names which imply a certain usage, e.g. `*` should not be named "MULTIPLY"
                    // because it might have a different meaning in the grammar

                    if (tokenType.length() == 1) {
                        // Use Unicode character name
                        // TODO: Maybe this should be opt-in since those names are rather verbose, and might depend on Unicode data of JDK
                        //   (and could therefore break reproducibility); instead use hardcoded names for some common chars?
                        return Character.getName(tokenType.charAt(0))
                            .replace(' ', '_').replace('-', '_')
                            .toUpperCase(Locale.ROOT);
                    }
                    // If token type consists only of letters and whitespace, use token type as name
                    if (!tokenType.isBlank() && tokenType.matches("[\\sa-zA-Z]+")) {
                        return tokenType.replaceAll("\\s", "_")
                            .replace('-', '_')
                            .toUpperCase(Locale.ROOT);
                    }

                    // Use generic unique name as fallback
                    return "TOKEN_" + index;
                }

                @Override
                public String generateFieldTokenName(String parentTypeName, String fieldName, String tokenType, int index) {
                    // Fast path for automatically generating all token names
                    if (!exhaustive && tokenNameMapping.isEmpty()) {
                        return generateDefaultTokenName(tokenType, index);
                    }

                    // Uses empty string as fallback key (for type and field name) because it is not a valid type or field name,
                    // and can be represented in JSON when the `tokenNameMapping` is deserialized from a JSON file
                    String fallbackKey = "";
                    /*
                     * Order of preference:
                     * 1. typeName, fieldName
                     * 2. typeName, ""
                     * 3. "", fieldName  (not sure how useful this combination is, but support it for simplicity and consistency)
                     * 4. "", ""
                     */
                    // Uses Stream to lazily check fallbacks
                    var tokenName = Stream.of(parentTypeName, fallbackKey)
                        .flatMap(parent -> Stream.of(fieldName, fallbackKey)
                            .map(field -> tokenNameMapping.getOrDefault(parent, Map.of()).getOrDefault(field, Map.of()).get(tokenType))
                        )
                        .filter(Objects::nonNull)
                        .findFirst();
                    if (tokenName.isPresent()) {
                        return tokenName.get();
                    } else if (exhaustive) {
                        throw new IllegalArgumentException("Token type not mapped: type = " + parentTypeName + ", field = " + fieldName + ", token = " + tokenType);
                    } else {
                        return generateDefaultTokenName(tokenType, index);
                    }
                }
            };
        }
    }

    /**
     * Default name generator which is suitable for most use cases.
     *
     * <p>This implementation is stateless and thread-safe (if the given {@link TokenNameGenerator} is stateless as well).
     */
    class DefaultNameGenerator implements NameGenerator {
        private final TokenNameGenerator tokenNameGenerator;

        // Note: This specifically supports customizing token names because unlike the other names which might be good
        //   enough for many use cases, automatic token name generation would produce too generic / not useful names
        public DefaultNameGenerator(TokenNameGenerator tokenNameGenerator) {
            this.tokenNameGenerator = Objects.requireNonNull(tokenNameGenerator);
        }

        @Override
        public String generateJavaTypeName(String typeName) {
            // Prefix makes names consistent and prevents clashes with JDK names, e.g. `String`
            return "Node" + typeNameToUpperCamel(typeName);
        }

        @Override
        public String generateTypeNameConstant(String typeName) {
            return "TYPE_NAME";
        }

        @Override
        public String generateTypeIdConstant(String typeName) {
            return "TYPE_ID";
        }

        @Override
        public String generateChildrenTypesName(String parentTypeName, List<String> childrenTypesNames) {
            return "Child";
        }

        @Override
        public String generateChildrenTokenTypeName(String parentTypeName, List<String> tokenChildrenTypesNames) {
            return generateChildrenTypesName(parentTypeName, tokenChildrenTypesNames) + "TokenType";
        }

        @Override
        public String generateChildrenTokenName(String parentTypeName, String tokenType, int index) {
            return tokenNameGenerator.generateChildrenTokenName(parentTypeName, tokenType, index);
        }

        @Override
        public String generateChildrenGetterName(String parentTypeName, List<String> childrenTypesNames, boolean multiple, boolean required) {
            return multiple ? "getChildren" : "getChild";
        }

        @Override
        public String generateFieldNameConstant(String parentTypeName, String fieldName) {
            return "FIELD_" + convertToConstantName(fieldName);
        }

        @Override
        public String generateFieldIdConstant(String parentTypeName, String fieldName) {
            return "FIELD_" + convertToConstantName(fieldName) + "_ID";
        }

        private static String fieldNameAsSuffix(String fieldName) {
            return upperFirstChar(convertSnakeToCamelCase(fieldName));
        }

        @Override
        public String generateFieldTypesName(String parentTypeName, String fieldName) {
            // Prefix makes names consistent and prevents clashes with JDK names, e.g. `String`
            return "Field" + fieldNameAsSuffix(fieldName);
        }

        @Override
        public String generateFieldTokenTypeName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames) {
            // Prefix makes names consistent and prevents clashes with JDK names, e.g. `String`
            return "FieldToken" + fieldNameAsSuffix(fieldName);
        }

        @Override
        public String generateFieldTokenName(String parentTypeName, String fieldName, String tokenType, int index) {
            return tokenNameGenerator.generateFieldTokenName(parentTypeName, fieldName, tokenType, index);
        }

        @Override
        public String generateFieldGetterName(String parentTypeName, String fieldName, boolean multiple, boolean required) {
            // Prefix makes names consistent and prevents clashes with Object method names or other generated method names
            return "getField" + fieldNameAsSuffix(fieldName);
        }

        @Override
        public Optional<String> generateNonNamedChildrenGetterName(String parentTypeName, boolean hasNamedChildren, boolean hasFields) {
            // For now only generate if type has no fields, otherwise user likely included non-named children as fields
            // in their grammar in case they are relevant
            return hasFields ? Optional.empty() : Optional.of("getUnnamedChildren");
        }
    }
}
