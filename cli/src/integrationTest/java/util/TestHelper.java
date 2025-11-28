package util;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;

import java.lang.foreign.*;
import java.nio.file.Path;

public class TestHelper {
    private TestHelper() {}

    public static Language loadLanguage(String langName) {
        // Use `toAbsolutePath()` to make troubleshooting easier in case lib is not found
        var libPath = Path.of(System.mapLibraryName("lang-" + langName)).toAbsolutePath();
        var languageSymbolLookup = SymbolLookup.libraryLookup(libPath, Arena.global());
        return Language.load(languageSymbolLookup, "tree_sitter_" + langName);
    }

    /**
     * Gets the underlying Tree-sitter query string from the given {@code TypedQuery} object.
     */
    // Has to use class `Object` because `TypedQuery` class differs per language
    public static String getQueryString(Object typedQuery) {
        try {
            // Generated code contains this non-public field for test assertions
            var field = typedQuery.getClass().getDeclaredField("queryString");
            field.setAccessible(true);
            return (String) field.get(typedQuery);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
