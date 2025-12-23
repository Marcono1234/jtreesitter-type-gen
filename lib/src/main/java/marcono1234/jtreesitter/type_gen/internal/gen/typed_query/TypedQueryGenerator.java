package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.TypedQueryNameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
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

/**
 * Code generator for the 'typed query' code, which provides a type-safe builder API to create a
 * tree-sitter query. See also the {@linkplain marcono1234.jtreesitter.type_gen.internal.gen.typed_query package Javadoc}.
 * The code generation is performed by {@link #generateCode(List)}.
 */
public class TypedQueryGenerator {
    final CodeGenHelper codeGenHelper;
    private final TypedQueryNameGenerator nameGenerator;
    final TypedQueryConfig typedQueryConfig;
    private final ClassName typedNodeName;

    /**
     * Type variable for the 'collector' which is provided by the user when executing the typed query and
     * passed to the user-defined capture handlers to process the captured nodes. It could be something as
     * simple as {@code List<TypedNode>} and the capture handlers are just {@code List::add}, or it could
     * be a custom user-defined type on which the user calls different methods in the different capture
     * handlers.
     *
     * <p>This type variable exists for all query builder classes ({@code QNode} and subtypes) as well as
     * the built {@code TypedQuery}.
     */
    final TypeVariableName typeVarCollector = TypeVariableName.get("C");

    /**
     * Type variable representing the node type. In many causes this is unbound (see also {@link #typeVarNodeBound})
     * to allow non-typed nodes such as error or wildcard nodes. Only in cases where the user can access the
     * matched node, e.g. for predicates and captures, the bound {@code ... extends TypedNode} variant should
     * be used.
     */
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
        var typedQueryMatchConfig = typedQueryConfig.typedQueryMatchConfig();
        var typeVarNode = this.typeVarNodeBound;

        return TypeSpec.interfaceBuilder(captureHandlerConfig.name())
            .addAnnotation(FunctionalInterface.class)
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(typeVarCollector)
            .addTypeVariable(typeVarNode)
            .addJavadoc("Called during query execution to handle a query match capture.")
            .addJavadoc("\nCapture handlers can be registered using {@link $T#$N}.", qCapturableConfig.name(), qCapturableConfig.methodCaptured())
            .addJavadoc(" See the {@link $T} documentation for more information.", typedQueryConfig.name())
            .addMethod(MethodSpec
                .methodBuilder(captureHandlerConfig.methodHandleCapture())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(typeVarCollector, "collector")
                .addParameter(typeVarNode, "node")
                .addJavadoc("Called during query execution to handle a query match capture.")
                .addJavadoc("\n\n<p><b>Important:</b> Implementations should pass the captured node only to the 'collector',")
                .addJavadoc("\nthis way collecting is scoped to a single execution of a query where the collector is provided,")
                .addJavadoc("\nsee {@link $T#$N}.", typedQueryMatchConfig.name(), typedQueryMatchConfig.methodCollectCaptures())
                .addJavadoc("\nPassing the captured node somewhere else other than the collector can make the query usage")
                .addJavadoc("\nmore error-prone, especially when the query is executed multiple times.")
                .build()
            )
            .build();
    }

    /** Used by the internal {@code CaptureRegistry} as prefix for capture names */
    private static final FieldSpec FIELD_CAPTURE_PREFIX = FieldSpec.builder(String.class, "CAPTURE_PREFIX", Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", "c")
        .build();

    /** Used by the internal {@code PredicateRegistry} as prefix for the name and capture of custom predicates */
    private static final FieldSpec FIELD_CUSTOM_PREDICATE_PREFIX = FieldSpec.builder(String.class, "CUSTOM_PREDICATE_PREFIX", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addJavadoc("Prefix used for custom predicates as well as the corresponding captures")
        .initializer("$S", "pc")
        .build();
    /** Used by the internal {@code PredicateRegistry} as prefix for captures of built-in predicates */
    private static final FieldSpec FIELD_BUILTIN_PREDICATE_CAPTURE_PREFIX = FieldSpec.builder(String.class, "BUILTIN_PREDICATE_CAPTURE_PREFIX", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
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

        var methodHasNoHandlers = MethodSpec.methodBuilder(captureRegistryConfig.methodHasNoHandlers())
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addStatement("return $N.isEmpty()", fieldHandlers)
            .build();

        String paramCollector = "collector";
        String paramCaptureName = "captureName";
        String paramNode = "node";
        String varHandlerIndex = "handlerIndex";
        String varHandler = "handler";
        var typeCaptureHandler = ParameterizedTypeName.get(typedQueryConfig.captureHandlerConfig().name(), typeVarCollector, typedNodeName);
        var methodInvoke = MethodSpec.methodBuilder(captureRegistryConfig.methodInvokeHandler())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(typeVarCollector, paramCollector)
            .addParameter(String.class, paramCaptureName)
            .addParameter(typedNodeName, paramNode)
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
            .addMethod(methodHasNoHandlers)
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
                "customPredicates",
                Modifier.PRIVATE, Modifier.FINAL
            )
            .initializer("new $T<>()", ArrayList.class)
            .build();

        var fieldBuiltInQueryCaptureIndex = FieldSpec.builder(int.class, "builtInQueryCaptureIndex", Modifier.PRIVATE).initializer("0").build();

        String varPredicateName = "predicateName";
        MethodSpec methodRegister;
        {
            String paramPredicate = "predicate";
            methodRegister = MethodSpec.methodBuilder(predicateRegistryConfig.methodRegisterCustomPredicate())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(typePredicate, paramPredicate)
                .addStatement(createNonNullCheck(paramPredicate))
                .addStatement("var $N = $N + $N.size()", varPredicateName, FIELD_CUSTOM_PREDICATE_PREFIX, fieldPredicates)
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
                .addStatement("if (!$N.startsWith($N) || !$N.endsWith(\"?\")) throw new $T(\"Unexpected predicate name: \" + $N)", varPredicateName, FIELD_CUSTOM_PREDICATE_PREFIX, varPredicateName, IllegalArgumentException.class, varPredicateName)
                .addComment("Remove trailing '?'")
                .addStatement("$1N = $1N.substring(0, $1N.length() - 1)", varPredicateName)
                .addStatement("int $N = $T.parseInt($N.substring($N.length()))", varPredicateIndex, Integer.class, varPredicateName, FIELD_CUSTOM_PREDICATE_PREFIX)
                .addStatement("var $N = $N.get($N)", varPredicate, fieldPredicates, varPredicateIndex)
                .addComment("Predicate name is also used as capture name")
                .addStatement("var $N = $N", varCaptureName, varPredicateName)
                .addStatement("var $N = $N.$N($N).stream().map($T::$N)", varCaptures, paramQueryMatch, queryMatch.methodFindNodes(), varCaptureName, typedNodeName, codeGenHelper.typedNodeConfig().methodFromNodeThrowing())
                .addStatement("return $N.test($N)", varPredicate, varCaptures)
                .build();
        }

        return TypeSpec.classBuilder(predicateRegistryConfig.name())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addField(FIELD_CUSTOM_PREDICATE_PREFIX)
            .addField(fieldPredicates)
            .addField(FIELD_BUILTIN_PREDICATE_CAPTURE_PREFIX)
            .addField(fieldBuiltInQueryCaptureIndex)
            .addMethod(methodRegister)
            .addMethod(methodRequestBuiltInQueryCapture)
            .addMethod(methodTest)
            .build();
    }

    private CodeBlock createNoCaptureHandlersCheck() {
        var qCapturableConfig = typedQueryConfig.qCapturableConfig();
        return CodeBlock.of("if ($N.$N()) throw new $T(\"No capture handlers have been registered using `$N#$N`\")", fieldCaptureRegistry, typedQueryConfig.captureRegistryConfig().methodHasNoHandlers(), IllegalStateException.class, qCapturableConfig.name().simpleName(), qCapturableConfig.methodCaptured());
    }

    // TODO: Maybe generate this as static nested class instead of non-static inner class to make it easier to use;
    //   e.g. currently it requires qualifying it with the enclosing class: `TypedQuery<C>.TypedQueryMatch` (?)
    private TypeSpec generateClassTypedQueryMatch() {
        var typedQueryMatchConfig = typedQueryConfig.typedQueryMatchConfig();
        var qCapturableConfig = typedQueryConfig.qCapturableConfig();
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
            .addModifiers(Modifier.PUBLIC)
            .addParameter(typeVarCollector, paramCollector)
            .addJavadoc("Collects all query match captures as typed nodes by calling the 'capture handlers' with the given '$N' and the captured nodes.", paramCollector)
            .addJavadoc("\nThe capture handlers had been registered using {@link $T#$N} during building of the query.", qCapturableConfig.name(), qCapturableConfig.methodCaptured())
            .addStatement(createNonNullCheck(paramCollector))
            // To simplify troubleshooting for user, throw exception when user tries to collect captures but no capture handlers have been registered
            // However, don't fail earlier (e.g. when building query) because user might have intentionally not specified capture, in case they
            // just want to check if input matches a certain pattern (defined by the query)
            .addStatement(createNoCaptureHandlersCheck())
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
            .addJavadoc("Returns the underlying jtreesitter {@code $N}.", queryMatch.className().simpleName())
            .addJavadoc("\n\n<p><b>Note:</b> Some information of the query match such as the captures and their names should")
            .addJavadoc("\nbe considered an implementation detail of the typed query builder and may change in the future.")
            .addStatement("return $N", fieldQueryMatch)
            .build();

        return TypeSpec.classBuilder(typedQueryMatchConfig.name())
            // Non-static inner class
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Type-safe variant of a query match. A match can have zero or more captured nodes which can be")
            .addJavadoc("\nobtained using {@link #$N}.", methodCollect)
            .addField(fieldQueryMatch)
            .addMethod(constructor)
            .addMethod(methodCollect)
            .addMethod(methodGetQueryMatch)
            .build();
    }


    private MethodSpec generateMethodFindMatches(FieldSpec fieldLanguage, FieldSpec fieldQuery, MethodSpec methodCreatePredicateCallback, boolean hasAllocatorParam) {
        // TODO: Should use `TypedNode` as start node? Probably not, but maybe have convenience overloads taking TypedNode, which just call `TypedNode#getNode`
        // TODO: Should verify that `node.getTree().getLanguage()` matches query language?

        var jtreesitter = codeGenHelper.jtreesitterConfig();
        var queryCursor = jtreesitter.queryCursor();

        String methodName = typedQueryConfig.methodFindMatches();
        var paramStartNode = ParameterSpec.builder(jtreesitter.node().className(), "startNode").build();
        var paramAllocator = ParameterSpec.builder(codeGenHelper.ffmApiConfig().classSegmentAllocator(), "allocator")
            .addJavadoc("allocator to use for the captured node objects; allows interacting with the nodes after the stream has been closed")
            .build();

        String varQueryCursor = "queryCursor";
        String varOptions = "options";
        var builder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get(Stream.class), typedQueryConfig.typedQueryMatchConfig().name()))
            .addParameter(paramStartNode)
            .addStatement(createNonNullCheck(paramStartNode));

        builder
            .addJavadoc("Executes the query and returns a stream of matches, starting at the given node.")
            .addJavadoc("\n\n<p><b>Important:</b> The {@code Stream} must be closed to release resources.")
            .addJavadoc("\nIt is recommended to use a try-with-resources statement.");

        if (hasAllocatorParam) {
            builder.addParameter(paramAllocator)
                .addStatement(createNonNullCheck(paramAllocator));
        } else {
            // The nodes are allocated with the Arena of the QueryCursor, so they cannot / should not be used anymore after the QueryCursor was closed
            builder
                .addJavadoc("\nAfter the stream was closed captured nodes should not be used anymore, otherwise the behavior is undefined,")
                .addJavadoc("\nincluding exceptions being thrown or possibly even a JVM crash.")
                // Add link to overload with custom 'allocator' parameter
                .addJavadoc("\nUse {@link #$N($T, $T)} to be able to access the nodes after the stream was closed.", methodName, paramStartNode.type(), paramAllocator.type());
        }

        builder
            .addJavadoc("\n\n<h4>Example</h4>")
            .addJavadoc("\n{@snippet lang=java :")
            .addJavadoc("\ntry (var matches = typedQuery.$N(start" + (hasAllocatorParam ? ", allocator" : "") + ")) {", methodName)
            .addJavadoc("\n  MyCollector collector = ...;")
            .addJavadoc("\n  matches.forEach(match -> {")
            .addJavadoc("\n    match.$N(collector);", typedQueryConfig.typedQueryMatchConfig().methodCollectCaptures())
            .addJavadoc("\n  });")
            .addJavadoc("\n  ...")
            .addJavadoc("\n}")
            .addJavadoc("\n}");

        String varNodeLanguage = "nodeLanguage";
        builder
            // Verify that given Node belongs to same Language object as query, because jtreesitter itself does not seem to verify this
            .addStatement("var $N = $N.$N().$N()", varNodeLanguage, paramStartNode, jtreesitter.node().methodGetTree(), jtreesitter.tree().methodGetLanguage())
            .addStatement("if (!$N.equals($N)) throw new $T(\"Node belongs to unexpected language; expected: \" + $N + \", actual: \" + $N)", varNodeLanguage, fieldLanguage, IllegalArgumentException.class, fieldLanguage, varNodeLanguage)
            .addStatement("var $N = new $T($N)", varQueryCursor, queryCursor.className(), fieldQuery)
            .addStatement("var $N = new $T($N())", varOptions, queryCursor.classNameOptions(), methodCreatePredicateCallback);

        var findMatchesCode = CodeBlock.builder().add("return $N.$N($N, ", varQueryCursor, queryCursor.methodFindMatches(), paramStartNode);
        if (hasAllocatorParam) {
            findMatchesCode.add("$N, ", paramAllocator);
        }
        findMatchesCode.add("$N)\n", varOptions)
            .add(".map($T::new)\n", typedQueryConfig.typedQueryMatchConfig().name())
            .add(".onClose($N::close)", varQueryCursor);

        return builder.addStatement(findMatchesCode.build()).build();
    }

    private MethodSpec generateMethodFindMatchesAndCollect() {
        var ffmApiConfig = codeGenHelper.ffmApiConfig();
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node().className();
        var typedQueryMatchConfig = typedQueryConfig.typedQueryMatchConfig();

        String methodName = typedQueryConfig.methodFindMatchesAndCollect();

        var paramStartNode = ParameterSpec.builder(jtreesitterNode, "startNode").build();
        var paramAllocator = ParameterSpec.builder(ffmApiConfig.classSegmentAllocator(), "allocator")
            .addJavadoc("allocator to use for the captured node objects")
            .build();
        var paramCollector = ParameterSpec.builder(typeVarCollector, "collector").build();
        String varMatches = "matches";
        return MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(paramStartNode)
            .addParameter(paramAllocator)
            .addParameter(paramCollector)
            .addJavadoc("Executes the query and for each match collects the captured nodes.")
            .addJavadoc("\n\n<p>This is a convenience method which first executes {@link #$N($T, $T)} and then for each match", typedQueryConfig.methodFindMatches(), jtreesitterNode, ffmApiConfig.classSegmentAllocator())
            .addJavadoc("\ncalls {@link $T#$N}.", typedQueryMatchConfig.name(), typedQueryMatchConfig.methodCollectCaptures())
            .addJavadoc("\n")
            .addJavadoc("\n<h4>Example</h4>")
            .addJavadoc("\n{@snippet lang=java :")
            .addJavadoc("\ntry (var arena = $N.$N()) {", ffmApiConfig.classArena().simpleName(), ffmApiConfig.methodArenaOfConfined())
            .addJavadoc("\n  var collector = ...;")
            .addJavadoc("\n  typedQuery.$N(startNode, arena, collector);", methodName)
            .addJavadoc("\n  ...")
            .addJavadoc("\n}")
            .addJavadoc("\n}")
            // Other params are checked by delegating call
            .addStatement(createNonNullCheck(paramCollector))
            // This is also checked by `TypedQueryMatch#collectCaptures`, but would not be called if there are no matches
            // Therefore check this here as well, to always fail if there are no capture handlers registered
            .addStatement(createNoCaptureHandlersCheck())
            .beginControlFlow("try (var $N = $N($N, $N))", varMatches, typedQueryConfig.methodFindMatches(), paramStartNode, paramAllocator)
            .addStatement("$N.forEach(match -> match.$N($N))", varMatches, typedQueryMatchConfig.methodCollectCaptures(), paramCollector)
            .endControlFlow()
            .build();
    }

    private void addTypedQueryMembers(TypeSpec.Builder builder) {
        var jtreesitter = codeGenHelper.jtreesitterConfig();
        var predicateRegistry = typedQueryConfig.predicateRegistryConfig();

        // This field is used to verify that when executing the query, the given Node object belongs to the same language
        // (the jtreesitter Query object stores the Language internally as well, but does not allow accessing it, and
        //  does not seem to check the Node language when executing a query)
        var fieldLanguage = FieldSpec.builder(jtreesitter.language().className(), "language", Modifier.PRIVATE, Modifier.FINAL).build();
        builder.addField(fieldLanguage);

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

        var constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(paramFromField(fieldLanguage))
            .addParameter(paramFromField(fieldQueryString))
            .addParameter(paramFromField(fieldCaptureRegistry))
            .addParameter(paramFromField(fieldPredicateRegistry))
            .addStatement("this.$N = $N", fieldLanguage, fieldLanguage)
            .addStatement("this.$N = $N", fieldQueryString, fieldQueryString)
            .beginControlFlow("try")
            .addStatement("this.$N = new $T($N, $N)", fieldQuery, jtreesitter.query().className(), fieldLanguage, fieldQueryString)
            .nextControlFlow("catch ($T e)", RuntimeException.class)
            .addStatement("throw new $T(\"Failed creating query; verify that children and fields are specified in the right order; if you expect the query to be valid please report this to the jtreesitter-type-gen maintainers; query string:\\n\\t\" + $N, e)", RuntimeException.class, fieldQueryString)
            .endControlFlow()
            .addStatement("this.$1N = $1N", fieldCaptureRegistry)
            .addStatement("this.$1N = $1N", fieldPredicateRegistry)
            .build();
        builder.addMethod(constructor);

        var methodClose = MethodSpec.methodBuilder("close")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addJavadoc("Releases the resources of the underlying jtreesitter query.")
            .addJavadoc("\nThis query object should not be used anymore after this method has been called.")
            .addStatement("$N.close()", fieldQuery)
            .build();
        builder.addMethod(methodClose);

        var methodToString = CodeGenHelper.createToStringMethodSignature()
            .addStatement("return $S + \"[query=\" + $N.toString() + \"]\"", typedQueryConfig.name().simpleName(), fieldQuery)
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

        var methodFindMatches = generateMethodFindMatches(fieldLanguage, fieldQuery, methodCreatePredicateCallback, false);
        builder.addMethod(methodFindMatches);
        var methodFindMatchesAllocator = generateMethodFindMatches(fieldLanguage, fieldQuery, methodCreatePredicateCallback, true);
        builder.addMethod(methodFindMatchesAllocator);

        builder.addMethod(generateMethodFindMatchesAndCollect());
    }

    private CodeBlock createTypedQueryJavadoc() {
        var qNodeConfig = typedQueryConfig.qNodeConfig();
        var qCapturableConfig = typedQueryConfig.qCapturableConfig();
        var typedQueryMatchConfig = typedQueryConfig.typedQueryMatchConfig();
        var builderConfig = typedQueryConfig.builderConfig();

        return CodeBlock.builder()
            .add("Type-safe wrapper around a Tree-sitter query.")
            .add("\n\n<p>The general usage looks like this:")
            .add("\n<ol>")
            .add("\n<li>Build the query using {@link $T}", builderConfig.name())
            .add("\n<li>Create a {@code $N} instance using {@link $T#$N $N} on one of the query node objects", typedQueryConfig.name().simpleName(), qNodeConfig.name(), qNodeConfig.methodBuildQuery(), qNodeConfig.methodBuildQuery())
            .add("\n<li>Execute the query using {@link #$N} or {@link #$N}", typedQueryConfig.methodFindMatches(), typedQueryConfig.methodFindMatchesAndCollect())
            .add("\n</ol>")
            .add("\nQuery builder nodes as well as the created {@code $N} are immutable.", typedQueryConfig.name().simpleName())
            .add("\nThe query should be {@linkplain #close() closed} eventually to release the underlying jtreesitter resources again.")
            .add("\n")
            .add("\n<h2>Capturing nodes</h2>")
            .add("\nJust like regular Tree-sitter queries, a typed query can capture matching nodes so that they")
            .add("\ncan be further inspected by user code afterwards. To support this in a type-safe way, there are two concepts involved:")
            .add("\n<ul>")
            .add("\n<li>a 'collector'<br>")
            .add("\nThis is a user-defined type which handles query captures and either directly processes them or collects them")
            .add("\nand makes them available after the query execution. In the typed query API this is represented as type variable {@code <$T>}.", typeVarCollector)
            .add("\nThe collector is specified when query captures are retrieved using {@link $T#$N}.", typedQueryMatchConfig.name(), typedQueryMatchConfig.methodCollectCaptures())
            .add("\n<li>a {@linkplain $T 'capture handler'}<br>", typedQueryConfig.captureHandlerConfig().name())
            .add("\nThis interface is implemented by the user. Capture handlers are registered using {@link $T#$N}.", qCapturableConfig.name(), qCapturableConfig.methodCaptured())
            .add("\nThey are called with the user-defined 'collector' and the captured node, and are then supposed to pass the")
            .add("\nnode to the collector.")
            .add("\n</ul>")
            .add("\nIn the simplest case the 'collector' might just be a {@code List<$N>} and the 'capture handlers'", codeGenHelper.typedNodeConfig().name())
            .add("\nare {@code List::add}. That means when {@code $N} is called a {@code List} is provided, the capture handlers", typedQueryMatchConfig.methodCollectCaptures())
            .add("\nadd the nodes to the list, and afterwards the captured nodes can be retrieved from the list.")
            .add("\n\n<p>However, depending on the use case a custom type might provide more flexibility. Consider this example")
            .add("\nwhich collects the hypothetical {@code NodeStringLiteral} and {@code NodeIntLiteral}:")
            .add("\n{@snippet lang=java :")
            .add("\nclass MyCollector {")
            .add("\n  public void addStringLiteral(NodeStringLiteral l) { ... }")
            .add("\n")
            .add("\n  public void addIntLiteral(NodeIntLiteral l) { ... }")
            .add("\n")
            .add("\n  public List<NodeStringLiteral> getStringLiterals() { ... }")
            .add("\n")
            .add("\n  public List<NodeIntLiteral> getIntLiterals() { ... }")
            .add("\n}")
            .add("\n")
            .add("\nvar q = new $N.$N<MyCollector>();", typedQueryConfig.name().simpleName(), builderConfig.name().simpleName())
            .add("\nvar typedQuery = q.$N(", builderConfig.methodAlternation())
            .add("\n    q.nodeStringLiteral().$N((myCollector, node) -> myCollector.addStringLiteral(node)),", qCapturableConfig.methodCaptured())
            .add("\n    q.nodeIntLiteral().$N((myCollector, node) -> myCollector.addIntLiteral(node))", qCapturableConfig.methodCaptured())
            .add("\n  ).$N(" + (codeGenHelper.languageUtilsConfig() == null ? "language" : "") + ")", typedQueryConfig.qNodeConfig().methodBuildQuery())
            .add("\n")
            .add("\ntry (var matches = typedQuery.$N(startNode)) {", typedQueryConfig.methodFindMatches())
            .add("\n  var myCollector = new MyCollector();")
            .add("\n  matches.forEach(match -> match.$N(myCollector));", typedQueryMatchConfig.methodCollectCaptures())
            .add("\n  System.out.println(\"strings: \" + myCollector.getStringLiterals());")
            .add("\n  System.out.println(\"ints: \" + myCollector.getIntLiterals());")
            .add("\n}")
            .add("\n")
            .add("\ntypedQuery.close();")
            .add("\n}")
            .add("\n")
            .add("\n<h2>Example</h2>")
            .add("\nConsider this hypothetical example for a 'class declaration' node which has fields 'name' and 'body', and")
            .add("\nwhere the body then has children such as a 'field declaration':")
            .add("\n{@snippet lang=java :")
            .add("\nvar q = new $N.$N<List<NodeIntLiteral>>();", typedQueryConfig.name().simpleName(), builderConfig.name().simpleName())
            .add("\nvar typedQuery = q.nodeClassDeclaration()")
            .add("\n  .withFieldName(q.nodeIdentifier().$N(\"MyClass\"))", typedQueryConfig.qFilterableConfig().methodTextEq())
            .add("\n  .withFieldBody(q.nodeClassBody().$N(", typedQueryConfig.qTypedNodeConfig().methodWithChildren())
            .add("\n    q.nodeFieldDeclaration()")
            .add("\n      // Capture int literals which are used as initializer")
            .add("\n      .withFieldInitializer(q.nodeIntLiteral().$N(List::add))", qCapturableConfig.methodCaptured())
            .add("\n  ))")
            .add("\n  .$N(" + (codeGenHelper.languageUtilsConfig() == null ? "language" : "") + ")", typedQueryConfig.qNodeConfig().methodBuildQuery())
            .add("\n")
            .add("\ntry (var matches = typedQuery.$N(startNode)) {", typedQueryConfig.methodFindMatches())
            .add("\n  var intLiterals = new ArrayList<NodeIntLiteral>();")
            .add("\n  matches.forEach(match -> match.$N(intLiterals));", typedQueryMatchConfig.methodCollectCaptures())
            .add("\n  System.out.println(\"ints: \" + intLiterals);")
            .add("\n}")
            .add("\n")
            .add("\ntypedQuery.close();")
            .add("\n}")
            .add("\n")
            .add("\n@param <$T> type of the user-defined 'collector' which processes query captures", typeVarCollector)
            .build();
    }

    /**
     * Generates the complete 'typed query' code.
     */
    public List<JavaFile> generateCode(List<GenNodeType> nodes) {
        var qNodeCommonGenerator = new QNodeCommonGenerator(this);
        var qTypedNodeGenerator = new QTypedNodeGenerator(qNodeCommonGenerator, nameGenerator);

        var javaFiles = new ArrayList<JavaFile>();
        var nodeBuilderMethodData = new ArrayList<QTypedNodeBuilderMethodData>();

        for (var node : nodes) {
            String typeName = node.getTypeName();
            String builderMethodName = nameGenerator.generateBuilderMethodName(typeName);
            var qTypedNodeData = qTypedNodeGenerator.generateQTypedNodeSubclass(node, builderMethodName);
            nodeBuilderMethodData.add(new QTypedNodeBuilderMethodData(
                builderMethodName,
                qTypedNodeData.className(),
                typeName
            ));
            javaFiles.add(qTypedNodeData.javaFile());
        }

        var typeBuilder = TypeSpec.classBuilder(typedQueryConfig.name())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addTypeVariable(typeVarCollector)
            .addSuperinterface(AutoCloseable.class)
            .addJavadoc(createTypedQueryJavadoc())
            .addType(generateInterfaceCaptureHandler())
            .addType(generateClassCaptureRegistry())
            .addType(generateClassPredicateRegistry())
            .addType(generateClassTypedQueryMatch())
            .addTypes(qNodeCommonGenerator.generateTypes())
            .addType(qTypedNodeGenerator.generateClass())
            .addType(qNodeCommonGenerator.generateBuilderClass(nodeBuilderMethodData));

        addTypedQueryMembers(typeBuilder);

        // Emit TypedQuery class first
        javaFiles.addFirst(codeGenHelper.createOwnJavaFile(typeBuilder));

        return javaFiles;
    }
}
