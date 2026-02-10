package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.CustomMethodsProvider;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CommonMethodsGenerator;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A method which will be generated.
 *
 * <p>This is only intended for methods which will be dynamically generated, based on the information from the
 * grammar or for example due to a {@link CustomMethodsProvider}. It should not be used for standard methods
 * which will always be generated, e.g. {@code fromNode(...)}.
 *
 * @param kind
 *      kind of the method, respectively why the method is being generated
 * @param signature
 *      signature of the method
 * @param returnType
 *      return type of the method; {@code null} for {@code void}
 *
 * @see CommonMethodsGenerator
 */
public record GeneratedMethod(Kind kind, Signature signature, @Nullable ReturnType returnType) {
    /**
     * Kind of the generated method.
     */
    public sealed interface Kind {
    }

    /**
     * Simple {@link Kind} without additional data.
     */
    public enum SimpleKind implements Kind {
        /**
         * Kind for a custom method, specified through {@link CustomMethodsProvider}.
         */
        CUSTOM_METHOD,
    }

    // Note: For 'children' and 'field' also consider the `multiple` and `required` values, as specified in the
    // grammar. They would result in incompatible return types anyway (e.g. `List<MyNode>` vs. `@Nullable MyNode`)
    // but it is probably more efficient to directly consider them incompatible without having to compare return types.

    /**
     * {@link Kind} for the getter method of the non-field children of a node.
     */
    public record KindChildren(boolean multiple, boolean required) implements Kind {
    }

    /**
     * {@link Kind} for the getter method of a node field.
     */
    // Include `fieldName` here to prevent fields with different names which accidentally use the same
    // getter method name to get a common method
    public record KindField(String fieldName, boolean multiple, boolean required) implements Kind {
    }

    /**
     * Resolves the supertypes of a class.
     *
     * <p>A resolver should only provide 'interesting' supertypes. For example, it should not return {@code Object}
     * or {@code TypedNode} because at least in the context of 'children' and 'field' getter methods, creating
     * a common method with that as return type provides little or no benefit.
     */
    public interface SupertypesResolver {
        /**
         * Gets all the supertypes (direct and transitive) of the given class, starting with the most direct one.
         *
         * <p>{@code null} if the supertypes of the class are unknown.
         */
        @Nullable SequencedSet<ClassName> getSupertypes(ClassName c);

        /**
         * Creates a resolver which resolves the supertypes for the given node types (and their supertypes).
         */
        static SupertypesResolver forNodeTypes(Collection<? extends GenNodeType> nodeTypes, CodeGenHelper codeGenHelper) {
            var allNodeTypes = new HashSet<GenNodeType>(nodeTypes);
            // Also resolve supertypes of all the node types; this is needed when generating common method for a method
            // which itself is already a common method which uses a supertype of the original type
            nodeTypes.stream().flatMap(n -> n.getAllSupertypesTopoOrder().stream()).forEach(allNodeTypes::add);

            var typeMapping = new HashMap<ClassName, GenNodeType>();
            for (var nodeType : allNodeTypes) {
                typeMapping.put(nodeType.createJavaTypeName(codeGenHelper), nodeType);
            }

            return c -> {
                var nodeType = typeMapping.get(c);
                if (nodeType == null) {
                    return null;
                }
                return nodeType.getAllSupertypesTopoOrder().stream()
                    .map(s -> s.createJavaTypeName(codeGenHelper))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            };
        }

        SupertypesResolver EMPTY = c -> null;

        /**
         * Resolver which consists of multiple other resolvers to which it delegates.
         */
        class ResolverCollection implements SupertypesResolver {
            private final List<SupertypesResolver> resolvers;

            public ResolverCollection() {
                this.resolvers = new ArrayList<>();
            }

            public void addResolver(SupertypesResolver resolver) {
                resolvers.add(resolver);
            }

            @Override
            public @Nullable SequencedSet<ClassName> getSupertypes(ClassName c) {
                for (var resolver : resolvers) {
                    var supertypes = resolver.getSupertypes(c);
                    if (supertypes != null) {
                        return supertypes;
                    }
                }
                return null;
            }
        }
    }

    /**
     * Return type of a method.
     *
     * @param supertypesResolver
     *      resolves (some) types referenced in the return type
     */
    public record ReturnType(TypeName type, SupertypesResolver supertypesResolver) {
        // Ignore supertypesResolver for `equals` and `hashCode`
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ReturnType other) {
                return type.equals(other.type);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        /**
         * Type choices during calculation of a common return type.
         */
        private sealed interface TypeChoices {
            /**
             * Given the type choices of this object and the other type, tries to determine the common choices.
             * Returns {@code null} if there are no common choices.
             */
            @Nullable TypeChoices getResultingTypeChoices(TypeName otherType, SupertypesResolver otherSupertypesResolver);

            private static TypeName unwrapWildcard(TypeName type) {
                // Only unwrap wildcard if no information would be lost
                if (type instanceof WildcardTypeName w && w.lowerBounds().isEmpty() && w.annotations().isEmpty()) {
                    var upperBounds = w.upperBounds();
                    if (upperBounds.size() == 1) {
                        return upperBounds.getFirst();
                    }
                }

                return type;
            }

            /**
             * Gets the type choices for the node type class, that is the class itself as well as its supertypes.
             * The classes are ordered from most specific to least specific, that is {@code type} comes first
             * followed by a direct supertype, ...
             */
            static SequencedSet<TypeName> getTypeChoices(TypeName type, SupertypesResolver supertypesResolver) {
                // Caller should have removed annotations
                assert !type.isAnnotated();

                // For now only support ClassName but don't recursively try to handle parameterized type,
                // that would be more complex and is currently not needed
                if (!(type instanceof ClassName selfC)) {
                    var set = new LinkedHashSet<TypeName>();
                    set.add(type);
                    return set;
                }

                var typeChoices = new LinkedHashSet<TypeName>();
                typeChoices.add(selfC);
                var supertypes = supertypesResolver.getSupertypes(selfC);
                if (supertypes != null) {
                    typeChoices.addAll(supertypes);
                }
                return typeChoices;
            }

            /**
             * From the type choices picks the final most specific type.
             */
            TypeName getFinalType();

            /**
             * From the type choices picks the final most specific type, for usage as type argument in a
             * parameterized type.
             */
            TypeName getFinalTypeAsTypeArg();
        }

        /**
         * The original return type, without supertype choices having been determined yet.
         * This is delayed until it is actually needed, when determining the intersection with the type choices
         * of another type. In the simplest case all return types are equivalent (all {@code OriginalType}) and
         * determining common supertypes is not necessary.
         */
        private record OriginalType(TypeName type, SupertypesResolver supertypesResolver) implements TypeChoices {
            @Override
            public @Nullable TypeChoices getResultingTypeChoices(TypeName otherType, SupertypesResolver otherSupertypesResolver) {
                // If both types are identical there is no need yet to determine common supertypes
                if (type.equals(otherType)) {
                    return this;
                }

                // Unwrapping here is safe because in the end will either create `Supertypes` (which also uses wildcard)
                // or return null
                var type = TypeChoices.unwrapWildcard(this.type);
                otherType = TypeChoices.unwrapWildcard(otherType);

                var annotations = type.annotations();
                if (!annotations.equals(otherType.annotations())) {
                    return null;
                }
                type = type.withoutAnnotations();
                otherType = otherType.withoutAnnotations();

                var ownTypeChoices = TypeChoices.getTypeChoices(type, supertypesResolver);
                var otherTypeChoices = TypeChoices.getTypeChoices(otherType, otherSupertypesResolver);

                var commonTypeChoices = new LinkedHashSet<>(ownTypeChoices);
                commonTypeChoices.retainAll(otherTypeChoices);
                if (commonTypeChoices.isEmpty()) {
                    return null;
                }

                return new Supertypes(commonTypeChoices, annotations);
            }

            @Override
            public TypeName getFinalType() {
                return type;
            }

            @Override
            public TypeName getFinalTypeAsTypeArg() {
                return type;
            }
        }

        /**
         * Indicates that at least for one of the common methods the type choices only represent supertypes but not
         * the original type anymore.
         */
        private record Supertypes(SequencedSet<TypeName> supertypes, List<AnnotationSpec> annotations) implements TypeChoices {
            @Override
            public @Nullable TypeChoices getResultingTypeChoices(TypeName otherType, SupertypesResolver otherSupertypesResolver) {
                // Unwrapping here is safe because in the end will either create `Supertypes` (which also uses wildcard)
                // or return null
                otherType = TypeChoices.unwrapWildcard(otherType);

                if (!annotations.equals(otherType.annotations())) {
                    return null;
                }
                otherType = otherType.withoutAnnotations();

                var otherTypeChoices = TypeChoices.getTypeChoices(otherType, otherSupertypesResolver);
                var commonTypeChoices = new LinkedHashSet<>(supertypes);
                commonTypeChoices.retainAll(otherTypeChoices);
                if (commonTypeChoices.isEmpty()) {
                    return null;
                }

                return new Supertypes(commonTypeChoices, annotations);
            }

            @Override
            public TypeName getFinalType() {
                return supertypes.getFirst().annotated(annotations);
            }

            @Override
            public TypeName getFinalTypeAsTypeArg() {
                // Does not represent the original type anymore, so must use a `? extends ...` wildcard
                return WildcardTypeName.subtypeOf(getFinalType());
            }
        }

        /**
         * Tries to determine a common return type for the given collection of return types. The common return type
         * might be a supertype of all return types.
         *
         * <p>Returns {@code null} if no common return type could be determined.
         */
        public static @Nullable ReturnType getCommonReturnType(SequencedCollection<ReturnType> returnTypes) {
            if (returnTypes.isEmpty()) {
                throw new IllegalArgumentException("Must specify at least one return type");
            }

            // For now only support ClassName (`typeArgsChoices == null`) and ParameterizedTypeName when determining
            // common supertype
            TypeChoices rawTypeChoices = null;
            List<TypeChoices> typeArgsChoices = null;
            TypeName otherType = null;

            // Collect all supertypes resolvers in case final return type is itself later used to determine common
            // return type as well, e.g. when common method is 'inherited' by superinterface
            var allSupertypesResolvers = new SupertypesResolver.ResolverCollection();

            for (ReturnType returnType : returnTypes) {
                var supertypesResolver = returnType.supertypesResolver;
                allSupertypesResolvers.addResolver(supertypesResolver);
                var returnTypeType = returnType.type;

                switch (returnTypeType) {
                    case ClassName c -> {
                        if (typeArgsChoices != null) {
                            // Previous types were parameterized but this one is not -> no common type
                            return null;
                        }
                        if (otherType != null) {
                            // Previous types were not ClassName -> no common type
                            return null;
                        }

                        if (rawTypeChoices == null) {
                            // first entry in `returnTypes`, use type as-is
                            rawTypeChoices = new OriginalType(c, supertypesResolver);
                        } else {
                            rawTypeChoices = rawTypeChoices.getResultingTypeChoices(c, supertypesResolver);
                            if (rawTypeChoices == null) {
                                return null;
                            }
                        }
                    }
                    case ParameterizedTypeName p -> {
                        if (p.enclosingType() != null) {
                            // For now don't support parameterized types with enclosing type
                            return null;
                        }

                        if (rawTypeChoices != null && typeArgsChoices == null) {
                            // Previous types were non-parameterized but this one is -> no common type
                            return null;
                        }
                        if (otherType != null) {
                            // Previous types were non-parameterized -> no common type
                            return null;
                        }

                        var rawType = p.rawType();

                        // The following is destructuring the parameterized type into raw type and type args, therefore
                        // make sure the annotations on the parameterized type are not lost
                        // The current ParameterizedTypeName implementation actually stores the annotations for
                        // itself and the nested raw type though, so there is no need to extract them manually
                        assert p.annotations().equals(rawType.annotations());

                        if (rawTypeChoices == null) {
                            // first entry in `returnTypes`, use type as-is
                            rawTypeChoices = new OriginalType(rawType, supertypesResolver);
                            typeArgsChoices = p.typeArguments().stream()
                                .map(t -> new OriginalType(t, supertypesResolver))
                                // Collect in mutable List because entries might have to be replaced when handling next return types
                                .collect(Collectors.toCollection(ArrayList::new));
                        } else {
                            rawTypeChoices = rawTypeChoices.getResultingTypeChoices(rawType, supertypesResolver);
                            if (rawTypeChoices == null) {
                                return null;
                            }

                            // Type arguments are treated independently of raw type to allow (in theory) something like `ArrayList<Sub>` -> `List<? extends Super>`
                            // This assumes that supertype of raw type has same number of type variables, with same meaning as for raw type

                            var typeArgs = p.typeArguments();
                            if (typeArgsChoices.size() != typeArgs.size()) {
                                return null;
                            }

                            for (int i = 0; i < typeArgsChoices.size(); i++) {
                                var typeArgChoices = typeArgsChoices.get(i);
                                var typeArg = typeArgs.get(i);

                                typeArgChoices = typeArgChoices.getResultingTypeChoices(typeArg, supertypesResolver);
                                if (typeArgChoices == null) {
                                    return null;
                                }
                                typeArgsChoices.set(i, typeArgChoices);
                            }
                        }
                    }
                    // For other types don't support any custom logic for finding supertype for now
                    default -> {
                        if (rawTypeChoices != null) {
                            // Previous types were of different kind -> no common type
                            return null;
                        }

                        if (otherType == null) {
                            otherType = returnTypeType;
                        } else if (!otherType.equals(returnTypeType)) {
                            return null;
                        }
                    }
                }
            }

            if (otherType != null) {
                return new ReturnType(otherType, allSupertypesResolvers);
            }

            var rawType = rawTypeChoices.getFinalType();
            if (typeArgsChoices == null) {
                return new ReturnType(rawType, allSupertypesResolvers);
            }

            var type = ParameterizedTypeName.get(
                (ClassName) rawType,
                typeArgsChoices.stream()
                    .map(TypeChoices::getFinalTypeAsTypeArg)
                    .toArray(TypeName[]::new)
            );
            return new ReturnType(type, allSupertypesResolvers);
        }
    }

    // Note: Need to consider type variables (at least for custom methods) because common method in supertype has to
    // declare them as well
    // Note: For parameters cannot just use `SequencedMap<String, TypeName>` because its `equals` implementation does
    // not check that order is the same
    public record Signature(String methodName, List<TypeVariableName> typeVariables, List<Parameter> parameters) {
        // Alternatively could use JavaPoet's `ParameterSpec` here, but that would also include redundant information
        // such as modifiers and Javadoc, which is irrelevant for determining if signatures are compatible
        public record Parameter(String name, TypeName type) {
            public static Parameter fromParamSpec(ParameterSpec paramSpec) {
                return new Parameter(paramSpec.name(), paramSpec.type());
            }

            public ParameterSpec toParamSpec() {
                return ParameterSpec.builder(type, name).build();
            }
        }

        /**
         * Method signature with no type variables and no parameters.
         */
        public Signature(String name) {
            this(name, List.of(), List.of());
        }
    }

    public TypeName returnTypeOrVoid() {
        return returnType != null ? returnType.type() : TypeName.VOID;
    }

    /**
     * Returns whether the given {@link MethodSpec} matches this signature.
     *
     * <p>This method is mainly intended for debug assertions.
     */
    public boolean matchesMethodSpec(MethodSpec methodSpec) {
        return methodSpec.name().equals(signature.methodName())
            && methodSpec.typeVariables().equals(signature.typeVariables())
            && methodSpec.parameters().stream().map(Signature.Parameter::fromParamSpec).toList().equals(signature.parameters())
            && methodSpec.returnType().equals(returnTypeOrVoid());
    }

    /**
     * Assuming that this is a to-be-generated {@linkplain CommonMethodsGenerator common method}, creates the
     * corresponding method spec for it.
     */
    public MethodSpec createCommonInterfaceMethodSpec() {
        // Don't try to use Javadoc of common methods in subtypes; even if it was identical for all of them it might
        // have Javadoc links which are relative to the declaring class, and which would not work for this common method
        String javadoc = switch (kind) {
            case KindChildren kindChildren -> "Retrieves the children nodes.";
            case KindField kindField -> "Retrieves the nodes of field " + CodeGenHelper.createJavadocCodeTag(kindField.fieldName()) + ".";
            case SimpleKind simpleKind -> switch (simpleKind) {
                case CUSTOM_METHOD -> "Custom method.";
            };
        } + "\n\n<p>This is a method common to all subtypes; see their implementations for details.";

        return MethodSpec.methodBuilder(signature.methodName())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addTypeVariables(signature.typeVariables())
            .returns(returnTypeOrVoid())
            .addParameters(signature.parameters().stream()
                .map(Signature.Parameter::toParamSpec)
                .toList())
            .addJavadoc(javadoc)
            .build();
    }
}
