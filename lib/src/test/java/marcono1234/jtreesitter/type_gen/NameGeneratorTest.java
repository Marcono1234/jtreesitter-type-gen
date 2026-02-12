package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.NameGenerator.TokenNameGenerator;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NameGeneratorTest {
    /**
     * Tests for {@link NameGenerator.DefaultNameGenerator}.
     *
     * <p>Tests specific to the token name generation are covered by {@link TokenNameGeneratorTest}.
     */
    @Nested
    class DefaultTest {
        private final NameGenerator nameGenerator = new NameGenerator.DefaultNameGenerator(TokenNameGenerator.AUTOMATIC);

        @ParameterizedTest
        // Some of these just verify that the result is somewhat reasonable, and that no exception occurs
        @CsvSource({
            "name,NodeName",
            "_name,NodeName",
            "name_,NodeName",
            "first_second,NodeFirstSecond",
            "__first__second__,NodeFirst_second_",
        })
        void generateJavaTypeName(String typeName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateJavaTypeName(typeName));
        }

        @ParameterizedTest
        @CsvSource({
            "my_node,TYPE_NAME",
        })
        void generateTypeNameConstant(String typeName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateTypeNameConstant(typeName));
        }

        @ParameterizedTest
        @CsvSource({
            "my_node,TYPE_ID",
        })
        void generateTypeIdConstant(String typeName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateTypeIdConstant(typeName));
        }

        @ParameterizedTest
        @CsvSource({
            "my_parent,'node_a,node_b',Child",
        })
        void generateChildrenTypesName(String parentTypeName, String childrenTypesNames, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateChildrenTypesName(parentTypeName, Arrays.asList(childrenTypesNames.split(",")))
            );
        }

        @ParameterizedTest
        @CsvSource({
            "my_parent,'+,-',ChildTokenType",
        })
        void generateChildrenTokenTypeName(String parentTypeName, String tokenChildrenTypesNames, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateChildrenTokenTypeName(parentTypeName, Arrays.asList(tokenChildrenTypesNames.split(",")))
            );
        }

        @Test
        void generateChildrenTokenName() {
            var e = assertThrows(AssertionError.class, () -> nameGenerator.generateChildrenTokenName("parent", "+", 0));
            assertEquals("currently unused", e.getMessage());
        }

        @ParameterizedTest
        @CsvSource({
            "my_parent,'node_a,node_b',false,false,getChild",
            "my_parent,'node_a,node_b',false,true,getChild",
            "my_parent,'node_a,node_b',true,false,getChildren",
            "my_parent,'node_a,node_b',true,true,getChildren",
        })
        void generateChildrenGetterName(String parentTypeName, String childrenTypesNames, boolean multiple, boolean required, String     expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateChildrenGetterName(parentTypeName, Arrays.asList(childrenTypesNames.split(",")), multiple, required)
            );
        }

        @ParameterizedTest
        // Some of these just verify that the result is somewhat reasonable, and that no exception occurs
        @CsvSource({
            "name,FIELD_NAME",
            "_name,FIELD__NAME",
            "name_,FIELD_NAME_",
            "first_second,FIELD_FIRST_SECOND",
            "__first__second__,FIELD___FIRST__SECOND__",
            "firstSecond,FIELD_FIRST_SECOND",
            "first1second2,FIELD_FIRST_1SECOND_2",
            "nAmE,FIELD_N_AM_E",
        })
        void generateFieldNameConstant(String fieldName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateFieldNameConstant("parent", fieldName));
        }

        @ParameterizedTest
        // Some of these just verify that the result is somewhat reasonable, and that no exception occurs
        @CsvSource({
            "name,FIELD_NAME_ID",
            "_name,FIELD__NAME_ID",
            "name_,FIELD_NAME__ID",
            "first_second,FIELD_FIRST_SECOND_ID",
            "__first__second__,FIELD___FIRST__SECOND___ID",
            "firstSecond,FIELD_FIRST_SECOND_ID",
            "first1second2,FIELD_FIRST_1SECOND_2_ID",
            "nAmE,FIELD_N_AM_E_ID",
        })
        void generateFieldIdConstant(String fieldName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateFieldIdConstant("parent", fieldName));
        }

        @ParameterizedTest
        // Some of these just verify that the result is somewhat reasonable, and that no exception occurs
        @CsvSource({
            "name,FieldName",
            "_name,FieldName",
            "name_,FieldName",
            "first_second,FieldFirstSecond",
            "__first__second__,Field_first_second_",
            "firstSecond,FieldFirstSecond",
            "first1second2,FieldFirst1second2",
            "nAmE,FieldNAmE",
        })
        void generateFieldTypesName(String fieldName, String expectedName) {
            assertEquals(expectedName, nameGenerator.generateFieldTypesName("parent", fieldName));
        }

        @ParameterizedTest
        @CsvSource({
            "name,'+,-',FieldTokenName",
            "first_second,'+,-',FieldTokenFirstSecond",
        })
        void generateFieldTokenTypeName(String fieldName, String tokenFieldTypesNames, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateFieldTokenTypeName("parent", fieldName, Arrays.asList(tokenFieldTypesNames.split(",")))
            );
        }

        @ParameterizedTest
        @CsvSource({
            "my_field,+,PLUS_SIGN",
            "my_field,+++,TOKEN_0",  // falls back to using token index
        })
        void generateFieldTokenName(String fieldName, String tokenType, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateFieldTokenName("parent", fieldName, tokenType, 0)
            );
        }

        @ParameterizedTest
        @CsvSource({
            "my_field,false,false,getFieldMyField",
            "my_field,false,true,getFieldMyField",
            "my_field,true,false,getFieldMyField",
            "my_field,true,true,getFieldMyField",
        })
        void generateFieldGetterName(String fieldName, boolean multiple, boolean required, String expectedName) {
            assertEquals(
                expectedName,
                nameGenerator.generateFieldGetterName("parent", fieldName, multiple, required)
            );
        }

        @ParameterizedTest
        @CsvSource({
            "false,false,getUnnamedChildren",
            "false,true,",
            "true,false,getUnnamedChildren",
            "true,true,",
        })
        void generateNonNamedChildrenGetterName(boolean hasNamedChildren, boolean hasFields, @Nullable String expectedName) {
            assertEquals(
                Optional.ofNullable(expectedName),
                nameGenerator.generateNonNamedChildrenGetterName("parent", hasNamedChildren, hasFields)
            );
        }

        @SuppressWarnings("SimplifiableAssertion")  // manually tests `equals` implementation
        @Test
        void equalsHashCode() {
            var tokenNameGenerator = TokenNameGenerator.AUTOMATIC;
            var generator1 = new NameGenerator.DefaultNameGenerator(tokenNameGenerator);
            var generator2 = new NameGenerator.DefaultNameGenerator(tokenNameGenerator);
            assertTrue(generator1.equals(generator2));
            assertTrue(generator2.equals(generator1));
            assertEquals(generator1.hashCode(), generator2.hashCode());

            var generatorOther = new NameGenerator.DefaultNameGenerator(TokenNameGenerator.fromMapping(Map.of("a", Map.of("b", Map.of("c", "d"))), true));
            assertFalse(generator1.equals(generatorOther));
            assertFalse(generatorOther.equals(generator1));

            var generatorSubClass = new NameGenerator.DefaultNameGenerator(tokenNameGenerator) {
                @Override
                public boolean equals(Object obj) {
                    return obj == this;
                }
            };
            assertFalse(generator1.equals(generatorSubClass));
            assertFalse(generatorSubClass.equals(generator1));
        }
    }

    @Nested
    class TokenNameGeneratorTest {
        @ParameterizedTest
        @CsvSource({
            // Unicode character name
            "=,EQUALS_SIGN",
            "some token,SOME_TOKEN",
            // For length > 1 don't use Unicode character name; fall back to generic name based on `index`
            "==,TOKEN_0",
            "<>,TOKEN_0",
        })
        void automatic(String token, String expectedName) {
            var nameGenerator = TokenNameGenerator.AUTOMATIC;
            var name = nameGenerator.generateFieldTokenName("parent", "field", token, 0);
            assertEquals(expectedName, name);
        }

        @Test
        void fromMapping_Child() {
            var nameGenerator = TokenNameGenerator.fromMapping(Map.of(), false);
            // `generateChildrenTokenName` is currently not supported because it is effectively unused
            var e = assertThrows(AssertionError.class, () -> nameGenerator.generateChildrenTokenName("parent", "token", 1));
            assertEquals("currently unused", e.getMessage());
        }

        @Test
        void fromMapping_Field() {
            {
                var nameGenerator = TokenNameGenerator.fromMapping(Map.of(), false);
                var name = nameGenerator.generateFieldTokenName("parent", "field", "some token", 0);
                // Should use automatic name
                assertEquals("SOME_TOKEN", name);
            }

            // Simple mapping without fallbacks
            {
                var nameGenerator = TokenNameGenerator.fromMapping(Map.of("A", Map.of("a", Map.of("+", "A.a"))), false);
                var name = nameGenerator.generateFieldTokenName("A", "a", "+", 0);
                assertEquals("A.a", name);

                // Should use automatic names for unknown token types
                name = nameGenerator.generateFieldTokenName("A", "a", "some token", 0);
                assertEquals("SOME_TOKEN", name);

                name = nameGenerator.generateFieldTokenName("A", "x", "+", 0);
                assertEquals("PLUS_SIGN", name);

                name = nameGenerator.generateFieldTokenName("X", "a", "+", 0);
                assertEquals("PLUS_SIGN", name);

                name = nameGenerator.generateFieldTokenName("X", "x", "+", 0);
                assertEquals("PLUS_SIGN", name);
            }

            // Mapping with fallbacks
            {
                var nameGenerator = TokenNameGenerator.fromMapping(
                    Map.of(
                        "A", Map.of(
                            "a", Map.of(
                                "+", "A.a+",
                                "-", "A.a-"
                            ),
                            "b", Map.of(
                                "+", "A.b+"
                            ),
                            "", Map.of(
                                "+", "A.fallback"
                            )
                        ),
                        "", Map.of(
                            "a", Map.of(
                                "+", "fallback.a"
                            ),
                            "", Map.of(
                                "+", "fallback.fallback",
                                // Token type `""` should be matched literally and not act as fallback, so
                                // effectively this is never reached because `""` is not a valid token type
                                "", "empty"
                            )
                        )
                    ),
                    false
                );
                var name = nameGenerator.generateFieldTokenName("A", "a", "+", 0);
                assertEquals("A.a+", name);

                name = nameGenerator.generateFieldTokenName("A", "a", "-", 0);
                assertEquals("A.a-", name);

                name = nameGenerator.generateFieldTokenName("A", "b", "+", 0);
                assertEquals("A.b+", name);

                name = nameGenerator.generateFieldTokenName("A", "x", "+", 0);
                assertEquals("A.fallback", name);

                name = nameGenerator.generateFieldTokenName("X", "a", "+", 0);
                assertEquals("fallback.a", name);

                name = nameGenerator.generateFieldTokenName("X", "x", "+", 0);
                assertEquals("fallback.fallback", name);

                // Should use automatic names for unknown token types
                name = nameGenerator.generateFieldTokenName("A", "a", "some token", 0);
                assertEquals("SOME_TOKEN", name);

                name = nameGenerator.generateFieldTokenName("A", "x", "some token", 0);
                assertEquals("SOME_TOKEN", name);

                name = nameGenerator.generateFieldTokenName("X", "a", "some token", 0);
                assertEquals("SOME_TOKEN", name);

                name = nameGenerator.generateFieldTokenName("X", "x", "some token", 0);
                // Should use automatic name, and not use "empty" defined above
                assertEquals("SOME_TOKEN", name);
            }
        }

        @Test
        void fromMapping_Field_Exhaustive() {
            {
                var nameGenerator = TokenNameGenerator.fromMapping(Map.of(), true);
                var e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("parent", "field", "some token", 0));
                assertEquals("Token type not mapped: type = parent, field = field, token = some token", e.getMessage());
            }

            // Simple mapping without fallbacks
            {
                var nameGenerator = TokenNameGenerator.fromMapping(Map.of("A", Map.of("a", Map.of("+", "A.a"))), true);
                var name = nameGenerator.generateFieldTokenName("A", "a", "+", 0);
                assertEquals("A.a", name);

                var e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("A", "a", "some token", 0));
                assertEquals("Token type not mapped: type = A, field = a, token = some token", e.getMessage());

                e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("A", "x", "+", 0));
                assertEquals("Token type not mapped: type = A, field = x, token = +", e.getMessage());

                e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("X", "a", "+", 0));
                assertEquals("Token type not mapped: type = X, field = a, token = +", e.getMessage());

                e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("X", "x", "+", 0));
                assertEquals("Token type not mapped: type = X, field = x, token = +", e.getMessage());
            }

            // Mapping with fallbacks
            {
                var nameGenerator = TokenNameGenerator.fromMapping(
                    Map.of(
                        "A", Map.of(
                            "a", Map.of(
                                "+", "A.a"
                            ),
                            "", Map.of(
                                "+", "A.fallback"
                            )
                        ),
                        "", Map.of(
                            "a", Map.of(
                                "+", "fallback.a"
                            ),
                            "", Map.of(
                                "+", "fallback.fallback"
                            )
                        )
                    ),
                    true
                );
                var name = nameGenerator.generateFieldTokenName("A", "a", "+", 0);
                assertEquals("A.a", name);

                // Fallbacks should work, even for `exhaustive`
                name = nameGenerator.generateFieldTokenName("A", "x", "+", 0);
                assertEquals("A.fallback", name);

                name = nameGenerator.generateFieldTokenName("X", "a", "+", 0);
                assertEquals("fallback.a", name);

                name = nameGenerator.generateFieldTokenName("X", "x", "+", 0);
                assertEquals("fallback.fallback", name);

                // Should throw exception for unknown token types
                var e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("A", "a", "some token", 0));
                assertEquals("Token type not mapped: type = A, field = a, token = some token", e.getMessage());

                e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("A", "x", "some token", 0));
                assertEquals("Token type not mapped: type = A, field = x, token = some token", e.getMessage());

                e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("X", "a", "some token", 0));
                assertEquals("Token type not mapped: type = X, field = a, token = some token", e.getMessage());

                e = assertThrows(IllegalArgumentException.class, () -> nameGenerator.generateFieldTokenName("X", "x", "some token", 0));
                assertEquals("Token type not mapped: type = X, field = x, token = some token", e.getMessage());
            }
        }
    }
}
