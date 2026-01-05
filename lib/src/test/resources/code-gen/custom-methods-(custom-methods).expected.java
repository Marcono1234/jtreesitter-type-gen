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
import org.example.custom.CustomMethods;
import org.jspecify.annotations.Nullable;

/**
 * Base type for all 'typed nodes'.
 * A jtreesitter {@link Node} can be converted to a typed node with {@link #fromNode} or {@link #fromNodeThrowing},
 * or with the corresponding methods on the specific typed node classes.
 *
 * <p>Custom methods:
 * <ul>
 * <li>{@link #typedNodeCustom(int, String)}
 * <ul>
 *
 * <h2>Node subtypes</h2>
 * <ul>
 * <li>{@link NodeContainedA contained_a}
 * <li>{@link NodeContainedB contained_b}
 * <li>{@link NodeChildrenSingle children_single}
 * <li>{@link NodeChildrenMulti children_multi}
 * <li>{@link NodeFields fields}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface TypedNode permits NodeContainedA, NodeContainedB, NodeChildrenSingle, NodeChildrenMulti, NodeFields, NodeChildrenMulti.Child, NodeFields.FieldMultiNamed, NodeFields.FieldTokenUnnamed, NodeFields.FieldMixed {
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
      case NodeContainedA.TYPE_NAME -> new NodeContainedA(node);
      case NodeContainedB.TYPE_NAME -> new NodeContainedB(node);
      case NodeChildrenSingle.TYPE_NAME -> new NodeChildrenSingle(node);
      case NodeChildrenMulti.TYPE_NAME -> new NodeChildrenMulti(node);
      case NodeFields.TYPE_NAME -> new NodeFields(node);
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

  default void typedNodeCustom(int a, String b) {
    CustomMethods.typedNode(this, a, b);
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
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.example.custom.CustomMethods;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 *
 * <p>Custom methods:
 * <ul>
 * <li>{@link #nodeTypeCustom(int)}
 * <ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeContainedA implements TypedNode,
    NodeChildrenMulti.Child,
    NodeFields.FieldMultiNamed,
    NodeFields.FieldMixed {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "contained_a";

  private final Node node;

  NodeContainedA(Node node) {
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
  public static @Nullable NodeContainedA fromNode(Node node) {
    NodeContainedA result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeContainedA(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeContainedA fromNodeThrowing(Node node) {
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

  private static Stream<NodeContainedA> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeContainedA.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeContainedA::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeContainedA.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeContainedA> findNodes(TypedNode startNode, SegmentAllocator allocator) {
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
   * try (var nodes = NodeContainedA.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeContainedA> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeContainedA other) {
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
    return "NodeContainedA" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }

  public void nodeTypeCustom(int a) {
    CustomMethods.nodeType(this, a, "contained_a");
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.example.custom.CustomMethods;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 *
 * <p>Custom methods:
 * <ul>
 * <li>{@link #nodeTypeCustom(int)}
 * <ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeContainedB implements TypedNode, NodeChildrenMulti.Child, NodeFields.FieldMultiNamed {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "contained_b";

  private final Node node;

  NodeContainedB(Node node) {
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
  public static @Nullable NodeContainedB fromNode(Node node) {
    NodeContainedB result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeContainedB(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeContainedB fromNodeThrowing(Node node) {
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

  private static Stream<NodeContainedB> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeContainedB.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeContainedB::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeContainedB.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeContainedB> findNodes(TypedNode startNode, SegmentAllocator allocator) {
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
   * try (var nodes = NodeContainedB.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeContainedB> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeContainedB other) {
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
    return "NodeContainedB" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }

  public void nodeTypeCustom(int a) {
    CustomMethods.nodeType(this, a, "contained_b");
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.example.custom.CustomMethods;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChild}
 *
 * <p>Custom methods:
 * <ul>
 * <li>{@link #nodeTypeCustom(int)}
 * <ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeChildrenSingle implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "children_single";

  private final Node node;

  NodeChildrenSingle(Node node) {
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
  public static @Nullable NodeChildrenSingle fromNode(Node node) {
    NodeChildrenSingle result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeChildrenSingle(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeChildrenSingle fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type: " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the children nodes.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeContainedA getChild() {
    var children = NodeUtils.getNonFieldChildren(node, true);
    Function<Node, NodeContainedA> namedMapper = NodeContainedA::fromNodeThrowing;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
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

  private static Stream<NodeChildrenSingle> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeChildrenSingle.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeChildrenSingle::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeChildrenSingle.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeChildrenSingle> findNodes(TypedNode startNode,
      SegmentAllocator allocator) {
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
   * try (var nodes = NodeChildrenSingle.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeChildrenSingle> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeChildrenSingle other) {
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
    return "NodeChildrenSingle" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }

  public void nodeTypeCustom(int a) {
    CustomMethods.nodeType(this, a, "children_single");
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.example.custom.CustomMethods;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChild}
 *
 * <p>Custom methods:
 * <ul>
 * <li>{@link #nodeTypeCustom(int)}
 * <ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeChildrenMulti implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "children_multi";

  private final Node node;

  NodeChildrenMulti(Node node) {
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
  public static @Nullable NodeChildrenMulti fromNode(Node node) {
    NodeChildrenMulti result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeChildrenMulti(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeChildrenMulti fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type: " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the children nodes.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable Child getChild() {
    var children = NodeUtils.getNonFieldChildren(node, true);
    var namedMapper = Child.class;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
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

  private static Stream<NodeChildrenMulti> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeChildrenMulti.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeChildrenMulti::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeChildrenMulti.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeChildrenMulti> findNodes(TypedNode startNode,
      SegmentAllocator allocator) {
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
   * try (var nodes = NodeChildrenMulti.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeChildrenMulti> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeChildrenMulti other) {
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
    return "NodeChildrenMulti" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }

  public void nodeTypeCustom(int a) {
    CustomMethods.nodeType(this, a, "children_multi");
  }

  /**
   * Child type returned by {@link NodeChildrenMulti#getChild}.
   * <p>Possible types:
   * <ul>
   * <li>{@link NodeContainedA contained_a}
   * <li>{@link NodeContainedB contained_b}
   * </ul>
   *
   * <p>Custom methods:
   * <ul>
   * <li>{@link #childrenTypeCustom(int)}
   * <ul>
   */
  public sealed interface Child extends TypedNode permits NodeContainedA, NodeContainedB {
    default void childrenTypeCustom(int a) {
      CustomMethods.childrenType(this, a, "children_multi", "contained_a");
    }
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import java.lang.Class;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.example.custom.CustomMethods;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Fields:
 * <ul>
 * <li>{@link #getFieldSingleNamed single_named}
 * <li>{@link #getFieldMultiNamed multi_named}
 * <li>{@link #getFieldUnnamed unnamed}
 * <li>{@link #getFieldMixed mixed}
 * </ul>
 *
 * <p>Custom methods:
 * <ul>
 * <li>{@link #nodeTypeCustom(int)}
 * <ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFields implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "fields";

  /**
   * Field name {@code single_named}
   *
   * @see #getFieldSingleNamed
   */
  public static final String FIELD_SINGLE_NAMED = "single_named";

  /**
   * Field name {@code multi_named}
   *
   * @see #getFieldMultiNamed
   */
  public static final String FIELD_MULTI_NAMED = "multi_named";

  /**
   * Field name {@code unnamed}
   *
   * @see #getFieldUnnamed
   */
  public static final String FIELD_UNNAMED = "unnamed";

  /**
   * Field name {@code mixed}
   *
   * @see #getFieldMixed
   */
  public static final String FIELD_MIXED = "mixed";

  private final Node node;

  NodeFields(Node node) {
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
  public static @Nullable NodeFields fromNode(Node node) {
    NodeFields result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeFields(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeFields fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type: " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_SINGLE_NAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeContainedA getFieldSingleNamed() {
    var children = node.getChildrenByFieldName(FIELD_SINGLE_NAMED);
    Function<Node, NodeContainedA> namedMapper = NodeContainedA::fromNodeThrowing;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_MULTI_NAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable FieldMultiNamed getFieldMultiNamed() {
    var children = node.getChildrenByFieldName(FIELD_MULTI_NAMED);
    var namedMapper = FieldMultiNamed.class;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_UNNAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable FieldTokenUnnamed getFieldUnnamed() {
    var children = node.getChildrenByFieldName(FIELD_UNNAMED);
    Function<Node, FieldTokenUnnamed> mapper = n -> new FieldTokenUnnamed(n, FieldTokenUnnamed.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, (Class<FieldTokenUnnamed>) null, mapper);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_MIXED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable FieldMixed getFieldMixed() {
    var children = node.getChildrenByFieldName(FIELD_MIXED);
    Function<Node, NodeContainedA> namedMapper = NodeContainedA::fromNodeThrowing;
    Function<Node, FieldTokenMixed> tokenMapper = n -> new FieldTokenMixed(n, FieldTokenMixed.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, tokenMapper);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  private static Stream<NodeFields> findNodesImpl(TypedNode startNode, SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeFields.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeFields::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeFields.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeFields> findNodes(TypedNode startNode, SegmentAllocator allocator) {
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
   * try (var nodes = NodeFields.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeFields> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFields other) {
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
    return "NodeFields" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }

  public void nodeTypeCustom(int a) {
    CustomMethods.nodeType(this, a, "fields");
  }

  /**
   * Child type returned by {@link NodeFields#getFieldMultiNamed}.
   * <p>Possible types:
   * <ul>
   * <li>{@link NodeContainedA contained_a}
   * <li>{@link NodeContainedB contained_b}
   * </ul>
   *
   * <p>Custom methods:
   * <ul>
   * <li>{@link #fieldTypeCustom_multi_named(int)}
   * <ul>
   */
  public sealed interface FieldMultiNamed extends TypedNode permits NodeContainedA, NodeContainedB {
    default void fieldTypeCustom_multi_named(int a) {
      CustomMethods.fieldType(this, a, "fields", "multi_named");
    }
  }

  /**
   * Child node type without name, returned by {@link NodeFields#getFieldUnnamed}.
   * <p>The type of the node can be obtained using {@link #getToken}.
   */
  public static final class FieldTokenUnnamed implements TypedNode {
    private final Node node;

    private final TokenType token;

    FieldTokenUnnamed(Node node, TokenType token) {
      this.node = node;
      this.token = token;
    }

    @Override
    public Node getNode() {
      return node;
    }

    /**
     * Returns the token type.
     */
    public TokenType getToken() {
      return token;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof FieldTokenUnnamed other) {
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
      return "FieldTokenUnnamed" + "[id=" + Long.toUnsignedString(node.getId()) + ",token=" + token + "]";
    }

    /**
     * Token types:
     * <ul>
     * <li>{@link #PLUS_SIGN '+'}
     * <li>{@link #HYPHEN_MINUS '-'}
     * </ul>
     */
    public enum TokenType {
      /**
       * {@code +}
       */
      PLUS_SIGN("+"),

      /**
       * {@code -}
       */
      HYPHEN_MINUS("-");

      private final String type;

      TokenType(String type) {
        this.type = type;
      }

      /**
       * Returns the grammar type of this token.
       */
      public String getType() {
        return type;
      }

      static TokenType fromNode(Node node) {
        var type = node.getType();
        for (var token : values()) {
          if (token.type.equals(type)) {
            return token;
          }
        }
        // Should not happen since all non-named child types are covered
        throw new IllegalArgumentException("Unknown token type: " + type);
      }
    }
  }

  /**
   * Child node type without name, returned by {@link NodeFields#getFieldMixed}.
   * <p>The type of the node can be obtained using {@link #getToken}.
   */
  public static final class FieldTokenMixed implements FieldMixed {
    private final Node node;

    private final TokenType token;

    FieldTokenMixed(Node node, TokenType token) {
      this.node = node;
      this.token = token;
    }

    @Override
    public Node getNode() {
      return node;
    }

    /**
     * Returns the token type.
     */
    public TokenType getToken() {
      return token;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof FieldTokenMixed other) {
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
      return "FieldTokenMixed" + "[id=" + Long.toUnsignedString(node.getId()) + ",token=" + token + "]";
    }

    /**
     * Token types:
     * <ul>
     * <li>{@link #PLUS_SIGN '+'}
     * </ul>
     */
    public enum TokenType {
      /**
       * {@code +}
       */
      PLUS_SIGN("+");

      private final String type;

      TokenType(String type) {
        this.type = type;
      }

      /**
       * Returns the grammar type of this token.
       */
      public String getType() {
        return type;
      }

      static TokenType fromNode(Node node) {
        var type = node.getType();
        for (var token : values()) {
          if (token.type.equals(type)) {
            return token;
          }
        }
        // Should not happen since all non-named child types are covered
        throw new IllegalArgumentException("Unknown token type: " + type);
      }
    }
  }

  /**
   * Child type returned by {@link NodeFields#getFieldMixed}.
   * <p>Possible types:
   * <ul>
   * <li>{@link NodeContainedA contained_a}
   * <li>{@linkplain FieldTokenMixed <i>tokens</i>}
   * </ul>
   *
   * <p>Custom methods:
   * <ul>
   * <li>{@link #fieldTypeCustom_mixed(int)}
   * <ul>
   */
  public sealed interface FieldMixed extends TypedNode permits NodeContainedA, FieldTokenMixed {
    default void fieldTypeCustom_mixed(int a) {
      CustomMethods.fieldType(this, a, "fields", "mixed");
    }
  }
}


/* ==================== */ 

