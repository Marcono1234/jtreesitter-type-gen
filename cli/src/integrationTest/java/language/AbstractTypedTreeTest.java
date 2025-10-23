package language;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;
import util.TestHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

// PER_CLASS is needed to use non-static `@MethodSource` below
@TestInstance(PER_CLASS)
public abstract class AbstractTypedTreeTest {
    protected static final boolean UPDATE_EXPECTED = System.getProperty("test-update-expected") != null;

    @AfterAll
    static void checkUpdatedExpected() {
        // Fail if expected was updated, to avoid accidentally always updating expected, and tests always 'succeeding'
        if (UPDATE_EXPECTED) {
            fail("Updated expected output");
        }
    }

    private final String languageName;
    private final String sourceFileExtension;
    protected final Language language;

    public AbstractTypedTreeTest(String languageName, String sourceFileExtension) {
        this.languageName = languageName;
        this.sourceFileExtension = sourceFileExtension;
        this.language = TestHelper.loadLanguage(languageName);
    }

    protected Tree parse(String source) {
        try (Parser parser = new Parser(language)) {
            return parser.parse(source).orElseThrow();
        }
    }

    private static final String EXPECTED_TYPED_TREE_EXTENSION = ".typed-tree.txt";

    Stream<Arguments> sourceCodeFiles() throws IOException {
        Path resourcesDir = Path.of("src/integrationTest/resources/typed-tree", languageName);
        try (var files = Files.list(resourcesDir)) {
            for (var file : (Iterable<Path>) files::iterator) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(sourceFileExtension) && !fileName.endsWith(EXPECTED_TYPED_TREE_EXTENSION)) {
                    fail("Unsupported file: " + file);
                }

                if (fileName.endsWith(EXPECTED_TYPED_TREE_EXTENSION)) {
                    String baseName = fileName.substring(0, fileName.length() - EXPECTED_TYPED_TREE_EXTENSION.length());
                    if (!Files.exists(file.resolveSibling(baseName + sourceFileExtension))) {
                        fail("Missing '%s' file for: %s".formatted(sourceFileExtension, file));
                    }
                }
            }
        }

        List<Arguments> arguments;
        try (var files = Files.list(resourcesDir)) {
            // Collect to List first because underlying `Files#list` stream will be closed in this `try` already
            arguments = files
                .filter(f -> f.getFileName().toString().endsWith(sourceFileExtension))
                .map(f -> Arguments.of(f.getFileName().toString(), f))
                .toList();
        }

        if (arguments.isEmpty()) {
            throw new IllegalStateException("Did not find test resources in: " + resourcesDir);
        }
        return arguments.stream();
    }

    /**
     * Parses the source code, passes the root node to the consumer and returns the result.
     *
     * <p>Note: Has to use type {@code Object} for root node because the different languages each have their own
     * generated {@code TypedNode} class.
     */
    protected abstract String parseSourceCode(String sourceCode, Function<Object, String> rootNodeConsumer);

    /**
     * Parses source code and checks the typed tree structure.
     *
     * <p>It calls the {@code TypedNode} methods recursively to walk the nodes and their children and converts
     * the tree to a string. Even though this does not cover all {@code TypedNode} functionality, it should still
     * achieve rather good coverage. For any special cases specific manual tests can be written.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sourceCodeFiles")
    void testTypedTreeString(String fileName, Path sourceCodePath) throws Exception {
        String baseFileName = fileName.substring(0, fileName.length() - sourceFileExtension.length());
        Path expectedFile = sourceCodePath.resolveSibling(baseFileName + EXPECTED_TYPED_TREE_EXTENSION);

        String sourceCode = Files.readString(sourceCodePath);

        String actualTypedTree = parseSourceCode(sourceCode, AbstractTypedTreeTest::getTypedTree);
        // Replace IDs to make test deterministic
        actualTypedTree = actualTypedTree.replaceAll("id=\\d+", "id=...");

        if (UPDATE_EXPECTED) {
            Files.writeString(expectedFile, actualTypedTree);
            throw new TestAbortedException("Expected file was updated");
        }

        String expectedTypedTree;
        if (Files.isRegularFile(expectedFile)) {
            expectedTypedTree = Files.readString(expectedFile);
        } else {
            throw (Exception) fail("Missing expected file: " + expectedFile);
        }

        assertEquals(expectedTypedTree, actualTypedTree);
    }


    // Note: Has to use type `Object` because the different languages each have their own generated `TypedNode` class
    private static String getTypedTree(Object node) {
        StringBuilder stringBuilder = new StringBuilder();
        getTypedTree(node, 0, stringBuilder);
        return stringBuilder.toString();
    }

    private static boolean isTypedNode(Class<?> nodeClass) {
        if (nodeClass.getSimpleName().equals("TypedNode")) {
            return true;
        }
        return Arrays.stream(nodeClass.getInterfaces()).anyMatch(AbstractTypedTreeTest::isTypedNode);
    }

    // TODO: Maybe this implementation is a bit too hacky / unreliable?
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    private static void getTypedTree(Object node, int indentation, StringBuilder stringBuilder) {
        // Sanity check to make sure this is not a completely unrelated object
        var nodeClass = node.getClass();
        assertTrue(isTypedNode(nodeClass), "Unexpected node class: " + nodeClass);

        stringBuilder.append("  ".repeat(indentation) + node + "\n");

        var methods = new ArrayList<>(Arrays.asList(nodeClass.getMethods()));
        // Sort methods to make their order deterministic
        methods.sort(Comparator.comparing(Method::getName));

        boolean hasChildrenOrFields = false;

        for (var method : methods) {
            String methodName = method.getName();
            // The names here are the default ones used by `NameGenerator`
            boolean isUnnamedChildren = methodName.equals("getUnnamedChildren");
            if (methodName.equals("getChild") || methodName.equals("getChildren") || methodName.startsWith("getField") || isUnnamedChildren) {
                if (!hasChildrenOrFields && !isUnnamedChildren) {
                    hasChildrenOrFields = true;
                }

                stringBuilder.append("  ".repeat(indentation) + "- " + methodName + "\n");

                Object result;
                try {
                    result = method.invoke(node);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (isUnnamedChildren && !((List<?>) result).isEmpty()) {
                    stringBuilder.append("  ".repeat(indentation + 1) + result + "\n");
                } else if (result instanceof List<?> children) {
                    for (var child : children) {
                        getTypedTree(child, indentation + 1, stringBuilder);
                    }
                } else if (result instanceof Optional<?> optional) {
                    optional.ifPresent(o -> getTypedTree(o, indentation + 1, stringBuilder));
                } else if (result != null) {
                    getTypedTree(result, indentation + 1, stringBuilder);
                }
            }
        }

        // For 'leaf' nodes include their text
        if (!hasChildrenOrFields) {
            String text;
            try {
                var getTextMethod = nodeClass.getMethod("getText");
                var result = getTextMethod.invoke(node);
                if (result instanceof Optional<?> optional) {
                    text = (String) optional.orElseThrow();
                } else {
                    text = (String) result;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            text = text.replace("\n", "\\n");
            stringBuilder.append("  ".repeat(indentation) + "- text: " + text + "\n");
        }
    }
}
