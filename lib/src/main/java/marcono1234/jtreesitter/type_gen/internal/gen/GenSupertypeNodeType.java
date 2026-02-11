package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import marcono1234.jtreesitter.type_gen.CodeGenException;
import marcono1234.jtreesitter.type_gen.NameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.*;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.TypedNodeConfig.JavaFieldRef;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.NodeType;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Consumer;

/**
 * "Supertype" node type, which only defines subtypes.
 *
 * <p>Use {@link #create} to create instances.
 */
public final class GenSupertypeNodeType implements GenJavaInterface, GenNodeType {
    /**
     * Represents Java elements (fields, methods, ...) which will be added as members to the generated
     * Java class.
     *
     * <p>This record is not exhaustive; additional elements may be generated.
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
        List<CustomMethodData> customMethods
    ) {
    }

    private final String typeName;
    private final boolean isExtra;
    private final ClassName javaTypeName;
    private final GeneratedJavaClassMembers javaClassMembers;

    private final List<String> subtypesNames;
    private boolean populatedSubtypes;
    /** Populated by {@link #populateSubtypes(NodeTypeLookup)}  */
    private final List<GenNodeType> subtypes;
    /** Populated by repeated calls to {@link #addInterfaceToImplement(GenJavaInterface)} */
    private final List<GenJavaInterface> interfacesToImplement;
    /** Populated by {@link #setCommonMethods(Collection)} (optional) */
    private @Nullable List<GeneratedMethod> commonMethods;

    private GenSupertypeNodeType(String typeName, boolean isExtra, ClassName javaTypeName, GeneratedJavaClassMembers javaClassMembers, List<String> subtypesNames) {
        this.typeName = typeName;
        this.isExtra = isExtra;
        this.javaTypeName = javaTypeName;
        this.javaClassMembers = javaClassMembers;

        this.subtypesNames = subtypesNames;
        this.populatedSubtypes = false;
        this.subtypes = new ArrayList<>();
        this.interfacesToImplement = new ArrayList<>();
    }

    public static GenSupertypeNodeType create(NodeType nodeType, NameGenerator nameGenerator, TypeNameCreator typeNameCreator, CustomMethodsProviderImpl customMethodsProvider) throws CodeGenException {
        String typeName = nodeType.type;

        if (nodeType.children != null) {
            throw new CodeGenException("Supertype '%s' should not have children".formatted(typeName));
        }
        if (nodeType.fields != null && !nodeType.fields.isEmpty()) {
            throw new CodeGenException("Supertype '%s' should not have fields".formatted(typeName));
        }

        List<String> subtypesNames = new ArrayList<>();
        var subtypes = nodeType.subtypes;
        if (subtypes == null) {
            // Caller should have already checked this
            throw new IllegalStateException("Supertype has no subtypes");
        }
        if (subtypes.size() < 2) {
            throw new CodeGenException("Supertype '%s' has less than 2 subtypes".formatted(typeName));
        }

        for (var subtype : subtypes) {
            if (subtype.named) {
                subtypesNames.add(subtype.type);
            }
            // Else: Ignore non-named subtype since no typed node will be generated for it;
            //   for example tree-sitter-java has supertype `statement` with non-named subtype ';' (for empty statement)
        }

        ClassName javaTypeName = typeNameCreator.createOwnClassName(nameGenerator.generateJavaTypeName(typeName));
        String typeNameConstant = nameGenerator.generateTypeNameConstant(typeName);
        String typeIdConstant = nameGenerator.generateTypeIdConstant(typeName);
        var customMethods = customMethodsProvider.customMethodsForNodeType(typeName);
        var javaClassMembers = new GeneratedJavaClassMembers(typeNameConstant, typeIdConstant, customMethods);

        return new GenSupertypeNodeType(typeName, nodeType.extra, javaTypeName, javaClassMembers, subtypesNames);
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

    /**
     * Populates information about node subtypes of this node type, and informs them that they should
     * implement this Java interface.
     */
    public void populateSubtypes(NodeTypeLookup nodeTypeLookup) {
        if (populatedSubtypes) {
            throw new IllegalStateException("Subtypes have already been populated");
        }
        populatedSubtypes = true;

        for (String subtype : subtypesNames) {
            var subtypeGen = nodeTypeLookup.getNodeType(subtype);
            subtypeGen.addInterfaceToImplement(this);
            subtypes.add(subtypeGen);
        }
    }

    private void checkPopulatedSubtypes() {
        if (!populatedSubtypes) {
            throw new IllegalStateException("Subtypes have not been populated yet");
        }
    }

    @Override
    public boolean refersToType(GenRegularNodeType type, Set<GenRegularNodeType> seenTypes) {
        checkPopulatedSubtypes();
        return subtypes.stream().anyMatch(t -> t.refersToType(type, seenTypes));
    }

    @Override
    public void addInterfaceToImplement(GenJavaInterface i) {
        interfacesToImplement.add(i);
    }

    @Override
    public void setCommonMethods(Collection<GeneratedMethod> commonMethods) {
        if (this.commonMethods != null) {
            throw new IllegalStateException("Common methods have already been set");
        }
        this.commonMethods = List.copyOf(commonMethods);
    }

    @Override
    public String getTypeNameConstant() {
        return javaClassMembers.typeNameConstant();
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

    @Override
    public List<GenNodeType> getSubtypes() {
        checkPopulatedSubtypes();
        return subtypes;
    }

    @Override
    public List<GeneratedMethod> getGeneratedMethods() {
        return javaClassMembers.customMethods().stream().map(CustomMethodData::asGeneratedMethod).toList();
    }

    private void generateJavadoc(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper) {
        typeBuilder.addJavadoc("Supertype $L, with subtypes:", CodeGenHelper.createJavadocCodeTag(typeName));
        codeGenHelper.addJavadocTypeMapping(typeBuilder, subtypes, null);

        CustomMethodData.createCustomMethodsJavadocSection(javaClassMembers.customMethods()).ifPresent(typeBuilder::addJavadoc);
    }

    private void getAllSubtypeClasses(Consumer<GenRegularNodeType> consumer) {
        for (var subtype : subtypes) {
            switch (subtype) {
                // Only collect regular node types, because supertypes don't exist as nodes in the parsed tree
                // TODO ^ is this correct?
                case GenSupertypeNodeType nestedSupertype -> nestedSupertype.getAllSubtypeClasses(consumer);
                case GenRegularNodeType regularNodeType -> consumer.accept(regularNodeType);
            }
        }
    }

    private SequencedSet<GenRegularNodeType> getAllSubtypeClasses() {
        // Uses Set for the case that same node type appears multiple times as (transitive) subtype
        SequencedSet<GenRegularNodeType> allSubtypes = new LinkedHashSet<>();
        getAllSubtypeClasses(allSubtypes::add);
        return allSubtypes;
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
        methodBuilder.beginControlFlow("var $N = switch ($N.$N())", resultVar, nodeParam, jtreesitterNode.methodGetType());

        // Consider all subtypes, including transitive ones
        for (var subtype : getAllSubtypeClasses()) {
            ClassName nodeClass = subtype.getJavaTypeName();
            methodBuilder.addStatement("case $T.$N -> new $T($N)", nodeClass, subtype.getTypeNameConstant(), nodeClass, nodeParam);
        }
        methodBuilder.addStatement("default -> null");
        methodBuilder.endControlFlow();
        methodBuilder.addStatement(""); // add trailing semicolon for assignment statement

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

    private List<MethodSpec> generateMethodsFindNodes(CodeGenHelper codeGenHelper) {
        // Explicitly use the type names of all (transitive) subtypes, because even if tree-sitter query supported
        // supertype type name, it might include unnamed nodes in case supertype has unnamed nodes as subtype;
        // however for those no typed node class is generated, see related https://github.com/Marcono1234/jtreesitter-type-gen/issues/17
        var allSubtypeFields = getAllSubtypeClasses().stream()
            .map(c -> new JavaFieldRef(c.getJavaTypeName(), c.getTypeNameConstant()))
            .toList();
        return codeGenHelper.typedNodeConfig().generateMethodsFindNodes(javaTypeName, allSubtypeFields);
    }

    @Override
    public List<JavaFile> generateJavaCode(CodeGenHelper codeGenHelper) {
        checkPopulatedSubtypes();

        var typeBuilder = TypeSpec.interfaceBuilder(javaTypeName)
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
            .addSuperinterface(codeGenHelper.typedNodeConfig().className());

        for (var superInterface : interfacesToImplement) {
            typeBuilder.addSuperinterface(superInterface.getJavaTypeName());
        }

        for (var subtype : subtypes) {
            typeBuilder.addPermittedSubclass(subtype.getJavaTypeName());
        }

        generateJavadoc(typeBuilder, codeGenHelper);

        typeBuilder.addField(CodeGenHelper.createTypeNameConstantField(typeName, javaClassMembers.typeNameConstant()));
        // Note: Most likely for supertype nodes the numeric type ID is not that useful because they don't exist
        // as `Node` objects; but at least this dynamic lookup through Language verifies that the type name exists
        if (codeGenHelper.generatesNumericIdConstants()) {
            typeBuilder.addField(codeGenHelper.createTypeIdConstantField(javaClassMembers.typeIdConstant(), javaClassMembers.typeNameConstant()));
        }

        typeBuilder.addMethod(generateMethodFromNode(codeGenHelper));
        typeBuilder.addMethod(generateMethodFromNodeThrowing(codeGenHelper));

        typeBuilder.addMethods(generateMethodsFindNodes(codeGenHelper));
        javaClassMembers.customMethods().forEach(m -> typeBuilder.addMethod(m.generateMethod(true)));

        if (commonMethods != null) {
            commonMethods.forEach(m -> typeBuilder.addMethod(m.createCommonInterfaceMethodSpec()));
        }

        return List.of(codeGenHelper.createJavaFile(typeBuilder, javaTypeName));
    }

    @Override
    public String toString() {
        return "GenSupertypeNodeType{" +
            "typeName='" + typeName + '\'' +
            '}';
    }
}
