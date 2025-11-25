package marcono1234.jtreesitter.type_gen.cli.converter;

import marcono1234.jtreesitter.type_gen.TypeName;
import picocli.CommandLine;

public class TypeNameConverter implements CommandLine.ITypeConverter<TypeName> {
    @Override
    public TypeName convert(String s) {
        return convertToTypeName(s);
    }

    // Dedicated method to reuse this logic for DisableableTypeNameConverter
    static TypeName convertToTypeName(String s) {
        return TypeName.fromQualifiedName(s);
    }
}
