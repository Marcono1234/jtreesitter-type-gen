package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

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

    /*
     * It looks like `Node#getChildren` / `Node#getNamedChildren` also includes children of fields, which is not
     * desired here. But it seems jtreesitter (or even tree-sitter itself) does not offer a method
     * to get all non-field children only, therefore need to perform this ourselves here.
     *
     * The implementation below first gets all (named) children, then removes all children which belong to fields.
     * This is rather inefficient. Maybe would be a bit more efficient to use `getFieldNameForChild` (not tested):
     * ```
     * var children = new ArrayList<Node>();
     * for (int i = 0; i < node.getChildCount(); i++) {
     *     if (node.getFieldNameForChild(i) == null) {
     *         var child = node.getChild(i).get();
     *         if (...) {  // additional checks, e.g. `isNamed`, `!isExtra`, ...
     *             children.add(child);
     *         }
     *     }
     * }
     * ```
     * But `Node#getFieldNameForChild` crashes the JVM in some cases; see https://github.com/tree-sitter/java-tree-sitter/issues/20
     * TODO: Use `getFieldNameForChild` once fix for it is released? Is that really more efficient though?
     */
    private MethodSpec generateGetNonFieldChildrenMethod() {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var jtreesitterNodeClass = jtreesitterNode.className();
        
        String nodeParam = "node";
        var fieldNamesParam = ParameterSpec.builder(ArrayTypeName.of(String.class), "fields")
            .addJavadoc("names of all fields; the implementation requires this to filter out field children\n")  // trailing '\n' due to https://github.com/palantir/javapoet/issues/128
            .build();
        var namedParam = ParameterSpec.builder(boolean.class, "named")
            .addJavadoc("whether to return named or non-named children")
            .build();

        String childrenVar = "children";
        String fieldNameVar = "field";
        var methodBuilder = MethodSpec.methodBuilder(codeGenHelper.nodeUtilsConfig().methodGetNonFieldChildren())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(jtreesitterNodeClass, nodeParam)
            .addParameter(fieldNamesParam)
            .addParameter(namedParam)
            .returns(listType(jtreesitterNodeClass))
            .addJavadoc("Gets all non-field children of the node.");

        String childrenStreamVar = "childrenStream";
        methodBuilder
            .addComment("First get all relevant children")
            .addStatement("var $N = new $T<$T>()", childrenVar, ArrayList.class, jtreesitterNodeClass)
            .addStatement("$T<$T> $N", Stream.class, jtreesitterNodeClass, childrenStreamVar)
            .beginControlFlow("if ($N)", namedParam)
            .addStatement("$N = $N.$N().stream()", childrenStreamVar, nodeParam, jtreesitterNode.methodGetNamedChildren())
            .nextControlFlow("else")
            .addStatement("$N = $N.$N().stream().filter(n -> !n.$N())", childrenStreamVar, nodeParam, jtreesitterNode.methodGetChildren(), jtreesitterNode.methodIsNamed())
            .endControlFlow()
            .addStatement("$N.filter(n -> !n.$N() && !n.$N() && !n.$N()).forEach($N::add)",
                childrenStreamVar, jtreesitterNode.methodIsError(), jtreesitterNode.methodIsMissing(), jtreesitterNode.methodIsExtra(), childrenVar
            );

        methodBuilder
            .addComment("Then remove all field children")
            .beginControlFlow("for (var $N : $N)", fieldNameVar, fieldNamesParam)
            // Return fast if there are no children anymore
            .beginControlFlow("if ($N.isEmpty())", childrenVar)
            .addStatement("return $N", childrenVar)
            .endControlFlow()
            // Remove all children of the field
            .addStatement("$N.removeAll($N.$N($N))", childrenVar, nodeParam, jtreesitterNode.methodGetChildrenByFieldName(), fieldNameVar)
            .endControlFlow()
            .addStatement("return $N", childrenVar)
            .build();

        return methodBuilder.build();
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

    // TODO: This generated mapping method is not actually used anymore; see also (non-existent) callers
    //   of `NodeUtilsConfig#methodMapChildren()`
    /**
     * Internal implementation for {@link #generateMapChildrenMethods(TypeSpec.Builder)}.
     */
    private void generateMapChildrenMethods(TypeSpec.Builder typeBuilder, Function<TypeVariableName, ParameterSpec> mapperParamSupplier, String mapperCode, List<Object> mapperArgs) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var nodeUtils = codeGenHelper.nodeUtilsConfig();
        String methodName = nodeUtils.methodMapChildren();

        String childrenVar = "children";

        var resultTypeVar = TypeVariableName.get("T", codeGenHelper.typedNodeConfig().className());

        var allMappingStmtArgs = new ArrayList<>(mapperArgs);
        allMappingStmtArgs.addFirst(childrenVar);

        var method = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(resultTypeVar)
            .addParameter(listType(jtreesitterNode.className()), childrenVar)
            .addParameter(mapperParamSupplier.apply(resultTypeVar))
            .returns(listType(resultTypeVar))
            .addStatement(String.format(Locale.ROOT, "return $N.stream().map(%s).toList()", mapperCode), allMappingStmtArgs.toArray())
            .build();
        typeBuilder.addMethod(method);
    }

    /**
     * Generates mapping methods for {@code List<Node> -> List<T extends TypedNode>} for children which
     * are only named node types.
     */
    private void generateMapChildrenMethods(TypeSpec.Builder typeBuilder) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var nodeUtils = codeGenHelper.nodeUtilsConfig();

        // Overload with dedicated mapper `Function<Node, T extends TypedNode>`
        String mapperParamName = "mapper";
        generateMapChildrenMethods(
            typeBuilder,
            typeVar -> ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Function.class), jtreesitterNode.className(), typeVar), mapperParamName).build(),
            // Just provide the Function to map: `map(mapper)`
            "$N",
            List.of(mapperParamName)
        );

        // Overload with `Class<T extends TypedNode>`, which performs `Node -> TypedNode` lookup and then verifies
        // that typed node is instance of `T` using the given `Class` object
        String classParamName = "nodeClass";
        generateMapChildrenMethods(
            typeBuilder,
            typeVar -> ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), typeVar), classParamName).build(),
            "n -> $N(n, $N)",
            List.of(nodeUtils.methodFromNodeThrowing(), classParamName)
        );
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
        generateMapChildrenMethods(typeBuilder);
        generateMapChildrenNamedNonNamedMethods(typeBuilder);

        generateNodeListConverterMethods(typeBuilder);

        return codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build();
    }
}
