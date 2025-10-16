package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.Class;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;

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
   * @param fields names of all fields; the implementation requires this to filter out field children
   * @param named whether to return named or non-named children
   */
  public static List<Node> getNonFieldChildren(Node node, String[] fields, boolean named) {
    // First get all relevant children
    var children = new ArrayList<Node>();
    Stream<Node> childrenStream;
    if (named) {
      childrenStream = node.getNamedChildren().stream();
    } else {
      childrenStream = node.getChildren().stream().filter(n -> !n.isNamed());
    }
    childrenStream.filter(n -> !n.isError() && !n.isMissing() && !n.isExtra()).forEach(children::add);
    // Then remove all field children
    for (var field : fields) {
      if (children.isEmpty()) {
        return children;
      }
      children.removeAll(node.getChildrenByFieldName(field));
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

  public static <T extends TypedNode> Optional<T> optionalSingleChild(List<T> nodes) {
    T result = null;
    int size = nodes.size();
    if (size == 1) {
      result = nodes.getFirst();
    } else if (size > 1) {
      throw new IllegalArgumentException("Unexpected nodes count: " + nodes);
    }
    return Optional.ofNullable(result);
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
import java.util.Optional;
import javax.annotation.processing.Generated;

/**
 * Base type for all 'typed nodes'.
 * A jtreesitter {@link Node} can be converted to a typed node with {@link #fromNode} or {@link #fromNodeThrowing},
 * or with the corresponding methods on the specific typed node classes.
 *
 * <h2>Node subtypes</h2>
 * <ul>
 * <li>{@link NodeContained contained}
 * <li>{@link NodeSingleOptional single_optional}
 * <li>{@link NodeSingleRequired single_required}
 * <li>{@link NodeMultipleOptional multiple_optional}
 * <li>{@link NodeMultipleRequired multiple_required}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface TypedNode permits NodeContained, NodeSingleOptional, NodeSingleRequired, NodeMultipleOptional, NodeMultipleRequired {
  /**
   * Returns the underlying jtreesitter node.
   */
  Node getNode();

  /**
   * Returns the source code of this node, if available.
   */
  default Optional<String> getText() {
    var result = this.getNode().getText();
    return Optional.ofNullable(result);
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
   * Wraps a jtreesitter node as typed node, returning an empty {@code Optional} if no corresponding typed node class exists.
   * Only works for <i>named</i> node types.
   *
   * @see #fromNodeThrowing
   */
  static Optional<TypedNode> fromNode(Node node) {
    var result = switch (node.getType()) {
      case NodeContained.TYPE_NAME -> new NodeContained(node);
      case NodeSingleOptional.TYPE_NAME -> new NodeSingleOptional(node);
      case NodeSingleRequired.TYPE_NAME -> new NodeSingleRequired(node);
      case NodeMultipleOptional.TYPE_NAME -> new NodeMultipleOptional(node);
      case NodeMultipleRequired.TYPE_NAME -> new NodeMultipleRequired(node);
      default -> null;
    }
    ;
    return Optional.ofNullable(result);
  }

  /**
   * Wraps a jtreesitter node as typed node, throwing an {@link IllegalArgumentException} if no corresponding typed node class exists.
   * Only works for <i>named</i> node types.
   *
   * @see #fromNode
   */
  static TypedNode fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional.orElse(null);
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;

/**
 * Type {@value #TYPE_NAME}.
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeContained implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "contained";

  private final Node node;

  NodeContained(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning an empty {@code Optional} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static Optional<NodeContained> fromNode(Node node) {
    NodeContained result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeContained(node);
    }
    return Optional.ofNullable(result);
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeContained fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional.orElse(null);
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
    var fieldNames = new String[] {};
    return NodeUtils.getNonFieldChildren(this.node, fieldNames, false).stream().map(n -> n.getType()).toList();
  }

  /**
   * Gets all nodes of this type, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   * After the stream was closed the resulting nodes should not be used anymore, otherwise the behavior is undefined,
   * including exceptions being thrown or possibly even a JVM crash.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var nodes = NodeContained.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeContained> findNodes(TypedNode startNode) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeContained.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = queryCursor.findMatches(startNodeUnwrapped);
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeContained::fromNodeThrowing).onClose(() -> {
          queryCursor.close();
          query.close();
        });
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeContained other) {
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
    return "NodeContained" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChild}
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeSingleOptional implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "single_optional";

  private final Node node;

  NodeSingleOptional(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning an empty {@code Optional} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static Optional<NodeSingleOptional> fromNode(Node node) {
    NodeSingleOptional result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeSingleOptional(node);
    }
    return Optional.ofNullable(result);
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeSingleOptional fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional.orElse(null);
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
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
  public Optional<NodeContained> getChild() {
    var fieldNames = new String[] {};
    var children = NodeUtils.getNonFieldChildren(node, fieldNames, true);
    Function<Node, NodeContained> namedMapper = NodeContained::fromNodeThrowing;
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
    var fieldNames = new String[] {};
    return NodeUtils.getNonFieldChildren(this.node, fieldNames, false).stream().map(n -> n.getType()).toList();
  }

  /**
   * Gets all nodes of this type, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   * After the stream was closed the resulting nodes should not be used anymore, otherwise the behavior is undefined,
   * including exceptions being thrown or possibly even a JVM crash.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var nodes = NodeSingleOptional.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeSingleOptional> findNodes(TypedNode startNode) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeSingleOptional.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = queryCursor.findMatches(startNodeUnwrapped);
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeSingleOptional::fromNodeThrowing).onClose(() -> {
          queryCursor.close();
          query.close();
        });
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeSingleOptional other) {
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
    return "NodeSingleOptional" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChild}
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeSingleRequired implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "single_required";

  private final Node node;

  NodeSingleRequired(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning an empty {@code Optional} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static Optional<NodeSingleRequired> fromNode(Node node) {
    NodeSingleRequired result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeSingleRequired(node);
    }
    return Optional.ofNullable(result);
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeSingleRequired fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional.orElse(null);
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the children nodes.
   * <ul>
   * <li>multiple: false
   * <li>required: true
   * </ul>
   */
  public NodeContained getChild() {
    var fieldNames = new String[] {};
    var children = NodeUtils.getNonFieldChildren(node, fieldNames, true);
    Function<Node, NodeContained> namedMapper = NodeContained::fromNodeThrowing;
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
    var fieldNames = new String[] {};
    return NodeUtils.getNonFieldChildren(this.node, fieldNames, false).stream().map(n -> n.getType()).toList();
  }

  /**
   * Gets all nodes of this type, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   * After the stream was closed the resulting nodes should not be used anymore, otherwise the behavior is undefined,
   * including exceptions being thrown or possibly even a JVM crash.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var nodes = NodeSingleRequired.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeSingleRequired> findNodes(TypedNode startNode) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeSingleRequired.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = queryCursor.findMatches(startNodeUnwrapped);
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeSingleRequired::fromNodeThrowing).onClose(() -> {
          queryCursor.close();
          query.close();
        });
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeSingleRequired other) {
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
    return "NodeSingleRequired" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChildren}
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeMultipleOptional implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "multiple_optional";

  private final Node node;

  NodeMultipleOptional(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning an empty {@code Optional} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static Optional<NodeMultipleOptional> fromNode(Node node) {
    NodeMultipleOptional result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeMultipleOptional(node);
    }
    return Optional.ofNullable(result);
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeMultipleOptional fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional.orElse(null);
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the children nodes.
   * <ul>
   * <li>multiple: true
   * <li>required: false
   * </ul>
   */
  public List<NodeContained> getChildren() {
    var fieldNames = new String[] {};
    var children = NodeUtils.getNonFieldChildren(node, fieldNames, true);
    Function<Node, NodeContained> namedMapper = NodeContained::fromNodeThrowing;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return childrenMapped;
  }

  /**
   * Returns the type names of the non-named, non-extra children, if any.
   *
   * <p><b>Important:</b> Whether this method has any useful or even any results at all depends on the grammar.
   * This method can be useful when the grammar defines a 'choice' of multiple keywords.
   * In that case this method returns the keywords which appear in the parsed source code.
   */
  public List<String> getUnnamedChildren() {
    var fieldNames = new String[] {};
    return NodeUtils.getNonFieldChildren(this.node, fieldNames, false).stream().map(n -> n.getType()).toList();
  }

  /**
   * Gets all nodes of this type, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   * After the stream was closed the resulting nodes should not be used anymore, otherwise the behavior is undefined,
   * including exceptions being thrown or possibly even a JVM crash.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var nodes = NodeMultipleOptional.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeMultipleOptional> findNodes(TypedNode startNode) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeMultipleOptional.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = queryCursor.findMatches(startNodeUnwrapped);
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeMultipleOptional::fromNodeThrowing).onClose(() -> {
          queryCursor.close();
          query.close();
        });
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeMultipleOptional other) {
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
    return "NodeMultipleOptional" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;

/**
 * Type {@value #TYPE_NAME}.
 * <p>Children: {@link #getChildren}
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public final class NodeMultipleRequired implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "multiple_required";

  private final Node node;

  NodeMultipleRequired(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  /**
   * Wraps a jtreesitter node as this node type, returning an empty {@code Optional} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static Optional<NodeMultipleRequired> fromNode(Node node) {
    NodeMultipleRequired result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeMultipleRequired(node);
    }
    return Optional.ofNullable(result);
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeMultipleRequired fromNodeThrowing(Node node) {
    var typedNodeOptional = fromNode(node);
    var typedNode = typedNodeOptional.orElse(null);
    if (typedNode == null) {
      throw new IllegalArgumentException("Wrong node type" + ": " + node.getType());
    }
    return typedNode;
  }

  /**
   * Retrieves the children nodes.
   * <ul>
   * <li>multiple: true
   * <li>required: true
   * </ul>
   */
  public @NonEmpty List<NodeContained> getChildren() {
    var fieldNames = new String[] {};
    var children = NodeUtils.getNonFieldChildren(node, fieldNames, true);
    Function<Node, NodeContained> namedMapper = NodeContained::fromNodeThrowing;
    var childrenMapped = NodeUtils.mapChildren(children, namedMapper, null);
    return NodeUtils.atLeastOneChild(childrenMapped);
  }

  /**
   * Returns the type names of the non-named, non-extra children, if any.
   *
   * <p><b>Important:</b> Whether this method has any useful or even any results at all depends on the grammar.
   * This method can be useful when the grammar defines a 'choice' of multiple keywords.
   * In that case this method returns the keywords which appear in the parsed source code.
   */
  public List<String> getUnnamedChildren() {
    var fieldNames = new String[] {};
    return NodeUtils.getNonFieldChildren(this.node, fieldNames, false).stream().map(n -> n.getType()).toList();
  }

  /**
   * Gets all nodes of this type, starting at the given node.
   *
   * <p><b>Important:</b> The {@code Stream} must be closed to release resources.
   * It is recommended to use a try-with-resources statement.
   * After the stream was closed the resulting nodes should not be used anymore, otherwise the behavior is undefined,
   * including exceptions being thrown or possibly even a JVM crash.
   *
   * <h4>Example</h4>
   * {@snippet lang=java :
   * try (var nodes = NodeMultipleRequired.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeMultipleRequired> findNodes(TypedNode startNode) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeMultipleRequired.TYPE_NAME + ") @" + captureName;
    var query = new Query(language, queryString);
    var queryCursor = new QueryCursor(query);
    var stream = queryCursor.findMatches(startNodeUnwrapped);
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeMultipleRequired::fromNodeThrowing).onClose(() -> {
          queryCursor.close();
          query.close();
        });
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeMultipleRequired other) {
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
    return "NodeMultipleRequired" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

