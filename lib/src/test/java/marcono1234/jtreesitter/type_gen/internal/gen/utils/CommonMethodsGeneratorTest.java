package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.KindChildren;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.ReturnType;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.SupertypesResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.SimpleKind.CUSTOM_METHOD;
import static org.junit.jupiter.api.Assertions.*;

class CommonMethodsGeneratorTest {
    static class DummyInterface implements CommonMethodsGenerator.InterfaceType, CommonMethodsGenerator.Subtype {
        private List<? extends CommonMethodsGenerator.Subtype> subtypes;
        private List<? extends CommonMethodsGenerator.InterfaceType> subInterfaces;
        private List<GeneratedMethod> generatedMethods;

        private List<GeneratedMethod> commonMethods;

        public DummyInterface() {
            this.subtypes = List.of();
            this.subInterfaces = List.of();
            this.generatedMethods = List.of();

            this.commonMethods = List.of();
        }

        public DummyInterface withSubtypes(List<? extends CommonMethodsGenerator.Subtype> subtypes) {
            this.subtypes = subtypes;
            this.subInterfaces = subtypes.stream()
                .filter(t -> t instanceof CommonMethodsGenerator.InterfaceType)
                .map(t -> (CommonMethodsGenerator.InterfaceType) t).toList();
            return this;
        }

        public DummyInterface withGeneratedMethods(List<GeneratedMethod> generatedMethods) {
            this.generatedMethods = generatedMethods;
            return this;
        }

        @Override
        public List<? extends CommonMethodsGenerator.Subtype> getSubtypes() {
            return subtypes;
        }

        @Override
        public List<? extends CommonMethodsGenerator.InterfaceType> getSubInterfaces() {
            return subInterfaces;
        }

        @Override
        public void setCommonMethods(Collection<GeneratedMethod> commonMethods) {
            if (!this.commonMethods.isEmpty()) {
                throw new IllegalStateException("Common methods have already been set");
            }
            this.commonMethods = List.copyOf(commonMethods);
        }

        public List<GeneratedMethod> getCommonMethods() {
            return commonMethods;
        }

        @Override
        public List<GeneratedMethod> getGeneratedMethods(CodeGenHelper codeGenHelper) {
            return generatedMethods;
        }
    }

    record DummyRegularSubtype(List<GeneratedMethod> methods) implements CommonMethodsGenerator.Subtype {
        @Override
        public List<GeneratedMethod> getGeneratedMethods(CodeGenHelper codeGenHelper) {
            return methods;
        }
    }

    @SafeVarargs
    private static <E> SequencedSet<E> seqSet(E... elements) {
        return new LinkedHashSet<>(Arrays.asList(elements));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addCommonMethods(boolean reverseOrder) {
        var childrenKind = new KindChildren(true, false);
        var classSuper = ClassName.get(Number.class);
        var classSub1 = ClassName.get(Integer.class);
        SupertypesResolver sub1Resolver = Map.of(classSub1, seqSet(classSuper))::get;
        var classSub2 = ClassName.get(Long.class);
        SupertypesResolver sub2Resolver = Map.of(classSub2, seqSet(classSuper))::get;

        var subtype1 = new DummyRegularSubtype(List.of(
            // common method with sibling subInterface2
            new GeneratedMethod(CUSTOM_METHOD, new GeneratedMethod.Signature("method1"), null),
            // common method, after common method is inherited from sibling subInterface2
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children1"), null),
            // common method (where supertype of return type has to be used), after common method is inherited from sibling subInterface2
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children2"), new ReturnType(classSub1, sub1Resolver))
        ));
        var subtype21 = new DummyRegularSubtype(List.of(
            new GeneratedMethod(CUSTOM_METHOD, new GeneratedMethod.Signature("method2"), null),
            // common method
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children1"), null),
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children2"), new ReturnType(classSuper, SupertypesResolver.EMPTY)),
            // not common, due to different return type
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("differentReturn"), new ReturnType(TypeName.INT, SupertypesResolver.EMPTY))
        ));
        var subtype22 = new DummyRegularSubtype(List.of(
            // should not be considered common due to different 'kind', despite having same signature
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("method2"), null),
            // common method
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children1"), null),
            // common method, but where supertype of return type has to be used
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children2"), new ReturnType(classSub2, sub2Resolver)),
            // not common, due to different return type
            new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("differentReturn"), new ReturnType(TypeName.BOOLEAN, SupertypesResolver.EMPTY))
        ));
        var subInterface2 = new DummyInterface()
            .withSubtypes(List.of(subtype21, subtype22))
            .withGeneratedMethods(List.of(
                // common method with sibling subtype1
                new GeneratedMethod(CUSTOM_METHOD, new GeneratedMethod.Signature("method1"), null)
            ));

        var rootInterface = new DummyInterface()
            .withSubtypes(List.of(subtype1, subInterface2));

        var interfaces = List.of(rootInterface, subInterface2);
        if (reverseOrder) {
            // should have no effect because implementation sorts interfaces based on subtype hierarchy
            interfaces = interfaces.reversed();
        }

        // TODO: This is wrong, but CodeGenHelper is currently simply ignored in this test setup; ideally `getGeneratedMethods` would not require CodeGenHelper
        CodeGenHelper codeGenHelper = null;
        CommonMethodsGenerator.addCommonMethods(interfaces, codeGenHelper);

        assertEquals(
            List.of(
                // common to subtype1 and subInterface2
                new GeneratedMethod(CUSTOM_METHOD, new GeneratedMethod.Signature("method1"), null),
                // common to subtype1 and subInterface2 (inherited from subtype21 and subtype22)
                new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children1"), null),
                new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children2"), new ReturnType(classSuper, SupertypesResolver.EMPTY))
            ),
            rootInterface.getCommonMethods()
        );
        assertEquals(
            List.of(
                new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children1"), null),
                new GeneratedMethod(childrenKind, new GeneratedMethod.Signature("children2"), new ReturnType(classSuper, SupertypesResolver.EMPTY))
            ),
            subInterface2.getCommonMethods()
        );
    }
}
