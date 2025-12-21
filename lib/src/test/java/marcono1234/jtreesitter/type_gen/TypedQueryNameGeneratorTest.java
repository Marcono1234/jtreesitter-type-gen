package marcono1234.jtreesitter.type_gen;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TypedQueryNameGeneratorTest {
    /**
     * Tests for {@link TypedQueryNameGenerator#createDefault(NameGenerator)}.
     */
    @Nested
    class DefaultTest {
        private final TypedQueryNameGenerator nameGenerator = TypedQueryNameGenerator.createDefault(NameGenerator.createDefault(NameGenerator.TokenNameGenerator.AUTOMATIC));

        @ParameterizedTest
        @CsvSource({
            "custom,QNodeCustom",
            "first_second,QNodeFirstSecond",
        })
        void generateBuilderClassName(String typeName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateBuilderClassName(typeName));
        }

        @ParameterizedTest
        @CsvSource({
            "custom,nodeCustom",
            "first_second,nodeFirstSecond",
        })
        void generateBuilderMethodName(String typeName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateBuilderMethodName(typeName));
        }

        @ParameterizedTest
        @CsvSource({
            "node,custom,asSubtypeOfNodeCustom",
            "my_node,my_parent,asSubtypeOfNodeMyParent",
        })
        void generateAsSubtypeMethodName(String typeName, String supertypeName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateAsSubtypeMethodName(typeName, supertypeName));
        }

        @ParameterizedTest
        @CsvSource({
            "parent,custom,withFieldCustom",
            "parent,my_field,withFieldMyField",
        })
        void generateWithFieldMethodName(String parentTypeName, String fieldName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateWithFieldMethodName(parentTypeName, fieldName));
        }

        @ParameterizedTest
        @CsvSource({
            "parent,custom,withoutFieldCustom",
            "parent,my_field,withoutFieldMyField",
        })
        void generateWithoutFieldMethodName(String parentTypeName, String fieldName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateWithoutFieldMethodName(parentTypeName, fieldName));
        }

        @ParameterizedTest
        @CsvSource({
            "parent,custom,'+,-',fieldTokenCustom",
            "parent,my_field,'+,-',fieldTokenMyField",
        })
        void generateFieldTokenMethodName(String parentTypeName, String fieldName, String tokenFieldTypesNames, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateFieldTokenMethodName(parentTypeName, fieldName, Arrays.asList(tokenFieldTypesNames.split(",")))
            );
        }

        @ParameterizedTest
        @CsvSource({
            "parent,'+,-',childToken",
        })
        void generateChildTokenMethodName(String parentTypeName, String tokenChildrenTypesNames, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateChildTokenMethodName(parentTypeName, Arrays.asList(tokenChildrenTypesNames.split(",")))
            );
        }
    }
}
