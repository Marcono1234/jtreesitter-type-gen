package marcono1234.jtreesitter.type_gen;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenConfigTest {
    private static final String DEFAULT_PACKAGE_NAME = "org.example";
    private static final String DEFAULT_NON_EMPTY_NAME = "NonEmpty";
    private static final CodeGenConfig.ChildTypeAsTopLevel DEFAULT_CHILD_AS_TOP_LEVEL = CodeGenConfig.ChildTypeAsTopLevel.AS_NEEDED;
    private static final NameGenerator DEFAULT_NAME_GENERATOR = NameGenerator.createDefault(NameGenerator.TokenNameGenerator.AUTOMATIC);

    @Test
    void packageName() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            "-invalid",
            Optional.empty(),
            Optional.empty(),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Not a valid package name: -invalid", e.getMessage());
    }

    @Test
    void nullMarkedPackageAnnotationTypeName() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.of(TypeName.JSPECIFY_NULLMARKED_ANNOTATION),
            DEFAULT_NON_EMPTY_NAME,
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Cannot use null-marked annotation when nullable annotation is not specified", e.getMessage());
    }

    @Test
    void nonEmptyTypeName() {
        var e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            "-invalid",
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Not a valid type name: -invalid", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> new CodeGenConfig(
            DEFAULT_PACKAGE_NAME,
            Optional.empty(),
            Optional.empty(),
            // Should also fail for qualified name because unqualified name is expected
            "qualified.Name",
            DEFAULT_CHILD_AS_TOP_LEVEL,
            Optional.empty(),
            DEFAULT_NAME_GENERATOR,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
        assertEquals("Non-qualified type name must not contain '.': qualified.Name", e.getMessage());
    }
}
