package language.java;

import com.example.java.*;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the generated code for tree-sitter-java, with nullable annotations.
 *
 * <p>Uses code generated for {@code node-types-java.json}.
 */
class JavaNullableTest extends AbstractTypedTreeTest {
    private JavaNullableTest() {
        super("java", ".java");
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
            public class Main {
                public static void main(String... args) throws IllegalArgumentException {
                    System.out.println("hello");
                }
            }
            """;

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());
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

            var callArgument = (NodeStringLiteral) call.getFieldArguments().getChildren().getFirst();
            assertEquals("\"hello\"", callArgument.getText());
        }
    }

    @Test
    void testNullable() {
        String source = "public class Main { }";
        try (var tree = TypedTree.fromTree(parse(source))) {
            var classDeclaration = (NodeClassDeclaration) tree.getRootNode().getChildren().getFirst();
            var modifiers = classDeclaration.getChild();
            assertNotNull(modifiers);
            assertEquals("public", modifiers.getText());
        }


        source = "class Main { }";
        try (var tree = TypedTree.fromTree(parse(source))) {
            var classDeclaration = (NodeClassDeclaration) tree.getRootNode().getChildren().getFirst();
            var modifiers = classDeclaration.getChild();
            assertNull(modifiers);
        }
    }

    @Test
    void testFromNode() {
        String source = "public class Main { }";

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

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

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

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
        }
    }
}
