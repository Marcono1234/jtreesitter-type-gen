package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
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
     * Generates mapping methods for {@code List<Node> -> List<T extends TypedNode>} for children which
     * can be named and non-named node types.
     */
    private void generateMapChildrenNamedNonNamedMethods(TypeSpec.Builder typeBuilder) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var jtreesitterNodeClass = jtreesitterNode.className();
        var nodeUtils = codeGenHelper.nodeUtilsConfig();

        var resultTypeVar = TypeVariableName.get("T", codeGenHelper.typedNodeConfig().className());
        // Mappers don't return `T` but instead `? extends T`
        var extendsResultTypeVar = WildcardTypeName.subtypeOf(resultTypeVar);

        var childrenParam = ParameterSpec.builder(listType(jtreesitterNodeClass), "children").build();

        var mapperType = ParameterizedTypeName.get(ClassName.get(Function.class), jtreesitterNodeClass, extendsResultTypeVar);
        var namedMapperParam = ParameterSpec.builder(mapperType, "namedMapper")
            .addJavadoc("maps named children; {@code null} if only non-named children are expected\n")  // trailing '\n' due to https://github.com/palantir/javapoet/issues/128
            .build();

        var nonNamedMapperParam = ParameterSpec.builder(mapperType, "nonNamedMapper")
            .addJavadoc("maps non-named children; {@code null} if only named children are expected")
            .build();

        var methodBuilder = MethodSpec.methodBuilder(nodeUtils.methodMapChildrenNamedNonNamed())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(resultTypeVar)
            .addParameter(childrenParam)
            // nullable
            .addParameter(namedMapperParam)
            // nullable
            .addParameter(nonNamedMapperParam)
            .returns(listType(resultTypeVar))
            .addJavadoc("Maps the children of a node (in the form of jtreesitter nodes) to typed nodes.")
            .addJavadoc("\nThis differentiates between named and non-named children, since separate typed node classes are used for them.");

        String childVar = "child";
        Class<?> thrownExceptionType = IllegalArgumentException.class;
        // Note: Ideally would use `addStatement` here but that does not work due to https://github.com/square/javapoet/issues/711
        methodBuilder.addCode(CodeBlock.builder()
            .add("return $N.stream().map($N -> {\n", childrenParam, childVar)
            .indent()
            .beginControlFlow("if ($N.$N())", childVar, jtreesitterNode.methodIsNamed())
            .addStatement("if ($N == null) throw new $T(\"Unexpected named child: \" + $N)", namedMapperParam, thrownExceptionType, childVar)
            .addStatement("return $N.apply($N)", namedMapperParam, childVar)
            .nextControlFlow("else")
            .addStatement("if ($N == null) throw new $T(\"Unexpected non-named child: \" + $N)", nonNamedMapperParam, thrownExceptionType, childVar)
            .addStatement("return $N.apply($N)", nonNamedMapperParam, childVar)
            .endControlFlow()
            .unindent()
            .add("}).toList();") // trailing ';' due to being unable to use `addStatement` above
            .build()
        );
        var method = methodBuilder.build();
        typeBuilder.addMethod(method);


        // Generate overload with `Class<T extends TypedNode>`, which performs `Node -> TypedNode` lookup and then verifies
        // that typed node is instance of `T` using the given `Class` object
        String namedClassParam = "namedNodeClass";
        typeBuilder.addMethod(MethodSpec.methodBuilder(method.name())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(resultTypeVar)
            .addParameter(childrenParam)
            .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), extendsResultTypeVar), namedClassParam)
                .addJavadoc("class of the named children; {@code null} if only non-named children are expected\n")  // trailing '\n' due to https://github.com/palantir/javapoet/issues/128)
                .build()
            )
            .addParameter(nonNamedMapperParam)
            .returns(listType(resultTypeVar))
            // Delegate to other overload
            .addStatement("return $N($N, n -> $N(n, $N), $N)", method.name(), childrenParam, nodeUtils.methodFromNodeThrowing(), namedClassParam, nonNamedMapperParam)
            .build()
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
