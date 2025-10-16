package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import javax.lang.model.element.Modifier;
import java.lang.annotation.*;
import java.util.Objects;

/**
 * Code generator for the {@code @NonEmpty} annotation type.
 */
public class NonEmptyAnnotationGenerator {
    private final CodeGenHelper codeGenHelper;

    public NonEmptyAnnotationGenerator(CodeGenHelper codeGenHelper) {
        this.codeGenHelper = Objects.requireNonNull(codeGenHelper);
    }

    private void generateJavadoc(TypeSpec.Builder typeBuilder) {
        typeBuilder.addJavadoc("Indicates that the annotated container type will not be empty.");
    }

    public JavaFile generateCode() {
        var typeBuilder = TypeSpec.annotationBuilder(codeGenHelper.getNonEmptyTypeName())
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

        return codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build();
    }
}
