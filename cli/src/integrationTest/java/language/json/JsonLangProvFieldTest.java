package language.json;

import com.example.json_lang_language_field.*;
import io.github.treesitter.jtreesitter.Language;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the generated code for tree-sitter-json, with a {@link Language} instance provided by
 * {@link LanguageProvider#languageField}.
 *
 * <p>Uses code generated for {@code node-types-json.json}.
 */
class JsonLangProvFieldTest extends AbstractTypedTreeTest {
    private JsonLangProvFieldTest() {
        super("json", ".json");
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

    // Suppress warning for switched `assertEquals` arguments; intentionally assert that value of generated
    // constant fields (= 'actual') has the expected value
    @SuppressWarnings("MisorderedAssertEqualsArguments")
    @Test
    void test() {
        String source = """
            {"a": 1}
            """;

        try (var tree = parseNoError(source)) {
            var rootNode = tree.getRootNode();
            assertEquals(rootNode.getNode().getType(), NodeDocument.TYPE_NAME);
            assertEquals(rootNode.getNode().getSymbol(), NodeDocument.TYPE_ID);
            assertEquals(language.getSymbolForName(NodeDocument.TYPE_NAME, true), NodeDocument.TYPE_ID);

            var object = (NodeObject) rootNode.getChildren().getFirst();
            var pair = object.getChildren().getFirst();

            // Verify that field methods work (which internally use the field ID)
            assertInstanceOf(NodeString.class, pair.getFieldKey());
            assertInstanceOf(NodeNumber.class, pair.getFieldValue());

            var pairNode = pair.getNode();
            assertEquals(pairNode.getType(), NodePair.TYPE_NAME);
            assertEquals(pairNode.getSymbol(), NodePair.TYPE_ID);
            assertEquals(language.getSymbolForName(NodePair.TYPE_NAME, true), NodePair.TYPE_ID);

            assertEquals(language.getFieldIdForName(NodePair.FIELD_KEY), NodePair.FIELD_KEY_ID);
            assertEquals(language.getFieldIdForName(NodePair.FIELD_VALUE), NodePair.FIELD_VALUE_ID);

            var keyNode = pair.getFieldKey().getNode();
            assertEquals(keyNode, pairNode.getChildByFieldName(NodePair.FIELD_KEY).orElseThrow());
            assertEquals(keyNode, pairNode.getChildByFieldId(NodePair.FIELD_KEY_ID).orElseThrow());

            var valueNode = pair.getFieldValue().getNode();
            assertEquals(valueNode, pairNode.getChildByFieldName(NodePair.FIELD_VALUE).orElseThrow());
            assertEquals(valueNode, pairNode.getChildByFieldId(NodePair.FIELD_VALUE_ID).orElseThrow());
        }
    }

    /**
     * Simple typed query test which ensures that the {@link Language} is obtained automatically from the
     * language provider.
     */
    @Test
    void typedQuery() {
        var q = new TypedQuery.Builder<List<NodePair>>();
        var query = q.nodePair()
            .withFieldValue(q.nodeNumber().textEq("12"))
            .captured(List::add)
            .buildQuery();

        String source = """
            {
              "a": 1,
              "b": 12,
              "c": 123
            }
            """;
        try (
            query;
            var tree = parseNoError(source);
            var matches = query.findMatches(tree.getRootNode().getNode())
        ) {
            var nodes = new ArrayList<NodePair>();
            matches.forEach(m -> m.collectCaptures(nodes));
            assertEquals(List.of("\"b\": 12"), nodes.stream().map(NodePair::getText).toList());
        }
    }

    // TODO: Move this test method to `JavaNullableTest` once validation is based on node-types.json and not on `language`
    //   (have to adjust node types used in the assertions below to use the Java types instead of JSON types)
    // TODO: Also add case for `q.unnamedNode(NodeStatement.TYPE_NAME, ";")` then, which should be allowed (but is already covered by other tests)
    @Test
    void typedQuery_UnnamedNode_Validation() {
        var q = new TypedQuery.Builder<>();
        assertDoesNotThrow(() -> q.unnamedNode(":"));

        var e = assertThrows(IllegalArgumentException.class, () -> q.unnamedNode("x"));
        assertEquals("Unknown unnamed node type: x", e.getMessage());

        // Should also fail when trying to use named node type
        e = assertThrows(IllegalArgumentException.class, () -> q.unnamedNode(NodeValue.TYPE_NAME));
        assertEquals("Unknown unnamed node type: " + NodeValue.TYPE_NAME, e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> q.unnamedNode("unknown", ":"));
        assertEquals("Unknown supertype node type: unknown", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> q.unnamedNode(NodeValue.TYPE_NAME, ":"));
        assertEquals("Node type '" + NodeValue.TYPE_NAME + "' is not a supertype of ':'", e.getMessage());
    }
}
