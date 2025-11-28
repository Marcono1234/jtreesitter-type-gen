package language.java;

import com.example.java.*;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import util.TestHelper;

import java.lang.foreign.Arena;
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
            // TODO: Not yet supported by tree-sitter; requires https://github.com/tree-sitter/tree-sitter/pull/4894 to be released
            // argumentSet("unnamed node with supertype",
            //     q.unnamedNode("statement", ";"),
            //     ""
            // ),
            argumentSet("any named node",
                q.anyNamedNode(),
                "(_)"
            ),
            argumentSet("any node",
                q.anyNode(),
                "_"
            ),
            argumentSet("error node",
                q.errorNode(),
                "(ERROR)"
            ),
            argumentSet("missing node",
                q.missingNode(),
                "(MISSING)"
            ),
            argumentSet("group",
                q.group(q.nodeStringLiteral(), q.nodeNullLiteral()),
                "((string_literal) (null_literal) )"
            ),
            argumentSet("alternation",
                q.alternation(q.nodeStringLiteral(), q.nodeNullLiteral()),
                "[(string_literal) (null_literal) ]"
            )
            // TODO more tests (children, fields, predicates, custom predicate, captures, subtypes, ...), also more complex
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

    // TODO Enable once supported by tree-sitter
    @Disabled("Not yet supported by tree-sitter; requires https://github.com/tree-sitter/tree-sitter/pull/4894 to be released")
    @Test
    void typedQuery_UnnamedSupertype() {
        var q = new TypedQuery.Builder<>();
        var query = q.unnamedNode(";").buildQuery(language);
        var querySupertype = q.unnamedNode(NodeStatement.TYPE_NAME, ";").buildQuery(language);

        try (query; querySupertype) {
            assertEquals("\";\"", TestHelper.getQueryString(query));
            assertEquals("(" + NodeStatement.TYPE_NAME + "/\";\")", TestHelper.getQueryString(querySupertype));

            try (var tree = parseNoError("int i = 1;")) {
                var matches = query.findMatches(tree.getRootNode().getNode()).toList();
                assertEquals(1, matches.size());

                var matchesSupertype = querySupertype.findMatches(tree.getRootNode().getNode()).toList();
                // Query with supertype should only find standalone ';', but not ';' as part of non-empty statement
                assertEquals(0, matchesSupertype.size());
            }

            try (var tree = parseNoError(";")) {
                var matches = query.findMatches(tree.getRootNode().getNode()).toList();
                assertEquals(1, matches.size());

                var matchesSupertype = querySupertype.findMatches(tree.getRootNode().getNode()).toList();
                // Query with supertype should have found standalone ';'
                assertEquals(1, matchesSupertype.size());
            }
        }
    }

    @Test
    void typedQuery_QuantifiedMatching() {
        var q = new TypedQuery.Builder<>();
        var streamLengths = new ArrayList<Long>();
        var query = q.nodeBlock().oneOrMore().matching(stream -> {
            streamLengths.add(stream.count());
            return true;
        }).buildQuery(language);

        try (query; var tree = parseNoError("{} {}")) {
            assertEquals("((block)+ @p0 (#p0?))", TestHelper.getQueryString(query));
            assertEquals(1, query.findMatches(tree.getRootNode().getNode()).count());
            // Predicate should have been called with a Stream with > 1 elements
            assertEquals(List.of(2L), streamLengths);
        }
    }
}
