package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.internal.JavaNameValidator;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Configuration related to a specific {@code node-types.json} file / language.
 *
 * <p><b>Tip:</b> Instead of using the constructor of this config class, prefer using {@link #builder()} to only specify
 * the data which differs from the default config, and to avoid experiencing breaking changes in case new config
 * parameters are added in the future.
 *
 * @param rootNodeTypeName
 *      Name of the root node type. This is usually the first {@code rules} entry in the {@code grammar.js}
 *      file. If provided, additional code will be generated to go from a jtreesitter {@code Tree} to a
 *      'typed tree'.
 *      <p>Starting with tree-sitter 0.24.0 this information is available in the {@code node-types.json} file
 *      (as {@code "root": true}), and must not be specified using this parameter here.
 * @param fallbackNodeTypeMapping
 *      Fallback mapping for unknown child node types. In some cases Tree-sitter can generate malformed
 *      {@code node-types.json} files where a child type uses the name of an 'alias' instead of the actual
 *      referenced node type (<a href="https://github.com/tree-sitter/tree-sitter/issues/1654">Tree-sitter issue</a>).
 *      This causes a code generation failure because that type name cannot be resolved. To work around this issue,
 *      this fallback mapping can be used to map these alias type names to the actual node type they are referencing.
 * @param languageProviderConfig
 *      Configuration for how the generated code can obtain a tree-sitter {@code Language} instance for the
 *      language represented by the {@code node-types.json} file. If provided, additional code will be,
 *      generated exposing for example numeric node and field IDs, and validating at runtime that the names
 *      in the {@code node-types.json} file actually match the loaded language.
 * @param expectedLanguageVersion
 *      Expected version number the loaded language should have ({@link LanguageVersion#patch() patch} version
 *      deviations are permitted). Specifying the expected version ensures that the loaded language is compatible
 *      with the generated code by generating additional validation code. Incompatibilities could otherwise cause
 *      exceptions or incorrect behavior.<br/>
 *      The language / grammar version is specified as {@code metadata.version} in the {@code tree-sitter.json}
 *      file of a grammar. See also the <a href="https://tree-sitter.github.io/tree-sitter/cli/version.html">Tree-sitter documentation</a>
 *      for how to set the version.
 *      <p>Can only be used if {@code languageProviderConfig} is provided.
 */
// Note: Have to adjust this once multiple root nodes (https://github.com/tree-sitter/tree-sitter/issues/870) or
//   root nodes selected at runtime (https://github.com/tree-sitter/tree-sitter/issues/711) will be possible
public record LanguageConfig(
    Optional<String> rootNodeTypeName,
    Map<String, String> fallbackNodeTypeMapping,
    Optional<LanguageProviderConfig> languageProviderConfig,
    Optional<LanguageVersion> expectedLanguageVersion
) {
    public LanguageConfig {
        Objects.requireNonNull(rootNodeTypeName);
        fallbackNodeTypeMapping = Map.copyOf(fallbackNodeTypeMapping);
        Objects.requireNonNull(languageProviderConfig);
        Objects.requireNonNull(expectedLanguageVersion);

        if (expectedLanguageVersion.isPresent() && languageProviderConfig.isEmpty()) {
            throw new IllegalArgumentException("Must specify language provider when using expectedLanguageVersion");
        }
    }

    /**
     * Creates a new builder for {@link LanguageConfig}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LanguageConfig}. The config instance can be created using {@link #build()}.
     *
     * <p>All builder methods modify the builder instance; the returned builder can be ignored and only exists as
     * convenience to allow method call chaining.
     *
     * <h2>Defaults</h2>
     * <ul>
     * <li>{@link LanguageConfig#rootNodeTypeName() rootNodeTypeName}: none
     * <li>{@link LanguageConfig#fallbackNodeTypeMapping() fallbackNodeTypeMapping}: empty map
     * <li>{@link LanguageConfig#languageProviderConfig() languageProviderConfig}: none
     * <li>{@link LanguageConfig#expectedLanguageVersion() expectedLanguageVersion}: none
     * </ul>
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder {
        private Builder() {
        }

        private Optional<String> rootNodeTypeName = Optional.empty();

        /**
         * @see LanguageConfig#rootNodeTypeName()
         */
        public Builder rootNodeTypeName(String rootNodeTypeName) {
            this.rootNodeTypeName = Optional.of(rootNodeTypeName);
            return this;
        }

        private Map<String, String> fallbackNodeTypeMapping = Map.of();

        /**
         * @see LanguageConfig#fallbackNodeTypeMapping()
         */
        public Builder fallbackNodeTypeMapping(Map<String, String> fallbackNodeTypeMapping) {
            this.fallbackNodeTypeMapping = Map.copyOf(fallbackNodeTypeMapping);
            return this;
        }

        private Optional<LanguageProviderConfig> languageProviderConfig = Optional.empty();

        /**
         * @see LanguageConfig#languageProviderConfig()
         */
        public Builder languageProviderConfig(LanguageProviderConfig languageProviderConfig) {
            this.languageProviderConfig = Optional.of(languageProviderConfig);
            return this;
        }

        private Optional<LanguageVersion> expectedLanguageVersion = Optional.empty();

        /**
         * @see LanguageConfig#expectedLanguageVersion()
         */
        public Builder expectedLanguageVersion(LanguageVersion expectedLanguageVersion) {
            this.expectedLanguageVersion = Optional.of(expectedLanguageVersion);
            return this;
        }

        /**
         * Applies the consumer to this builder and afterwards returns this builder.
         *
         * <p>This allows using a separate method to apply configuration, without interrupting the method call chain.
         */
        public Builder apply(Consumer<? super Builder> consumer) {
            consumer.accept(this);
            return this;
        }

        public LanguageConfig build() {
            return new LanguageConfig(
                rootNodeTypeName,
                fallbackNodeTypeMapping,
                languageProviderConfig,
                expectedLanguageVersion
            );
        }
    }

    /**
     * Represents information about how the generated code can obtain a tree-sitter {@code Language} instance
     * for the language the {@code node-types.json} belongs to.
     */
    // Note: If in the future the numeric node and field IDs are stored in `node-types.json` (see
    // https://github.com/tree-sitter/tree-sitter/issues/1475) it might not be necessary anymore to obtain
    // a `Language` object; but might still be useful for `node-types.json` generated by older versions, and
    // by looking up IDs at runtime it also verifies that the type and field names match those of the loaded
    // language and that it is not out-of-sync with `node-types.json`
    public sealed interface LanguageProviderConfig {
        /**
         * The type which declares the field or method which provides the {@code Language} instance.
         */
        TypeName declaringType();

        /**
         * Public static field
         */
        record Field(TypeName declaringType, String fieldName) implements LanguageProviderConfig {
            public Field {
                Objects.requireNonNull(declaringType);
                JavaNameValidator.checkMemberName(fieldName);
            }
        }

        /**
         * Public static no-args method
         */
        record Method(TypeName declaringType, String methodName) implements LanguageProviderConfig {
            public Method {
                Objects.requireNonNull(declaringType);
                JavaNameValidator.checkMemberName(methodName);
            }
        }

        /**
         * Obtains a language provider config from a string representation:
         * <ul>
         *     <li>{@code org.example.MyClass#field}: {@link Field}
         *     <li>{@code org.example.MyClass#method()}: {@link Method}
         * </ul>
         */
        static LanguageProviderConfig fromString(String string) {
            int memberSeparatorIndex = string.indexOf('#');
            if (memberSeparatorIndex == -1) {
                throw new IllegalArgumentException("Does not contain member name separator '#'");
            }

            TypeName type = TypeName.fromQualifiedName(string.substring(0, memberSeparatorIndex));
            String memberName = string.substring(memberSeparatorIndex + 1);

            if (memberName.endsWith("()")) {
                memberName = memberName.substring(0, memberName.length() - "()".length());
                return new Method(type, memberName);
            } else if (memberName.contains("(") || memberName.contains(")")) {
                throw new IllegalArgumentException("Only no-args method is supported");
            } else {
                return new Field(type, memberName);
            }
        }
    }

    /**
     * Tree-sitter language / grammar version, in the format {@code <major>.<minor>.<patch>}.
     */
    public record LanguageVersion(int major, int minor, int patch) {
        public LanguageVersion {
            if (major < 0 || minor < 0 || patch < 0) {
                throw new IllegalArgumentException("Version number must not be < 0");
            }
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }

        private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

        /**
         * Parses a version from the string representation {@code <major>.<minor>.<patch>}.
         */
        public static LanguageVersion fromString(String string) {
            var matcher = VERSION_PATTERN.matcher(string);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Version should have format '<major>.<minor>.<patch>', but is: " + string);
            }

            return new LanguageVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
            );
        }
    }
}
