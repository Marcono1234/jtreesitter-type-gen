package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.JavaLiteral;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomMethodDataTest {

    static List<Arguments> literals() {
        @SuppressWarnings("RedundantArrayCreation")  // explicitly create array to allow trailing ','
        var argumentValues = Arrays.asList(new Object[] {
            new JavaLiteral.Boolean(true), "true",
            new JavaLiteral.Boolean(false), "false",
            new JavaLiteral.Integer(0), "0",
            new JavaLiteral.Integer(Integer.MIN_VALUE), "-2147483648",
            new JavaLiteral.Long(0), "0L",
            new JavaLiteral.Long(Long.MIN_VALUE), "-9223372036854775808L",
            new JavaLiteral.Double(0), "0.0",
            new JavaLiteral.Double(-0.0), "-0.0",
            new JavaLiteral.Double(1e200), "1.0E200",
            new JavaLiteral.Double(Double.NaN), "java.lang.Double.NaN",
            new JavaLiteral.Double(Double.POSITIVE_INFINITY), "java.lang.Double.POSITIVE_INFINITY",
            new JavaLiteral.Double(Double.NEGATIVE_INFINITY), "java.lang.Double.NEGATIVE_INFINITY",
            new JavaLiteral.String(""), "\"\"",
            new JavaLiteral.String("test"), "\"test\"",
            new JavaLiteral.String("\"test"), "\"\\\"test\"",
        });
        var arguments = new ArrayList<Arguments>(argumentValues.size() / 2);
        for (int i = 0; i < argumentValues.size(); i += 2) {
            //noinspection RedundantCast; cast makes sure to fail fast if argument types are incorrect
            arguments.add(Arguments.of((JavaLiteral) argumentValues.get(i), (String) argumentValues.get(i + 1)));
        }
        return arguments;
    }

    @ParameterizedTest
    @MethodSource("literals")
    void emitLiteral(JavaLiteral literal, String expectedCode) {
        var code = CustomMethodData.emitLiteral(literal);
        assertEquals(expectedCode, code.toString());
    }

    @Test
    void asGeneratedMethod() {
        var customMethod = new CustomMethodData(
            "myName",
            List.of(TypeVariableName.get("T", Number.class)),
            List.of(ParameterSpec.builder(String.class, "s").build()),
            TypeName.BOOLEAN,
            "some javadoc",
            ClassName.get("example", "Receiver"),
            "receiverMethod",
            List.of(new JavaLiteral.Integer(1))
        );

        var expectedGeneratedMethod = new GeneratedMethod(
            GeneratedMethod.SimpleKind.CUSTOM_METHOD,
            new GeneratedMethod.Signature(
                "myName",
                List.of(TypeVariableName.get("T", Number.class)),
                List.of(new GeneratedMethod.Signature.Parameter("s", ClassName.get(String.class)))
            ),
            new GeneratedMethod.ReturnType(TypeName.BOOLEAN, GeneratedMethod.SupertypesResolver.EMPTY)
        );
        assertEquals(expectedGeneratedMethod, customMethod.asGeneratedMethod());
    }

    @Test
    void asGeneratedMethod_VoidReturn() {
        var customMethod = new CustomMethodData(
            "myName",
            List.of(TypeVariableName.get("T", Number.class)),
            List.of(ParameterSpec.builder(String.class, "s").build()),
            null,
            "some javadoc",
            ClassName.get("example", "Receiver"),
            "receiverMethod",
            List.of(new JavaLiteral.Integer(1))
        );

        var expectedGeneratedMethod = new GeneratedMethod(
            GeneratedMethod.SimpleKind.CUSTOM_METHOD,
            new GeneratedMethod.Signature(
                "myName",
                List.of(TypeVariableName.get("T", Number.class)),
                List.of(new GeneratedMethod.Signature.Parameter("s", ClassName.get(String.class)))
            ),
            null
        );
        assertEquals(expectedGeneratedMethod, customMethod.asGeneratedMethod());
    }

    @Test
    void typeNameForJavadoc() {
        AnnotationSpec annotation = AnnotationSpec.builder(NonNull.class).build();

        assertEquals(
            TypeName.INT,
            CustomMethodData.typeNameForJavadoc(TypeName.INT.annotated(annotation))
        );

        assertEquals(
            ClassName.get(String.class),
            CustomMethodData.typeNameForJavadoc(ClassName.get(String.class).annotated(annotation))
        );

        assertEquals(
            ClassName.get(List.class),
            CustomMethodData.typeNameForJavadoc(ParameterizedTypeName.get(List.class, String.class).annotated(annotation))
        );

        assertEquals(
            ArrayTypeName.of(ClassName.get(List.class)),
            CustomMethodData.typeNameForJavadoc(ArrayTypeName.of(ParameterizedTypeName.get(List.class, String.class).annotated(annotation)).annotated(annotation))
        );

        assertEquals(
            TypeVariableName.get("T"),
            CustomMethodData.typeNameForJavadoc(TypeVariableName.get("T", ClassName.get(List.class).annotated(annotation)).annotated(annotation))
        );

        var e = assertThrows(AssertionError.class, () -> CustomMethodData.typeNameForJavadoc(WildcardTypeName.subtypeOf(List.class)));
        assertEquals("unreachable", e.getMessage());
    }
}
