package language.java;

import com.example.java_optional.*;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the generated code for tree-sitter-java, with {@link Optional}.
 *
 * <p>Uses code generated for {@code node-types-java.json}.
 */
class JavaOptionalTest extends AbstractTypedTreeTest {
    private JavaOptionalTest() {
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
        String source = "public class Main { }";
        try (var tree = parseNoError(source)) {
            var classDeclaration = (NodeClassDeclaration) tree.getRootNode().getChildren().getFirst();
            var modifiers = classDeclaration.getChild();
            assertEquals(Optional.of("public"), modifiers.flatMap(NodeModifiers::getText));
        }

        source = "class Main { }";
        try (var tree = parseNoError(source)) {
            var classDeclaration = (NodeClassDeclaration) tree.getRootNode().getChildren().getFirst();
            var modifiers = classDeclaration.getChild();
            assertEquals(Optional.empty(), modifiers);
        }
    }

    @Test
    void testFromNode() {
        String source = "public class Main { }";

        try (var tree = parseNoError(source)) {
            var classNode = tree.getRootNode().getChildren().getFirst().getNode();

            assertInstanceOf(NodeClassDeclaration.class, NodeClassDeclaration.fromNode(classNode).orElseThrow());
            assertInstanceOf(NodeClassDeclaration.class, NodeClassDeclaration.fromNodeThrowing(classNode));
            assertInstanceOf(NodeClassDeclaration.class, NodeDeclaration.fromNode(classNode).orElseThrow());
            assertInstanceOf(NodeClassDeclaration.class, NodeDeclaration.fromNodeThrowing(classNode));
            assertInstanceOf(NodeClassDeclaration.class, TypedNode.fromNode(classNode).orElseThrow());
            assertInstanceOf(NodeClassDeclaration.class, TypedNode.fromNodeThrowing(classNode));

            assertEquals(Optional.empty(), NodeInterfaceDeclaration.fromNode(classNode));
            var e = assertThrows(IllegalArgumentException.class, () -> NodeInterfaceDeclaration.fromNodeThrowing(classNode));
            assertEquals("Wrong node type: class_declaration", e.getMessage());
        }
    }
}
