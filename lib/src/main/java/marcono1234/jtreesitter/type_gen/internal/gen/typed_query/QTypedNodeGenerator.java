package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.TypedQueryNameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenRegularNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenSupertypeNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.typed_query.TypedQueryConfig.QTypedNodeConfig;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;

import java.util.*;

import static marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.*;

public class QTypedNodeGenerator {
    private final TypedQueryNameGenerator nameGenerator;
    private final CodeGenHelper codeGenHelper;
    private final QNodeCommonGenerator qNodeCommonGenerator;
    private final TypedQueryConfig typedQueryConfig;
    private final TypeVariableName typeVarCollector;
    private final TypeVariableName typeVarNodeBound;

    private final QTypedNodeConfig qTypedNodeConfig;
    private final ParameterizedTypeName typeDataParameterized;
    private final ParameterizedTypeName typeChildEntryParameterized;

    private final ParameterizedTypeName typeQNodeWildcard;
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
            .addStatement(createNonNullCheck(fieldNode.name()))
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
            .addStatement(createNonNullCheck(fieldName.name()))
            .addStatement(createNonNullCheck(fieldNode.name()))
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
                .addStatement("if ($N.contains($N)) throw new $T(\"Field '\" + $N + \"' has already been added to \\\"without fields\\\"\")", fieldWithoutFields, paramFieldName, IllegalArgumentException.class, paramFieldName)
                // Use field name also as name for local variable
                .addStatement("var $N = new $T<>(this.$N)", fieldWithFieldNames, HashSet.class, fieldWithFieldNames)
                .addStatement("if (!$N.add($N) && !$N) throw new $T(\"Field '\" + $N + \"' has already been added\")", fieldWithFieldNames, paramFieldName, paramAllowMultiple, IllegalArgumentException.class, paramFieldName)
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
                .addStatement("if ($N.contains($N)) throw new $T(\"Field '\" + $N + \"' has already been added to \\\"with fields\\\"\")", fieldWithFieldNames, paramFieldName, IllegalArgumentException.class, paramFieldName)
                // Use field name also as name for local variable
                .addStatement("var $N = new $T<>(this.$N)", fieldWithoutFields, LinkedHashSet.class, fieldWithoutFields)
                .addStatement("$N.add($N)", fieldWithoutFields, paramFieldName)
                .addStatement("return new $T<>($N, $N, $N)", dataConfig.name(), fieldChildren, fieldWithFieldNames, fieldWithoutFields)
                .build();
        }

        MethodSpec methodWithAnchor = MethodSpec.methodBuilder(dataConfig.methodWithAnchor())
            .addModifiers(Modifier.PUBLIC)
            .returns(typeDataParameterized)
            .addStatement("if (!$N.isEmpty() && $N.getLast() instanceof $T) throw new $T(\"Duplicate anchor is not valid\")", fieldChildren, fieldChildren, qTypedNodeConfig.classAnchor(), IllegalStateException.class)
            // Use field name also as name for local variable
            .addStatement("var $N = new $T<>(this.$N)", fieldChildren, ArrayList.class, fieldChildren)
            .addStatement("$N.add($T.$N())", fieldChildren, qTypedNodeConfig.classAnchor(), methodNameAnchorInstance)
            .addStatement("return new $T<>($N, $N, $N)", dataConfig.name(), fieldChildren, fieldWithFieldNames, fieldWithoutFields)
            .build();

        MethodSpec methodBuildQuery = MethodSpec.methodBuilder(dataConfig.methodBuildQuery())
            .addParameter(paramQueryStringBuilder)
            .addParameter(paramCaptureRegistry)
            .addParameter(paramPredicateRegistry)
            // Apparently have to write 'without fields' first, because tree-sitter query parser permits them
            // in front of "first child" anchor, but disallows them after "last child" anchor
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
            .addMethod(methodBuildQuery)
            .build();
    }

    public TypeSpec generateClass() {
        var fieldSupertype = FieldSpec.builder(String.class, "supertype", Modifier.PRIVATE, Modifier.FINAL)
            .addJavadoc("nullable")
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

        var methodBuildQuery = qNodeCommonGenerator.createBuildQueryImplOverride()
            .addStatement("$N.append('(')", paramQueryStringBuilder)
            .beginControlFlow("if ($N != null)", fieldSupertype)
            .addStatement("$N.append($N).append('/')", paramQueryStringBuilder, fieldSupertype)
            .endControlFlow()
            .addStatement("$N.append($N)", paramQueryStringBuilder, fieldNodeType)
            .addStatement("$N.$N($N, $N, $N)", fieldData, qTypedNodeConfig.dataConfig().methodBuildQuery(), paramQueryStringBuilder, paramCaptureRegistry, paramPredicateRegistry)
            .addStatement("$N.append(')')", paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(qTypedNodeConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            .superclass(ParameterizedTypeName.get(typedQueryConfig.classQCapturableQuantifiable(), typeVarCollector, typeVarNodeBound))
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
            .addMethod(methodBuildQuery)
            .build();
    }

    private void addSupertypesMembers(ClassName ownQueryNodeClass, ClassName ownTypedNodeClass, String ownNodeTypeName, ParameterSpec paramOld, TypeSpec.Builder typeBuilder, List<GenSupertypeNodeType> supertypes) {
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
            String methodName = nameGenerator.generateAsSubtypeMethod(ownNodeTypeName, supertype.getTypeName());
            var methodAsSubtype = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                // Return `QTypedNode` instead of current subclass to only allow setting supertype once
                .returns(ParameterizedTypeName.get(qTypedNodeConfig.name(), typeVarCollector, ownTypedNodeClass))
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.SUPERTYPE_NODE.createHtmlLink())
                .addStatement("return new $T<>(this, $T.$N)", ownQueryNodeClass, supertype.createJavaTypeName(codeGenHelper), supertype.getTypeNameConstant())
                .build();

            typeBuilder.addMethod(methodAsSubtype);
        }
    }

    /**
     * If the node type is an 'extra' node, adds an `asExtra` method which uses an unbounded `N` to
     */
    private void addAsExtraMethod(GenNodeType node, TypeSpec.Builder typeBuilder) {
        if (!node.isExtra()) {
            return;
        }

        // Note: For convenience would ideally generate this as instance method instead of as `static` method; however
        // to provide all query builder functionality, would need to add `...Extra` variants of `QCapturableQuantified` ...
        // which would be quite cumbersome to generate

        String paramNode = "node";
        // Create type var without bounds
        var typeVarNode = TypeVariableName.get(typeVarNodeBound.name());
        // Return as `QNode` instead of `QTypedNode` because `N` is unbounded so capturing or filtering would not be type-safe anymore
        var typeQNodeUnbound = ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, typeVarNode);
        var methodAsExtra = MethodSpec.methodBuilder(qTypedNodeConfig.methodAsExtra())
            .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .returns(typeQNodeUnbound)
            .addParameter(
                ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, WildcardTypeName.subtypeOf(node.createJavaTypeName(codeGenHelper))),
                paramNode
            )
            // TODO: Javadoc should also show example snippet
            .addJavadoc("TODO")
            .addStatement(createNonNullCheck(paramNode))
            .addStatement("return ($T) $N", typeQNodeUnbound, paramNode)
            .build();
        typeBuilder.addMethod(methodAsExtra);
    }

    public record QTypedNodeImplData(ClassName className, JavaFile javaFile) {
    }

    public QTypedNodeImplData generateQTypedNode(GenNodeType node) {
        return switch (node) {
            case GenRegularNodeType regularNode -> generateRegularNode(regularNode);
            case GenSupertypeNodeType supertypeNode -> generateSupertypeNode(supertypeNode);
        };
    }

    private QTypedNodeImplData generateSupertypeNode(GenSupertypeNodeType node) {
        String ownNodeTypeName = node.getTypeName();
        var ownClassName = codeGenHelper.createOwnClassName(nameGenerator.generateJavaTypeName(ownNodeTypeName));

        var nodeClass = node.createJavaTypeName(codeGenHelper);

        var constructor = MethodSpec.constructorBuilder()
            .addStatement("super($T.$N)", nodeClass, node.getTypeNameConstant())
            .build();

        var typeBuilder = TypeSpec.classBuilder(ownClassName)
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(typeVarCollector)
            .superclass(ParameterizedTypeName.get(qTypedNodeConfig.name(), typeVarCollector, nodeClass))
            .addJavadoc("TODO")
            .addMethod(constructor);

        var paramOld = ParameterSpec.builder(ParameterizedTypeName.get(ownClassName, typeVarCollector), "old").build();
        addSupertypesMembers(ownClassName, nodeClass, ownNodeTypeName, paramOld, typeBuilder, node.getSupertypes());
        addAsExtraMethod(node, typeBuilder);

        return new QTypedNodeImplData(
            ownClassName,
            codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build()
        );
    }

    private QTypedNodeImplData generateRegularNode(GenRegularNodeType node) {
        var dataConfig = qTypedNodeConfig.dataConfig();

        String ownNodeTypeName = node.getTypeName();
        var ownClassName = codeGenHelper.createOwnClassName(nameGenerator.generateJavaTypeName(ownNodeTypeName));
        var ownClassParameterized = ParameterizedTypeName.get(ownClassName, typeVarCollector);

        var nodeClass = node.createJavaTypeName(codeGenHelper);

        var constructor = MethodSpec.constructorBuilder()
            .addStatement("super($T.$N)", nodeClass, node.getTypeNameConstant())
            .build();

        var paramOld = ParameterSpec.builder(ParameterizedTypeName.get(ownClassName, typeVarCollector), "old").build();
        String paramData = "data";
        var constructorNewData = MethodSpec.constructorBuilder()
            .addParameter(paramOld)
            .addParameter(typeDataParameterized, paramData)
            .addStatement("super($N, $N)", paramOld, paramData)
            .build();

        var typeBuilder = TypeSpec.classBuilder(ownClassName)
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(typeVarCollector)
            .superclass(ParameterizedTypeName.get(qTypedNodeConfig.name(), typeVarCollector, nodeClass))
            .addJavadoc("TODO")
            .addMethod(constructor)
            .addMethod(constructorNewData);


        ClassName childClassName = null;
        {
            var genChildren = node.getGenChildren();
            if (genChildren != null) {
                childClassName = genChildren.getGenChildType().createJavaTypeName(codeGenHelper);
            }
        }

        String paramChildren = "children";
        var methodWithChildren = MethodSpec.methodBuilder(qTypedNodeConfig.methodWithChildren())
            .addAnnotation(SafeVarargs.class)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .returns(ownClassParameterized)
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
            .varargs()
            // TODO if `childClassName == null` should explain `Void` node type in Javadoc
            .addJavadoc("TODO")
            .addStatement("return new $T<>(this, $N.$N($N))", ownClassName, qTypedNodeConfig.fieldData(), dataConfig.methodWithChildren(), paramChildren)
            .build();
        typeBuilder.addMethod(methodWithChildren);

        for (var genField : node.getGenFields()) {
            String fieldName = genField.getFieldName();

            // Only generate 'without field' method if field is optional
            if (!genField.isRequired()) {
                var methodWithoutField = MethodSpec.methodBuilder(nameGenerator.generateWithoutFieldMethodName(ownNodeTypeName, fieldName))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ownClassParameterized)
                    .addJavadoc("TODO")
                    .addJavadoc("\n@see " + TreeSitterDoc.NEGATED_FIELD.createHtmlLink())
                    .addStatement("return new $T<>(this, $N.$N($T.$N))", ownClassName, qTypedNodeConfig.fieldData(), dataConfig.methodWithoutField(), nodeClass, genField.getFieldNameConstant())
                    .build();
                typeBuilder.addMethod(methodWithoutField);
            }

            var fieldType = genField.getGenChildType().createJavaTypeName(codeGenHelper);
            var tokenEnumInfo = genField.getGenChildType().getTokenEnumInfo(codeGenHelper);
            String methodNameFieldToken = tokenEnumInfo == null ? null : nameGenerator.generateFieldTokenMethodName(ownNodeTypeName, fieldName, tokenEnumInfo.tokenTypes());

            MethodSpec methodWithField;
            {
                String paramField = "field";
                var builder = MethodSpec.methodBuilder(nameGenerator.generateWithFieldMethodName(ownNodeTypeName, fieldName))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ownClassParameterized)
                    .addParameter(
                        ParameterizedTypeName.get(
                            typedQueryConfig.qNodeConfig().name(),
                            typeVarCollector,
                            WildcardTypeName.subtypeOf(fieldType)
                        ),
                        paramField
                    )
                    .addJavadoc("TODO");

                if (methodNameFieldToken != null) {
                    builder.addJavadoc("\n@see #$N", methodNameFieldToken);
                }
                builder.addJavadoc("\n@see " + TreeSitterDoc.FIELD.createHtmlLink());

                methodWithField = builder
                    .addStatement("return new $T<>(this, $N.$N($T.$N, $N, $L))", ownClassName, qTypedNodeConfig.fieldData(), dataConfig.methodWithField(), nodeClass, genField.getFieldNameConstant(), paramField, genField.isMultiple())
                    .build();
            }
            typeBuilder.addMethod(methodWithField);

            if (methodNameFieldToken != null) {
                String paramTokenEnum = "tokenEnum";
                var methodFieldToken = MethodSpec.methodBuilder(methodNameFieldToken)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(typeVarCollector)
                    // Only use `QQuantifiable` as return type instead of `QTypedNode` because token type (= unnamed node) might
                    // not be unique and be used in other parts of the grammar which could lead to type-safety issues
                    // Also due to that the `TypedNode#fromNode` API does not support retrieving a `TypedNode` for these token
                    // types (even though they do implement the `TypedNode` interface)
                    .returns(ParameterizedTypeName.get(typedQueryConfig.qQuantifiableConfig().name(), typeVarCollector, fieldType))
                    .addParameter(tokenEnumInfo.tokenEnumClass(), paramTokenEnum)
                    .addJavadoc("TODO")
                    // Token types are for unnamed nodes, so just create an unnamed node
                    .addStatement("return new $T<>(null, $N.$N())", typedQueryConfig.classQUnnamedNode(), paramTokenEnum, codeGenHelper.tokenEnumConfig().methodGetTypeName())
                    .build();
                typeBuilder.addMethod(methodFieldToken);
            }
        }

        var methodWithChildAnchor = MethodSpec.methodBuilder(qTypedNodeConfig.methodWithChildAnchor())
            .addModifiers(Modifier.PUBLIC)
            .returns(ownClassParameterized)
            .addJavadoc("TODO")
            .addJavadoc("\n@see " + TreeSitterDoc.ANCHOR.createHtmlLink())
            .addStatement("return new $T<>(this, $N.$N())", ownClassName, qTypedNodeConfig.fieldData(), dataConfig.methodWithAnchor())
            .build();
        typeBuilder.addMethod(methodWithChildAnchor);

        addSupertypesMembers(ownClassName, nodeClass, ownNodeTypeName, paramOld, typeBuilder, node.getSupertypes());
        addAsExtraMethod(node, typeBuilder);

        return new QTypedNodeImplData(
            ownClassName,
            codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build()
        );
    }
}
