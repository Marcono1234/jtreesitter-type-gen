package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.CustomMethodsProvider;
import marcono1234.jtreesitter.type_gen.JavaLiteral;
import marcono1234.jtreesitter.type_gen.JavaType;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;

/**
 * Internal representation of {@link CustomMethodsProvider.MethodData}.
 */
public record CustomMethodData(
    String name,
    List<TypeVariableName> typeVariables,
    List<ParameterSpec> parameters,
    @Nullable TypeName returnType,
    @Nullable String javadoc,
    ClassName receiverType,
    String receiverMethod,
    List<JavaLiteral> additionalArguments
) {
    private static TypeName getJavaPoetType(JavaType javaType) {
        return switch (javaType) {
            case JavaTypeImpl t -> t.type();
        };
    }

    public static CustomMethodData fromUserConfig(CustomMethodsProvider.MethodData userMethodData) {
        return new CustomMethodData(
            userMethodData.name(),
            userMethodData.typeVariables().stream()
                .map(typeVar -> TypeVariableName.get(typeVar.name(), typeVar.bounds().stream()
                    .map(CustomMethodData::getJavaPoetType)
                    .toArray(TypeName[]::new)
                ))
                .toList(),
            userMethodData.parameters().entrySet().stream()
                .map(entry -> ParameterSpec.builder(getJavaPoetType(entry.getValue()), entry.getKey()).build())
                .toList(),
            userMethodData.returnType().map(CustomMethodData::getJavaPoetType).orElse(null),
            userMethodData.javadoc().orElse(null),
            CodeGenHelper.createClassName(userMethodData.receiverType()),
            userMethodData.receiverMethod(),
            userMethodData.additionalArguments()
        );
    }

    static CodeBlock emitLiteral(JavaLiteral literal) {
        return switch (literal) {
            case JavaLiteral.Boolean l -> CodeBlock.of("$L", l.value());
            case JavaLiteral.Integer l -> CodeBlock.of("$L", l.value());
            case JavaLiteral.Long l -> CodeBlock.of("$LL", l.value());
            case JavaLiteral.Double l -> {
                double value = l.value();
                if (Double.isNaN(value)) {
                    yield CodeBlock.of("$T.NaN", Double.class);
                } else if (Double.isInfinite(value)) {
                    yield CodeBlock.of("$T.$N", Double.class, value < 0 ? "NEGATIVE_INFINITY" : "POSITIVE_INFINITY");
                }
                yield CodeBlock.of("$L", l.value());
            }
            case JavaLiteral.String l -> CodeBlock.of("$S", l.value());
        };
    }

    /**
     * @param forInterface
     *      whether the type to which the method will be added is a Java interface ({@code true}) or a Java class ({@code false})
     */
    public MethodSpec generateMethod(boolean forInterface) {
        var methodBuilder = MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC);

        var statementBuilder = CodeBlock.builder();

        if (forInterface) {
            methodBuilder.addModifiers(Modifier.DEFAULT);
        }
        if (returnType != null) {
            methodBuilder.returns(returnType);
            statementBuilder.add("return ");
        }
        if (javadoc != null) {
            methodBuilder.addJavadoc(javadoc);
        }

        methodBuilder.addTypeVariables(typeVariables);
        methodBuilder.addParameters(parameters);

        statementBuilder.add("$T.$N(this", receiverType, receiverMethod);

        for (var parameter : parameters) {
            statementBuilder.add(", $N", parameter);
        }

        for (var additionalArgument : additionalArguments) {
            statementBuilder.add(", ").add(emitLiteral(additionalArgument));
        }

        statementBuilder.add(")");
        methodBuilder.addStatement(statementBuilder.build());

        return methodBuilder.build();
    }

    public GeneratedMethod asGeneratedMethod() {
        return new GeneratedMethod(
            GeneratedMethod.SimpleKind.CUSTOM_METHOD,
            new GeneratedMethod.Signature(
                name,
                typeVariables,
                parameters.stream().map(GeneratedMethod.Signature.Parameter::fromParamSpec).toList()
            ),
            returnType != null
                // Use empty resolver because cannot easily know which types are used and which supertypes they have
                // Could maybe in the future add resolving of standard Java types (e.g. List -> Collection) but maybe not worth
                // it at the moment
                ? new GeneratedMethod.ReturnType(returnType, GeneratedMethod.SupertypesResolver.EMPTY)
                : null
        );
    }

    // For Javadoc can simplify the type a bit, e.g. omit type arguments of parameterized type
    static TypeName typeNameForJavadoc(TypeName typeName) {
        typeName = typeName.withoutAnnotations();

        return switch (typeName) {
            case ParameterizedTypeName p -> p.rawType();
            case ArrayTypeName a -> ArrayTypeName.of(typeNameForJavadoc(a.componentType()));
            case TypeVariableName v -> TypeVariableName.get(v.name());
            // Wildcard type handling here should be unreachable because it can only appear nested in a parameterized type,
            // and for that the type arguments are ignored
            case WildcardTypeName ignored -> throw new AssertionError("unreachable");
            default -> typeName;
        };
    }

    public CodeBlock generateJavadocLink() {
        var builder = CodeBlock.builder()
            .add("{@link #$N(", name);
        // Use fully qualified method with parameters in case multiple methods with same name (but different parameters) exist
        //   or method has same name as default generated method (but different parameters)
        for (int i = 0; i < parameters.size(); i++) {
            builder.add("$T", typeNameForJavadoc(parameters.get(i).type()));

            if (i < parameters.size() - 1) {
                builder.add(", ");
            }
        }

        return builder.add(")}").build();
    }

    // Uses Optional here (despite internal implementation normally using @Nullable) to allow callers to simply do `Optional.ifPresent(...)`
    public static Optional<CodeBlock> createCustomMethodsJavadocSection(List<CustomMethodData> customMethods) {
        if (customMethods.isEmpty()) {
            return Optional.empty();
        }

        var javadocBuilder = CodeBlock.builder()
            .add("\n\n<p>Custom methods:")
            .add("\n<ul>");

        for (var methodData : customMethods) {
            javadocBuilder.add("\n<li>").add(methodData.generateJavadocLink());
        }

        return Optional.of(javadocBuilder.add("\n</ul>").build());
    }
}
