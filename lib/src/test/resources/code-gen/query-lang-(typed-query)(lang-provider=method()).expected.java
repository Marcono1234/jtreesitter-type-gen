package org.example;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Unsigned;
import java.lang.Error;
import java.lang.IllegalArgumentException;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.Throwable;
import java.util.Objects;
import javax.annotation.processing.Generated;
import org.example.lang.LangProvider;

/**
 * Internal helper class.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
final class LanguageUtils {
  static final Language language;

  static {
    try {
      language = Objects.requireNonNull(LangProvider.method());
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Failed obtaining language instance", e);
    }
  }

  private LanguageUtils() {
  }

  public static @Unsigned short getTypeId(String name) {
    short id = language.getSymbolForName(name, true);
    if (id == 0) {
      throw new IllegalArgumentException("Unknown type name: " + name);
    }
    return id;
  }

  public static @Unsigned short getFieldId(String name) {
    short id = language.getFieldIdForName(name);
    if (id == 0) {
      throw new IllegalArgumentException("Unknown field name: " + name);
    }
    return id;
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.Class;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Internal helper class.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
final class NodeUtils {
  private NodeUtils() {
  }

  /**
   * Converts a jtreesitter node to a typed node, throwing an {@link IllegalArgumentException} if the node type is unknown or unexpected.
   * This method is intended for typed nodes which don't have a dedicated {@code fromNodeThrowing} method.
   */
  public static <T extends TypedNode> T fromNodeThrowing(Node node, Class<T> nodeClass) {
    var typedNode = TypedNode.fromNodeThrowing(node);
    if (nodeClass.isInstance(typedNode)) {
      return nodeClass.cast(typedNode);
    } else {
      throw new IllegalArgumentException("Unexpected node type, expected '" + nodeClass + "' but got: " + typedNode.getClass());
    }
  }

  /**
   * Gets all non-field children of the node.
   * @param named whether to return named or non-named children
   */
  public static List<Node> getNonFieldChildren(Node node, boolean named) {
    var children = new ArrayList<Node>();
    // Use custom allocator to ensure that nodes are usable after cursor was closed
    var arena = Arena.ofAuto();
    try (var cursor = node.walk()) {
      if (cursor.gotoFirstChild()) {
        do {
          // Only consider non-field children
          if (cursor.getCurrentFieldId() == 0) {
            var currentNode = cursor.getCurrentNode(arena);
            if (currentNode.isError() || currentNode.isMissing()) {
              throw new IllegalStateException("Child is error or missing node: " + currentNode);
            }
            if (currentNode.isNamed() == named && !currentNode.isExtra()) {
              children.add(currentNode);
            }
          }
        } while (cursor.gotoNextSibling());
      }
    }
    return children;
  }

  /**
   * Maps the children of a node (in the form of jtreesitter nodes) to typed nodes.
   * This differentiates between named and non-named children, since separate typed node classes are used for them.
   * @param namedMapper maps named children; {@code null} if only non-named children are expected
   * @param nonNamedMapper maps non-named children; {@code null} if only named children are expected
   */
  public static <T extends TypedNode> List<T> mapChildren(List<Node> children,
      Function<Node, ? extends T> namedMapper, Function<Node, ? extends T> nonNamedMapper) {
    return children.stream().map(child -> {
      if (child.isNamed()) {
        if (namedMapper == null) throw new IllegalArgumentException("Unexpected named child: " + child);
        return namedMapper.apply(child);
      } else {
        if (nonNamedMapper == null) throw new IllegalArgumentException("Unexpected non-named child: " + child);
        return nonNamedMapper.apply(child);
      }
    }).toList();
  }

  /**
   * @param namedNodeClass class of the named children; {@code null} if only non-named children are expected
   * @param nonNamedMapper maps non-named children; {@code null} if only named children are expected
   */
  public static <T extends TypedNode> List<T> mapChildren(List<Node> children,
      Class<? extends T> namedNodeClass, Function<Node, ? extends T> nonNamedMapper) {
    return mapChildren(children, n -> fromNodeThrowing(n, namedNodeClass), nonNamedMapper);
  }

  public static <T extends TypedNode> T requiredSingleChild(List<T> nodes) {
    if (nodes.size() == 1) {
      return nodes.getFirst();
    }
    throw new IllegalArgumentException("Unexpected nodes count: " + nodes);
  }

  public static <T extends TypedNode> @Nullable T optionalSingleChild(List<T> nodes) {
    T result = null;
    int size = nodes.size();
    if (size == 1) {
      result = nodes.getFirst();
    } else if (size > 1) {
      throw new IllegalArgumentException("Unexpected nodes count: " + nodes);
    }
    return result;
  }

  public static <T extends TypedNode> List<T> atLeastOneChild(List<T> nodes) {
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Expected at least one node");
    }
    return nodes;
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Point;
import io.github.treesitter.jtreesitter.Range;
import java.lang.IllegalArgumentException;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Base type for all 'typed nodes'.
 * A jtreesitter {@link Node} can be converted to a typed node with {@link #fromNode} or {@link #fromNodeThrowing},
 * or with the corresponding methods on the specific typed node classes.
 *
 * <h2>Node subtypes</h2>
 * <ul>
 * <li>{@link NodeFirst first}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface TypedNode permits NodeFirst {
  /**
   * Returns the underlying jtreesitter node.
   */
  Node getNode();

  /**
   * Returns the source code of this node, if available.
   */
  default @Nullable String getText() {
    var result = getNode().getText();
    return result;
  }

  /**
   * Returns the range of this node.
   */
  default Range getRange() {
    return getNode().getRange();
  }

  /**
   * Returns the start point of this node.
   */
  default Point getStartPoint() {
    return getNode().getStartPoint();
  }

  /**
   * Returns the end point of this node.
   */
  default Point getEndPoint() {
    return getNode().getEndPoint();
  }

  /**
   * Returns whether this node or any of its child nodes represents an ERROR.
   */
  default boolean hasError() {
    return getNode().hasError();
  }

  /**
   * Wraps a jtreesitter node as typed node, returning {@code null} if no corresponding typed node class exists.
   * Only works for <i>named</i> node types.
   *
   * @see #fromNodeThrowing
   */
  static @Nullable TypedNode fromNode(Node node) {
    var result = switch (node.getType()) {
      case NodeFirst.TYPE_NAME -> new NodeFirst(node);
      default -> null;
    }
    ;
    return result;
  }

  /**
   * Wraps a jtreesitter node as typed node, throwing an {@link IllegalArgumentException} if no corresponding typed node class exists.
   * Only works for <i>named</i> node types.
   *
   * @see #fromNode
   */
  static TypedNode fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Unknown node type: " + node.getType());
    }
    return typedNode;
  }
}


/* ==================== */ 

package org.example;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.processing.Generated;

/**
 * Indicates that the annotated container type will not be empty.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE_USE })
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public @interface NonEmpty {
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import io.github.treesitter.jtreesitter.Unsigned;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFirst implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "first";

  /**
   * Type ID of this node, assigned by tree-sitter.
   * @see Node#getSymbol
   */
  public static final @Unsigned short TYPE_ID = LanguageUtils.getTypeId(TYPE_NAME);

  private final Node node;

  NodeFirst(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static @Nullable NodeFirst fromNode(Node node) {
    NodeFirst result = null;
    if (node.getSymbol() == TYPE_ID) {
      result = new NodeFirst(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeFirst fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type: " + node.getType());
    }
    return typedNode;
  }

  /**
   * Returns the type names of the non-named, non-extra children, if any.
   *
   * <p><b>Important:</b> Whether this method has any useful or even any results at all depends on the grammar.
   * This method can be useful when the grammar defines a 'choice' of multiple keywords.
   * In that case this method returns the keywords which appear in the parsed source code.
   */
  public List<String> getUnnamedChildren() {
    return NodeUtils.getNonFieldChildren(node, false).stream().map(n -> n.getType()).toList();
  }

  private static Stream<NodeFirst> findNodesImpl(TypedNode startNode, SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeFirst.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, null);
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeFirst::fromNodeThrowing).onClose(() -> {
          queryCursor.close();
          query.close();
        });
  }

  /**
   * Gets all nodes of this type, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var nodes = NodeFirst.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeFirst> findNodes(TypedNode startNode, SegmentAllocator allocator) {
    Objects.requireNonNull(startNode);
    Objects.requireNonNull(allocator);
    return findNodesImpl(startNode, allocator);
  }

  /**
   * Gets all nodes of this type, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   * After the stream was closed the resulting nodes should not be used anymore, otherwise the behavior is undefined,
   * including exceptions being thrown or possibly even a JVM crash.
   * Use {@link #findNodes(TypedNode, SegmentAllocator)} to be able to access the nodes after the stream was closed.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var nodes = NodeFirst.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeFirst> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFirst other) {
      return node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeFirst" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import io.github.treesitter.jtreesitter.QueryMatch;
import io.github.treesitter.jtreesitter.QueryPredicate;
import java.lang.AutoCloseable;
import java.lang.FunctionalInterface;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.SafeVarargs;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.lang.foreign.SegmentAllocator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;

/**
 * Type-safe wrapper around a Tree-sitter query.
 *
 * <p>The general usage looks like this:
 * <ol>
 * <li>Build the query using {@link TypedQuery.Builder}
 * <li>Create a {@code TypedQuery} instance using {@link TypedQuery.QNode#buildQuery buildQuery} on one of the query node objects
 * <li>Execute the query using {@link #findMatches} or {@link #findMatchesAndCollect}
 * </ol>
 * Query builder nodes as well as the created {@code TypedQuery} are immutable.
 * The query should be {@linkplain #close() closed} eventually to release the underlying jtreesitter resources again.
 *
 * <h2>Capturing nodes</h2>
 * Just like regular Tree-sitter queries, a typed query can capture matching nodes so that they
 * can be further inspected by user code afterwards. To support this in a type-safe way, there are two concepts involved:
 * <ul>
 * <li>a 'collector'<br>
 * This is a user-defined type which handles query captures and either directly processes them or collects them
 * and makes them available after the query execution. In the typed query API this is represented as type variable {@code <C>}.
 * The collector is specified when query captures are retrieved using {@link TypedQuery.TypedQueryMatch#collectCaptures}.
 * <li>a {@linkplain TypedQuery.CaptureHandler 'capture handler'}<br>
 * This interface is implemented by the user. Capture handlers are registered using {@link TypedQuery.QCapturable#captured}.
 * They are called with the user-defined 'collector' and the captured node, and are then supposed to pass the
 * node to the collector.
 * </ul>
 * In the simplest case the 'collector' might just be a {@code List<TypedNode>} and the 'capture handlers'
 * are {@code List::add}. That means when {@code collectCaptures} is called a {@code List} is provided, the capture handlers
 * add the nodes to the list, and afterwards the captured nodes can be retrieved from the list.
 *
 * <p>However, depending on the use case a custom type might provide more flexibility. Consider this example
 * which collects the hypothetical {@code NodeStringLiteral} and {@code NodeIntLiteral}:
 * {@snippet lang=java :
 * class MyCollector {
 *   public void addStringLiteral(NodeStringLiteral l) { ... }
 *
 *   public void addIntLiteral(NodeIntLiteral l) { ... }
 *
 *   public List<NodeStringLiteral> getStringLiterals() { ... }
 *
 *   public List<NodeIntLiteral> getIntLiterals() { ... }
 * }
 *
 * var q = new TypedQuery.Builder<MyCollector>();
 * var typedQuery = q.alternation(
 *     q.nodeStringLiteral().captured((myCollector, node) -> myCollector.addStringLiteral(node)),
 *     q.nodeIntLiteral().captured((myCollector, node) -> myCollector.addIntLiteral(node))
 *   ).buildQuery()
 *
 * try (var matches = typedQuery.findMatches(startNode)) {
 *   var myCollector = new MyCollector();
 *   matches.forEach(match -> match.collectCaptures(myCollector));
 *   System.out.println("strings: " + myCollector.getStringLiterals());
 *   System.out.println("ints: " + myCollector.getIntLiterals());
 * }
 *
 * typedQuery.close();
 * }
 *
 * <h2>Example</h2>
 * Consider this hypothetical example for a 'class declaration' node which has fields 'name' and 'body', and
 * where the body then has children such as a 'field declaration':
 * {@snippet lang=java :
 * var q = new TypedQuery.Builder<List<NodeIntLiteral>>();
 * var typedQuery = q.nodeClassDeclaration()
 *   .withFieldName(q.nodeIdentifier().textEq("MyClass"))
 *   .withFieldBody(q.nodeClassBody().withChildren(
 *     q.nodeFieldDeclaration()
 *       // Capture int literals which are used as initializer
 *       .withFieldInitializer(q.nodeIntLiteral().captured(List::add))
 *   ))
 *   .buildQuery()
 *
 * try (var matches = typedQuery.findMatches(startNode)) {
 *   var intLiterals = new ArrayList<NodeIntLiteral>();
 *   matches.forEach(match -> match.collectCaptures(intLiterals));
 *   System.out.println("ints: " + intLiterals);
 * }
 *
 * typedQuery.close();
 * }
 *
 * @param <C> type of the user-defined 'collector' which processes query captures
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class TypedQuery<C> implements AutoCloseable {
  private final Language language;

  final String queryString;

  private final Query query;

  private final CaptureRegistry<C> captureRegistry;

  private final PredicateRegistry predicateRegistry;

  private TypedQuery(Language language, String queryString, CaptureRegistry<C> captureRegistry,
      PredicateRegistry predicateRegistry) {
    this.language = language;
    this.queryString = queryString;
    try {
      this.query = new Query(language, queryString);
    } catch (RuntimeException e) {
      throw new RuntimeException("Failed creating query; verify that children and fields are specified in the right order; if you expect the query to be valid please report this to the jtreesitter-type-gen maintainers; query string:\n\t" + queryString, e);
    }
    this.captureRegistry = captureRegistry;
    this.predicateRegistry = predicateRegistry;
  }

  /**
   * Releases the resources of the underlying jtreesitter query.
   * This query object should not be used anymore after this method has been called.
   */
  @Override
  public void close() {
    query.close();
  }

  @Override
  public String toString() {
    return "TypedQuery" + "[query=" + query.toString() + "]";
  }

  private BiPredicate<QueryPredicate, QueryMatch> createPredicateCallback() {
    return predicateRegistry::test;
  }

  /**
   * Executes the query and returns a stream of matches, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   * After the stream was closed captured nodes should not be used anymore, otherwise the behavior is undefined,
   * including exceptions being thrown or possibly even a JVM crash.
   * Use {@link #findMatches(Node, SegmentAllocator)} to be able to access the nodes after the stream was closed.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var matches = typedQuery.findMatches(start)) {
   *   MyCollector collector = ...;
   *   matches.forEach(match -> {
   *     match.collectCaptures(collector);
   *   });
   *   ...
   * }
   * }
   */
  public Stream<TypedQueryMatch> findMatches(Node startNode) {
    Objects.requireNonNull(startNode);
    var nodeLanguage = startNode.getTree().getLanguage();
    if (!nodeLanguage.equals(language)) throw new IllegalArgumentException("Node belongs to unexpected language; expected: " + language + ", actual: " + nodeLanguage);
    var queryCursor = new QueryCursor(query);
    var options = new QueryCursor.Options(createPredicateCallback());
    return queryCursor.findMatches(startNode, options)
        .map(TypedQueryMatch::new)
        .onClose(queryCursor::close);
  }

  /**
   * Executes the query and returns a stream of matches, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var matches = typedQuery.findMatches(start, allocator)) {
   *   MyCollector collector = ...;
   *   matches.forEach(match -> {
   *     match.collectCaptures(collector);
   *   });
   *   ...
   * }
   * }
   * @param allocator allocator to use for the captured node objects; allows interacting with the nodes after the stream has been closed
   */
  public Stream<TypedQueryMatch> findMatches(Node startNode, SegmentAllocator allocator) {
    Objects.requireNonNull(startNode);
    Objects.requireNonNull(allocator);
    var nodeLanguage = startNode.getTree().getLanguage();
    if (!nodeLanguage.equals(language)) throw new IllegalArgumentException("Node belongs to unexpected language; expected: " + language + ", actual: " + nodeLanguage);
    var queryCursor = new QueryCursor(query);
    var options = new QueryCursor.Options(createPredicateCallback());
    return queryCursor.findMatches(startNode, allocator, options)
        .map(TypedQueryMatch::new)
        .onClose(queryCursor::close);
  }

  /**
   * Executes the query and for each match collects the captured nodes.
   *
   * <p>This is a convenience method which first executes {@link #findMatches(Node, SegmentAllocator)} and then for each match
   * calls {@link TypedQueryMatch#collectCaptures}.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var arena = Arena.ofConfined()) {
   *   var collector = ...;
   *   typedQuery.findMatchesAndCollect(startNode, arena, collector);
   *   ...
   * }
   * }
   * @param allocator allocator to use for the captured node objects
   */
  public void findMatchesAndCollect(Node startNode, SegmentAllocator allocator, C collector) {
    Objects.requireNonNull(collector);
    if (captureRegistry.hasNoHandlers()) throw new IllegalStateException("No capture handlers have been registered using `QCapturable#captured`");
    try (var matches = findMatches(startNode, allocator)) {
      matches.forEach(match -> match.collectCaptures(collector));
    }
  }

  /**
   * Called during query execution to handle a query match capture.
   * Capture handlers can be registered using {@link QCapturable#captured}. See the {@link TypedQuery} documentation for more information.
   */
  @FunctionalInterface
  public interface CaptureHandler<C, N extends TypedNode> {
    /**
     * Called during query execution to handle a query match capture.
     *
     * <p><b>Important:</b> Implementations should pass the captured node only to the 'collector',
     * this way collecting is scoped to a single execution of a query where the collector is provided,
     * see {@link TypedQueryMatch#collectCaptures}.
     * Passing the captured node somewhere else other than the collector can make the query usage
     * more error-prone, especially when the query is executed multiple times.
     */
    void handleCapture(C collector, N node);
  }

  private static class CaptureRegistry<C> {
    static final String CAPTURE_PREFIX = "c";

    private final List<CaptureHandler<C, ?>> handlers = new ArrayList<>();

    public String registerHandler(CaptureHandler<C, ?> captureHandler) {
      Objects.requireNonNull(captureHandler);
      var captureName = CAPTURE_PREFIX + handlers.size();
      handlers.add(captureHandler);
      return captureName;
    }

    public boolean hasNoHandlers() {
      return handlers.isEmpty();
    }

    public void invokeHandler(C collector, String captureName, TypedNode node) {
      if (!captureName.startsWith(CAPTURE_PREFIX)) throw new IllegalArgumentException("Unexpected capture name: " + captureName);
      int handlerIndex = Integer.parseInt(captureName.substring(CAPTURE_PREFIX.length()));
      @SuppressWarnings("unchecked") var handler = (CaptureHandler<C, TypedNode>) handlers.get(handlerIndex);
      handler.handleCapture(collector, node);
    }
  }

  private static class PredicateRegistry {
    /**
     * Prefix used for custom predicates as well as the corresponding captures
     */
    private static final String CUSTOM_PREDICATE_PREFIX = "pc";

    private static final String BUILTIN_PREDICATE_CAPTURE_PREFIX = "pb";

    private final List<Predicate<Stream<TypedNode>>> customPredicates = new ArrayList<>();

    private int builtInQueryCaptureIndex = 0;

    public String registerCustomPredicate(Predicate<Stream<TypedNode>> predicate) {
      Objects.requireNonNull(predicate);
      var predicateName = CUSTOM_PREDICATE_PREFIX + customPredicates.size();
      customPredicates.add(predicate);
      return predicateName;
    }

    public String requestBuiltInQueryCapture() {
      return BUILTIN_PREDICATE_CAPTURE_PREFIX + builtInQueryCaptureIndex++;
    }

    public boolean test(QueryPredicate queryPredicate, QueryMatch queryMatch) {
      if (!queryPredicate.getArgs().isEmpty()) throw new IllegalArgumentException("Unexpected predicate args: " + queryPredicate);
      var predicateName = queryPredicate.getName();
      if (!predicateName.startsWith(CUSTOM_PREDICATE_PREFIX) || !predicateName.endsWith("?")) throw new IllegalArgumentException("Unexpected predicate name: " + predicateName);
      // Remove trailing '?'
      predicateName = predicateName.substring(0, predicateName.length() - 1);
      int predicateIndex = Integer.parseInt(predicateName.substring(CUSTOM_PREDICATE_PREFIX.length()));
      var predicate = customPredicates.get(predicateIndex);
      // Predicate name is also used as capture name
      var captureName = predicateName;
      var captures = queryMatch.findNodes(captureName).stream().map(TypedNode::fromNodeThrowing);
      return predicate.test(captures);
    }
  }

  /**
   * Type-safe variant of a query match. A match can have zero or more captured nodes which can be
   * obtained using {@link #collectCaptures}.
   */
  public class TypedQueryMatch {
    private final QueryMatch queryMatch;

    TypedQueryMatch(QueryMatch queryMatch) {
      this.queryMatch = queryMatch;
    }

    /**
     * Collects all query match captures as typed nodes by calling the 'capture handlers' with the given 'collector' and the captured nodes.
     * The capture handlers had been registered using {@link QCapturable#captured} during building of the query.
     */
    public void collectCaptures(C collector) {
      Objects.requireNonNull(collector);
      if (captureRegistry.hasNoHandlers()) throw new IllegalStateException("No capture handlers have been registered using `QCapturable#captured`");
      for (var capture : queryMatch.captures()) {
        var captureName = capture.name();
        // Ignore predicate captures
        if (captureName.startsWith(CaptureRegistry.CAPTURE_PREFIX)) {
          var typedNode = TypedNode.fromNodeThrowing(capture.node());
          captureRegistry.invokeHandler(collector, captureName, typedNode);
        }
      }
    }

    /**
     * Returns the underlying jtreesitter {@code QueryMatch}.
     *
     * <p><b>Note:</b> Some information of the query match such as the captures and their names should
     * be considered an implementation detail of the typed query builder and may change in the future.
     */
    public QueryMatch getQueryMatch() {
      return queryMatch;
    }
  }

  /**
   * Base type for all 'typed query' builder classes.
   * Instances of the builder classes can be obtained from {@link Builder}.
   * @param <N> type of the matched node
   * @param <C> type of the user-defined 'collector' which processes query captures; see the {@link TypedQuery} documentation
   */
  public sealed interface QNode<C, N> {
    /**
     * Builds the typed query object, which can then be used to execute the query.
     */
    TypedQuery<C> buildQuery();
  }

  private abstract static non-sealed class QNodeImpl<C, N> implements QNode<C, N> {
    void verifyValidState() {
      // Overridden by subclasses to perform additional validation
    }

    abstract void buildQueryImpl(StringBuilder queryStringBuilder,
        CaptureRegistry<C> captureRegistry, PredicateRegistry predicateRegistry);

    @Override
    public TypedQuery<C> buildQuery() {
      verifyValidState();
      var language = LanguageUtils.language;
      var queryStringBuilder = new StringBuilder();
      var captureRegistry = new CaptureRegistry<C>();
      var predicateRegistry = new PredicateRegistry();
      buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
      return new TypedQuery<>(language, queryStringBuilder.toString(), captureRegistry, predicateRegistry);
    }

    static <C, N> QNodeImpl<C, N> from(QNode<C, N> node) {
      Objects.requireNonNull(node);
      // Expect that every QNode is actually an instance of QNodeImpl
      var n = (QNodeImpl<C, N>) node;
      n.verifyValidState();
      return n;
    }

    @SuppressWarnings("unchecked")
    static <C, N> List<QNodeImpl<C, ? extends N>> listOf(QNode<C, ? extends N>... nodes) {
      // Expect that every QNode is actually an instance of QNodeImpl
      var n = (List<QNodeImpl<C, ? extends N>>) (List<?>) List.of(nodes);
      n.forEach(QNodeImpl::verifyValidState);
      return n;
    }

    static String createStringLiteral(String s) {
      return "\"" + s
          .replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\n", "\\n")
          .replace("\r", "\\r")
          .replace("\t", "\\t")
          .replace("\0", "\\0")
           + "\"";
    }
  }

  /**
   * Allows specifying how often a node should be matched by the query.
   * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#quantification-operators">Tree-sitter documentation 'quantification operators'</a>
   */
  public abstract static class QQuantifiable<C, N> extends QNodeImpl<C, N> {
    QQuantifiable() {
    }

    /**
     * Specifies that the node can occur zero or more times.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#quantification-operators">Tree-sitter documentation 'quantification operators'</a>
     */
    public QNode<C, N> zeroOrMore() {
      return new QQuantified<>(this, '*');
    }

    /**
     * Specifies that the node must occur one or more times.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#quantification-operators">Tree-sitter documentation 'quantification operators'</a>
     */
    public QNode<C, N> oneOrMore() {
      return new QQuantified<>(this, '+');
    }

    /**
     * Specifies that the node is optional (can occur zero or one time).
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#quantification-operators">Tree-sitter documentation 'quantification operators'</a>
     */
    public QNode<C, N> optional() {
      return new QQuantified<>(this, '?');
    }
  }

  private static class QQuantified<C, N> extends QNodeImpl<C, N> {
    private final QNodeImpl<C, N> node;

    private final char quantifier;

    QQuantified(QNodeImpl<C, N> node, char quantifier) {
      this.node = node;
      this.quantifier = quantifier;
      node.verifyValidState();
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      node.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
      queryStringBuilder.append(quantifier);
    }
  }

  private static class QGroup<C, N> extends QQuantifiable<C, N> {
    private final List<QNodeImpl<C, ? extends N>> nodes;

    QGroup(List<QNodeImpl<C, ? extends N>> nodes) {
      this.nodes = nodes;
      if (nodes.isEmpty()) throw new IllegalArgumentException("Must specify at least one node");
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      queryStringBuilder.append('(');
      nodes.forEach(n -> {
            n.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
            queryStringBuilder.append(' ');
          });
      queryStringBuilder.append(')');
    }
  }

  private static class QAlternation<C, N> extends QQuantifiable<C, N> {
    private final List<QNodeImpl<C, ? extends N>> nodes;

    QAlternation(List<QNodeImpl<C, ? extends N>> nodes) {
      this.nodes = nodes;
      if (nodes.isEmpty()) throw new IllegalArgumentException("Must specify at least one node");
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      queryStringBuilder.append('[');
      nodes.forEach(n -> {
            n.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
            queryStringBuilder.append(' ');
          });
      queryStringBuilder.append(']');
    }
  }

  private static class QAlternationTyped<C, N extends TypedNode> extends QCapturableQuantifiable<C, N> {
    private final List<QNodeImpl<C, ? extends N>> nodes;

    QAlternationTyped(List<QNodeImpl<C, ? extends N>> nodes) {
      this.nodes = nodes;
      if (nodes.isEmpty()) throw new IllegalArgumentException("Must specify at least one node");
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      queryStringBuilder.append('[');
      nodes.forEach(n -> {
            n.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
            queryStringBuilder.append(' ');
          });
      queryStringBuilder.append(']');
    }
  }

  static class QUnnamedNode<C, N> extends QQuantifiable<C, N> {
    /**
     * nullable
     */
    private final String supertype;

    private final String nodeType;

    QUnnamedNode(String supertype, String nodeType) {
      this.supertype = supertype;
      this.nodeType = nodeType;
      Objects.requireNonNull(nodeType);
      checkUnnamedNodeType(supertype, nodeType);
    }

    static void checkUnnamedNodeType(String supertype, String nodeType) {
      var language = LanguageUtils.language;
      var typeId = language.getSymbolForName(nodeType, false);
      if (typeId == 0) throw new IllegalArgumentException("Unknown unnamed node type: " + nodeType);
      if (supertype != null) {
        var supertypeId = language.getSymbolForName(supertype, true);
        if (supertypeId == 0) throw new IllegalArgumentException("Unknown supertype node type: " + supertype);
        var foundSubtype = false;
        for (var subtypeId : language.getSubtypes(supertypeId)) {
          if (subtypeId == typeId) {
            foundSubtype = true;
            break;
          }
        }
        if (!foundSubtype) throw new IllegalArgumentException("Node type '" + supertype + "' is not a supertype of '" + nodeType + "'");
      }
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
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
    static final QWildcardNode<?, ?> NAMED = new QWildcardNode<>(true);

    static final QWildcardNode<?, ?> NAMED_OR_UNNAMED = new QWildcardNode<>(false);

    private final boolean isNamed;

    QWildcardNode(boolean isNamed) {
      this.isNamed = isNamed;
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      // Wrap wildcard `_` (named or unnamed) in an alternation as `[_]` to avoid accidentally turning it into a named-only `(_)` when wrapped for example in a group
      queryStringBuilder.append(isNamed ? '(' : '[');
      queryStringBuilder.append('_');
      queryStringBuilder.append(isNamed ? ')' : ']');
    }
  }

  private static class QErrorNode<C, N> extends QQuantifiable<C, N> {
    static final QErrorNode<?, ?> INSTANCE = new QErrorNode<>();

    private QErrorNode() {
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      queryStringBuilder.append("(ERROR)");
    }
  }

  private static class QMissingNode<C, N> extends QQuantifiable<C, N> {
    static final QMissingNode<?, ?> ANY = new QMissingNode<>(null, false);

    /**
     * nullable
     */
    private final String nodeType;

    private final boolean isUnnamed;

    QMissingNode(String nodeType, boolean isUnnamed) {
      this.nodeType = nodeType;
      this.isUnnamed = isUnnamed;
      if (isUnnamed) QUnnamedNode.checkUnnamedNodeType(null, nodeType);
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      queryStringBuilder.append("(MISSING");
      if (nodeType != null) {
        queryStringBuilder.append(' ');
        if (isUnnamed) {
          queryStringBuilder.append(createStringLiteral(nodeType));
        } else {
          queryStringBuilder.append(nodeType);
        }
      }
      queryStringBuilder.append(')');
    }
  }

  /**
   * Allows specifying predicates which the node must fulfill.
   */
  public sealed interface QFilterable<C, N extends TypedNode> extends QNode<C, N> {
    /**
     * Specifies that the {@linkplain Node#getText text of the node} must be equal to the given text.
     */
    default QCapturable<C, N> textEq(String s) {
      var p = new QFiltered.BuiltinPredicate("eq", List.of(s));
      return new QFiltered<>(QNodeImpl.from(this), p);
    }

    /**
     * Specifies that the {@linkplain Node#getText text of the node} must not be equal the given text.
     */
    default QCapturable<C, N> textNotEq(String s) {
      var p = new QFiltered.BuiltinPredicate("not-eq", List.of(s));
      return new QFiltered<>(QNodeImpl.from(this), p);
    }

    /**
     * Specifies that the {@linkplain Node#getText text of the node} must be equal to any of the given texts.
     */
    default QCapturable<C, N> textAnyOf(String... s) {
      var strings = List.of(s);
      if (strings.isEmpty()) throw new IllegalArgumentException("Must specify at least one string");
      var p = new QFiltered.BuiltinPredicate("any-of", strings);
      return new QFiltered<>(QNodeImpl.from(this), p);
    }

    /**
     * Specifies that the nodes must fulfill the given predicate.
     */
    default QCapturable<C, N> matching(Predicate<? super Stream<N>> predicate) {
      Objects.requireNonNull(predicate);
      @SuppressWarnings("unchecked") var predicateU = (Predicate<Stream<TypedNode>>) (Predicate<?>) predicate;
      var p = new QFiltered.CustomPredicate(predicateU);
      return new QFiltered<>(QNodeImpl.from(this), p);
    }
  }

  private static final class QFiltered<C, N extends TypedNode> extends QNodeImpl<C, N> implements QCapturable<C, N> {
    private final QNodeImpl<C, N> node;

    private final QPredicate predicate;

    QFiltered(QNodeImpl<C, N> node, QPredicate predicate) {
      this.node = node;
      this.predicate = predicate;
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      // To avoid any ambiguity wrap the whole node in a group
      queryStringBuilder.append('(');
      node.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
      queryStringBuilder.append(' ');
      predicate.build(queryStringBuilder, predicateRegistry);
      queryStringBuilder.append(')');
    }

    interface QPredicate {
      void build(StringBuilder queryStringBuilder, PredicateRegistry predicateRegistry);
    }

    record BuiltinPredicate(String name, List<String> args) implements QPredicate {
      @Override
      public void build(StringBuilder queryStringBuilder, PredicateRegistry predicateRegistry) {
        var captureName = predicateRegistry.requestBuiltInQueryCapture();
        queryStringBuilder
            .append('@').append(captureName)
            .append(" (")
            .append('#').append(name).append('?')
            .append(' ')
            .append('@').append(captureName);
        args.forEach(a -> queryStringBuilder.append(' ').append(createStringLiteral(a)));
        queryStringBuilder.append(')');
      }
    }

    record CustomPredicate(Predicate<Stream<TypedNode>> predicate) implements QPredicate {
      @Override
      public void build(StringBuilder queryStringBuilder, PredicateRegistry predicateRegistry) {
        var predicateName = predicateRegistry.registerCustomPredicate(predicate);
        var captureName = predicateName;
        queryStringBuilder
            .append('@').append(captureName)
            .append(" (")
            .append('#')
            .append(predicateName)
            // Don't add any predicate args; when evaluating predicate it will get the corresponding capture based on the predicate name
            .append("?)");
      }
    }
  }

  /**
   * Allows capturing a node and accessing it during query execution. See the {@link TypedQuery} documentation for more information.
   */
  public sealed interface QCapturable<C, N extends TypedNode> extends QFilterable<C, N> {
    /**
     * Specifies that matching nodes should be captured during query execution and provided to the given capture handler.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#capturing-nodes">Tree-sitter documentation 'capturing nodes'</a>
     */
    default QNode<C, N> captured(CaptureHandler<C, N> captureHandler) {
      Objects.requireNonNull(captureHandler);
      return new QCaptured<>(QNodeImpl.from(this), captureHandler);
    }
  }

  private static class QCaptured<C, N extends TypedNode> extends QNodeImpl<C, N> {
    private final QNodeImpl<C, N> node;

    private final CaptureHandler<C, ?> captureHandler;

    QCaptured(QNodeImpl<C, N> node, CaptureHandler<C, ?> captureHandler) {
      this.node = node;
      this.captureHandler = captureHandler;
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      node.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
      var captureName = captureRegistry.registerHandler(captureHandler);
      queryStringBuilder.append(" @").append(captureName);
    }
  }

  /**
   * Allows both {@linkplain QCapturable capturing} and {@linkplain QQuantifiable quantifying} matching nodes.
   * To do both, first use one of the quantifying methods such as {@link #zeroOrMore} and afterwards the capturing method {@link #captured}.
   * Calling the methods in the opposite order is not possible.
   */
  public abstract static non-sealed class QCapturableQuantifiable<C, N extends TypedNode> extends QQuantifiable<C, N> implements QCapturable<C, N> {
    QCapturableQuantifiable() {
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
    QCapturableQuantified(QQuantified<C, N> quantifiedNode) {
      super(quantifiedNode.node, quantifiedNode.quantifier);
    }
  }

  /**
   * Base type for all query builder classes representing node types as defined in the Tree-sitter {@code node-types.json} file.
   *
   * <p>The following query functionality is provided. If multiple of these are used, they must be applied in this order.
   * When used in a different order the query builder API might not support calling all of these methods.
   * <ol>
   * <li>children and field requirements
   * <br>(provided by subclasses of {@code QTypedNode}; depends on the node type represented by the query builder class)
   * <li>supertype requirements, to match contexts where the node is used as subtype of one of its supertypes
   * <br>(provided by subclasses of {@code QTypedNode}; depends on the node type represented by the query builder class)
   * <li>{@linkplain QQuantifiable quantifying operator}
   * <li>{@linkplain QFilterable filtering requirements}
   * <li>{@linkplain QCapturable capturing matches}
   * <li>as 'extra' node (method {@code asExtra})
   * <br>(provided by subclasses of {@code QTypedNode}; depends on the node type represented by the query builder class)
   * </ol>
   * Here is an example applying all of these:
   * {@snippet lang=java :
   * QNodeMyNode.asExtra(
   *   q.nodeMyNode()
   *     .withChildren(...)
   *     .asSubtypeOfMySupertype()
   *     .zeroOrMore()
   *     .textEq(...)
   *     .captured(...)
   * )
   * }
   */
  public abstract static class QTypedNode<C, N extends TypedNode> extends QCapturableQuantifiable<C, N> {
    /**
     * nullable
     */
    private final String supertype;

    private final String nodeType;

    final Data<C> data;

    private QTypedNode(String supertype, String nodeType, Data<C> data) {
      this.supertype = supertype;
      this.nodeType = nodeType;
      this.data = data;
    }

    QTypedNode(String nodeType) {
      this(null, nodeType, new Data<>());
    }

    QTypedNode(QTypedNode<C, N> old, String supertype) {
      this(supertype, old.nodeType, old.data);
    }

    QTypedNode(QTypedNode<C, N> old, Data<C> data) {
      this(old.supertype, old.nodeType, data);
    }

    @Override
    void verifyValidState() {
      data.verifyValidState();
    }

    @Override
    void buildQueryImpl(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
        PredicateRegistry predicateRegistry) {
      queryStringBuilder.append('(');
      if (supertype != null) {
        queryStringBuilder.append(supertype).append('/');
      }
      queryStringBuilder.append(nodeType);
      data.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
      queryStringBuilder.append(')');
    }

    private interface ChildEntry<C> {
      void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
          PredicateRegistry predicateRegistry);
    }

    private record Child<C>(QNodeImpl<C, ?> node) implements ChildEntry<C> {
      Child {
        Objects.requireNonNull(node);
      }

      @Override
      public void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
          PredicateRegistry predicateRegistry) {
        node.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
      }
    }

    private record Field<C>(String name, QNodeImpl<C, ?> node) implements ChildEntry<C> {
      Field {
        Objects.requireNonNull(name);
        Objects.requireNonNull(node);
      }

      @Override
      public void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
          PredicateRegistry predicateRegistry) {
        queryStringBuilder.append(name).append(": ");
        node.buildQueryImpl(queryStringBuilder, captureRegistry, predicateRegistry);
      }
    }

    private static class Anchor<C> implements ChildEntry<C> {
      private static final Anchor<?> INSTANCE = new Anchor<>();

      private Anchor() {
      }

      @SuppressWarnings("unchecked")
      static <C> Anchor<C> instance() {
        return (Anchor<C>) INSTANCE;
      }

      @Override
      public void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
          PredicateRegistry predicateRegistry) {
        queryStringBuilder.append('.');
      }
    }

    record Data<C>(List<ChildEntry<C>> children, Set<String> withFieldNames,
        SequencedSet<String> withoutFields) {
      Data() {
        this(List.of(), new LinkedHashSet<>(), new LinkedHashSet<>());
      }

      @SafeVarargs
      @SuppressWarnings("varargs")
      public final Data<C> withChildren(QNode<C, ?>... additionalChildren) {
        var children = new ArrayList<>(this.children);
        Arrays.stream(additionalChildren).map(QNodeImpl::from).map(Child::new).forEach(children::add);
        return new Data<>(children, withFieldNames, withoutFields);
      }

      public Data<C> withField(String fieldName, QNode<C, ?> fieldNode, boolean allowMultiple) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(fieldNode);
        if (withoutFields.contains(fieldName)) throw new IllegalStateException("Field '" + fieldName + "' has already been added to \"without fields\"");
        var withFieldNames = new HashSet<>(this.withFieldNames);
        if (!withFieldNames.add(fieldName) && !allowMultiple) throw new IllegalStateException("Field '" + fieldName + "' has already been added");
        var children = new ArrayList<>(this.children);
        children.add(new Field<>(fieldName, QNodeImpl.from(fieldNode)));
        return new Data<>(children, withFieldNames, withoutFields);
      }

      public Data<C> withoutField(String fieldName) {
        Objects.requireNonNull(fieldName);
        if (withFieldNames.contains(fieldName)) throw new IllegalStateException("Field '" + fieldName + "' has already been added to \"with fields\"");
        var withoutFields = new LinkedHashSet<>(this.withoutFields);
        withoutFields.add(fieldName);
        return new Data<>(children, withFieldNames, withoutFields);
      }

      public Data<C> withAnchor() {
        if (!children.isEmpty() && children.getLast() instanceof Anchor) throw new IllegalStateException("Duplicate anchor is not valid");
        var children = new ArrayList<>(this.children);
        children.add(Anchor.instance());
        return new Data<>(children, withFieldNames, withoutFields);
      }

      void verifyValidState() {
        if (children.size() == 1 && children.getFirst() instanceof Anchor) throw new IllegalStateException("Must specify children or fields when using `withChildAnchor`");
      }

      void buildQuery(StringBuilder queryStringBuilder, CaptureRegistry<C> captureRegistry,
          PredicateRegistry predicateRegistry) {
        withoutFields.forEach(f -> queryStringBuilder.append(" !").append(f));
        children.forEach(c -> {
              queryStringBuilder.append(' ');
              c.buildQuery(queryStringBuilder, captureRegistry, predicateRegistry);
            });
      }
    }
  }

  /**
   * Provides convenience methods for obtaining typed query builder objects.
   *
   * <p>The methods are non-static to help with type inference for the {@code <C>} type variable.
   * This builder as well as all returned builder objects are immutable. That means when calling any methods
   * on the query builder objects, the result must not be discarded, otherwise the call has no effect.
   *
   * <p>The expected usage looks like this:
   * {@snippet lang=java :
   * var q = new TypedQuery.Builder<MyCollector>();
   * var typedQuery = q.alternation(
   *     q.errorNode(),
   *     q.nodeMyCustomNode().captured((myCollector, node) -> ...)
   *   ).buildQuery()
   * }
   *
   * <h2>General builder methods</h2>
   * <ul>
   * <li>{@link #unnamedNode}
   * <li>{@link #anyNamedNode}, {@link #anyNode}
   * <li>{@link #errorNode}
   * <li>{@link #missingNode}
   * <li>{@link #group}
   * <li>{@link #alternation}
   * </ul>
   *
   * <h2>Node type builder methods</h2>Additionally for each node type defined in the Tree-sitter grammar a dedicated query builder method exists:
   * <ul>
   * <li>{@link #nodeFirst first}
   * </ul>
   * @param <C> type of the user-defined 'collector' which processes query captures; see the {@link TypedQuery} documentation
   */
  public static class Builder<C> {
    public Builder() {
    }

    /**
     * Provides a query builder for an unnamed node.
     *
     * <p>The node type variable {@code <N>} is unbound to allow using this query node at any position in the query.
     * @see #unnamedNode(String, String)
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#anonymous-nodes">Tree-sitter documentation 'anonymous nodes'</a>
     */
    public <N> QQuantifiable<C, N> unnamedNode(String nodeType) {
      return new QUnnamedNode<>(null, nodeType);
    }

    /**
     * Provides a query builder for an unnamed node with supertype.
     *
     * <p>This can be useful when an unnamed node can appear in multiple contexts and only one of them should be matched.
     * For example in Java code a single {@code ';'} can represent an "empty statement".
     * To match only such usage and ignore all other occurrences {@code unnamedNode("statement", ";")} could be used.
     *
     * <p>The node type variable {@code <N>} is unbound to allow using this query node at any position in the query.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#anonymous-nodes">Tree-sitter documentation 'anonymous nodes'</a>
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#supertype-nodes">Tree-sitter documentation 'supertype nodes'</a>
     */
    public <N> QQuantifiable<C, N> unnamedNode(String supertype, String nodeType) {
      Objects.requireNonNull(supertype);
      return new QUnnamedNode<>(supertype, nodeType);
    }

    /**
     * Provides a query builder for a wildcard node which matches any named node.
     *
     * <p>The node type variable {@code <N>} is unbound to allow using this query node at any position in the query.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#the-wildcard-node">Tree-sitter documentation 'the wildcard node'</a>
     */
    @SuppressWarnings("unchecked")
    public <N> QQuantifiable<C, N> anyNamedNode() {
      return (QWildcardNode<C, N>) QWildcardNode.NAMED;
    }

    /**
     * Provides a query a builder for a wildcard node which matches any node (named or unnamed).
     *
     * <p>The node type variable {@code <N>} is unbound to allow using this query node at any position in the query.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#the-wildcard-node">Tree-sitter documentation 'the wildcard node'</a>
     */
    @SuppressWarnings("unchecked")
    public <N> QQuantifiable<C, N> anyNode() {
      return (QWildcardNode<C, N>) QWildcardNode.NAMED_OR_UNNAMED;
    }

    /**
     * Provides a query builder which matches any ERROR node.
     *
     * <p>The node type variable {@code <N>} is unbound to allow using this query node at any position in the query.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#the-error-node">Tree-sitter documentation 'the ERROR node'</a>
     */
    @SuppressWarnings("unchecked")
    public <N> QQuantifiable<C, N> errorNode() {
      return (QErrorNode<C, N>) QErrorNode.INSTANCE;
    }

    /**
     * Provides a query builder which matches any MISSING node.
     *
     * <p>The node type variable {@code <N>} is unbound to allow using this query node at any position in the query.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html#the-missing-node">Tree-sitter documentation 'the MISSING node'</a>
     */
    @SuppressWarnings("unchecked")
    public <N> QQuantifiable<C, N> missingNode() {
      return (QMissingNode<C, N>) QMissingNode.ANY;
    }

    /**
     * Provides a query builder which matches a group of multiple sibling nodes.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#grouping-sibling-nodes">Tree-sitter documentation 'grouping sibling nodes'</a>
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <N> QQuantifiable<C, N> group(QNode<C, ? extends N>... nodes) {
      return new QGroup<>(QNodeImpl.listOf(nodes));
    }

    /**
     * Provides a query builder which matches any of the given nodes.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#alternations">Tree-sitter documentation 'alternations'</a>
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <N> QQuantifiable<C, N> alternation(QNode<C, ? extends N>... nodes) {
      return new QAlternation<>(QNodeImpl.listOf(nodes));
    }

    /**
     * Provides a query builder which matches any of the given nodes.
     *
     * <p>This overload takes query nodes which are capturable, and returns a query node which is capturable itself as well.
     * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#alternations">Tree-sitter documentation 'alternations'</a>
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <N extends TypedNode> QCapturableQuantifiable<C, N> alternation(
        QCapturable<C, ? extends N>... nodes) {
      return new QAlternationTyped<>(QNodeImpl.listOf(nodes));
    }

    /**
     * Provides a query builder which matches nodes of type {@code first}.
     */
    public QNodeFirst<C> nodeFirst() {
      return new QNodeFirst<>();
    }
  }
}


/* ==================== */ 

package org.example;

import java.lang.SafeVarargs;
import java.lang.SuppressWarnings;
import java.lang.Void;
import javax.annotation.processing.Generated;

/**
 * Query builder for node type {@code first}.
 * Instances can be obtained using {@link TypedQuery.Builder#nodeFirst()}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public class QNodeFirst<C> extends TypedQuery.QTypedNode<C, NodeFirst> {
  QNodeFirst() {
    super(NodeFirst.TYPE_NAME);
  }

  QNodeFirst(QNodeFirst<C> old, TypedQuery.QTypedNode.Data<C> data) {
    super(old, data);
  }

  /**
   * Creates a copy of this query node with the given additional children requirements.
   *
   * <p>The children type has {@code Void} as node type argument because the Tree-sitter grammar has no
   * explicit children types defined for this node. However, by using {@code Void} this method still permits
   * all wildcard-like query node types which have an unbound node type, such as
   * {@link TypedQuery.Builder#errorNode()}.
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public final QNodeFirst<C> withChildren(TypedQuery.QNode<C, Void>... children) {
    return new QNodeFirst<>(this, data.withChildren(children));
  }

  /**
   * Creates a copy of this query node with an additional child anchor.
   * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers/queries/2-operators.html#anchors">Tree-sitter documentation 'anchors'</a>
   */
  public QNodeFirst<C> withChildAnchor() {
    return new QNodeFirst<>(this, data.withAnchor());
  }
}


/* ==================== */ 

