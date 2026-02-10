package marcono1234.jtreesitter.type_gen.internal.gen;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.ReturnType;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.Signature;
import marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.SupertypesResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Stream;

import static marcono1234.jtreesitter.type_gen.internal.gen.GeneratedMethod.SimpleKind.CUSTOM_METHOD;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class GeneratedMethodTest {
    static Stream<Arguments> methodSpecMatches() {
        return Stream.of(
            argumentSet("simple",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature("name"),
                    null
                ),
                MethodSpec.methodBuilder("name").build()
            ),
            argumentSet("with param",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature(
                        "name",
                        List.of(),
                        List.of(new Signature.Parameter("a", TypeName.INT))
                    ),
                    null
                ),
                MethodSpec.methodBuilder("name")
                    .addParameter(TypeName.INT, "a")
                    .build()
            ),
            argumentSet("with type var",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature(
                        "name",
                        List.of(TypeVariableName.get("T")),
                        List.of(new Signature.Parameter("a", TypeName.INT))
                    ),
                    null
                ),
                MethodSpec.methodBuilder("name")
                    .addTypeVariable(TypeVariableName.get("T"))
                    .addParameter(TypeName.INT, "a")
                    .build()
            ),
            argumentSet("with return type",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature(
                        "name",
                        List.of(TypeVariableName.get("T")),
                        List.of(new Signature.Parameter("a", TypeName.INT))
                    ),
                    new ReturnType(TypeName.BOOLEAN, SupertypesResolver.EMPTY)
                ),
                MethodSpec.methodBuilder("name")
                    .returns(TypeName.BOOLEAN)
                    .addTypeVariable(TypeVariableName.get("T"))
                    .addParameter(TypeName.INT, "a")
                    .build()
            )
        );
    }

    @ParameterizedTest
    @MethodSource("methodSpecMatches")
    void matchesMethodSpec_Match(GeneratedMethod method, MethodSpec methodSpec) {
        assertTrue(method.matchesMethodSpec(methodSpec));
    }

    static Stream<Arguments> methodSpecNoMatches() {
        return Stream.of(
            argumentSet("wrong name",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature("name"),
                    null
                ),
                MethodSpec.methodBuilder("otherName").build()
            ),
            argumentSet("wrong param",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature(
                        "name",
                        List.of(),
                        List.of(new Signature.Parameter("a", TypeName.INT))
                    ),
                    null
                ),
                MethodSpec.methodBuilder("name")
                    .addParameter(TypeName.FLOAT, "a")
                    .build()
            ),
            argumentSet("wrong type var",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature(
                        "name",
                        List.of(TypeVariableName.get("T")),
                        List.of()
                    ),
                    null
                ),
                MethodSpec.methodBuilder("name")
                    .addTypeVariable(TypeVariableName.get("A"))
                    .build()
            ),
            argumentSet("wrong return type",
                new GeneratedMethod(CUSTOM_METHOD,
                    new Signature(
                        "name",
                        List.of(),
                        List.of()
                    ),
                    new ReturnType(TypeName.BOOLEAN, SupertypesResolver.EMPTY)
                ),
                MethodSpec.methodBuilder("name")
                    .returns(TypeName.INT)
                    .build()
            )
        );
    }

    @ParameterizedTest
    @MethodSource("methodSpecNoMatches")
    void matchesMethodSpec_NoMatch(GeneratedMethod method, MethodSpec methodSpec) {
        assertFalse(method.matchesMethodSpec(methodSpec));
    }

    @Test
    void createCommonInterfaceMethodSpec() {
        var method = new GeneratedMethod(CUSTOM_METHOD,
            new Signature(
                "name",
                List.of(TypeVariableName.get("T")),
                List.of(new Signature.Parameter("a", TypeName.INT))
            ),
            new ReturnType(TypeName.BOOLEAN, SupertypesResolver.EMPTY)
        );

        var methodSpec = method.createCommonInterfaceMethodSpec();
        assertEquals(Set.of(Modifier.PUBLIC, Modifier.ABSTRACT), methodSpec.modifiers());
        assertEquals(method.signature().methodName(), methodSpec.name());
        assertEquals(method.signature().typeVariables(), methodSpec.typeVariables());
        assertEquals(method.returnTypeOrVoid(), methodSpec.returnType());
        assertEquals(
            method.signature().parameters().stream()
                .map(Signature.Parameter::toParamSpec)
                .toList(),
            methodSpec.parameters()
        );

        assertEquals(
            "Custom method.\n\n<p>This is a method common to all subtypes; see their implementations for details.",
            methodSpec.javadoc().toString()
        );
    }

    /**
     * Tests for {@link GeneratedMethod.SupertypesResolver}
     */
    @Nested
    class SupertypesResolverTest {
        @Test
        void empty() {
            assertNull(SupertypesResolver.EMPTY.getSupertypes(ClassName.get(List.class)));
        }
    }

    /**
     * Tests for {@link GeneratedMethod.ReturnType}
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)  // for @MethodSource usage below
    class ReturnTypeTest {
        private ReturnType asReturnType(TypeName type) {
            return new ReturnType(type, SupertypesResolver.EMPTY);
        }
        
        Stream<Arguments> commonSimple() {
            var className = ClassName.get("example", "C");
            var ann = AnnotationSpec.builder(ClassName.get("a", "A")).build();

            return Stream.of(
                argumentSet("single primitive",
                    List.of(TypeName.BOOLEAN),
                    TypeName.BOOLEAN
                ),
                argumentSet("multiple primitive",
                    List.of(TypeName.BOOLEAN, TypeName.BOOLEAN),
                    TypeName.BOOLEAN
                ),
                argumentSet("multiple primitive (same annotations)",
                    List.of(TypeName.BOOLEAN.annotated(ann), TypeName.BOOLEAN.annotated(ann)),
                    TypeName.BOOLEAN.annotated(ann)
                ),
                argumentSet("single class",
                    List.of(className),
                    className
                ),
                argumentSet("multiple classes",
                    List.of(className, className),
                    className
                ),
                argumentSet("multiple classes (same annotations)",
                    List.of(className.annotated(ann), className.annotated(ann)),
                    className.annotated(ann)
                ),
                argumentSet("single parameterized",
                    List.of(ParameterizedTypeName.get(List.class, String.class)),
                    ParameterizedTypeName.get(List.class, String.class)
                ),
                argumentSet("multiple parameterized",
                    List.of(ParameterizedTypeName.get(List.class, String.class), ParameterizedTypeName.get(List.class, String.class)),
                    ParameterizedTypeName.get(List.class, String.class)
                ),
                argumentSet("multiple parameterized wildcard (first)",
                    List.of(
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class)),
                        ParameterizedTypeName.get(List.class, String.class)
                    ),
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class))
                ),
                argumentSet("multiple parameterized wildcard (second)",
                    List.of(
                        ParameterizedTypeName.get(List.class, String.class),
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class))
                    ),
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class))
                ),
                argumentSet("multiple parameterized wildcard type var (second)",
                    List.of(
                        ParameterizedTypeName.get(ClassName.get(List.class), TypeVariableName.get("T")),
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(TypeVariableName.get("T")))
                    ),
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(TypeVariableName.get("T")))
                ),
                argumentSet("multiple parameterized (annotated) wildcard",
                    List.of(
                        ParameterizedTypeName.get(List.class, String.class).annotated(ann),
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class)).annotated(ann)
                    ),
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class)).annotated(ann)
                ),
                argumentSet("multiple parameterized wildcard (both)",
                    List.of(
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class)),
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class))
                    ),
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class))
                ),
                argumentSet("multiple parameterized wildcard (annotated)",
                    List.of(
                        ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class).annotated(ann)),
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(ClassName.get(String.class).annotated(ann))),
                        // Have third return type here to verify that it is compared properly against type choices of previous
                        ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(ClassName.get(String.class).annotated(ann)))
                    ),
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(ClassName.get(String.class).annotated(ann)))
                ),
                argumentSet("single type var",
                    List.of(TypeVariableName.get("T")),
                    TypeVariableName.get("T")
                ),
                argumentSet("multiple type var",
                    List.of(TypeVariableName.get("T"), TypeVariableName.get("T")),
                    TypeVariableName.get("T")
                )
            );
        }

        @ParameterizedTest
        @MethodSource("commonSimple")
        void getCommonReturnType_CommonSimple(List<TypeName> returnTypes, TypeName expectedReturnType) {
            var commonReturnType = ReturnType.getCommonReturnType(returnTypes.stream().map(this::asReturnType).toList());
            assertNotNull(commonReturnType);
            assertEquals(expectedReturnType, commonReturnType.type());
        }

        Stream<Arguments> noCommonSimple() {
            var class1 = ClassName.get("example", "C1");
            var class2 = ClassName.get("example", "C2");
            var ann = AnnotationSpec.builder(ClassName.get("a", "A")).build();

            return Stream.of(
                argumentSet("different primitives", List.of(
                    TypeName.BOOLEAN,
                    TypeName.INT
                )),
                argumentSet("different classes", List.of(
                    class1,
                    class2
                )),
                argumentSet("different annotated", List.of(
                    class1,
                    class1.annotated(ann)
                )),
                argumentSet("class and parameterized", List.of(
                    class1,
                    ParameterizedTypeName.get(List.class, String.class)
                )),
                argumentSet("different parameterized (raw)", List.of(
                    ParameterizedTypeName.get(List.class, String.class),
                    ParameterizedTypeName.get(Optional.class, String.class)
                )),
                argumentSet("different parameterized (raw annotated)", List.of(
                    ParameterizedTypeName.get(List.class, String.class),
                    ParameterizedTypeName.get(List.class, String.class).annotated(ann)
                )),
                argumentSet("different parameterized (type arg)", List.of(
                    ParameterizedTypeName.get(List.class, Integer.class),
                    ParameterizedTypeName.get(List.class, String.class)
                )),
                argumentSet("different parameterized (type arg annotated)", List.of(
                    ParameterizedTypeName.get(List.class, String.class),
                    ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class).annotated(ann))
                )),
                argumentSet("different parameterized wildcard", List.of(
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(String.class)),
                    ParameterizedTypeName.get(ClassName.get(List.class), WildcardTypeName.subtypeOf(Integer.class))
                )),
                argumentSet("class and type var", List.of(
                    class1,
                    TypeVariableName.get("T")
                )),
                argumentSet("different type var", List.of(
                    TypeVariableName.get("T1"),
                    TypeVariableName.get("T2")
                ))
            );
        }

        @ParameterizedTest
        @MethodSource("noCommonSimple")
        void getCommonReturnType_NoCommonSimple(List<TypeName> returnTypes) {
            var commonReturnType = ReturnType.getCommonReturnType(returnTypes.stream().map(this::asReturnType).toList());
            assertNull(commonReturnType);
        }

        @SafeVarargs
        private static <E> SequencedSet<E> seqSet(E... elements) {
            return new LinkedHashSet<>(Arrays.asList(elements));
        }

        @Test
        void getCommonReturnType_Supertypes() {
            var c1 = ClassName.get("", "C1");
            var c2 = ClassName.get("", "C2");
            var c3 = ClassName.get("", "C3");

            var returnTypes = List.of(
                new ReturnType(c1, Map.of(c1, seqSet(c2, c3))::get),
                new ReturnType(c2, Map.of(c2, seqSet(c3))::get),
                new ReturnType(c3, SupertypesResolver.EMPTY)
            );

            var commonReturnType = ReturnType.getCommonReturnType(returnTypes);
            assertNotNull(commonReturnType);
            @SuppressWarnings("UnnecessaryLocalVariable")
            var expectedReturnType = c3;
            assertEquals(expectedReturnType, commonReturnType.type());

            // Repeat with reversed order
            commonReturnType = ReturnType.getCommonReturnType(returnTypes.reversed());
            assertNotNull(commonReturnType);
            assertEquals(expectedReturnType, commonReturnType.type());
        }

        @Test
        void getCommonReturnType_Supertypes_Annotated() {
            var c1 = ClassName.get("", "C1");
            var c2 = ClassName.get("", "C2");
            var c3 = ClassName.get("", "C3");
            var ann = AnnotationSpec.builder(ClassName.get("a", "A")).build();

            var returnTypes = List.of(
                new ReturnType(c1.annotated(ann), Map.of(c1, seqSet(c2, c3))::get),
                new ReturnType(c2.annotated(ann), Map.of(c2, seqSet(c3))::get),
                new ReturnType(c3.annotated(ann), SupertypesResolver.EMPTY)
            );

            var commonReturnType = ReturnType.getCommonReturnType(returnTypes);
            assertNotNull(commonReturnType);
            var expectedReturnType = c3.annotated(ann);
            assertEquals(expectedReturnType, commonReturnType.type());

            // Repeat with reversed order
            commonReturnType = ReturnType.getCommonReturnType(returnTypes.reversed());
            assertNotNull(commonReturnType);
            assertEquals(expectedReturnType, commonReturnType.type());
        }

        @Test
        void getCommonReturnType_SupertypesMutual() {
            var c1 = ClassName.get("", "C1");
            var c2 = ClassName.get("", "C2");
            var c22 = ClassName.get("", "C22");
            var superMutual = ClassName.get("", "S1");
            var superSuperMutual = ClassName.get("", "S2");

            var returnTypes = List.of(
                // c1 -> superMutual -> superSuperMutual
                new ReturnType(c1, Map.of(c1, seqSet(superMutual, superSuperMutual))::get),
                // c22 -> c2 -> superMutual -> superSuperMutual
                new ReturnType(c22, Map.of(c22, seqSet(c2, superMutual, superSuperMutual))::get)
            );

            var commonReturnType = ReturnType.getCommonReturnType(returnTypes);
            assertNotNull(commonReturnType);
            @SuppressWarnings("UnnecessaryLocalVariable")
            var expectedReturnType = superMutual;
            assertEquals(expectedReturnType, commonReturnType.type());

            // Repeat with reversed order
            commonReturnType = ReturnType.getCommonReturnType(returnTypes.reversed());
            assertNotNull(commonReturnType);
            assertEquals(expectedReturnType, commonReturnType.type());
        }

        @Test
        void getCommonReturnType_SupertypesParameterized() {
            var cList = ClassName.get(List.class);
            var c1 = ClassName.get("", "C1");
            var c2 = ClassName.get("", "C2");
            var c3 = ClassName.get("", "C3");

            var returnTypes = List.of(
                new ReturnType(ParameterizedTypeName.get(cList, c1), Map.of(c1, seqSet(c2, c3))::get),
                new ReturnType(ParameterizedTypeName.get(cList, c2), Map.of(c2, seqSet(c3))::get),
                new ReturnType(ParameterizedTypeName.get(cList, c3), SupertypesResolver.EMPTY)
            );

            var commonReturnType = ReturnType.getCommonReturnType(returnTypes);
            assertNotNull(commonReturnType);
            var expectedReturnType = ParameterizedTypeName.get(cList, WildcardTypeName.subtypeOf(c3));
            assertEquals(expectedReturnType, commonReturnType.type());

            // Repeat with reversed order
            commonReturnType = ReturnType.getCommonReturnType(returnTypes.reversed());
            assertNotNull(commonReturnType);
            assertEquals(expectedReturnType, commonReturnType.type());
        }

        @Test
        void getCommonReturnType_SupertypesParameterized_Annotated() {
            var cList = ClassName.get(List.class);
            var c1 = ClassName.get("", "C1");
            var c2 = ClassName.get("", "C2");
            var c3 = ClassName.get("", "C3");
            var ann = AnnotationSpec.builder(ClassName.get("a", "A")).build();

            var returnTypes = List.of(
                new ReturnType(ParameterizedTypeName.get(cList, c1).annotated(ann), Map.of(c1, seqSet(c2, c3))::get),
                new ReturnType(ParameterizedTypeName.get(cList, c2).annotated(ann), Map.of(c2, seqSet(c3))::get),
                new ReturnType(ParameterizedTypeName.get(cList, c3).annotated(ann), SupertypesResolver.EMPTY)
            );

            var commonReturnType = ReturnType.getCommonReturnType(returnTypes);
            assertNotNull(commonReturnType);
            var expectedReturnType = ParameterizedTypeName.get(cList, WildcardTypeName.subtypeOf(c3)).annotated(ann);
            assertEquals(expectedReturnType, commonReturnType.type());

            // Repeat with reversed order
            commonReturnType = ReturnType.getCommonReturnType(returnTypes.reversed());
            assertNotNull(commonReturnType);
            assertEquals(expectedReturnType, commonReturnType.type());
        }
    }
}
