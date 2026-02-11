package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.NameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.common_classes.TypedNodeInterfaceGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.*;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.TypedNodeConfig.JavaFieldRef;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.ChildType;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.NodeType;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Consumer;

/**
 * A "regular" node type, optionally with children or fields.
 *
 * <p>Use {@link #create} to create instances.
 */
public final class GenRegularNodeType implements GenNodeType, GenJavaType {
    /**
     * Represents Java elements (fields, methods, ...) which will be added as members to the generated
     * Java class.
     *
     * <p>This record is not exhaustive; additional elements may be generated for children and fields.
     *
     * @param typeNameConstant
     *       Name of the Java constant field in the generated class storing the node type name.
     * @param typeIdConstant
     *      Name of the Java constant field in the generated class storing the numeric node type ID.
     *
     *      <p>Only generated if {@link CodeGenHelper#generatesNumericIdConstants()}.
     */
    private record GeneratedJavaClassMembers(
        String typeNameConstant,
        String typeIdConstant,
        @Nullable
        String getNonNamedChildrenMethodName,
        List<CustomMethodData> customMethods
    ) {
    }

    private final String typeName;
    private final boolean isExtra;
    private final ClassName javaTypeName;
    private final GeneratedJavaClassMembers javaClassMembers;

    private boolean populatedChildren;
    @Nullable
    private final ChildType childrenRaw;
    /** Initialized by {@link #populateChildrenAndFields} */
    @Nullable
    private GenChildren children;

    private final Map<String, ChildType> fieldsRaw;
    /** Initialized by {@link #populateChildrenAndFields} */
    private final List<GenField> fields;

    private final List<GenJavaInterface> interfacesToImplement;

    private GenRegularNodeType(String typeName, boolean isExtra, ClassName javaTypeName, GeneratedJavaClassMembers javaClassMembers, @Nullable ChildType childrenRaw, Map<String, ChildType> fieldsRaw) {
        this.typeName = typeName;
        this.isExtra = isExtra;
        this.javaTypeName = javaTypeName;
        this.javaClassMembers = javaClassMembers;

        this.populatedChildren = false;
        this.childrenRaw = childrenRaw;
        this.children = null; // is initialized later
        this.fieldsRaw = fieldsRaw;
        this.fields = new ArrayList<>();

        this.interfacesToImplement = new ArrayList<>();
    }

    public static GenRegularNodeType create(NodeType nodeType, NameGenerator nameGenerator, TypeNameCreator typeNameCreator, CustomMethodsProviderImpl customMethodsProvider) {
        String typeName = nodeType.type;
        ClassName javaTypeName = typeNameCreator.createOwnClassName(nameGenerator.generateJavaTypeName(typeName));
        String typeNameConstant = nameGenerator.generateTypeNameConstant(typeName);
        String typeIdConstant = nameGenerator.generateTypeIdConstant(typeName);

        var childrenRaw = nodeType.children;

        var fieldsRaw = nodeType.fields;
        if (fieldsRaw == null) {
            fieldsRaw = Map.of();
        }

        String getNonNamedChildrenMethodName = nameGenerator.generateNonNamedChildrenGetterName(typeName, childrenRaw != null, !fieldsRaw.isEmpty()).orElse(null);
        var customMethods = customMethodsProvider.customMethodsForNodeType(typeName);
        var javaClassMembers = new GeneratedJavaClassMembers(
            typeNameConstant,
            typeIdConstant,
            getNonNamedChildrenMethodName,
            customMethods
        );

        return new GenRegularNodeType(typeName, nodeType.extra, javaTypeName, javaClassMembers, childrenRaw, fieldsRaw);
    }

    @Override
    public void addInterfaceToImplement(GenJavaInterface i) {
        interfacesToImplement.add(i);
    }

    // TODO: Maybe implement this in a cleaner way?
    @Override
    public List<GenSupertypeNodeType> getSupertypes() {
        //noinspection NullableProblems; IntelliJ does not understand `Objects::nonNull` check?
        return interfacesToImplement.stream()
            .map(i -> i instanceof GenSupertypeNodeType supertype ? supertype : null)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Populates information about node children and fields of this node type.
     *
     * @param additionalTypedNodeSubtypeCollector
     *      collects TypedNode Java types in case additional ones are needed for children or fields
     */
    public void populateChildrenAndFields(NodeTypeLookup nodeTypeLookup, NameGenerator nameGenerator, TypeNameCreator typeNameCreator, CustomMethodsProviderImpl customMethodsProvider, Consumer<GenJavaType> additionalTypedNodeSubtypeCollector) {
        if (populatedChildren) {
            throw new IllegalStateException("Children or fields have already been populated");
        }
        populatedChildren = true;

        if (childrenRaw != null) {
            children = GenChildren.create(typeName, this, childrenRaw, nodeTypeLookup, nameGenerator, typeNameCreator, customMethodsProvider, additionalTypedNodeSubtypeCollector);
        }
        for (var field : fieldsRaw.entrySet()) {
            String fieldName = field.getKey();
            ChildType fieldType = field.getValue();
            fields.add(GenField.create(typeName, this, fieldName, fieldType, nodeTypeLookup, nameGenerator, typeNameCreator, customMethodsProvider, additionalTypedNodeSubtypeCollector));
        }
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public boolean isExtra() {
        return isExtra;
    }

    @Override
    public ClassName getJavaTypeName() {
        return javaTypeName;
    }

    @Override
    public String getTypeNameConstant() {
        return javaClassMembers.typeNameConstant();
    }

    private void checkPopulatedChildren() {
        if (!populatedChildren) {
            throw new IllegalStateException("Children have not been populated yet");
        }
    }

    public @Nullable GenChildren getGenChildren() {
        checkPopulatedChildren();
        return children;
    }

    public List<GenField> getGenFields() {
        checkPopulatedChildren();
        return fields;
    }

    @Override
    public boolean refersToType(GenRegularNodeType type, Set<GenRegularNodeType> seenTypes) {
        checkPopulatedChildren();

        if (seenTypes.add(this)) {
            return this.equals(type)
                || (children != null && children.refersToTypeThroughInterface(type, seenTypes))
                || fields.stream().anyMatch(f -> f.refersToTypeThroughInterface(type, seenTypes));
        } else {
            return false;
        }
    }

    @Override
    public List<GeneratedMethod> getGeneratedMethods() {
        checkPopulatedChildren();

        List<GeneratedMethod> methods = new ArrayList<>();
        if (children != null) {
            methods.add(children.getGetterGeneratedMethod());
        }
        for (var field : fields) {
            methods.add(field.getGetterGeneratedMethod());
        }

        javaClassMembers.customMethods().stream().map(CustomMethodData::asGeneratedMethod).forEach(methods::add);

        return methods;
    }

    /** Generates {@code Object} methods such as {@code equals}, {@code hashCode} and {@code toString}. */
    private void generateOverriddenObjectMethods(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper, String nodeField) {
        var equalsMethod = CodeGenHelper.createDelegatingEqualsMethod(javaTypeName, nodeField);
        typeBuilder.addMethod(equalsMethod);

        var hashCodeMethod = CodeGenHelper.createDelegatingHashCodeMethod(nodeField);
        typeBuilder.addMethod(hashCodeMethod);

        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var toStringMethod = CodeGenHelper.createToStringMethodSignature()
            // TODO: Include more information, e.g. position information? Or include wrapped node.toString()?
            .addStatement("return $S + \"[id=\" + $T.toUnsignedString($N.$N()) + \"]\"", javaTypeName.simpleName(), Long.class, nodeField, jtreesitterNode.methodGetId())
            .build();
        typeBuilder.addMethod(toStringMethod);
    }

    /**
     * Generates a getter method for non-field non-named children. {node-types.json} unfortunately does not
     * include information about them, but some grammars use them to represent relevant information:
     * For example tree-sitter-java defines {@code modifiers} as 'choice' of multiple keywords
     * ('public', 'protected', ...) so it would be useful for users to obtain the used keywords.
     */
    private MethodSpec generateMethodGetNonNamedChildren(CodeGenHelper codeGenHelper, String methodName, String nodeField) {
        var methodBuilder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(List.class, String.class))
            .addJavadoc("Returns the type names of the non-named, non-extra children, if any.")
            .addJavadoc("\n")
            .addJavadoc("\n<p><b>Important:</b> Whether this method has any useful or even any results at all depends on the grammar.")
            .addJavadoc("\nThis method can be useful when the grammar defines a 'choice' of multiple keywords.")
            .addJavadoc("\nIn that case this method returns the keywords which appear in the parsed source code.");

        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var nodeUtils = codeGenHelper.nodeUtilsConfig();
        methodBuilder.addStatement("return $T.$N($N, false).stream().map(n -> n.$N()).toList()", nodeUtils.className(), nodeUtils.methodGetNonFieldChildren(), nodeField, jtreesitterNode.methodGetType());
        return methodBuilder.build();
    }

    /** Generates the {@code fromNode} method. */
    private MethodSpec generateMethodFromNode(CodeGenHelper codeGenHelper) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var typedNode = codeGenHelper.typedNodeConfig();

        String nodeParam = "node";
        var methodBuilder = MethodSpec.methodBuilder(typedNode.methodFromNode())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(jtreesitterNode.className(), nodeParam)
            .returns(codeGenHelper.getReturnOptionalType(javaTypeName))
            .addJavadoc("Wraps a jtreesitter node as this node type, returning $L if the node has the wrong type.", codeGenHelper.getEmptyOptionalJavadocText())
            .addJavadoc("\n\n@see #$N", typedNode.methodFromNodeThrowing());

        String resultVar = "result";
        methodBuilder.addStatement("$T $N = null", javaTypeName, resultVar);
        CodeBlock nodeTypeCheck = codeGenHelper.generatesNumericIdConstants() ?
            CodeBlock.of("if ($N.$N() == $N)", nodeParam, jtreesitterNode.methodGetTypeId(), javaClassMembers.typeIdConstant())
            : CodeBlock.of("if ($N.equals($N.$N()))", javaClassMembers.typeNameConstant(), nodeParam, jtreesitterNode.methodGetType());
        methodBuilder
            .beginControlFlow(nodeTypeCheck)
            .addStatement("$N = new $T($N)", resultVar, javaTypeName, nodeParam)
            .endControlFlow();

        codeGenHelper.addReturnOptionalStatement(methodBuilder, resultVar);
        return methodBuilder.build();
    }

    /** Generates the {@code fromNodeThrowing} method. */
    private MethodSpec generateMethodFromNodeThrowing(CodeGenHelper codeGenHelper) {
        return codeGenHelper.typedNodeConfig().generateMethodFromNodeThrowing(
            javaTypeName,
            "Wraps a jtreesitter node as this node type, throwing an {@link $T} if the node has the wrong type.",
            "Wrong node type"
        );
    }

    private void generateJavadoc(TypeSpec.Builder typeBuilder) {
        typeBuilder.addJavadoc("Type {@value #$N}.", javaClassMembers.typeNameConstant());

        if (children != null) {
            typeBuilder.addJavadoc("\n<p>Children: {@link #$N}", children.getGetterName());
        }

        if (!fields.isEmpty()) {
            typeBuilder.addJavadoc("\n<p>Fields:");
            // TODO: Should generate HTML table instead of list?
            typeBuilder.addJavadoc("\n<ul>");
            for (var field : fields) {
                typeBuilder.addJavadoc("\n<li>{@link #$N $L}", field.getGetterName(), CodeGenHelper.escapeJavadocText(field.getFieldName()));
            }
            typeBuilder.addJavadoc("\n</ul>");
        }

        CustomMethodData.createCustomMethodsJavadocSection(javaClassMembers.customMethods()).ifPresent(typeBuilder::addJavadoc);
    }

    @Override
    public List<JavaFile> generateJavaCode(CodeGenHelper codeGenHelper) {
        checkPopulatedChildren();

        var typedNode = codeGenHelper.typedNodeConfig();

        var typeBuilder = TypeSpec.classBuilder(javaTypeName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(typedNode.className());

        for (var superInterface : interfacesToImplement) {
            typeBuilder.addSuperinterface(superInterface.getJavaTypeName());
        }

        generateJavadoc(typeBuilder);

        typeBuilder.addField(CodeGenHelper.createTypeNameConstantField(typeName, javaClassMembers.typeNameConstant()));
        if (codeGenHelper.generatesNumericIdConstants()) {
            typeBuilder.addField(codeGenHelper.createTypeIdConstantField(javaClassMembers.typeIdConstant(), javaClassMembers.typeNameConstant()));
        }

        String nodeField = "node";
        TypedNodeInterfaceGenerator.generateTypedNodeImplementation(typeBuilder, codeGenHelper, nodeField);
        typeBuilder.addMethod(generateMethodFromNode(codeGenHelper));
        typeBuilder.addMethod(generateMethodFromNodeThrowing(codeGenHelper));

        List<TypeBuilderWithName> javaTypes = new ArrayList<>();
        if (children != null) {
            javaTypes.addAll(children.generateJavaCode(typeBuilder, codeGenHelper, nodeField));
        }

        String getNonNamedChildrenMethodName = javaClassMembers.getNonNamedChildrenMethodName();
        if (getNonNamedChildrenMethodName != null) {
            typeBuilder.addMethod(generateMethodGetNonNamedChildren(codeGenHelper, getNonNamedChildrenMethodName, nodeField));
        }

        for (var field : fields) {
            javaTypes.addAll(field.generateJavaCode(typeBuilder, codeGenHelper, nodeField));
        }

        typeBuilder.addMethods(typedNode.generateMethodsFindNodes(javaTypeName, List.of(new JavaFieldRef(javaTypeName, javaClassMembers.typeNameConstant()))));

        generateOverriddenObjectMethods(typeBuilder, codeGenHelper, nodeField);
        javaClassMembers.customMethods().forEach(m -> typeBuilder.addMethod(m.generateMethod(false)));

        javaTypes.add(new TypeBuilderWithName(typeBuilder, javaTypeName));

        return javaTypes.stream().map(codeGenHelper::createJavaFile).toList();
    }

    @Override
    public String toString() {
        return "GenRegularNodeType{" +
            "typeName='" + typeName + '\'' +
            '}';
    }
}
