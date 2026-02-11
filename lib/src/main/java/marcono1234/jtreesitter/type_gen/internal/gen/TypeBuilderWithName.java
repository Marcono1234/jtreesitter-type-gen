package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;

/**
 * {@link TypeSpec.Builder} combined with the corresponding {@link ClassName}.
 *
 * <p>(Because {@link TypeSpec.Builder} does not offer a way to retrieve the type name until the type is built,
 * and even then {@link TypeSpec} only stores the simple name, not the fully qualified one.)
 */
public record TypeBuilderWithName(TypeSpec.Builder typeBuilder, ClassName typeName) {
    public boolean isTopLevel() {
        return typeName.enclosingClassName() == null;
    }
}
