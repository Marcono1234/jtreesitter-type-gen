package marcono1234.jtreesitter.type_gen;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypeNameTest {
    @Test
    void fromQualifiedName() {
        var typeName = TypeName.fromQualifiedName("MyClass");
        assertEquals("", typeName.packageName());
        assertEquals("MyClass", typeName.name());

        typeName = TypeName.fromQualifiedName("my.custom_package.MyClass$Nested");
        assertEquals("my.custom_package", typeName.packageName());
        assertEquals("MyClass$Nested", typeName.name());
    }

    @Test
    void fromQualifiedName_Invalid() {
        var e = assertThrows(IllegalArgumentException.class, () -> TypeName.fromQualifiedName(""));
        assertEquals("Not a valid type name: ", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> TypeName.fromQualifiedName(".MyClass"));
        assertEquals("Invalid leading dot: .MyClass", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> TypeName.fromQualifiedName("mypackage."));
        assertEquals("Not a valid type name: ", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> TypeName.fromQualifiedName("com.mypackage."));
        assertEquals("Not a valid type name: ", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> TypeName.fromQualifiedName(".mypackage.MyClass"));
        assertEquals("Not a valid package name: .mypackage", e.getMessage());
    }

    @Test
    void fromClass() {
        var typeName = TypeName.fromClass(String.class);
        assertEquals("java.lang", typeName.packageName());
        assertEquals("String", typeName.name());

        typeName = TypeName.fromClass(Map.Entry.class);
        assertEquals("java.util", typeName.packageName());
        assertEquals("Map$Entry", typeName.name());

        // Using primitive types is not supported
        var e = assertThrows(IllegalArgumentException.class, () -> TypeName.fromClass(int.class));
        assertEquals("Not a valid type name: int", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> TypeName.fromClass(String[].class));
        assertEquals("Array classes are not supported", e.getMessage());
    }
}
