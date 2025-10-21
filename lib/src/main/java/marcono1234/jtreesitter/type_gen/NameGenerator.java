package marcono1234.jtreesitter.type_gen;

import javax.lang.model.SourceVersion;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@summary Generates names for generated source code elements (classes, fields, methods ...) based on the
 * information in {@code node-types.json}.}
 * Implementations do not have to use all arguments provided to the generation methods if they can create
 * unique and meaningful names without them. A default implementation is available as {@link #DEFAULT}.
 *
 * <p><b>Important:</b> Implementations must make sure to create valid Java identifier names, and must not
 * produce conflicting names, for example by using a specific prefix or suffix to avoid conflicts.
 *
 * @see javax.lang.model.SourceVersion#isName(CharSequence, SourceVersion)
 */
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
     * @param childrenTypeNames
     *      names of the children types; can be ignored because the class name does not have to be
     *      unique across different node types
     * @return Java interface name
     * @see #generateChildrenGetterName(String, List, boolean, boolean)
     */
    String generateChildrenTypesName(String parentTypeName, List<String> childrenTypeNames);

    /**
     * Same as {@link #generateFieldTokenTypeName(String, String, List)}, except for a non-field child.
     *
     * <p><b>Note:</b> Currently this method is effectively unused. tree-sitter does not list non-named
     * children in the {@code node-types.json} file at the moment. As workaround an unspecific method for obtaining
     * non-named children is generated, see {@link #generateNonNamedChildrenGetter(String, boolean, boolean)}.
     */
    default String generateChildrenTokenTypeName(String parentTypeName, List<String> tokenChildrenTypeNames) {
        throw new AssertionError("currently unused");
    }

    /**
     * Same as {@link #generateFieldTokenName(String, String, String, int)}, except for a non-field child.
     *
     * <p><b>Note:</b> Currently this method is effectively unused. tree-sitter does not list non-named
     * children in the {@code node-types.json} file at the moment. As workaround an unspecific method for obtaining
     * non-named children is generated, see {@link #generateNonNamedChildrenGetter(String, boolean, boolean)}.
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
     * @param childrenTypeNames
     *      names of the children types; can be ignored because a node has only one group of children, so at
     *      most one getter method is generated
     * @param multiple whether the getter method may return more than one child
     * @param required whether the getter method returns at least one child
     * @return Java method name
     * @see #generateChildrenTypesName(String, List)
     */
    String generateChildrenGetterName(String parentTypeName, List<String> childrenTypeNames, boolean multiple, boolean required);

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
     *             "type": "field_type_a",  // @highlight substring="field_type_a"
     *             "named": true
     *           },
     *           {
     *             "type": "field_type_b",  // @highlight substring="field_type_b"
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
     * @param fieldTypesNames all field types
     * @return Java interface name
     * @see #generateFieldGetterName(String, String, List, boolean, boolean)
     */
    String generateFieldTypesName(String parentTypeName, String fieldName, List<String> fieldTypesNames);

    /**
     * For the non-named field types of a node type generates the name of the Java class representing those field types.
     *
     * <p>Non-named node types are types with {@code "named": false} in the {@code node-types.json} file; these often
     * keywords or operator tokens such as {@code '+'}. These node types are referred to as "token" by the code
     * generator. The following code is generated for them:
     * <ul>
     *     <li>a class wrapping the underlying {@code Node} of these tokens (this name generator method here)
     *     <li>a nested enum which contains all possible token node types specified in the grammar (current this name
     *     cannot be configured)
     *     <li>the enum constants representing the token node types ({@link #generateFieldTokenName(String, String, String, int)})
     * </ul>
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
     *      names of all token node types; depending on the use case they can potentially be used to deduce a name
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
     *             "type": "field_type_a",  // @highlight substring="field_type_a"
     *             "named": true
     *           },
     *           {
     *             "type": "field_type_b",  // @highlight substring="field_type_b"
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
     * @param fieldTypesNames all field types
     * @param multiple whether the getter method may return more than one field node
     * @param required whether the getter method returns at least one field node
     * @return Java method name
     * @see #generateFieldTypesName(String, String, List)
     */
    String generateFieldGetterName(String parentTypeName, String fieldName, List<String> fieldTypesNames, boolean multiple, boolean required);

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
    // TODO should this also allow customizing the Javadoc of the generated method? Currently it is quite generic
    Optional<String> generateNonNamedChildrenGetter(String parentTypeName, boolean hasNamedChildren, boolean hasFields);

    /**
     * Default name generator, suitable for most use cases.
     */
    NameGenerator DEFAULT = new NameGenerator() {
        /**
         * Converts {@code snake_case} to {@code camelCase}.
         */
        private String convertSnakeToCamelCase(String s) {
            StringBuilder stringBuilder = new StringBuilder(s.length());
            int nextStartIndex = 0;

            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '_') {
                    stringBuilder.append(s, nextStartIndex, i);
                    i++;
                    if (i < s.length()) {
                        stringBuilder.append(Character.toUpperCase(s.charAt(i)));
                    }
                    nextStartIndex = i + 1; // skip uppercased char
                }
            }

            // Add remainder of string
            stringBuilder.append(s, nextStartIndex, s.length());
            return stringBuilder.toString();
        }

        /**
         * Converts the string to a Java constant name, e.g. {@code MY_CONSTANT}.
         */
        private String convertToConstantName(String s) {
            StringBuilder stringBuilder = new StringBuilder(s.length());
            int nextStartIndex = 0;
            boolean wasLower = false;

            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                boolean isLower = Character.isLowerCase(c);
                // Check for switch from lower to upper (respectively lower letter to non-letter)
                if (wasLower && !isLower) {
                    stringBuilder.append(s, nextStartIndex, i);
                    stringBuilder.append('_');

                    if (c == '_' || c == '-') {
                        i++;
                    }
                    nextStartIndex = i;
                }
                wasLower = isLower;
            }

            // Add remainder of string
            stringBuilder.append(s, nextStartIndex, s.length());
            return stringBuilder.toString().toUpperCase(Locale.ROOT);
        }

        private String upperFirstChar(String s) {
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Empty string is not supported");
            }

            // For simplicity just use first char, don't consider supplementary code points or special Unicode
            // case conversion rules here
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }

        @Override
        public String generateJavaTypeName(String typeName) {
            // Remove leading '_', for hidden types
            if (typeName.startsWith("_")) {
                typeName = typeName.substring(1);
            }

            // Prefix makes names consistent and prevents clashes with JDK names, e.g. `String`
            return "Node" + upperFirstChar(convertSnakeToCamelCase(typeName));
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
        public String generateChildrenTypesName(String parentTypeName, List<String> childrenTypeNames) {
            return "Child";
        }

        @Override
        public String generateChildrenTokenTypeName(String parentTypeName, List<String> tokenChildrenTypeNames) {
            return generateChildrenTypesName(parentTypeName, tokenChildrenTypeNames) + "TokenType";
        }

        @Override
        public String generateChildrenTokenName(String parentTypeName, String tokenType, int index) {
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
            return "TOKEN_" + index;
        }

        @Override
        public String generateChildrenGetterName(String parentTypeName, List<String> childrenTypeNames, boolean multiple, boolean required) {
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

        @Override
        public String generateFieldTypesName(String parentTypeName, String fieldName, List<String> fieldTypeNames) {
            // TODO: Change to "FieldType<...>" / "Field<...>"?
            // Suffix makes names consistent and prevents clashes with JDK names, e.g. `String`
            return upperFirstChar(convertSnakeToCamelCase(fieldName)) + "Type";
        }

        @Override
        public String generateFieldTokenTypeName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames) {
            // TODO: Change to "FieldTokenType<...>" / "FieldToken<...>"?
            // Suffix makes names consistent and prevents clashes with JDK names, e.g. `String`
            return upperFirstChar(convertSnakeToCamelCase(fieldName)) + "TokenType";
        }

        @Override
        public String generateFieldTokenName(String parentTypeName, String fieldName, String tokenType, int index) {
            return generateChildrenTokenName(parentTypeName, tokenType, index);
        }

        @Override
        public String generateFieldGetterName(String parentTypeName, String fieldName, List<String> fieldTypesNames, boolean multiple, boolean required) {
            // Prefix makes names consistent and prevents clashes with Object method names or other generated method names
            return "getField" + upperFirstChar(convertSnakeToCamelCase(fieldName));
        }

        @Override
        public Optional<String> generateNonNamedChildrenGetter(String parentTypeName, boolean hasNamedChildren, boolean hasFields) {
            // For now only generate if type has no fields, otherwise user likely included non-named children as fields
            // in their grammar in case they are relevant
            return hasFields ? Optional.empty() : Optional.of("getUnnamedChildren");
        }
    };
}
