package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import marcono1234.jtreesitter.type_gen.TypeName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenHelperTest {
    @Test
    void createClassName() {
        var className = CodeGenHelper.createClassName(new TypeName("", "MyClass"));
        assertEquals("", className.packageName());
        assertEquals("MyClass", className.simpleName());
        assertEquals(List.of("MyClass"), className.simpleNames());

        className = CodeGenHelper.createClassName(new TypeName("mypackage.nested", "MyClass"));
        assertEquals("mypackage.nested", className.packageName());
        assertEquals("MyClass", className.simpleName());
        assertEquals(List.of("MyClass"), className.simpleNames());

        className = CodeGenHelper.createClassName(new TypeName("mypackage", "MyClass$Nested1$Nested2"));
        assertEquals("mypackage", className.packageName());
        assertEquals("Nested2", className.simpleName());
        assertEquals(List.of("MyClass", "Nested1", "Nested2"), className.simpleNames());
    }
}
