package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

import com.palantir.javapoet.ClassName;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.TypeNameCreator;

import java.util.List;
import java.util.function.Predicate;

/**
 * Config for the 'typed query' code generation. Provides class and member names to be used in the generated code.
 */
// TODO: Rename the `ClassName name()` methods here to `className()` for consistency with the CodeGenHelper config classes?
class TypedQueryConfig {
    private final ClassName name;

    public TypedQueryConfig(TypeNameCreator typeNameCreator) {
        this.name = typeNameCreator.createOwnClassName("TypedQuery");
    }

    public ClassName name() {
        return name;
    }

    /**
     * Method for executing the {@code TypedQuery} and obtaining its matches.
     */
    public String methodFindMatches() {
        return "findMatches";
    }

    /**
     * Method for executing the {@code TypedQuery} and directly collecting captured nodes from the matches.
     */
    public String methodFindMatchesAndCollect() {
        return "findMatchesAndCollect";
    }

    public TypedQueryMatchConfig typedQueryMatchConfig() {
        return new TypedQueryMatchConfig();
    }

    /**
     * Config for the class {@code TypedQueryMatch}, the result of {@link #methodFindMatches()}.
     */
    public class TypedQueryMatchConfig {
        private TypedQueryMatchConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("TypedQueryMatch");
        }

        /**
         * Method for collecting all captured typed nodes.
         */
        public String methodCollectCaptures() {
            return "collectCaptures";
        }

        /**
         * Method for getting the underlying jtreesitter {@code QueryMatch}.
         */
        public String methodGetQueryMatch() {
            return "getQueryMatch";
        }
    }

    public CaptureHandlerConfig captureHandlerConfig() {
        return new CaptureHandlerConfig();
    }

    /**
     * Config for the interface {@code CaptureHandler}, which is implemented by users to handle a query match capture.
     */
    public class CaptureHandlerConfig {
        private CaptureHandlerConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("CaptureHandler");
        }

        /**
         * Abstract method implemented by users to handle a capture.
         */
        public String methodHandleCapture() {
            return "handleCapture";
        }
    }

    public CaptureRegistryConfig captureRegistryConfig() {
        return new CaptureRegistryConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code CaptureRegistry}, which is used by the query builder for registering
     * capture handlers, and once the query was built and is executed, for processing the underlying jtreesitter
     * query captures and invoking the corresponding user defined capture handlers.
     */
    public class CaptureRegistryConfig {
        private CaptureRegistryConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("CaptureRegistry");
        }

        public String methodRegisterHandler() {
            return "registerHandler";
        }

        public String methodHasNoHandlers() {
            return "hasNoHandlers";
        }

        public String methodInvokeHandler() {
            return "invokeHandler";
        }
    }

    public PredicateRegistryConfig predicateRegistryConfig() {
        return new PredicateRegistryConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code PredicateRegistry}, which is used by the query builder for
     * registering query predicates, and once the query was built and is executed, for forwarding the jtreesitter
     * predicate checks to the registered predicates.
     */
    public class PredicateRegistryConfig {
        private PredicateRegistryConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("PredicateRegistry");
        }

        public String methodRegisterCustomPredicate() {
            return "registerCustomPredicate";
        }

        /**
         * Method for requesting a unique capture name for the usage of a built-in tree-sitter query (e.g. {@code #eq?}).
         */
        public String methodRequestBuiltInQueryCapture() {
            return "requestBuiltInQueryCapture";
        }

        /**
         * Method for looking up and executing the custom predicate for a jtreesitter parsed predicate.
         */
        public String methodTest() {
            return "test";
        }
    }

    public QNodeConfig qNodeConfig() {
        return new QNodeConfig();
    }

    /**
     * Config for the interface {@code QNode}, which is the base type for all 'typed query' builder classes.
     */
    public class QNodeConfig {
        private QNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QNode");
        }

        /**
         * Method for building a {@code TypedQuery} class from a {@code QNode} object.
         */
        public String methodBuildQuery() {
            return "buildQuery";
        }
    }

    public QNodeImplConfig qNodeImplConfig() {
        return new QNodeImplConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code QNodeImpl}, which is the internal base implementation of
     * {@link QNodeConfig QNode}.
     */
    public class QNodeImplConfig {
        private QNodeImplConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QNodeImpl");
        }

        /**
         * Internal method which casts a {@code QNode} to a {@code QNodeImpl}.
         */
        public String methodFromNode() {
            return "from";
        }

        /**
         * Internal method which converts multiple {@code QNode} to a {@link List} of {@code QNodeImpl}.
         */
        public String methodListOf() {
            return "listOf";
        }

        /**
         * Internal method which verifies that the query node is in a valid state, and would not produce an invalid query string.
         * This method exists for query nodes which are built incrementally (e.g. {@code QTypedNode} subclasses, with
         * their children and fields) where the API cannot prevent all incorrect usages.
         *
         * <p>This method should be called as early as possible, e.g. when adding a query node as subnode (e.g. as child)
         * to another node, or at last when the query string is being built.
         */
        public String methodVerifyValidState() {
            return "verifyValidState";
        }

        /**
         * Internal method which creates a tree-sitter query string literal from a given string.
         */
        public String methodCreateStringLiteral() {
            return "createStringLiteral";
        }

        /**
         * Abstract internal method which is the actual implementation of the typed query creation.
         */
        public String methodBuildQueryImpl() {
            return "buildQueryImpl";
        }
    }

    public QQuantifiableConfig qQuantifiableConfig() {
        return new QQuantifiableConfig();
    }

    /**
     * Config for the class {@code QQuantifiable}, which is a builder class for quantifiable nodes.
     *
     * @see QQuantifiedConfig
     */
    public class QQuantifiableConfig {
        private QQuantifiableConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QQuantifiable");
        }

        /**
         * Method for creating a quantified "zero or more" query builder node.
         */
        public String methodZeroOrMore() {
            return "zeroOrMore";
        }

        /**
         * Method for creating a quantified "one or more" query builder node.
         */
        public String methodOneOrMore() {
            return "oneOrMore";
        }

        /**
         * Method for creating a quantified "optional" query builder node.
         */
        public String methodOptional() {
            return "optional";
        }
    }

    public QQuantifiedConfig qQuantifiedConfig() {
        return new QQuantifiedConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code QQuantified}, which is a builder class for a quantified node.
     *
     * @see QQuantifiableConfig
     */
    public class QQuantifiedConfig {
        private QQuantifiedConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QQuantified");
        }

        // Define these internal field names here because `QCapturableQuantified` has to access them
        /**
         * Internal field storing the underlying quantified node.
         */
        public String fieldNode() {
            return "node";
        }

        /**
         * Internal field storing the quantifier char as used by tree-sitter, e.g. {@code '+'} for "one or more".
         */
        public String fieldQuantifier() {
            return "quantifier";
        }
    }

    /** Class {@code QGroup} (<b>internal</b>), for a query group {@code (...)}. */
    public ClassName classQGroup() {
        return TypedQueryConfig.this.name().nestedClass("QGroup");
    }

    /**
     * Class {@code QAlternation} (<b>internal</b>), for a query alternation {@code [...]}.
     *
     * @param typed whether this is for a query node of {@code <N extends TypedNode>} ({@code true}) or just {@code <N>} ({@code false})
     */
    public ClassName classQAlternation(boolean typed) {
        return TypedQueryConfig.this.name().nestedClass(typed ? "QAlternationTyped" : "QAlternation");
    }

    public QUnnamedNodeConfig qUnnamedNodeConfig() {
        return new QUnnamedNodeConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code QUnnamedNode}, which is a builder class for an unnamed
     * node {@code "..."} (optionally with supertype).
     */
    public class QUnnamedNodeConfig {
        private QUnnamedNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QUnnamedNode");
        }

        public String methodCheckUnnamedNodeType() {
            return "checkUnnamedNodeType";
        }
    }

    public QWildcardNodeConfig qWildcardNodeConfig() {
        return new QWildcardNodeConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code QWildcardNode}, which is a builder class for a wildcard node.
     */
    public class QWildcardNodeConfig {
        private QWildcardNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QWildcardNode");
        }

        /**
         * Java constant field for the singleton 'named' wildcard node.
         */
        public String constantNamed() {
            return "NAMED";
        }

        /**
         * Java constant field for the singleton 'named' or 'unnamed' wildcard node.
         */
        public String constantNamedOrUnnamed() {
            return "NAMED_OR_UNNAMED";
        }
    }

    public QErrorNodeConfig qErrorNodeConfig() {
        return new QErrorNodeConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code QErrorNode}, which is a builder class for an error node.
     */
    public class QErrorNodeConfig {
        private QErrorNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QErrorNode");
        }

        /**
         * Java constant field for the singleton instance.
         */
        public String constantInstance() {
            return "INSTANCE";
        }
    }

    public QMissingNodeConfig qMissingNodeConfig() {
        return new QMissingNodeConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code QMissingNode}, which is a builder class for a missing node.
     */
    public class QMissingNodeConfig {
        private QMissingNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QMissingNode");
        }

        /**
         * Java constant field for the singleton instance matching any missing node.
         */
        public String constantAny() {
            return "ANY";
        }
    }

    public QFilterableConfig qFilterableConfig() {
        return new QFilterableConfig();
    }

    /**
     * Config for the sealed interface {@code QFilterable}, which is a builder class for a typed node for which
     * query predicates can be applied.
     *
     * @see QFilteredConfig
     */
    public class QFilterableConfig {
        private QFilterableConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QFilterable");
        }

        /**
         * Method for the built-in predicate {@code #eq?}.
         */
        public String methodTextEq() {
            return "textEq";
        }

        /**
         * Method for the built-in predicate {@code #not-eq?}.
         */
        public String methodTextNotEq() {
            return "textNotEq";
        }

        /**
         * Method for the built-in predicate {@code #any-of?}.
         */
        public String methodTextAnyOf() {
            return "textAnyOf";
        }

        /**
         * Method for registering a custom user-defined {@link Predicate}.
         */
        public String methodMatching() {
            return "matching";
        }
    }

    public QFilteredConfig qFilteredConfig() {
        return new QFilteredConfig();
    }

    /**
     * Config for the <b>internal</b> class {@code QFiltered}, which is a builder class for a node on which
     * a predicate has been applied.
     *
     * @see QFilterableConfig
     */
    public class QFilteredConfig {
        private QFilteredConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QFiltered");
        }

        /**
         * Internal nested class representing a tree-sitter built-in predicate.
         */
        public ClassName classBuiltinPredicate() {
            return name().nestedClass("BuiltinPredicate");
        }

        /**
         * Internal nested class representing a custom user-defined {@link Predicate}.
         */
        public ClassName classCustomPredicate() {
            return name().nestedClass("CustomPredicate");
        }
    }

    public QCapturableConfig qCapturableConfig() {
        return new QCapturableConfig();
    }

    /**
     * Config for the sealed interface {@code QCapturable}, which is a builder class which supports capturing
     * a typed node.
     *
     * @see #classQCaptured()
     */
    public class QCapturableConfig {
        private QCapturableConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QCapturable");
        }

        /**
         * Method for registering a capture handler which is called when the query is executed and the query
         * node captures a node.
         *
         * @see CaptureHandlerConfig
         */
        public String methodCaptured() {
            return "captured";
        }
    }

    /**
     * Class {@code QCaptured} (<b>internal</b>), for a query node which specifies a capture.
     *
     * @see QCapturableConfig
     */
    public ClassName classQCaptured() {
        return TypedQueryConfig.this.name().nestedClass("QCaptured");
    }

    /**
     * Class {@code QCapturableQuantifiable}, for a query node which is both capturable and quantifiable.
     *
     * @see QQuantifiableConfig
     * @see QCapturableConfig
     */
    public ClassName classQCapturableQuantifiable() {
        return TypedQueryConfig.this.name().nestedClass("QCapturableQuantifiable");
    }

    /**
     * Class {@code QCapturableQuantified} (<b>internal</b>), for a query node which has been quantified but
     * is still capturable.
     *
     * @see QQuantifiedConfig
     * @see QCapturableConfig
     */
    public ClassName classQCapturableQuantified() {
        return TypedQueryConfig.this.name().nestedClass("QCapturableQuantified");
    }

    public QTypedNodeConfig qTypedNodeConfig() {
        return new QTypedNodeConfig();
    }

    /**
     * Config for the class {@code QTypedNode}, which is the base builder class for all builder classes for the
     * typed nodes (derived from the {@code node-types.json}).
     */
    public class QTypedNodeConfig {
        private QTypedNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QTypedNode");
        }

        /**
         * Internal interface for any kind of node child entry.
         */
        public ClassName classChildEntry() {
            return name().nestedClass("ChildEntry");
        }

        /**
         * Internal class representing a node child; implements {@link #classChildEntry()}.
         */
        public ClassName classChild() {
            return name().nestedClass("Child");
        }

        /**
         * Internal class representing a node field; implements {@link #classChildEntry()}.
         */
        public ClassName classField() {
            return name().nestedClass("Field");
        }

        /**
         * Internal class representing a node child anchor (that is, {@code '.'}); implements {@link #classChildEntry()}.
         */
        public ClassName classAnchor() {
            return name().nestedClass("Anchor");
        }

        /**
         * Internal field storing the children and fields data specified for the query node.
         *
         * @see DataConfig
         */
        // Field is accessed by subclasses when creating a copy with modified data
        public String fieldData() {
            return "data";
        }

        // Methods implemented by QTypedNode subclasses
        /**
         * Method for adding required matching children to the query node.
         */
        public String methodWithChildren() {
            return "withChildren";
        }

        /**
         * Method for adding a child anchor ({@code '.'}) to the query node.
         */
        public String methodWithChildAnchor() {
            return "withChildAnchor";
        }

        /**
         * Method for treating a node as an 'extra' node which can appear anywhere in the parse tree
         * (and in the query).
         */
        public String methodAsExtra() {
            return "asExtra";
        }

        public DataConfig dataConfig() {
            return new DataConfig();
        }

        /**
         * Internal class storing the children and fields data specified for a query node.
         *
         * @see QTypedNodeConfig#fieldData()
         */
        public class DataConfig {
            private DataConfig() {
            }

            public ClassName name() {
                return QTypedNodeConfig.this.name().nestedClass("Data");
            }

            public String methodWithChildren() {
                return "withChildren";
            }

            public String methodWithField() {
                return "withField";
            }

            public String methodWithoutField() {
                return "withoutField";
            }

            public String methodWithAnchor() {
                return "withAnchor";
            }

            /**
             * @see QNodeImplConfig#methodVerifyValidState()
             */
            public String methodVerifyValidState() {
                return "verifyValidState";
            }

            public String methodBuildQuery() {
                return "buildQuery";
            }
        }
    }

    public BuilderConfig builderConfig() {
        return new BuilderConfig();
    }

    /**
     * Class which provides all the convenience builder methods to the user.
     */
    public class BuilderConfig {
        private BuilderConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("Builder");
        }

        /**
         * Method for obtaining a builder object for an unnamed node.
         *
         * @see QUnnamedNodeConfig
         */
        public String methodUnnamedNode() {
            return "unnamedNode";
        }

        /**
         * Method for obtaining a builder object for a wildcard 'named' node.
         *
         * @see QWildcardNodeConfig
         */
        public String methodAnyNamedNode() {
            return "anyNamedNode";
        }

        /**
         * Method for obtaining a builder object for a wildcard 'named' or 'unnamed' node (= any node).
         *
         * @see QWildcardNodeConfig
         */
        public String methodAnyNode() {
            return "anyNode";
        }

        /**
         * Method for obtaining a builder object for an error node.
         *
         * @see QErrorNodeConfig
         */
        public String methodErrorNode() {
            return "errorNode";
        }

        /**
         * Method for obtaining a builder object for a missing node.
         *
         * @see QMissingNodeConfig
         */
        public String methodMissingNode() {
            return "missingNode";
        }

        /**
         * Method for obtaining a builder object for a node group.
         *
         * @see #classQGroup()
         */
        public String methodGroup() {
            return "group";
        }

        /**
         * Method for obtaining a builder object for a node alternation.
         *
         * @see #classQAlternation(boolean)
         */
        public String methodAlternation() {
            return "alternation";
        }
    }
}
