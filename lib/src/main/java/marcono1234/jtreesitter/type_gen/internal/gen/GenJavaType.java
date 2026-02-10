package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.ClassName;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CommonMethodsGenerator;

import java.util.List;

/**
 * {@summary A generated Java type (class, interface, ...).}
 */
public interface GenJavaType extends CommonMethodsGenerator.Subtype {
    ClassName createJavaTypeName(CodeGenHelper codeGenHelper);

    @Override
    List<GeneratedMethod> getGeneratedMethods(CodeGenHelper codeGenHelper);
}
