package marcono1234.jtreesitter.type_gen.cli.converter;

import marcono1234.jtreesitter.type_gen.cli.NamePattern;
import marcono1234.jtreesitter.type_gen.cli.TypedNodeNamePatternParser;
import picocli.CommandLine;

public class NamePatternConverter<D> implements CommandLine.ITypeConverter<NamePattern<D>> {
    private final NamePattern.Parser<D> patternParser;

    protected NamePatternConverter(NamePattern.Parser<D> patternParser) {
        this.patternParser = patternParser;
    }

    @Override
    public NamePattern<D> convert(String value) {
        return patternParser.parse(value);
    }


    // Separate subclasses to allow referencing them as `@CommandLine.Option(converter = ...)`

    public static class TypedNode extends NamePatternConverter<TypedNodeNamePatternParser.Data> {
        public TypedNode() {
            super(TypedNodeNamePatternParser.INSTANCE);
        }
    }
}
