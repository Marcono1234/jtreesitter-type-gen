package language.json.typed_query_old;

import com.example.json.*;

/*
 TODO: For code generation:
    - Generate QNode... as top-level classes
    - Generate `TypedQuery.Nodes` / `TypedQuery.NodeTypes` or similar which contains all the `node...()` factory methods
        -> Or call it `TypedQuery.Builder`?
 */
// TODO: For field tokens, generate `fieldToken...(TokenEnum)` in the QNode... class which creates typed
//   instance of QUnnamedNode
//   -> or rather generate overload of `withField...` method? but that would not allow using it for example in alternations
// This is a class with type parameter `C` and instance builder methods because for static methods Java's type inference
// is not good enough apparently, requiring to always specify capture type
public class TypedQueryNodes<C> {
    public static class QNodeValue<C> extends TypedQuery.QTypedNode<C, NodeValue> {
        QNodeValue() {
            super(NodeValue.TYPE_NAME);
        }
    }

    public QNodeValue<C> nodeValue() {
        return new QNodeValue<>();
    }

    public static class QNodeNull<C> extends TypedQuery.QTypedNode<C, NodeNull> {
        QNodeNull(String supertype, Data<C> data) {
            super(supertype, NodeNull.TYPE_NAME, data);
        }

        QNodeNull() {
            super(NodeNull.TYPE_NAME);
        }

        @SafeVarargs
        // Use `Void` as child type to only support query nodes which can be used anywhere (e.g. wildcard)
        public final QNodeNull<C> withUnnamedChildren(TypedQuery.QNode<C, Void>... children) {
            return new QNodeNull<>(supertype, data.withChildren(children));
        }

        public QNodeNull<C> withChildAnchor() {
            return new QNodeNull<>(supertype, data.withAnchor());
        }

        public TypedQuery.QCapturableQuantifiable<C, NodeNull> asSubtypeOfNodeValue() {
            return new QNodeNull<>(NodeValue.TYPE_NAME, data);
        }

        public static <C, N> TypedQuery.QNode<C, N> asExtra(TypedQuery.QNode<C, ? extends NodeNull> node) {
            return (TypedQuery.QNode<C, N>) node;
        }
    }

    public QNodeString<C> nodeString() {
        return new QNodeString<>();
    }

    public static class QNodeString<C> extends TypedQuery.QTypedNode<C, NodeString> {
        QNodeString(String supertype, Data<C> data) {
            super(supertype, NodeString.TYPE_NAME, data);
        }

        QNodeString() {
            super(NodeString.TYPE_NAME);
        }

        public QNodeString<C> withChildAnchor() {
            return new QNodeString<>(supertype, data.withAnchor());
        }

        public TypedQuery.QCapturableQuantifiable<C, NodeString> asSubtypeOfNodeValue() {
            return new QNodeString<>(NodeValue.TYPE_NAME, data);
        }
    }

    public QNodeNull<C> nodeNull() {
        return new QNodeNull<>();
    }

    public static class QNodeArray<C> extends TypedQuery.QTypedNode<C, NodeArray> {
        QNodeArray(String supertype, Data<C> data) {
            super(supertype, NodeArray.TYPE_NAME, data);
        }

        QNodeArray() {
            super(NodeArray.TYPE_NAME);
        }

        @SafeVarargs
        public final QNodeArray<C> withChildren(TypedQuery.QNode<C, ? extends NodeValue>... children) {
            return new QNodeArray<>(supertype, data.withChildren(children));
        }

        public QNodeArray<C> withChildAnchor() {
            return new QNodeArray<>(supertype, data.withAnchor());
        }

        public TypedQuery.QCapturableQuantifiable<C, NodeArray> asSubtypeOfNodeValue() {
            return new QNodeArray<>(NodeValue.TYPE_NAME, data);
        }
    }

    public QNodeArray<C> nodeArray() {
        return new QNodeArray<>();
    }

    public static class QNodeObject<C> extends TypedQuery.QTypedNode<C, NodeObject> {
        QNodeObject(String supertype, Data<C> data) {
            super(supertype, NodeObject.TYPE_NAME, data);
        }

        QNodeObject() {
            super(NodeObject.TYPE_NAME);
        }

        @SafeVarargs
        public final QNodeObject<C> withChildren(TypedQuery.QNode<C, ? extends NodePair>... children) {
            return new QNodeObject<>(supertype, data.withChildren(children));
        }

        public QNodeObject<C> withChildAnchor() {
            return new QNodeObject<>(supertype, data.withAnchor());
        }

        public TypedQuery.QCapturableQuantifiable<C, NodeObject> asSubtypeOfNodeValue() {
            return new QNodeObject<>(NodeValue.TYPE_NAME, data);
        }
    }

    public QNodeObject<C> nodeObject() {
        return new QNodeObject<>();
    }

    public static class QNodePair<C> extends TypedQuery.QTypedNode<C, NodePair> {
        QNodePair(String supertype, Data<C> data) {
            super(supertype, NodePair.TYPE_NAME, data);
        }

        QNodePair() {
            super(NodePair.TYPE_NAME);
        }

        @SafeVarargs
        // Use `Void` as child type to only support query nodes which can be used anywhere (e.g. wildcard)
        public final QNodePair<C> withUnnamedChildren(TypedQuery.QNode<C, Void>... children) {
            return new QNodePair<>(supertype, data.withChildren(children));
        }

        public QNodePair<C> withFieldKey(TypedQuery.QNode<C, ? extends NodeString> field) {
            return new QNodePair<>(supertype, data.withField(NodePair.FIELD_KEY, field));
        }

        public QNodePair<C> withFieldValue(TypedQuery.QNode<C, ? extends NodeValue> field) {
            return new QNodePair<>(supertype, data.withField(NodePair.FIELD_VALUE, field));
        }

        // TODO: Only generate those if the fields are optional
        public QNodePair<C> withoutFieldKey() {
            return new QNodePair<>(supertype, data.withoutField(NodePair.FIELD_KEY));
        }
        public QNodePair<C> withoutFieldValue() {
            return new QNodePair<>(supertype, data.withoutField(NodePair.FIELD_VALUE));
        }

        public QNodePair<C> withChildAnchor() {
            return new QNodePair<>(supertype, data.withAnchor());
        }
    }

    public QNodePair<C> nodePair() {
        return new QNodePair<>();
    }
}
