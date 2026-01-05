package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import com.palantir.javapoet.*;
import marcono1234.jtreesitter.type_gen.JavaType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal implementation of {@link JavaType}.
 */
public record JavaTypeImpl(TypeName type) implements JavaType {

    public static JavaTypeImpl fromTypeString(String typeString) {
        var type = new TypeStringParser(typeString).parse();
        return new JavaTypeImpl(type);
    }

    // It seems JavaPoet itself does not provide functionality to parse a type string, see https://github.com/square/javapoet/issues/940#issuecomment-1362996591
    private static class TypeStringParser {
        private final String s;
        private int i;

        public TypeStringParser(String s) {
            this.s = s;
            this.i = 0;
        }

        private static final int END = -1;

        private int c() {
            if (i < s.length()) {
                return s.charAt(i);
            }
            return END;
        }

        private boolean tryConsume(char c) {
            if (c() == c) {
                i++;
                return true;
            }
            return false;
        }

        private boolean tryConsume(String str) {
            if (str.length() > s.length() - i) {
                return false;
            }

            if (s.regionMatches(i, str, 0, str.length())) {
                i += str.length();
                return true;
            }
            return false;
        }

        private RuntimeException error(String message) {
            // Include a '^' marker for the error position
            return new IllegalArgumentException(message + "\n\t" + s + "\n\t" + " ".repeat(i) + "^");
        }

        public TypeName parse() {
            var type = parseNonWildcard(true);
            if (i != s.length()) {
                throw error("Invalid trailing character");
            }
            return type;
        }

        private TypeName parseNonWildcard(boolean isTopLevel) {
            var annotations = parseAnnotations();
            TypeName type = maybeParseTypeVariable();
            ParsedClassName parsedClassName = null;

            if (type != null) {
                if (c() == '<') {
                    throw error("Cannot specify type arguments for type variable");
                }
            } else {
                parsedClassName = parseClassName();

                if (tryConsume('<')) {
                    var className = parsedClassName.asClassName(this);
                    var typeArgs = new ArrayList<TypeName>();
                    do {
                        TypeName typeArg = maybeParseWildcard();
                        if (typeArg == null) {
                            typeArg = parseNonWildcard(false);
                        }
                        typeArgs.add(typeArg);
                    } while (tryConsume(", "));

                    if (!tryConsume('>')) {
                        throw error("Expected ', ' or '>'");
                    }

                    type = ParameterizedTypeName.get(className, typeArgs.toArray(TypeName[]::new));
                } else {
                    type = parsedClassName.asTypeName();
                }
            }

            // Apply annotations after generic type arguments (if any) were parsed
            type = type.annotated(annotations);

            boolean isArray = false;
            var allArrayAnnotations = new ArrayList<List<AnnotationSpec>>();
            while (true) {
                List<AnnotationSpec> arrayAnnotations = List.of();
                if (tryConsume(' ')) {
                    arrayAnnotations = parseAnnotations();
                    if (arrayAnnotations.isEmpty()) {
                        i--;
                        throw error("Unexpected space");
                    }
                }

                if (!tryConsume('[')) {
                    if (!arrayAnnotations.isEmpty()) {
                        throw error("Missing '[]'");
                    }
                    break;
                }
                if (!tryConsume(']')) {
                    throw error("Missing ']'");
                }

                isArray = true;
                allArrayAnnotations.add(arrayAnnotations);
            }

            if (!isArray && !isTopLevel && parsedClassName != null) {
                // Primitive types cannot be used as type arguments or wildcard bounds, and JavaPoet checks this,
                // so fail fast here (though maybe that could become valid in future Java versions related to Project Valhalla?)
                var ignored = parsedClassName.asClassName(this);
            }

            // Create array type and apply annotations; has to be done here in reverse order because that is how Java treats them, e.g. `@Component int @Outer [] @Inner [] @InnerInner []`
            // Annotations list also defines the dimensions of the array, so the individual annotation lists might be empty
            for (var arrayAnnotations : allArrayAnnotations.reversed()) {
                type = ArrayTypeName.of(type).annotated(arrayAnnotations);
            }

            return type;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private static boolean isIdentifierChar(int c) {
            // For simplicity don't differentiate between identifier 'start' and 'part' here
            return Character.isJavaIdentifierStart(c) || Character.isJavaIdentifierPart(c);
        }

        // Note: This intentionally deviates from standard Java code annotation parsing behavior where using the qualified name
        // would require placing the annotation after the package name, e.g. `java.lang.@TA Object`
        // see https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.7.4-400
        // However, that makes parsing more complicated and usage more cumbersome because user always has to use qualified names
        private List<AnnotationSpec> parseAnnotations() {
            var annotations = new ArrayList<AnnotationSpec>();

            while (tryConsume('@')) {
                var annotationClass = parseClassName().asClassName(this);
                // For now don't support annotation element values, because parsing them would be too complex,
                // and they are most likely not that often used here
                annotations.add(AnnotationSpec.builder(annotationClass).build());

                if (!tryConsume(' ')) {
                    throw error("Expected space after annotation");
                }
            }

            return annotations;
        }

        private @Nullable TypeVariableName maybeParseTypeVariable() {
            // Require that type variable names start with an uppercase letter (but don't require that they
            // are fully uppercase); this also avoids accidentally considering primitive types to be type variables
            if (!Character.isUpperCase(c())) {
                return null;
            }

            int typeNameI = i;
            while (typeNameI < s.length()) {
                int c = s.charAt(typeNameI);
                // Check if name contains '.' or '$'  and is therefore not a type variable
                if (c == '.' || c == '$') {
                    typeNameI = END;
                    break;
                }
                if (!isIdentifierChar(c)) {
                    break;
                }
                typeNameI++;
            }

            if (typeNameI == i || typeNameI == END) {
                return null;
            }

            String name = s.substring(i, typeNameI);
            i = typeNameI;
            return TypeVariableName.get(name);
        }

        private @Nullable WildcardTypeName maybeParseWildcard() {
            if (!tryConsume('?')) {
                return null;
            }

            if (tryConsume(" extends ")) {
                var upperBound = parseNonWildcard(false);
                return WildcardTypeName.subtypeOf(upperBound);
            } else if (tryConsume(" super ")) {
                var lowerBound = parseNonWildcard(false);
                return WildcardTypeName.supertypeOf(lowerBound);
            }
            return CodeGenHelper.unboundedWildcard();
        }

        sealed interface ParsedClassName {
            TypeName asTypeName();

            ClassName asClassName(TypeStringParser parser);

            record PrimitiveType(TypeName t, int startIndex) implements ParsedClassName {
                @Override
                public TypeName asTypeName() {
                    return t;
                }

                @Override
                public ClassName asClassName(TypeStringParser parser) {
                    parser.i = startIndex;
                    throw parser.error("Primitive type is not allowed here");
                }
            }

            record RegularClass(ClassName c) implements ParsedClassName {
                @Override
                public TypeName asTypeName() {
                    return c;
                }

                @Override
                public ClassName asClassName(TypeStringParser parser) {
                    return c;
                }
            }
        }

        // Note: This uses '$' to differentiate nested type names, to avoid ambiguity
        private ParsedClassName parseClassName() {
            int start = i;
            int lastDot = i - 1;
            var dollarSignPos = new ArrayList<Integer>();

            while (true) {
                int c = c();
                if (c == '.') {
                    if (i == lastDot + 1) {
                        throw error("Invalid dot");
                    }
                    if (!dollarSignPos.isEmpty()) {
                        throw error("Invalid dot in nested name");
                    }
                    lastDot = i;
                } else if (c == '$') {
                    if (!dollarSignPos.isEmpty() && i == dollarSignPos.getLast() + 1) {
                        throw error("Invalid $");
                    }
                    if (i == lastDot + 1) {
                        throw error("Missing enclosing name for nested name");
                    }
                    dollarSignPos.add(i);
                } else if (!isIdentifierChar(c)) {
                    break;
                }
                i++;
            }

            if (start == i) {
                throw error(i == s.length() ? "Empty name" : (c() == ' ' ? "Unexpected space" : "Empty name or invalid char"));
            }
            if (lastDot + 1 == i) {
                i--;
                throw error("Invalid trailing dot");
            }
            if (!dollarSignPos.isEmpty() && dollarSignPos.getLast() + 1 == i) {
                i--;
                throw error("Invalid trailing $");
            }

            String packageName = lastDot > start ? s.substring(start, lastDot) : "";
            var simpleNames = new ArrayList<String>();
            int simpleNameStart = lastDot + 1;
            for (int dollarPos : dollarSignPos) {
                simpleNames.add(s.substring(simpleNameStart, dollarPos));
                simpleNameStart = dollarPos + 1;
            }
            simpleNames.add(s.substring(simpleNameStart, i));

            if (packageName.isEmpty() && simpleNames.size() == 1) {
                return createSimpleClassName(simpleNames.getFirst(), start);
            }

            var className = ClassName.get(packageName, simpleNames.getFirst(), simpleNames.stream().skip(1).toArray(String[]::new));
            return new ParsedClassName.RegularClass(className);
        }

        private ParsedClassName createSimpleClassName(String name, int startIndex) {
            // ClassName cannot be used for primitive types
            var primitiveType = switch (name) {
                case "boolean" -> TypeName.BOOLEAN;
                case "byte" -> TypeName.BYTE;
                case "char" -> TypeName.CHAR;
                case "short" -> TypeName.SHORT;
                case "int" -> TypeName.INT;
                case "long" -> TypeName.LONG;
                case "float" -> TypeName.FLOAT;
                case "double" -> TypeName.DOUBLE;
                case "void" -> {
                    this.i = startIndex;
                    // For return type use empty Optional / null instead
                    throw error("'void' is not valid");
                }
                default -> null;
            };
            if (primitiveType != null) {
                return new ParsedClassName.PrimitiveType(primitiveType, startIndex);
            }
            return new ParsedClassName.RegularClass(ClassName.get("", name));
        }
    }
}
