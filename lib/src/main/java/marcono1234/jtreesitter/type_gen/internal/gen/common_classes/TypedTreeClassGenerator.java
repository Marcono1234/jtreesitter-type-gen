package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import marcono1234.jtreesitter.type_gen.internal.gen.GenNodeType;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CustomMethodData;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Code generator for the {@code TypedTree} class, the 'typed' variant of the jtreesitter {@code Tree}.
 */
public class TypedTreeClassGenerator {
    public record Config(
        ClassName className,
        String methodFromTree,
        String methodGetTree,
        String methodGetRootNode,
        String methodHasError
    ) {
        public static Config createDefault(CodeGenHelper codeGenHelper) {
            var jtreesitter = codeGenHelper.jtreesitterConfig();
            return new Config(
                codeGenHelper.createOwnClassName("TypedTree"),
                "fromTree",
                "getTree",
                // Uses same method name as jtreesitter
                jtreesitter.tree().methodGetRootNode(),
                // Uses same method name as jtreesitter
                jtreesitter.node().methodHasError()
            );
        }
    }

    private final CodeGenHelper codeGenHelper;
    private final Config config;
    private final List<CustomMethodData> customMethods;

    public TypedTreeClassGenerator(CodeGenHelper codeGenHelper, Config config, List<CustomMethodData> customMethods) {
        this.codeGenHelper = codeGenHelper;
        this.config = config;
        this.customMethods = customMethods;
    }

    public TypedTreeClassGenerator(CodeGenHelper codeGenHelper, List<CustomMethodData> customMethods) {
        this(codeGenHelper, Config.createDefault(codeGenHelper), customMethods);
    }

    private void generateJavadoc(TypeSpec.Builder typeBuilder, GenNodeType rootNodeType) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var typedNode = codeGenHelper.typedNodeConfig();

        typeBuilder.addJavadoc("A 'typed parse-tree', with expected root node {@link $T $L}.", rootNodeType.createJavaTypeName(codeGenHelper), CodeGenHelper.escapeJavadocText(rootNodeType.getTypeName()));
        typeBuilder.addJavadoc(" jtreesitter {@link $T} can be converted to a typed tree with {@link #$N}.", jtreesitterNode.className(), config.methodFromTree());

        typeBuilder.addJavadoc("\n\n<p>Individual jtreesitter nodes can be converted to a typed node with {@link $T#$N},", typedNode.className(), typedNode.methodFromNode());
        typeBuilder.addJavadoc(" or the {@code $N} method of the specific typed node classes.", typedNode.methodFromNode());

        CustomMethodData.createCustomMethodsJavadocSection(customMethods).ifPresent(typeBuilder::addJavadoc);
    }

    /** Generates {@code Object} methods such as {@code equals}, {@code hashCode} and {@code toString}. */
    private void generateOverriddenObjectMethods(TypeSpec.Builder typeBuilder, String treeField) {
        var className = config.className();
        var equalsMethod = CodeGenHelper.createDelegatingEqualsMethod(className, treeField);
        typeBuilder.addMethod(equalsMethod);

        var hashCodeMethod = CodeGenHelper.createDelegatingHashCodeMethod(treeField);
        typeBuilder.addMethod(hashCodeMethod);

        var toStringMethod = CodeGenHelper.createToStringMethodSignature()
            // TODO: Include more information?
            .addStatement("return $S", className.simpleName())
            .build();
        typeBuilder.addMethod(toStringMethod);
    }

    private void generateBody(TypeSpec.Builder typeBuilder, CodeGenHelper codeGenHelper, GenNodeType rootNodeType) {
        String treeField = "tree";
        var jtreesitter = codeGenHelper.jtreesitterConfig();
        var jtreesitterTree = jtreesitter.tree();
        var jtreesitterTreeClass = jtreesitterTree.className();
        var jtreesitterNode = jtreesitter.node();
        typeBuilder.addField(jtreesitterTreeClass, treeField, Modifier.PRIVATE, Modifier.FINAL);

        String treeParam = "tree";
        // Package-private constructor; users should call static factory method instead
        var constructor = MethodSpec.constructorBuilder()
            .addParameter(jtreesitterTreeClass, treeParam)
            .addStatement("this.$N = $N", treeField, treeParam)
            .build();
        typeBuilder.addMethod(constructor);

        var getTreeMethod = MethodSpec.methodBuilder(config.methodGetTree())
            .addModifiers(Modifier.PUBLIC)
            .returns(jtreesitterTreeClass)
            .addStatement("return $N", treeField)
            .addJavadoc("Returns the underlying jtreesitter tree.")
            .build();
        typeBuilder.addMethod(getTreeMethod);

        var className = config.className();
        Class<?> thrownExceptionType = IllegalArgumentException.class;
        String rootNodeTypeVar = "rootType";
        var fromTreeMethod = MethodSpec.methodBuilder(config.methodFromTree())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(jtreesitterTreeClass, treeParam)
            .returns(className)
            .addJavadoc("Wraps a jtreesitter tree as a typed tree, throwing an {@link $T} if the tree has an unexpected root node.", thrownExceptionType)
            // Check root node to prevent confusing exceptions later if wrong / unexpected tree was provided
            .addStatement("var $N = $N.$N().$N()", rootNodeTypeVar, treeParam, jtreesitterTree.methodGetRootNode(), jtreesitterNode.methodGetType())
            .beginControlFlow("if ($N.equals($S))", rootNodeTypeVar, rootNodeType.getTypeName())
            .addStatement("return new $T($N)", className, treeParam)
            .endControlFlow()
            .addStatement("throw new $T(\"Wrong node type: \" + $N)", thrownExceptionType, rootNodeTypeVar)
            .build();
        typeBuilder.addMethod(fromTreeMethod);

        var typedNode = codeGenHelper.typedNodeConfig();
        String rootNodeVar = "rootNode";
        String typedNodeVar = "result";
        var getRootNodeMethodBuilder = MethodSpec.methodBuilder(config.methodGetRootNode())
            .addModifiers(Modifier.PUBLIC)
            .returns(rootNodeType.createJavaTypeName(codeGenHelper))
            .addJavadoc("Returns the typed root node.")
            .addStatement("var $N = $N.$N()", rootNodeVar, treeField, jtreesitterTree.methodGetRootNode())
            .addStatement("var $N = $T.$N($N)", typedNodeVar, rootNodeType.createJavaTypeName(codeGenHelper), typedNode.methodFromNodeThrowing(), rootNodeVar)
            .addStatement("return $N", typedNodeVar);
        typeBuilder.addMethod(getRootNodeMethodBuilder.build());

        var getTextMethod = codeGenHelper.createNullableDelegatingGetter(jtreesitterTree.methodGetText(), ClassName.get(String.class), treeField)
            .addJavadoc("Returns the source code of the syntax tree, if available.")
            .build();
        typeBuilder.addMethod(getTextMethod);

        var hasErrorMethod = MethodSpec.methodBuilder(config.methodHasError())
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addJavadoc("Returns whether this tree contains any nodes with errors.")
            .addStatement("return $N.$N().$N()", treeField, jtreesitterTree.methodGetRootNode(), jtreesitterNode.methodHasError())
            .build();
        typeBuilder.addMethod(hasErrorMethod);

        typeBuilder.addSuperinterface(AutoCloseable.class);
        var closeMethod = MethodSpec.methodBuilder("close")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .addJavadoc("Closes the underlying jtreesitter tree, releasing the resources it holds.")
            .addStatement("$N.close()", treeField)
            .build();
        typeBuilder.addMethod(closeMethod);

        generateOverriddenObjectMethods(typeBuilder, treeField);

        customMethods.forEach(m -> typeBuilder.addMethod(m.generateMethod(false)));
    }

    public JavaFile generateCode(GenNodeType rootNodeType) {
        var typeBuilder = TypeSpec.classBuilder(config.className())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        generateJavadoc(typeBuilder, rootNodeType);

        generateBody(typeBuilder, codeGenHelper, rootNodeType);

        return codeGenHelper.createOwnJavaFile(typeBuilder);
    }
}
