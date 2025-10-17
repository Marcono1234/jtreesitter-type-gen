package language.json;

import com.example.json_lang_language_field.*;
import io.github.treesitter.jtreesitter.Language;
import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Test;

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

    @Override
    protected String parseSourceCode(String sourceCode, Function<Object, String> rootNodeConsumer) {
        try (var tree = TypedTree.fromTree(parse(sourceCode))) {
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

        try (var tree = TypedTree.fromTree(parse(source))) {
            assertFalse(tree.hasError());

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
}
