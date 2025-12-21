package marcono1234.jtreesitter.type_gen.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class JavaNameGeneratorHelperTest {
    @ParameterizedTest
    @CsvSource({
        "a,a",
        "aB,aB",
        "a_b,aB",
        "ab_cd,abCd",
        "a_,a",
        "_a,A",  // Note: Maybe improve this case in the future (don't uppercase, just omit leading '_'?)
        "a__b,a_b",  // Note: Maybe improve this case in the future
    })
    void convertSnakeToCamelCase(String input, String expectedOutput) {
        assertEquals(expectedOutput, JavaNameGeneratorHelper.convertSnakeToCamelCase(input));
    }

    @Test
    void convertSnakeToCamelCase_Empty() {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameGeneratorHelper.convertSnakeToCamelCase(""));
        assertEquals("Empty string is not supported", e.getMessage());
    }

    @ParameterizedTest
    // Some of these just verify that the result is somewhat reasonable, and that no exception occurs
    @CsvSource({
        "name,NAME",
        "_name,_NAME",
        "name_,NAME_",
        "first_second,FIRST_SECOND",
        "__first__second__,__FIRST__SECOND__",
        "firstSecond,FIRST_SECOND",
        "first1second2,FIRST_1SECOND_2",
        "nAmE,N_AM_E",
    })
    void convertToConstantName(String input, String expectedOutput) {
        assertEquals(expectedOutput, JavaNameGeneratorHelper.convertToConstantName(input));
    }

    @Test
    void convertToConstantName_Empty() {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameGeneratorHelper.convertToConstantName(""));
        assertEquals("Empty string is not supported", e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "a,A",
        "ab,Ab",
        "Ab,Ab",
        "0a,0a",
    })
    void upperFirstChar(String input, String expectedOutput) {
        assertEquals(expectedOutput, JavaNameGeneratorHelper.upperFirstChar(input));
    }

    @Test
    void upperFirstChar_Empty() {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameGeneratorHelper.upperFirstChar(""));
        assertEquals("Empty string is not supported", e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "A,a",
        "Ab,ab",
        "aB,aB",
        "0A,0A",
    })
    void lowerFirstChar(String input, String expectedOutput) {
        assertEquals(expectedOutput, JavaNameGeneratorHelper.lowerFirstChar(input));
    }

    @Test
    void lowerFirstChar_Empty() {
        var e = assertThrows(IllegalArgumentException.class, () -> JavaNameGeneratorHelper.lowerFirstChar(""));
        assertEquals("Empty string is not supported", e.getMessage());
    }
}
