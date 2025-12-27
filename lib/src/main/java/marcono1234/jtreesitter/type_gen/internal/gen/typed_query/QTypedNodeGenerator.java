package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.TypedQueryNameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.*;
import marcono1234.jtreesitter.type_gen.internal.gen.typed_query.TypedQueryConfig.QTypedNodeConfig;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;

import java.util.*;

import static marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.*;

/**
 * Code generator for {@code QTypedNode} and subclasses.
 */
class QTypedNodeGenerator {
    private final TypedQueryNameGenerator nameGenerator;
    private final CodeGenHelper codeGenHelper;
    private final QNodeCommonGenerator qNodeCommonGenerator;
    private final TypedQueryConfig typedQueryConfig;
    private final TypeVariableName typeVarCollector;
    private final TypeVariableName typeVarNodeBound;

    private final QTypedNodeConfig qTypedNodeConfig;
    /** {@link QTypedNodeConfig.DataConfig#name()} parameterized with {@link #typeVarCollector} */
    private final ParameterizedTypeName typeDataParameterized;
    /** {@link QTypedNodeConfig#classChildEntry()} parameterized with {@link #typeVarCollector} */
    private final ParameterizedTypeName typeChildEntryParameterized;

    /** Type {@code QNode<C, ?>} */
    private final ParameterizedTypeName typeQNodeWildcard;
    /** Type {@code QNodeImpl<C, ?>} */
    private final ParameterizedTypeName typeQNodeImplWildcard;

    private final ParameterSpec paramQueryStringBuilder;
    private final ParameterSpec paramCaptureRegistry;
    private final ParameterSpec paramPredicateRegistry;

    QTypedNodeGenerator(QNodeCommonGenerator qNodeCommonGenerator, TypedQueryNameGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
        this.qNodeCommonGenerator = qNodeCommonGenerator;
        this.codeGenHelper = qNodeCommonGenerator.codeGenHelper;
        this.typedQueryConfig = qNodeCommonGenerator.typedQueryConfig;
        this.typeVarCollector = qNodeCommonGenerator.typeVarCollector;
        this.typeVarNodeBound = qNodeCommonGenerator.typeVarNodeBound;

        this.qTypedNodeConfig = typedQueryConfig.qTypedNodeConfig();
        this.typeDataParameterized = ParameterizedTypeName.get(qTypedNodeConfig.dataConfig().name(), typeVarCollector);
        this.typeChildEntryParameterized = ParameterizedTypeName.get(qTypedNodeConfig.classChildEntry(), typeVarCollector);

        this.typeQNodeWildcard = ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, unboundedWildcard());
        this.typeQNodeImplWildcard = ParameterizedTypeName.get(typedQueryConfig.qNodeImplConfig().name(), typeVarCollector, unboundedWildcard());

        this.paramQueryStringBuilder = qNodeCommonGenerator.paramQueryStringBuilder;
        this.paramCaptureRegistry = qNodeCommonGenerator.paramCaptureRegistry;
        this.paramPredicateRegistry = qNodeCommonGenerator.paramPredicateRegistry;
    }

    // TODO: Define this in ...Config class instead?
    private final String methodNameChildBuildQuery = "buildQuery";

    private MethodSpec.Builder createChildBuildQuerySignature() {
        return MethodSpec.methodBuilder(methodNameChildBuildQuery)
            // Note: This method is not part of the API; it is only a public method of an internal interface
            .addModifiers(Modifier.PUBLIC)
            .addParameter(paramQueryStringBuilder)
            .addParameter(paramCaptureRegistry)
            .addParameter(paramPredicateRegistry);
    }

    private MethodSpec.Builder createChildBuildQueryOverride() {
        return createChildBuildQuerySignature().addAnnotation(Override.class);
    }

    private TypeSpec generateInterfaceChildEntry() {
        var methodBuildQuery = createChildBuildQuerySignature().addModifiers(Modifier.ABSTRACT).build();

        return TypeSpec.interfaceBuilder(qTypedNodeConfig.classChildEntry())
            .addModifiers(Modifier.PRIVATE)
            .addTypeVariable(typeVarCollector)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateRecordChild() {
        var fieldNode = ParameterSpec.builder(typeQNodeImplWildcard, "node").build();

        var constructor = MethodSpec.compactConstructorBuilder()
            .addStatement(createNonNullCheck(fieldNode))
            .build();

        var methodBuildQuery = createChildBuildQueryOverride()
            .addStatement(qNodeCommonGenerator.createDelegatingBuildQueryCall(fieldNode.name()))
            .build();

        return TypeSpec.recordBuilder(qTypedNodeConfig.classChild())
            .addModifiers(Modifier.PRIVATE)
            .addTypeVariable(typeVarCollector)
            .addSuperinterface(typeChildEntryParameterized)
            .recordConstructor(canonicalRecordConstructor(fieldNode))
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateRecordField() {
        var fieldName = ParameterSpec.builder(String.class, "name").build();
        var fieldNode = ParameterSpec.builder(typeQNodeImplWildcard, "node").build();

        var constructor = MethodSpec.compactConstructorBuilder()
            .addStatement(createNonNullCheck(fieldName))
            .addStatement(createNonNullCheck(fieldNode))
            .build();

        var methodBuildQuery = createChildBuildQueryOverride()
            .addStatement("$N.append($N).append(\": \")", paramQueryStringBuilder, fieldName)
            .addStatement(qNodeCommonGenerator.createDelegatingBuildQueryCall(fieldNode.name()))
            .build();

        return TypeSpec.recordBuilder(qTypedNodeConfig.classField())
            .addModifiers(Modifier.PRIVATE)
            .addTypeVariable(typeVarCollector)
            .addSuperinterface(typeChildEntryParameterized)
            .recordConstructor(canonicalRecordConstructor(fieldName, fieldNode))
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    // TODO: Define this in ...Config class instead?
    private final String methodNameAnchorInstance = "instance";

    private TypeSpec generateClassAnchor() {
        var className = qTypedNodeConfig.classAnchor();
        var constantInstance = FieldSpec.builder(ParameterizedTypeName.get(className, unboundedWildcard()), "INSTANCE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T<>()", className)
            .build();

        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();

        var typeAnchorParameterized = ParameterizedTypeName.get(className, typeVarCollector);
        var methodInstance = MethodSpec.methodBuilder(methodNameAnchorInstance)
            .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
            .addModifiers(Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .returns(typeAnchorParameterized)
            .addStatement("return ($T) $N", typeAnchorParameterized, constantInstance)
            .build();

        var methodBuildQuery = createChildBuildQueryOverride()
            .addStatement("$N.append('.')", paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addSuperinterface(typeChildEntryParameterized)
            .addField(constantInstance)
            .addMethod(constructor)
            .addMethod(methodInstance)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateRecordData() {
        var dataConfig = qTypedNodeConfig.dataConfig();

        // Have a single list combining non-field and field children because apparently anchors can appear
        // between them, and order matters in that case
        var fieldChildren = ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), typeChildEntryParameterized), "children").build();
        var fieldWithFieldNames = ParameterSpec.builder(ParameterizedTypeName.get(Set.class, String.class), "withFieldNames").build();
        // SequencedSet to preserve order
        var fieldWithoutFields = ParameterSpec.builder(ParameterizedTypeName.get(SequencedSet.class, String.class), "withoutFields").build();

        var constructor = MethodSpec.constructorBuilder()
            .addStatement("this($T.of(), new $T<>(), new $T<>())", List.class, LinkedHashSet.class, LinkedHashSet.class)
            .build();

        // Note: Don't check multiplicity for children, because this method can also be used for special nodes where multiplicity
        // might not apply, e.g. error or extra nodes
        MethodSpec methodWithChildren;
        {
            var qNodeImplConfig = typedQueryConfig.qNodeImplConfig();
            String paramAdditionalChildren = "additionalChildren";
            methodWithChildren = MethodSpec.methodBuilder(dataConfig.methodWithChildren())
                .addAnnotation(SafeVarargs.class)
                .addAnnotation(SUPPRESS_WARNINGS_VARARGS)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(typeDataParameterized)
                .addParameter(ArrayTypeName.of(typeQNodeWildcard), paramAdditionalChildren)
                .varargs()
                // Use field name also as name for local variable
                .addStatement("var $N = new $T<>(this.$N)", fieldChildren, ArrayList.class, fieldChildren)
                .addStatement(CodeBlock.builder()
                    .add("$T.stream($N)", Arrays.class, paramAdditionalChildren)
                    // Note: These `map` operations perform null checks for the elements
                    .add(".map($T::$N)", qNodeImplConfig.name(), qNodeImplConfig.methodFromNode())
                    .add(".map($T::new)", qTypedNodeConfig.classChild())
                    .add(".forEach($N::add)", fieldChildren)
                    .build()
                )
                .addStatement("return new $T<>($N, $N, $N)", dataConfig.name(), fieldChildren, fieldWithFieldNames, fieldWithoutFields)
                .build();
        }

        MethodSpec methodWithField;
        {
            var qNodeImplConfig = typedQueryConfig.qNodeImplConfig();
            String paramFieldName = "fieldName";
            String paramFieldNode = "fieldNode";
            String paramAllowMultiple = "allowMultiple";
            methodWithField = MethodSpec.methodBuilder(dataConfig.methodWithField())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeDataParameterized)
                .addParameter(String.class, paramFieldName)
                .addParameter(typeQNodeWildcard, paramFieldNode)
                .addParameter(boolean.class, paramAllowMultiple)
                .addStatement(createNonNullCheck(paramFieldName))
                .addStatement(createNonNullCheck(paramFieldNode))
                .addStatement("if ($N.contains($N)) throw new $T(\"Field '\" + $N + \"' has already been added to \\\"without fields\\\"\")", fieldWithoutFields, paramFieldName, IllegalStateException.class, paramFieldName)
                // Use field name also as name for local variable
                .addStatement("var $N = new $T<>(this.$N)", fieldWithFieldNames, HashSet.class, fieldWithFieldNames)
                .addStatement("if (!$N.add($N) && !$N) throw new $T(\"Field '\" + $N + \"' has already been added\")", fieldWithFieldNames, paramFieldName, paramAllowMultiple, IllegalStateException.class, paramFieldName)
                // Use field name also as name for local variable
                .addStatement("var $N = new $T<>(this.$N)", fieldChildren, ArrayList.class, fieldChildren)
                .addStatement("$N.add(new $T<>($N, $T.$N($N)))", fieldChildren, qTypedNodeConfig.classField(), paramFieldName, qNodeImplConfig.name(), qNodeImplConfig.methodFromNode(), paramFieldNode)
                .addStatement("return new $T<>($N, $N, $N)", dataConfig.name(), fieldChildren, fieldWithFieldNames, fieldWithoutFields)
                .build();
        }

        MethodSpec methodWithoutField;
        {
            String paramFieldName = "fieldName";
            methodWithoutField = MethodSpec.methodBuilder(dataConfig.methodWithoutField())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeDataParameterized)
                .addParameter(String.class, paramFieldName)
                .addStatement(createNonNullCheck(paramFieldName))
                .addStatement("if ($N.contains($N)) throw new $T(\"Field '\" + $N + \"' has already been added to \\\"with fields\\\"\")", fieldWithFieldNames, paramFieldName, IllegalStateException.class, paramFieldName)
                // Use field name also as name for local variable
                .addStatement("var $N = new $T<>(this.$N)", fieldWithoutFields, LinkedHashSet.class, fieldWithoutFields)
                .addStatement("$N.add($N)", fieldWithoutFields, paramFieldName)
                .addStatement("return new $T<>($N, $N, $N)", dataConfig.name(), fieldChildren, fieldWithFieldNames, fieldWithoutFields)
                .build();
        }

        var methodWithAnchor = MethodSpec.methodBuilder(dataConfig.methodWithAnchor())
            .addModifiers(Modifier.PUBLIC)
            .returns(typeDataParameterized)
            .addStatement("if (!$N.isEmpty() && $N.getLast() instanceof $T) throw new $T(\"Duplicate anchor is not valid\")", fieldChildren, fieldChildren, qTypedNodeConfig.classAnchor(), IllegalStateException.class)
            // Use field name also as name for local variable
            .addStatement("var $N = new $T<>(this.$N)", fieldChildren, ArrayList.class, fieldChildren)
            .addStatement("$N.add($T.$N())", fieldChildren, qTypedNodeConfig.classAnchor(), methodNameAnchorInstance)
            .addStatement("return new $T<>($N, $N, $N)", dataConfig.name(), fieldChildren, fieldWithFieldNames, fieldWithoutFields)
            .build();

        var methodVerifyValidState = MethodSpec.methodBuilder(dataConfig.methodVerifyValidState())
            .addStatement("if ($N.size() == 1 && $N.getFirst() instanceof $T) throw new $T(\"Must specify children or fields when using `$N`\")", fieldChildren, fieldChildren, qTypedNodeConfig.classAnchor(), IllegalStateException.class, qTypedNodeConfig.methodWithChildAnchor())
            .build();

        var methodBuildQuery = MethodSpec.methodBuilder(dataConfig.methodBuildQuery())
            .addParameter(paramQueryStringBuilder)
            .addParameter(paramCaptureRegistry)
            .addParameter(paramPredicateRegistry)
            // Apparently have to write 'without fields' first, because tree-sitter query parser permits them in front
            // of "first child" anchor (`(my_node !field . (child))`), but disallows them after "last child" anchor (`(my_node (child) . !field)`)
            .addStatement("$N.forEach(f -> $N.append(\" !\").append(f))", fieldWithoutFields, paramQueryStringBuilder)
            .addStatement(CodeBlock.builder()
                .add("$N.forEach(c -> {\n", fieldChildren)
                .indent()
                .add("$N.append(' ');\n", paramQueryStringBuilder)
                .add("c.$N($N, $N, $N);\n", methodNameChildBuildQuery, paramQueryStringBuilder, paramCaptureRegistry, paramPredicateRegistry)
                .unindent()
                .add("})")
                .build()
            )
            .build();

        return TypeSpec.recordBuilder(dataConfig.name())
            .addTypeVariable(typeVarCollector)
            .recordConstructor(canonicalRecordConstructor(fieldChildren, fieldWithFieldNames, fieldWithoutFields))
            .addMethod(constructor)
            .addMethod(methodWithChildren)
            .addMethod(methodWithField)
            .addMethod(methodWithoutField)
            .addMethod(methodWithAnchor)
            .addMethod(methodVerifyValidState)
            .addMethod(methodBuildQuery)
            .build();
    }

    /**
     * Generates the {@code QTypedNode} class.
     */
    public TypeSpec generateClass() {
        var fieldSupertype = FieldSpec.builder(String.class, "supertype", Modifier.PRIVATE, Modifier.FINAL)
            .addJavadoc("nullable")  // don't use @Nullable to not make any assumptions about libraries used by jtreesitter
            .build();
        var fieldNodeType = FieldSpec.builder(String.class, "nodeType", Modifier.PRIVATE, Modifier.FINAL).build();
        // Package-private because subclasses have to access it
        var fieldData = FieldSpec.builder(typeDataParameterized, "data", Modifier.FINAL).build();

        var constructor = createInitializingConstructorBuilder(fieldSupertype, fieldNodeType, fieldData)
            .addModifiers(Modifier.PRIVATE)
            .build();

        var constructorNodeType = MethodSpec.constructorBuilder()
            .addParameter(paramFromField(fieldNodeType))
            .addStatement("this(null, $N, new $T<>())", fieldNodeType, typeDataParameterized.rawType())
            .build();

        var paramQTypedNodeOld = ParameterSpec.builder(ParameterizedTypeName.get(qTypedNodeConfig.name(), typeVarCollector, typeVarNodeBound), "old").build();

        // Constructor for setting a new supertype
        var constructorNewSupertype = MethodSpec.constructorBuilder()
            .addParameter(paramQTypedNodeOld)
            .addParameter(paramFromField(fieldSupertype))
            .addStatement("this($N, $N.$N, $N.$N)", fieldSupertype, paramQTypedNodeOld, fieldNodeType, paramQTypedNodeOld, fieldData)
            .build();

        // Constructor for setting new data
        var constructorNewData = MethodSpec.constructorBuilder()
            .addParameter(paramQTypedNodeOld)
            .addParameter(paramFromField(fieldData))
            .addStatement("this($N.$N, $N.$N, $N)", paramQTypedNodeOld, fieldSupertype, paramQTypedNodeOld, fieldNodeType, fieldData)
            .build();

        // Note: Don't generate common methods such as `withChildAnchor()` here, because these methods are only relevant
        // for query nodes of regular node types but not for supertype node types

        var methodVerifyValidState = MethodSpec.methodBuilder(typedQueryConfig.qNodeImplConfig().methodVerifyValidState())
            .addAnnotation(Override.class)
            .addStatement("$N.$N()", qTypedNodeConfig.fieldData(), qTypedNodeConfig.dataConfig().methodVerifyValidState())
            .build();

        var methodBuildQuery = qNodeCommonGenerator.createBuildQueryImplOverride()
            .addStatement("$N.append('(')", paramQueryStringBuilder)
            .beginControlFlow("if ($N != null)", fieldSupertype)
            .addStatement("$N.append($N).append('/')", paramQueryStringBuilder, fieldSupertype)
            .endControlFlow()
            .addStatement("$N.append($N)", paramQueryStringBuilder, fieldNodeType)
            .addStatement("$N.$N($N, $N, $N)", fieldData, qTypedNodeConfig.dataConfig().methodBuildQuery(), paramQueryStringBuilder, paramCaptureRegistry, paramPredicateRegistry)
            .addStatement("$N.append(')')", paramQueryStringBuilder)
            .build();

        var providedBySubclassesJavadoc = CodeBlock.of("\n<br>(provided by subclasses of {@code $N}; depends on the node type represented by the query builder class)", qTypedNodeConfig.name().simpleName());
        return TypeSpec.classBuilder(qTypedNodeConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            .superclass(ParameterizedTypeName.get(typedQueryConfig.classQCapturableQuantifiable(), typeVarCollector, typeVarNodeBound))
            .addJavadoc("Base type for all query builder classes representing named node types as defined in the Tree-sitter grammar.")
            .addJavadoc("\n\n<p>The following query functionality is provided. If multiple of these methods are used, they must be applied in this order.")
            .addJavadoc("\nWhen used in a different order the query builder API might not support calling all of these methods.")
            .addJavadoc("\n<ol>")
            .addJavadoc("\n<li>children and field requirements")
            .addJavadoc(providedBySubclassesJavadoc)
            .addJavadoc("\n<li>supertype requirements, to match only contexts where the node is used as subtype of one of its supertypes")
            .addJavadoc(providedBySubclassesJavadoc)
            .addJavadoc("\n<li>{@linkplain $T quantifying matches}", typedQueryConfig.qQuantifiableConfig().name())
            .addJavadoc("\n<li>{@linkplain $T filtering nodes}", typedQueryConfig.qFilterableConfig().name())
            .addJavadoc("\n<li>{@linkplain $T capturing matching nodes}", typedQueryConfig.qCapturableConfig().name())
            .addJavadoc("\n<li>as 'extra' node (method {@code $N})", qTypedNodeConfig.methodAsExtra())
            .addJavadoc(providedBySubclassesJavadoc)
            .addJavadoc("\n</ol>")
            .addJavadoc("\nHere is an example applying all of these:")
            .addJavadoc("\n{@snippet lang=java :")
            .addJavadoc("\nQNodeMyNode.$N(", qTypedNodeConfig.methodAsExtra())
            .addJavadoc("\n  q.nodeMyNode()")
            .addJavadoc("\n    .$N(...)", qTypedNodeConfig.methodWithChildren())
            .addJavadoc("\n    .asSubtypeOfMySupertype()")
            .addJavadoc("\n    .$N()", typedQueryConfig.qQuantifiableConfig().methodZeroOrMore())
            .addJavadoc("\n    .$N(...)", typedQueryConfig.qFilterableConfig().methodTextEq())
            .addJavadoc("\n    .$N(...)", typedQueryConfig.qCapturableConfig().methodCaptured())
            .addJavadoc("\n)")
            .addJavadoc("\n}")
            .addType(generateInterfaceChildEntry())
            .addType(generateRecordChild())
            .addType(generateRecordField())
            .addType(generateClassAnchor())
            .addType(generateRecordData())
            .addField(fieldSupertype)
            .addField(fieldNodeType)
            .addField(fieldData)
            .addMethod(constructor)
            .addMethod(constructorNodeType)
            .addMethod(constructorNewSupertype)
            .addMethod(constructorNewData)
            .addMethod(methodVerifyValidState)
            .addMethod(methodBuildQuery)
            .build();
    }

    /**
     * Adds Java members for handling node supertypes (if any).
     */
    private void addSupertypesMembers(TypeSpec.Builder typeBuilder, NodeData nodeData, ParameterSpec paramOld, SequencedSet<GenSupertypeNodeType> supertypes, boolean isRegularNode) {
        if (supertypes.isEmpty()) {
            return;
        }

        String paramSupertype = "supertype";
        var constructorNewSupertype = MethodSpec.constructorBuilder()
            .addParameter(paramOld)
            .addParameter(String.class, paramSupertype)
            .addStatement("super($N, $N)", paramOld, paramSupertype)
            .build();

        typeBuilder.addMethod(constructorNewSupertype);

        for (var supertype : supertypes) {
            String methodName = nameGenerator.generateAsSubtypeMethodName(nodeData.nodeType(), supertype.getTypeName());
            var methodAsSubtypeBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                // Return `QTypedNode` instead of current subclass to only allow setting supertype once
                .returns(ParameterizedTypeName.get(qTypedNodeConfig.name(), typeVarCollector, nodeData.typedNodeClass()))
                .addJavadoc("Returns this node builder as subtype of its supertype $L.", CodeGenHelper.createJavadocCodeTag(supertype.getTypeName()))
                .addJavadoc("\n\n<p>This can be useful to restrict matches to locations where the node is used as subtype of that supertype")
                .addJavadoc("\nand to exclude all other occurrences. For example consider a node type 'identifier' for which the query should")
                .addJavadoc("\nonly match the occurrences where it is used as 'expression' (its supertype) and not other usages such as")
                .addJavadoc("\nthe name of a variable declaration.");

            if (isRegularNode) {
                methodAsSubtypeBuilder.addJavadoc("\n\n<p>Note that this method returns {@code $N}, so adding children or field requirements has to be done before calling this method.", qTypedNodeConfig.name().simpleName());
            }

            typeBuilder.addMethod(methodAsSubtypeBuilder
                .addJavadoc(TreeSitterDoc.SUPERTYPE_NODE.createJavadocSee())
                .addStatement("return new $T<>(this, $T.$N)", nodeData.queryNodeClass(), supertype.createJavaTypeName(codeGenHelper), supertype.getTypeNameConstant())
                .build()
            );
        }
    }

    /**
     * @param queryNodeClassParameterized
     *      {@link #queryNodeClass()} parameterized with {@link QTypedNodeGenerator#typeVarCollector}
     * @param builderMethodName
     *      name of the corresponding factory method in the {@code Builder} class
     * @param nodeType
     *      node type name, as it appears in {@code node-types.json}
     * @param isExtra
     *      whether this node type is an 'extra' node
     */
    private record NodeData(
        ClassName queryNodeClass,
        ParameterizedTypeName queryNodeClassParameterized,
        String builderMethodName,
        ClassName typedNodeClass,
        String nodeType,
        boolean isExtra
    ) {}

    /**
     * If the node type is an 'extra' node, adds an {@code asExtra} method which uses an unbound {@code <N>} to
     * permit using the node at any location in the query.
     */
    private void addAsExtraMethod(TypeSpec.Builder typeBuilder, NodeData nodeData) {
        if (!nodeData.isExtra()) {
            return;
        }

        // Note: For convenience would ideally generate this as instance method instead of as `static` method; however
        // to provide all query builder functionality, would need to add `...Extra` variants of `QCapturableQuantified` ...
        // which would be quite cumbersome to generate

        String paramNode = "node";
        // Create type var without bounds
        var typeVarNode = TypeVariableName.get(typeVarNodeBound.name());
        // Return as `QNode` instead of `QTypedNode` because `N` is unbound so capturing or filtering would not be type-safe anymore
        var typeQNodeUnbound = ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, typeVarNode);
        var methodAsExtra = MethodSpec.methodBuilder(qTypedNodeConfig.methodAsExtra())
            .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .returns(typeQNodeUnbound)
            .addParameter(
                ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, WildcardTypeName.subtypeOf(nodeData.typedNodeClass())),
                paramNode
            )
            .addJavadoc("Converts the given query node to an unbound 'extra' query node which can appear anywhere in the query.")
            .addJavadoc("\nJust like the corresponding 'extra' node type in the Tree-sitter grammar which can appear anywhere in the input.")
            .addJavadoc("\n\n<p>This method should be applied after all other match requirements such as children or fields have already")
            .addJavadoc("\nbeen specified, because this method returns {@code $N} and does not allow any further configuration afterwards.", typedQueryConfig.qNodeConfig().name().simpleName())
            .addJavadoc("\n")
            .addJavadoc("\n<h4>Example</h4>")
            .addJavadoc("\n{@snippet lang=java :")
            .addJavadoc("\nq.nodeMyNode().$N(", qTypedNodeConfig.methodWithChildren())
            .addJavadoc("\n  // Due to `$N` can use this node here, even though it is not listed as explicit child type", qTypedNodeConfig.methodAsExtra())
            .addJavadoc("\n  $N.$N(q.$N()) // @highlight substring=\"$N\"", nodeData.queryNodeClass().simpleName(), qTypedNodeConfig.methodAsExtra(), nodeData.builderMethodName(), qTypedNodeConfig.methodAsExtra())
            .addJavadoc("\n)")
            .addJavadoc("\n}")
            .addStatement(createNonNullCheck(paramNode))
            .addStatement("return ($T) $N", typeQNodeUnbound, paramNode)
            .build();
        typeBuilder.addMethod(methodAsExtra);
    }

    /**
     * @param className qualified name of the subclass
     * @param javaFile Java file containing the subclass source code
     */
    public record QTypedNodeSubclassData(ClassName className, JavaFile javaFile) {
    }

    /**
     * Generates a {@code QTypedNode} subclass for the given node.
     */
    public QTypedNodeSubclassData generateQTypedNodeSubclass(GenNodeType node, String builderMethodName) {
        String nodeType = node.getTypeName();
        var queryNodeClass = codeGenHelper.createOwnClassName(nameGenerator.generateBuilderClassName(nodeType));
        var nodeData = new NodeData(
            queryNodeClass,
            ParameterizedTypeName.get(queryNodeClass, typeVarCollector),
            builderMethodName,
            node.createJavaTypeName(codeGenHelper),
            nodeType,
            node.isExtra()
        );

        return switch (node) {
            case GenRegularNodeType regularNode -> generateRegularNode(regularNode, nodeData);
            case GenSupertypeNodeType supertypeNode -> generateSupertypeNode(supertypeNode, nodeData);
        };
    }

    private QTypedNodeSubclassData generateSupertypeNode(GenSupertypeNodeType node, NodeData nodeData) {
        var constructor = MethodSpec.constructorBuilder()
            .addStatement("super($T.$N)", nodeData.typedNodeClass(), node.getTypeNameConstant())
            .build();

        var typeBuilder = TypeSpec.classBuilder(nodeData.queryNodeClass())
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(typeVarCollector)
            .superclass(ParameterizedTypeName.get(qTypedNodeConfig.name(), typeVarCollector, nodeData.typedNodeClass()))
            .addJavadoc("Query builder for node type $L.", CodeGenHelper.createJavadocCodeTag(nodeData.nodeType()))
            .addJavadoc("\nInstances can be obtained using {@link $T#$N()}.", typedQueryConfig.builderConfig().name(), nodeData.builderMethodName())
            .addMethod(constructor);

        var paramOld = ParameterSpec.builder(nodeData.queryNodeClassParameterized(), "old").build();
        addSupertypesMembers(typeBuilder, nodeData, paramOld, node.getAllSupertypes(), false);
        addAsExtraMethod(typeBuilder, nodeData);

        return new QTypedNodeSubclassData(
            nodeData.queryNodeClass(),
            codeGenHelper.createOwnJavaFile(typeBuilder)
        );
    }

    private MethodSpec createTokenConversionMethod(String methodName, GenChildType.TokenEnumInfo tokenEnumInfo, ClassName childClassName, String javadocSeeMethod, @Nullable String fieldName) {
        String paramTokenEnum = "tokenEnum";
        var methodBuilder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            // Only use `QQuantifiable` as return type instead of `QTypedNode` because token type (= unnamed node) might
            // not be unique and be used in other parts of the grammar which could lead to type-safety issues
            // Also due to that the `TypedNode#fromNode` API does not support retrieving a `TypedNode` for these token
            // types (even though they do implement the `TypedNode` interface)
            .returns(ParameterizedTypeName.get(typedQueryConfig.qQuantifiableConfig().name(), typeVarCollector, childClassName))
            .addParameter(tokenEnumInfo.tokenEnumClass(), paramTokenEnum);

        if (fieldName == null) {
            methodBuilder.addJavadoc("Converts the given token enum constant to the corresponding query node.");
        } else {
            methodBuilder.addJavadoc("Converts the given token enum constant to the corresponding query node for field $L.", CodeGenHelper.createJavadocCodeTag(fieldName));
        }
        methodBuilder.addJavadoc("\n@see #$N", javadocSeeMethod);

        return methodBuilder
            // Token types are for unnamed nodes, so just create an unnamed node
            .addStatement("return new $T<>(null, $N.$N())", typedQueryConfig.qUnnamedNodeConfig().name(), paramTokenEnum, codeGenHelper.tokenEnumConfig().methodGetTypeName())
            .build();
    }

    private void addChildrenMethods(TypeSpec.Builder typeBuilder, @Nullable GenChildren genChildren, NodeData nodeData) {
        var dataConfig = qTypedNodeConfig.dataConfig();

        ClassName childClassName = null;
        GenChildType.TokenEnumInfo tokenEnumInfo = null;
        String methodNameChildToken = null;
        if (genChildren != null) {
            var genChildType = genChildren.getGenChildType();
            childClassName = genChildType.createJavaTypeName(codeGenHelper);
            tokenEnumInfo = genChildType.getTokenEnumInfo(codeGenHelper);
            if (tokenEnumInfo != null) {
                methodNameChildToken = nameGenerator.generateChildTokenMethodName(nodeData.nodeType(), tokenEnumInfo.tokenTypes());
            }
        }

        String paramChildren = "children";
        var methodWithChildrenBuilder = MethodSpec.methodBuilder(qTypedNodeConfig.methodWithChildren())
            .addAnnotation(SafeVarargs.class)
            .addAnnotation(SUPPRESS_WARNINGS_VARARGS)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .returns(nodeData.queryNodeClassParameterized())
            .addParameter(
                ArrayTypeName.of(ParameterizedTypeName.get(
                    typedQueryConfig.qNodeConfig().name(),
                    typeVarCollector,
                    childClassName != null ? WildcardTypeName.subtypeOf(childClassName)
                        // Use `Void` as child node type to still allow all wildcard-like types, e.g. wildcard, missing, unnamed, ...
                        : ClassName.get(Void.class)
                )),
                paramChildren
            )
            .varargs();

        methodWithChildrenBuilder.addJavadoc("Creates a copy of this query node with the given additional children requirements.");
        if (childClassName == null) {
            methodWithChildrenBuilder
                .addJavadoc("\n\n<p>The children type has {@code Void} as node type argument because the Tree-sitter grammar has no")
                .addJavadoc("\nexplicit children types defined for this node type. However, by using {@code Void} this method still")
                .addJavadoc("\npermits all wildcard-like query node types which have an unbound node type, such as")
                .addJavadoc("\n{@link $T#$N()}.", typedQueryConfig.builderConfig().name(), typedQueryConfig.builderConfig().methodErrorNode());
        }

        if (methodNameChildToken != null) {
            methodWithChildrenBuilder.addJavadoc("\n@see #$N", methodNameChildToken);
        }

        var methodWithChildren =  methodWithChildrenBuilder
            .addStatement("return new $T<>(this, $N.$N($N))", nodeData.queryNodeClass(), qTypedNodeConfig.fieldData(), dataConfig.methodWithChildren(), paramChildren)
            .build();
        typeBuilder.addMethod(methodWithChildren);

        // Note: Currently effectively unused because tree-sitter does not include non-named node types in list of children
        // and therefore no token enum will be generated for non-field children
        if (tokenEnumInfo != null) {
            assert childClassName != null;
            typeBuilder.addMethod(createTokenConversionMethod(methodNameChildToken, tokenEnumInfo, childClassName, methodWithChildren.name(), null));
        }
    }

    private void addFieldMethods(TypeSpec.Builder typeBuilder, GenField genField, NodeData nodeData) {
        var dataConfig = qTypedNodeConfig.dataConfig();

        String fieldName = genField.getFieldName();

        var fieldType = genField.getGenChildType().createJavaTypeName(codeGenHelper);
        var tokenEnumInfo = genField.getGenChildType().getTokenEnumInfo(codeGenHelper);
        String methodNameFieldToken = tokenEnumInfo == null ? null : nameGenerator.generateFieldTokenMethodName(nodeData.nodeType(), fieldName, tokenEnumInfo.tokenTypes());

        // Only generate 'without field' method if field is optional
        String methodNameWithoutField = genField.isRequired() ? null : nameGenerator.generateWithoutFieldMethodName(nodeData.nodeType(), fieldName);

        MethodSpec methodWithField;
        {
            String paramField = "field";
            var builder = MethodSpec.methodBuilder(nameGenerator.generateWithFieldMethodName(nodeData.nodeType(), fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(nodeData.queryNodeClassParameterized())
                .addParameter(
                    ParameterizedTypeName.get(
                        typedQueryConfig.qNodeConfig().name(),
                        typeVarCollector,
                        WildcardTypeName.subtypeOf(fieldType)
                    ),
                    paramField
                )
                .addJavadoc("Creates a copy of this query node with the given additional requirements for the field $L.", CodeGenHelper.createJavadocCodeTag(fieldName));

            if (methodNameFieldToken != null) {
                builder.addJavadoc("\n@see #$N", methodNameFieldToken);
            }
            if (methodNameWithoutField != null) {
                builder.addJavadoc("\n@see #$N", methodNameWithoutField);
            }
            builder.addJavadoc(TreeSitterDoc.FIELD.createJavadocSee());

            methodWithField = builder
                .addStatement("return new $T<>(this, $N.$N($T.$N, $N, $L))", nodeData.queryNodeClass(), qTypedNodeConfig.fieldData(), dataConfig.methodWithField(), nodeData.typedNodeClass(), genField.getFieldNameConstant(), paramField, genField.isMultiple())
                .build();
        }
        typeBuilder.addMethod(methodWithField);

        if (methodNameFieldToken != null) {
            typeBuilder.addMethod(createTokenConversionMethod(methodNameFieldToken, tokenEnumInfo, fieldType, methodWithField.name(), fieldName));
        }

        if (methodNameWithoutField != null) {
            var methodWithoutField = MethodSpec.methodBuilder(methodNameWithoutField)
                .addModifiers(Modifier.PUBLIC)
                .returns(nodeData.queryNodeClassParameterized())
                .addJavadoc("Creates a copy of this query node which requires that field $L is not present.", CodeGenHelper.createJavadocCodeTag(fieldName))
                .addJavadoc("\n@see #$N", methodWithField)
                .addJavadoc(TreeSitterDoc.NEGATED_FIELD.createJavadocSee())
                .addStatement("return new $T<>(this, $N.$N($T.$N))", nodeData.queryNodeClass(), qTypedNodeConfig.fieldData(), dataConfig.methodWithoutField(), nodeData.typedNodeClass(), genField.getFieldNameConstant())
                .build();
            typeBuilder.addMethod(methodWithoutField);
        }
    }

    private QTypedNodeSubclassData generateRegularNode(GenRegularNodeType node, NodeData nodeData) {
        var dataConfig = qTypedNodeConfig.dataConfig();

        var constructor = MethodSpec.constructorBuilder()
            .addStatement("super($T.$N)", nodeData.typedNodeClass(), node.getTypeNameConstant())
            .build();

        var paramOld = ParameterSpec.builder(nodeData.queryNodeClassParameterized(), "old").build();
        String paramData = "data";
        var constructorNewData = MethodSpec.constructorBuilder()
            .addParameter(paramOld)
            .addParameter(typeDataParameterized, paramData)
            .addStatement("super($N, $N)", paramOld, paramData)
            .build();

        var typeBuilder = TypeSpec.classBuilder(nodeData.queryNodeClass)
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(typeVarCollector)
            .superclass(ParameterizedTypeName.get(qTypedNodeConfig.name(), typeVarCollector, nodeData.typedNodeClass()))
            .addJavadoc("Query builder for node type $L.", CodeGenHelper.createJavadocCodeTag(nodeData.nodeType()))
            .addJavadoc("\nInstances can be obtained using {@link $T#$N()}.", typedQueryConfig.builderConfig().name(), nodeData.builderMethodName())
            .addMethod(constructor)
            .addMethod(constructorNewData);


        addChildrenMethods(typeBuilder, node.getGenChildren(), nodeData);

        for (var genField : node.getGenFields()) {
            addFieldMethods(typeBuilder, genField, nodeData);
        }

        var methodWithChildAnchor = MethodSpec.methodBuilder(qTypedNodeConfig.methodWithChildAnchor())
            .addModifiers(Modifier.PUBLIC)
            .returns(nodeData.queryNodeClassParameterized())
            .addJavadoc("Creates a copy of this query node with an additional child anchor.")
            .addJavadoc(TreeSitterDoc.ANCHOR.createJavadocSee())
            .addStatement("return new $T<>(this, $N.$N())", nodeData.queryNodeClass(), qTypedNodeConfig.fieldData(), dataConfig.methodWithAnchor())
            .build();
        typeBuilder.addMethod(methodWithChildAnchor);

        addSupertypesMembers(typeBuilder, nodeData, paramOld, node.getAllSupertypes(), true);
        addAsExtraMethod(typeBuilder, nodeData);

        return new QTypedNodeSubclassData(
            nodeData.queryNodeClass(),
            codeGenHelper.createOwnJavaFile(typeBuilder)
        );
    }
}
