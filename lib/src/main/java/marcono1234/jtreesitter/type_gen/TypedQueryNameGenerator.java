package marcono1234.jtreesitter.type_gen;

import java.util.List;
import java.util.Objects;

// TODO: Also wrap in 'validating name generator', similar to how regular `NameGenerator` is used

public interface TypedQueryNameGenerator {
    String generateJavaTypeName(String typeName);
    String generateBuilderMethodName(String typeName);

    String generateAsSubtypeMethod(String typeName, String supertypeName);

    // Note: For consistency with `NameGenerator` should maybe include parameter `List<String> fieldTypesNames`?
    //   However, not sure how useful that really is, and currently the typed query generator cannot access that information
    String generateWithFieldMethodName(String parentTypeName, String fieldName);
    String generateWithoutFieldMethodName(String parentTypeName, String fieldName);

    String generateFieldTokenMethodName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames);

    /**
     * Creates a default typed query name generator based partially on a {@link NameGenerator},
     * which is suitable for most use cases.
     */
    static TypedQueryNameGenerator createDefault(NameGenerator nameGenerator) {
        Objects.requireNonNull(nameGenerator);

        return new TypedQueryNameGenerator() {
            private String upperFirstChar(String s) {
                if (s.isEmpty()) {
                    throw new IllegalArgumentException("Empty string is not supported");
                }

                // For simplicity just use first char, don't consider supplementary code points or special Unicode
                // case conversion rules here
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
            }

            private String lowerFirstChar(String s) {
                if (s.isEmpty()) {
                    throw new IllegalArgumentException("Empty string is not supported");
                }

                // For simplicity just use first char, don't consider supplementary code points or special Unicode
                // case conversion rules here
                return Character.toLowerCase(s.charAt(0)) + s.substring(1);
            }

            @Override
            public String generateJavaTypeName(String typeName) {
                return "Q" + nameGenerator.generateJavaTypeName(typeName);
            }

            @Override
            public String generateBuilderMethodName(String typeName) {
                // Just lowercase the first char so that the type name is suitable as method name;
                // this assumes that the Java type name starts with a unique prefix
                return lowerFirstChar(nameGenerator.generateJavaTypeName(typeName));
            }

            @Override
            public String generateAsSubtypeMethod(String typeName, String supertypeName) {
                return "asSubtypeOf" + nameGenerator.generateJavaTypeName(supertypeName);
            }

            @Override
            public String generateWithFieldMethodName(String parentTypeName, String fieldName) {
                return "withField" + upperFirstChar(fieldName);
            }

            @Override
            public String generateWithoutFieldMethodName(String parentTypeName, String fieldName) {
                return "withoutField" + upperFirstChar(fieldName);
            }

            @Override
            public String generateFieldTokenMethodName(String parentTypeName, String fieldName, List<String> tokenFieldTypesNames) {
                return "fieldToken" + upperFirstChar(fieldName);
            }
        };
    }
}
