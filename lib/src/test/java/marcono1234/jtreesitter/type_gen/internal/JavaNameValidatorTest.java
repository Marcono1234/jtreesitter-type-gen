package marcono1234.jtreesitter.type_gen.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class JavaNameValidatorTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a",
        "a.b",
        "first.second.third",
    })
    void checkPackageName(String name) {
        assertDoesNotThrow(() -> JavaNameValidator.checkPackageName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/",
        "a/b",
        "a\\b",
        "a:b",
        ".",
        "..",
        "a.",
        ".b",
        "a..b",
        "a b",
    })
    void checkPackageName_Invalid(String name) {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameValidator.checkPackageName(name));
        assertEquals("Not a valid package name: " + name, e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "A",
        "a.B",
        "a.B$C",
        "a.B$C$D",
    })
    void checkTypeName_Qualified(String name) {
        assertDoesNotThrow(() -> JavaNameValidator.checkTypeName(name, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a.",
        ".b",
        "a..b.C",
        "a.b.C.",
        "/",
        "a/b",
        "a/b$C",
        "a b",
        "a{}",
    })
    void checkTypeName_Qualified_Invalid(String name) {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameValidator.checkTypeName(name, true));
        assertEquals("Not a valid type name: " + name, e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a",
        "A$B",
        "First$Second$Third",
    })
    void checkTypeName_NonQualified(String name) {
        assertDoesNotThrow(() -> JavaNameValidator.checkTypeName(name, false));
    }

    @Test
    void checkTypeName_NonQualified_Invalid() {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameValidator.checkTypeName("", false));
        assertEquals("Not a valid type name: ", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> JavaNameValidator.checkTypeName("a.B", false));
        assertEquals("Non-qualified type name must not contain '.': a.B", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a",
        "a$b",
        "a_",
        "a_b",
        "_a",
    })
    void checkMemberName(String name) {
        assertDoesNotThrow(() -> JavaNameValidator.checkMemberName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "a/b",
        "a\\b",
        "a:b",
        "a.b",
        ".",
        "..",
        "a.",
        ".b",
        "a..b",
        "a b",
        "a()",
        "a{}",
    })
    void checkMemberName_Invalid(String name) {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameValidator.checkMemberName(name));
        assertEquals("Not a valid member name: " + name, e.getMessage());
    }
}
