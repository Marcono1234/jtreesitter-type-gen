package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.TypeNameCreator;

import javax.lang.model.element.Modifier;
import java.lang.annotation.*;

/**
 * Code generator for the {@code @NonEmpty} annotation type.
 */
public class NonEmptyAnnotationGenerator {
    private final ClassName typeName;

    public NonEmptyAnnotationGenerator(TypeNameCreator typeNameCreator) {
        this.typeName = typeNameCreator.getNonEmptyTypeName();
    }

    private void generateJavadoc(TypeSpec.Builder typeBuilder) {
        typeBuilder.addJavadoc("Indicates that the annotated container type will not be empty.");
    }

    public JavaFile generateCode(CodeGenHelper codeGenHelper) {
        var typeBuilder = TypeSpec.annotationBuilder(typeName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Documented.class)
            .addAnnotation(AnnotationSpec.builder(Retention.class)
                .addMember("value", "$T.$N", RetentionPolicy.class, RetentionPolicy.SOURCE.name())
                .build()
            )
            .addAnnotation(AnnotationSpec.builder(Target.class)
                .addMember("value", "{ $T.$N }", ElementType.class, ElementType.TYPE_USE.name())
                .build()
            );

        generateJavadoc(typeBuilder);

        return codeGenHelper.createJavaFile(typeBuilder, typeName);
    }
}
