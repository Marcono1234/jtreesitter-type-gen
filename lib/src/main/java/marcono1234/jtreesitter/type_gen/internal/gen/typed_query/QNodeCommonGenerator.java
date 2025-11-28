package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.*;

class QNodeCommonGenerator {
    final CodeGenHelper codeGenHelper;
    final TypedQueryConfig typedQueryConfig;
    final TypeVariableName typeVarCollector;
    private final TypeVariableName typeVarNode;
    final TypeVariableName typeVarNodeBound;

    private final ParameterizedTypeName typeQNode;
    private final ParameterizedTypeName typeQNodeImpl;
    private final ParameterizedTypeName typeQQuantifiable;
    private final ParameterizedTypeName typeQCapturable;

    private final ParameterSpec paramLanguage;
    final ParameterSpec paramQueryStringBuilder;
    final ParameterSpec paramCaptureRegistry;
    final ParameterSpec paramPredicateRegistry;

    QNodeCommonGenerator(TypedQueryGenerator typedQueryGenerator) {
        this.codeGenHelper = typedQueryGenerator.codeGenHelper;
        this.typedQueryConfig = typedQueryGenerator.typedQueryConfig;
        this.typeVarCollector = typedQueryGenerator.typeVarCollector;
        this.typeVarNode = typedQueryGenerator.typeVarNode;
        this.typeVarNodeBound = typedQueryGenerator.typeVarNodeBound;

        this.typeQNode = ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, typeVarNode);
        this.typeQNodeImpl = ParameterizedTypeName.get(typedQueryConfig.qNodeImplConfig().name(), typeVarCollector, typeVarNode);
        this.typeQQuantifiable = ParameterizedTypeName.get(typedQueryConfig.qQuantifiableConfig().name(), typeVarCollector, typeVarNode);
        this.typeQCapturable = ParameterizedTypeName.get(typedQueryConfig.qCapturableConfig().name(), typeVarCollector, typeVarNode);

        this.paramLanguage = ParameterSpec.builder(codeGenHelper.jtreesitterConfig().language().className(), "language").build();
        this.paramQueryStringBuilder = ParameterSpec.builder(StringBuilder.class, "queryStringBuilder").build();
        this.paramCaptureRegistry = ParameterSpec.builder(
            ParameterizedTypeName.get(typedQueryConfig.captureRegistryConfig().name(), typeVarCollector),
            "captureRegistry"
        ).build();
        this.paramPredicateRegistry = ParameterSpec.builder(typedQueryConfig.predicateRegistryConfig().name(), "predicateRegistry").build();
    }

    private MethodSpec.Builder createBuildQuerySignature() {
        var builder = MethodSpec.methodBuilder(typedQueryConfig.qNodeConfig().methodBuildQuery())
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(typedQueryConfig.name(), typeVarCollector));

        // If Language provider is not specified, user must provide Language themselves
        if (codeGenHelper.languageUtilsConfig() == null) {
            builder.addParameter(paramLanguage);
        }
        return builder;
    }

    private MethodSpec.Builder createBuildQueryImplSignature() {
        return MethodSpec.methodBuilder(typedQueryConfig.qNodeImplConfig().methodBuildQueryImpl())
            .addParameter(paramQueryStringBuilder)
            .addParameter(paramCaptureRegistry)
            .addParameter(paramPredicateRegistry);
    }

    MethodSpec.Builder createBuildQueryImplOverride() {
        return createBuildQueryImplSignature().addAnnotation(Override.class);
    }

    private TypeSpec generateInterfaceQNode() {
        var qNodeConfig = typedQueryConfig.qNodeConfig();

        var methodBuildQuery = createBuildQuerySignature()
            .addModifiers(Modifier.ABSTRACT)
            .addJavadoc("TODO")
            .build();

        return TypeSpec.interfaceBuilder(qNodeConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .addJavadoc("TODO")
            .addJavadoc("\n@param <$T> TODO", typeVarNode)
            .addJavadoc("\n@param <$T> TODO", typeVarCollector)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateClassQNodeImpl() {
        var qNodeImplConfig = typedQueryConfig.qNodeImplConfig();

        var methodBuildQueryImpl = createBuildQueryImplSignature().addModifiers(Modifier.ABSTRACT).build();

        MethodSpec methodBuildQuery;
        {
            var builder = createBuildQuerySignature()
                .addAnnotation(Override.class);

            var languageUtilsConfig = codeGenHelper.languageUtilsConfig();
            if (languageUtilsConfig == null) {
                // Language is provided as parameter by user; validate it
                builder.addStatement(createNonNullCheck(paramLanguage.name()));
            } else {
                builder.addStatement("var $N = $T.$N", paramLanguage, languageUtilsConfig.className(), languageUtilsConfig.fieldLanguage());
            }

            methodBuildQuery = builder
                // Use same variable names as for the parameters
                .addStatement("var $N = new $T()", paramQueryStringBuilder, StringBuilder.class)
                .addStatement("var $N = new $T()", paramCaptureRegistry, paramCaptureRegistry.type())
                .addStatement("var $N = new $T()", paramPredicateRegistry, paramPredicateRegistry.type())
                .addStatement("$N($N, $N, $N)", methodBuildQueryImpl.name(), paramQueryStringBuilder, paramCaptureRegistry, paramPredicateRegistry)
                .addStatement("return new $T<>($N, $N.toString(), $N, $N)", typedQueryConfig.name(), paramLanguage, paramQueryStringBuilder, paramCaptureRegistry, paramPredicateRegistry)
                .build();
        }

        String paramNode = "node";
        var methodFromNode = MethodSpec.methodBuilder(qNodeImplConfig.methodFromNode())
            .addModifiers(Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .returns(typeQNodeImpl)
            .addParameter(typeQNode, paramNode)
            .addStatement(createNonNullCheck(paramNode))
            .addComment("Expect that every " + typeQNode.rawType().simpleName() + " is actually an instance of " + typeQNodeImpl.rawType().simpleName())
            .addStatement("return ($T) $N", typeQNodeImpl, paramNode)
            .build();

        String paramNodes = "nodes";
        // Type `List<QNodeImpl<C, ? extends N>>`
        var typeListQNodeImpl = ParameterizedTypeName.get(ClassName.get(List.class), ParameterizedTypeName.get(typedQueryConfig.qNodeImplConfig().name(), typeVarCollector, WildcardTypeName.subtypeOf(typeVarNode)));
        var typeListWildcard = ParameterizedTypeName.get(ClassName.get(List.class), unboundedWildcard());
        var methodListOf = MethodSpec.methodBuilder(qNodeImplConfig.methodListOf())
            .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
            .addModifiers(Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .addParameter(
                ArrayTypeName.of(ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, WildcardTypeName.subtypeOf(typeVarNode))),
                paramNodes
            )
            .varargs()
            .returns(typeListQNodeImpl)
            // Uses `List#of` to disallow null elements
            .addStatement("return ($T) ($T) $T.of($N)", typeListQNodeImpl, typeListWildcard, List.class, paramNodes)
            .build();

        String paramStr = "s";
        var methodCreateStringLiteral = MethodSpec.methodBuilder(qNodeImplConfig.methodCreateStringLiteral())
            .addModifiers(Modifier.STATIC)
            .addParameter(String.class, paramStr)
            .returns(String.class)
            // Wrap in quotes, and escape '\' and '"'
            .addStatement("return \"\\\"\" + $N.replace(\"\\\\\", \"\\\\\\\\\").replace(\"\\\"\", \"\\\\\\\"\") + \"\\\"\"", paramStr)
            .build();

        return TypeSpec.classBuilder(qNodeImplConfig.name())
            // non-sealed but class is not publicly accessible
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.ABSTRACT, Modifier.NON_SEALED)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .addSuperinterface(ParameterizedTypeName.get(
                typedQueryConfig.qNodeConfig().name(),
                typeVarCollector,
                typeVarNode
            ))
            .addMethod(methodBuildQueryImpl)
            .addMethod(methodBuildQuery)
            .addMethod(methodFromNode)
            .addMethod(methodListOf)
            .addMethod(methodCreateStringLiteral)
            .build();
    }

    private TypeSpec generateClassQQuantifiable() {
        var qQuantifiableConfig = typedQueryConfig.qQuantifiableConfig();
        var typeQQuantified = typedQueryConfig.qQuantifiedConfig().name();

        // Package-private constructor, to prevent user code from accessing it
        var constructor = MethodSpec.constructorBuilder().build();

        var methodZeroOrMore = MethodSpec.methodBuilder(qQuantifiableConfig.methodZeroOrMore())
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQNode)
            .addJavadoc("TODO")
            .addJavadoc("\n@see " + TreeSitterDoc.QUANTIFICATION_OPERATOR.createHtmlLink())
            .addStatement("return new $T<>(this, '*')", typeQQuantified)
            .build();

        var methodOneOrMore = MethodSpec.methodBuilder(qQuantifiableConfig.methodOneOrMore())
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQNode)
            .addJavadoc("TODO")
            .addJavadoc("\n@see " + TreeSitterDoc.QUANTIFICATION_OPERATOR.createHtmlLink())
            .addStatement("return new $T<>(this, '+')", typeQQuantified)
            .build();

        var methodOptional = MethodSpec.methodBuilder(qQuantifiableConfig.methodOptional())
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQNode)
            .addJavadoc("TODO")
            .addJavadoc("\n@see " + TreeSitterDoc.QUANTIFICATION_OPERATOR.createHtmlLink())
            .addStatement("return new $T<>(this, '?')", typeQQuantified)
            .build();

        // Important: When adding more methods in the future, generate overrides in `generateQCapturableQuantifiable`

        return TypeSpec.classBuilder(qQuantifiableConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .superclass(typeQNodeImpl)
            .addJavadoc("TODO")
            .addJavadoc("\n@see " + TreeSitterDoc.QUANTIFICATION_OPERATOR.createHtmlLink())
            .addMethod(constructor)
            .addMethod(methodOneOrMore)
            .addMethod(methodZeroOrMore)
            .addMethod(methodOptional)
            .build();
    }

    /**
     * Creates code which performs a delegating call to the {@code QNodeImpl#buildQuery} method of
     * {@code varDelegateNode}.
     */
    CodeBlock createDelegatingBuildQueryCall(String varDelegateNode) {
        return CodeBlock.of("$N.$N($N, $N, $N)",
            varDelegateNode,
            typedQueryConfig.qNodeImplConfig().methodBuildQueryImpl(),
            paramQueryStringBuilder, paramCaptureRegistry, paramPredicateRegistry
        );
    }

    private TypeSpec generateClassQQuantified() {
        var qQuantifiedConfig = typedQueryConfig.qQuantifiedConfig();

        var fieldNode = FieldSpec.builder(typeQNodeImpl, qQuantifiedConfig.fieldNode(), Modifier.PRIVATE, Modifier.FINAL).build();
        var fieldQuantifier = FieldSpec.builder(char.class, qQuantifiedConfig.fieldQuantifier(), Modifier.PRIVATE, Modifier.FINAL).build();

        var constructor = createInitializingConstructor(fieldNode, fieldQuantifier);

        var methodBuildQuery = createBuildQueryImplOverride()
            .addStatement(createDelegatingBuildQueryCall(fieldNode.name()))
            .addStatement("$N.append($N)", paramQueryStringBuilder, fieldQuantifier)
            .build();

        return TypeSpec.classBuilder(qQuantifiedConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            // Do not use 'quantifiable' as superclass, to prevent applying multiple quantifiers to same node
            .superclass(typeQNodeImpl)
            .addField(fieldNode)
            .addField(fieldQuantifier)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateClassQGroup() {
        var typeListQNodeImplWildcard = ParameterizedTypeName.get(
            ClassName.get(List.class),
            ParameterizedTypeName.get(
                typedQueryConfig.qNodeImplConfig().name(),
                typeVarCollector,
                WildcardTypeName.subtypeOf(typeVarNode)
            )
        );
        var fieldNodes = FieldSpec.builder(typeListQNodeImplWildcard, "nodes", Modifier.PRIVATE, Modifier.FINAL).build();

        var constructor = createInitializingConstructor(fieldNodes);

        var methodBuildQuery = createBuildQueryImplOverride()
            .addStatement("$N.append('(')", paramQueryStringBuilder)
            .addStatement(CodeBlock.builder()
                .add("$N.forEach(n -> {\n", fieldNodes).indent()
                .add(createDelegatingBuildQueryCall("n")).add(";\n")
                .add("$N.append(' ');\n", paramQueryStringBuilder)
                .unindent()
                .add("})")
                .build()
            )
            .addStatement("$N.append(')')", paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(typedQueryConfig.classQGroup())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .superclass(typeQQuantifiable)
            .addField(fieldNodes)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateClassQAlternation() {
        var typeListQNodeImplWildcard = ParameterizedTypeName.get(
            ClassName.get(List.class),
            ParameterizedTypeName.get(
                typedQueryConfig.qNodeImplConfig().name(),
                typeVarCollector,
                WildcardTypeName.subtypeOf(typeVarNode)
            )
        );
        var fieldNodes = FieldSpec.builder(typeListQNodeImplWildcard, "nodes", Modifier.PRIVATE, Modifier.FINAL).build();

        var constructor = createInitializingConstructor(fieldNodes);

        var methodBuildQuery = createBuildQueryImplOverride()
            .addStatement("$N.append('[')", paramQueryStringBuilder)
            .addStatement(CodeBlock.builder()
                .add("$N.forEach(n -> {\n", fieldNodes).indent()
                .add(createDelegatingBuildQueryCall("n")).add(";\n")
                .add("$N.append(' ');\n", paramQueryStringBuilder)
                .unindent()
                .add("})")
                .build()
            )
            .addStatement("$N.append(']')", paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(typedQueryConfig.classQAlternation())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .superclass(typeQQuantifiable)
            .addField(fieldNodes)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateClassQUnnamedNode() {
        var fieldSupertype = FieldSpec.builder(String.class, "supertype", Modifier.PRIVATE, Modifier.FINAL).addJavadoc("nullable").build();
        var fieldNodeType = FieldSpec.builder(String.class, "nodeType", Modifier.PRIVATE, Modifier.FINAL).build();

        var constructorBuilder = createInitializingConstructorBuilder(fieldSupertype, fieldNodeType)
            .addStatement(createNonNullCheck(fieldNodeType.name()));

        // TODO: Instead of performing this validation using `Language`, could instead retrieve this information from node-types.json
        //   and then have a `Map<String, Set<String>>`, mapping from unnamed node types to their supertypes (possibly empty)
        var languageUtilsConfig = codeGenHelper.languageUtilsConfig();
        // If Language object is available, perform additional validation to fail fast
        // However, for invalid queries tree-sitter would also fail when building query in the end
        if (languageUtilsConfig != null) {
            var languageConfig = codeGenHelper.jtreesitterConfig().language();

            String varLanguage = "language";
            String varTypeId = "typeId";
            String varTypeIdSupertype = "supertypeId";
            String varFoundSubtype = "foundSubtype";
            String varTypeIdSubtype = "subtypeId";

            constructorBuilder
                .addStatement("var $N = $T.$N", varLanguage, languageUtilsConfig.className(), languageUtilsConfig.fieldLanguage())
                .addStatement("var $N = $N.$N($N, false)", varTypeId, varLanguage, languageConfig.methodGetTypeId(), fieldNodeType)
                .addStatement("if ($N == 0) throw new $T(\"Unknown unnamed node type: \" + $N)", varTypeId, IllegalArgumentException.class, fieldNodeType)
                .beginControlFlow("if ($N != null)", fieldSupertype)
                .addStatement("var $N = $N.$N($N, true)", varTypeIdSupertype, varLanguage, languageConfig.methodGetTypeId(), fieldSupertype)
                .addStatement("if ($N == 0) throw new $T(\"Unknown supertype node type: \" + $N)", varTypeIdSupertype, IllegalArgumentException.class, fieldSupertype)
                .addStatement("var $N = false", varFoundSubtype)
                .beginControlFlow("for (var $N : $N.$N($N))", varTypeIdSubtype, varLanguage, languageConfig.methodGetSubtypes(), varTypeIdSupertype)
                .beginControlFlow("if ($N == $N)", varTypeIdSubtype, varTypeId)
                .addStatement("$N = true", varFoundSubtype)
                .addStatement("break")
                .endControlFlow()
                .endControlFlow()
                .addStatement("if (!$N) throw new $T(\"Node type '\" + $N + \"' is not a supertype of '\" + $N + \"'\")", varFoundSubtype, IllegalArgumentException.class, fieldSupertype, fieldNodeType)
                .endControlFlow();
        }

        var methodBuildQuery = createBuildQueryImplOverride()
            .beginControlFlow("if ($N != null)", fieldSupertype)
            .addStatement("$N.append('(').append($N).append('/')", paramQueryStringBuilder, fieldSupertype)
            .endControlFlow()
            .addStatement("$N.append($N($N))", paramQueryStringBuilder, typedQueryConfig.qNodeImplConfig().methodCreateStringLiteral(), fieldNodeType)
            .beginControlFlow("if ($N != null)", fieldSupertype)
            .addStatement("$N.append(')')", paramQueryStringBuilder)
            .endControlFlow()
            .build();

        return TypeSpec.classBuilder(typedQueryConfig.classQUnnamedNode())
            // Package-private class to allow `fieldToken...` methods to access the constructor
            .addModifiers(Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .superclass(typeQQuantifiable)
            .addField(fieldSupertype)
            .addField(fieldNodeType)
            .addMethod(constructorBuilder.build())
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateClassQWildcardNode() {
        var qWildcardNodeConfig = typedQueryConfig.qWildcardNodeConfig();

        var typeSelfWildcard = ParameterizedTypeName.get(
            qWildcardNodeConfig.name(),
            unboundedWildcard(),
            unboundedWildcard()
        );
        var constantNamed = FieldSpec.builder(typeSelfWildcard, qWildcardNodeConfig.constantNamed(), Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T<>(true)", qWildcardNodeConfig.name())
            .build();
        var constantNamedOrUnnamed = FieldSpec.builder(typeSelfWildcard, qWildcardNodeConfig.constantNamedOrUnnamed(), Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T<>(false)", qWildcardNodeConfig.name())
            .build();
        var fieldIsNamed = FieldSpec.builder(boolean.class, "isNamed", Modifier.PRIVATE, Modifier.FINAL).build();

        var constructor = createInitializingConstructor(fieldIsNamed);

        var methodBuildQuery = createBuildQueryImplOverride()
            .addStatement("if ($N) $N.append('(')", fieldIsNamed, paramQueryStringBuilder)
            .addStatement("$N.append('_')", paramQueryStringBuilder)
            .addStatement("if ($N) $N.append(')')", fieldIsNamed, paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(qWildcardNodeConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .superclass(typeQQuantifiable)
            .addField(constantNamed)
            .addField(constantNamedOrUnnamed)
            .addField(fieldIsNamed)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateClassQErrorNode() {
        var qErrorNodeConfig = typedQueryConfig.qErrorNodeConfig();

        var typeSelfWildcard = ParameterizedTypeName.get(
            qErrorNodeConfig.name(),
            unboundedWildcard(),
            unboundedWildcard()
        );
        var constantInstance = FieldSpec.builder(typeSelfWildcard, qErrorNodeConfig.constantInstance(), Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T<>()", qErrorNodeConfig.name())
            .build();

        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();

        var methodBuildQuery = createBuildQueryImplOverride()
            .addStatement("$N.append(\"(ERROR)\")", paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(qErrorNodeConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .superclass(typeQQuantifiable)
            .addField(constantInstance)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    private TypeSpec generateClassQMissingNode() {
        var qMissingNodeConfig = typedQueryConfig.qMissingNodeConfig();

        var typeSelfWildcard = ParameterizedTypeName.get(
            qMissingNodeConfig.name(),
            unboundedWildcard(),
            unboundedWildcard()
        );
        var constantAny = FieldSpec.builder(typeSelfWildcard, qMissingNodeConfig.constantAny(), Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T<>(null, false)", qMissingNodeConfig.name())
            .build();
        var fieldNodeType = FieldSpec.builder(String.class, "nodeType", Modifier.PRIVATE, Modifier.FINAL).addJavadoc("nullable").build();
        var fieldIsUnnamed = FieldSpec.builder(boolean.class, "isUnnamed", Modifier.PRIVATE, Modifier.FINAL).build();

        // TODO: How to generate code which allows constructing specific missing nodes in a type-safe way?
        //   Currently only the `QMissingNode#ANY` variant is available through the typed query API
        //   Maybe add `asMissing()` to `QUnnamedNode` and `QTypedNode`?
        //   but for typed node have to make sure no children, fields or captures have been specified yet
        var constructor = createInitializingConstructor(fieldNodeType, fieldIsUnnamed);

        var methodBuildQuery = createBuildQueryImplOverride()
            .addStatement("$N.append(\"(MISSING\")", paramQueryStringBuilder)
            .beginControlFlow("if ($N != null)", fieldNodeType)
            .addStatement("$N.append(' ')", paramQueryStringBuilder)
            .beginControlFlow("if ($N)", fieldIsUnnamed)
            .addStatement("$N.append($N($N))", paramQueryStringBuilder, typedQueryConfig.qNodeImplConfig().methodCreateStringLiteral(), fieldNodeType)
            .nextControlFlow("else")
            .addStatement("$N.append($N)", paramQueryStringBuilder, fieldNodeType)
            .endControlFlow()
            .endControlFlow()
            .addStatement("$N.append(')')", paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(qMissingNodeConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .superclass(typeQQuantifiable)
            .addField(constantAny)
            .addField(fieldNodeType)
            .addField(fieldIsUnnamed)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    private CodeBlock createQFilteredReturn(String varPredicate) {
        var qNodeImp = typedQueryConfig.qNodeImplConfig();

        return CodeBlock.builder()
            .add("return new $T<>(", typedQueryConfig.qFilteredConfig().name())
            .add("$T.$N(this), ", qNodeImp.name(), qNodeImp.methodFromNode())
            .add("$N", varPredicate)
            .add(")")
            .build();
    }

    private TypeSpec generateInterfaceQFilterable() {
        var qFilterableConfig = typedQueryConfig.qFilterableConfig();
        var qFilteredConfig = typedQueryConfig.qFilteredConfig();

        String paramStr = "s";
        // Note: Avoid a name conflict with the `paramPredicate` below
        String varPredicate = "p";
        var methodTextEq = MethodSpec.methodBuilder(qFilterableConfig.methodTextEq())
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(typeQCapturable)
            .addParameter(String.class, paramStr)
            .addJavadoc("TODO")
            // Uses `List#of` to disallow null elements
            .addStatement("var $N = new $T(\"eq\", $T.of($N))", varPredicate, qFilteredConfig.classBuiltinPredicate(), List.class, paramStr)
            .addStatement(createQFilteredReturn(varPredicate))
            .build();

        var methodTextNotEq = MethodSpec.methodBuilder(qFilterableConfig.methodTextNotEq())
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(typeQCapturable)
            .addParameter(String.class, paramStr)
            .addJavadoc("TODO")
            // Uses `List#of` to disallow null elements
            .addStatement("var $N = new $T(\"not-eq\", $T.of($N))", varPredicate, qFilteredConfig.classBuiltinPredicate(), List.class, paramStr)
            .addStatement(createQFilteredReturn(varPredicate))
            .build();

        String varStrings = "strings";
        var methodTextAnyOf = MethodSpec.methodBuilder(qFilterableConfig.methodTextAnyOf())
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(typeQCapturable)
            .addParameter(ArrayTypeName.of(String.class), paramStr)
            .varargs()
            .addJavadoc("TODO")
            // Uses `List#of` to disallow null elements
            .addStatement("var $N = $T.of($N)", varStrings, List.class, paramStr)
            .addStatement("if ($N.isEmpty()) throw new $T(\"Must specify at least one string\")", varStrings, IllegalArgumentException.class)
            .addStatement("var $N = new $T(\"any-of\", $N)", varPredicate, qFilteredConfig.classBuiltinPredicate(), varStrings)
            .addStatement(createQFilteredReturn(varPredicate))
            .build();

        // Don't support `#match?` regex predicate; a type-safe implementation should accept a Java `Pattern` but then
        // would have to convert it to string just so that jtreesitter in the end recreates it as `Pattern`, which is
        // inefficient and error-prone; maybe users should rather use a custom predicate for it


        String paramPredicate = "predicate";
        // Type `Predicate<Stream<TypedNode>>`; used by the internal implementation to avoid casting later
        var typePredicateStream = ParameterizedTypeName.get(ClassName.get(Predicate.class), ParameterizedTypeName.get(ClassName.get(Stream.class), codeGenHelper.typedNodeConfig().className()));
        var typePredicateWildcard = ParameterizedTypeName.get(ClassName.get(Predicate.class), unboundedWildcard());
        String varPredicateU = "predicateU";
        var methodMatching = MethodSpec.methodBuilder(qFilterableConfig.methodMatching())
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(typeQCapturable)
            .addParameter(
                // Type `Predicate<? super Stream<N>>`
                // Uses `Stream` instead of `List` since internal mapping of nodes obtains `Stream` anyway, and that also
                // makes it easier for user to directly call `anyOf` or `allOf` to check all nodes
                ParameterizedTypeName.get(
                    ClassName.get(Predicate.class),
                    WildcardTypeName.supertypeOf(ParameterizedTypeName.get(ClassName.get(Stream.class), typeVarNode))),
                paramPredicate
            )
            .addJavadoc("TODO")
            .addStatement(createNonNullCheck(paramPredicate))
            // Note: In the generated code IntelliJ claims the cast to `typePredicateWildcard` (`Predicate<?>`) is redundant, but without it compilation fails
            .addStatement("$L var $N = ($T) ($T) $N", SUPPRESS_WARNINGS_UNCHECKED, varPredicateU, typePredicateStream, typePredicateWildcard, paramPredicate)
            .addStatement("var $N = new $T($N)", varPredicate, qFilteredConfig.classCustomPredicate(), varPredicateU)
            .addStatement(createQFilteredReturn(varPredicate))
            .build();

        return TypeSpec.interfaceBuilder(qFilterableConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
            .addTypeVariable(typeVarCollector)
            // Only support filtering of typed nodes
            .addTypeVariable(typeVarNodeBound)
            .addSuperinterface(typeQNode)
            .addJavadoc("TODO")
            .addMethod(methodTextEq)
            .addMethod(methodTextNotEq)
            .addMethod(methodTextAnyOf)
            .addMethod(methodMatching)
            .build();
    }

    private MethodSpec.Builder createQPredicateBuildMethodSignature() {
        return MethodSpec.methodBuilder("build")
            // Note: This method is not part of the API; it is only a public method of an internal interface
            .addModifiers(Modifier.PUBLIC)
            .addParameter(paramQueryStringBuilder)
            .addParameter(paramPredicateRegistry);
    }

    private TypeSpec generateClassQFiltered() {
        var qFilteredConfig = typedQueryConfig.qFilteredConfig();

        var methodBuild = createQPredicateBuildMethodSignature().addModifiers(Modifier.ABSTRACT).build();
        var typeQPredicate = qFilteredConfig.name().nestedClass("QPredicate");
        var interfaceQPredicate = TypeSpec.interfaceBuilder(typeQPredicate)
            .addMethod(methodBuild)
            .build();

        TypeSpec classBuiltinPredicate;
        {
            var fieldName = ParameterSpec.builder(String.class, "name").build();
            var fieldArgs = ParameterSpec.builder(ParameterizedTypeName.get(List.class, String.class), "args").build();

            String varCaptureName = "captureName";
            var methodBuildImpl = createQPredicateBuildMethodSignature()
                .addAnnotation(Override.class)
                .addStatement("var $N = $N.$N()", varCaptureName, paramPredicateRegistry, typedQueryConfig.predicateRegistryConfig().methodRequestBuiltInQueryCapture())
                .addStatement(CodeBlock.builder()
                    .add("$N\n", paramQueryStringBuilder)
                    .indent() // TODO: Is this needed or does JavaPoet add it automatically?
                    .add(".append('@').append($N)\n", varCaptureName)
                    .add(".append(\" (\")\n")
                    .add(".append('#').append($N).append('?')\n", fieldName)
                    .add(".append(' ')\n")
                    .add(".append('@').append($N)", varCaptureName)
                    .build()
                )
                .addStatement("$N.forEach(a -> $N.append(' ').append($N(a)))", fieldArgs, paramQueryStringBuilder, typedQueryConfig.qNodeImplConfig().methodCreateStringLiteral())
                .addStatement("$N.append(')')", paramQueryStringBuilder)
                .build();

            classBuiltinPredicate = TypeSpec.recordBuilder(qFilteredConfig.classBuiltinPredicate())
                .recordConstructor(canonicalRecordConstructor(fieldName, fieldArgs))
                .addSuperinterface(typeQPredicate)
                .addMethod(methodBuildImpl)
                .build();
        }

        TypeSpec classCustomPredicate;
        {
            var fieldPredicate = ParameterSpec.builder(
                ParameterizedTypeName.get(ClassName.get(Predicate.class), ParameterizedTypeName.get(ClassName.get(Stream.class), codeGenHelper.typedNodeConfig().className())),
                "predicate"
            ).build();

            String varPredicateName = "predicateName";
            String varCaptureName = "captureName";
            var methodBuildImpl = createQPredicateBuildMethodSignature()
                .addAnnotation(Override.class)
                .addStatement("var $N = $N.$N($N)", varPredicateName, paramPredicateRegistry, typedQueryConfig.predicateRegistryConfig().methodRegister(), fieldPredicate)
                .addStatement("var $N = $N", varCaptureName, varPredicateName)
                .addStatement(CodeBlock.builder()
                    .add("$N\n", paramQueryStringBuilder)
                    .indent() // TODO: Is this needed or does JavaPoet add it automatically?
                    .add(".append('@').append($N)\n", varCaptureName)
                    .add(".append(\" (\")\n")
                    .add(".append('#')\n")
                    .add(".append($N)\n", varPredicateName)
                    .add("// Don't add any predicate args; when evaluating predicate it will get the corresponding capture based on the predicate name\n")
                    .add(".append(\"?)\")")
                    .build()
                )
                .build();

            classCustomPredicate = TypeSpec.recordBuilder(qFilteredConfig.classCustomPredicate())
                .recordConstructor(canonicalRecordConstructor(fieldPredicate))
                .addSuperinterface(typeQPredicate)
                .addMethod(methodBuildImpl)
                .build();
        }

        var fieldNode = FieldSpec.builder(typeQNodeImpl, "node", Modifier.PRIVATE, Modifier.FINAL).build();
        var fieldPredicate = FieldSpec.builder(typeQPredicate, "predicate", Modifier.PRIVATE, Modifier.FINAL).build();

        var constructor = createInitializingConstructor(fieldNode, fieldPredicate);

        var methodBuildQuery = createBuildQueryImplOverride()
            .addComment("To avoid any ambiguity wrap the whole node in a group")
            .addStatement("$N.append('(')", paramQueryStringBuilder)
            .addStatement(createDelegatingBuildQueryCall(fieldNode.name()))
            .addStatement("$N.append(' ')", paramQueryStringBuilder)
            .addStatement("$N.$N($N, $N)", fieldPredicate, methodBuild.name(), paramQueryStringBuilder, paramPredicateRegistry)
            .addStatement("$N.append(')')", paramQueryStringBuilder)
            .build();

        return TypeSpec.classBuilder(qFilteredConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            .superclass(typeQNodeImpl)
            // Implement `QCapturable` to also allow capturing, in addition to filtering
            .addSuperinterface(typeQCapturable)
            .addType(interfaceQPredicate)
            .addType(classBuiltinPredicate)
            .addType(classCustomPredicate)
            .addField(fieldNode)
            .addField(fieldPredicate)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();

    }

    private TypeSpec generateInterfaceQCapturable() {
        var qCapturableConfig = typedQueryConfig.qCapturableConfig();
        var qNodeImplConfig = typedQueryConfig.qNodeImplConfig();

        String paramCaptureHandler = "captureHandler";
        var methodCaptured = MethodSpec.methodBuilder(qCapturableConfig.methodCaptured())
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .returns(typeQNode)
            .addParameter(
                ParameterizedTypeName.get(typedQueryConfig.captureHandlerConfig().name(), typeVarCollector, typeVarNode),
                paramCaptureHandler
            )
            .addJavadoc("TODO")
            .addJavadoc("\n@see " + TreeSitterDoc.CAPTURE.createHtmlLink())
            .addStatement(createNonNullCheck(paramCaptureHandler))
            .addStatement("return new $T<>($T.$N(this), $N)", typedQueryConfig.classQCaptured(), qNodeImplConfig.name(), qNodeImplConfig.methodFromNode(), paramCaptureHandler)
            .build();

        return TypeSpec.interfaceBuilder(qCapturableConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            .addSuperinterface(ParameterizedTypeName.get(typedQueryConfig.qFilterableConfig().name(), typeVarCollector, typeVarNode))
            .addJavadoc("TODO")
            .addMethod(methodCaptured)
            .build();
    }

    private TypeSpec generateClassQCaptured() {
        var fieldNode = FieldSpec.builder(typeQNodeImpl, "node", Modifier.PRIVATE, Modifier.FINAL).build();
        var fieldCaptureHandler = FieldSpec.builder(
            ParameterizedTypeName.get(typedQueryConfig.captureHandlerConfig().name(), typeVarCollector, unboundedWildcard()),
            "captureHandler",
            Modifier.PRIVATE, Modifier.FINAL
        ).build();

        var constructor = createInitializingConstructor(fieldNode, fieldCaptureHandler);

        String varCaptureName = "captureName";
        var methodBuildQuery = createBuildQueryImplOverride()
            .addStatement(createDelegatingBuildQueryCall(fieldNode.name()))
            .addStatement("var $N = $N.$N($N)", varCaptureName, paramCaptureRegistry, typedQueryConfig.captureRegistryConfig().methodRegisterHandler(), fieldCaptureHandler)
            .addStatement("$N.append(\" @\").append($N)", paramQueryStringBuilder, varCaptureName)
            .build();

        return TypeSpec.classBuilder(typedQueryConfig.classQCaptured())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            // Do not use 'quantifiable' as superclass, because quantifier should be applied before capturing
            .superclass(typeQNodeImpl)
            .addField(fieldNode)
            .addField(fieldCaptureHandler)
            .addMethod(constructor)
            .addMethod(methodBuildQuery)
            .build();
    }

    // TODO: QTypedNode is the only class implementing this, so maybe just move the method overrides there?
    private TypeSpec generateQCapturableQuantifiable() {
        var qQuantifiableConfig = typedQueryConfig.qQuantifiableConfig();

        // Package-private constructor, to prevent user code from accessing it
        var constructor = MethodSpec.constructorBuilder().build();

        var typeQQuantified = ParameterizedTypeName.get(typedQueryConfig.qQuantifiedConfig().name(), typeVarCollector, typeVarNode);
        String paramNode = "node";
        // Helper method because the `super` methods declare `QNode` as return type but actually return the internal class `QQuantified`
        var methodAsQuantified = MethodSpec.methodBuilder("asQuantified")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .returns(typeQQuantified)
            .addParameter(typeQNode, paramNode)
            .addStatement("return ($T) $N", typeQQuantified, paramNode)
            .build();

        // Override methods to return `QCapturable` instead of just `QNode`
        var typeCapturableQuantified = typedQueryConfig.classQCapturableQuantified();
        var methodZeroOrMore = MethodSpec.methodBuilder(qQuantifiableConfig.methodZeroOrMore())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQCapturable)
            .addStatement("return new $T<>($N(super.$N()))", typeCapturableQuantified, methodAsQuantified, qQuantifiableConfig.methodZeroOrMore())
            .build();

        var methodOneOrMore = MethodSpec.methodBuilder(qQuantifiableConfig.methodOneOrMore())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQCapturable)
            .addStatement("return new $T<>($N(super.$N()))", typeCapturableQuantified, methodAsQuantified, qQuantifiableConfig.methodOneOrMore())
            .build();

        var methodOptional = MethodSpec.methodBuilder(qQuantifiableConfig.methodOptional())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(typeQCapturable)
            .addStatement("return new $T<>($N(super.$N()))", typeCapturableQuantified, methodAsQuantified, qQuantifiableConfig.methodOptional())
            .build();

        return TypeSpec.classBuilder(typedQueryConfig.classQCapturableQuantifiable())
            // non-sealed but constructor is not publicly accessible
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.STATIC, Modifier.NON_SEALED)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            .superclass(typeQQuantifiable)
            .addSuperinterface(typeQCapturable)
            .addJavadoc("TODO")
            .addMethod(constructor)
            .addMethod(methodAsQuantified)
            .addMethod(methodZeroOrMore)
            .addMethod(methodOneOrMore)
            .addMethod(methodOptional)
            .build();
    }

    private TypeSpec generateQCapturableQuantified() {
        var qQuantifiedConfig = typedQueryConfig.qQuantifiedConfig();

        var typeQuantified = ParameterizedTypeName.get(qQuantifiedConfig.name(), typeVarCollector, typeVarNode);
        String paramQuantifiedNode = "quantifiedNode";
        var constructor = MethodSpec.constructorBuilder()
            .addParameter(typeQuantified, paramQuantifiedNode)
            // Access fields of node and pass them to super constructor
            .addStatement("super($N.$N, $N.$N)", paramQuantifiedNode, qQuantifiedConfig.fieldNode(), paramQuantifiedNode, qQuantifiedConfig.fieldQuantifier())
            .build();

        return TypeSpec.classBuilder(typedQueryConfig.classQCapturableQuantified())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            .superclass(typeQuantified)
            .addSuperinterface(typeQCapturable)
            .addMethod(constructor)
            .build();
    }

    public record QTypedNodeBuilderMethodData(String methodName, ClassName qTypeName, String nodeType) {
    }

    public TypeSpec generateBuilderClass(List<QTypedNodeBuilderMethodData> nodeBuilderMethodData) {
        var builderConfig = typedQueryConfig.builderConfig();

        // TODO Doc: Have list with general builder functions (missing, wildcard, alternation, ...)
        //   and list mapping from node type to builder method?
        var typeBuilder = TypeSpec.classBuilder(builderConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addJavadoc("TODO")
            // TODO Doc: Link to `collected` method?
            .addJavadoc("\n@param <$T> TODO", typeVarCollector);

        // Explicit public constructor
        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
        typeBuilder.addMethod(constructor);

        {
            String paramNodeType = "nodeType";
            var methodUnnamedNode = MethodSpec.methodBuilder(builderConfig.methodUnnamedNode())
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addParameter(String.class, paramNodeType)
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.ANONYMOUS_NODE.createHtmlLink())
                .addStatement("return new $T<>(null, $N)", typedQueryConfig.classQUnnamedNode(), paramNodeType)
                .build();
            typeBuilder.addMethod(methodUnnamedNode);

            String paramSupertype = "supertype";
            var methodUnnamedNodeWithSupertype = MethodSpec.methodBuilder(builderConfig.methodUnnamedNode())
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addParameter(String.class, paramSupertype)
                .addParameter(String.class, paramNodeType)
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.ANONYMOUS_NODE.createHtmlLink())
                .addJavadoc("\n@see " + TreeSitterDoc.SUPERTYPE_NODE.createHtmlLink())
                .addStatement(createNonNullCheck(paramSupertype))
                .addStatement("return new $T<>($N, $N)", typedQueryConfig.classQUnnamedNode(), paramSupertype, paramNodeType)
                .build();
            typeBuilder.addMethod(methodUnnamedNodeWithSupertype);
        }
        {
            var qWildcardNodeConfig = typedQueryConfig.qWildcardNodeConfig();
            var typeQWildcardNode = qWildcardNodeConfig.name();
            var typeQWildcardNodeParameterized = ParameterizedTypeName.get(typeQWildcardNode, typeVarCollector, typeVarNode);

            var methodAnyNamedNode = MethodSpec.methodBuilder(builderConfig.methodAnyNamedNode())
                .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.WILDCARD_NODE.createHtmlLink())
                .addStatement("return ($T) $T.$N", typeQWildcardNodeParameterized, typeQWildcardNode, qWildcardNodeConfig.constantNamed())
                .build();
            typeBuilder.addMethod(methodAnyNamedNode);

            var methodAnyNode = MethodSpec.methodBuilder(builderConfig.methodAnyNode())
                .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.WILDCARD_NODE.createHtmlLink())
                .addStatement("return ($T) $T.$N", typeQWildcardNodeParameterized, typeQWildcardNode, qWildcardNodeConfig.constantNamedOrUnnamed())
                .build();
            typeBuilder.addMethod(methodAnyNode);
        }

        {
            var qErrorNodeConfig = typedQueryConfig.qErrorNodeConfig();
            var typeQErrorNode = qErrorNodeConfig.name();
            var typeQErrorNodeParameterized = ParameterizedTypeName.get(typeQErrorNode, typeVarCollector, typeVarNode);
            var methodErrorNode = MethodSpec.methodBuilder(builderConfig.methodErrorNode())
                .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.ERROR_NODE.createHtmlLink())
                .addStatement("return ($T) $T.$N", typeQErrorNodeParameterized, typeQErrorNode, qErrorNodeConfig.constantInstance())
                .build();
            typeBuilder.addMethod(methodErrorNode);
        }

        {
            var qMissingNodeConfig = typedQueryConfig.qMissingNodeConfig();
            var typeQMissingNode = qMissingNodeConfig.name();
            var typeQMissingNodeParameterized = ParameterizedTypeName.get(typeQMissingNode, typeVarCollector, typeVarNode);
            var methodMissingNode = MethodSpec.methodBuilder(builderConfig.methodMissingNode())
                .addAnnotation(SUPPRESS_WARNINGS_UNCHECKED)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.MISSING_NODE.createHtmlLink())
                .addStatement("return ($T) $T.$N", typeQMissingNodeParameterized, typeQMissingNode, qMissingNodeConfig.constantAny())
                .build();
            typeBuilder.addMethod(methodMissingNode);
        }

        {
            var qNodeImplConfig = typedQueryConfig.qNodeImplConfig();
            String paramNodes = "nodes";
            var methodGroup = MethodSpec.methodBuilder(builderConfig.methodGroup())
                .addAnnotation(SafeVarargs.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addParameter(
                    ArrayTypeName.of(ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, WildcardTypeName.subtypeOf(typeVarNode))),
                    paramNodes
                )
                .varargs()
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.GROUP.createHtmlLink())
                .addStatement("return new $T<>($T.$N($N))", typedQueryConfig.classQGroup(), qNodeImplConfig.name(), qNodeImplConfig.methodListOf(), paramNodes)
                .build();
            typeBuilder.addMethod(methodGroup);
        }

        {
            var qNodeImplConfig = typedQueryConfig.qNodeImplConfig();
            String paramNodes = "nodes";
            var methodAlternation = MethodSpec.methodBuilder(builderConfig.methodAlternation())
                .addAnnotation(SafeVarargs.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addTypeVariable(typeVarNode)
                .returns(typeQQuantifiable)
                .addParameter(
                    ArrayTypeName.of(ParameterizedTypeName.get(typedQueryConfig.qNodeConfig().name(), typeVarCollector, WildcardTypeName.subtypeOf(typeVarNode))),
                    paramNodes
                )
                .varargs()
                .addJavadoc("TODO")
                .addJavadoc("\n@see " + TreeSitterDoc.ALTERNATION.createHtmlLink())
                .addStatement("return new $T<>($T.$N($N))", typedQueryConfig.classQAlternation(), qNodeImplConfig.name(), qNodeImplConfig.methodListOf(), paramNodes)
                .build();
            typeBuilder.addMethod(methodAlternation);
        }

        for (var builderMethodData : nodeBuilderMethodData) {
            var method = MethodSpec.methodBuilder(builderMethodData.methodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(builderMethodData.qTypeName(), typeVarCollector))
                .addJavadoc("TODO")
                .addStatement("return new $T<>()", builderMethodData.qTypeName())
                .build();
            typeBuilder.addMethod(method);
        }

        return typeBuilder.build();
    }

    public List<TypeSpec> generateTypes() {
        return List.of(
            generateInterfaceQNode(),
            generateClassQNodeImpl(),
            generateClassQQuantifiable(),
            generateClassQQuantified(),
            generateClassQGroup(),
            generateClassQAlternation(),
            generateClassQUnnamedNode(),
            generateClassQWildcardNode(),
            generateClassQErrorNode(),
            generateClassQMissingNode(),
            generateInterfaceQFilterable(),
            generateClassQFiltered(),
            generateInterfaceQCapturable(),
            generateClassQCaptured(),
            generateQCapturableQuantifiable(),
            generateQCapturableQuantified()
        );
    }
}
