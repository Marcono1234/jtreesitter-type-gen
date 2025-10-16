package marcono1234.jtreesitter.type_gen.cli.converter;

import picocli.CommandLine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

class EnumConverter<E extends Enum<E>> implements CommandLine.ITypeConverter<E> {
    private final Map<String, E> nameToConstant;

    public EnumConverter(Class<E> enumClass) {
        nameToConstant = new LinkedHashMap<>();
        for (var constant : enumClass.getEnumConstants()) {
            String name = constant.name().replace("_", "-").toLowerCase(Locale.ROOT);
            var existing = nameToConstant.put(name, constant);
            if (existing != null) {
                throw new IllegalStateException("Duplicate name: " + name);
            }
        }
    }

    @Override
    public E convert(String value) {
        var converted = nameToConstant.get(value);
        if (converted == null) {
            throw new CommandLine.TypeConversionException("Unknown value '" + value + "'; expected one of: " + String.join(", ", nameToConstant.keySet()));
        }
        return converted;
    }
}
