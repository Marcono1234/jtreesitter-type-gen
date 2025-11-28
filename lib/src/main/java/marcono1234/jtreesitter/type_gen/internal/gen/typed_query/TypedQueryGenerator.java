package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.NameGenerator;
import marcono1234.jtreesitter.type_gen.TypedQueryNameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenRegularNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenSupertypeNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.typed_query.QNodeCommonGenerator.QTypedNodeBuilderMethodData;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.*;

// TODO: Usage of JavaPoet for the code generation here is quite verbose, given that most of this is
//   boilerplate code, unaffected by the specified node-types.json
//   Is there a way to use a Java template / snippet with JavaPoet?

// TODO: Replace "TODO" Javadoc

// TODO: More comments and Javadoc

/**
 * Code generator for the {@code TypedQuery} class, which provides a type-safe builder API to create a
 * tree-sitter query.
 *
 * TODO: Add implementation note section (or comment) which explains general structure of generated code
 *   (e.g. collector, generated classes, methods, ...)
 */
public class TypedQueryGenerator {
    final CodeGenHelper codeGenHelper;
    private final TypedQueryNameGenerator nameGenerator;
    final TypedQueryConfig typedQueryConfig;
    private final ClassName typedNodeName;

    final TypeVariableName typeVarCollector = TypeVariableName.get("C");

    final TypeVariableName typeVarNode = TypeVariableName.get("N");
    /** Like {@link #typeVarNode}, but with a bound of {@code extends TypedNode} */
    final TypeVariableName typeVarNodeBound;

    private final FieldSpec fieldCaptureRegistry;

    public TypedQueryGenerator(CodeGenHelper codeGenHelper, TypedQueryNameGenerator nameGenerator) {
        this.codeGenHelper = codeGenHelper;
        this.nameGenerator = nameGenerator;
        this.typedQueryConfig = new TypedQueryConfig(codeGenHelper);
        this.typedNodeName = codeGenHelper.typedNodeConfig().className();
        // For consistency and simplicity use the same type var name as `typeVarNode`
        this.typeVarNodeBound = typeVarNode.withBounds(typedNodeName);

        this.fieldCaptureRegistry = FieldSpec.builder(
            ParameterizedTypeName.get(typedQueryConfig.captureRegistryConfig().name(), typeVarCollector),
            "captureRegistry",
            Modifier.PRIVATE, Modifier.FINAL
        ).build();
    }

    private TypeSpec generateInterfaceCaptureHandler() {
        var captureHandlerConfig = typedQueryConfig.captureHandlerConfig();
        var qCapturableConfig = typedQueryConfig.qCapturableConfig();

        return TypeSpec.interfaceBuilder(captureHandlerConfig.name())
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNodeBound)
            // TODO Doc: extend
            .addJavadoc("Used by {@link $T#$N}.", qCapturableConfig.name(), qCapturableConfig.methodCaptured())
            .addMethod(MethodSpec
                .methodBuilder(captureHandlerConfig.methodHandleCapture())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(typeVarCollector, "collector")
                .addParameter(typeVarNodeBound, "node")
                .addJavadoc("TODO")
                .build()
            )
            .build();
    }

    /** Used by the internal {@code CaptureRegistry} as prefix for capture names */
    private static final FieldSpec FIELD_CAPTURE_PREFIX = FieldSpec.builder(String.class, "CAPTURE_PREFIX", Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", "c")
        .build();

    /** Used by the internal {@code PredicateRegistry} as prefix for the name and capture of custom predicates */
    private static final FieldSpec FIELD_PREDICATE_PREFIX = FieldSpec.builder(String.class, "PREFIX", Modifier.STATIC, Modifier.FINAL)
        .addJavadoc("Prefix used for the predicates as well as the captures")
        .initializer("$S", "p")
        .build();
    /** Used by the internal {@code PredicateRegistry} as prefix for captures built-in predicates */
    private static final FieldSpec FIELD_BUILTIN_PREDICATE_CAPTURE_PREFIX = FieldSpec.builder(String.class, "BUILT_IN_QUERY_CAPTURE_PREFIX", Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", "pb")
        .build();

    private TypeSpec generateClassCaptureRegistry() {
        var captureRegistryConfig = typedQueryConfig.captureRegistryConfig();

        var typeCaptureHandlerWildcard = ParameterizedTypeName.get(typedQueryConfig.captureHandlerConfig().name(), typeVarCollector, unboundedWildcard());

        var fieldHandlers = FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(List.class), typeCaptureHandlerWildcard),
                "handlers",
                Modifier.PRIVATE, Modifier.FINAL
            )
            .initializer("new $T<>()", ArrayList.class)
            .build();

        String paramCaptureHandler = "captureHandler";
        String varCaptureName = "captureName";
        var methodRegister = MethodSpec.methodBuilder(captureRegistryConfig.methodRegisterHandler())
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addParameter(typeCaptureHandlerWildcard, paramCaptureHandler)
            .addStatement(createNonNullCheck(paramCaptureHandler))
            .addStatement("var $N = $N + $N.size()", varCaptureName, FIELD_CAPTURE_PREFIX, fieldHandlers)
            .addStatement("$N.add($N)", fieldHandlers, paramCaptureHandler)
            .addStatement("return $N", varCaptureName)
            .build();

        String paramCollector = "collector";
        String paramCaptureName = "captureName";
        String paramNode = "node";
        String varHandlerIndex = "handlerIndex";
        String varHandler = "handler";
        var typeCaptureHandler = ParameterizedTypeName.get(typedQueryConfig.captureHandlerConfig().name(), typeVarCollector, typedNodeName);
        var predicateRegistryName = typedQueryConfig.predicateRegistryConfig().name();
        var methodInvoke = MethodSpec.methodBuilder(captureRegistryConfig.methodInvokeHandler())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(typeVarCollector, paramCollector)
            .addParameter(String.class, paramCaptureName)
            .addParameter(typedNodeName, paramNode)
            .addComment("Ignore captures used by predicates")
            .beginControlFlow("if ($N.startsWith($T.$N) || $N.startsWith($T.$N))", paramCaptureName, predicateRegistryName, FIELD_PREDICATE_PREFIX, paramCaptureName, predicateRegistryName, FIELD_BUILTIN_PREDICATE_CAPTURE_PREFIX)
            .addStatement("return")
            .endControlFlow()
            .addStatement("if (!$N.startsWith($N)) throw new $T(\"Unexpected capture name: \" + $N)", paramCaptureName, FIELD_CAPTURE_PREFIX, IllegalArgumentException.class, paramCaptureName)
            .addStatement("int $N = $T.parseInt($N.substring($N.length()))", varHandlerIndex, Integer.class, paramCaptureName, FIELD_CAPTURE_PREFIX)
            .addStatement("$L var $N = ($T) $N.get($N)", SUPPRESS_WARNINGS_UNCHECKED, varHandler, typeCaptureHandler, fieldHandlers, varHandlerIndex)
            .addStatement("$N.$N($N, $N)", varHandler, typedQueryConfig.captureHandlerConfig().methodHandleCapture(), paramCollector, paramNode)
            .build();

        return TypeSpec.classBuilder(captureRegistryConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addTypeVariable(typeVarCollector)
            .addField(FIELD_CAPTURE_PREFIX)
            .addField(fieldHandlers)
            .addMethod(methodRegister)
            .addMethod(methodInvoke)
            .build();
    }

    private TypeSpec generateClassPredicateRegistry() {
        var predicateRegistryConfig = typedQueryConfig.predicateRegistryConfig();

        var typePredicate = ParameterizedTypeName.get(
            ClassName.get(Predicate.class),
            ParameterizedTypeName.get(ClassName.get(Stream.class), typedNodeName)
        );

        var fieldPredicates = FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(List.class), typePredicate),
                "predicates",
                Modifier.PRIVATE, Modifier.FINAL
            )
            .initializer("new $T<>()", ArrayList.class)
            .build();

        var fieldBuiltInQueryCaptureIndex = FieldSpec.builder(int.class, "builtInQueryCaptureIndex", Modifier.PRIVATE).initializer("0").build();

        String varPredicateName = "predicateName";
        MethodSpec methodRegister;
        {
            String paramPredicate = "predicate";
            methodRegister = MethodSpec.methodBuilder(predicateRegistryConfig.methodRegister())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(typePredicate, paramPredicate)
                .addStatement(createNonNullCheck(paramPredicate))
                .addStatement("var $N = $N + $N.size()", varPredicateName, FIELD_PREDICATE_PREFIX, fieldPredicates)
                .addStatement("$N.add($N)", fieldPredicates, paramPredicate)
                .addStatement("return $N", varPredicateName)
                .build();
        }

        var methodRequestBuiltInQueryCapture = MethodSpec.methodBuilder(predicateRegistryConfig.methodRequestBuiltInQueryCapture())
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return $N + $N++", FIELD_BUILTIN_PREDICATE_CAPTURE_PREFIX, fieldBuiltInQueryCaptureIndex)
            .build();

        MethodSpec methodTest;
        {
            String paramQueryPredicate = "queryPredicate";
            String paramQueryMatch = "queryMatch";
            String varPredicateIndex = "predicateIndex";
            String varPredicate = "predicate";
            String varCaptureName = "captureName";
            String varCaptures = "captures";
            var queryPredicate = codeGenHelper.jtreesitterConfig().queryPredicate();
            var queryMatch = codeGenHelper.jtreesitterConfig().queryMatch();
            methodTest = MethodSpec.methodBuilder(predicateRegistryConfig.methodTest())
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(queryPredicate.className(), paramQueryPredicate)
                .addParameter(queryMatch.className(), paramQueryMatch)
                .addStatement("if (!$N.$N().isEmpty()) throw new $T(\"Unexpected predicate args: \" + $N)", paramQueryPredicate, queryPredicate.methodGetArgs(), IllegalArgumentException.class, paramQueryPredicate)
                .addStatement("var $N = $N.$N()", varPredicateName, paramQueryPredicate, queryPredicate.methodGetName())
                .addStatement("if (!$N.startsWith($N) || !$N.endsWith(\"?\")) throw new $T(\"Unexpected predicate name: \" + $N)", varPredicateName, FIELD_PREDICATE_PREFIX, varPredicateName, IllegalArgumentException.class, varPredicateName)
                .addComment("Remove trailing '?'")
                .addStatement("$1N = $1N.substring(0, $1N.length() - 1)", varPredicateName)
                .addStatement("int $N = $T.parseInt($N.substring($N.length()))", varPredicateIndex, Integer.class, varPredicateName, FIELD_PREDICATE_PREFIX)
                .addStatement("var $N = $N.get($N)", varPredicate, fieldPredicates, varPredicateIndex)
                .addComment("Predicate name is also used as capture name")
                .addStatement("var $N = $N", varCaptureName, varPredicateName)
                .addStatement("var $N = $N.$N($N).stream().map($T::$N)", varCaptures, paramQueryMatch, queryMatch.methodFindNodes(), varCaptureName, typedNodeName, codeGenHelper.typedNodeConfig().methodFromNodeThrowing())
                .addStatement("return $N.test($N)", varPredicate, varCaptures)
                .build();
        }

        return TypeSpec.classBuilder(predicateRegistryConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addField(FIELD_PREDICATE_PREFIX)
            .addField(fieldPredicates)
            .addField(FIELD_BUILTIN_PREDICATE_CAPTURE_PREFIX)
            .addField(fieldBuiltInQueryCaptureIndex)
            .addMethod(methodRegister)
            .addMethod(methodRequestBuiltInQueryCapture)
            .addMethod(methodTest)
            .build();
    }

    private TypeSpec generateClassTypedQueryMatch() {
        var typedQueryMatchConfig = typedQueryConfig.typedQueryMatchConfig();
        var classQueryMatch = codeGenHelper.jtreesitterConfig().queryMatch().className();

        var fieldQueryMatch = FieldSpec.builder(classQueryMatch, "queryMatch").addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();

        var constructor = createInitializingConstructor(fieldQueryMatch);

        String paramCollector = "collector";
        String varCapture = "capture";
        String varCaptureName = "captureName";
        String varTypedNode = "typedNode";
        var typedNode = codeGenHelper.typedNodeConfig();
        var captureRegistry = typedQueryConfig.captureRegistryConfig();
        var queryMatch = codeGenHelper.jtreesitterConfig().queryMatch();
        var queryCapture = codeGenHelper.jtreesitterConfig().queryCapture();
        var methodCollect = MethodSpec.methodBuilder(typedQueryMatchConfig.methodCollectCaptures())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addParameter(typeVarCollector, paramCollector)
            .addJavadoc("TODO")
            .addStatement(createNonNullCheck(paramCollector))
            .beginControlFlow("for (var $N : $N.$N())", varCapture, fieldQueryMatch, queryMatch.methodCaptures())
            .addStatement("var $N = $N.$N()", varCaptureName, varCapture, queryCapture.methodName())
            .addComment("Ignore predicate captures")
            .beginControlFlow("if ($N.startsWith($T.$N))", varCaptureName, captureRegistry.name(), FIELD_CAPTURE_PREFIX)
            .addStatement("var $N = $T.$N($N.$N())", varTypedNode, typedNode.className(), typedNode.methodFromNodeThrowing(), varCapture, queryCapture.methodNode())
            .addStatement("$N.$N($N, $N, $N)", fieldCaptureRegistry, captureRegistry.methodInvokeHandler(), paramCollector, varCaptureName, varTypedNode)
            .endControlFlow()
            .endControlFlow()
            .build();

        var methodGetQueryMatch = MethodSpec.methodBuilder(typedQueryMatchConfig.methodGetQueryMatch())
            .addModifiers(Modifier.PUBLIC)
            .returns(queryMatch.className())
            .addJavadoc("TODO")
            .addStatement("return $N", fieldQueryMatch)
            .build();

        return TypeSpec.classBuilder(typedQueryMatchConfig.name())
            // Non-static inner class
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("TODO")
            .addField(fieldQueryMatch)
            .addMethod(constructor)
            .addMethod(methodCollect)
            .addMethod(methodGetQueryMatch)
            .build();
    }


    private MethodSpec generateMethodFindMatches(FieldSpec fieldQuery, MethodSpec methodCreatePredicateCallback, boolean hasAllocatorParam) {
        // TODO: Should use `TypedNode` as start node? Probably not, but maybe have convenience overloads taking TypedNode, which just call `TypedNode#getNode`
        // TODO: Should verify that `node.getTree().getLanguage()` matches query language?

        var jtreesitter = codeGenHelper.jtreesitterConfig();
        var queryCursor = jtreesitter.queryCursor();

        String paramNode = "node";
        String paramAllocator = "allocator";
        String varQueryCursor = "queryCursor";
        String varOptions = "options";
        var builder = MethodSpec.methodBuilder(typedQueryConfig.methodFindMatches())
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get(Stream.class), typedQueryConfig.typedQueryMatchConfig().name()))
            .addParameter(jtreesitter.node().className(), paramNode)
            .addJavadoc("TODO")
            .addStatement(createNonNullCheck(paramNode));

        // TODO: Generate Javadoc specific to whether custom allocator is used or not
        if (hasAllocatorParam) {
            builder.addParameter(codeGenHelper.ffmApiConfig().classSegmentAllocator(), paramAllocator)
                .addStatement(createNonNullCheck(paramAllocator));
        }

        // TODO: Generate example Javadoc

        builder.addStatement("var $N = new $T($N)", varQueryCursor, queryCursor.className(), fieldQuery)
            .addStatement("var $N = new $T($N())", varOptions, queryCursor.classNameOptions(), methodCreatePredicateCallback);

        var findMatchesCode = CodeBlock.builder().add("return $N.$N($N, ", varQueryCursor, queryCursor.methodFindMatches(), paramNode);
        if (hasAllocatorParam) {
            findMatchesCode.add("$N, ", paramAllocator);
        }
        findMatchesCode.add("$N)\n", varOptions)
            .add(".map($T::new)\n", typedQueryConfig.typedQueryMatchConfig().name())
            .add(".onClose($N::close)", varQueryCursor);

        return builder.addStatement(findMatchesCode.build()).build();
    }

    private void addTypedQueryMembers(TypeSpec.Builder builder) {
        var jtreesitter = codeGenHelper.jtreesitterConfig();
        var predicateRegistry = typedQueryConfig.predicateRegistryConfig();

        // This query string field is mainly for tests within this project to perform assertions on it;
        // it is not needed for functionality
        var fieldQueryString = FieldSpec.builder(String.class, "queryString", Modifier.FINAL).build();
        builder.addField(fieldQueryString);

        var fieldQuery = FieldSpec.builder(jtreesitter.query().className(), "query", Modifier.PRIVATE, Modifier.FINAL).build();
        builder.addField(fieldQuery);

        var fieldCaptureRegistry = FieldSpec.builder(
            ParameterizedTypeName.get(typedQueryConfig.captureRegistryConfig().name(), typeVarCollector),
            "captureRegistry",
            Modifier.PRIVATE, Modifier.FINAL
        ).build();
        builder.addField(fieldCaptureRegistry);

        var fieldPredicateRegistry = FieldSpec.builder(predicateRegistry.name(), "predicateRegistry", Modifier.PRIVATE, Modifier.FINAL).build();
        builder.addField(fieldPredicateRegistry);

        var paramLanguage = "language";
        var paramQueryString = "queryString";
        var constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(jtreesitter.language().className(), paramLanguage)
            .addParameter(String.class, paramQueryString)
            .addParameter(paramFromField(fieldCaptureRegistry))
            .addParameter(paramFromField(fieldPredicateRegistry))
            .addStatement("this.$N = $N", fieldQueryString, paramQueryString)
            .beginControlFlow("try")
            .addStatement("this.$N = new $T($N, $N)", fieldQuery, jtreesitter.query().className(), paramLanguage, paramQueryString)
            .nextControlFlow("catch ($T e)", RuntimeException.class)
            .addStatement("throw new $T(\"Failed creating query; please report this to the jtreesitter-type-gen maintainers; query string:\\n\\t\" + $N, e)", RuntimeException.class, paramQueryString)
            .endControlFlow()
            .addStatement("this.$1N = $1N", fieldCaptureRegistry)
            .addStatement("this.$1N = $1N", fieldPredicateRegistry)
            .build();
        builder.addMethod(constructor);

        var methodClose = MethodSpec.methodBuilder("close")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addJavadoc("TODO")
            .addStatement("$N.close()", fieldQuery)
            .build();
        builder.addMethod(methodClose);

        var methodToString = CodeGenHelper.createToStringMethodSignature()
            .addStatement("return $S + \"[query=\" + $N.toString() + \"]\"", typedQueryConfig.name(), fieldQuery)
            .build();
        builder.addMethod(methodToString);

        var methodCreatePredicateCallback = MethodSpec.methodBuilder("createPredicateCallback")
            .addModifiers(Modifier.PRIVATE)
            .returns(ParameterizedTypeName.get(
                ClassName.get(BiPredicate.class),
                jtreesitter.queryPredicate().className(),
                jtreesitter.queryMatch().className()
            ))
            .addStatement("return $N::$N", fieldPredicateRegistry, predicateRegistry.methodTest())
            .build();
        builder.addMethod(methodCreatePredicateCallback);

        var methodFindMatches = generateMethodFindMatches(fieldQuery, methodCreatePredicateCallback, false);
        builder.addMethod(methodFindMatches);
        var methodFindMatchesAllocator = generateMethodFindMatches(fieldQuery, methodCreatePredicateCallback, true);
        builder.addMethod(methodFindMatchesAllocator);
    }

    public List<JavaFile> generateCode(List<GenNodeType> nodes) {
        var qNodeCommonGenerator = new QNodeCommonGenerator(this);
        var qTypedNodeGenerator = new QTypedNodeGenerator(qNodeCommonGenerator, nameGenerator);

        var javaFiles = new ArrayList<JavaFile>();
        var nodeBuilderMethodData = new ArrayList<QTypedNodeBuilderMethodData>();

        for (var node : nodes) {
            String typeName = node.getTypeName();
            var qTypedNodeData = qTypedNodeGenerator.generateQTypedNode(node);
            nodeBuilderMethodData.add(new QTypedNodeBuilderMethodData(
                nameGenerator.generateBuilderMethodName(typeName),
                qTypedNodeData.className(),
                typeName
            ));
            javaFiles.add(qTypedNodeData.javaFile());
        }

        var typeBuilder = TypeSpec.classBuilder(typedQueryConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addTypeVariable(typeVarCollector)
            .addSuperinterface(AutoCloseable.class)
            .addJavadoc("TODO")
            .addType(generateInterfaceCaptureHandler())
            .addType(generateClassCaptureRegistry())
            .addType(generateClassPredicateRegistry())
            .addType(generateClassTypedQueryMatch())
            .addTypes(qNodeCommonGenerator.generateTypes())
            .addType(qTypedNodeGenerator.generateClass())
            .addType(qNodeCommonGenerator.generateBuilderClass(nodeBuilderMethodData));

        addTypedQueryMembers(typeBuilder);

        // Emit TypedQuery class first
        javaFiles.addFirst(codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build());

        return javaFiles;
    }
}
