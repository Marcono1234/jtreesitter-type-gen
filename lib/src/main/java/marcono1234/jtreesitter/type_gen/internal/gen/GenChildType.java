package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import marcono1234.jtreesitter.type_gen.internal.gen.common_classes.NodeUtilsGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.common_classes.TypedNodeInterfaceGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.NodeTypeLookup;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.Type;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Type of a {@link GenChildren}.
 *
 * <p>Use {@link #create} to create instances.
 */
sealed interface GenChildType {
    /** Internal interface for child types variants which generate a new Java type. */
    sealed interface GenChildTypeAsNewJavaType extends GenChildType {
        default ClassName createNestedJavaTypeName(String javaEnclosingName, String javaName, CodeGenHelper codeGenHelper) {
            if (isChildTypeAsTopLevel(codeGenHelper)) {
                // Create top-level class name
                return codeGenHelper.createOwnClassName(javaEnclosingName + "$" + javaName);
            } else {
                // Create nested class name
                return codeGenHelper.createOwnClassName(javaEnclosingName, javaName);
            }
        }

        boolean isChildTypeAsTopLevel(CodeGenHelper codeGenHelper);
    }

    /**
     * Gets the Java type name for this type.
     */
    ClassName createJavaTypeName(CodeGenHelper codeGenHelper);

    /**
     * Generates code for converting jtreesitter {@code List<Node>} {@code childrenVar} to a
     * {@code List<T extends TypedNode>} and creates a local variable {@code resultVar} with the result.
     */
    void addConvertingCall(MethodSpec.Builder builder, CodeGenHelper codeGenHelper, String childrenVar, String resultVar);

    /**
     * Returns whether this child type generates a Java interface which directly or indirectly
     * refers to {@code type}. To avoid infinite recursion, already seen types are tracked in {@code seenTypes}.
     *
     * @see GenNodeType#refersToType(GenRegularNodeType, Set)
     */
    boolean refersToTypeThroughInterface(GenRegularNodeType type, Set<GenJavaType> seenTypes);

    record JavaTypeConfig(TypeSpec.Builder type, boolean asTopLevel) {}
    List<JavaTypeConfig> generateJavaTypes(CodeGenHelper codeGenHelper);

    interface ChildTypeNameGenerator {
        String generateInterfaceName(List<String> allChildTypes);
        String generateTokenClassName(List<String> tokenTypeNames);
        String generateTokenTypeConstantName(String tokenType, int index);
    }

    static GenChildType create(GenRegularNodeType enclosingNodeType, List<Type> types, ChildTypeNameGenerator nameGenerator, NodeTypeLookup nodeTypeLookup, Consumer<GenJavaType> additionalTypedNodeSubtypeCollector) {
        if (types.isEmpty()) {
            throw new IllegalArgumentException("Empty types");
        }

        List<String> nonNamedTypes = new ArrayList<>();
        List<Type> namedTypes = new ArrayList<>();
        for (var type : types) {
            if (type.named) {
                namedTypes.add(type);
            } else {
                nonNamedTypes.add(type.type);
            }
        }

        /*
         * The following cases are covered:
         * 1. Only single named type -> use it as child type
         * 2. Only unnamed types -> generate a Java class for them and use it as child type
         * 3. Only named types -> generate a common interface for them and use it as child type
         * 4. Named & unnamed types -> similar to 3, but also generate a class as in 2 and let it
         *                             implement the interface as well
         *
         * Cases 3 and 4 are actually the same code path below, which creates a `MultiChildType`.
         */

        if (nonNamedTypes.isEmpty() && namedTypes.size() == 1) {
            return new SingleChildType(nodeTypeLookup.getNodeType(namedTypes.getFirst().type));
        }

        UnnamedTokensChildType tokensChildType = null;
        if (!nonNamedTypes.isEmpty()) {
            String javaName = nameGenerator.generateTokenClassName(nonNamedTypes);

            Map<String, String> tokensToJavaConstants = new LinkedHashMap<>();
            for (int i = 0; i < nonNamedTypes.size(); i++) {
                String tokenType = nonNamedTypes.get(i);
                String constantName = nameGenerator.generateTokenTypeConstantName(tokenType, i);
                var existing = tokensToJavaConstants.put(tokenType, constantName);
                if (existing != null) {
                    throw new IllegalArgumentException("Duplicate non-named type '%s' for enclosing type '%s'".formatted(tokenType, enclosingNodeType.getTypeName()));
                }
            }

            tokensChildType = new UnnamedTokensChildType(enclosingNodeType, javaName, tokensToJavaConstants);

            if (namedTypes.isEmpty()) {
                // Only add tokensChildType at this place, if `namedTypes.isEmpty()`; otherwise if it is part of MultiChildType,
                // then it will transitively implement the TypedNode interface
                additionalTypedNodeSubtypeCollector.accept(tokensChildType);

                return tokensChildType;
            }
        }

        // Note: Uses `types` here (instead of `namedTypes`), which potentially includes non-named types as well
        List<String> allTypeNames = types.stream().map(t -> t.type).toList();
        String javaName = nameGenerator.generateInterfaceName(allTypeNames);

        List<String> namedTypeNames = namedTypes.stream().map(t -> t.type).toList();
        List<GenNodeType> genTypes = namedTypeNames.stream().map(nodeTypeLookup::getNodeType).toList();
        MultiChildType childType = new MultiChildType(genTypes, tokensChildType, enclosingNodeType, javaName);
        additionalTypedNodeSubtypeCollector.accept(childType);
        genTypes.forEach(t -> t.addInterfaceToImplement(childType));
        return childType;
    }

    /**
     * Adds a mapper variable with the given {@code mapperVar} name, which is used by the {@link NodeUtilsGenerator}
     * methods to convert from a jtreesitter Node to a TypedNode.
     */
    private static void addMapperVariable(MethodSpec.Builder builder, CodeGenHelper codeGenHelper, String mapperVar, GenJavaType javaType) {
        ClassName javaTypeName = javaType.createJavaTypeName(codeGenHelper);

        // Only GenNodeType classes have dedicated `fromNode` methods
        if (javaType instanceof GenNodeType) {
            var nodeType = codeGenHelper.jtreesitterConfig().node().className();
            var mapperType = ParameterizedTypeName.get(ClassName.get(Function.class), nodeType, javaTypeName);
            builder.addStatement("$T $N = $T::$N", mapperType, mapperVar, javaTypeName, codeGenHelper.typedNodeConfig().methodFromNodeThrowing());
        } else {
            // Specify mapper as `Class`
            builder.addStatement("var $N = $T.class", mapperVar, javaTypeName);
        }
    }

    /**
     * Child type which represents a single named type.
     */
    record SingleChildType(GenNodeType nodeType) implements GenChildType {
        @Override
        public ClassName createJavaTypeName(CodeGenHelper codeGenHelper) {
            return nodeType.createJavaTypeName(codeGenHelper);
        }

        @Override
        public List<JavaTypeConfig> generateJavaTypes(CodeGenHelper codeGenHelper) {
            // Refers to existing type, nothing to generate
            return List.of();
        }

        @Override
        public boolean refersToTypeThroughInterface(GenRegularNodeType type, Set<GenJavaType> seenTypes) {
            // Irrelevant since no new Java interface is generated for this type
            return false;
        }

        @Override
        public void addConvertingCall(MethodSpec.Builder builder, CodeGenHelper codeGenHelper, String childrenVar, String resultVar) {
            var nodeUtils = codeGenHelper.nodeUtilsConfig();

            String mapperVar = "namedMapper";
            GenChildType.addMapperVariable(builder, codeGenHelper, mapperVar, nodeType);
            builder.addStatement("var $N = $T.$N($N, $N, null)", resultVar, nodeUtils.className(), nodeUtils.methodMapChildrenNamedNonNamed(), childrenVar, mapperVar);
        }
    }

    /**
     * For non-named child types, create an enum class representing all of the 'token' types,
     * and then an enclosing {@code ChildType{Node, TokenType}}.
     */
    final class UnnamedTokensChildType implements GenChildTypeAsNewJavaType, GenJavaType {
        private final GenRegularNodeType enclosingNodeType;
        private final String javaName;
        /** Map from token type name to Java constant name */
        private final Map<String, String> tokensToJavaConstants;
        /** Initialized after construction */
        @Nullable
        private GenJavaInterface interfaceToImplement;

        public UnnamedTokensChildType(GenRegularNodeType enclosingNodeType, String javaName, Map<String, String> tokensToJavaConstants) {
            this.enclosingNodeType = enclosingNodeType;
            this.javaName = javaName;
            this.tokensToJavaConstants = tokensToJavaConstants;
            this.interfaceToImplement = null;
        }

        /**
         * Sets the Java interface which this child type should implement.
         *
         * <p>Calling this method is optional and depends on whether this 'tokens child type' is directly used
         * as child return type or whether a common interface for the child types is used instead.
         */
        public void setInterfaceToImplement(GenJavaInterface interfaceToImplement) {
            if (this.interfaceToImplement != null) {
                throw new IllegalStateException("Interface already defined");
            }
            this.interfaceToImplement = Objects.requireNonNull(interfaceToImplement);
        }

        @Override
        public boolean isChildTypeAsTopLevel(CodeGenHelper codeGenHelper) {
            return switch (codeGenHelper.getChildTypeAsTopLevel()) {
                // This is generated as Java class and not as interface, so there is no need to implement it as top-level
                // because there won't be cyclic inheritance
                case NEVER, AS_NEEDED -> false;
                case ALWAYS -> true;
            };
        }

        @Override
        public boolean refersToTypeThroughInterface(GenRegularNodeType type, Set<GenJavaType> seenTypes) {
            // Irrelevant since a Java class and not an interface is generated for this type
            return false;
        }

        @Override
        public ClassName createJavaTypeName(CodeGenHelper codeGenHelper) {
            return createNestedJavaTypeName(enclosingNodeType.getJavaName(), javaName, codeGenHelper);
        }

        @Override
        public boolean isJavaInterface() {
            return false;
        }

        // Internal method name
        private static final String TOKEN_ENUM_FROM_NODE_METHOD_NAME = "fromNode";

        private ClassName getTokenEnumClassName(CodeGenHelper codeGenHelper) {
            return createJavaTypeName(codeGenHelper).nestedClass(codeGenHelper.tokenEnumConfig().name());
        }

        /** Generates the Java enum where each constant represents one of the 'tokens' / non-named types. */
        private TypeSpec generateTokenEnum(CodeGenHelper codeGenHelper, ClassName className) {
            var tokenNode = codeGenHelper.tokenEnumConfig();

            var typeBuilder = TypeSpec.enumBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Token type:"); // remaining Javadoc is generated below

            String typeField = "type";
            typeBuilder.addField(String.class, typeField, Modifier.PRIVATE, Modifier.FINAL);

            var constructor = MethodSpec.constructorBuilder()
                .addParameter(String.class, typeField)
                .addStatement("this.$N = $N", typeField, typeField)
                .build();
            typeBuilder.addMethod(constructor);

            var getTypeMethod = MethodSpec.methodBuilder(tokenNode.methodGetTypeName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return this.$N", typeField)
                .addJavadoc("Returns the grammar type of this token.")
                .build();
            typeBuilder.addMethod(getTypeMethod);

            // TODO: Should generate HTML table instead?
            typeBuilder.addJavadoc("\n<ul>");
            for (var token : tokensToJavaConstants.entrySet()) {
                String typeName = token.getKey();
                String constantName = token.getValue();

                // Add to enum Javadoc
                typeBuilder.addJavadoc("\n<li>{@link #$N '$L'}", constantName, CodeGenHelper.escapeJavadocText(typeName));

                var enumConstant = TypeSpec.anonymousClassBuilder("$S", typeName)
                    .addJavadoc(CodeGenHelper.createJavadocCodeTag(typeName))
                    .build();
                typeBuilder.addEnumConstant(constantName, enumConstant);
            }
            typeBuilder.addJavadoc("\n</ul>");

            var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
            String nodeParam = "node";
            String typeVar = "type";
            String tokenVar = "token";
            // package-private because this is an internal method
            var fromNodeMethod = MethodSpec.methodBuilder(TOKEN_ENUM_FROM_NODE_METHOD_NAME)
                .addModifiers(Modifier.STATIC)
                .addParameter(jtreesitterNode.className(), nodeParam)
                .returns(className)
                .addStatement("var $N = $N.$N()", typeVar, nodeParam, jtreesitterNode.methodGetType())
                .beginControlFlow("for (var $N : values())", tokenVar)
                .beginControlFlow("if ($N.$N.equals($N))", tokenVar, typeField, typeVar)
                .addStatement("return $N", tokenVar)
                .endControlFlow()
                .endControlFlow()
                .addComment("Should not happen since all non-named child types are covered")
                .addStatement("throw new $T(\"Unknown token type: \" + $N)", IllegalArgumentException.class, typeVar)
                .build();
            typeBuilder.addMethod(fromNodeMethod);

            return typeBuilder.build();
        }

        /** Generates {@code Object} methods such as {@code equals}, {@code hashCode} and {@code toString}. */
        private void generateOverriddenObjectMethods(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper, String nodeField, String tokenField) {
            ClassName ownClassName = createJavaTypeName(codeGenHelper);
            var equalsMethod = CodeGenHelper.createDelegatingEqualsMethod(ownClassName, nodeField);
            typeBuilder.addMethod(equalsMethod);

            var hashCodeMethod = CodeGenHelper.createDelegatingHashCodeMethod(nodeField);
            typeBuilder.addMethod(hashCodeMethod);

            var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
            var toStringMethod = CodeGenHelper.createToStringMethodSignature()
                // TODO: Include more information, e.g. position information? Or include wrapped node.toString()?
                .addStatement("return $S + \"[id=\" + $T.toUnsignedString(this.$N.$N()) + \",token=\" + this.$N + \"]\"", javaName, Long.class, nodeField, jtreesitterNode.methodGetId(), tokenField)
                .build();
            typeBuilder.addMethod(toStringMethod);
        }

        private void generateJavadoc(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper) {
            typeBuilder.addJavadoc("Child node type without name.");
            if (isChildTypeAsTopLevel(codeGenHelper)) {
                // If child is generated as top-level class, add Javadoc link to 'enclosing' class
                typeBuilder.addJavadoc(" Child type of {@link $T}.", enclosingNodeType.createJavaTypeName(codeGenHelper));
            }
        }

        @Override
        public List<JavaTypeConfig> generateJavaTypes(CodeGenHelper codeGenHelper) {
            var typeBuilder = TypeSpec.classBuilder(createJavaTypeName(codeGenHelper))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            boolean asTopLevel = isChildTypeAsTopLevel(codeGenHelper);
            if (!asTopLevel) {
                typeBuilder.addModifiers(Modifier.STATIC);
            }

            generateJavadoc(typeBuilder, codeGenHelper);

            if (interfaceToImplement == null) {
                typeBuilder.addSuperinterface(codeGenHelper.typedNodeConfig().className());
            } else {
                typeBuilder.addSuperinterface(interfaceToImplement.createJavaTypeName(codeGenHelper));
            }

            var tokenClassName = getTokenEnumClassName(codeGenHelper);
            var tokenType = generateTokenEnum(codeGenHelper, tokenClassName);
            // Add as nested type
            typeBuilder.addType(tokenType);

            String nodeField = "node";
            String tokenField = "token";
            TypedNodeInterfaceGenerator.generateTypedNodeImplementation(typeBuilder, codeGenHelper, nodeField, new TypedNodeInterfaceGenerator.JavaFieldData(tokenClassName, tokenField));

            var getTokenTypeMethod = MethodSpec.methodBuilder(codeGenHelper.tokenEnumConfig().enclosingMethodGetToken())
                .addModifiers(Modifier.PUBLIC)
                .returns(tokenClassName)
                .addJavadoc("Returns the token type.")
                .addStatement("return this.$N", tokenField)
                .build();
            typeBuilder.addMethod(getTokenTypeMethod);

            generateOverriddenObjectMethods(typeBuilder, codeGenHelper, nodeField, tokenField);

            return List.of(new JavaTypeConfig(typeBuilder, asTopLevel));
        }

        /**
         * Adds a mapper variable with the given {@code mapperVar} name, which is used by the {@link NodeUtilsGenerator}
         * methods to convert from a jtreesitter Node to a TypedNode.
         */
        void addMapperVariable(MethodSpec.Builder builder, CodeGenHelper codeGenHelper, String mapperVar) {
            ClassName ownClass = createJavaTypeName(codeGenHelper);
            var nodeType = codeGenHelper.jtreesitterConfig().node().className();
            var mapperType = ParameterizedTypeName.get(ClassName.get(Function.class), nodeType, ownClass);
            builder.addStatement("$T $N = n -> new $T(n, $T.$N(n))", mapperType, mapperVar, ownClass, getTokenEnumClassName(codeGenHelper), TOKEN_ENUM_FROM_NODE_METHOD_NAME);
        }

        @Override
        public void addConvertingCall(MethodSpec.Builder builder, CodeGenHelper codeGenHelper, String childrenVar, String resultVar) {
            var nodeUtils = codeGenHelper.nodeUtilsConfig();
            String mapperVar = "mapper";
            addMapperVariable(builder, codeGenHelper, mapperVar);

            // Cast `null` to `Class<...>` to avoid overload ambiguity
            var ownClassType = ParameterizedTypeName.get(ClassName.get(Class.class), createJavaTypeName(codeGenHelper));
            builder.addStatement("var $N = $T.$N($N, ($T) null, $N)", resultVar, nodeUtils.className(), nodeUtils.methodMapChildrenNamedNonNamed(), childrenVar, ownClassType, mapperVar);
        }
    }

    /**
     * Child type which represents multiple types, including at least one named type.
     * May optionally include non-named types as part of {@code tokensChildType}.
     *
     * <p>A single named type is represented by {@link SingleChildType}.
     */
    record MultiChildType(List<GenNodeType> types, @Nullable UnnamedTokensChildType tokensChildType, GenRegularNodeType enclosingNodeType, String javaName) implements GenJavaInterface, GenChildTypeAsNewJavaType {
        public MultiChildType {
            if (tokensChildType != null) {
                tokensChildType.setInterfaceToImplement(this);
            }
        }

        @Override
        public boolean isChildTypeAsTopLevel(CodeGenHelper codeGenHelper) {
            return switch (codeGenHelper.getChildTypeAsTopLevel()) {
                case NEVER -> false;
                case ALWAYS -> true;
                // Check if there is a direct or indirect reference back to the enclosing type,
                // for example `Node.Child -> Node` or `Node.Child -> Supertype -> Node`
                // In that case child type must be top-level; generating it as nested type leads to "cyclic inheritance" error
                case AS_NEEDED -> types.contains(enclosingNodeType)
                    // Also have to check indirect references, e.g. `Node.Child -> Supertype -> Node`; javac does not
                    // permit this either when Child is a nested class
                    || types.stream().anyMatch(t -> t.refersToType(enclosingNodeType, new HashSet<>()));
            };
        }

        @Override
        public boolean refersToTypeThroughInterface(GenRegularNodeType type, Set<GenJavaType> seenTypes) {
            if (seenTypes.add(this)) {
                return types.stream().anyMatch(t -> t.refersToType(type, seenTypes));
            } else {
                return false;
            }
        }

        @Override
        public ClassName createJavaTypeName(CodeGenHelper codeGenHelper) {
            return createNestedJavaTypeName(enclosingNodeType.getJavaName(), javaName, codeGenHelper);
        }

        private void generateJavadoc(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper) {
            if (isChildTypeAsTopLevel(codeGenHelper)) {
                // If child is generated as top-level class, add Javadoc link to 'enclosing' class
                typeBuilder.addJavadoc("Child type of {@link $T}.\n<p>", enclosingNodeType.createJavaTypeName(codeGenHelper));
            }
            typeBuilder.addJavadoc("Possible types:");
            codeGenHelper.addJavadocTypeMapping(typeBuilder, types, tokensChildType);
        }

        @Override
        public List<JavaTypeConfig> generateJavaTypes(CodeGenHelper codeGenHelper) {
            var typeBuilder = TypeSpec.interfaceBuilder(createJavaTypeName(codeGenHelper))
                .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                .addSuperinterface(codeGenHelper.typedNodeConfig().className());

            for (var subtype : types) {
                typeBuilder.addPermittedSubclass(subtype.createJavaTypeName(codeGenHelper));
            }

            generateJavadoc(typeBuilder, codeGenHelper);

            List<JavaTypeConfig> javaTypes = new ArrayList<>();
            if (tokensChildType != null) {
                typeBuilder.addPermittedSubclass(tokensChildType.createJavaTypeName(codeGenHelper));
                javaTypes.addAll(tokensChildType.generateJavaTypes(codeGenHelper));
            }

            boolean asTopLevel = isChildTypeAsTopLevel(codeGenHelper);
            javaTypes.add(new JavaTypeConfig(typeBuilder, asTopLevel));

            return javaTypes;
        }

        @Override
        public void addConvertingCall(MethodSpec.Builder builder, CodeGenHelper codeGenHelper, String childrenVar, String resultVar) {
            var nodeUtils = codeGenHelper.nodeUtilsConfig();

            String namedMapperVar = "namedMapper";
            // If refers only to 1 other named type, can generate more efficient code by using its `fromNode` method
            // instead of `TypedNode#fromNode`
            var javaType = types.size() == 1 ? types.getFirst() : this;
            GenChildType.addMapperVariable(builder, codeGenHelper, namedMapperVar, javaType);

            if (tokensChildType == null) {
                builder.addStatement("var $N = $T.$N($N, $N, null)", resultVar, nodeUtils.className(), nodeUtils.methodMapChildrenNamedNonNamed(), childrenVar, namedMapperVar);
            } else {
                String tokenMapperVar = "tokenMapper";
                tokensChildType.addMapperVariable(builder, codeGenHelper, tokenMapperVar);
                builder.addStatement("var $N = $T.$N($N, $N, $N)", resultVar, nodeUtils.className(), nodeUtils.methodMapChildrenNamedNonNamed(), childrenVar, namedMapperVar, tokenMapperVar);
            }
        }
    }
}
