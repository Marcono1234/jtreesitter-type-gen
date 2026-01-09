package marcono1234.jtreesitter.type_gen.cli;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NamePatternTest {
    private static class CustomParser extends NamePattern.Parser<CustomParser.Data> {
        public CustomParser() {
            super(Map.of(
                "upper", data -> data.s().toUpperCase(Locale.ROOT),
                "lower", data -> data.s().toLowerCase(Locale.ROOT)
            ));
        }

        record Data(String s) {
        }
    }
    private static final CustomParser parser = new CustomParser();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{upper}|C",
        "{lower}|c",
        "a{lower}|ac",
        "{lower}a|ca",
        "a{upper}b|aCb",
        "a{upper}b{lower}d|aCbcd",
        // Repeated placeholders, maybe not that useful for real NamePattern usage but permitted nonetheless
        "a{upper}{lower}{upper}{lower}b|aCcCcb",
        // No placeholder, maybe not that useful for real NamePattern usage but permitted nonetheless
        "ab|ab",
    })
    void parse_getName(String patternString, String expectedName) {
        var pattern = parser.parse(patternString);
        assertEquals(expectedName, pattern.createName(new CustomParser.Data("c")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "''|Pattern string must not be empty",
        "{|Missing closing '}'",
        "}|Unpaired closing '}'",
        "{}|Placeholder name cannot be empty",
        "{upper|Missing closing '}'",
        "{{upper}|Unexpected '{' in placeholder name",
        "{upper{}|Unexpected '{' in placeholder name",
        "{upper}}|Unpaired closing '}'",
        // Cannot nest placeholders
        "{upper{lower}}|Unexpected '{' in placeholder name",
        "a{unknown}b|Unknown placeholder: unknown",
    })
    void parse_Invalid(String patternString, String expectedExceptionMessage) {
        var e = assertThrows(IllegalArgumentException.class, () -> parser.parse(patternString));
        assertEquals(expectedExceptionMessage, e.getMessage());
    }
}
