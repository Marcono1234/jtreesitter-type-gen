package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * For interfaces, determines the common methods they should 'inherit' from their subtypes.
 */
public class CommonMethodsGenerator {
    private CommonMethodsGenerator() {
    }

    // Note: For cleaner code and to allow easier testing, this class defines its own interfaces which only provide
    // the functionality needed here, instead of using GenJavaInterface or similar

    public interface InterfaceType {
        /**
         * Gets direct subtypes of this interface. This can include subinterfaces as well.
         */
        List<? extends Subtype> getSubtypes();

        /**
         * Gets direct subinterfaces of this interface.
         */
        List<? extends InterfaceType> getSubInterfaces();

        /**
         * Sets the to-be-generated common methods. Should only be called at most once.
         */
        void setCommonMethods(Collection<GeneratedMethod> commonMethods);
    }

    /**
     * Subtype of an {@link InterfaceType}. Can itself be an {@code InterfaceType} as well.
     */
    public interface Subtype {
        /**
         * Gets the methods which will be generated for this type.
         */
        List<GeneratedMethod> getGeneratedMethods(CodeGenHelper codeGenHelper);
    }

    /**
     * Determines the custom methods to generate for the interfaces, and adds them using
     * {@link InterfaceType#setCommonMethods(Collection)}.
     */
    public static void addCommonMethods(List<? extends InterfaceType> interfaces, CodeGenHelper codeGenHelper) {
        // Perform topological sorting so that leaf subtypes are processed first and common methods are propagated,
        // e.g. when for an interface all its subinterfaces have a common method then the interface gets that
        // common method as well, and so on
        var sortedInterfaces = new TopoSorter<InterfaceType>(InterfaceType::getSubInterfaces).sort(interfaces).reversed();

        /**
         * Signature for a common method candidate.
         *
         * @param kind
         *      Kind of the method; including this here avoids generating common methods when methods of different
         *      kinds just have the same signature by accident, without actually being related
         */
        record CommonMethodSignature(GeneratedMethod.Signature s, GeneratedMethod.Kind kind) {
        }

        var methodsToGenerate = new LinkedHashMap<InterfaceType, List<GeneratedMethod>>();

        for (var i : sortedInterfaces) {
            // Map of common method candidates to the union of their required return types
            SequencedMap<CommonMethodSignature, SequencedSet<GeneratedMethod.@Nullable ReturnType>> commonMethodsCandidates = null;

            for (var subtype : i.getSubtypes()) {
                var methods = new ArrayList<>(subtype.getGeneratedMethods(codeGenHelper));
                // Add to-be-generated common methods of subinterfaces which were determined in a previous iteration (if any)
                if (subtype instanceof InterfaceType subinterface) {
                    methods.addAll(methodsToGenerate.getOrDefault(subinterface, Collections.emptyList()));
                }

                if (commonMethodsCandidates == null) {
                    commonMethodsCandidates = new LinkedHashMap<>();
                    for (var m : methods) {
                        var returnTypes = new LinkedHashSet<GeneratedMethod.@Nullable ReturnType>();
                        returnTypes.add(m.returnType());
                        var old = commonMethodsCandidates.putIfAbsent(new CommonMethodSignature(m.signature(), m.kind()), returnTypes);
                        if (old != null) {
                            throw new IllegalStateException("Duplicate method " + m + " for " + subtype);
                        }
                    }
                } else {
                    var methodsToRemove = new HashSet<>(commonMethodsCandidates.keySet());

                    for (var m : methods) {
                        var methodSignature = new CommonMethodSignature(m.signature(), m.kind());

                        if (!methodsToRemove.remove(methodSignature)) {
                            // Method does not exist for previous subtypes; continue because it is not a possible candidate
                            continue;
                        }

                        commonMethodsCandidates.get(methodSignature).add(m.returnType());
                    }

                    // Remove all candidates which did not exist for this subtype, and are therefore not valid candidates
                    commonMethodsCandidates.keySet().removeAll(methodsToRemove);
                }
            }

            if (commonMethodsCandidates != null) {
                var methods = new ArrayList<GeneratedMethod>();

                for (var methodEntry : commonMethodsCandidates.entrySet()) {
                    CommonMethodSignature signature = methodEntry.getKey();
                    var returnTypes = methodEntry.getValue();
                    GeneratedMethod.ReturnType commonReturnType;
                    if (returnTypes.size() == 1 && returnTypes.getFirst() == null) {
                        // Common return type is `void`
                        commonReturnType = null;
                    } else {
                        //noinspection NullableProblems; `returnTypes` does not contain null due to previous `if` branch
                        commonReturnType = GeneratedMethod.ReturnType.getCommonReturnType(returnTypes);
                        if (commonReturnType == null) {
                            continue;
                        }
                    }

                    methods.add(new GeneratedMethod(signature.kind(), signature.s(), commonReturnType));
                }

                if (!methods.isEmpty()) {
                    methodsToGenerate.put(i, methods);
                }
            }

        }

        /*
         * Note: Could maybe enhance this by
         * - removing common method if same common method was also propagated to superinterface
         *   -> not doing this for now; probably not worth the additional complexity and having both methods might
         *      make it easier to discover for users
         * - marking methods in subtypes as @Override (for common methods, and existing generated methods)
         *   -> not doing this for now; would be nice to have but probably not worth the additional complexity
         *      Since these common methods are abstract, it would lead to a compilation error anyway if a subclass
         *      does not implement them for whatever reason
         */

        /*
         * Note: In theory there could be clashes between common methods and custom methods (e.g. if supertype also
         * has an existing custom method with the same signature), so maybe conflicting common methods should be ignored.
         * On the other hand those existing custom methods would be overridden by all subtypes, so their implementations
         * would never be called. Therefore, for now assume that it is unlikely that the user defines such conflicting
         * custom methods.
         */

        methodsToGenerate.forEach(InterfaceType::setCommonMethods);
    }
}
