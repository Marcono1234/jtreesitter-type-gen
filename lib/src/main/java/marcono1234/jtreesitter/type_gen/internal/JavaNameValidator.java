package marcono1234.jtreesitter.type_gen.internal;

import javax.lang.model.SourceVersion;

public class JavaNameValidator {
    public static final SourceVersion TARGET_VERSION = SourceVersion.RELEASE_21;

    private JavaNameValidator() {
    }

    /**
     * Checks if the name is a valid Java package name.
     *
     * @throws IllegalArgumentException if the name is invalid
     */
    public static void checkPackageName(String name) {
        // Permit 'default' package
        if (name.isEmpty()) {
            return;
        }

        if (!SourceVersion.isName(name, TARGET_VERSION)) {
            throw new IllegalArgumentException("Not a valid package name: " + name);
        }
    }

    /**
     * Checks if the name is a valid Java type name.
     *
     * @param isQualified whether the name includes the package name
     * @throws IllegalArgumentException if the name is invalid
     */
    public static void checkTypeName(String name, boolean isQualified) {
        if (!isQualified && name.contains(".")) {
            throw new IllegalArgumentException("Non-qualified type name must not contain '.': " + name);
        }

        if (!SourceVersion.isName(name, TARGET_VERSION)) {
            throw new IllegalArgumentException("Not a valid type name: " + name);
        }
    }

    /**
     * Checks if the name is a valid Java member name (field or method).
     * @throws IllegalArgumentException if the name is invalid
     */
    public static void checkMemberName(String name) {
        if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name, TARGET_VERSION)) {
            throw new IllegalArgumentException("Not a valid member name: " + name);
        }
    }
}
