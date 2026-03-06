package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.CodeGenerator;
import marcono1234.jtreesitter.type_gen.NameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.*;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CustomJavadocProviderImpl.SpecificCustomJavadocProvider;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.ChildType;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Children of a {@link GenRegularNodeType}.
 *
 * <p>Use {@link #create} to create instances.
 */
public class GenChildren {
    /**
     * Information about the getter method for retrieving the children, generated in the parent.
     */
    private record Getter(String methodName, Supplier<TypeName> returnType, SpecificCustomJavadocProvider customJavadocProvider) {
        public static Getter create(String methodName, TypeNameCreator typeNameCreator, Supplier<? extends TypeName> baseReturnType, boolean multiple, boolean required, SpecificCustomJavadocProvider customJavadocProvider) {
            // Must delay creation of the return type because it is not available until the children of all node types have been populated
            Supplier<TypeName> returnTypeSupplier = () -> {
                TypeName returnType = baseReturnType.get();
                if (multiple) {
                    returnType = ParameterizedTypeName.get(ClassName.get(List.class), returnType);
                    if (required) {
                        returnType = typeNameCreator.annotatedNonEmpty(returnType);
                    }
                } else {
                    if (!required) {
                        returnType = typeNameCreator.getReturnOptionalType(returnType);
                    }
                }
                return returnType;
            };
            returnTypeSupplier = new MemoizedSupplier<>(returnTypeSupplier);
            return new Getter(methodName, returnTypeSupplier, customJavadocProvider);
        }
    }

    private final Getter getter;
    private final GenChildType type;
    protected final boolean multiple;
    protected final boolean required;

    private GenChildren(Getter getter, GenChildType type, boolean multiple, boolean required) {
        this.getter = getter;
        this.type = type;
        this.multiple = multiple;
        this.required = required;
    }

    protected GenChildren(String getterName, TypeNameCreator typeNameCreator, GenChildType type, boolean multiple, boolean required, SpecificCustomJavadocProvider getterCustomJavadocProvider) {
        this(
            Getter.create(getterName, typeNameCreator, type.getJavaTypeNameSupplier(), multiple, required, getterCustomJavadocProvider),
            type,
            multiple,
            required
        );
    }

    public String getGetterName() {
        return getter.methodName();
    }

    public GenChildType getGenChildType() {
        return type;
    }

    /**
     * Whether the tree-sitter grammar permits multiple occurrences of this child.
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Whether the tree-sitter grammar requires that this child is present.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @see GenChildType#refersToTypeThroughInterface(GenRegularNodeType, Set)
     */
    public boolean refersToTypeThroughInterface(GenRegularNodeType type, Set<GenRegularNodeType> seenTypes) {
        return this.type.refersToTypeThroughInterface(type, seenTypes);
    }

    GeneratedMethod.Kind getGetterKind() {
        return new GeneratedMethod.KindChildren(multiple, required);
    }

    /**
     * For the to-be-generated getter method for obtaining the node children, returns a {@link GeneratedMethod}
     * representing that getter.
     */
    public GeneratedMethod getGetterGeneratedMethod() {
        var nodeTypes = new ArrayList<GenNodeType>();
        var nodeType = type.getRepresentedNodeType();
        if (nodeType != null) {
            nodeTypes.add(nodeType);
        }
        var supertypesResolver = GeneratedMethod.SupertypesResolver.forNodeTypes(nodeTypes);
        var returnType = new GeneratedMethod.ReturnType(getter.returnType().get(), supertypesResolver);

        return new GeneratedMethod(
            getGetterKind(),
            new GeneratedMethod.Signature(getter.methodName()),
            returnType
        );
    }

    /**
     * Generates code which obtains the children jtreesitter Node objects.
     *
     * @param nodeJavaFieldName name of the Java field which stores the underlying jtreesitter Node
     * @param childrenVarName name of the local variable to generate, which should store the obtained children
     */
    protected void addGetChildrenStatement(MethodSpec.Builder methodBuilder, CodeGenHelper codeGenHelper, String nodeJavaFieldName, String childrenVarName) {
        var nodeUtils = codeGenHelper.nodeUtilsConfig();

        // For now only include named children because `node-types.json` does not include information about non-named
        // child types, see `!t.named` check and comment further below
        methodBuilder.addStatement("var $N = $T.$N($N, true)", childrenVarName, nodeUtils.className(), nodeUtils.methodGetNonFieldChildren(), nodeJavaFieldName);
    }

    /**
     * Generates the complete method body for obtaining the children jtreesitter Node objects and converting
     * them to {@code TypedNode} objects.
     *
     * @param nodeJavaFieldName name of the Java field which stores the underlying jtreesitter Node
     */
    private void generateChildrenMethodBody(MethodSpec.Builder methodBuilder, CodeGenHelper codeGenHelper, String nodeJavaFieldName) {
        methodBuilder.returns(getter.returnType().get());

        String childrenVar = "children";
        addGetChildrenStatement(methodBuilder, codeGenHelper, nodeJavaFieldName, childrenVar);

        String mappedChildrenVar = "childrenMapped";
        type.addConvertingCall(methodBuilder, codeGenHelper, childrenVar, mappedChildrenVar);

        var nodeUtils = codeGenHelper.nodeUtilsConfig();
        String utilsMethodName = null;
        if (multiple) {
            if (required) {
                utilsMethodName = nodeUtils.methodAtLeastOneChild();
            }
        } else if (required) {
            utilsMethodName = nodeUtils.methodRequiredChild();
        } else {
            utilsMethodName = nodeUtils.methodOptionalChild();
        }

        if (utilsMethodName == null) {
            methodBuilder.addStatement("return $N", mappedChildrenVar);
        } else {
            methodBuilder.addStatement("return $T.$N($N)", nodeUtils.className(), utilsMethodName, mappedChildrenVar);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected void generateChildrenMethodJavadoc(MethodSpec.Builder methodBuilder, Optional<String> customJavadoc) {
        methodBuilder.addJavadoc("Retrieves the children nodes.");
        methodBuilder.addJavadoc("\n<ul>");
        methodBuilder.addJavadoc("\n<li>multiple: $L", multiple);
        methodBuilder.addJavadoc("\n<li>required: $L", required);
        methodBuilder.addJavadoc("\n</ul>");

        customJavadoc.ifPresent(methodBuilder::addJavadoc);
    }

    /**
     * @param enclosingTypeBuilder
     *      builder of the enclosing type; the children getter method is added to this builder, and if the child
     *      types are not generated as top-level types they are added to this builder as well
     * @param nodeJavaFieldName name of the Java field storing the jtreesitter Node object
     * @return top-level types to generate
     */
    public List<TypeBuilderWithName> generateJavaCode(TypeSpec.Builder enclosingTypeBuilder, CodeGenHelper codeGenHelper, String nodeJavaFieldName) {
        var getterMethodBuilder = MethodSpec.methodBuilder(getter.methodName())
            .addModifiers(Modifier.PUBLIC);

        generateChildrenMethodBody(getterMethodBuilder, codeGenHelper, nodeJavaFieldName);
        var getterCustomJavadoc = getter.customJavadocProvider().getJavadoc(codeGenHelper.customJavadocProvider());
        generateChildrenMethodJavadoc(getterMethodBuilder, getterCustomJavadoc);
        var getterMethod = getterMethodBuilder.build();
        assert getGetterGeneratedMethod().matchesMethodSpec(getterMethod);

        enclosingTypeBuilder.addMethod(getterMethod);
        var childJavaTypes = type.generateJavaTypes(codeGenHelper, getter.methodName());
        List<TypeBuilderWithName> topLevelTypes = new ArrayList<>();
        for (var childJavaType : childJavaTypes) {
            if (childJavaType.isTopLevel()) {
                topLevelTypes.add(childJavaType);
            } else {
                // Otherwise add as nested class
                enclosingTypeBuilder.addType(childJavaType.typeBuilder().build());
            }
        }
        return topLevelTypes;
    }

    public static GenChildren create(
        GenRegularNodeType parent,
        ChildType childTypeRaw,
        NodeTypeLookup nodeTypeLookup,
        NameGenerator nameGenerator,
        TypeNameCreator typeNameCreator,
        CustomMethodsProviderImpl customMethodsProvider,
        Consumer<GenJavaType> additionalTypedNodeSubtypeCollector
    ) {
        boolean multiple = childTypeRaw.multiple;
        boolean required = childTypeRaw.required;

        String parentNodeType = parent.getNodeType();

        // Currently getting non-field non-named children is not supported here, because it seems `node-types.json`
        // never contains that. Only for fields it includes non-named types.
        // Maybe code generation would actually support it, though it might produce clashing class names when both
        // named and non-named children exist.
        // To account for non-named children not being listed, for now a `getUnnamedChildren` method is generated
        // for typed node classes, see `GenRegularNodeType`.
        if (childTypeRaw.types.stream().anyMatch(t -> !t.named)) {
            throw new IllegalArgumentException(
                // In case Tree-sitter actually can generate such a `node-types.json`, user should report it so that
                // a possible solution can be investigated
                "Type '%s' defines non-named child types; please report this at %s"
                    .formatted(parentNodeType, CodeGenerator.version().gitRepository())
            );
        }

        List<String> childrenTypesNames = childTypeRaw.types.stream().map(t -> t.type).toList();
        String getterName = nameGenerator.generateChildrenGetterName(parentNodeType, childrenTypesNames, multiple, required);

        var childTypeConfigProvider = new GenChildType.ChildTypeConfigProvider() {
            @Override
            public String generateInterfaceName() {
                return nameGenerator.generateChildrenTypesName(parentNodeType, childrenTypesNames);
            }

            @Override
            public Optional<String> getInterfaceCustomJavadoc(CustomJavadocProviderImpl customJavadocProvider) {
                return customJavadocProvider.forNodeChildrenInterface(parentNodeType, childrenTypesNames);
            }

            /*
             * Note: The following methods for token types are effectively unused, see `!t.named` check and
             * comment at beginning of method above
             */

            @Override
            public String generateTokenClassName(List<String> tokenTypesNames) {
                return nameGenerator.generateChildrenTokenTypeName(parentNodeType, tokenTypesNames);
            }

            @Override
            public Optional<String> getTokenClassCustomJavadoc(CustomJavadocProviderImpl customJavadocProvider, List<String> tokenTypesNames) {
                return customJavadocProvider.forNodeChildrenTokenClass(parentNodeType, tokenTypesNames);
            }

            @Override
            public String generateTokenTypeConstantName(String tokenType, int index) {
                return nameGenerator.generateChildrenTokenName(parentNodeType, tokenType, index);
            }

            @Override
            public Optional<String> getTokenTypeCustomJavadoc(CustomJavadocProviderImpl customJavadocProvider, String tokenType) {
                return customJavadocProvider.forNodeChildrenToken(parentNodeType, tokenType);
            }
        };

        var childCustomMethodsProvider = new GenChildType.ChildCustomMethodsProvider() {
            @Override
            public List<CustomMethodData> createCustomMethods() {
                return customMethodsProvider.customMethodsForNodeChildrenInterface(parentNodeType, childrenTypesNames);
            }
        };

        var childType = GenChildType.create(parent, childTypeRaw.types, childTypeConfigProvider, typeNameCreator, nodeTypeLookup, additionalTypedNodeSubtypeCollector, childCustomMethodsProvider);
        SpecificCustomJavadocProvider getterCustomJavadocProvider = javadocProvider -> javadocProvider.forNodeChildrenGetter(parentNodeType, childrenTypesNames);

        return new GenChildren(getterName, typeNameCreator, childType, multiple, required, getterCustomJavadocProvider);
    }
}
