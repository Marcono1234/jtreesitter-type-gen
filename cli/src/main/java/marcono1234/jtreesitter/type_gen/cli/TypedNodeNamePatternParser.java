package marcono1234.jtreesitter.type_gen.cli;

import java.util.Map;

import static marcono1234.jtreesitter.type_gen.internal.JavaNameGeneratorHelper.typeNameToUpperCamel;

/**
 * {@link NamePattern.Parser} for the name of typed node classes.
 */
// Note: This does not actually have to be a dedicated class, could instead just do `new NamePattern.Parser<>(...)`,
//   but having a dedicated class allows grouping the `Data` record inside it
public class TypedNodeNamePatternParser extends NamePattern.Parser<TypedNodeNamePatternParser.Data> {
    public static final TypedNodeNamePatternParser INSTANCE = new TypedNodeNamePatternParser();

    /**
     * @param typeName node type name as defined in the Tree-sitter grammar
     */
    public record Data(String typeName) {
    }

    private TypedNodeNamePatternParser() {
        super(Map.of(
            // Something like "node_type" might be more correct, but prefer a shorter name to keep it concise
            "node", data -> typeNameToUpperCamel(data.typeName())
        ));
    }
}
