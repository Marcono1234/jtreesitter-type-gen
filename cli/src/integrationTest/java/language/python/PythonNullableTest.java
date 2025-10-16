package language.python;

import language.AbstractTypedTreeTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

/**
 * Tests the generated code for tree-sitter-python, with nullable annotations.
 *
 * <p>Uses code generated for {@code node-types-python.json}.
 */
@Disabled("TODO: Currently no code is generated, see build.gradle.kts") // TODO
class PythonNullableTest extends AbstractTypedTreeTest {
    private PythonNullableTest() {
        super("python", ".py");
    }

    @Override
    protected String parseSourceCode(String sourceCode, Function<Object, String> rootNodeConsumer) {
        throw new AssertionError("TODO");
    }

    @Test
    void test() {
        throw new AssertionError("TODO");
    }

    @Test
    void testFromNode() {
        throw new AssertionError("TODO");
    }

    @Test
    void testFindNodes() {
        throw new AssertionError("TODO");
    }
}
