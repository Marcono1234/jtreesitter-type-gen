package marcono1234.jtreesitter.type_gen.cli.converter;

import marcono1234.jtreesitter.type_gen.TypeName;

public class DisableableTypeNameConverter extends DisableableArgConverter<TypeName> {
    @Override
    protected TypeName convertValue(String value) {
        return TypeName.fromQualifiedName(value);
    }
}
