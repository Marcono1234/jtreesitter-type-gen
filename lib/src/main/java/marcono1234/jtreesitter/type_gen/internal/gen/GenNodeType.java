package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import java.util.List;
import java.util.Set;

/**
 * Node type for which a top-level Java type should be generated.
 */
public sealed interface GenNodeType extends GenJavaType permits GenRegularNodeType, GenSupertypeNodeType {
    /**
     * Gets the type name defined in the grammar.
     */
    String getTypeName();

    /**
     * Gets the Java class name for the generated class.
     */
    String getJavaName();

    @Override
    default ClassName createJavaTypeName(CodeGenHelper codeGenHelper) {
        return codeGenHelper.createOwnClassName(getJavaName());
    }

    /**
     * Returns whether this type directly (e.g. as subtype of a supertype node) or indirectly (e.g. through child)
     * refers to the type. To avoid infinite recursion, already seen types are tracked in {@code seenTypes}.
     */
    boolean refersToType(GenRegularNodeType type, Set<GenRegularNodeType> seenTypes);

    /**
     * Adds a Java interface which should be implemented by this type.
     */
    void addInterfaceToImplement(GenJavaInterface i);

    List<JavaFile> generateJavaCode(CodeGenHelper codeGenHelper);
}
