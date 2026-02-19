package marcono1234.jtreesitter.type_gen.internal.gen;

import marcono1234.jtreesitter.type_gen.internal.gen.utils.CommonMethodsGenerator;

import java.util.*;

/**
 * A generated Java interface.
 */
public interface GenJavaInterface extends GenJavaType, CommonMethodsGenerator.InterfaceType {
    @Override
    List<? extends GenJavaType> getSubtypes();

    @Override
    default List<GenSupertypeNodeType> getSubInterfaces() {
        //noinspection NullableProblems; IntelliJ does not understand `Objects::nonNull` check?
        return getSubtypes().stream()
            .map(type -> type instanceof GenSupertypeNodeType i ? i : null)
            .filter(Objects::nonNull)
            .toList();
    }
}
