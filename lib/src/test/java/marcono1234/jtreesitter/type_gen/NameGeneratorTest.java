package marcono1234.jtreesitter.type_gen;

import marcono1234.jtreesitter.type_gen.NameGenerator.TokenNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NameGeneratorTest {
    /**
     * Tests for {@link NameGenerator#createDefault(TokenNameGenerator)}.
     *
     * <p>Tests specific to the token name generation are covered by {@link TokenNameGeneratorTest}.
     */
    @Nested
    class DefaultTest {
        private final NameGenerator nameGenerator = NameGenerator.createDefault(TokenNameGenerator.AUTOMATIC);

        @ParameterizedTest
        // Some of these verify that the result is somewhat reasonable, and that no exception occurs
        @CsvSource({
            "name,NodeName",
            "_name,NodeName",
            "name_,NodeName",
            "first_second,NodeFirstSecond",
            "__first__second__,NodeFirst_second_",
        })
        void generateJavaTypeName(String typeName, String expectedJavaName) {
            assertEquals(expectedJavaName, nameGenerator.generateJavaTypeName(typeName));
        }

        @ParameterizedTest
        // Some of these verify that the result is somewhat reasonable, and that no exception occurs
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
        void generateFieldNameConstant(String fieldName, String expectedJavaName) {
            assertEquals(expectedJavaName, nameGenerator.generateFieldNameConstant("parent", fieldName));
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
