package marcono1234.jtreesitter.type_gen.cli.json_configs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import marcono1234.jtreesitter.type_gen.CustomMethodsProvider;
import marcono1234.jtreesitter.type_gen.JavaType;
import marcono1234.jtreesitter.type_gen.JavaTypeVariable;
import marcono1234.jtreesitter.type_gen.TypeName;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;

import java.nio.file.Path;
import java.util.*;

/**
 * JSON config for custom methods to be added to the generated code.
 *
 * @see marcono1234.jtreesitter.type_gen.CustomMethodsProvider
 */
// Note: `@JsonProperty(required = true)` has currently no effect, see https://github.com/FasterXML/jackson-databind/issues/230
public class CustomMethodsConfig {
    public static class MethodConfig {
        @JsonProperty(value = "name", required = true)
        public String name;

        @JsonProperty("type-variables")
        @Nullable
        public List<TypeVariableConfig> typeVariables;

        @JsonProperty("parameters")
        @Nullable
        public SequencedMap<String, JavaType> parameters;

        @JsonProperty("return-type")
        @Nullable
        public JavaType returnType;

        @JsonProperty("javadoc")
        @Nullable
        public String javadoc;

        @JsonProperty(value = "receiver", required = true)
        public Receiver receiver;

        @JsonIgnore
        public CustomMethodsProvider.MethodData asMethodData() {
            return new CustomMethodsProvider.MethodData(
                name,
                typeVariables == null ? List.of()
                    : typeVariables.stream().map(v -> new JavaTypeVariable(v.name, v.bounds != null ? v.bounds : List.of())).toList(),
                parameters != null ? parameters : new LinkedHashMap<>(),
                Optional.ofNullable(returnType),
                Optional.ofNullable(javadoc),
                receiver.type(),
                receiver.methodName(),
                // Don't support custom arguments for now
                List.of()
            );
        }

        public static List<CustomMethodsProvider.MethodData> asMethodsData(@Nullable List<MethodConfig> configs) {
            if (configs == null) {
                return List.of();
            }
            return configs.stream().map(MethodConfig::asMethodData).toList();
        }
    }

    @JsonDeserialize(using = ReceiverDeserializer.class)
    public record Receiver(TypeName type, String methodName) {
    }

    public static class TypeVariableConfig {
        @JsonProperty(value = "name", required = true)
        public String name;

        @JsonProperty("bounds")
        @Nullable
        public List<JavaType> bounds;
    }

    @JsonProperty("typed-tree")
    @Nullable
    public List<MethodConfig> typedTree;

    @JsonProperty("typed-node")
    @Nullable
    public List<MethodConfig> typedNode;

    @JsonProperty("node-types")
    @Nullable
    public Map<String, List<MethodConfig>> nodeTypes;

    // Note: For now don't support custom methods for children and field types from the CLI since that
    // is probably not that often used


    private static final ObjectMapper mapper = ObjectMappers.verboseMapperBuilder()
        .addModule(new SimpleModule()
            .addDeserializer(JavaType.class, new JavaTypeDeserializer())
            .addDeserializer(TypeName.class, new TypeNameDeserializer())
        )
        .build();

    public static CustomMethodsConfig readFromFile(Path file) throws JacksonException {
        return mapper.readValue(file, CustomMethodsConfig.class);
    }

    /** Strict deserializer which only accepts a JSON string and performs no coercion. */
    private static abstract class FromStringDeserializer<T> extends StdDeserializer<T> {
        protected FromStringDeserializer(Class<T> vc) {
            super(vc);
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            if (!p.hasToken(JsonToken.VALUE_STRING)) {
                ctxt.reportWrongTokenException(handledType(), p.currentToken(), null);
            }

            String s = p.getString();
            try {
                return fromString(s, p);
            } catch (JacksonException e) {
                throw e;
            } catch (RuntimeException e) {
                var thrown = inputException(p, "Failed deserializing value as " + handledType().getSimpleName() + ": " + s);
                thrown.initCause(e);
                throw thrown;
            }
        }

        protected MismatchedInputException inputException(JsonParser p, String message) {
            return MismatchedInputException.from(p, handledType(), message);
        }

        /**
         * @param p should only be used for constructing the {@link MismatchedInputException}
         */
        protected abstract T fromString(String s, JsonParser p) throws MismatchedInputException;
    }

    private static class JavaTypeDeserializer extends FromStringDeserializer<JavaType> {
        public JavaTypeDeserializer() {
            super(JavaType.class);
        }

        @Override
        protected JavaType fromString(String s, JsonParser p) {
            return JavaType.fromTypeString(s);
        }
    }

    private static class TypeNameDeserializer extends FromStringDeserializer<TypeName> {
        public TypeNameDeserializer() {
            super(TypeName.class);
        }

        @Override
        protected TypeName fromString(String s, JsonParser p) {
            return TypeName.fromQualifiedName(s);
        }
    }

    private static class ReceiverDeserializer extends FromStringDeserializer<Receiver> {
        public ReceiverDeserializer() {
            super(Receiver.class);
        }

        @Override
        protected Receiver fromString(String s, JsonParser p) throws MismatchedInputException {
            int sepIndex = s.indexOf('#');
            if (sepIndex == -1) {
                throw inputException(p, "Missing '#': " + s);
            }

            var typeName = TypeName.fromQualifiedName(s.substring(0, sepIndex));
            var methodName = s.substring(sepIndex + 1);
            return new Receiver(typeName, methodName);
        }
    }
}
