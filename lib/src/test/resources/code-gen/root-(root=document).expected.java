package org.example;

import io.github.treesitter.jtreesitter.Node;
import java.lang.Class;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
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
 * <li>{@link NodeDocument document}
 * </ul>
 */
@Generated(
    value = "marcono1234.jtreesitter.type_gen.CodeGenerator",
    date = "1970-01-01T00:00:00Z",
    comments = "code-generator-version=0.0.0 (0000000000000000000000000000000000000000); custom comment"
)
public sealed interface TypedNode permits NodeDocument {
  /**
   * Returns the underlying jtreesitter node.
   */
  Node getNode();

  /**
   * Returns the source code of this node, if available.
   */
  @Nullable String getText();

  /**
   * Returns the range of this node.
   */
  Range getRange();

  /**
   * Returns the start point of this node.
   */
  Point getStartPoint();

  /**
   * Returns the end point of this node.
   */
  Point getEndPoint();

  /**
   * Returns whether this node or any of its child nodes represents an ERROR.
   */
  boolean hasError();

  /**
   * Wraps a jtreesitter node as typed node, returning {@code null} if no corresponding typed node class exists.
   * Only works for <i>named</i> node types.
   *
   * @see #fromNodeThrowing
   */
  static @Nullable TypedNode fromNode(Node node) {
    var result = switch (node.getType()) {
      case NodeDocument.TYPE_NAME -> new NodeDocument(node);
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
import io.github.treesitter.jtreesitter.Point;
import io.github.treesitter.jtreesitter.Range;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
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
public final class NodeDocument implements TypedNode {
  /**
   * Type name of this node, as defined in the grammar.
   */
  public static final String TYPE_NAME = "document";

  private final Node node;

  NodeDocument(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return this.node;
  }

  @Override
  public @Nullable String getText() {
    var result = this.node.getText();
    return result;
  }

  @Override
  public Range getRange() {
    return this.node.getRange();
  }

  @Override
  public Point getStartPoint() {
    return this.node.getStartPoint();
  }

  @Override
  public Point getEndPoint() {
    return this.node.getEndPoint();
  }

  @Override
  public boolean hasError() {
    return this.node.hasError();
  }

  /**
   * Wraps a jtreesitter node as this node type, returning {@code null} if the node has the wrong type.
   *
   * @see #fromNodeThrowing
   */
  public static @Nullable NodeDocument fromNode(Node node) {
    NodeDocument result = null;
    if (TYPE_NAME.equals(node.getType())) {
      result = new NodeDocument(node);
    }
    return result;
  }

  /**
   * Wraps a jtreesitter node as this node type, throwing an {@link IllegalArgumentException} if the node has the wrong type.
   *
   * @see #fromNode
   */
  public static NodeDocument fromNodeThrowing(Node node) {
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
   * try (var nodes = NodeDocument.findNodes(start)) {
   *   List<String> texts = nodes.map(n -> n.getText()).toList();
   *   ...
   * }
   * }
   */
  public static Stream<NodeDocument> findNodes(TypedNode startNode) {
    var startNodeUnwrapped = startNode.getNode();
    var language = startNodeUnwrapped.getTree().getLanguage();
    // tree-sitter query which matches the nodes of this type, and captures them
    var captureName = "node";
    var queryString = "(" + NodeDocument.TYPE_NAME + ") @" + captureName;
    var query = language.query(queryString);
    var stream = query.findMatches(startNodeUnwrapped);
    return stream.flatMap(m -> m.findNodes(captureName).stream()).map(NodeDocument::fromNodeThrowing).onClose(query::close);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeDocument other) {
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
    return "NodeDocument" + "[id=" + Long.toUnsignedString(this.node.getId()) + "]";
  }
}


/* ==================== */ 

package org.example;

import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Tree;
import java.lang.AutoCloseable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.processing.Generated;
import org.jspecify.annotations.Nullable;

/**
 * A 'typed parse-tree', with expected root node {@link NodeDocument document}. jtreesitter {@link Node} can be converted to a typed tree with {@link #fromTree}.
 *
 * <p>Individual jtreesitter nodes can be converted to a typed node with {@link TypedNode#fromNode}, or the {@code fromNode} method of the specific typed node classes.
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
    return this.tree;
  }

  /**
   * Wraps a jtreesitter tree as a typed tree, throwing an {@link IllegalArgumentException} if the tree has an unexpected root node.
   */
  public static TypedTree fromTree(Tree tree) {
    var rootType = tree.getRootNode().getType();
    if (rootType.equals("document")) {
      return new TypedTree(tree);
    }
    throw new IllegalArgumentException("Wrong node type: " + rootType);
  }

  /**
   * Returns the typed root node.
   */
  public NodeDocument getRootNode() {
    var rootNode = this.tree.getRootNode();
    var result = NodeDocument.fromNodeThrowing(rootNode);
    return result;
  }

  /**
   * Returns the source code of the syntax tree, if available.
   */
  public @Nullable String getText() {
    var result = this.tree.getText();
    return result;
  }

  /**
   * Returns whether this tree contains any nodes with errors.
   */
  public boolean hasError() {
    return this.tree.getRootNode().hasError();
  }

  /**
   * Closes the underlying jtreesitter tree, releasing the resources it holds.
   */
  @Override
  public void close() {
    this.tree.close();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TypedTree other) {
      return this.tree.equals(other.tree);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.tree.hashCode();
  }

  @Override
  public String toString() {
    return "TypedTree";
  }
}


/* ==================== */ 

