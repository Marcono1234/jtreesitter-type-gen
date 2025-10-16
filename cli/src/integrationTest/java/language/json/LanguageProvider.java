package language.json;

import io.github.treesitter.jtreesitter.Language;
import util.TestHelper;

// Used by generated code
public class LanguageProvider {
    public static final Language languageField = TestHelper.loadLanguage("json");

    public static Language languageMethod() {
        return languageField;
    }
}
