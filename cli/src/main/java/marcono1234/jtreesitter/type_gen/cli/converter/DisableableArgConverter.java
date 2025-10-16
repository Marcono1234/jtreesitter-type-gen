package marcono1234.jtreesitter.type_gen.cli.converter;

import picocli.CommandLine;

abstract class DisableableArgConverter<T> implements CommandLine.ITypeConverter<DisableableArg<T>> {
    @Override
    public DisableableArg<T> convert(String value) throws Exception {
        if (value.equals("-")) {
            return DisableableArg.disabled();
        }
        return DisableableArg.enabled(convertValue(value));
    }

    protected abstract T convertValue(String value) throws Exception;
}
