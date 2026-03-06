package marcono1234.jtreesitter.type_gen.internal.gen.utils;

import marcono1234.jtreesitter.type_gen.TypeName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenHelperTest {
    @Test
    void createClassName() {
        var className = CodeGenHelper.createClassName(new TypeName("", "MyClass"));
        assertEquals("", className.packageName());
        assertEquals("MyClass", className.simpleName());
        assertEquals(List.of("MyClass"), className.simpleNames());

        className = CodeGenHelper.createClassName(new TypeName("mypackage.nested", "MyClass"));
        assertEquals("mypackage.nested", className.packageName());
        assertEquals("MyClass", className.simpleName());
        assertEquals(List.of("MyClass"), className.simpleNames());

        className = CodeGenHelper.createClassName(new TypeName("mypackage", "MyClass$Nested1$Nested2"));
        assertEquals("mypackage", className.packageName());
        assertEquals("Nested2", className.simpleName());
        assertEquals(List.of("MyClass", "Nested1", "Nested2"), className.simpleNames());
    }

    @ParameterizedTest
    @CsvSource({
        "'',''",
        "test,test",
        "'a\nb','a\nb'",
        // Special HTML chars
        "a <b> &test;,a &lt;b&gt; &amp;test;",
        // Special Javadoc chars
        "a {,a &lbrace;",
        "a },a &rbrace;",
        "a @,a &commat;",
        "{@code a},&lbrace;&commat;code a&rbrace;",
        // Potential Unicode escape
        "a \\,a &bsol;",
        "a \\u1234,a &bsol;u1234",
        // Potential Javadoc end
        "a *,a &ast;",
        "a */,a &ast;/",
    })
    void escapeJavadocText(String text, String expected) {
        assertEquals(expected, CodeGenHelper.escapeJavadocText(text));
    }

    @ParameterizedTest
    @CsvSource({
        "'',{@code }",
        "test,{@code test}",
        // Line breaks are allowed
        "'a\nb','{@code a\nb}'",
        // Special HTML chars are allowed
        "a <b> &test;,{@code a <b> &test;}",
        // Special Javadoc chars
        "a {,<code>a &lbrace;</code>",
        "a },<code>a &rbrace;</code>",
        "a @,<code>a &commat;</code>",
        // Potential Unicode escape
        "a \\,<code>a &bsol;</code>",
        "a \\u1234,<code>a &bsol;u1234</code>",
        // Potential Javadoc end
        "a *,<code>a &ast;</code>",
        "a */,<code>a &ast;/</code>",
        // If content has to be escaped, then special HTML chars have to be escaped as well 
        "a { <b> &test;,<code>a &lbrace; &lt;b&gt; &amp;test;</code>",
    })
    void createJavadocCodeTag(String content, String expected) {
        assertEquals(expected, CodeGenHelper.createJavadocCodeTag(content));
    }
}
