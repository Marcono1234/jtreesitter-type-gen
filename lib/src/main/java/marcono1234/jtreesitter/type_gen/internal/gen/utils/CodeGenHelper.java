package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.CodeGenConfig;
import marcono1234.jtreesitter.type_gen.CodeGenerator;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageProviderConfig;
import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.common_classes.LanguageUtilsGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.common_classes.NodeUtilsGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.common_classes.TypedNodeInterfaceGenerator;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings({"CodeBlock2Expr", "Convert2MethodRef"}) // Suppress IntelliJ warnings for rewriting lambda expressions
public class CodeGenHelper {
    private final CodeGenConfig config;
    private final CodeGenerator.Version versionInfo;
    @Nullable // null when no access to the Language object is possible
    private final LanguageProviderConfig languageProviderConfig;
    private final Instant generationTime;
    @Nullable // null when `Optional<T>` instead of `@Nullable T` should be generated
    private final AnnotationSpec nullableAnnotation;
    private final ClassName nonEmptyClassName;
    private final AnnotationSpec nonEmptyAnnotation;

    public CodeGenHelper(CodeGenConfig config, CodeGenerator.Version versionInfo, Optional<LanguageProviderConfig> languageProviderConfig) {
        this.config = Objects.requireNonNull(config);
        this.versionInfo = Objects.requireNonNull(versionInfo);
        this.languageProviderConfig = languageProviderConfig.orElse(null);

        generationTime = this.config.generatedAnnotationConfig().flatMap(c -> c.generationTime()).orElseGet(Instant::now);
        nullableAnnotation = this.config.nullableAnnotationTypeName()
            .map(name -> AnnotationSpec.builder(createClassName(name)).build())
            .orElse(null);
        nonEmptyClassName = ClassName.get(config.packageName(), config.nonEmptyTypeName());
        nonEmptyAnnotation = AnnotationSpec.builder(nonEmptyClassName).build();
    }

    /**
     * Creates a JavaPoet class name from the given type name.
     */
    public static ClassName createClassName(marcono1234.jtreesitter.type_gen.TypeName typeName) {
        List<String> simpleNames = Arrays.asList(typeName.name().split("\\$"));
        String[] nestedNames = simpleNames.subList(1, simpleNames.size()).toArray(String[]::new);
        return ClassName.get(typeName.packageName(), simpleNames.getFirst(), nestedNames);
    }

    /**
     * Whether to generate child types as top-level interface instead of nested interface.
     * This is needed when a node type can have itself as child. If a nested interface is
     * used, the compiler rejects the code, e.g.:
     * {@snippet lang=java :
     * class MyNode implements MyNode.Children {
     *     interface Children {}
     * }
     * }
     */
    public CodeGenConfig.ChildTypeAsTopLevel getChildTypeAsTopLevel() {
        return config.childTypeAsTopLevel();
    }

    /**
     * Creates a JavaFile builder for the given type. Should be used for all generated top-level types.
     */
    public JavaFile.Builder createOwnJavaFileBuilder(TypeSpec.Builder typeBuilder) {
        config.generatedAnnotationConfig().ifPresent(generatedAnnotationConfig -> {
            var annotationType = generatedAnnotationConfig.annotationType();

            StringBuilder comments = new StringBuilder();
            String codeGeneratorVersion = versionInfo.version() + " (" + versionInfo.gitCommitId() + ")";
            comments.append("code-generator-version=").append(codeGeneratorVersion);

            generatedAnnotationConfig.additionalInformation().ifPresent(additionalInformation -> {
                comments.append("; ");
                comments.append(additionalInformation);
            });

            var generatedAnnotationBuilder = AnnotationSpec.builder(createClassName(annotationType.typeName()));
            annotationType.generatorElementName().ifPresent(name -> {
                generatedAnnotationBuilder.addMember(name, "$S", CodeGenerator.class.getName());
            });
            annotationType.dateElementName().ifPresent(name -> {
                generatedAnnotationBuilder.addMember(name, "$S", generationTime.toString());
            });

            if (!comments.isEmpty()) {
                annotationType.commentsElementName().ifPresent(name -> {
                    generatedAnnotationBuilder.addMember(name, "$S", comments);
                });
            }
            typeBuilder.addAnnotation(generatedAnnotationBuilder.build());
        });

        return JavaFile.builder(config.packageName(), typeBuilder.build());
    }

    /**
     * Creates a JavaPoet class name for the given name, in the generated package.
     */
    // TODO: Maybe instead of creating this on demand, create it when the ...Gen classes are created
    public ClassName createOwnClassName(String name, String... nestedNames) {
        return ClassName.get(config.packageName(), name, nestedNames);
    }

    /**
     * Creates a Java constant field with name {@code constantName} storing the node type name {@code typeName}.
     */
    // TODO: Make this method non-static to allow customizing it in the future?
    public static FieldSpec createTypeNameConstantField(String typeName, String constantName) {
        return FieldSpec.builder(String.class, constantName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", typeName)
            .addJavadoc("Type name of this node, as defined in the grammar.")
            .build();
    }

    /**
     * Creates a Java constant field with name {@code constantName} storing the node type ID for {@code typeName}.
     * Throws an exception if {@link #generatesNumericIdConstants()} is false.
     */
    public FieldSpec createTypeIdConstantField(String typeName, String constantName) {
        var languageUtils = languageUtilsConfig().orElseThrow();
        var jtreesitterNode = jtreesitterConfig().node();
        var fieldType = jtreesitterConfig().language().numericIdType();

        return FieldSpec.builder(fieldType, constantName, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$T.$N($S)", languageUtils.className(), languageUtils.methodGetTypeId(), typeName)
            .addJavadoc("Type ID of this node, assigned by tree-sitter.")
            .addJavadoc("\n@see $T#$N", jtreesitterNode.className(), jtreesitterNode.methodGetTypeId())
            .build();
    }

    /**
     * Name for the {@code @NonEmpty} annotation.
     */
    public String getNonEmptyTypeName() {
        return nonEmptyClassName.simpleName();
    }

    /**
     * Creates a new type name annotated with {@code @NonEmpty}.
     */
    public TypeName annotatedNonEmpty(TypeName type) {
        return type.annotated(nonEmptyAnnotation);
    }

    /**
     * Creates a type name indicating that the type is optional. Depending on the config either by
     * wrapping the type in {@link Optional}, or by annotating it with {@code @Nullable}.
     *
     * <p>Only intended for single optional types; not for {@code List<T>} or similar.
     */
    public TypeName getReturnOptionalType(TypeName type) {
        if (nullableAnnotation == null) {
            return ParameterizedTypeName.get(ClassName.get(Optional.class), type);
        } else {
            return type.annotated(nullableAnnotation);
        }
    }

    /**
     * Adds a {@code return} statement which returns the nullable {@code resultVarName} as optional value.
     * Depending on the config either as is (if {@code @Nullable} is used) or wrapped inside {@link Optional}.
     *
     * @see #getReturnOptionalType(TypeName)
     */
    public void addReturnOptionalStatement(MethodSpec.Builder builder, String resultVarName) {
        if (nullableAnnotation != null) {
            // Directly return nullable value
            builder.addStatement("return $N", resultVarName);
        } else {
            builder.addStatement("return $T.ofNullable($N)", Optional.class, resultVarName);
        }
    }

    /**
     * Adds a statement which unwraps an optional value from {@code fromVarName} and stores it in {@code toVarName}
     * as nullable value.
     *
     * @see #getReturnOptionalType(TypeName)
     */
    public void addUnwrapToNullStatement(MethodSpec.Builder builder, String fromVarName, String toVarName) {
        if (nullableAnnotation != null) {
            // Already nullable; perform no-op assignment
            builder.addStatement("var $N = $N", toVarName, fromVarName);
        } else {
            builder.addStatement("var $N = $N.orElse(null)", toVarName, fromVarName);
        }
    }

    /**
     * Gets Javadoc text which describes an optional value.
     *
     * @see #getReturnOptionalType(TypeName)
     */
    public String getEmptyOptionalJavadocText() {
        return nullableAnnotation != null ? "{@code null}" : "an empty {@code Optional}";
    }

    /**
     * Adds Javadoc which for each Java type in {@code types} mentions its node type name.
     */
    public void addJavadocTypeMapping(TypeSpec.Builder builder, List<GenNodeType> types) {
        // TODO should generate HTML table instead of list?
        builder.addJavadoc("\n<ul>");
        for (var type : types) {
            builder.addJavadoc("\n<li>{@link $T $L}", type.createJavaTypeName(this), CodeGenHelper.escapeJavadocText(type.getTypeName()));
        }
        builder.addJavadoc("\n</ul>");
    }

    /**
     * Config for the generated {@code TypedNode} interface.
     *
     * @see TypedNodeInterfaceGenerator
     */
    public record TypedNodeConfig(
        CodeGenHelper codeGenHelper,
        String name,
        String methodGetNode,
        // These method names are also used for the static methods in the TypedNode subtypes
        String methodFromNode,
        String methodFromNodeThrowing, Class<? extends Exception> exceptionFromNodeThrowing,
        String methodFindNodes
    ) {
        public ClassName className() {
            return codeGenHelper.createOwnClassName(name);
        }

        /**
         * Generates a {@link #methodFromNodeThrowing()} implementation which delegates to {@link #methodFromNode()}
         * in the same class.
         *
         * @param javadoc
         *      method Javadoc; should contain {@code $T} as placeholder for the exception type name
         * @param wrongNodeMessage
         *      exception message for the case that the given node has the wrong type
         */
        public MethodSpec generateMethodFromNodeThrowing(ClassName returnType, String javadoc, String wrongNodeMessage) {
            var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();

            var thrownExceptionType = exceptionFromNodeThrowing();
            String nodeParam = "node";
            var methodBuilder = MethodSpec.methodBuilder(methodFromNodeThrowing())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(jtreesitterNode.className(), nodeParam)
                .returns(returnType)
                .addJavadoc(javadoc, thrownExceptionType)
                .addJavadoc("\n\n@see #$N", methodFromNode());

            String typedNodeOptionalVar = "typedNodeOptional";
            methodBuilder.addStatement("var $N = $N($N)", typedNodeOptionalVar, methodFromNode(), nodeParam);

            String typedNodeVar = "typedNode";
            codeGenHelper.addUnwrapToNullStatement(methodBuilder, typedNodeOptionalVar, typedNodeVar);

            methodBuilder
                .beginControlFlow("if ($N == null)", typedNodeVar)
                .addStatement("throw new $T($S + \": \" + $N.$N())", thrownExceptionType, wrongNodeMessage, nodeParam, jtreesitterNode.methodGetType())
                .endControlFlow()
                .addStatement("return $N", typedNodeVar);

            return methodBuilder.build();
        }

        public record JavaFieldRef(ClassName owner, String name) {
            /** Adds a field access to the given code block. */
            public void addTo(CodeBlock.Builder block) {
                block.add("$T.$N", owner, name);
            }
        }

        /**
         * Generates the actual implementation of {@link #generateMethodFindNodes}.
         */
        private MethodSpec generateMethodFindNodesImpl(String implMethodName, ClassName nodeClass, List<JavaFieldRef> nodeTypeConstants) {
            var jtreesitter = codeGenHelper.jtreesitterConfig();

            String captureNameVar = "captureName";
            String queryStringVar = "queryString";
            var queryStringCode = CodeBlock.builder().add("var $N = ", queryStringVar);
            if (nodeTypeConstants.isEmpty()) {
                throw new IllegalArgumentException("Must provide at least one node type");
            } else if (nodeTypeConstants.size() == 1) {
                queryStringCode.add("\"(\" + ");
                nodeTypeConstants.getFirst().addTo(queryStringCode);
                queryStringCode.add(" + \")");
            } else {
                // Create tree-sitter query alternation `[ ... ]` which covers all types
                // Manually performs String joining here instead of using `String#join` or similar to create compile-time
                // constant value (since all node type constants are compile-time constants as well)
                queryStringCode.add("\"[\"");
                for (var nodeTypeConstant : nodeTypeConstants) {
                    queryStringCode.add("\n+ \"(\" + ");
                    nodeTypeConstant.addTo(queryStringCode);
                    queryStringCode.add(" + \")\"");
                }
                queryStringCode.add("\n+ \"]");
            }
            queryStringCode.add(" @\" + $N", captureNameVar);

            String startNodeParam = "startNode";
            String allocatorParam = "allocator";
            String startNodeUnwrappedVar = "startNodeUnwrapped";
            String languageVar = "language";
            String queryVar = "query";
            String queryCursorVar = "queryCursor";
            String resultStreamVar = "stream";
            return MethodSpec.methodBuilder(implMethodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                // Use TypedNode instead of jtreesitter Node as parameter to make sure node (and its language)
                // actually belongs to generated code
                .addParameter(className(), startNodeParam)
                .addParameter(codeGenHelper.ffmApiConfig().classSegmentAllocator(), allocatorParam)
                .returns(ParameterizedTypeName.get(ClassName.get(Stream.class), nodeClass))
                // First create the Query
                .addStatement("var $N = $N.$N()", startNodeUnwrappedVar, startNodeParam, methodGetNode())
                .addStatement("var $N = $N.$N().$N()", languageVar, startNodeUnwrappedVar, jtreesitter.node().methodGetTree(), jtreesitter.tree().methodGetLanguage())
                .addComment("tree-sitter query which matches the nodes of this type, and captures them")
                .addStatement("var $N = $S", captureNameVar, "node")
                .addStatement(queryStringCode.build())
                .addStatement("var $N = new $T($N, $N)", queryVar, jtreesitter.query().className(), languageVar, queryStringVar)
                .addStatement("var $N = new $T($N)", queryCursorVar, jtreesitter.queryCursor().className(), queryVar)
                // Run the query
                .addStatement(CodeBlock.builder()
                    .add("var $N = $N == null ?$W", resultStreamVar, allocatorParam)
                    // Variant without allocator
                    .add("$N.$N($N)$W", queryCursorVar, jtreesitter.queryCursor().methodFindMatches(), startNodeUnwrappedVar)
                    // Variant with allocator (and with default `Options`, unfortunately there is no overload without it)
                    .add(": $N.$N($N, $N, new $T(($T<$T>) null))", queryCursorVar, jtreesitter.queryCursor().methodFindMatches(), startNodeUnwrappedVar, allocatorParam, jtreesitter.queryCursor().classNameOptions(), Predicate.class, jtreesitter.queryCursor().classNameState())
                    .build()
                )
                // Convert the captured nodes
                .addStatement(CodeBlock.builder()
                    .add("return $N.flatMap(m -> m.$N($N).stream())", resultStreamVar, jtreesitter.queryMatch().methodFindNodes(), captureNameVar)
                    .add(".map($T::$N)", nodeClass, methodFromNodeThrowing())
                    .add(".onClose(() -> {\n")
                    .indent()
                    .add("$N.close();\n", queryCursorVar)
                    .add("$N.close();\n", queryVar)
                    .unindent()
                    .add("})")
                    .build()
                )
                .build();
        }

        /**
         * Used by {@link #generateMethodsFindNodes} to generate a {@code public} {@code findNodes} method.
         *
         * @param methodName name of the method to generate
         * @param implMethodName name of the implementation method to which the method should delegate
         * @param nodeClass type of the method result nodes
         * @param hasAllocatorParam whether the method should have an 'allocator' parameter
         */
        private MethodSpec generateMethodFindNodes(String methodName, String implMethodName, ClassName nodeClass, boolean hasAllocatorParam) {
            var startNodeParam = ParameterSpec.builder(className(), "startNode").build();
            var allocatorParam = ParameterSpec.builder(codeGenHelper.ffmApiConfig().classSegmentAllocator(), "allocator")
                .addJavadoc("allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed")
                .build();

            var methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(startNodeParam);

            if (hasAllocatorParam) {
                methodBuilder.addParameter(allocatorParam);
            }

            methodBuilder
                .returns(ParameterizedTypeName.get(ClassName.get(Stream.class), nodeClass))
                .addJavadoc("Gets all nodes of this type, starting at the given node.")
                .addJavadoc("\n\n<p><b>Important:</b> The {@code Stream} must be closed to release resources.")
                .addJavadoc("\nIt is recommended to use a try-with-resources statement.");

            if (!hasAllocatorParam) {
                methodBuilder
                    // The nodes are allocated with the Arena of the Query, so they cannot / should not be used anymore after the Query was closed
                    .addJavadoc("\nAfter the stream was closed the resulting nodes should not be used anymore, otherwise the behavior is undefined,")
                    .addJavadoc("\nincluding exceptions being thrown or possibly even a JVM crash.")
                    // Add link to overload with custom 'allocator' parameter
                    .addJavadoc("\nUse {@link #$N($T, $T)} to be able to access the nodes after the stream was closed.", methodName, startNodeParam.type(), allocatorParam.type());
            }

            methodBuilder
                .addJavadoc("\n\n<h4>Example</h4>")
                .addJavadoc("\n{@snippet lang=java :")
                .addJavadoc("\ntry (var nodes = $T.$N(start" + (hasAllocatorParam ? ", allocator" : "") + ")) {", nodeClass, methodName)
                .addJavadoc("\n  List<String> texts = nodes.map(n -> n.$N()).toList();", codeGenHelper.jtreesitterConfig().node().methodGetText())
                .addJavadoc("\n  ...")
                .addJavadoc("\n}")
                .addJavadoc("\n}");

            if (hasAllocatorParam) {
                methodBuilder.addStatement("return $N($N, $N)", implMethodName, startNodeParam, allocatorParam);
            } else {
                methodBuilder.addStatement("return $N($N, null)", implMethodName, startNodeParam);
            }

            return methodBuilder.build();
        }

        /**
         * Generates the {@link #methodFindNodes()} methods, which start at a given node and return a stream of
         * found {@code nodeClass} sub nodes.
         *
         * @param nodeClass
         *      whose node instances should be returned
         * @param nodeTypeConstants
         *      the Java fields storing the node type names for all implementations of {@code nodeClass}
         */
        public List<MethodSpec> generateMethodsFindNodes(ClassName nodeClass, List<JavaFieldRef> nodeTypeConstants) {
            String methodName = methodFindNodes();
            String implMethodName = methodName + "Impl";
            return List.of(
                generateMethodFindNodesImpl(implMethodName, nodeClass, nodeTypeConstants),
                generateMethodFindNodes(methodName, implMethodName, nodeClass, true),
                generateMethodFindNodes(methodName, implMethodName, nodeClass, false)
            );
        }

        public static TypedNodeConfig createDefault(CodeGenHelper codeGenHelper) {
            return new TypedNodeConfig(
                codeGenHelper,
                "TypedNode",
                "getNode",
                "fromNode",
                "fromNodeThrowing", IllegalArgumentException.class,
                "findNodes"
            );
        }
    }

    public TypedNodeConfig typedNodeConfig() {
        return TypedNodeConfig.createDefault(this);
    }

    /**
     * Config for Java enums generated for non-named ('token') node children.
     */
    // Note: `enclosingMethodGetToken` is for enclosing class, but maybe not worth it to have separate config for it
    public record TokenEnumConfig(String name, String methodGetTypeName, String enclosingMethodGetToken) {
        public static final TokenEnumConfig DEFAULT = new TokenEnumConfig(
            "TokenType",
            // For simplicity call this just "getType" instead of "getTypeName"; but technically this is the tree-sitter
            // type name
            "getType",
            "getToken"
        );
    }

    public TokenEnumConfig tokenEnumConfig() {
        return TokenEnumConfig.DEFAULT;
    }

    /**
     * Config for the generated {@code NodeUtils} class.
     *
     * @see NodeUtilsGenerator
     */
    public record NodeUtilsConfig(
        CodeGenHelper codeGenHelper,
        String name,
        String methodFromNodeThrowing,
        String methodGetNonFieldChildren,
        String methodMapChildrenNamedNonNamed,
        // Methods for converting List<TypedNode> to TypedNode / Optional<TypedNode> or @NonEmpty List<TypedNode>
        String methodOptionalChild, String methodRequiredChild, String methodAtLeastOneChild
    ) {
        public ClassName className() {
            return codeGenHelper.createOwnClassName(name);
        }

        public static NodeUtilsConfig createDefault(CodeGenHelper codeGenHelper) {
            return new NodeUtilsConfig(
                codeGenHelper,
                "NodeUtils",
                "fromNodeThrowing",
                "getNonFieldChildren",
                "mapChildren",
                "optionalSingleChild", "requiredSingleChild", "atLeastOneChild"
            );
        }
    }

    public NodeUtilsConfig nodeUtilsConfig() {
        return NodeUtilsConfig.createDefault(this);
    }

    /**
     * Config for the generated {@code LanguageUtils} class.
     *
     * @see LanguageUtilsGenerator
     */
    public record LanguageUtilsConfig(
        CodeGenHelper codeGenHelper,
        LanguageProviderConfig languageProviderConfig,
        String name,
        String methodGetTypeId, String methodGetFieldId
    ) {
        public ClassName className() {
            return codeGenHelper.createOwnClassName(name);
        }

        public static LanguageUtilsConfig createDefault(CodeGenHelper codeGenHelper) {
            return new LanguageUtilsConfig(
                codeGenHelper,
                codeGenHelper.languageProviderConfig,
                "LanguageUtils",
                "getTypeId", "getFieldId"
            );
        }
    }

    /**
     * Empty if {@code LanguageUtils} class is not being generated.
     */
    public Optional<LanguageUtilsConfig> languageUtilsConfig() {
        return languageProviderConfig == null ? Optional.empty() : Optional.of(LanguageUtilsConfig.createDefault(this));
    }

    /**
     * {@return whether numeric type and field ID constants are being generated}
     */
    public boolean generatesNumericIdConstants() {
        return languageUtilsConfig().isPresent();
    }

    /**
     * Config for the jtreesitter library, providing class and method names.
     */
    public record JTreestitterConfig(
        Language language,
        Query query,
        QueryCursor queryCursor,
        QueryMatch queryMatch,
        Tree tree,
        TreeCursor treeCursor,
        Node node,
        ClassName classRange,
        ClassName classPoint
    ) {
        /** jtreesitter {@code Language} class */
        public record Language(
            ClassName className,
            TypeName numericIdType,
            String methodGetTypeId,
            String methodGetFieldId
        ) {
            public static final Language DEFAULT = new Language(
                ClassName.get("io.github.treesitter.jtreesitter", "Language"),
                // Use the same type as jtreesitter to increase interoperability with its methods, e.g. `Node#getSymbol`
                // Otherwise when manually converting with `Short#toUnsignedInt` it would make comparing values with
                // jtreesitter values more cumbersome and error-prone
                TypeName.SHORT.annotated(AnnotationSpec.builder(ClassName.get("io.github.treesitter.jtreesitter", "Unsigned")).build()),
                "getSymbolForName",
                "getFieldIdForName"
            );

            /**
             * Generates code for obtaining the node type ID from a node type name.
             *
             * @param nameParam
             *      name of the method parameter storing the type name
             * @param languageField
             *      name of the field storing the {@code Language} instance
             */
            public CodeBlock generateGetTypeIdCode(String nameParam, String languageField) {
                String idVar = "id";
                var codeBuilder = CodeBlock.builder();
                codeBuilder
                    .addStatement("short $N = $N.$N($N, true)", idVar, languageField, methodGetTypeId, nameParam)
                    .beginControlFlow("if ($N == 0)", idVar)
                    .addStatement("throw new $T(\"Unknown type name: \" + $N)", IllegalArgumentException.class, nameParam)
                    .endControlFlow()
                    .addStatement("return $N", idVar);
                return codeBuilder.build();
            }

            /**
             * Generates code for obtaining the node field ID from a node field name.
             *
             * @param nameParam
             *      name of the method parameter storing the field name
             * @param languageField
             *      name of the field storing the {@code Language} instance
             */
            public CodeBlock generateGetFieldIdCode(String nameParam, String languageField) {
                String idVar = "id";
                var codeBuilder = CodeBlock.builder();
                codeBuilder
                    .addStatement("short $N = $N.$N($N)", idVar, languageField, methodGetFieldId, nameParam)
                    .beginControlFlow("if ($N == 0)", idVar)
                    .addStatement("throw new $T(\"Unknown field name: \" + $N)", IllegalArgumentException.class, nameParam)
                    .endControlFlow()
                    .addStatement("return $N", idVar);
                return codeBuilder.build();
            }
        }

        /** jtreesitter {@code Query} class */
        public record Query(
            ClassName className
        ) {
            public static final Query DEFAULT = new Query(
                ClassName.get("io.github.treesitter.jtreesitter", "Query")
            );
        }

        /**
         * jtreesitter {@code QueryCursor}
         *
         * @param classNameOptions nested class {@code Options}
         * @param classNameState nested class {@code State}
         */
        public record QueryCursor(
            ClassName className,
            String methodFindMatches,
            ClassName classNameOptions,
            ClassName classNameState
        ) {
            private static final ClassName className_ = ClassName.get("io.github.treesitter.jtreesitter", "QueryCursor");

            public static final QueryCursor DEFAULT = new QueryCursor(
                className_,
                "findMatches",
                className_.nestedClass("Options"),
                className_.nestedClass("State")
            );
        }

        /** jtreesitter {@code QueryMatch} */
        public record QueryMatch(
            String methodFindNodes
        ) {
            public static final QueryMatch DEFAULT = new QueryMatch(
                "findNodes"
            );
        }

        /** jtreesitter {@code Tree} class */
        public record Tree(
            ClassName className,
            String methodGetRootNode, String methodGetText, String methodGetLanguage
        ) {
            public static final Tree DEFAULT = new Tree(
                ClassName.get("io.github.treesitter.jtreesitter", "Tree"),
                "getRootNode", "getText", "getLanguage"
            );
        }

        /** jtreesitter {@code TreeCursor} class */
        public record TreeCursor(
            ClassName className,
            String methodGotoFirstChild,
            String methodGotoNextSibling,
            String methodGetCurrentNode,
            String methodGetCurrentFieldId
        ) {
            public static final TreeCursor DEFAULT = new TreeCursor(
                ClassName.get("io.github.treesitter.jtreesitter", "TreeCursor"),
                "gotoFirstChild",
                "gotoNextSibling",
                "getCurrentNode",
                "getCurrentFieldId"
            );
        }

        /** jtreesitter {@code Node} class */
        // TODO Should use `Node#getGrammarType` instead of `getType` and `getGrammarSymbol` instead of `getSymbol` to ignore aliases?
        //   But will this work? node-types.json seems to be based on alias names
        public record Node(
            ClassName className,
            String methodGetType,
            String methodGetTypeId,
            String methodGetId,
            String methodGetChildrenByFieldName, String methodGetChildrenByFieldId,
            String methodGetText,
            String methodGetTree,
            String methodGetRange, String methodGetStartPoint, String methodGetEndPoint,
            String methodHasError, String methodIsNamed, String methodIsError, String methodIsMissing, String methodIsExtra,
            String methodWalk
        ) {
            public static final Node DEFAULT = new Node(
                ClassName.get("io.github.treesitter.jtreesitter", "Node"),
                "getType",
                "getSymbol",
                "getId",
                "getChildrenByFieldName", "getChildrenByFieldId",
                "getText",
                "getTree",
                "getRange", "getStartPoint", "getEndPoint",
                "hasError", "isNamed", "isError", "isMissing", "isExtra",
                "walk"
            );
        }

        public static final JTreestitterConfig DEFAULT = new JTreestitterConfig(
            Language.DEFAULT,
            Query.DEFAULT,
            QueryCursor.DEFAULT,
            QueryMatch.DEFAULT,
            Tree.DEFAULT,
            TreeCursor.DEFAULT,
            Node.DEFAULT,
            ClassName.get("io.github.treesitter.jtreesitter", "Range"),
            ClassName.get("io.github.treesitter.jtreesitter", "Point")
        );
    }

    public JTreestitterConfig jtreesitterConfig() {
        return JTreestitterConfig.DEFAULT;
    }

    /**
     * Config for the Java FFM API (package {@code java.lang.foreign}).
     */
    // Note: This config class exists because this project is currently built with JDK 21 but the FFM API classes only became stable in JDK 22
    // TODO: Remove once this project is built with JDK > 21
    public record FFMApiConfig(
        ClassName classSegmentAllocator,
        ClassName classArena,
        String methodArenaOfAuto
    ) {
        public static final FFMApiConfig DEFAULT = new FFMApiConfig(
            ClassName.get("java.lang.foreign", "SegmentAllocator"),
            ClassName.get("java.lang.foreign", "Arena"),
            "ofAuto"
        );
    }

    public FFMApiConfig ffmApiConfig() {
        return FFMApiConfig.DEFAULT;
    }

    // TODO: Make the following methods non-static to allow customizing them in the future?

    /**
     * Creates the signature for an {@code Object#equals} override.
     */
    // TODO: Does JavaPoet define convenience methods for this?
    public static MethodSpec.Builder createEqualsMethodSignature(String otherParam) {
        return MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Object.class, otherParam)
            .addAnnotation(Override.class)
            .returns(boolean.class);
    }

    /**
     * Creates an {@code Object#equals} override which delegates the call to an object stored in
     * {@code delegateField}.
     */
    public static MethodSpec createDelegatingEqualsMethod(ClassName ownType, String delegateField) {
        String otherParam = "obj";
        String otherCastVar = "other";
        return createEqualsMethodSignature(otherParam)
            .beginControlFlow("if ($N instanceof $T $N)", otherParam, ownType, otherCastVar)
            .addStatement("return this.$N.equals($N.$N)", delegateField, otherCastVar, delegateField)
            .endControlFlow()
            .addStatement("return false")
            .build();
    }

    /**
     * Creates the signature for an {@code Object#hashCode} override.
     */
    public static MethodSpec.Builder createHashCodeMethodSignature() {
        return MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(int.class);
    }

    /**
     * Creates an {@code Object#hashCode} override which delegates the call to an object stored in
     * {@code delegateField}.
     */
    public static MethodSpec createDelegatingHashCodeMethod(String delegateField) {
        return createHashCodeMethodSignature()
            .addStatement("return this.$N.hashCode()", delegateField)
            .build();
    }

    /**
     * Creates the signature for an {@code Object#toString} override.
     */
    public static MethodSpec.Builder createToStringMethodSignature() {
        return MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class);
    }

    /**
     * Creates a getter method with name {@code methodName} which delegates the call to a method of the same
     * name on the object provided by {@code delegate} ({@code field} or {@code method()}).
     */
    public static MethodSpec.Builder createDelegatingGetter(String methodName, TypeName returnType, String delegate) {
        return MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(returnType)
            .addStatement("return this.$L.$N()", delegate, methodName);
    }

    /**
     * Creates a getter method with name {@code methodName} which delegates the call to a method of the same
     * name on the object provided by {@code delegate} ({@code field} or {@code method()}).
     *
     * <p>The delegate returns a nullable value which is wrapped as {@link Optional} depending on the config,
     * see {@link #getReturnOptionalType(TypeName)}.
     */
    public MethodSpec.Builder createNullableDelegatingGetter(String methodName, TypeName returnType, String delegate) {
        String resultVar = "result";
        var builder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(getReturnOptionalType(returnType))
            .addStatement("var $N = this.$L.$N()", resultVar, delegate, methodName);
        addReturnOptionalStatement(builder, resultVar);
        return builder;
    }

    /**
     * HTML-escapes {@code text} for usage in Javadoc.
     */
    public static String escapeJavadocText(String text) {
        return text
            // HTML
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            // Javadoc-specific
            .replace("{", "&lbrace;").replace("}", "&rbrace;").replace("@", "&commat;");
    }

    /**
     * Creates a Javadoc 'code' tag, escaping the content if necessary.
     */
    public static String createJavadocCodeTag(String content) {
        if (content.chars().anyMatch(c -> c == '{' || c == '}' || c == '@')) {
            return "<code>%s</code>".formatted(escapeJavadocText(content));
        } else {
            return "{@code %s}".formatted(content);
        }
    }
}
