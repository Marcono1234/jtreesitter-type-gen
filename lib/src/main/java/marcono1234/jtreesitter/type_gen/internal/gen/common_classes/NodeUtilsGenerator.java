package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Code generator for the internal {@code NodeUtils} class, which provides convenience methods for working
 * with {@code Node}.
 */
public class NodeUtilsGenerator {
    private final CodeGenHelper codeGenHelper;

    public NodeUtilsGenerator(CodeGenHelper codeGenHelper) {
        this.codeGenHelper = Objects.requireNonNull(codeGenHelper);
    }

    private static ParameterizedTypeName listType(TypeName argType) {
        return ParameterizedTypeName.get(ClassName.get(List.class), argType);
    }

    private MethodSpec generateGetNonFieldChildrenMethod() {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var jtreesitterNodeClass = jtreesitterNode.className();
        var jtreesitterCursor = codeGenHelper.jtreesitterConfig().treeCursor();
        var ffmApi = codeGenHelper.ffmApiConfig();
        
        String nodeParam = "node";
        var namedParam = ParameterSpec.builder(boolean.class, "named")
            .addJavadoc("whether to return named or non-named children")
            .build();

        var methodBuilder = MethodSpec.methodBuilder(codeGenHelper.nodeUtilsConfig().methodGetNonFieldChildren())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(jtreesitterNodeClass, nodeParam)
            .addParameter(namedParam)
            .returns(listType(jtreesitterNodeClass))
            .addJavadoc("Gets all non-field children of the node.");

        String childrenVar = "children";
        String arenaVar = "arena";
        String cursorVar = "cursor";
        String currentNodeVar = "currentNode";
        return methodBuilder
            .addStatement("var $N = new $T<$T>()", childrenVar, ArrayList.class, jtreesitterNodeClass)
            .addComment("Use custom allocator to ensure that nodes are usable after cursor was closed")
            .addStatement("var $N = $T.$N()", arenaVar, ffmApi.classArena(), ffmApi.methodArenaOfAuto())
            .beginControlFlow("try (var $N = $N.$N())", cursorVar, nodeParam, jtreesitterNode.methodWalk())
            .beginControlFlow("if ($N.$N())", cursorVar, jtreesitterCursor.methodGotoFirstChild())
            .beginControlFlow("do")
            .addComment("Only consider non-field children")
            .beginControlFlow("if ($N.$N() == 0)", cursorVar, jtreesitterCursor.methodGetCurrentFieldId())
            .addStatement("var $N = $N.$N($N)", currentNodeVar, cursorVar, jtreesitterCursor.methodGetCurrentNode(), arenaVar)
            // Cannot convert error node to typed node; for easier troubleshooting directly throw exception instead of silently discarding it
            .beginControlFlow("if ($N.$N() || $N.$N())", currentNodeVar, jtreesitterNode.methodIsError(), currentNodeVar, jtreesitterNode.methodIsMissing())
            .addStatement("throw new $T(\"Child is error or missing node: \" + $N)", IllegalStateException.class, currentNodeVar)
            .endControlFlow()
            .beginControlFlow(CodeBlock.builder()
                .add("if (")
                .add("$N.$N() == $N", currentNodeVar, jtreesitterNode.methodIsNamed(), namedParam)
                // Ignore extra nodes; they would lead to exceptions when trying to convert them to typed nodes
                .add(" && !$N.$N()", currentNodeVar, jtreesitterNode.methodIsExtra())
                .add(")")
                .build()
            )
            .addStatement("$N.add($N)", childrenVar, currentNodeVar)
            .endControlFlow()
            .endControlFlow()
            .endControlFlow("while ($N.$N())", cursorVar, jtreesitterCursor.methodGotoNextSibling())
            .endControlFlow()
            .endControlFlow()
            .addStatement("return $N", childrenVar)
            .build();
    }

    private MethodSpec generateFromNodeThrowingMethod() {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var typedNode = codeGenHelper.typedNodeConfig();

        String nodeParam = "node";
        String expectedNodeClassParam = "nodeClass";
        TypeVariableName resultTypeVar = TypeVariableName.get("T", typedNode.className());
        Class<?> thrownExceptionType = typedNode.exceptionFromNodeThrowing();

        String typedNodeVar = "typedNode";
        return MethodSpec.methodBuilder(codeGenHelper.nodeUtilsConfig().methodFromNodeThrowing())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(resultTypeVar)
            .addParameter(jtreesitterNode.className(), nodeParam)
            .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), resultTypeVar), expectedNodeClassParam)
            .addJavadoc("Converts a jtreesitter node to a typed node, throwing an {@link $T} if the node type is unknown or unexpected.", thrownExceptionType)
            .addJavadoc("\nThis method is intended for typed nodes which don't have a dedicated {@code $N} method.", codeGenHelper.typedNodeConfig().methodFromNodeThrowing())
            .returns(resultTypeVar)
            .addStatement("var $N = $T.$N($N)", typedNodeVar, typedNode.className(), typedNode.methodFromNodeThrowing(), nodeParam)
            .beginControlFlow("if ($N.isInstance($N))", expectedNodeClassParam, typedNodeVar)
            .addStatement("return $N.cast($N)", expectedNodeClassParam, typedNodeVar)
            .nextControlFlow("else")
            .addStatement("throw new $T(\"Unexpected node type, expected '\" + $N + \"' but got: \" + $N.getClass())", thrownExceptionType, expectedNodeClassParam, typedNodeVar)
            .endControlFlow()
            .build();
    }

    /**
     * Internal implementation for {@link #generateMapChildrenNamedNonNamedMethods(TypeSpec.Builder)}.
     */
    // The `named...` parameters are for converting named children
    private void generateMapChildrenNamedNonNamedMethods(TypeSpec.Builder typeBuilder, Function<TypeName, ParameterSpec.Builder> namedMapperParamSupplier, String namedMapperCode, List<Object> namedMapperArgs) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var jtreesitterNodeClass = jtreesitterNode.className();
        var nodeUtils = codeGenHelper.nodeUtilsConfig();

        var resultTypeVar = TypeVariableName.get("T", codeGenHelper.typedNodeConfig().className());
        // Mappers don't return `T` but instead `? extends T`
        var extendsResultTypeVar = WildcardTypeName.subtypeOf(resultTypeVar);

        String childrenParam = "children";

        var namedMapperParam = namedMapperParamSupplier.apply(extendsResultTypeVar)
            .addJavadoc("maps named children; {@code null} if only non-named children are expected\n")  // trailing '\n' due to https://github.com/palantir/javapoet/issues/128
            .build();

        var nonNamedMapperType = ParameterizedTypeName.get(ClassName.get(Function.class), jtreesitterNodeClass, extendsResultTypeVar);
        var nonNamedMapperParam = ParameterSpec.builder(nonNamedMapperType, "nonNamedMapper")
            .addJavadoc("maps non-named children; {@code null} if only named children are expected")
            .build();

        String namedChildrenVar = "namedChildren";
        String nonNamedChildrenVar = "nonNamedChildren";
        String childVar = "child";

        var methodBuilder = MethodSpec.methodBuilder(nodeUtils.methodMapChildrenNamedNonNamed())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(resultTypeVar)
            .addParameter(listType(jtreesitterNodeClass), childrenParam)
            // nullable
            .addParameter(namedMapperParam)
            // nullable
            .addParameter(nonNamedMapperParam)
            .returns(listType(resultTypeVar))
            .addJavadoc("Maps the children of a node (in the form of jtreesitter nodes) to typed nodes.")
            .addJavadoc("\nThis differentiates between named and non-named children, since separate typed node classes are used for them.");

        methodBuilder
            .addComment("First split between named and non-named children")
            .addStatement("var $N = new $T<$T>()", namedChildrenVar, ArrayList.class, jtreesitterNodeClass)
            .addStatement("var $N = new $T<$T>()", nonNamedChildrenVar, ArrayList.class, jtreesitterNodeClass)
            .beginControlFlow("for (var $N : $N)", childVar, childrenParam)
            .beginControlFlow("if ($N.$N())", childVar, jtreesitterNode.methodIsNamed())
            .addStatement("$N.add($N)", namedChildrenVar, childVar)
            .nextControlFlow("else")
            .addStatement("$N.add($N)", nonNamedChildrenVar, childVar)
            .endControlFlow()
            .endControlFlow();

        String resultChildrenVar = "result";
        Class<?> thrownExceptionType = IllegalArgumentException.class;
        var allNamedMappingStmtArgs = new ArrayList<>(namedMapperArgs);
        allNamedMappingStmtArgs.addFirst(namedChildrenVar);
        allNamedMappingStmtArgs.addLast(resultChildrenVar);
        // Map named children
        methodBuilder
            .addComment("Map named children (in case they are expected)")
            .addStatement("var $N = new $T<$T>()", resultChildrenVar, ArrayList.class, resultTypeVar)
            .beginControlFlow("if ($N != null)", namedMapperParam)
            .addStatement(String.format(Locale.ROOT, "$N.stream().map(%s).forEach($N::add)", namedMapperCode), allNamedMappingStmtArgs.toArray())
            .nextControlFlow("else if (!$N.isEmpty())", namedChildrenVar)
            .addStatement("throw new $T(\"Unexpected named children: \" + $N)", thrownExceptionType, namedChildrenVar)
            .endControlFlow()
            .build();

        // Map non-named children
        methodBuilder
            .addComment("Map non-named children (in case they are expected)")
            .beginControlFlow("if ($N != null)", nonNamedMapperParam)
            .addStatement("$N.stream().map($N).forEach($N::add)", nonNamedChildrenVar, nonNamedMapperParam, resultChildrenVar)
            .nextControlFlow("else if (!$N.isEmpty())", nonNamedChildrenVar)
            .addStatement("throw new $T(\"Unexpected non-named children: \" + $N)", thrownExceptionType, nonNamedChildrenVar)
            .endControlFlow();

        methodBuilder.addStatement("return $N", resultChildrenVar);

        typeBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Generates mapping methods for {@code List<Node> -> List<T extends TypedNode>} for children which
     * can be named and non-named node types.
     */
    private void generateMapChildrenNamedNonNamedMethods(TypeSpec.Builder typeBuilder) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var nodeUtils = codeGenHelper.nodeUtilsConfig();

        // Overload with dedicated mapper `Function<Node, T extends TypedNode>`
        String mapperParamName = "namedMapper";
        generateMapChildrenNamedNonNamedMethods(
            typeBuilder,
            typeVar -> ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Function.class), jtreesitterNode.className(), typeVar), mapperParamName),
            // Just provide the Function to map: `map(mapper)`
            "$N",
            List.of(mapperParamName)
        );

        // Overload with `Class<T extends TypedNode>`, which performs `Node -> TypedNode` lookup and then verifies
        // that typed node is instance of `T` using the given `Class` object
        String classParamName = "namedNodeClass";
        generateMapChildrenNamedNonNamedMethods(
            typeBuilder,
            typeVar -> ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), classParamName),
            "n -> $N(n, $N)",
            List.of(nodeUtils.methodFromNodeThrowing(), classParamName)
        );
    }

    /**
     * Generates methods which convert from a {@code List<T extends TypedNode>} to:
     * <ul>
     *     <li>at most one: {@code Optional<T>} / {@code @Nullable T}<br>({@link CodeGenHelper.NodeUtilsConfig#methodOptionalChild()})
     *     <li>exactly one: {@code T} (non-null)<br>({@link CodeGenHelper.NodeUtilsConfig#methodRequiredChild()})
     *     <li>at least one: {@code @NonEmpty List<T>}<br>({@link CodeGenHelper.NodeUtilsConfig#methodAtLeastOneChild()})
     * </ul>
     */
    private void generateNodeListConverterMethods(TypeSpec.Builder typeBuilder) {
        var nodeUtils = codeGenHelper.nodeUtilsConfig();
        var typedNodeClass = codeGenHelper.typedNodeConfig().className();

        String nodesParamName = "nodes";
        var typedNodeTypeVar = TypeVariableName.get("T", typedNodeClass);
        var typedNodeListType = listType(typedNodeTypeVar);
        var nodesParam = ParameterSpec.builder(typedNodeListType, nodesParamName)
            .build();

        Class<?> thrownExceptionType = IllegalArgumentException.class;
        var requiredChildMethod = MethodSpec.methodBuilder(nodeUtils.methodRequiredChild())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(typedNodeTypeVar)
            .addParameter(nodesParam)
            .returns(typedNodeTypeVar)
            .beginControlFlow("if ($N.size() == 1)", nodesParamName)
            .addStatement("return $N.getFirst()", nodesParamName)
            .endControlFlow()
            .addStatement("throw new $T(\"Unexpected nodes count: \" + $N)", thrownExceptionType, nodesParamName)
            .build();
        typeBuilder.addMethod(requiredChildMethod);

        String sizeVar = "size";
        String resultVar = "result";
        var optionalChildMethodBuilder = MethodSpec.methodBuilder(nodeUtils.methodOptionalChild())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(typedNodeTypeVar)
            .addParameter(nodesParam)
            .returns(codeGenHelper.getReturnOptionalType(typedNodeTypeVar))
            .addStatement("$T $N = null", typedNodeTypeVar, resultVar)
            .addStatement("int $N = $N.size()", sizeVar, nodesParamName)
            .beginControlFlow("if ($N == 1)", sizeVar)
            .addStatement("$N = $N.getFirst()", resultVar, nodesParamName)
            .nextControlFlow("else if ($N > 1)", sizeVar)
            .addStatement("throw new $T(\"Unexpected nodes count: \" + $N)", thrownExceptionType, nodesParamName)
            .endControlFlow();
        codeGenHelper.addReturnOptionalStatement(optionalChildMethodBuilder, resultVar);
        typeBuilder.addMethod(optionalChildMethodBuilder.build());

        var atLeastOneChildMethod = MethodSpec.methodBuilder(nodeUtils.methodAtLeastOneChild())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(typedNodeTypeVar)
            .returns(typedNodeListType)
            .addParameter(nodesParam)
            .beginControlFlow("if ($N.isEmpty())", nodesParamName)
            .addStatement("throw new $T(\"Expected at least one node\")", thrownExceptionType)
            .endControlFlow()
            .addStatement("return $N", nodesParamName)
            .build();
        typeBuilder.addMethod(atLeastOneChildMethod);
    }

    public JavaFile generateCode() {
        var typeBuilder = TypeSpec.classBuilder(codeGenHelper.nodeUtilsConfig().name())
            .addModifiers(Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
            .addJavadoc("Internal helper class.");

        typeBuilder.addMethod(generateFromNodeThrowingMethod());
        typeBuilder.addMethod(generateGetNonFieldChildrenMethod());
        generateMapChildrenNamedNonNamedMethods(typeBuilder);

        generateNodeListConverterMethods(typeBuilder);

        return codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build();
    }
}
