package language.java;

import com.example.java.*;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Parser;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import util.TestHelper;

import java.lang.foreign.Arena;
import java.lang.foreign.SegmentAllocator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Tests the generated code for tree-sitter-java, with nullable annotations.
 *
 * <p>Uses code generated for {@code node-types-java.json}.
 */
class JavaNullableTest extends AbstractTypedTreeTest {
    private JavaNullableTest() {
        super("java", ".java");
    }

    private TypedTree parseNoError(String source) {
        var tree = TypedTree.fromTree(parse(source));
        assertFalse(tree.hasError());
        return tree;
    }

    @Override
    protected String parseSourceCode(String sourceCode, Function<Object, String> rootNodeConsumer) {
        try (var tree = parseNoError(sourceCode)) {
            return rootNodeConsumer.apply(tree.getRootNode());
        }
    }

    @Test
    void test() {
        String source = """
            public class Main {
                public static void main(String... args) throws IllegalArgumentException {
                    System.out.println("hello" + " world");
                }
            }
            """;

        try (var tree = parseNoError(source)) {
            assertEquals(source, tree.getText());
            // Obtain underlying jtreesitter Tree
            assertEquals(source, tree.getTree().getText());

            var rootNode = tree.getRootNode();
            var rootChildren = rootNode.getChildren();
            assertEquals(1, rootChildren.size());
            var classDeclaration = (NodeClassDeclaration) rootChildren.getFirst();
            assertEquals("Main", classDeclaration.getFieldName().getText());

            var classBody = classDeclaration.getFieldBody();
            var classChildren = classBody.getChildren();
            assertEquals(1, classChildren.size());

            var method = (NodeMethodDeclaration) classChildren.getFirst();
            assertEquals("main", method.getFieldName().getText());
            assertEquals("String... args", method.getFieldParameters().getChildren().getFirst().getText());

            var methodBody = method.getFieldBody();
            assertNotNull(methodBody);
            var methodChildren = methodBody.getChildren();
            assertEquals(1, methodChildren.size());

            var callStatement = (NodeExpressionStatement) methodChildren.getFirst();
            var call = (NodeMethodInvocation) callStatement.getChild();
            assertEquals("println", call.getFieldName().getText());
            var callQualifier = call.getFieldObject();
            assertNotNull(callQualifier);
            assertEquals("System.out", callQualifier.getText());

            var binaryExpression = (NodeBinaryExpression) call.getFieldArguments().getChildren().getFirst();
            assertEquals(NodeBinaryExpression.FieldTokenOperator.TokenType.PLUS_SIGN, binaryExpression.getFieldOperator().getToken());
            assertEquals("\"hello\"", binaryExpression.getFieldLeft().getText());
            assertEquals("\" world\"", binaryExpression.getFieldRight().getText());
        }
    }

    @Test
    void testNullable() {
        String source = "public class Main { }";
        try (var tree = parseNoError(source)) {
            var classDeclaration = (NodeClassDeclaration) tree.getRootNode().getChildren().getFirst();
            var modifiers = classDeclaration.getChild();
            assertNotNull(modifiers);
            assertEquals("public", modifiers.getText());
        }


        source = "class Main { }";
        try (var tree = parseNoError(source)) {
            var classDeclaration = (NodeClassDeclaration) tree.getRootNode().getChildren().getFirst();
            var modifiers = classDeclaration.getChild();
            assertNull(modifiers);
        }
    }

    @Test
    void testFromNode() {
        String source = "public class Main { }";

        try (var tree = parseNoError(source)) {
            var classNode = tree.getRootNode().getChildren().getFirst().getNode();

            assertInstanceOf(NodeClassDeclaration.class, NodeClassDeclaration.fromNode(classNode));
            assertInstanceOf(NodeClassDeclaration.class, NodeClassDeclaration.fromNodeThrowing(classNode));
            assertInstanceOf(NodeClassDeclaration.class, NodeDeclaration.fromNode(classNode));
            assertInstanceOf(NodeClassDeclaration.class, NodeDeclaration.fromNodeThrowing(classNode));
            assertInstanceOf(NodeClassDeclaration.class, TypedNode.fromNode(classNode));
            assertInstanceOf(NodeClassDeclaration.class, TypedNode.fromNodeThrowing(classNode));

            assertNull(NodeInterfaceDeclaration.fromNode(classNode));
            var e = assertThrows(IllegalArgumentException.class, () -> NodeInterfaceDeclaration.fromNodeThrowing(classNode));
            assertEquals("Wrong node type: class_declaration", e.getMessage());

            // Obtain the node for "public"
            var unnamedNode = classNode.getChild(0).orElseThrow().getChild(0).orElseThrow();
            assertFalse(unnamedNode.isNamed());
            assertEquals("public", unnamedNode.getType());

            assertNull(NodeClassDeclaration.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> NodeClassDeclaration.fromNodeThrowing(unnamedNode));
            assertEquals("Wrong node type: public", e.getMessage());

            assertNull(NodeDeclaration.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> NodeDeclaration.fromNodeThrowing(unnamedNode));
            assertEquals("Wrong node type: public", e.getMessage());

            assertNull(TypedNode.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> TypedNode.fromNodeThrowing(unnamedNode));
            assertEquals("Unknown node type: public", e.getMessage());
        }
    }

    @Test
    void testFindNodes() {
        String source = """
            public class Main {
                private static final int CONSTANT = 123;
            
                public static void main(String... args) throws IllegalArgumentException {
                    int value = 456;
                    boolean b = true;
                }
            }
            """;

        try (var tree = parseNoError(source)) {
            try (var nodes = NodeDecimalIntegerLiteral.findNodes(tree.getRootNode())) {
                List<String> foundInts = nodes.map(NodeDecimalIntegerLiteral::getText).toList();
                assertEquals(List.of("123", "456"), foundInts);
            }

            var field = (NodeFieldDeclaration) ((NodeClassDeclaration) tree.getRootNode().getChildren().getFirst()).getFieldBody().getChildren().getFirst();
            try (var nodes = NodeDecimalIntegerLiteral.findNodes(field)) {
                List<String> foundInts = nodes.map(NodeDecimalIntegerLiteral::getText).toList();
                assertEquals(List.of("123"), foundInts);
            }

            // Has no result because source does not contain string literal
            try (var nodes = NodeStringLiteral.findNodes(tree.getRootNode())) {
                assertEquals(List.of(), nodes.toList());
            }


            var intLiteral = (NodeDecimalIntegerLiteral) field.getFieldDeclarator().getFirst().getFieldValue();
            assertNotNull(intLiteral);
            // Should also include the start node itself in case its type matches
            try (var nodes = NodeDecimalIntegerLiteral.findNodes(intLiteral)) {
                assertEquals(List.of(intLiteral), nodes.toList());
            }


            // Tests supertype `NodeLiteral`
            try (var nodes = NodeLiteral.findNodes(tree.getRootNode())) {
                assertEquals(List.of("123", "456", "true"), nodes.map(NodeLiteral::getText).toList());
            }


            // Tests using a custom allocator
            try (var arena = Arena.ofConfined()) {
                List<NodeDecimalIntegerLiteral> nodesList;
                try (var nodes = NodeDecimalIntegerLiteral.findNodes(tree.getRootNode(), arena)) {
                    nodesList = nodes.toList();
                }

                List<String> foundInts = nodesList.stream().map(NodeDecimalIntegerLiteral::getText).toList();
                assertEquals(List.of("123", "456"), foundInts);

                // Should still be able to use nodes and navigate tree, despite the stream having been closed already
                nodesList.stream().map(NodeDecimalIntegerLiteral::getNode).forEach(node -> {
                    var parent = node.getParent().orElseThrow();
                    assertTrue(parent.getChildren().contains(node));
                });
            }
        }
    }

    static Stream<Arguments.ArgumentSet> queryStringArgs() {
        var q = new TypedQuery.Builder<>();

        return Stream.of(
            argumentSet("unnamed node",
                q.unnamedNode("{"),
                "\"{\""
            ),
            argumentSet("unnamed node escaping",
                q.unnamedNode("\""),
                "\"\\\"\""
            ),
            // TODO: Not yet supported by tree-sitter; requires https://github.com/tree-sitter/tree-sitter/pull/4894 to be released
            // argumentSet("unnamed node with supertype",
            //     q.unnamedNode("statement", ";"),
            //     "(statement/\";\")"
            // ),
            argumentSet("any named node",
                q.anyNamedNode(),
                "(_)"
            ),
            argumentSet("any node",
                q.anyNode(),
                // Wrapped in an alternation to avoid it accidentally becoming a named-only wildcard when wrapped in a group
                "[_]"
            ),
            // Verify that "any node" `_` cannot be accidentally turned into a named-only `(_)` when wrapped for example in a group
            argumentSet("any node in group",
                q.group(q.anyNode()),
                // Wrapped in an alternation as `[_]` to avoid it accidentally becoming a named-only wildcard when wrapped in a group
                "([_] )"
            ),
            argumentSet("any node quantified",
                q.anyNode().optional(),
                // Wrapped in an alternation as `[_]` to avoid it accidentally becoming a named-only wildcard when wrapped in a group
                "[_]?"
            ),
            argumentSet("error node",
                q.errorNode(),
                "(ERROR)"
            ),
            argumentSet("missing node",
                q.missingNode(),
                "(MISSING)"
            ),
            // TODO: Add tests for MISSING node of specific type (named and unnamed + escaping) once the API supports it
            //   ...
            argumentSet("group",
                q.group(q.nodeStringLiteral(), q.nodeNullLiteral()),
                "((string_literal) (null_literal) )"
            ),
            argumentSet("alternation untyped",
                q.alternation(q.unnamedNode("+"), q.unnamedNode("-")),
                "[\"+\" \"-\" ]"
            ),
            argumentSet("alternation typed",
                q.alternation(q.nodeStringLiteral(), q.nodeNullLiteral()),
                "[(string_literal) (null_literal) ]"
            ),
            argumentSet("alternation mixed",
                q.alternation(q.nodeStringLiteral(), q.unnamedNode("+")),
                "[(string_literal) \"+\" ]"
            ),
            // Quantifiers
            argumentSet("quantifier ?",
                q.errorNode().optional(),
                "(ERROR)?"
            ),
            argumentSet("quantifier *",
                q.errorNode().zeroOrMore(),
                "(ERROR)*"
            ),
            argumentSet("quantifier +",
                q.errorNode().oneOrMore(),
                "(ERROR)+"
            ),
            // Node type specific methods
            argumentSet("as subtype",
                q.nodeStringLiteral().asSubtypeOfNodeLiteral(),
                "(_literal/string_literal)"
            ),
            argumentSet("children none",
                // Permit empty `withChildren()` call for now, but it should have no effect
                q.nodeBlock().withChildren(),
                "(block)"
            ),
            argumentSet("children",
                q.nodeBlock().withChildren(q.nodeAssertStatement()),
                "(block (assert_statement))"
            ),
            argumentSet("children multiple",
                // Also permit empty `withChildren()` call for now, but it should have no effect
                q.nodeBlock().withChildren(q.nodeAssertStatement()).withChildren(q.nodeIfStatement(), q.nodeReturnStatement()).withChildren(),
                "(block (assert_statement) (if_statement) (return_statement))"
            ),
            argumentSet("children anchor",
                q.nodeBlock().withChildAnchor().withChildren(q.nodeReturnStatement()).withChildAnchor(),
                "(block . (return_statement) .)"
            ),
            argumentSet("fields",
                q.nodeClassDeclaration().withFieldName(q.nodeIdentifier()),
                "(class_declaration name: (identifier))"
            ),
            argumentSet("fields multiple",
                q.nodeClassDeclaration().withFieldName(q.nodeIdentifier()).withFieldBody(q.nodeClassBody()),
                "(class_declaration name: (identifier) body: (class_body))"
            ),
            argumentSet("fields without",
                q.nodeClassDeclaration().withoutFieldPermits(),
                "(class_declaration !permits)"
            ),
            argumentSet("fields children complex",
                q.nodeClassDeclaration().withChildAnchor().withChildren(q.nodeModifiers()).withChildAnchor().withFieldName(q.nodeIdentifier()).withoutFieldPermits().withFieldInterfaces(q.nodeSuperInterfaces()),
                "(class_declaration !permits . (modifiers) . name: (identifier) interfaces: (super_interfaces))"
            ),
            // Quantifiers for typed nodes
            argumentSet("quantifier typed ?",
                q.nodeClassDeclaration().optional(),
                "(class_declaration)?"
            ),
            argumentSet("quantifier typed *",
                q.nodeClassDeclaration().zeroOrMore(),
                "(class_declaration)*"
            ),
            argumentSet("quantifier typed +",
                q.nodeClassDeclaration().oneOrMore(),
                "(class_declaration)+"
            ),
            // Predicates
            argumentSet("predicate #eq?",
                q.nodeDecimalIntegerLiteral().textEq("1"),
                "((decimal_integer_literal) @pb0 (#eq? @pb0 \"1\"))"
            ),
            argumentSet("predicate #eq? escaping",
                q.nodeDecimalIntegerLiteral().textEq("_\\_\"_\n_\r_\t_\0_"),
                "((decimal_integer_literal) @pb0 (#eq? @pb0 \"_\\\\_\\\"_\\n_\\r_\\t_\\0_\"))"
            ),
            argumentSet("predicate #not-eq?",
                q.nodeDecimalIntegerLiteral().textNotEq("1"),
                "((decimal_integer_literal) @pb0 (#not-eq? @pb0 \"1\"))"
            ),
            argumentSet("predicate #any-of?",
                q.nodeDecimalIntegerLiteral().textAnyOf("1", "2"),
                "((decimal_integer_literal) @pb0 (#any-of? @pb0 \"1\" \"2\"))"
            ),
            argumentSet("predicate custom",
                q.nodeIdentifier().matching(_ -> true),
                "((identifier) @pc0 (#pc0?))"
            ),
            argumentSet("predicate multiple",
                // Specifying multiple predicates is possible, but it might be more efficient to just use a single custom predicate
                q.nodeIdentifier().matching(_ -> true).textEq("a").textAnyOf("a", "b"),
                "((((identifier) @pc0 (#pc0?)) @pb0 (#eq? @pb0 \"a\")) @pb1 (#any-of? @pb1 \"a\" \"b\"))"
            ),
            // Captures
            argumentSet("captures",
                q.nodeIdentifier().captured(((_, _) -> {})),
                "(identifier) @c0"
            ),
            // Mixed
            argumentSet("mixed",
                q.nodeClassDeclaration()
                    .withoutFieldPermits()
                    .withChildAnchor()
                    .withoutFieldSuperclass()
                    .withChildren(q.nodeModifiers())
                    .withChildAnchor()
                    .withFieldName(q.nodeIdentifier())
                    .zeroOrMore()
                    .matching(_ -> true)
                    .captured(((_, _) -> {})),
                "((class_declaration !permits !superclass . (modifiers) . name: (identifier))* @pc0 (#pc0?)) @c0"
            )
        );
    }

    @MethodSource("queryStringArgs")
    @ParameterizedTest
    void typedQuery_QueryString(TypedQuery.QNode<?, ?> node, String expectedQueryString) {
        try (var query = node.buildQuery(language)) {
            String queryString = TestHelper.getQueryString(query);
            assertEquals(expectedQueryString, queryString);
        }
    }

    // TODO: Add typed query test for 'extra' (for Java that is `NodeLineComment` or `NodeBlockComment`); however node-types.json does not include `"extra": true` yet

    // TODO: Enable once supported by tree-sitter
    @Disabled("Not yet supported by tree-sitter; requires https://github.com/tree-sitter/tree-sitter/pull/4894 to be released")
    @Test
    void typedQuery_UnnamedNode_Supertype() {
        var q = new TypedQuery.Builder<>();
        var query = q.unnamedNode(";").buildQuery(language);
        var querySupertype = q.unnamedNode(NodeStatement.TYPE_NAME, ";").buildQuery(language);

        try (query; querySupertype) {
            assertEquals("\";\"", TestHelper.getQueryString(query));
            assertEquals("(" + NodeStatement.TYPE_NAME + "/\";\")", TestHelper.getQueryString(querySupertype));

            try (var tree = parseNoError("int i = 1;")) {
                try (var matches = query.findMatches(tree.getRootNode().getNode())) {
                    assertEquals(1, matches.count());
                }

                try (var matchesSupertype = querySupertype.findMatches(tree.getRootNode().getNode())) {
                    // Query with supertype should not find ';' as part of non-empty statement, only standalone ';'
                    assertEquals(0, matchesSupertype.count());
                }
            }

            try (var tree = parseNoError(";")) {
                try (var matches = query.findMatches(tree.getRootNode().getNode())) {
                    assertEquals(1, matches.count());
                }

                try (var matchesSupertype = querySupertype.findMatches(tree.getRootNode().getNode())) {
                    // Query with supertype should have found standalone ';'
                    assertEquals(1, matchesSupertype.count());
                }
            }
        }
    }

    // TODO: Enable once supported by typed query API
    @Disabled("Specifying type of MISSING node is not support yet by typed query API")
    @Test
    void typedQuery_Missing_Validation() {
        // Should check that MISSING node validates that unnamed node exists
        throw new AssertionError("TODO");
    }

    @Test
    void typedQuery_Group_Validation() {
        var q = new TypedQuery.Builder<>();

        // Permit a group with a single type for now, even though it is pointless (?)
        try (var query = q.group(q.nodeIdentifier()).buildQuery(language)) {
            assertEquals("((identifier) )", TestHelper.getQueryString(query));
        }

        @SuppressWarnings("Convert2MethodRef")
        var e = assertThrows(IllegalArgumentException.class, () -> q.group());
        assertEquals("Must specify at least one node", e.getMessage());
    }

    @Test
    void typedQuery_Alternation_Validation() {
        var q = new TypedQuery.Builder<>();

        // Permit an alternation with a single type for now, even though it is pointless (?)
        try (var query = q.alternation(q.nodeIdentifier()).buildQuery(language)) {
            assertEquals("[(identifier) ]", TestHelper.getQueryString(query));
        }

        @SuppressWarnings("unchecked")
        var e = assertThrows(IllegalArgumentException.class, () -> q.alternation(new TypedQuery.QNode[0]));
        assertEquals("Must specify at least one node", e.getMessage());

        @SuppressWarnings("unchecked")
        var e2 = assertThrows(IllegalArgumentException.class, () -> q.alternation(new TypedQuery.QCapturable[0]));
        assertEquals("Must specify at least one node", e2.getMessage());
    }

    @Test
    void typedQuery_QuantifiedMatching() {
        var q = new TypedQuery.Builder<List<NodeBlock>>();
        var streamLengths = new ArrayList<Long>();
        var query = q.nodeBlock()
            .oneOrMore()
            .matching(stream -> {
                streamLengths.add(stream.count());
                return true;
            })
            .captured(List::add)
            .buildQuery(language);

        try (
            query;
            var tree = parseNoError("{} {}");
            var matches = query.findMatches(tree.getRootNode().getNode())
        ) {
            assertEquals("((block)+ @pc0 (#pc0?)) @c0", TestHelper.getQueryString(query));

            var matchesList = matches.toList();
            assertEquals(1, matchesList.size());
            // Predicate should have been called with a Stream with > 1 elements
            assertEquals(List.of(2L), streamLengths);

            var nodes = new ArrayList<NodeBlock>();
            matchesList.getFirst().collectCaptures(nodes);
            assertEquals(2, nodes.size());
        }
    }

    @Test
    void typedQuery_FieldValidation() {
        var q = new TypedQuery.Builder<>();
        var nodeClassWithSuperclass = q.nodeClassDeclaration().withFieldSuperclass(q.nodeSuperclass());

        var e = assertThrows(IllegalStateException.class, () -> nodeClassWithSuperclass.withFieldSuperclass(q.nodeSuperclass()));
        assertEquals("Field 'superclass' has already been added", e.getMessage());

        //noinspection Convert2MethodRef
        e = assertThrows(IllegalStateException.class, () -> nodeClassWithSuperclass.withoutFieldSuperclass());
        assertEquals("Field 'superclass' has already been added to \"with fields\"", e.getMessage());

        // Should also detect first 'withoutField' then 'withField'
        e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withoutFieldSuperclass().withFieldSuperclass(q.nodeSuperclass()));
        assertEquals("Field 'superclass' has already been added to \"without fields\"", e.getMessage());

        // Duplicate 'withoutField' is permitted for now but should only add exclusion once
        try (var query = q.nodeClassDeclaration().withoutFieldSuperclass().withoutFieldSuperclass().buildQuery(language)) {
            assertEquals("(class_declaration !superclass)", TestHelper.getQueryString(query));
        }

        // Should allow multiple calls to 'withField' when field can occur multiple times
        try (var query = q.nodeArrayCreationExpression().withFieldDimensions(q.nodeDimensionsExpr()).withFieldDimensions(q.nodeDimensions()).buildQuery(language)) {
            assertEquals("(array_creation_expression dimensions: (dimensions_expr) dimensions: (dimensions))", TestHelper.getQueryString(query));
        }
    }

    @Test
    void typedQuery_AnchorValidation() {
        var q = new TypedQuery.Builder<>();

        {
            // Lone anchor without any children or fields is invalid, tree-sitter rejects the query string; should fail
            // before letting tree-sitter attempt to parse the query string
            var e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().buildQuery(language));
            assertEquals("Must specify children or fields when using `withChildAnchor`", e.getMessage());

            // Should also fail when using empty `withChildren` call
            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().withChildren().buildQuery(language));
            assertEquals("Must specify children or fields when using `withChildAnchor`", e.getMessage());

            // Should also fail when only using 'withoutField'
            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().withoutFieldSuperclass().buildQuery(language));
            assertEquals("Must specify children or fields when using `withChildAnchor`", e.getMessage());

            // Should already fail fast when trying to add query node in this invalid state as subnode to other node
            e = assertThrows(IllegalStateException.class, () -> q.alternation(q.nodeClassDeclaration().withChildAnchor()));
            assertEquals("Must specify children or fields when using `withChildAnchor`", e.getMessage());

            // Should already fail fast when using general QTypedNode methods (and therefore the invalid state cannot be corrected afterwards anymore)
            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().zeroOrMore());
            assertEquals("Must specify children or fields when using `withChildAnchor`", e.getMessage());
        }

        try (var query = q.nodeClassDeclaration().withChildAnchor().withChildren(q.nodeModifiers()).withChildAnchor().buildQuery(language)) {
            assertEquals("(class_declaration . (modifiers) .)", TestHelper.getQueryString(query));
        }

        {
            var e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().withChildAnchor());
            assertEquals("Duplicate anchor is not valid", e.getMessage());

            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildren(q.nodeModifiers()).withChildAnchor().withChildAnchor());
            assertEquals("Duplicate anchor is not valid", e.getMessage());

            // Should also fail when using empty `withChildren` call
            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().withChildren().withChildAnchor());
            assertEquals("Duplicate anchor is not valid", e.getMessage());

            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().withChildAnchor().withChildren(q.nodeModifiers()));
            assertEquals("Duplicate anchor is not valid", e.getMessage());

            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildren(q.nodeModifiers()).withChildAnchor().withChildAnchor().withChildren(q.nodeModifiers()));
            assertEquals("Duplicate anchor is not valid", e.getMessage());

            // 'withoutField' should not be considered for duplicate anchor check
            e = assertThrows(IllegalStateException.class, () -> q.nodeClassDeclaration().withChildAnchor().withoutFieldPermits().withChildAnchor());
            assertEquals("Duplicate anchor is not valid", e.getMessage());
        }
    }

    @Test
    void typedQuery_textAnyOf_Validation() {
        var queryNode = new  TypedQuery.Builder<>().nodeAnnotation();
        var e = assertThrows(IllegalArgumentException.class, () -> queryNode.textAnyOf(new String[0]));
        assertEquals("Must specify at least one string", e.getMessage());

        assertThrows(NullPointerException.class, () -> queryNode.textAnyOf((String[]) null));
        assertThrows(NullPointerException.class, () -> queryNode.textAnyOf("a", null));
    }

    /**
     * Tests known cases where the builder API currently cannot prevent or detect invalid query strings during construction.
     */
    @SuppressWarnings("resource")  // ignore warnings about `TypedQuery` not being closed in `assertThrows(...)`
    @Test
    void typedQuery_Invalid() {
        var q = new TypedQuery.Builder<>();

        // tree-sitter probably rejects this because child anchor requires wrong order for `modifiers` child and `name` field
        var e = assertThrows(RuntimeException.class, () -> q.nodeClassDeclaration().withFieldName(q.nodeIdentifier()).withChildAnchor().withChildren(q.nodeModifiers()).buildQuery(language));
        assertTrue(e.getMessage().startsWith("Failed creating query; verify that children and fields are specified in the right order; if you expect the query to be valid please report this to the jtreesitter-type-gen maintainers; query string:"), "Message: " + e.getMessage());
        assertTrue(e.getMessage().contains("(class_declaration name: (identifier) . (modifiers))"), "Message: " + e.getMessage());

        // tree-sitter probably rejects this because `modifiers` child may only appear once
        // Typed query builder cannot currently validate this because `withChildren` is also used for wildcard-like node types such as MISSING nodes and 'extra' nodes
        e = assertThrows(RuntimeException.class, () -> q.nodeClassDeclaration().withChildren(q.nodeModifiers()).withChildren(q.nodeModifiers()).buildQuery(language));
        assertTrue(e.getMessage().startsWith("Failed creating query; verify that children and fields are specified in the right order; if you expect the query to be valid please report this to the jtreesitter-type-gen maintainers; query string:"), "Message: " + e.getMessage());
        assertTrue(e.getMessage().contains("(class_declaration (modifiers) (modifiers))"), "Message: " + e.getMessage());
    }

    /**
     * Tests the behavior of {@link TypedQuery#findMatches} when a node for a different language is provided.
     */
    @Test
    void typedQuery_WrongLanguage() {
        var q = new TypedQuery.Builder<List<NodeIdentifier>>();
        var query = q.nodeIdentifier().buildQuery(language);

        var languageJson = TestHelper.loadLanguage("json");

        try (
            query;
            var parser = new Parser(languageJson);
            var tree = parser.parse("{}").orElseThrow()
        ) {
            var e = assertThrows(IllegalArgumentException.class, () -> query.findMatches(tree.getRootNode()));
            assertEquals(
                "Node belongs to unexpected language; expected: " + language + ", actual: " + languageJson,
                e.getMessage()
            );
        }
    }

    @Test
    void typedQuery_Supertype() {
        var q = new TypedQuery.Builder<List<NodeIdentifier>>();
        var query = q.nodeIdentifier()
            .captured(List::add)
            .buildQuery(language);
        var querySupertype = q.nodeIdentifier()
            .asSubtypeOfNodePrimaryExpression()
            .captured(List::add)
            .buildQuery(language);
        var querySuperSupertype = q.nodeIdentifier()
            // 'expression' is the supertype of 'primary_expression'
            .asSubtypeOfNodeExpression()
            .captured(List::add)
            .buildQuery(language);

        String source = """
            class MyClass {
                int i = 12;
                int j = 34;
                int x = i + arr[0];
            }
            """;

        try (query; querySupertype; querySuperSupertype; var tree = parseNoError(source)) {
            assertEquals("(identifier) @c0", TestHelper.getQueryString(query));
            assertEquals("(primary_expression/identifier) @c0", TestHelper.getQueryString(querySupertype));
            assertEquals("(expression/identifier) @c0", TestHelper.getQueryString(querySuperSupertype));

            var startNode = tree.getRootNode().getNode();

            try (var matches = query.findMatches(startNode)) {
                var nodes = new ArrayList<NodeIdentifier>();
                matches.forEach(m -> m.collectCaptures(nodes));
                assertEquals(List.of("MyClass", "i", "j", "x", "i", "arr"), nodes.stream().map(NodeIdentifier::getText).toList());
            }

            try (var matchesSupertype = querySupertype.findMatches(startNode)) {
                var nodes = new ArrayList<NodeIdentifier>();
                matchesSupertype.forEach(m -> m.collectCaptures(nodes));
                assertEquals(List.of("i", "arr"), nodes.stream().map(NodeIdentifier::getText).toList());
            }

            try (var matchesSuperSupertype = querySuperSupertype.findMatches(startNode)) {
                var nodes = new ArrayList<NodeIdentifier>();
                matchesSuperSupertype.forEach(m -> m.collectCaptures(nodes));
                // Does not match `arr` because the tree-sitter-java grammar requires that the array reference
                // is a 'primary_expression' (and not just an 'expression')
                assertEquals(List.of("i"), nodes.stream().map(NodeIdentifier::getText).toList());
            }
        }
    }

    @Test
    void typedQuery_FieldAnchor() {
        var q = new TypedQuery.Builder<>();
        var query = q.nodeForStatement()
            .withFieldUpdate(q.nodeAssignmentExpression())
            // TODO: It seems for tree-sitter it does not actually make a difference whether an anchor is used between
            //   fields? Even without the anchor it seems to enforce the order
            .withChildAnchor()
            .withFieldUpdate(q.nodeMethodInvocation())
            .buildQuery(language);

        try (query) {
            assertEquals("(for_statement update: (assignment_expression) . update: (method_invocation))", TestHelper.getQueryString(query));

            try (
                var tree = parseNoError("for (;; i += 1, call()) { }");
                var matches = query.findMatches(tree.getRootNode().getNode())
            ) {
                assertEquals(1, matches.count());
            }

            // Should not match when nodes are in different order
            try (
                var treeNoMatches = parseNoError("for (;; call(), i += 1) { }");
                var matches = query.findMatches(treeNoMatches.getRootNode().getNode())
            ) {
                assertEquals(0, matches.count());
            }
        }
    }

    @Test
    void typedQuery_FieldToken() {
        var q = new TypedQuery.Builder<List<NodeBinaryExpression>>();
        var query = q.nodeBinaryExpression()
            .withFieldOperator(QNodeBinaryExpression.fieldTokenOperator(NodeBinaryExpression.FieldTokenOperator.TokenType.PLUS_SIGN))
            .captured(List::add)
            .buildQuery(language);

        String source = """
            class MyClass {
                int i = 1 + 2;
                int j = 3 - 4;
            }
            """;

        try (
            query;
            var tree = parseNoError(source);
            var matches = query.findMatches(tree.getRootNode().getNode())
        ) {
            assertEquals("(binary_expression operator: \"+\") @c0", TestHelper.getQueryString(query));

            var nodes = new ArrayList<NodeBinaryExpression>();
            matches.forEach(m -> m.collectCaptures(nodes));
            assertEquals(List.of("1 + 2"), nodes.stream().map(NodeBinaryExpression::getText).toList());
        }
    }

    /**
     * Verify that it is possible to create and execute a query without any captures, e.g. to check if a
     * given input matches a pattern (as defined by the query).
     */
    @Test
    void typedQuery_NoCapture() {
        var q = new TypedQuery.Builder<>();
        var query = q.nodeDecimalIntegerLiteral().buildQuery(language);

        try (query) {
            try (var tree = parseNoError("int i = 0xA;"); var matches = query.findMatches(tree.getRootNode().getNode())) {
                assertEquals(0, matches.count());
            }

            try (var tree = parseNoError("int i = 1;"); var matches = query.findMatches(tree.getRootNode().getNode())) {
                var matchesList = matches.toList();
                assertEquals(1, matchesList.size());

                var match = matchesList.getFirst();
                assertNotNull(match.getQueryMatch());
                assertEquals(0, match.getQueryMatch().captures().size());

                // But trying to collect captures should fail
                var e = assertThrows(IllegalStateException.class, () -> matchesList.getFirst().collectCaptures(new ArrayList<>()));
                assertEquals("No capture handlers have been registered using `QCapturable#captured`", e.getMessage());
            }
        }
    }

    @Test
    void typedQuery() {
        class CustomCollector {
            final List<NodeDecimalIntegerLiteral> decimalLiterals = new ArrayList<>();
            final List<NodeLiteral> otherLiterals =  new ArrayList<>();
        }

        var q = new TypedQuery.Builder<CustomCollector>();
        var query = q.nodeClassDeclaration()
            .withFieldName(q.nodeIdentifier().matching(s -> s.allMatch(n -> n.getText().startsWith("My"))))
            .withFieldBody(q.nodeClassBody().withChildren(
                q.nodeFieldDeclaration()
                    .withFieldDeclarator(q.alternation(
                        q.nodeVariableDeclarator()
                            .withFieldName(q.nodeIdentifier().textAnyOf("i", "i2"))
                            .withFieldValue(q.nodeDecimalIntegerLiteral().captured((c, n) -> c.decimalLiterals.add(n))),
                        q.nodeVariableDeclarator()
                            .withFieldValue(q.alternation(q.nodeHexIntegerLiteral(), q.nodeOctalIntegerLiteral()).captured((c, n) -> c.otherLiterals.add(n)))
                    ))
            ))
            .buildQuery(language);

        String source = """
            class FirstClass {
                int i = 1;
                int j = 2;
            }
            
            class MyClass {
                int i = 12;
                int j = 34;
                int i2 = 56;
                int j2 = 78;
            
                int x = 123;
                int x2 = 0x123;
                int x3 = 0456;
            }
            
            class OtherClass {
                int i = 9;
            }
            """;

        try (
            query;
            var tree = parseNoError(source);
            var matches = query.findMatches(tree.getRootNode().getNode())
        ) {
            assertEquals(
                "(class_declaration name: ((identifier) @pc0 (#pc0?)) body: (class_body (field_declaration declarator: [(variable_declarator name: ((identifier) @pb0 (#any-of? @pb0 \"i\" \"i2\")) value: (decimal_integer_literal) @c0) (variable_declarator value: [(hex_integer_literal) (octal_integer_literal) ] @c1) ])))",
                TestHelper.getQueryString(query)
            );

            var collector = new CustomCollector();
            matches.forEach(match -> {
                match.collectCaptures(collector);
                assertNotNull(match.getQueryMatch());
            });

            assertEquals(List.of("12", "56"), collector.decimalLiterals.stream().map(NodeLiteral::getText).toList());
            assertEquals(List.of("0x123", "0456"), collector.otherLiterals.stream().map(NodeLiteral::getText).toList());
        }
    }

    /**
     * Tests using {@link TypedQuery#findMatches(Node, SegmentAllocator)}, with a custom allocator.
     */
    @Test
    void typedQuery_Allocator() {
        String source = """
            class MyClass {
                int i = 12;
                int j = 34;
            }
            """;

        try (var tree = parseNoError(source); var arena = Arena.ofConfined()) {
            var q = new TypedQuery.Builder<List<NodeDecimalIntegerLiteral>>();
            var nodes = new ArrayList<NodeDecimalIntegerLiteral>();

            try (
                var query = q.nodeDecimalIntegerLiteral().captured(List::add).buildQuery(language);
                var matches = query.findMatches(tree.getRootNode().getNode(), arena)
            ) {
                matches.forEach(match -> match.collectCaptures(nodes));
            }

            assertEquals(List.of("12", "34"), nodes.stream().map(NodeDecimalIntegerLiteral::getText).toList());

            // Should still be able to use nodes and navigate tree, despite the matches stream having been closed already
            nodes.stream().map(NodeDecimalIntegerLiteral::getNode).forEach(node -> {
                var parent = node.getParent().orElseThrow();
                assertTrue(parent.getChildren().contains(node));
            });
        }
    }

    /**
     * Test for {@link TypedQuery#findMatchesAndCollect}
     */
    @Test
    void typedQuery_findMatchesAndCollect() {
        var q = new TypedQuery.Builder<List<NodeDecimalIntegerLiteral>>();

        String source = """
            class MyClass {
                int i = 1;
                int j = 2;
            }
            """;

        try (
            var query = q.nodeDecimalIntegerLiteral().captured(List::add).buildQuery(language);
            var tree = parseNoError(source)
        ) {
            var startNode = tree.getRootNode().getNode();

            // First try manual collection
            try (var matches = query.findMatches(startNode)) {
                var matchesList = matches.toList();
                assertEquals(2, matchesList.size());

                var nodes = new ArrayList<NodeDecimalIntegerLiteral>();
                matchesList.forEach(match -> match.collectCaptures(nodes));
                assertEquals(List.of("1", "2"), nodes.stream().map(NodeDecimalIntegerLiteral::getText).toList());
            }

            // Now compare with `findMatchesAndCollect`
            try (var arena = Arena.ofConfined()) {
                var nodes = new ArrayList<NodeDecimalIntegerLiteral>();
                query.findMatchesAndCollect(startNode, arena, nodes);
                assertEquals(List.of("1", "2"), nodes.stream().map(NodeDecimalIntegerLiteral::getText).toList());
            }
        }


        try (
            var queryNoCapture = q.nodeDecimalIntegerLiteral().buildQuery(language);
            var treeNoMatch = parseNoError("class MyClass {}")
        ) {
            var startNode = treeNoMatch.getRootNode().getNode();

            try (var matches = queryNoCapture.findMatches(startNode)) {
                assertEquals(0, matches.count());
            }

            try (var arena = Arena.ofConfined()) {
                // Should fail fast, despite the query having no matches for the input (and therefore internally not
                // actually trying to collect captures, which would normally cause this exception)
                var e = assertThrows(IllegalStateException.class, () -> queryNoCapture.findMatchesAndCollect(startNode, arena, new ArrayList<>()));
                assertEquals("No capture handlers have been registered using `QCapturable#captured`", e.getMessage());
            }
        }
    }
}
