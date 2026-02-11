package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.JavaFile;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.TopoSorter;

import java.util.*;

/**
 * Node type for which a top-level Java type should be generated.
 */
public sealed interface GenNodeType extends GenJavaType permits GenRegularNodeType, GenSupertypeNodeType {
    /**
     * Gets the type name defined in the grammar.
     */
    String getTypeName();

    /**
     * Whether the grammar defines this as 'extra' node which can appear anywhere.
     */
    boolean isExtra();

    /**
     * Gets the name of the Java field in the generated class which stores the value of {@link #getTypeName()}.
     */
    String getTypeNameConstant();

    /**
     * Returns whether this type directly (e.g. as subtype of a supertype node) or indirectly (e.g. through child)
     * refers to the type. To avoid infinite recursion, already seen types are tracked in {@code seenTypes}.
     */
    boolean refersToType(GenRegularNodeType type, Set<GenRegularNodeType> seenTypes);

    /**
     * Adds a Java interface which should be implemented by this type.
     */
    void addInterfaceToImplement(GenJavaInterface i);

    /**
     * Gets the direct node supertypes of this type.
     */
    List<GenSupertypeNodeType> getSupertypes();

    /**
     * Gets all direct and transitive node supertypes of this type.
     */
    // TODO: Delegate to getAllSupertypesTopoOrder().reversed()?
    default SequencedSet<GenSupertypeNodeType> getAllSupertypes() {
        // Use a Set to handle the case where the same supertype appears multiple times, directly or transitively
        SequencedSet<GenSupertypeNodeType> supertypes = new LinkedHashSet<>();
        for (var supertype : getSupertypes()) {
            supertypes.add(supertype);
            supertypes.addAll(supertype.getAllSupertypes());
        }
        return supertypes;
    }

    /**
     * Gets all direct and transitive node supertypes of this type, starting with the direct supertypes.
     */
    default SequencedSet<GenSupertypeNodeType> getAllSupertypesTopoOrder() {
        // Add unknown referenced to result set, because this just starts with direct subtypes, going to transitive
        // ones recursively
        var unknownReferencesStrategy = TopoSorter.UnknownReferenceStrategy.ADD;
        return new TopoSorter<>(GenSupertypeNodeType::getSupertypes).sort(getSupertypes(), unknownReferencesStrategy);
    }

    List<JavaFile> generateJavaCode(CodeGenHelper codeGenHelper);
}
