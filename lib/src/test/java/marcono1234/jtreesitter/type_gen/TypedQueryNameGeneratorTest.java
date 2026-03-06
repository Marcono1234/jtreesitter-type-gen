package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.NameGenerator.TokenNameGenerator;
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
        private final TypedQueryNameGenerator nameGenerator = TypedQueryNameGenerator.createDefault(new NameGenerator.DefaultNameGenerator(TokenNameGenerator.AUTOMATIC));

        @ParameterizedTest
        @CsvSource({
            "custom,QNodeCustom",
            "first_second,QNodeFirstSecond",
        })
        void generateBuilderClassName(String nodeType, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateBuilderClassName(nodeType));
        }

        @ParameterizedTest
        @CsvSource({
            "custom,nodeCustom",
            "first_second,nodeFirstSecond",
        })
        void generateBuilderMethodName(String nodeType, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateBuilderMethodName(nodeType));
        }

        @ParameterizedTest
        @CsvSource({
            "node,custom,asSubtypeOfNodeCustom",
            "my_node,my_parent,asSubtypeOfNodeMyParent",
        })
        void generateAsSubtypeMethodName(String nodeType, String nodeSupertype, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateAsSubtypeMethodName(nodeType, nodeSupertype));
        }

        @ParameterizedTest
        @CsvSource({
            "parent,custom,withFieldCustom",
            "parent,my_field,withFieldMyField",
        })
        void generateWithFieldMethodName(String parentNodeType, String fieldName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateWithFieldMethodName(parentNodeType, fieldName));
        }

        @ParameterizedTest
        @CsvSource({
            "parent,custom,withoutFieldCustom",
            "parent,my_field,withoutFieldMyField",
        })
        void generateWithoutFieldMethodName(String parentNodeType, String fieldName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateWithoutFieldMethodName(parentNodeType, fieldName));
        }

        @ParameterizedTest
        @CsvSource({
            "parent,custom,'+,-',fieldTokenCustom",
            "parent,my_field,'+,-',fieldTokenMyField",
        })
        void generateFieldTokenMethodName(String parentNodeType, String fieldName, String tokenFieldTypesNames, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateFieldTokenMethodName(parentNodeType, fieldName, Arrays.asList(tokenFieldTypesNames.split(",")))
            );
        }

        @ParameterizedTest
        @CsvSource({
            "parent,'+,-',childToken",
        })
        void generateChildTokenMethodName(String parentNodeType, String tokenChildrenTypesNames, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateChildTokenMethodName(parentNodeType, Arrays.asList(tokenChildrenTypesNames.split(",")))
            );
        }
    }
}
