package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageProviderConfig;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper.LanguageUtilsConfig;

import javax.lang.model.element.Modifier;
import java.util.Objects;

/**
 * Code generator for the internal {@code LanguageUtils} class, which provides convenience methods for working
 * with the {@code Language} object representing the language of the {@code node-types.json}.
 */
public class LanguageUtilsGenerator {
    private final CodeGenHelper codeGenHelper;
    private final LanguageUtilsConfig languageUtilsConfig;

    public LanguageUtilsGenerator(CodeGenHelper codeGenHelper, LanguageUtilsConfig languageUtilsConfig) {
        this.codeGenHelper = Objects.requireNonNull(codeGenHelper);
        this.languageUtilsConfig = Objects.requireNonNull(languageUtilsConfig);
    }

    private MethodSpec generateGetTypeIdMethod(String languageField) {
        var language = codeGenHelper.jtreesitterConfig().language();

        String nameParam = "name";
        return MethodSpec.methodBuilder(languageUtilsConfig.methodGetTypeId())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String.class, nameParam)
            .returns(language.numericIdType())
            .addCode(language.generateGetTypeIdCode(nameParam, languageField))
            .build();
    }

    private MethodSpec generateGetFieldIdMethod(String languageField) {
        var language = codeGenHelper.jtreesitterConfig().language();

        String nameParam = "name";
        return MethodSpec.methodBuilder(languageUtilsConfig.methodGetFieldId())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String.class, nameParam)
            .returns(language.numericIdType())
            .addCode(language.generateGetFieldIdCode(nameParam, languageField))
            .build();
    }

    private void generateLanguageField(TypeSpec.Builder typeBuilder, String fieldName, LanguageProviderConfig languageProviderConfig) {
        var jtreesitter = codeGenHelper.jtreesitterConfig();

        var fieldBuilder = FieldSpec.builder(jtreesitter.language().className(), fieldName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        var providerDeclaringType = CodeGenHelper.createClassName(languageProviderConfig.declaringType());
        switch (languageProviderConfig) {
            case LanguageProviderConfig.Field field -> {
                fieldBuilder.initializer("$T.$N", providerDeclaringType, field.fieldName());
            }
            case LanguageProviderConfig.Method method -> {
                // Generates code which tries to call provider method, handling checked exceptions
                var code = CodeBlock.builder()
                    .beginControlFlow("try")
                    .addStatement("$N = $T.$N()", fieldName, providerDeclaringType, method.methodName())
                    .nextControlFlow("catch ($T | $T e)", Error.class, RuntimeException.class)
                    .addStatement("throw e")
                    .nextControlFlow("catch ($T e)", Throwable.class)
                    .addStatement("throw new $T(\"Failed obtaining language instance\", e)", RuntimeException.class)
                    .endControlFlow()
                    .build();
                typeBuilder.addStaticBlock(code);
            }
        }

        typeBuilder.addField(fieldBuilder.build());
    }


    public JavaFile generateCode() {
        var typeBuilder = TypeSpec.classBuilder(languageUtilsConfig.name())
            .addModifiers(Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
            .addJavadoc("Internal helper class.");

        String languageFieldName = "language";
        generateLanguageField(typeBuilder, languageFieldName, languageUtilsConfig.languageProviderConfig());

        typeBuilder.addMethod(generateGetTypeIdMethod(languageFieldName));
        typeBuilder.addMethod(generateGetFieldIdMethod(languageFieldName));

        return codeGenHelper.createOwnJavaFileBuilder(typeBuilder).build();
    }
}
