package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypeBuilderWithNameTest {
    private static TypeBuilderWithName typeBuilder(ClassName typeName) {
        return new TypeBuilderWithName(TypeSpec.classBuilder(typeName), typeName);
    }

    @Test
    void isTopLevel() {
        assertTrue(typeBuilder(ClassName.get(Map.class)).isTopLevel());
        assertFalse(typeBuilder(ClassName.get(Map.Entry.class)).isTopLevel());
    }
}
