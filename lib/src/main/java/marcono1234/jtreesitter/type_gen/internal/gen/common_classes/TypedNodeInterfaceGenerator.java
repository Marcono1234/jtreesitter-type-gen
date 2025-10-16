package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.internal.gen.GenJavaType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.GenRegularNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Code generator for the {@code TypedNode} interface, the base interface for all typed node classes, the
 * 'typed' variants of the jtreesitter {@code Node} class.
 */
public class TypedNodeInterfaceGenerator {
    private final CodeGenHelper codeGenHelper;
    private final CodeGenHelper.TypedNodeConfig config;

    public TypedNodeInterfaceGenerator(CodeGenHelper codeGenHelper) {
        this.codeGenHelper = Objects.requireNonNull(codeGenHelper);
        this.config = this.codeGenHelper.typedNodeConfig();
    }

    private void generateJavadoc(TypeSpec.Builder typeBuilder, List<GenNodeType> nodeTypes) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();

        typeBuilder.addJavadoc("Base type for all 'typed nodes'.");
        typeBuilder.addJavadoc("\nA jtreesitter {@link $T} can be converted to a typed node with {@link #$N} or {@link #$N},",
            jtreesitterNode.className(), config.methodFromNode(), config.methodFromNodeThrowing())
        .addJavadoc("\nor with the corresponding methods on the specific typed node classes.");

        // TODO: Is this really helpful?
        typeBuilder.addJavadoc("\n\n<h2>Node subtypes</h2>");
        codeGenHelper.addJavadocTypeMapping(typeBuilder, nodeTypes);
    }

    /** Generates the {@code fromNode} method, converting {@code Node -> TypedNode}. */
    private MethodSpec generateMethodFromNode(List<GenNodeType> nodeTypes) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();

        String nodeParam = "node";
        var methodBuilder = MethodSpec.methodBuilder(config.methodFromNode())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(jtreesitterNode.className(), nodeParam)
            .returns(codeGenHelper.getReturnOptionalType(config.className()))
            .addJavadoc("Wraps a jtreesitter node as typed node, returning $L if no corresponding typed node class exists.", codeGenHelper.getEmptyOptionalJavadocText())
            .addJavadoc("\nOnly works for <i>named</i> node types.")
            .addJavadoc("\n\n@see #$N", config.methodFromNodeThrowing());

        String resultVar = "result";
        methodBuilder.beginControlFlow("var $N = switch ($N.$N())", resultVar, nodeParam, jtreesitterNode.methodGetType());
        for (var nodeType : nodeTypes) {
            // Only consider regular node types, because supertypes don't exist as nodes in the parsed tree
            // TODO ^ is this correct?
            if (nodeType instanceof GenRegularNodeType regularNodeType) {
                ClassName nodeClass = nodeType.createJavaTypeName(codeGenHelper);
                methodBuilder.addStatement("case $T.$N -> new $T($N)", nodeClass, regularNodeType.getTypeNameConstant(), nodeClass, nodeParam);
            }
        }
        methodBuilder.addStatement("default -> null");
        methodBuilder.endControlFlow();
        methodBuilder.addStatement(""); // add trailing semicolon for assignment statement

        codeGenHelper.addReturnOptionalStatement(methodBuilder, resultVar);
        return methodBuilder.build();
    }

    /** Generates the {@code fromNodeThrowing} method, converting {@code Node -> TypedNode}. */
    private MethodSpec generateMethodFromNodeThrowing() {
        return config.generateMethodFromNodeThrowing(
            config.className(),
            "Wraps a jtreesitter node as typed node, throwing an {@link $T} if no corresponding typed node class exists."
            + "\nOnly works for <i>named</i> node types.",
            "Unknown node type"
        );
    }

    /**
     * @param nodeTypes
     *      all node types specified in {@code node-types.json} for which classes were generated
     * @param subtypes
     *      all subtypes of {@code TypedNode}; might be more than {@code nodeTypes} in case additional classes
     *      were generated, e.g. to combine multiple node child types
     * @return the generated Java code
     */
    public JavaFile generateCode(List<GenNodeType> nodeTypes, List<GenJavaType> subtypes) {
        var typeBuilder = TypeSpec.interfaceBuilder(config.name())
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED);

        for (var subtype : subtypes) {
            typeBuilder.addPermittedSubclass(subtype.createJavaTypeName(codeGenHelper));
        }

        generateJavadoc(typeBuilder, nodeTypes);

        generateTypedNodeAbstractMethods(typeBuilder, codeGenHelper);

        typeBuilder.addMethod(generateMethodFromNode(nodeTypes));
        typeBuilder.addMethod(generateMethodFromNodeThrowing());

        return codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build();
    }

    /** Generates all TypedNode abstract methods to be implemented by the subclasses. */
    private void generateTypedNodeAbstractMethods(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper) {
        var jtreesitter = codeGenHelper.jtreesitterConfig();
        var jtreesitterNode = jtreesitter.node();

        typeBuilder.addMethod(MethodSpec.methodBuilder(config.methodGetNode())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(jtreesitterNode.className())
            .addJavadoc("Returns the underlying jtreesitter node.")
            .build()
        );

        typeBuilder.addMethod(MethodSpec.methodBuilder(jtreesitterNode.methodGetText())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(codeGenHelper.getReturnOptionalType(ClassName.get(String.class)))
            .addJavadoc("Returns the source code of this node, if available.")
            .build());

        typeBuilder.addMethod(MethodSpec.methodBuilder(jtreesitterNode.methodGetRange())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(jtreesitter.classRange())
            .addJavadoc("Returns the range of this node.")
            .build());

        typeBuilder.addMethod(MethodSpec.methodBuilder(jtreesitterNode.methodGetStartPoint())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(jtreesitter.classPoint())
            .addJavadoc("Returns the start point of this node.")
            .build());

        typeBuilder.addMethod(MethodSpec.methodBuilder(jtreesitterNode.methodGetEndPoint())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(jtreesitter.classPoint())
            .addJavadoc("Returns the end point of this node.")
            .build());

        typeBuilder.addMethod(MethodSpec.methodBuilder(jtreesitterNode.methodHasError())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(boolean.class)
            // Note: Most likely the node itself cannot be an ERROR, because then construction of TypedNode would
            // have failed
            .addJavadoc("Returns whether this node or any of its child nodes represents an ERROR.")
            .build());
    }

    public record JavaFieldData(TypeName typeName, String name) {}

    // TODO: Instead of having every TypedNode subtype implement these methods, could instead define them as
    //   `default` methods in TypedNode, which use `#getNode()`?
    /**
     * Generates the implementation for all {@linkplain #generateTypedNodeAbstractMethods abstract typed node methods}.
     *
     * @param nodeField name of the Java field to generate which stores the jtreesitter Node
     * @param additionalFields additional Java fields to generate
     */
    public static void generateTypedNodeImplementation(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper, String nodeField, JavaFieldData... additionalFields) {
        var jtreesitter = codeGenHelper.jtreesitterConfig();
        var jtreesitterNode = jtreesitter.node();

        List<JavaFieldData> allFields = new ArrayList<>();
        allFields.add(new JavaFieldData(jtreesitterNode.className(), nodeField));
        Collections.addAll(allFields, additionalFields);

        // Package-private constructor; users should call static factory method instead
        var constructorBuilder = MethodSpec.constructorBuilder();

        for (var field : allFields) {
            String paramName = field.name();
            typeBuilder.addField(field.typeName(), field.name(), Modifier.PRIVATE, Modifier.FINAL);
            constructorBuilder.addParameter(field.typeName(), paramName);
            constructorBuilder.addStatement("this.$N = $N", field.name(), paramName);
        }

        typeBuilder.addMethod(constructorBuilder.build());

        var getNodeMethod = MethodSpec.methodBuilder(codeGenHelper.typedNodeConfig().methodGetNode())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(jtreesitterNode.className())
            .addStatement("return this.$N", nodeField)
            .build();
        typeBuilder.addMethod(getNodeMethod);

        var getTextMethod = codeGenHelper.createNullableDelegatingGetter(jtreesitterNode.methodGetText(), ClassName.get(String.class), nodeField)
            .addAnnotation(Override.class)
            .build();
        typeBuilder.addMethod(getTextMethod);

        var getRangeMethod = CodeGenHelper.createDelegatingGetter(jtreesitterNode.methodGetRange(), jtreesitter.classRange(), nodeField)
            .addAnnotation(Override.class)
            .build();
        typeBuilder.addMethod(getRangeMethod);

        var getStartPointMethod = CodeGenHelper.createDelegatingGetter(jtreesitterNode.methodGetStartPoint(), jtreesitter.classPoint(), nodeField)
            .addAnnotation(Override.class)
            .build();
        typeBuilder.addMethod(getStartPointMethod);

        var getEndPointMethod = CodeGenHelper.createDelegatingGetter(jtreesitterNode.methodGetEndPoint(), jtreesitter.classPoint(), nodeField)
            .addAnnotation(Override.class)
            .build();
        typeBuilder.addMethod(getEndPointMethod);

        var hasErrorMethod = CodeGenHelper.createDelegatingGetter(jtreesitterNode.methodHasError(), TypeName.get(boolean.class), nodeField)
            .addAnnotation(Override.class)
            .build();
        typeBuilder.addMethod(hasErrorMethod);
    }
}
