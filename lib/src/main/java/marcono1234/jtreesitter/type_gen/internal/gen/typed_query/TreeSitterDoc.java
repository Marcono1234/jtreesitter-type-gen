package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;

/**
 * Provides links to the Tree-sitter query syntax documentation.
 */
enum TreeSitterDoc {
    FIELD("1-syntax.html#fields", "fields"),
    NEGATED_FIELD("1-syntax.html#negated-fields", "negated fields"),
    ANONYMOUS_NODE("1-syntax.html#anonymous-nodes", "anonymous nodes"),
    WILDCARD_NODE("1-syntax.html#the-wildcard-node", "the wildcard node"),
    ERROR_NODE("1-syntax.html#the-error-node", "the ERROR node"),
    MISSING_NODE("1-syntax.html#the-missing-node", "the MISSING node"),
    SUPERTYPE_NODE("1-syntax.html#supertype-nodes", "supertype nodes"),
    CAPTURE("2-operators.html#capturing-nodes", "capturing nodes"),
    QUANTIFICATION_OPERATOR("2-operators.html#quantification-operators", "quantification operators"),
    GROUP("2-operators.html#grouping-sibling-nodes", "grouping sibling nodes"),
    ALTERNATION("2-operators.html#alternations", "alternations"),
    ANCHOR("2-operators.html#anchors", "anchors"),
    PREDICATES("3-predicates-and-directives.html#predicates", "predicates"),
    ;

    private final String url;
    private final String sectionName;

    TreeSitterDoc(String url, String sectionName) {
        this.url = "https://tree-sitter.github.io/tree-sitter/using-parsers/queries/" + url;
        this.sectionName = sectionName;
    }

    public String createHtmlLink() {
        return "<a href=\"" + url + "\">Tree-sitter documentation '" + sectionName + "'</a>";
    }

    /**
     * Creates a Javadoc {@code @see ...} block tag for this doc link.
     */
    public String createJavadocSee() {
        return "\n@see " + createHtmlLink();
    }
}
