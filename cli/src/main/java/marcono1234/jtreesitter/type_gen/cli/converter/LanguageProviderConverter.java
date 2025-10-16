package marcono1234.jtreesitter.type_gen.cli.converter;

import marcono1234.jtreesitter.type_gen.LanguageConfig.LanguageProviderConfig;
import picocli.CommandLine;

public class LanguageProviderConverter implements CommandLine.ITypeConverter<LanguageProviderConfig> {
    @Override
    public LanguageProviderConfig convert(String s) {
        return LanguageProviderConfig.fromString(s);
    }
}
