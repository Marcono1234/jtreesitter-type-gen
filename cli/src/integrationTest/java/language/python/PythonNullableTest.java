package language.python;

import com.example.python.*;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the generated code for tree-sitter-python, with nullable annotations.
 *
 * <p>Uses code generated for {@code node-types-python.json}.
 */
class PythonNullableTest extends AbstractTypedTreeTest {
    private PythonNullableTest() {
        super("python", ".py");
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
            def main():
                print("Hello")
                return 0
            """;

        try (var tree = parseNoError(source)) {
            assertEquals(source, tree.getText());
            // Obtain underlying jtreesitter Tree
            assertEquals(source, tree.getTree().getText());

            var rootNode = tree.getRootNode();
            var rootChildren = rootNode.getChildren();
            assertEquals(1, rootChildren.size());
            var functionDefinition = (NodeFunctionDefinition) rootChildren.getFirst();
            assertEquals("main", functionDefinition.getFieldName().getText());

            var functionBody = functionDefinition.getFieldBody();
            var functionChildren = functionBody.getChildren();
            assertEquals(2, functionChildren.size());

            var printStatement = (NodeExpressionStatement) functionChildren.getFirst();
            var printCall = (NodeCall) printStatement.getChildren().getFirst();
            assertEquals("print", printCall.getFieldFunction().getText());
            var printArgument = ((NodeArgumentList) printCall.getFieldArguments()).getChildren().getFirst();
            var stringLiteral = (NodeString) printArgument;
            assertEquals("\"Hello\"", stringLiteral.getText());

            var returnStatement = (NodeReturnStatement) functionChildren.get(1);
            var returnValue = (NodeInteger) returnStatement.getChild();
            assertNotNull(returnValue);
            assertEquals("0", returnValue.getText());
        }
    }

    @Test
    void testNullable() {
        String source = """
            def func():
                pass
            """;

        try (var tree = parseNoError(source)) {
            var functionDefinition = (NodeFunctionDefinition) tree.getRootNode().getChildren().getFirst();
            assertNull(functionDefinition.getFieldReturnType());
        }

        source = """
            def func() -> int:
                return 0
            """;

        try (var tree = parseNoError(source)) {
            var functionDefinition = (NodeFunctionDefinition) tree.getRootNode().getChildren().getFirst();
            var returnType = functionDefinition.getFieldReturnType();
            assertNotNull(returnType);
            assertEquals("int", returnType.getText());
        }
    }

    @Test
    void testFromNode() {
        String source = """
            value = 1
            """;

        try (var tree = parseNoError(source)) {
            var assignment = (NodeAssignment) ((NodeExpressionStatement) tree.getRootNode().getChildren().getFirst()).getChildren().getFirst();
            var intValue = (NodeInteger) assignment.getFieldRight();
            assertNotNull(intValue);
            var intNode = intValue.getNode();

            assertInstanceOf(NodeInteger.class, NodeInteger.fromNode(intNode));
            assertInstanceOf(NodeInteger.class, NodeInteger.fromNodeThrowing(intNode));
            assertInstanceOf(NodeInteger.class, NodeExpression.fromNode(intNode));
            assertInstanceOf(NodeInteger.class, NodeExpression.fromNodeThrowing(intNode));
            assertInstanceOf(NodeInteger.class, TypedNode.fromNode(intNode));
            assertInstanceOf(NodeInteger.class, TypedNode.fromNodeThrowing(intNode));

            assertNull(NodeString.fromNode(intNode));
            var e = assertThrows(IllegalArgumentException.class, () -> NodeString.fromNodeThrowing(intNode));
            assertEquals("Wrong node type: integer", e.getMessage());

            // Obtain the node for "="
            var unnamedNode = assignment.getNode().getChild(1).orElseThrow();
            assertFalse(unnamedNode.isNamed());
            assertEquals("=", unnamedNode.getType());

            assertNull(NodeInteger.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> NodeInteger.fromNodeThrowing(unnamedNode));
            assertEquals("Wrong node type: =", e.getMessage());

            assertNull(NodeExpression.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> NodeExpression.fromNodeThrowing(unnamedNode));
            assertEquals("Wrong node type: =", e.getMessage());

            assertNull(TypedNode.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> TypedNode.fromNodeThrowing(unnamedNode));
            assertEquals("Unknown node type: =", e.getMessage());
        }
    }

    @Test
    void testFindNodes() {
        String source = """
            first = 1
            second = 2
            third = True
            """;

        try (var tree = parseNoError(source)) {
            try (var nodes = NodeInteger.findNodes(tree.getRootNode())) {
                List<String> foundInts = nodes.map(NodeInteger::getText).toList();
                assertEquals(List.of("1", "2"), foundInts);
            }

            var assignment = tree.getRootNode().getChildren().get(1);
            try (var nodes = NodeInteger.findNodes(assignment)) {
                List<String> foundInts = nodes.map(NodeInteger::getText).toList();
                assertEquals(List.of("2"), foundInts);
            }

            // Has no result because source does not contain string literal
            try (var nodes = NodeString.findNodes(tree.getRootNode())) {
                assertEquals(List.of(), nodes.toList());
            }

            try (var nodes = NodeInteger.findNodes(assignment)) {
                var nodesList = nodes.toList();
                assertEquals(1, nodesList.size());

                // Should also include the start node itself in case its type matches
                try (var nodes2 = NodeInteger.findNodes(nodesList.getFirst())) {
                    assertEquals(nodesList, nodes2.toList());
                }
            }

            // Tests supertype `NodePrimaryExpression`
            try (var nodes = NodePrimaryExpression.findNodes(tree.getRootNode())) {
                assertEquals(List.of("first", "1", "second", "2", "third", "True"), nodes.map(NodePrimaryExpression::getText).toList());
            }

            // Tests using a custom allocator
            try (var arena = Arena.ofConfined()) {
                List<NodeInteger> nodesList;
                try (var nodes = NodeInteger.findNodes(tree.getRootNode(), arena)) {
                    nodesList = nodes.toList();
                }

                List<String> foundInts = nodesList.stream().map(NodeInteger::getText).toList();
                assertEquals(List.of("1", "2"), foundInts);

                // Should still be able to use nodes and navigate tree, despite the stream having been closed already
                nodesList.stream().map(NodeInteger::getNode).forEach(node -> {
                    var parent = node.getParent().orElseThrow();
                    assertTrue(parent.getChildren().contains(node));
                });
            }
        }
    }
}
