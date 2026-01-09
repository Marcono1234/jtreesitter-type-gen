package marcono1234.jtreesitter.type_gen.internal;

import java.util.Locale;

// TODO: Maybe expose these utility methods publicly? They might be useful to users who implement their own custom
//    name generator, and it would allow the CLI to use them without `module-info` having to export internal packages to CLI
/**
 * Helper class for generating Java type and member names.
 */
public class JavaNameGeneratorHelper {
    private JavaNameGeneratorHelper() {
    }

    /**
     * Converts {@code snake_case} to {@code camelCase}.
     */
    public static String convertSnakeToCamelCase(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty string is not supported");
        }

        StringBuilder stringBuilder = new StringBuilder(s.length());
        int nextStartIndex = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '_') {
                stringBuilder.append(s, nextStartIndex, i);
                i++;
                if (i < s.length()) {
                    stringBuilder.append(Character.toUpperCase(s.charAt(i)));
                }
                nextStartIndex = i + 1; // skip uppercased char
            }
        }

        // Add remainder of string
        if (nextStartIndex < s.length()) {
            stringBuilder.append(s, nextStartIndex, s.length());
        }
        return stringBuilder.toString();
    }

    /**
     * Converts the string to a Java constant name, e.g. {@code MY_CONSTANT}.
     */
    public static String convertToConstantName(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty string is not supported");
        }

        StringBuilder stringBuilder = new StringBuilder(s.length());
        int nextStartIndex = 0;
        boolean wasLower = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean isLower = Character.isLowerCase(c);
            // Check for switch from lower to upper (respectively lower letter to non-letter)
            if (wasLower && !isLower) {
                stringBuilder.append(s, nextStartIndex, i);
                stringBuilder.append('_');

                if (c == '_' || c == '-') {
                    i++;
                }
                nextStartIndex = i;
            }
            wasLower = isLower;
        }

        // Add remainder of string
        stringBuilder.append(s, nextStartIndex, s.length());
        return stringBuilder.toString().toUpperCase(Locale.ROOT);
    }

    public static String upperFirstChar(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty string is not supported");
        }

        // For simplicity just use first char, don't consider supplementary code points or special Unicode
        // case conversion rules here
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String lowerFirstChar(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty string is not supported");
        }

        // For simplicity just use first char, don't consider supplementary code points or special Unicode
        // case conversion rules here
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Converts a node type name, as defined in the grammar, to a Java uppercased camel case name.
     */
    public static String typeNameToUpperCamel(String typeName) {
        // Remove leading '_', for hidden types, which might appear in the `node-types.json` nonetheless,
        // in case they are a supertype (see for example tree-sitter-java's `_literal`)
        if (typeName.startsWith("_")) {
            typeName = typeName.substring(1);
        }

        return upperFirstChar(convertSnakeToCamelCase(typeName));
    }
}
