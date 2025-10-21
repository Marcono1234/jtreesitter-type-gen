package marcono1234.jtreesitter.type_gen.cli.converter;

import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageVersion;
import picocli.CommandLine;

public class LanguageVersionConverter implements CommandLine.ITypeConverter<LanguageVersion> {
    @Override
    public LanguageVersion convert(String s) {
        return LanguageVersion.fromString(s);
    }
}
