package language.json;

import com.example.json.*;
import io.github.treesitter.jtreesitter.Point;
import io.github.treesitter.jtreesitter.Range;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the generated code for tree-sitter-json, with nullable annotations.
 *
 * <p>Uses code generated for {@code node-types-json.json}.
 */
class JsonNullableTest extends AbstractTypedTreeTest {
    private JsonNullableTest() {
        super("json", ".json");
    }

    @Override
    protected String parseSourceCode(String sourceCode, Function<Object, String> rootNodeConsumer) {
        try (var tree = TypedTree.fromTree(parse(sourceCode))) {
            return rootNodeConsumer.apply(tree.getRootNode());
        }
    }

    @Test
    void test() {
        String source = """
            [
                {
                    "a": 1,
                    "b": null
                }
            ]
            """;

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());
            assertEquals(source, tree.getText());
            // Obtain underlying jtreesitter Tree
            assertEquals(source, tree.getTree().getText());

            var rootNode = tree.getRootNode();
            var rootChildren = rootNode.getChildren();
            assertEquals(1, rootChildren.size());

            var array = (NodeArray) rootChildren.getFirst();
            var arrayChildren = array.getChildren();
            assertEquals(1, arrayChildren.size());

            var object = (NodeObject) arrayChildren.getFirst();
            var objectChildren = object.getChildren();
            assertEquals(2, objectChildren.size());

            var pair1 = objectChildren.get(0);
            assertEquals("\"a\"", pair1.getFieldKey().getText());
            var pair1Value = pair1.getFieldValue();
            assertInstanceOf(NodeNumber.class, pair1Value);
            assertEquals("1", pair1Value.getText());

            var pair2 = objectChildren.get(1);
            assertEquals("\"b\"", pair2.getFieldKey().getText());
            var pair2Value = pair2.getFieldValue();
            assertInstanceOf(NodeNull.class, pair2Value);
            assertEquals("null", pair2Value.getText());
        }
    }

    @Test
    void testFromNode() {
        String source = "[]";

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

            var arrayNode = tree.getRootNode().getChildren().getFirst().getNode();

            assertInstanceOf(NodeArray.class, NodeArray.fromNode(arrayNode));
            assertInstanceOf(NodeArray.class, NodeArray.fromNodeThrowing(arrayNode));
            assertInstanceOf(NodeArray.class, NodeValue.fromNode(arrayNode));
            assertInstanceOf(NodeArray.class, NodeValue.fromNodeThrowing(arrayNode));
            assertInstanceOf(NodeArray.class, TypedNode.fromNode(arrayNode));
            assertInstanceOf(NodeArray.class, TypedNode.fromNodeThrowing(arrayNode));

            assertNull(NodeObject.fromNode(arrayNode));
            var e = assertThrows(IllegalArgumentException.class, () -> NodeObject.fromNodeThrowing(arrayNode));
            assertEquals("Wrong node type: array", e.getMessage());


            // Obtain the node for "["
            var unnamedNode = arrayNode.getChild(0).orElseThrow();
            assertFalse(unnamedNode.isNamed());
            assertEquals("[", unnamedNode.getType());

            assertNull(NodeArray.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> NodeArray.fromNodeThrowing(unnamedNode));
            assertEquals("Wrong node type: [", e.getMessage());

            assertNull(NodeValue.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> NodeValue.fromNodeThrowing(unnamedNode));
            assertEquals("Wrong node type: [", e.getMessage());

            assertNull(TypedNode.fromNode(unnamedNode));
            e = assertThrows(IllegalArgumentException.class, () -> TypedNode.fromNodeThrowing(unnamedNode));
            assertEquals("Unknown node type: [", e.getMessage());
        }
    }

    @Test
    void testFindNodes() {
        String source = """
            [1, 2, true]
            [null, 3, ["a", 4, {"b": [5, false]}]]
            """;

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

            try (var nodes = NodeNumber.findNodes(tree.getRootNode())) {
                List<String> foundNumbers = nodes.map(NodeNumber::getText).toList();
                assertEquals(List.of("1", "2", "3", "4", "5"), foundNumbers);
            }

            NodeArray firstArray = (NodeArray) tree.getRootNode().getChildren().getFirst();
            try (var nodes = NodeNumber.findNodes(firstArray)) {
                List<String> foundNumbers = nodes.map(NodeNumber::getText).toList();
                assertEquals(List.of("1", "2"), foundNumbers);
            }

            // Has no results because first array does not contain `null`
            try (var nodes = NodeNull.findNodes(firstArray)) {
                assertEquals(List.of(), nodes.toList());
            }


            NodeNumber number = (NodeNumber) firstArray.getChildren().getFirst();
            // Should also include the start node itself in case its type matches
            try (var nodes = NodeNumber.findNodes(number)) {
                assertEquals(List.of(number), nodes.toList());
            }
        }

        source = "{\"a\": 1}";
        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

            // Tests supertype `NodeValue`
            try (var nodes = NodeValue.findNodes(tree.getRootNode())) {
                List<String> foundValues = nodes.map(NodeValue::getText).toList();
                assertEquals(List.of(source, "\"a\"", "1"), foundValues);
            }
        }
    }

    @Test
    void testNodeDelegatingMethods() {
        String source = "[]";

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

            var typedNode = (NodeArray) tree.getRootNode().getChildren().getFirst();
            assertEquals(NodeArray.TYPE_NAME, typedNode.getNode().getType());
            assertFalse(typedNode.hasError());
            assertEquals("[]", typedNode.getText());

            Point startPoint = new Point(0, 0);
            assertEquals(startPoint, typedNode.getStartPoint());

            Point endPoint = new Point(0, 2);
            assertEquals(endPoint, typedNode.getEndPoint());

            assertEquals(new Range(startPoint, endPoint, 0, 2), typedNode.getRange());

            assertEquals(List.of(), typedNode.getChildren());
            assertEquals(List.of("[", "]"), typedNode.getUnnamedChildren());
        }
    }

    // Suppress warnings because this intentionally tests the `equals` implementation
    @SuppressWarnings({"SimplifiableAssertion", "EqualsWithItself", "EqualsBetweenInconvertibleTypes", "ConstantValue"})
    @Test
    void testTreeEquals_HashCode() {
        String source = "true";

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

            // Note: Currently jtreesitter's Tree does not override `equals` and `hashCode` from Object,
            // therefore don't compare different jtreesitter Tree instances here

            assertEquals(tree.hashCode(), tree.hashCode());

            assertTrue(tree.equals(tree));
            assertFalse(tree.equals(null));
            // typed tree and jtreesitter tree should not be equal
            assertFalse(tree.equals(tree.getTree()));

            // Unwrap and wrap again
            var otherTree = TypedTree.fromTree(tree.getTree());
            assertNotSame(tree, otherTree);
            assertEquals(tree.hashCode(), otherTree.hashCode());
            assertTrue(tree.equals(otherTree));
        }
    }

    // Suppress warnings because this intentionally tests the `equals` implementation
    @SuppressWarnings({"SimplifiableAssertion", "EqualsWithItself", "EqualsBetweenInconvertibleTypes", "ConstantValue"})
    @Test
    void testNodeEquals_HashCode() {
        // Contains `true` twice, but they are separate nodes and should not be considered equal
        String source = "true\ntrue";

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

            var document = tree.getRootNode();
            NodeTrue nodeA = (NodeTrue) document.getChildren().get(0);
            NodeTrue nodeB = (NodeTrue) document.getChildren().get(1);

            assertEquals(nodeA.hashCode(), nodeA.hashCode());

            assertTrue(nodeA.equals(nodeA));
            assertTrue(nodeB.equals(nodeB));
            assertFalse(nodeA.equals(nodeB));
            assertFalse(nodeA.equals(null));
            // typed node and jtreesitter node should not be equal
            assertFalse(nodeA.equals(nodeA.getNode()));

            // Unwrap and wrap again
            var otherA = NodeTrue.fromNodeThrowing(nodeA.getNode());
            assertNotSame(nodeA, otherA);
            assertEquals(nodeA.hashCode(), otherA.hashCode());
            assertTrue(nodeA.equals(otherA));
        }
    }
}
