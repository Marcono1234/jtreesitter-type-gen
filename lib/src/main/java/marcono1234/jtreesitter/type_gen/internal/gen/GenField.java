package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import marcono1234.jtreesitter.type_gen.NameGenerator;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CustomMethodData;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.NodeTypeLookup;
import marcono1234.jtreesitter.type_gen.internal.node_types_json.ChildType;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tree-sitter field of a {@link GenRegularNodeType}.
 *
 * <p>Use {@link #create} to create instances.
 */
public class GenField extends GenChildren {
    /**
     * Tree-sitter node field name.
     */
    private final String fieldName;
    /**
     * Name of the Java constant field in the generated class storing the field name.
     */
    private final String fieldNameConstant;
    /**
     * Name of the Java constant field in the generated class storing the numeric field name ID.
     *
     * <p>Only generated if {@link CodeGenHelper#generatesNumericIdConstants()}.
     */
    private final String fieldIdConstant;

    private GenField(String fieldName, String fieldNameConstant, String fieldIdConstant, String getterName, GenChildType type, boolean multiple, boolean required) {
        super(getterName, type, multiple, required);
        this.fieldName = fieldName;
        this.fieldNameConstant = fieldNameConstant;
        this.fieldIdConstant = fieldIdConstant;
    }

    /** Gets the tree-sitter node field name. */
    public String getFieldName() {
        return fieldName;
    }

    /** Name of the Java constant field in the generated class storing the field name. */
    public String getFieldNameConstant() {
        return fieldNameConstant;
    }

    @Override
    protected void generateChildrenMethodJavadoc(MethodSpec.Builder methodBuilder) {
        methodBuilder.addJavadoc("Retrieves the nodes of field {@value #$N}.", fieldNameConstant);
        methodBuilder.addJavadoc("\n<ul>");
        methodBuilder.addJavadoc("\n<li>multiple: $L", multiple);
        methodBuilder.addJavadoc("\n<li>required: $L", required);
        methodBuilder.addJavadoc("\n</ul>");
    }

    @Override
    protected void addGetChildrenStatement(MethodSpec.Builder methodBuilder, CodeGenHelper codeGenHelper, String nodeJavaFieldName, String childrenVarName) {
        var jtreesitterNode = codeGenHelper.jtreesitterConfig().node();
        var codeBuilder = CodeBlock.builder()
            .add("var $N = $N.", childrenVarName, nodeJavaFieldName);

        if (codeGenHelper.generatesNumericIdConstants()) {
            codeBuilder.add("$N($N)", jtreesitterNode.methodGetChildrenByFieldId(), fieldIdConstant);
        } else {
            codeBuilder.add("$N($N)", jtreesitterNode.methodGetChildrenByFieldName(), fieldNameConstant);
        }

        methodBuilder.addStatement(codeBuilder.build());
    }

    @Override
    public List<TypeSpec.Builder> generateJavaCode(TypeSpec.Builder enclosingTypeBuilder, CodeGenHelper codeGenHelper, String nodeFieldName) {
        enclosingTypeBuilder.addField(FieldSpec.builder(String.class, fieldNameConstant, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", fieldName)
            .addJavadoc("Field name $L", CodeGenHelper.createJavadocCodeTag(fieldName))
            .addJavadoc("\n\n@see #$N", getterName)
            .build()
        );

        if (codeGenHelper.generatesNumericIdConstants()) {
            var languageUtils = Objects.requireNonNull(codeGenHelper.languageUtilsConfig());
            var jtreesitter = codeGenHelper.jtreesitterConfig();
            var jtreesitterTreeCursor = jtreesitter.treeCursor();
            var fieldType = jtreesitter.language().numericIdType();

            enclosingTypeBuilder.addField(FieldSpec.builder(fieldType, fieldIdConstant, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.$N($N)", languageUtils.className(), languageUtils.methodGetFieldId(), fieldNameConstant)
                .addJavadoc("Field ID for field $L, assigned by tree-sitter.", CodeGenHelper.createJavadocCodeTag(fieldName))
                .addJavadoc("\n@see $T#$N", jtreesitterTreeCursor.className(), jtreesitterTreeCursor.methodGetCurrentFieldId())
                .addJavadoc("\n@see #$N", fieldNameConstant)
                .build()
            );
        }

        return super.generateJavaCode(enclosingTypeBuilder, codeGenHelper, nodeFieldName);
    }

    public static GenField create(String parentTypeName, GenRegularNodeType enclosingNodeType, String fieldName, ChildType fieldTypeRaw, NodeTypeLookup nodeTypeLookup, NameGenerator nameGenerator, Consumer<GenJavaType> additionalTypedNodeSubtypeCollector) {
        boolean multiple = fieldTypeRaw.multiple;
        boolean required = fieldTypeRaw.required;

        String nameConstant = nameGenerator.generateFieldNameConstant(parentTypeName, fieldName);
        String idConstant = nameGenerator.generateFieldIdConstant(parentTypeName, fieldName);
        String getterName = nameGenerator.generateFieldGetterName(parentTypeName, fieldName, multiple, required);

        var fieldTypeNameGenerator = new GenChildType.ChildTypeNameGenerator() {
            @Override
            public String generateInterfaceName(List<String> allChildTypes) {
                // For now ignore `allChildTypes` for name generation since field name probably suffices
                return nameGenerator.generateFieldTypesName(parentTypeName, fieldName);
            }

            @Override
            public String generateTokenClassName(List<String> tokenTypesNames) {
                return nameGenerator.generateFieldTokenTypeName(parentTypeName, fieldName, tokenTypesNames);
            }

            @Override
            public String generateTokenTypeConstantName(String tokenType, int index) {
                return nameGenerator.generateFieldTokenName(parentTypeName, fieldName, tokenType, index);
            }
        };
        var customMethodsProvider = new GenChildType.CustomMethodsProvider() {
            @Override
            public List<CustomMethodData> createCustomMethods(CodeGenHelper codeGenHelper, List<String> allChildTypes) {
                // For now ignore `allChildTypes` for obtaining custom methods since field name probably suffices
                return codeGenHelper.customMethodsForNodeFieldType(parentTypeName, fieldName);
            }
        };
        var fieldType = GenChildType.create(enclosingNodeType, fieldTypeRaw.types, fieldTypeNameGenerator, nodeTypeLookup, additionalTypedNodeSubtypeCollector, customMethodsProvider);

        return new GenField(fieldName, nameConstant, idConstant, getterName, fieldType, multiple, required);
    }
}
