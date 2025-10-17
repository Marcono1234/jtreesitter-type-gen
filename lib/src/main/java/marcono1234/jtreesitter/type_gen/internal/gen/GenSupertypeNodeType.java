package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import marcono1234.jtreesitter.type_gen.CodeGenException;
import marcono1234.jtreesitter.type_gen.NameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.TypedNodeConfig.JavaFieldRef;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.NodeTypeLookup;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.NodeType;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Consumer;

/**
 * "Supertype" node type, which only defines subtypes.
 *
 * <p>Use {@link #create} to create instances.
 */
public final class GenSupertypeNodeType implements GenJavaInterface, GenNodeType {
    private final String typeName;
    private final String javaName;
    /** Name of the Java constant field in the generated class storing the node type name. */
    private final String typeNameConstant;
    /**
     * Name of the Java constant field in the generated class storing the numeric node type ID.
     *
     * <p>Only generated if {@link CodeGenHelper#generatesNumericIdConstants()}.
     */
    private final String typeIdConstant;
    private final List<String> subtypeNames;
    private boolean populatedSubtypes;
    private final List<GenNodeType> subtypes;
    private final List<GenJavaInterface> interfacesToImplement;

    private GenSupertypeNodeType(String typeName, String javaName, String typeNameConstant, String typeIdConstant, List<String> subtypeNames) {
        this.typeName = Objects.requireNonNull(typeName);
        this.javaName = Objects.requireNonNull(javaName);
        this.typeNameConstant = Objects.requireNonNull(typeNameConstant);
        this.typeIdConstant = Objects.requireNonNull(typeIdConstant);
        this.subtypeNames = Objects.requireNonNull(subtypeNames);
        this.populatedSubtypes = false;
        this.subtypes = new ArrayList<>();
        this.interfacesToImplement = new ArrayList<>();
    }

    public static GenSupertypeNodeType create(NodeType nodeType, NameGenerator nameGenerator) throws CodeGenException {
        String typeName = nodeType.type;

        if (nodeType.children != null) {
            throw new CodeGenException("Supertype '%s' should not have children".formatted(typeName));
        }
        if (nodeType.fields != null && !nodeType.fields.isEmpty()) {
            throw new CodeGenException("Supertype '%s' should not have fields".formatted(typeName));
        }

        List<String> subtypeNames = new ArrayList<>();
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
                subtypeNames.add(subtype.type);
            }
            // Else: Ignore non-named subtype since no typed node will be generated for it;
            //   for example tree-sitter-java has supertype `statement` with non-named subtype ';' (for empty statement)
        }

        String javaName = nameGenerator.generateJavaTypeName(typeName);
        String typeNameConstant = nameGenerator.generateTypeNameConstant(typeName);
        String typeIdConstant = nameGenerator.generateTypeIdConstant(typeName);
        return new GenSupertypeNodeType(typeName, javaName, typeNameConstant, typeIdConstant, subtypeNames);
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String getJavaName() {
        return javaName;
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

        for (String subtype : subtypeNames) {
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
    public boolean refersToType(GenRegularNodeType type, Set<GenJavaType> seenTypes) {
        checkPopulatedSubtypes();

        if (seenTypes.add(this)) {
            return subtypes.stream().anyMatch(t -> t.refersToType(type, seenTypes));
        } else {
            return false;
        }
    }

    @Override
    public void addInterfaceToImplement(GenJavaInterface i) {
        interfacesToImplement.add(i);
    }

    private void generateJavadoc(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper) {
        typeBuilder.addJavadoc("Supertype $L, with subtypes:", CodeGenHelper.createJavadocCodeTag(typeName));
        codeGenHelper.addJavadocTypeMapping(typeBuilder, subtypes);
    }

    private static void getAllSubtypeClasses(GenSupertypeNodeType type, Consumer<GenRegularNodeType> consumer) {
        for (var subtype : type.subtypes) {
            switch (subtype) {
                // Only collect regular node types, because supertypes don't exist as nodes in the parsed tree
                // TODO ^ is this correct?
                case GenSupertypeNodeType nestedSupertype -> getAllSubtypeClasses(nestedSupertype, consumer);
                case GenRegularNodeType regularNodeType -> consumer.accept(regularNodeType);
            }
        }
    }

    private Set<GenRegularNodeType> getAllSubtypeClasses() {
        // Uses Set for the case that same node type appears multiple times as (transitive) subtype
        Set<GenRegularNodeType> allSubtypes = new LinkedHashSet<>();
        getAllSubtypeClasses(this, allSubtypes::add);
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
            .returns(codeGenHelper.getReturnOptionalType(createJavaTypeName(codeGenHelper)))
            .addJavadoc("Wraps a jtreesitter node as this node type, returning $L if the node has the wrong type.", codeGenHelper.getEmptyOptionalJavadocText())
            .addJavadoc("\n\n@see #$N", typedNode.methodFromNodeThrowing());

        String resultVar = "result";
        methodBuilder.beginControlFlow("var $N = switch ($N.$N())", resultVar, nodeParam, jtreesitterNode.methodGetType());

        // Consider all subtypes, including transitive ones
        for (var subtype : getAllSubtypeClasses()) {
            ClassName nodeClass = subtype.createJavaTypeName(codeGenHelper);
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
            createJavaTypeName(codeGenHelper),
            "Wraps a jtreesitter node as this node type, throwing an {@link $T} if the node has the wrong type.",
            "Wrong node type"
        );
    }

    private List<MethodSpec> generateMethodsFindNodes(CodeGenHelper codeGenHelper) {
        var ownClassName = createJavaTypeName(codeGenHelper);

        // Explicitly use the type names of all (transitive) subtypes, because even if tree-sitter query supported
        // supertype type name, it might include unnamed nodes in case supertype has unnamed nodes as subtype;
        // however for those no typed node class is generated
        var allSubtypeFields = getAllSubtypeClasses().stream()
            .map(c -> new JavaFieldRef(c.createJavaTypeName(codeGenHelper), c.getTypeNameConstant()))
            .toList();
        return codeGenHelper.typedNodeConfig().generateMethodsFindNodes(ownClassName, allSubtypeFields);
    }

    @Override
    public List<JavaFile> generateJavaCode(CodeGenHelper codeGenHelper) {
        checkPopulatedSubtypes();

        var typeBuilder = TypeSpec.interfaceBuilder(javaName)
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
            .addSuperinterface(codeGenHelper.typedNodeConfig().className());

        for (var superInterface : interfacesToImplement) {
            typeBuilder.addSuperinterface(superInterface.createJavaTypeName(codeGenHelper));
        }

        for (var subtype : subtypes) {
            typeBuilder.addPermittedSubclass(subtype.createJavaTypeName(codeGenHelper));
        }

        generateJavadoc(typeBuilder, codeGenHelper);

        typeBuilder.addField(CodeGenHelper.createTypeNameConstantField(typeName, typeNameConstant));
        // Note: Most likely for supertype nodes the numeric type ID is not that useful because they don't exist
        // as `Node` objects; but at least this dynamic lookup through Language verifies that the type name exists
        if (codeGenHelper.generatesNumericIdConstants()) {
            typeBuilder.addField(codeGenHelper.createTypeIdConstantField(typeName, typeIdConstant));
        }

        typeBuilder.addMethod(generateMethodFromNode(codeGenHelper));
        typeBuilder.addMethod(generateMethodFromNodeThrowing(codeGenHelper));

        typeBuilder.addMethods(generateMethodsFindNodes(codeGenHelper));

        return List.of(codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build());
    }

    @Override
    public String toString() {
        return "GenSupertypeNodeType{" +
            "typeName='" + typeName + '\'' +
            '}';
    }
}
