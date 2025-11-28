package language.json.typed_query_old;

import com.example.json.TypedNode;
import io.github.treesitter.jtreesitter.*;
import language.json.LanguageProvider;

import java.lang.foreign.SegmentAllocator;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TypedQuery<C> implements AutoCloseable {
    private final Query query;
    private final CaptureRegistry<C> captureRegistry;
    private final PredicateRegistry predicateRegistry;

    private TypedQuery(String queryString, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
        var language = LanguageProvider.languageMethod();
        this.query = new Query(language, queryString);
        this.captureRegistry = captureRegistry;
        this.predicateRegistry = predicateRegistry;
    }

    @Override
    public void close() {
        query.close();
    }

    @Override
    public String toString() {
        return query.toString();
    }

    // Internal
    private static class CaptureRegistry<C> {
        private static final String CAPTURE_PREFIX = "c";
        private final List<CaptureHandler<C, ?>> handlers = new ArrayList<>();

        public String register(CaptureHandler<C, ?> captureHandler) {
            Objects.requireNonNull(captureHandler);
            String captureName = CAPTURE_PREFIX + handlers.size();
            handlers.add(captureHandler);
            return captureName;
        }

        public void invokeHandler(C collector, String captureName, TypedNode node) {
            // Ignore captures used by predicates
            if (captureName.startsWith(PredicateRegistry.PREFIX) || captureName.startsWith(PredicateRegistry.BUILT_IN_QUERY_CAPTURE_PREFIX)) {
                return;
            }

            if (!captureName.startsWith(CAPTURE_PREFIX)) {
                throw new IllegalArgumentException("Invalid capture name: " + captureName);
            }
            int handlerIndex = Integer.parseInt(captureName.substring(CAPTURE_PREFIX.length()));
            @SuppressWarnings("unchecked")
            CaptureHandler<C, TypedNode> handler = (CaptureHandler<C, TypedNode>) handlers.get(handlerIndex);
            handler.consumeCapture(collector, node);
        }
    }

    private static class PredicateRegistry {
        /** Prefix used for the predicates as well as the captures */
        private static final String PREFIX = "p";
        private final List<Predicate<Stream<TypedNode>>> predicates = new ArrayList<>();

        private static final String BUILT_IN_QUERY_CAPTURE_PREFIX = "pb";
        private int builtInQueryCaptureIndex = 0;

        public String register(Predicate<Stream<TypedNode>> predicate) {
            Objects.requireNonNull(predicate);
            String predicateName = PREFIX + predicates.size();
            predicates.add(predicate);
            return predicateName;
        }

        public String requestBuiltInQueryCapture() {
            return BUILT_IN_QUERY_CAPTURE_PREFIX + (builtInQueryCaptureIndex++);
        }

        public boolean check(QueryPredicate queryPredicate, QueryMatch queryMatch) {
            assert queryPredicate.getArgs().isEmpty();
            var predicateName = queryPredicate.getName();
            if (!predicateName.startsWith(PREFIX) || !predicateName.endsWith("?")) {
                throw new IllegalArgumentException("Invalid predicate name: " + predicateName);
            }
            int predicateIndex = Integer.parseInt(predicateName.substring(PREFIX.length(), predicateName.length() - 1));
            var predicate = predicates.get(predicateIndex);

            // Predicate name is also used as capture name
            // Remove trailing '?'
            var captureName = predicateName.substring(0, predicateName.length() - 1);

            var captures = queryMatch.findNodes(captureName).stream().map(TypedNode::fromNodeThrowing);
            return predicate.test(captures);
        }
    }

    public interface QNode<C, N> {
        TypedQuery<C> buildQuery();
    }

    private static abstract class QNodeImpl<C, N> implements QNode<C, N> {
        // Expect that every QNode is actually an instance of QNodeImpl
        static <C, N> QNodeImpl<C, N> from(QNode<C, N> node) {
            return (QNodeImpl<C, N>) Objects.requireNonNull(node);
        }

        @SuppressWarnings("unchecked")
        static <C, N> List<QNodeImpl<C, ? extends N>> listOf(QNode<C, ? extends N>... nodes) {
            return (List<QNodeImpl<C, ? extends N>>) (List<?>) List.of(nodes);
        }

        abstract void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry);

        @Override
        public TypedQuery<C> buildQuery() {
            var queryStringBuilder = new StringBuilder();
            var captureRegistry = new CaptureRegistry<C>();
            var predicateRegistry = new PredicateRegistry();
            buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            return new TypedQuery<>(queryStringBuilder.toString(), captureRegistry, predicateRegistry);
        }
    }

    public static abstract class QQuantifiable<C, N> extends QNodeImpl<C, N> {
        QQuantifiable() {
        }

        public QNode<C, N> zeroOrMore() {
            return new QQuantified<>(this, '*');
        }

        public QNode<C, N> oneOrMore() {
            return new QQuantified<>(this, '+');
        }

        public QNode<C, N> optional() {
            return new QQuantified<>(this, '?');
        }
    }

    // Does not extend 'quantifiable' to prevent applying multiple quantifiers to same node
    private static class QQuantified<C, N> extends QNodeImpl<C, N> {
        private final QNodeImpl<C, N> node;
        private final char quantifier;

        private QQuantified(QNodeImpl<C, N> node, char quantifier) {
            this.node = node;
            this.quantifier = quantifier;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            node.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            queryStringBuilder.append(quantifier);
        }
    }

    private static class QGroup<C, N> extends QQuantifiable<C, N> {
        private final List<QNodeImpl<C, ? extends N>> nodes;

        private QGroup(List<QNodeImpl<C, ? extends N>> nodes) {
            this.nodes = nodes;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            queryStringBuilder.append('(');
            nodes.forEach(n -> {
                n.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
                queryStringBuilder.append(' ');
            });
            queryStringBuilder.append(')');
        }
    }

    private static class QAlternation<C, N> extends QQuantifiable<C, N> {
        private final List<QNodeImpl<C, ? extends N>> nodes;

        private QAlternation(List<QNodeImpl<C, ? extends N>> nodes) {
            this.nodes = nodes;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            queryStringBuilder.append('[');
            nodes.forEach(n -> {
                n.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
                queryStringBuilder.append(' ');
            });
            queryStringBuilder.append(']');
        }
    }

    // TODO: Move all these static builder methods to typed builder as well, where `C` is bound

    public static <C, N> QQuantifiable<C, N> unnamedNode(String nodeType) {
        return new QUnnamedNode<>(null, nodeType);
    }

    // Note: Could maybe provide type-safety or checks here for `supertype` (i.e. to verify that supertype exists),
    // but not for the unnamed node type
    public static <C, N> QQuantifiable<C, N> unnamedNode(String supertype, String nodeType) {
        Objects.requireNonNull(supertype);
        return new QUnnamedNode<>(null, nodeType);
    }

    // TODO: Can wildcard, error or missing nodes have children or fields?


    @SuppressWarnings("unchecked")
    public static <C, N> QNode<C, N> anyNamedNode() {
        return (QWildcardNode<C, N>) QWildcardNode.NAMED;
    }

    @SuppressWarnings("unchecked")
    public static <C, N> QNode<C, N> anyNode() {
        return (QWildcardNode<C, N>) QWildcardNode.NAMED_OR_UNNAMED;
    }

    @SuppressWarnings("unchecked")
    public static <C, N> QNode<C, N> errorNode() {
        return (QErrorNode<C, N>) QErrorNode.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <C, N> QNode<C, N> missingNode() {
        return (QMissingNode<C, N>) QMissingNode.ANY;
    }

    @SafeVarargs
    public static <C, N> QNode<C, N> group(QNode<C, ? extends N>... nodes) {
        // Use `List#of` to disallow null elements
        return new QGroup<>(QNodeImpl.listOf(nodes));
    }

    @SafeVarargs
    public static <C, N> QNode<C, N> alternation(QNode<C, ? extends N>... nodes) {
        // Use `List#of` to disallow null elements
        return new QAlternation<>(QNodeImpl.listOf(nodes));
    }

    // TODO: How can capturing be supported for wildcard, error and missing nodes in a type-safe way?

    private static String createStringLiteral(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // Non-named node, which does not necessarily have a dedicated typed node class
    private static class QUnnamedNode<C, N> extends QQuantifiable<C, N> {
        private final String supertype;
        private final String nodeType;

        private QUnnamedNode(String supertype, String nodeType) {
            this.supertype = supertype;
            this.nodeType = nodeType;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            if (supertype != null) {
                queryStringBuilder.append('(').append(supertype).append('/');
            }
            queryStringBuilder.append(createStringLiteral(nodeType));
            if (supertype != null) {
                queryStringBuilder.append(')');
            }
        }
    }

    private static class QWildcardNode<C, N> extends QQuantifiable<C, N> {
        public static final QWildcardNode<?, ?> NAMED = new QWildcardNode<>(true);
        public static final QWildcardNode<?, ?> NAMED_OR_UNNAMED = new QWildcardNode<>(false);

        private final boolean isNamed;

        private QWildcardNode(boolean isNamed) {
            this.isNamed = isNamed;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            if (isNamed) queryStringBuilder.append('(');
            queryStringBuilder.append('_');
            if (isNamed) queryStringBuilder.append(')');
        }
    }

    private static class QErrorNode<C, N> extends QQuantifiable<C, N> {
        public static final QErrorNode<?, ?> INSTANCE = new QErrorNode<>();

        private QErrorNode() {}

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            queryStringBuilder.append("(ERROR)");
        }
    }

    private static class QMissingNode<C, N> extends QQuantifiable<C, N> {
        public static final QMissingNode<?, ?> ANY = new QMissingNode<>(null);

        private final String nodeType;

        // TODO: How to construct specific missing nodes in a type-safe way? Maybe add `asMissing()` to `QUnnamedNode` and `QTypedNode`?
        //   but for typed node have to make sure no children, fields or captures have been specified yet
        QMissingNode(String nodeType) {
            this.nodeType = nodeType;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            queryStringBuilder.append("(MISSING");
            if (nodeType != null) {
                queryStringBuilder.append(' ').append(nodeType);
            }
            queryStringBuilder.append(')');
        }
    }

    public sealed interface QFilterable<C, N extends TypedNode> extends QNode<C, N> {
        default QCapturable<C, N> textEq(String s) {
            // `List#of` disallows null elements
            return new QFiltered<>(QNodeImpl.from(this), new QFiltered.BuiltinPredicate("eq", List.of(s)));
        }

        default QCapturable<C, N> textNotEq(String s) {
            // `List#of` disallows null elements
            return new QFiltered<>(QNodeImpl.from(this), new QFiltered.BuiltinPredicate("not-eq", List.of(s)));
        }

        default QCapturable<C, N> textAnyOf(String... s) {
            // `List#of` disallows null elements
            var args = List.of(s);
            if (args.isEmpty()) {
                throw new IllegalArgumentException("Must specify at least one string");
            }
            return new QFiltered<>(QNodeImpl.from(this), new QFiltered.BuiltinPredicate("any-of", args));
        }

        // Don't support `match?` regex predicate; a type-safe implementation should accept a Java `Pattern` but then
        // would have to convert it to string just so that jtreesitter in the end recreates it as `Pattern`, which is
        // inefficient and error-prone; maybe users should rather use a custom predicate for it


        default QCapturable<C, N> matching(Predicate<? super Stream<N>> predicate) {
            @SuppressWarnings("unchecked")
            var predicateU = (Predicate<Stream<TypedNode>>) (Predicate<?>) Objects.requireNonNull(predicate);
            return new QFiltered<>(QNodeImpl.from(this), new QFiltered.CustomPredicate(predicateU));
        }
    }

    public sealed interface QCapturable<C, N extends TypedNode> extends QFilterable<C, N> {
        default QNode<C, N> captured(CaptureHandler<C, N> captureHandler) {
            return new QCaptured<>(QNodeImpl.from(this), captureHandler);
        }
    }

    // Implements `QCapturable` to still allow capturing, even if filtering has been performed
    private static final class QFiltered<C, N extends TypedNode> extends QNodeImpl<C, N> implements QCapturable<C, N> {
        private interface QPredicate {
            void build(StringBuilder queryStringBuilder, PredicateRegistry predicateRegistry);
        }
        private record BuiltinPredicate(String name, List<String> args) implements QPredicate {
            @Override
            public void build(StringBuilder queryStringBuilder, PredicateRegistry predicateRegistry) {
                var captureName = predicateRegistry.requestBuiltInQueryCapture();
                queryStringBuilder
                    .append('@').append(captureName)
                    .append(" (")
                    .append('#').append(name).append("?")
                    .append(' ')
                    .append('@').append(captureName);

                args.forEach(a -> queryStringBuilder.append(' ').append(createStringLiteral(a)));
                queryStringBuilder.append(')');
            }
        }
        private record CustomPredicate(Predicate<Stream<TypedNode>> predicate) implements QPredicate {
            @Override
            public void build(StringBuilder queryStringBuilder, PredicateRegistry predicateRegistry) {
                var predicateName = predicateRegistry.register(predicate);
                // Capture name is the same as the predicate name
                @SuppressWarnings("UnnecessaryLocalVariable")
                var captureName = predicateName;
                queryStringBuilder
                    .append('@').append(captureName)
                    .append(" (")
                    .append('#')
                    .append(predicateName)
                    // Don't add any predicate args; when evaluating predicate it will get the corresponding capture
                    // based on the predicate name
                    .append("?)");
            }
        }

        private final QNodeImpl<C, N> node;
        private final QPredicate predicate;

        QFiltered(QNodeImpl<C, N> node, QPredicate predicate) {
            this.node = node;
            this.predicate = predicate;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            // To avoid any ambiguity wrap the whole node in a group
            queryStringBuilder.append('(');
            node.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            queryStringBuilder.append(' ');
            predicate.build(queryStringBuilder, predicateRegistry);
            queryStringBuilder.append(')');
        }
    }

    // Don't extend 'quantifiable node' because quantifier has to be applied before capture
    private static class QCaptured<C, N extends TypedNode> extends QNodeImpl<C, N> {
        private final QNodeImpl<C, N> node;
        private final CaptureHandler<C, ?> captureHandler;

        private QCaptured(QNodeImpl<C, N> node, CaptureHandler<C, ?> captureHandler) {
            this.node = Objects.requireNonNull(node);
            this.captureHandler = Objects.requireNonNull(captureHandler);
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            node.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            var captureName = captureRegistry.register(captureHandler);
            queryStringBuilder.append(" @").append(captureName);
        }
    }

    public abstract sealed static class QCapturableQuantifiable<C, N extends TypedNode> extends QQuantifiable<C, N> implements QCapturable<C, N> {
        private QCapturableQuantifiable() {
        }

        private static <C, N> QQuantified<C, N> asQuantified(QNode<C, N> node) {
            return (QQuantified<C, N>) node;
        }

        @Override
        public QCapturable<C, N> zeroOrMore() {
            return new QCapturableQuantified<>(asQuantified(super.zeroOrMore()));
        }

        @Override
        public QCapturable<C, N> oneOrMore() {
            return new QCapturableQuantified<>(asQuantified(super.oneOrMore()));
        }

        @Override
        public QCapturable<C, N> optional() {
            return new QCapturableQuantified<>(asQuantified(super.optional()));
        }
    }

    private static final class QCapturableQuantified<C, N extends TypedNode> extends QQuantified<C, N> implements QCapturable<C, N> {
        private QCapturableQuantified(QQuantified<C, N> quantifiedNode) {
            super(quantifiedNode.node, quantifiedNode.quantifier);
        }
    }

    public static abstract non-sealed class QTypedNode<C, N extends TypedNode> extends QCapturableQuantifiable<C, N> {
        private sealed interface ChildEntry<C> {
            void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry);
        }
        private record Child<C>(QNodeImpl<C, ?> node) implements ChildEntry<C> {
            public Child {
                Objects.requireNonNull(node);
            }

            @Override
            public void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
                node.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            }
        }
        private record Field<C>(String name, QNodeImpl<C, ?> node) implements ChildEntry<C> {
            public Field {
                Objects.requireNonNull(name);
                Objects.requireNonNull(node);
            }

            @Override
            public void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
                queryStringBuilder
                    .append(name)
                    .append(": ");
                node.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            }
        }
        private static final class Anchor<C> implements ChildEntry<C> {
            private static final Anchor<?> INSTANCE = new Anchor<>();

            @SuppressWarnings("unchecked")
            public static <C> Anchor<C> instance() {
                return (Anchor<C>) INSTANCE;
            }

            @Override
            public void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
                queryStringBuilder.append('.');
            }
        }

        // TODO: Maybe also verify that fields which are only permitted to occur once are not specified multiple times
        record Data<C>(
            // Have a single list combining non-field and field children because apparently anchors can appear
            // between them, and order matters in that case
            List<ChildEntry<C>> children,
            Set<String> withFieldNames,
            // LinkedHashSet to preserve order
            LinkedHashSet<String> withoutFields
        ) {
            public Data() {
                this(List.of(), new LinkedHashSet<>(), new LinkedHashSet<>());
            }

            @SafeVarargs
            public final Data<C> withChildren(QNode<C, ?>... additionalChildren) {
                var children = new ArrayList<>(this.children);
                Arrays.stream(additionalChildren).map(QNodeImpl::from).map(Child::new).forEach(children::add);
                return new Data<>(children, withFieldNames, withoutFields);
            }

            public Data<C> withField(String fieldName, QNode<C, ?> fieldNode) {
                Objects.requireNonNull(fieldName);
                Objects.requireNonNull(fieldNode);

                if (withoutFields.contains(fieldName)) {
                    throw new IllegalArgumentException("Field '" + fieldName + "' has already been added to \"without fields\"");
                }
                var withFieldNames = new HashSet<>(this.withFieldNames);
                if (!withFieldNames.add(fieldName)) {
                    throw new IllegalArgumentException("Field '" + fieldName + "' has already been added");
                }
                var children = new ArrayList<>(this.children);
                children.add(new Field<>(fieldName, QNodeImpl.from(fieldNode)));
                return new Data<>(children, withFieldNames, withoutFields);
            }

            public Data<C> withoutField(String fieldName) {
                Objects.requireNonNull(fieldName);
                if (withFieldNames.contains(fieldName)) {
                    throw new IllegalArgumentException("Field '" + fieldName + "' has already been added to \"with fields\"");
                }
                var withoutFields = new LinkedHashSet<>(this.withoutFields);
                withoutFields.add(fieldName);
                return new Data<>(children, withFieldNames, withoutFields);
            }

            public Data<C> withAnchor() {
                if (!children.isEmpty() && children.getLast() instanceof Anchor) {
                    throw new IllegalStateException("Duplicate anchor is not valid");
                }
                var children = new ArrayList<>(this.children);
                children.add(Anchor.instance());
                return new Data<>(children, withFieldNames, withoutFields);
            }

            void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
                // Apparently have to write 'without fields' first, because query parser permits them in front of
                // "first child" anchor, but disallows them after "last child" anchor
                withoutFields.forEach(f -> queryStringBuilder.append("!").append(f));
                children.forEach(c -> {
                    queryStringBuilder.append(' ');
                    c.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
                });
            }
        }

        final String supertype;
        private final String nodeType;
        final Data<C> data;

        QTypedNode(String supertype, String nodeType, Data<C> data) {
            this.supertype = supertype;
            this.nodeType = nodeType;
            this.data = data;
        }

        QTypedNode(String nodeType) {
            this(null, nodeType, new Data<>());
        }

        QTypedNode(QTypedNode<C, N> old, String supertype) {
            this.supertype = supertype;
            this.nodeType = old.nodeType;
            this.data = old.data;
        }

        QTypedNode(QTypedNode<C, N> old, Data<C> data) {
            this.supertype = old.supertype;
            this.nodeType = old.nodeType;
            this.data = data;
        }

        @Override
        void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry) {
            queryStringBuilder.append('(');
            if (supertype != null) {
                queryStringBuilder.append(supertype).append('/');
            }
            queryStringBuilder.append(nodeType);
            data.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            queryStringBuilder.append(')');
        }

        // TODO: Generate this per subnode which has a supernode
        //   In the Query use https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#supertype-nodes syntax then
        // QTypedNode asSubtypeOf(Object supernode);

        // TODO: methods: `withChildren(...)` `withField...(...)`, `withChildAnchor()`
    }

    public interface CaptureHandler<C, N extends TypedNode> {
        void consumeCapture(C collector, N node);
    }

    public class TypedQueryMatch {
        private final QueryMatch queryMatch;

        private TypedQueryMatch(QueryMatch queryMatch) {
            this.queryMatch = queryMatch;
        }

        public void collectCaptures(C collector) {
            Objects.requireNonNull(collector);
            for (var capture : queryMatch.captures()) {
                // TODO: Have to ignore predicate captures
                var typedNode = TypedNode.fromNodeThrowing(capture.node());
                captureRegistry.invokeHandler(collector, capture.name(), typedNode);
            }
        }

        public QueryMatch getQueryMatch() {
            return queryMatch;
        }
    }

    private BiPredicate<QueryPredicate, QueryMatch> createPredicateCallback() {
        return predicateRegistry::check;
    }

    // TODO: Should use `TypedNode` as start node? Probably not, but maybe have convenience overloads taking TypedNode, which just call `TypedNode#getNode`
    public Stream<TypedQueryMatch> findMatches(Node node) {
        // TODO: Should verify that `node.getTree().getLanguage()` matches query language?
        var queryCursor = new QueryCursor(query);
        return queryCursor.findMatches(node, new QueryCursor.Options(createPredicateCallback()))
            .map(TypedQueryMatch::new)
            .onClose(queryCursor::close);
    }
    public Stream<TypedQueryMatch> findMatches(Node node, SegmentAllocator allocator) {
        // TODO: Should verify that `node.getTree().getLanguage()` matches query language?
        var queryCursor = new QueryCursor(query);
        return queryCursor.findMatches(node, allocator, new QueryCursor.Options(createPredicateCallback()))
            .map(TypedQueryMatch::new)
            .onClose(queryCursor::close);
    }

    /*
     * TODO: Support custom query predicates?
     *  Would create query string then which contains predicate without any args, because there is no point in including args
     *  in query string, letting tree-sitter parse the args and then pass to user Java code again, instead user Java code should
     *  include the args (e.g. string literal)
     *  So only provide `QueryMatch` to user code (maybe wrapped in 'typed' abstraction)
     *  -> But how to support defining captures?
     * -> Probably does not make sense to support custom queries; when java-tree-sitter evaluates them, it has already obtained
     *    QueryMatch anyway, so might be easier if user just defines normal capture and then performs filtering manually themselves
     *    during iteration of the results
     */

    // TODO: Support some common built-in predicates, but only those comparing node text against string
    //   can either be applied to existing capture, or implicitly create one?
}
