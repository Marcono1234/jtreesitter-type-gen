package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageProviderConfig;
import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageVersion;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LanguageConfigTest {
    @Test
    void testNew() {
        var config = new LanguageConfig(Optional.empty(), Map.of(), Optional.empty(), Optional.empty());
        assertEquals(Optional.empty(), config.rootNodeTypeName());
        assertEquals(Optional.empty(), config.languageProviderConfig());
        assertEquals(Optional.empty(), config.expectedLanguageVersion());

        var languageProvider = LanguageProviderConfig.fromString("MyClass#field");
        var languageVersion = new LanguageVersion(1, 2, 3);
        config = new LanguageConfig(
            Optional.of("root"),
            Map.of(),
            Optional.of(languageProvider),
            Optional.of(languageVersion)
        );
        assertEquals(Optional.of("root"), config.rootNodeTypeName());
        assertEquals(Optional.of(languageProvider), config.languageProviderConfig());
        assertEquals(Optional.of(languageVersion), config.expectedLanguageVersion());

        var e = assertThrows(IllegalArgumentException.class, () -> new LanguageConfig(Optional.empty(), Map.of(), Optional.empty(), Optional.of(languageVersion)));
        assertEquals("Must specify language provider when using expectedLanguageVersion", e.getMessage());
    }

    @Nested
    class LanguageProviderConfigTest {
        @Test
        void field() {
            var provider = LanguageProviderConfig.fromString("MyClass#field");
            assertEquals(new LanguageProviderConfig.Field(TypeName.fromQualifiedName("MyClass"), "field"), provider);

            provider = LanguageProviderConfig.fromString("my.custom_package.MyClass#field$");
            assertEquals(new LanguageProviderConfig.Field(TypeName.fromQualifiedName("my.custom_package.MyClass"), "field$"), provider);
        }

        @Test
        void method() {
            var provider = LanguageProviderConfig.fromString("MyClass#method()");
            assertEquals(new LanguageProviderConfig.Method(TypeName.fromQualifiedName("MyClass"), "method"), provider);

            provider = LanguageProviderConfig.fromString("my.custom_package.MyClass#method$()");
            assertEquals(new LanguageProviderConfig.Method(TypeName.fromQualifiedName("my.custom_package.MyClass"), "method$"), provider);
        }

        @Test
        void invalid() {
            var e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("test"));
            assertEquals("Does not contain member name separator '#'", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("#"));
            assertEquals("Not a valid type name: ", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("MyClass#"));
            assertEquals("Not a valid member name: ", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("#test"));
            assertEquals("Not a valid type name: ", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("MyClass#test("));
            assertEquals("Only no-args method is supported", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("MyClass#test)"));
            assertEquals("Only no-args method is supported", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("tMyClass#est(int)"));
            assertEquals("Only no-args method is supported", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("MyClass#test()()"));
            assertEquals("Not a valid member name: test()", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> LanguageProviderConfig.fromString("MyClass#test[]()"));
            assertEquals("Not a valid member name: test[]", e.getMessage());
        }
    }

    @Nested
    class LanguageVersionTest {
        @Test
        void constructor() {
            var version = new LanguageVersion(1, 2, 3);
            assertEquals(1, version.major());
            assertEquals(2, version.minor());
            assertEquals(3, version.patch());

            version = new LanguageVersion(0, 0, 0);
            assertEquals(0, version.major());
            assertEquals(0, version.minor());
            assertEquals(0, version.patch());

            var e = assertThrows(IllegalArgumentException.class, () -> new LanguageVersion(-1, 0, 0));
            assertEquals("Version number must not be < 0", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> new LanguageVersion(0, -1, 0));
            assertEquals("Version number must not be < 0", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> new LanguageVersion(0, 0, -1));
            assertEquals("Version number must not be < 0", e.getMessage());
        }

        @Test
        void fromString_Valid() {
            var version = LanguageVersion.fromString("1.2.3");
            assertEquals(1, version.major());
            assertEquals(2, version.minor());
            assertEquals(3, version.patch());

            version = LanguageVersion.fromString("123.234.345");
            assertEquals(123, version.major());
            assertEquals(234, version.minor());
            assertEquals(345, version.patch());

            version = LanguageVersion.fromString("0.0.0");
            assertEquals(0, version.major());
            assertEquals(0, version.minor());
            assertEquals(0, version.patch());

            version = LanguageVersion.fromString("0.1.2");
            assertEquals(0, version.major());
            assertEquals(1, version.minor());
            assertEquals(2, version.patch());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "",
            "..",
            "1",
            "1.2",
            "1.2.",
            ".2.3",
            "a.2.3",
            "1.2.3a",
            "-1.2.3",
            // Should not match non-ASCII digits
            "\u0661.\u0661.\u0661",
        })
        void fromString_Invalid(String version) {
            var e = assertThrows(IllegalArgumentException.class, () -> LanguageVersion.fromString(version));
            assertEquals("Version should have format '<major>.<minor>.<patch>', but is: " + version, e.getMessage());
        }

        @Test
        void testToString() {
            var version = new LanguageVersion(1, 2, 3);
            assertEquals("1.2.3", version.toString());
        }
    }
}
