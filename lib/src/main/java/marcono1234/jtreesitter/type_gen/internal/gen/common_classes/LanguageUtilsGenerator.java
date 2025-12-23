package marcono1234.jtreesitter.type_gen.internal.gen.common_classes;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.LanguageConfig;
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
            .addModifiers(Modifier.STATIC, Modifier.FINAL);

        var providerDeclaringType = CodeGenHelper.createClassName(languageProviderConfig.declaringType());
        switch (languageProviderConfig) {
            case LanguageProviderConfig.Field field -> {
                fieldBuilder.initializer("$T.requireNonNull($T.$N)", Objects.class, providerDeclaringType, field.fieldName());
            }
            case LanguageProviderConfig.Method method -> {
                // Generates code which tries to call provider method, handling checked exceptions
                var code = CodeBlock.builder()
                    .beginControlFlow("try")
                    .addStatement("$N = $T.requireNonNull($T.$N())", fieldName, Objects.class, providerDeclaringType, method.methodName())
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

    // Note: Could also consider checking `Language#getName()`, but probably not worth it since it is most likely
    //   pretty obvious if user accidentally provides wrong Language
    private void generateLanguageVersionCheck(TypeSpec.Builder typeBuilder, String languageFieldName, LanguageConfig.LanguageVersion expectedVersion) {
        var language = codeGenHelper.jtreesitterConfig().language();
        var languageMetadata = codeGenHelper.jtreesitterConfig().languageMetadata();

        String methodName = "checkLanguageVersion";
        String expectedMajorVar = "expectedMajor";
        String expectedMinorVar = "expectedMinor";
        String expectedPatchVar = "expectedPatch";
        String metadataVar = "metadata";
        String versionVar = "languageVersion";
        typeBuilder.addMethod(MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addStatement("int $N = $L", expectedMajorVar, expectedVersion.major())
            .addStatement("int $N = $L", expectedMinorVar, expectedVersion.minor())
            .addStatement("int $N = $L", expectedPatchVar, expectedVersion.patch())
            .addStatement("var $N = $N.$N()", metadataVar, languageFieldName, language.methodGetMetadata())
            .addStatement("if ($N == null) throw new $T(\"Language metadata is not available\")", metadataVar, IllegalStateException.class)
            .addStatement("var $N = $N.$N()", versionVar, metadataVar, languageMetadata.methodVersion())
            // See also https://tree-sitter.github.io/tree-sitter/creating-parsers/6-publishing.html#adhering-to-semantic-versioning
            .addComment("Only allow 'patch' deviation, and only if 'major' is non-0 (otherwise it is interpreted as `0.<major>.<minor>`)")
            .addComment("This is stricter than SemVar, but is necessary because addition of node type ('minor' change) could lead to")
            .addComment("exceptions because generated code has no dedicated typed node class for this new node type")
            .beginControlFlow(CodeBlock.builder()
                .add("if (")
                .add("$N.$N() != $N", versionVar, languageMetadata.methodVersionMajor(), expectedMajorVar)
                .add(" || $N.$N() != $N", versionVar, languageMetadata.methodVersionMinor(), expectedMinorVar)
                .add(" || ($N == 0 && $N.$N() != $N)", expectedMajorVar, versionVar, languageMetadata.methodVersionPatch(), expectedPatchVar)
                .add(")")
                .build()
            )
            .addStatement("throw new $T(\"Unsupported language version, expected \" + $N + \".\" + $N + \".\" + $N + \" but was: \" + $N)", IllegalStateException.class, expectedMajorVar, expectedMinorVar, expectedPatchVar, versionVar)
            .endControlFlow()
            .build()
        );

        typeBuilder.addStaticBlock(CodeBlock.builder().addStatement("$N()", methodName).build());
    }


    public JavaFile generateCode() {
        var typeBuilder = TypeSpec.classBuilder(languageUtilsConfig.name())
            .addModifiers(Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
            .addJavadoc("Internal helper class.");

        String languageFieldName = languageUtilsConfig.fieldLanguage();
        generateLanguageField(typeBuilder, languageFieldName, languageUtilsConfig.languageProviderConfig());

        // Note: Generate this after the language field has already been initialized (above); otherwise it would read uninitialized field
        var expectedLanguageVersion = languageUtilsConfig.expectedLanguageVersion();
        if (expectedLanguageVersion != null) {
            generateLanguageVersionCheck(typeBuilder, languageFieldName, expectedLanguageVersion);
        }

        typeBuilder.addMethod(generateGetTypeIdMethod(languageFieldName));
        typeBuilder.addMethod(generateGetFieldIdMethod(languageFieldName));

        return codeGenHelper.createOwnJavaFile(typeBuilder);
    }
}
