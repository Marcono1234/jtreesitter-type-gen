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
}
