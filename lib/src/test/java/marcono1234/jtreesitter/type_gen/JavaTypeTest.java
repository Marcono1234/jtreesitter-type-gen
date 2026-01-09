package marcono1234.jtreesitter.type_gen;

import com.palantir.javapoet.*;
import com.palantir.javapoet.TypeName;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.JavaTypeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaTypeTest {
    private static TypeName getJavaPoetType(JavaType javaType) {
        return switch (javaType) {
            case JavaTypeImpl t -> t.type();
        };
    }

    private static void assertTypeNameEquals(TypeName expected, TypeName actual) {
        // Note: JavaPoet `TypeName#equals` is just implemented by checking `getClass()` and comparing `toString()`
        // That should include all information (since it is the same as emitting it as code), but could lead to
        // ambiguity for equivalent string representations (especially for nested types such as parameterized type
        // arguments for which the `getClass()` check has no effect), for example regarding '.' and '$' in ClassName
        // and for ClassName vs. TypeVariable
        assertEquals(expected, actual);
    }

    @Test
    void fromType() {
        var type = JavaType.fromType(Map.Entry.class);
        assertTypeNameEquals(ClassName.get(Map.Entry.class), getJavaPoetType(type));

        type = JavaType.fromType(int.class);
        assertTypeNameEquals(TypeName.INT, getJavaPoetType(type));
    }

    private static AnnotationSpec ann(String packageName, String simpleName, String... simpleNames) {
        return AnnotationSpec.builder(ClassName.get(packageName, simpleName, simpleNames)).build();
    }

    static List<Arguments> typeStrings() {
        @SuppressWarnings("RedundantArrayCreation")  // explicitly create array to allow trailing ','
        var argumentValues = Arrays.asList(new Object[] {
            // Type and class names
            "boolean", TypeName.BOOLEAN,
            "byte", TypeName.BYTE,
            "char", TypeName.CHAR,
            "short", TypeName.SHORT,
            "int", TypeName.INT,
            "long", TypeName.LONG,
            "float", TypeName.FLOAT,
            "double", TypeName.DOUBLE,
            "a", ClassName.get("", "a"),  // treat unqualified lower name as ClassName (probably not very common / useful though)
            "a.A", ClassName.get("a", "A"),
            "A$B", ClassName.get("", "A", "B"),
            "A1$B2", ClassName.get("", "A1", "B2"),
            "a.b.c.A$B$C", ClassName.get("a.b.c", "A", "B", "C"),
            // Annotations
            "@a.A b.B", ClassName.get("b", "B").annotated(ann("a", "A")),
            "@a.A @b.B @c.C1$C2 d.D", ClassName.get("d", "D").annotated(ann("a", "A"), ann("b", "B"), ann("c", "C1", "C2")),
            // Unqualified annotation type name; probably not very common or useful but should at least not cause
            // any internal exceptions (e.g. due to `T` being parsed as type variable)
            "@T a.A", ClassName.get("a", "A").annotated(ann("", "T")),
            // Parameterized
            "a.A<b.B>", ParameterizedTypeName.get(ClassName.get("a", "A"), ClassName.get("b", "B")),
            "a.A<b.B, c.C>", ParameterizedTypeName.get(ClassName.get("a", "A"), ClassName.get("b", "B"), ClassName.get("c", "C")),
            "a.A<int[]>", ParameterizedTypeName.get(ClassName.get("a", "A"), ArrayTypeName.of(TypeName.INT)),
            // Wildcards
            "a.A<?>", ParameterizedTypeName.get(ClassName.get("a", "A"), CodeGenHelper.unboundedWildcard()),
            "a.A<? extends T>", ParameterizedTypeName.get(ClassName.get("a", "A"), WildcardTypeName.subtypeOf(TypeVariableName.get("T"))),
            "a.A<? super T>", ParameterizedTypeName.get(ClassName.get("a", "A"), WildcardTypeName.supertypeOf(TypeVariableName.get("T"))),
            "a.A<? super char[]>", ParameterizedTypeName.get(ClassName.get("a", "A"), WildcardTypeName.supertypeOf(ArrayTypeName.of(TypeName.CHAR))),
            // Type variables
            "T", TypeVariableName.get("T"),
            "Test", TypeVariableName.get("Test"),  // treating this as type variable is probably fine, since user should use fully qualified name for classes
            "@a.A T", TypeVariableName.get("T").annotated(ann("a", "A")),
            "java.util.List<T>", ParameterizedTypeName.get(ClassName.get(List.class), TypeVariableName.get("T")),
            "@a.A T @b.B []", ArrayTypeName.of(TypeVariableName.get("T").annotated(ann("a", "A"))).annotated(ann("b", "B")),
            // Arrays
            "int[]", ArrayTypeName.of(TypeName.INT),
            "a.A[]", ArrayTypeName.of(ClassName.get("a", "A")),
            "a.A<b.B>[]", ArrayTypeName.of(ParameterizedTypeName.get(ClassName.get("a", "A"), ClassName.get("b", "B"))),
            "a.A[][]", ArrayTypeName.of(ArrayTypeName.of(ClassName.get("a", "A"))),
            "@a.A int @b.B []", ArrayTypeName.of(TypeName.INT.annotated(ann("a", "A"))).annotated(ann("b", "B")),
            "@a.A int @b.B @c.C [] @d.D []", ArrayTypeName.of(ArrayTypeName.of(TypeName.INT.annotated(ann("a", "A"))).annotated(ann("d", "D"))).annotated(ann("b", "B"), ann("c", "C")),
            // Mixed
            "a.A<int[], b.B<T, ? extends c.C[], d.D<@e.E U>[]>>",
                ParameterizedTypeName.get(ClassName.get("a", "A"),
                    ArrayTypeName.of(TypeName.INT),
                    ParameterizedTypeName.get(ClassName.get("b", "B"),
                        TypeVariableName.get("T"),
                        WildcardTypeName.subtypeOf(ArrayTypeName.of(ClassName.get("c", "C"))),
                        ArrayTypeName.of(ParameterizedTypeName.get(ClassName.get("d", "D"),
                            TypeVariableName.get("U").annotated(ann("e", "E"))
                        ))
                    )
                ),
        });

        var arguments = new ArrayList<Arguments>(argumentValues.size() / 2);
        for (int i = 0; i < argumentValues.size(); i += 2) {
            //noinspection RedundantCast; cast makes sure to fail fast if argument types are incorrect
            arguments.add(Arguments.of((String) argumentValues.get(i), (TypeName) argumentValues.get(i + 1)));
        }
        return arguments;
    }

    @ParameterizedTest
    @MethodSource("typeStrings")
    void fromTypeString(String typeStr, TypeName expectedType) {
        var actualType = getJavaPoetType(JavaType.fromTypeString(typeStr));
        assertTypeNameEquals(expectedType, actualType);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        // Simple ClassName
        "''|Empty name",
        "' '|Unexpected space",
        "void|\\'void\\' is not valid",
        "a.|Invalid trailing dot",
        "a..A|Invalid dot",
        "a.$A|Missing enclosing name for nested name",
        ".A|Invalid dot",
        ".a.A|Invalid dot",
        "A$|Invalid trailing $",
        "A$$B|Invalid $",
        "a.B$C.D|Invalid dot in nested name",
        // Annotations
        "@|Empty name",
        "@ a.A|Unexpected space",
        "@a.A|Expected space after annotation",
        "'@a.A '|Empty name",
        "@@a.A b.B|Empty name or invalid char",
        "@a.A@b.B c.C|Expected space after annotation",
        "@a.A  b.B|Unexpected space",
        "@a.A  @b.B c.C|Unexpected space",
        "@int|Primitive type is not allowed here",
        "@void|\\'void\\' is not valid",
        // Parameterized
        "<T>|Empty name or invalid char",
        "a.A<>|Empty name or invalid char",
        "a.A<b.B|Expected ', ' or '>'",
        "a.A<b.B,>|Expected ', ' or '>'",
        "'a.A<b.B, '|Empty name",
        "a.A<b.B, >|Empty name or invalid char",
        "a.A<b.B, c.C|Expected ', ' or '>'",
        "a.A<b.B,  c.C>|Unexpected space",
        "a.A<<b.B>>|Empty name or invalid char",
        "int<a.A>|Primitive type is not allowed here",
        "a.A<int>|Primitive type is not allowed here",
        "@a.A<T>|Expected space after annotation",
        "@a.A <T>|Empty name or invalid char",
        // Wildcards
        "?|Empty name or invalid char",  // top level wildcard is not allowed
        "? extends T|Empty name or invalid char",
        "a.A<??>|Expected ', ' or '>'",
        "a.A<? >|Expected ', ' or '>'",
        "a.A<? extends ?>|Empty name or invalid char",
        "a.A<? extends int>|Primitive type is not allowed here",
        "a.A<? extends >|Empty name or invalid char",
        "a.A<? super ?>|Empty name or invalid char",
        "a.A<? super >|Empty name or invalid char",
        // Type variable
        "T<a.A>|Cannot specify type arguments for type variable",
        "a.A<T extends a.B>|Unexpected space",  // only type var declaration can define bounds, not type var usage
        // Arrays
        "[]|Empty name or invalid char",
        "@a.A []|Empty name or invalid char",
        "a.A[|Missing ']'",
        "a.A]|Invalid trailing character",
        "a.A[]]|Invalid trailing character",
        "a.A@b.B|Invalid trailing character",
        "a.A @b.B|Expected space after annotation",
        "'a.A @b.B '|Missing '[]'",
        "a.A @b.B[]|Expected space after annotation",
        "a.A @b.B [|Missing ']'",
    })
    void fromTypeString_Invalid(String typeStr, String expectedExceptionMessage) {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaType.fromTypeString(typeStr));
        String message = e.getMessage();
        // Omit the 'error marker', because that makes comparing the expected message rather cumbersome
        // TODO: Or refactor test to use @MethodSource instead of @CsvSource, and then specify expected error message as text block?
        int lineBreakIndex = message.indexOf('\n');
        message = message.substring(0, lineBreakIndex);

        // Note: Maybe a bit hacky; when using `\` JUnit's @CsvSource seems to not recognize `'` as quote anymore (as desired),
        // but it does not actually treat it as an escaping character and remove it on its own?
        expectedExceptionMessage = expectedExceptionMessage.replace("\\'", "'");
        assertEquals(expectedExceptionMessage, message);
    }

    private static String adjustExpectedErrorMarkerMessage(String message) {
        // Replace leading 4 spaces with '\t' (but only leading ones, since error marker is indented with spaces as well)
        message = message.replaceAll("(?m)^ {4}", "\t");

        if (message.endsWith("\n")) {
            message = message.substring(0, message.length() - 1);
        }
        return message;
    }

    @Test
    void fromTypeString_Invalid_ErrorMarker() {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaType.fromTypeString("a.b.C$D.E"));
        assertEquals(
            adjustExpectedErrorMarkerMessage("""
            Invalid dot in nested name
                a.b.C$D.E
                       ^
            """),
            e.getMessage()
        );

        e = assertThrows(IllegalArgumentException.class, () -> JavaType.fromTypeString("a.b.C$"));
        assertEquals(
            adjustExpectedErrorMarkerMessage("""
            Invalid trailing $
                a.b.C$
                     ^
            """),
            e.getMessage()
        );

        e = assertThrows(IllegalArgumentException.class, () -> JavaType.fromTypeString("a.A<int>"));
        assertEquals(
            adjustExpectedErrorMarkerMessage("""
            Primitive type is not allowed here
                a.A<int>
                    ^
            """),
            e.getMessage()
        );
    }
}
