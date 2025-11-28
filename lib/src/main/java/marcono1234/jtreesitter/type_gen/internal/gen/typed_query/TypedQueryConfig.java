package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

import com.palantir.javapoet.ClassName;
import marcono1234.jtreesitter.type_gen.internal.gen.utils.CodeGenHelper;

import java.util.Objects;

// TODO: Rename the `ClassName name()` methods here to `className()` for consistency with the CodeGenHelper config classes?
//   and maybe have separate `String name()` and `ClassName className()` similar to CodeGenHelper config classes?
public class TypedQueryConfig {
    private final CodeGenHelper codeGenHelper;

    public TypedQueryConfig(CodeGenHelper codeGenHelper) {
        this.codeGenHelper = Objects.requireNonNull(codeGenHelper);
    }

    public ClassName name() {
        return codeGenHelper.createOwnClassName("TypedQuery");
    }

    public String methodFindMatches() {
        return "findMatches";
    }

    public TypedQueryMatchConfig typedQueryMatchConfig() {
        return new TypedQueryMatchConfig();
    }

    public class TypedQueryMatchConfig {
        private TypedQueryMatchConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("TypedQueryMatch");
        }

        public String methodCollectCaptures() {
            return "collectCaptures";
        }

        public String methodGetQueryMatch() {
            return "getQueryMatch";
        }
    }

    public CaptureHandlerConfig captureHandlerConfig() {
        return new CaptureHandlerConfig();
    }

    public class CaptureHandlerConfig {
        private CaptureHandlerConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("CaptureHandler");
        }

        public String methodHandleCapture() {
            return "handleCapture";
        }
    }

    // TODO: This does not even need to be configurable since it is an internal generated class;
    //   just need to define these names somewhere to avoid code duplication
    public CaptureRegistryConfig captureRegistryConfig() {
        return new CaptureRegistryConfig();
    }

    public class CaptureRegistryConfig {
        private CaptureRegistryConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("CaptureRegistry");
        }

        public String methodRegisterHandler() {
            return "registerHandler";
        }

        public String methodInvokeHandler() {
            return "invokeHandler";
        }
    }

    // TODO: This does not even need to be configurable since it is an internal generated class;
    //   just need to define these names somewhere to avoid code duplication
    public PredicateRegistryConfig predicateRegistryConfig() {
        return new PredicateRegistryConfig();
    }

    public class PredicateRegistryConfig {
        private PredicateRegistryConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("PredicateRegistry");
        }

        public String methodRegister() {
            return "register";
        }

        public String methodRequestBuiltInQueryCapture() {
            return "requestBuiltInQueryCapture";
        }

        public String methodTest() {
            return "test";
        }
    }

    public QNodeConfig qNodeConfig() {
        return new QNodeConfig();
    }

    public class QNodeConfig {
        private QNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QNode");
        }

        public String methodBuildQuery() {
            return "buildQuery";
        }
    }

    // TODO: This does not even need to be configurable since it is an internal generated class;
    //   just need to define these names somewhere to avoid code duplication
    public QNodeImplConfig qNodeImplConfig() {
        return new QNodeImplConfig();
    }

    public class QNodeImplConfig {
        private QNodeImplConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QNodeImpl");
        }

        public String methodFromNode() {
            return "from";
        }

        public String methodListOf() {
            return "listOf";
        }

        public String methodCreateStringLiteral() {
            return "createStringLiteral";
        }

        public String methodBuildQueryImpl() {
            return "buildQueryImpl";
        }
    }

    public QQuantifiableConfig qQuantifiableConfig() {
        return new QQuantifiableConfig();
    }

    public class QQuantifiableConfig {
        private QQuantifiableConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QQuantifiable");
        }

        public String methodZeroOrMore() {
            return "zeroOrMore";
        }

        public String methodOneOrMore() {
            return "oneOrMore";
        }

        public String methodOptional() {
            return "optional";
        }
    }

    public QQuantifiedConfig qQuantifiedConfig() {
        return new QQuantifiedConfig();
    }

    public class QQuantifiedConfig {
        private QQuantifiedConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QQuantified");
        }

        // Define these internal field names here because `QCapturableQuantified` has to access them
        public String fieldNode() {
            return "node";
        }

        public String fieldQuantifier() {
            return "quantifier";
        }
    }

    public ClassName classQGroup() {
        return TypedQueryConfig.this.name().nestedClass("QGroup");
    }

    public ClassName classQAlternation() {
        return TypedQueryConfig.this.name().nestedClass("QAlternation");
    }

    public ClassName classQUnnamedNode() {
        return TypedQueryConfig.this.name().nestedClass("QUnnamedNode");
    }

    public QWildcardNodeConfig qWildcardNodeConfig() {
        return new QWildcardNodeConfig();
    }

    public class QWildcardNodeConfig {
        private QWildcardNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QWildcardNode");
        }

        public String constantNamed() {
            return "NAMED";
        }

        public String constantNamedOrUnnamed() {
            return "NAMED_OR_UNNAMED";
        }
    }

    public QErrorNodeConfig qErrorNodeConfig() {
        return new QErrorNodeConfig();
    }

    public class QErrorNodeConfig {
        private QErrorNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QErrorNode");
        }

        public String constantInstance() {
            return "INSTANCE";
        }
    }

    public QMissingNodeConfig qMissingNodeConfig() {
        return new QMissingNodeConfig();
    }

    public class QMissingNodeConfig {
        private QMissingNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QMissingNode");
        }

        public String constantAny() {
            return "ANY";
        }
    }

    public QFilterableConfig qFilterableConfig() {
        return new QFilterableConfig();
    }

    public class QFilterableConfig {
        private QFilterableConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QFilterable");
        }

        public String methodTextEq() {
            return "textEq";
        }

        public String methodTextNotEq() {
            return "textNotEq";
        }

        public String methodTextAnyOf() {
            return "textAnyOf";
        }

        public String methodMatching() {
            return "matching";
        }
    }

    public QFilteredConfig qFilteredConfig() {
        return new QFilteredConfig();
    }

    public class QFilteredConfig {
        private QFilteredConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QFiltered");
        }

        public ClassName classBuiltinPredicate() {
            return name().nestedClass("BuiltinPredicate");
        }

        public ClassName classCustomPredicate() {
            return name().nestedClass("CustomPredicate");
        }
    }

    public QCapturableConfig qCapturableConfig() {
        return new QCapturableConfig();
    }

    public class QCapturableConfig {
        private QCapturableConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QCapturable");
        }

        public String methodCaptured() {
            return "captured";
        }
    }

    public ClassName classQCaptured() {
        return TypedQueryConfig.this.name().nestedClass("QCaptured");
    }

    public ClassName classQCapturableQuantifiable() {
        return TypedQueryConfig.this.name().nestedClass("QCapturableQuantifiable");
    }

    public ClassName classQCapturableQuantified() {
        return TypedQueryConfig.this.name().nestedClass("QCapturableQuantified");
    }

    public QTypedNodeConfig qTypedNodeConfig() {
        return new QTypedNodeConfig();
    }

    public class QTypedNodeConfig {
        private QTypedNodeConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("QTypedNode");
        }

        public ClassName classChildEntry() {
            return name().nestedClass("ChildEntry");
        }

        public ClassName classChild() {
            return name().nestedClass("Child");
        }

        public ClassName classField() {
            return name().nestedClass("Field");
        }

        public ClassName classAnchor() {
            return name().nestedClass("Anchor");
        }

        // Field is accessed by subclasses when creating a copy with modified data
        public String fieldData() {
            return "data";
        }

        // Methods implemented by QTypedNode subclasses
        public String methodWithChildren() {
            return "withChildren";
        }

        public String methodWithChildAnchor() {
            return "withChildAnchor";
        }

        public String methodAsExtra() {
            return "asExtra";
        }

        public DataConfig dataConfig() {
            return new DataConfig();
        }

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

            public String methodBuildQuery() {
                return "buildQuery";
            }
        }
    }

    public BuilderConfig builderConfig() {
        return new BuilderConfig();
    }

    public class BuilderConfig {
        private BuilderConfig() {
        }

        public ClassName name() {
            return TypedQueryConfig.this.name().nestedClass("Builder");
        }

        public String methodUnnamedNode() {
            return "unnamedNode";
        }

        public String methodAnyNamedNode() {
            return "anyNamedNode";
        }

        public String methodAnyNode() {
            return "anyNode";
        }

        public String methodErrorNode() {
            return "errorNode";
        }

        public String methodMissingNode() {
            return "missingNode";
        }

        public String methodGroup() {
            return "group";
        }

        public String methodAlternation() {
            return "alternation";
        }
    }
}
