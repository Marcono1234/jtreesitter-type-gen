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
 * <li>{@link NodeMyNodeA my_node_a}
 * <li>{@link NodeMyNodeB my_node_b}
 * <li>{@link NodeSuper super}
 * </ul>
 *
 * <hr>
 *
 * typed node
 * my_node_a: org.example.NodeMyNodeA
 * unknown_node: 'unknown'
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface TypedNode permits NodeMyNodeA, NodeMyNodeB, NodeSuper, NodeMyNodeA$FieldMulti, NodeMyNodeA.FieldTokenTokens, NodeMyNodeA$FieldMixed, NodeMyNodeB$Child {
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
      case NodeMyNodeA.TYPE_NAME -> new NodeMyNodeA(node);
      case NodeMyNodeB.TYPE_NAME -> new NodeMyNodeB(node);
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

import javax.annotation.processing.Generated;

/**
 * Child type returned by {@link NodeMyNodeA#getFieldMulti}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeMyNodeA my_node_a}
 * <li>{@link NodeMyNodeB my_node_b}
 * </ul>
 *
 * <hr>
 *
 * field interface
 * my_node_a
 * multi
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeMyNodeA$FieldMulti extends TypedNode permits NodeMyNodeA, NodeMyNodeB {
}


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type returned by {@link NodeMyNodeA#getFieldMixed}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeMyNodeA my_node_a}
 * <li>{@linkplain NodeMyNodeA.FieldTokenMixed <i>tokens</i>}
 * </ul>
 *
 * <hr>
 *
 * field interface
 * my_node_a
 * mixed
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeMyNodeA$FieldMixed extends TypedNode permits NodeMyNodeA, NodeMyNodeA.FieldTokenMixed {
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
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChild}
 * <p>Fields:
 * <ul>
 * <li>{@link #getFieldSingle single}
 * <li>{@link #getFieldMulti multi}
 * <li>{@link #getFieldTokens tokens}
 * <li>{@link #getFieldMixed mixed}
 * </ul>
 *
 * <hr>
 *
 * own node type: my_node_a
 * my_node_a: org.example.NodeMyNodeA
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeMyNodeA implements TypedNode,
    NodeSuper,
    NodeMyNodeA$FieldMulti,
    NodeMyNodeA$FieldMixed,
    NodeMyNodeB$Child {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "my_node_a";

  /**
   * Field name {@code single}
   *
   * @see #getFieldSingle
   */
  public static final String FIELD_SINGLE = "single";

  /**
   * Field name {@code multi}
   *
   * @see #getFieldMulti
   */
  public static final String FIELD_MULTI = "multi";

  /**
   * Field name {@code tokens}
   *
   * @see #getFieldTokens
   */
  public static final String FIELD_TOKENS = "tokens";

  /**
   * Field name {@code mixed}
   *
   * @see #getFieldMixed
   */
  public static final String FIELD_MIXED = "mixed";

  private final Node node;

  NodeMyNodeA(Node node) {
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
  public static @Nullable NodeMyNodeA fromNode(Node node) {
    NodeMyNodeA result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeMyNodeA(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeMyNodeA fromNodeThrowing(Node node) {
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
   * <li>required: true
   * </ul>
   *
   * <hr>
   *
   * children getter
   * my_node_a
   * my_node_a
   */
  public NodeMyNodeA getChild() {
    var children = NodeUtils.getNonFieldChildren(node, true);
    Function<Node, NodeMyNodeA> namedMapper = NodeMyNodeA::fromNodeThrowing;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.requiredSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_SINGLE}.
   * <ul>
   * <li>multiple: false
   * <li>required: true
   * </ul>
   *
   * <hr>
   *
   * field getter
   * my_node_a
   * single
   */
  public NodeMyNodeA getFieldSingle() {
    var children = node.getChildrenByFieldName(FIELD_SINGLE);
    Function<Node, NodeMyNodeA> namedMapper = NodeMyNodeA::fromNodeThrowing;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.requiredSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_MULTI}.
   * <ul>
   * <li>multiple: false
   * <li>required: true
   * </ul>
   *
   * <hr>
   *
   * field getter
   * my_node_a
   * multi
   */
  public NodeMyNodeA$FieldMulti getFieldMulti() {
    var children = node.getChildrenByFieldName(FIELD_MULTI);
    var namedMapper = NodeMyNodeA$FieldMulti.class;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.requiredSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_TOKENS}.
   * <ul>
   * <li>multiple: false
   * <li>required: true
   * </ul>
   *
   * <hr>
   *
   * field getter
   * my_node_a
   * tokens
   */
  public FieldTokenTokens getFieldTokens() {
    var children = node.getChildrenByFieldName(FIELD_TOKENS);
    Function<Node, FieldTokenTokens> mapper = n -> new FieldTokenTokens(n, FieldTokenTokens.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, (Class<FieldTokenTokens>) null, mapper);
    return NodeUtils.requiredSingleChild(childrenMapped);
  }

  /**
   * Retrieves the nodes of field {@value #FIELD_MIXED}.
   * <ul>
   * <li>multiple: false
   * <li>required: true
   * </ul>
   *
   * <hr>
   *
   * field getter
   * my_node_a
   * mixed
   */
  public NodeMyNodeA$FieldMixed getFieldMixed() {
    var children = node.getChildrenByFieldName(FIELD_MIXED);
    Function<Node, NodeMyNodeA> namedMapper = NodeMyNodeA::fromNodeThrowing;
    Function<Node, FieldTokenMixed> tokenMapper = n -> new FieldTokenMixed(n, FieldTokenMixed.TokenType.fromNode(n));
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, tokenMapper);
    return NodeUtils.requiredSingleChild(childrenMapped);
  }

  private static Stream<NodeMyNodeA> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeMyNodeA.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeMyNodeA::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeMyNodeA.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeMyNodeA> findNodes(TypedNode startNode, SegmentAllocator allocator) {
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
   * try (var nodes = NodeMyNodeA.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeMyNodeA> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeMyNodeA other) {
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
    return "NodeMyNodeA" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }

  /**
   * Child node type without name, returned by {@link NodeMyNodeA#getFieldTokens}.
   * <p>The type of the node can be obtained using {@link #getToken}.
   *
   * <hr>
   *
   * field token class
   * my_node_a
   * tokens
   * + -
   */
  public static final class FieldTokenTokens implements TypedNode {
    private final Node node;

    private final TokenType token;

    FieldTokenTokens(Node node, TokenType token) {
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
      if (obj instanceof FieldTokenTokens other) {
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
      return "FieldTokenTokens" + "[id=" + Long.toUnsignedString(node.getId()) + ",token=" + token + "]";
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
       *
       * <hr>
       *
       * field token
       * my_node_a
       * tokens
       * +
       */
      PLUS_SIGN("+"),

      /**
       * {@code -}
       *
       * <hr>
       *
       * field token
       * my_node_a
       * tokens
       * -
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
   * Child node type without name, returned by {@link NodeMyNodeA#getFieldMixed}.
   * <p>The type of the node can be obtained using {@link #getToken}.
   *
   * <hr>
   *
   * field token class
   * my_node_a
   * mixed
   * + -
   */
  public static final class FieldTokenMixed implements NodeMyNodeA$FieldMixed {
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
     * <li>{@link #HYPHEN_MINUS '-'}
     * </ul>
     */
    public enum TokenType {
      /**
       * {@code +}
       *
       * <hr>
       *
       * field token
       * my_node_a
       * mixed
       * +
       */
      PLUS_SIGN("+"),

      /**
       * {@code -}
       *
       * <hr>
       *
       * field token
       * my_node_a
       * mixed
       * -
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
}


/* ==================== */ 

package org.example;

import javax.annotation.processing.Generated;

/**
 * Child type returned by {@link NodeMyNodeB#getChild}.
 * <p>Possible types:
 * <ul>
 * <li>{@link NodeMyNodeA my_node_a}
 * <li>{@link NodeMyNodeB my_node_b}
 * </ul>
 *
 * <hr>
 *
 * children interface
 * my_node_b
 * my_node_a my_node_b
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeMyNodeB$Child extends TypedNode permits NodeMyNodeA, NodeMyNodeB {
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
import org.jspecify.annotations.Nullable;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChild}
 *
 * <hr>
 *
 * own node type: my_node_b
 * my_node_b: org.example.NodeMyNodeB
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeMyNodeB implements TypedNode,
    NodeSuper,
    NodeMyNodeA$FieldMulti,
    NodeMyNodeB$Child {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "my_node_b";

  private final Node node;

  NodeMyNodeB(Node node) {
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
  public static @Nullable NodeMyNodeB fromNode(Node node) {
    NodeMyNodeB result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeMyNodeB(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeMyNodeB fromNodeThrowing(Node node) {
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
   * <li>required: true
   * </ul>
   *
   * <hr>
   *
   * children getter
   * my_node_b
   * my_node_a my_node_b
   */
  public NodeMyNodeB$Child getChild() {
    var children = NodeUtils.getNonFieldChildren(node, true);
    var namedMapper = NodeMyNodeB$Child.class;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.requiredSingleChild(childrenMapped);
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

  private static Stream<NodeMyNodeB> findNodesImpl(TypedNode startNode,
      SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeMyNodeB.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeMyNodeB::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeMyNodeB.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  public static Stream<NodeMyNodeB> findNodes(TypedNode startNode, SegmentAllocator allocator) {
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
   * try (var nodes = NodeMyNodeB.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeMyNodeB> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeMyNodeB other) {
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
    return "NodeMyNodeB" + "[id=" + Long.toUnsignedString(node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Query;
import io.github.treesitter.jtreesitter.QueryCursor;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.lang.foreign.SegmentAllocator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * Supertype {@code super}, with subtypes:
 * <ul>
 * <li>{@link NodeMyNodeA my_node_a}
 * <li>{@link NodeMyNodeB my_node_b}
 * </ul>
 *
 * <hr>
 *
 * own node type: super
 * super: org.example.NodeSuper
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface NodeSuper extends TypedNode permits NodeMyNodeA, NodeMyNodeB {
  /**
   * Type name of this node, as defined in the grammar.
   */
  String TYPE_NAME = "super";

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  static @Nullable NodeSuper fromNode(Node node) {
    var result = switch (node.getType()) {
      case NodeMyNodeA.TYPE_NAME -> new NodeMyNodeA(node);
      case NodeMyNodeB.TYPE_NAME -> new NodeMyNodeB(node);
      default -> null;
    }
    ;
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  static NodeSuper fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional;
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type: " + node.getType());
    }
    return typedNode;
  }

  private static Stream<NodeSuper> findNodesImpl(TypedNode startNode, SegmentAllocator allocator) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "["
        + "(" + NodeMyNodeA.TYPE_NAME + ")"
        + "(" + NodeMyNodeB.TYPE_NAME + ")"
        + "] @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = allocator == null ? queryCursor.findMatches(startNodeUnwrapped)
        : queryCursor.findMatches(startNodeUnwrapped, allocator, new QueryCursor.Options((Predicate<QueryCursor.State>) null));
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeSuper::fromNodeThrowing).onClose(() -> {
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
   * try (var nodes = NodeSuper.findNodes(start, allocator)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   * @param allocator allocator to use for the found node objects; allows interacting with the nodes after the stream has been closed
   */
  static Stream<NodeSuper> findNodes(TypedNode startNode, SegmentAllocator allocator) {
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
   * try (var nodes = NodeSuper.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  static Stream<NodeSuper> findNodes(TypedNode startNode) {
    Objects.requireNonNull(startNode);
    return findNodesImpl(startNode, null);
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Tree;
import java.lang.AutoCloseable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * A 'typed parse-tree', with expected root node {@link NodeMyNodeA my_node_a}. A jtreesitter {@link Tree} can be converted to a typed tree with {@link #fromTree}.
 *
 * <p>Individual jtreesitter nodes can be converted to a typed node with {@link TypedNode#fromNode}, or the {@code fromNode} method of the specific typed node classes.
 *
 * <hr>
 *
 * typed tree
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class TypedTree implements AutoCloseable {
  private final Tree tree;

  TypedTree(Tree tree) {
    this.tree = tree;
  }

  /**
   * Returns the underlying jtreesitter tree.
   */
  public Tree getTree() {
    return tree;
  }

  /**
   * Wraps a jtreesitter tree as a typed tree, throwing an {@link IllegalArgumentException} if the tree has an unexpected root node.
   */
  public static TypedTree fromTree(Tree tree) {
    var rootType = tree.getRootNode().getType();
    if (rootType.equals("my_node_a")) {
      return new TypedTree(tree);
    }
    throw new IllegalArgumentException("Wrong node type: " + rootType);
  }

  /**
   * Returns the typed root node.
   */
  public NodeMyNodeA getRootNode() {
    var rootNode = tree.getRootNode();
    var result = NodeMyNodeA.fromNodeThrowing(rootNode);
    return result;
  }

  /**
   * Returns the source code of the syntax tree, if available.
   */
  public @Nullable String getText() {
    var result = tree.getText();
    return result;
  }

  /**
   * Returns whether this tree contains any nodes with errors.
   */
  public boolean hasError() {
    return tree.getRootNode().hasError();
  }

  /**
   * Closes the underlying jtreesitter tree, releasing the resources it holds.
   */
  @Override
  public void close() {
    tree.close();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TypedTree other) {
      return tree.equals(other.tree);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return tree.hashCode();
  }

  @Override
  public String toString() {
    return "TypedTree";
  }
}


/* ==================== */ 

