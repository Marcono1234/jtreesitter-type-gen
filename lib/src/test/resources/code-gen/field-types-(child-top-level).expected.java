package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.Class;
import java.lang.IllegalArgumentException;
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
            if (currentNode.isNamed() == named && !currentNode.isError() && !currentNode.isMissing() && !currentNode.isExtra()) {
              children.add(currentNode);
            }
          }
        } while (cursor.gotoNextSibling());
      }
    }
    return children;
  }

  public static <T extends TypedNode> List<T> mapChildren(List<Node> children,
      Function<Node, T> mapper) {
    return children.stream().map(mapper).toList();
  }

  public static <T extends TypedNode> List<T> mapChildren(List<Node> children, Class<T> nodeClass) {
    return children.stream().map(n -> fromNodeThrowing(n, nodeClass)).toList();
  }

  /**
   * Maps the children of a node (in the form of jtreesitter nodes) to typed nodes.
   * This differentiates between named and non-named children, since separate typed node classes are used for them.
   * @param namedMapper maps named children; {@code null} if only non-named children are expected
   * @param nonNamedMapper maps non-named children; {@code null} if only named children are expected
   */
  public static <T extends TypedNode> List<T> mapChildren(List<Node> children,
      Function<Node, ? extends T> namedMapper, Function<Node, ? extends T> nonNamedMapper) {
    // First split between named and non-named children
    var namedChildren = new ArrayList<Node>();
    var nonNamedChildren = new ArrayList<Node>();
    for (var child : children) {
      if (child.isNamed()) {
        namedChildren.add(child);
      } else {
        nonNamedChildren.add(child);
      }
    }
    // Map named children (in case they are expected)
    var result = new ArrayList<T>();
    if (namedMapper != null) {
      namedChildren.stream().map(namedMapper).forEach(result::add);
    } else if (!namedChildren.isEmpty()) {
      throw new IllegalArgumentException("Unexpected named children: " + namedChildren);
    }
    // Map non-named children (in case they are expected)
    if (nonNamedMapper != null) {
      nonNamedChildren.stream().map(nonNamedMapper).forEach(result::add);
    } else if (!nonNamedChildren.isEmpty()) {
      throw new IllegalArgumentException("Unexpected non-named children: " + nonNamedChildren);
    }
    return result;
  }

  /**
   * Maps the children of a node (in the form of jtreesitter nodes) to typed nodes.
   * This differentiates between named and non-named children, since separate typed node classes are used for them.
   * @param namedNodeClass maps named children; {@code null} if only non-named children are expected
   * @param nonNamedMapper maps non-named children; {@code null} if only named children are expected
   */
  public static <T extends TypedNode> List<T> mapChildren(List<Node> children,
      Class<? extends T> namedNodeClass, Function<Node, ? extends T> nonNamedMapper) {
    // First split between named and non-named children
    var namedChildren = new ArrayList<Node>();
    var nonNamedChildren = new ArrayList<Node>();
    for (var child : children) {
      if (child.isNamed()) {
        namedChildren.add(child);
      } else {
        nonNamedChildren.add(child);
      }
    }
    // Map named children (in case they are expected)
    var result = new ArrayList<T>();
    if (namedNodeClass != null) {
      namedChildren.stream().map(n -> fromNodeThrowing(n, namedNodeClass)).forEach(result::add);
    } else if (!namedChildren.isEmpty()) {
      throw new IllegalArgumentException("Unexpected named children: " + namedChildren);
    }
    // Map non-named children (in case they are expected)
    if (nonNamedMapper != null) {
      nonNamedChildren.stream().map(nonNamedMapper).forEach(result::add);
    } else if (!nonNamedChildren.isEmpty()) {
      throw new IllegalArgumentException("Unexpected non-named children: " + nonNamedChildren);
    }
    return result;
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
 * <li>{@link NodeContainedA contained_a}
 * <li>{@link NodeContainedB contained_b}
 * <li>{@link NodeRoot root}
 * <li>{@link NodeFieldOfEachOtherA field_of_each_other_a}
 * <li>{@link NodeFieldOfEachOtherB field_of_each_other_b}
 * <li>{@link NodeFieldOfEachOtherWithTokenA field_of_each_other_with_token_a}
 * <li>{@link NodeFieldOfEachOtherWithTokenB field_of_each_other_with_token_b}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface TypedNode permits NodeContainedA, NodeContainedB, NodeRoot, NodeFieldOfEachOtherA, NodeFieldOfEachOtherB, NodeFieldOfEachOtherWithTokenA, NodeFieldOfEachOtherWithTokenB, NodeRoot$MultiTypeNamedType, NodeRoot$SingleTypeNonNamedTokenType, NodeRoot$MultiTypeNonNamedTokenType, NodeRoot$MixedNamedNonNamedType, NodeFieldOfEachOtherA$FType, NodeFieldOfEachOtherB$FType, NodeFieldOfEachOtherWithTokenA$FType, NodeFieldOfEachOtherWithTokenB$FType {
  /**
   * Returns the underlying jtreesitter node.
   */
  Node getNode();

  /**
   * Returns the source code of this node, if available.
   */
  default @Nullable String getText() {
    var result = this.getNode().getText();
    return result;
  }

  /**
   * Returns the range of this node.
   */
  default Range getRange() {
    return this.getNode().getRange();
  }

  /**
   * Returns the start point of this node.
   */
  default Point getStartPoint() {
    return this.getNode().getStartPoint();
  }

  /**
   * Returns the end point of this node.
   */
  default Point getEndPoint() {
    return this.getNode().getEndPoint();
  }

  /**
   * Returns whether this node or any of its child nodes represents an ERROR.
   */
  default boolean hasError() {
    return this.getNode().hasError();
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
      case NodeRoot.TYPE_NAME -> new NodeRoot(node);
      case NodeFieldOfEachOtherA.TYPE_NAME -> new NodeFieldOfEachOtherA(node);
      case NodeFieldOfEachOtherB.TYPE_NAME -> new NodeFieldOfEachOtherB(node);
      case NodeFieldOfEachOtherWithTokenA.TYPE_NAME -> new NodeFieldOfEachOtherWithTokenA(node);
      case NodeFieldOfEachOtherWithTokenB.TYPE_NAME -> new NodeFieldOfEachOtherWithTokenB(node);
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
      throw new IllegalArgumentException("Unknown node type" + ": " + node.getType());
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
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.List;
import java.util.function.Predicate;
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
public final class NodeContainedA implements TypedNode, NodeRoot$MultiTypeNamedType, NodeRoot$MixedNamedNonNamedType, NodeFieldOfEachOtherA$FType, NodeFieldOfEachOtherB$FType {
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
    return this.node;
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
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
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
    return NodeUtils.getNonFieldChildren(this.node, false).stream().map(n -> n.getType()).toList();
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
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeContainedA other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeContainedA" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
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
import java.util.function.Predicate;
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
public final class NodeContainedB implements TypedNode, NodeRoot$MultiTypeNamedType {
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
    return this.node;
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
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
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
    return NodeUtils.getNonFieldChildren(this.node, false).stream().map(n -> n.getType()).toList();
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
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeContainedB other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeContainedB" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type of {@link NodeRoot}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeContainedA contained_a}
 * <li>{@link NodeContainedB contained_b}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeRoot$MultiTypeNamedType extends TypedNode permits NodeContainedA, NodeContainedB {
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;

/**
 * Child node type without name. Child type of {@link NodeRoot}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeRoot$SingleTypeNonNamedTokenType implements TypedNode {
  private final Node node;

  private final TokenType token;

  NodeRoot$SingleTypeNonNamedTokenType(Node node, TokenType token) {
    this.node = node;
    this.token = token;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Returns the token type.
   */
  public TokenType getToken() {
    return this.token;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeRoot$SingleTypeNonNamedTokenType other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "SingleTypeNonNamedTokenType" + "[id=" + Long.toUnsignedString(this.node.getId()) + ",token=" + this.token + "]";
  }

  /**
   * Token type:
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
      return this.type;
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


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;

/**
 * Child node type without name. Child type of {@link NodeRoot}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeRoot$MultiTypeNonNamedTokenType implements TypedNode {
  private final Node node;

  private final TokenType token;

  NodeRoot$MultiTypeNonNamedTokenType(Node node, TokenType token) {
    this.node = node;
    this.token = token;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Returns the token type.
   */
  public TokenType getToken() {
    return this.token;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeRoot$MultiTypeNonNamedTokenType other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "MultiTypeNonNamedTokenType" + "[id=" + Long.toUnsignedString(this.node.getId()) + ",token=" + this.token + "]";
  }

  /**
   * Token type:
   * <ul>
   * <li>{@link #PLUS_SIGN '+'}
   * <li>{@link #HYPHEN_MINUS '-'}
   * <li>{@link #TOKEN_2 '&lt;test&gt;'}
   * <li>{@link #TOKEN_3 '&lbrace;&commat;test&rbrace;'}
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
    HYPHEN_MINUS("-"),

    /**
     * {@code <test>}
     */
    TOKEN_2("<test>"),

    /**
     * <code>&lbrace;&commat;test&rbrace;</code>
     */
    TOKEN_3("{@test}");

    private final String type;

    TokenType(String type) {
      this.type = type;
    }

    /**
     * Returns the grammar type of this token.
     */
    public String getType() {
      return this.type;
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


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;

/**
 * Child node type without name. Child type of {@link NodeRoot}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeRoot$MixedNamedNonNamedTokenType implements NodeRoot$MixedNamedNonNamedType {
  private final Node node;

  private final TokenType token;

  NodeRoot$MixedNamedNonNamedTokenType(Node node, TokenType token) {
    this.node = node;
    this.token = token;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Returns the token type.
   */
  public TokenType getToken() {
    return this.token;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeRoot$MixedNamedNonNamedTokenType other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "MixedNamedNonNamedTokenType" + "[id=" + Long.toUnsignedString(this.node.getId()) + ",token=" + this.token + "]";
  }

  /**
   * Token type:
   * <ul>
   * <li>{@link #HYPHEN_MINUS '-'}
   * </ul>
   */
  public enum TokenType {
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
      return this.type;
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


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type of {@link NodeRoot}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeContainedA contained_a}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeRoot$MixedNamedNonNamedType extends TypedNode permits NodeContainedA, NodeRoot$MixedNamedNonNamedTokenType {
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Fields:
 * <ul>
 * <li>{@link #getFieldSingleTypeNamed single_type_named}
 * <li>{@link #getFieldMultiTypeNamed multi_type_named}
 * <li>{@link #getFieldSingleTypeNonNamed single_type_non_named}
 * <li>{@link #getFieldMultiTypeNonNamed multi_type_non_named}
 * <li>{@link #getFieldMixedNamedNonNamed mixed_named_non_named}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeRoot implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "root";

  /**
   * Field name {@code single_type_named}
   *
   * @see #getFieldSingleTypeNamed
   */
  public static final String FIELD_SINGLE_TYPE_NAMED = "single_type_named";

  /**
   * Field name {@code multi_type_named}
   *
   * @see #getFieldMultiTypeNamed
   */
  public static final String FIELD_MULTI_TYPE_NAMED = "multi_type_named";

  /**
   * Field name {@code single_type_non_named}
   *
   * @see #getFieldSingleTypeNonNamed
   */
  public static final String FIELD_SINGLE_TYPE_NON_NAMED = "single_type_non_named";

  /**
   * Field name {@code multi_type_non_named}
   *
   * @see #getFieldMultiTypeNonNamed
   */
  public static final String FIELD_MULTI_TYPE_NON_NAMED = "multi_type_non_named";

  /**
   * Field name {@code mixed_named_non_named}
   *
   * @see #getFieldMixedNamedNonNamed
   */
  public static final String FIELD_MIXED_NAMED_NON_NAMED = "mixed_named_non_named";

  private final Node node;

  NodeRoot(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static @Nullable NodeRoot fromNode(Node node) {
    NodeRoot result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeRoot(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeRoot fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_SINGLE_TYPE_NAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeContainedA getFieldSingleTypeNamed() {
    var children = this.node.getChildrenByFieldName(FIELD_SINGLE_TYPE_NAMED);
    Function<Node, NodeContainedA> namedMapper = NodeContainedA::fromNodeThrowing;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_MULTI_TYPE_NAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeRoot$MultiTypeNamedType getFieldMultiTypeNamed() {
    var children = this.node.getChildrenByFieldName(FIELD_MULTI_TYPE_NAMED);
    var namedMapper = NodeRoot$MultiTypeNamedType.class;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_SINGLE_TYPE_NON_NAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeRoot$SingleTypeNonNamedTokenType getFieldSingleTypeNonNamed() {
    var children = this.node.getChildrenByFieldName(FIELD_SINGLE_TYPE_NON_NAMED);
    Function<Node, NodeRoot$SingleTypeNonNamedTokenType> mapper = n -> new NodeRoot$SingleTypeNonNamedTokenType(n, NodeRoot$SingleTypeNonNamedTokenType.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, (Class<NodeRoot$SingleTypeNonNamedTokenType>) null, mapper);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_MULTI_TYPE_NON_NAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeRoot$MultiTypeNonNamedTokenType getFieldMultiTypeNonNamed() {
    var children = this.node.getChildrenByFieldName(FIELD_MULTI_TYPE_NON_NAMED);
    Function<Node, NodeRoot$MultiTypeNonNamedTokenType> mapper = n -> new NodeRoot$MultiTypeNonNamedTokenType(n, NodeRoot$MultiTypeNonNamedTokenType.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, (Class<NodeRoot$MultiTypeNonNamedTokenType>) null, mapper);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_MIXED_NAMED_NON_NAMED}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeRoot$MixedNamedNonNamedType getFieldMixedNamedNonNamed() {
    var children = this.node.getChildrenByFieldName(FIELD_MIXED_NAMED_NON_NAMED);
    Function<Node, NodeContainedA> namedMapper = NodeContainedA::fromNodeThrowing;
    Function<Node, NodeRoot$MixedNamedNonNamedTokenType> tokenMapper = n -> new NodeRoot$MixedNamedNonNamedTokenType(n, NodeRoot$MixedNamedNonNamedTokenType.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, tokenMapper);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  private static Stream<NodeRoot> findNodesImpl(TypedNode startNode, SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeRoot.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeRoot::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeRoot.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeRoot> findNodes(TypedNode startNode, SegmentAllocator allocator) {
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
   * try (var nodes = NodeRoot.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeRoot> findNodes(TypedNode startNode) {
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeRoot other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeRoot" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type of {@link NodeFieldOfEachOtherA}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeContainedA contained_a}
 * <li>{@link NodeFieldOfEachOtherB field_of_each_other_b}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeFieldOfEachOtherA$FType extends TypedNode permits NodeContainedA, NodeFieldOfEachOtherB {
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
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Fields:
 * <ul>
 * <li>{@link #getFieldF f}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFieldOfEachOtherA implements TypedNode, NodeFieldOfEachOtherB$FType {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "field_of_each_other_a";

  /**
   * Field name {@code f}
   *
   * @see #getFieldF
   */
  public static final String FIELD_F = "f";

  private final Node node;

  NodeFieldOfEachOtherA(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static @Nullable NodeFieldOfEachOtherA fromNode(Node node) {
    NodeFieldOfEachOtherA result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeFieldOfEachOtherA(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeFieldOfEachOtherA fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_F}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeFieldOfEachOtherA$FType getFieldF() {
    var children = this.node.getChildrenByFieldName(FIELD_F);
    var namedMapper = NodeFieldOfEachOtherA$FType.class;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  private static Stream<NodeFieldOfEachOtherA> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeFieldOfEachOtherA.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeFieldOfEachOtherA::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeFieldOfEachOtherA.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeFieldOfEachOtherA> findNodes(TypedNode startNode,
      SegmentAllocator allocator) {
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
   * try (var nodes = NodeFieldOfEachOtherA.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeFieldOfEachOtherA> findNodes(TypedNode startNode) {
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFieldOfEachOtherA other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeFieldOfEachOtherA" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type of {@link NodeFieldOfEachOtherB}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeContainedA contained_a}
 * <li>{@link NodeFieldOfEachOtherA field_of_each_other_a}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeFieldOfEachOtherB$FType extends TypedNode permits NodeContainedA, NodeFieldOfEachOtherA {
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
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Fields:
 * <ul>
 * <li>{@link #getFieldF f}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFieldOfEachOtherB implements TypedNode, NodeFieldOfEachOtherA$FType {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "field_of_each_other_b";

  /**
   * Field name {@code f}
   *
   * @see #getFieldF
   */
  public static final String FIELD_F = "f";

  private final Node node;

  NodeFieldOfEachOtherB(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static @Nullable NodeFieldOfEachOtherB fromNode(Node node) {
    NodeFieldOfEachOtherB result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeFieldOfEachOtherB(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeFieldOfEachOtherB fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_F}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeFieldOfEachOtherB$FType getFieldF() {
    var children = this.node.getChildrenByFieldName(FIELD_F);
    var namedMapper = NodeFieldOfEachOtherB$FType.class;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  private static Stream<NodeFieldOfEachOtherB> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeFieldOfEachOtherB.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeFieldOfEachOtherB::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeFieldOfEachOtherB.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeFieldOfEachOtherB> findNodes(TypedNode startNode,
      SegmentAllocator allocator) {
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
   * try (var nodes = NodeFieldOfEachOtherB.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeFieldOfEachOtherB> findNodes(TypedNode startNode) {
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFieldOfEachOtherB other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeFieldOfEachOtherB" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;

/**
 * Child node type without name. Child type of {@link NodeFieldOfEachOtherWithTokenA}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFieldOfEachOtherWithTokenA$FTokenType implements NodeFieldOfEachOtherWithTokenA$FType {
  private final Node node;

  private final TokenType token;

  NodeFieldOfEachOtherWithTokenA$FTokenType(Node node, TokenType token) {
    this.node = node;
    this.token = token;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Returns the token type.
   */
  public TokenType getToken() {
    return this.token;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFieldOfEachOtherWithTokenA$FTokenType other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "FTokenType" + "[id=" + Long.toUnsignedString(this.node.getId()) + ",token=" + this.token + "]";
  }

  /**
   * Token type:
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
      return this.type;
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


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type of {@link NodeFieldOfEachOtherWithTokenA}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeFieldOfEachOtherWithTokenB field_of_each_other_with_token_b}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeFieldOfEachOtherWithTokenA$FType extends TypedNode permits NodeFieldOfEachOtherWithTokenB, NodeFieldOfEachOtherWithTokenA$FTokenType {
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Fields:
 * <ul>
 * <li>{@link #getFieldF f}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFieldOfEachOtherWithTokenA implements TypedNode, NodeFieldOfEachOtherWithTokenB$FType {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "field_of_each_other_with_token_a";

  /**
   * Field name {@code f}
   *
   * @see #getFieldF
   */
  public static final String FIELD_F = "f";

  private final Node node;

  NodeFieldOfEachOtherWithTokenA(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static @Nullable NodeFieldOfEachOtherWithTokenA fromNode(Node node) {
    NodeFieldOfEachOtherWithTokenA result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeFieldOfEachOtherWithTokenA(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeFieldOfEachOtherWithTokenA fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_F}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeFieldOfEachOtherWithTokenA$FType getFieldF() {
    var children = this.node.getChildrenByFieldName(FIELD_F);
    Function<Node, NodeFieldOfEachOtherWithTokenB> namedMapper = NodeFieldOfEachOtherWithTokenB::fromNodeThrowing;
    Function<Node, NodeFieldOfEachOtherWithTokenA$FTokenType> tokenMapper = n -> new NodeFieldOfEachOtherWithTokenA$FTokenType(n, NodeFieldOfEachOtherWithTokenA$FTokenType.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, tokenMapper);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  private static Stream<NodeFieldOfEachOtherWithTokenA> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeFieldOfEachOtherWithTokenA.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeFieldOfEachOtherWithTokenA::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeFieldOfEachOtherWithTokenA.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeFieldOfEachOtherWithTokenA> findNodes(TypedNode startNode,
      SegmentAllocator allocator) {
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
   * try (var nodes = NodeFieldOfEachOtherWithTokenA.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeFieldOfEachOtherWithTokenA> findNodes(TypedNode startNode) {
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFieldOfEachOtherWithTokenA other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeFieldOfEachOtherWithTokenA" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;

/**
 * Child node type without name. Child type of {@link NodeFieldOfEachOtherWithTokenB}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFieldOfEachOtherWithTokenB$FTokenType implements NodeFieldOfEachOtherWithTokenB$FType {
  private final Node node;

  private final TokenType token;

  NodeFieldOfEachOtherWithTokenB$FTokenType(Node node, TokenType token) {
    this.node = node;
    this.token = token;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Returns the token type.
   */
  public TokenType getToken() {
    return this.token;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFieldOfEachOtherWithTokenB$FTokenType other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "FTokenType" + "[id=" + Long.toUnsignedString(this.node.getId()) + ",token=" + this.token + "]";
  }

  /**
   * Token type:
   * <ul>
   * <li>{@link #HYPHEN_MINUS '-'}
   * </ul>
   */
  public enum TokenType {
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
      return this.type;
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


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type of {@link NodeFieldOfEachOtherWithTokenB}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeFieldOfEachOtherWithTokenA field_of_each_other_with_token_a}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeFieldOfEachOtherWithTokenB$FType extends TypedNode permits NodeFieldOfEachOtherWithTokenA, NodeFieldOfEachOtherWithTokenB$FTokenType {
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Fields:
 * <ul>
 * <li>{@link #getFieldF f}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeFieldOfEachOtherWithTokenB implements TypedNode, NodeFieldOfEachOtherWithTokenA$FType {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "field_of_each_other_with_token_b";

  /**
   * Field name {@code f}
   *
   * @see #getFieldF
   */
  public static final String FIELD_F = "f";

  private final Node node;

  NodeFieldOfEachOtherWithTokenB(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static @Nullable NodeFieldOfEachOtherWithTokenB fromNode(Node node) {
    NodeFieldOfEachOtherWithTokenB result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeFieldOfEachOtherWithTokenB(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeFieldOfEachOtherWithTokenB fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_F}.
   * <ul>
   * <li>multiple: false
   * <li>required: false
   * </ul>
   */
  public @Nullable NodeFieldOfEachOtherWithTokenB$FType getFieldF() {
    var children = this.node.getChildrenByFieldName(FIELD_F);
    Function<Node, NodeFieldOfEachOtherWithTokenA> namedMapper = NodeFieldOfEachOtherWithTokenA::fromNodeThrowing;
    Function<Node, NodeFieldOfEachOtherWithTokenB$FTokenType> tokenMapper = n -> new NodeFieldOfEachOtherWithTokenB$FTokenType(n, NodeFieldOfEachOtherWithTokenB$FTokenType.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, tokenMapper);
    return NodeUtils.optionalSingleChild(childrenMapped);
  }

  private static Stream<NodeFieldOfEachOtherWithTokenB> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeFieldOfEachOtherWithTokenB.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeFieldOfEachOtherWithTokenB::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeFieldOfEachOtherWithTokenB.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeFieldOfEachOtherWithTokenB> findNodes(TypedNode startNode,
      SegmentAllocator allocator) {
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
   * try (var nodes = NodeFieldOfEachOtherWithTokenB.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeFieldOfEachOtherWithTokenB> findNodes(TypedNode startNode) {
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeFieldOfEachOtherWithTokenB other) {
      return this.node.equals(other.node);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.node.hashCode();
  }

  @Override
  public String toString() {
    return "NodeFieldOfEachOtherWithTokenB" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

