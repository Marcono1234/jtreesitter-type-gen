/**
 * Implementation of the 'typed query' code generation.
 *
 * <p>The typed query works like this:
 * <ol>
 *   <li>User uses typed query builder objects (subtypes of {@code QNode}) to build the query
 *   <li>User creates a {@code TypedQuery} object from a builder object
 *   <li>User executes the {@code TypedQuery} and can collect the captured nodes as typed nodes
 * </ol>
 * {@code TypedQuery} wraps the underlying jtreesitter {@code Query} and contains the information from the
 * builder for forwarding jtreesitter query predicate evaluations and converting captured jtreesitter nodes
 * to the corresponding typed nodes and passing them to the user.
 *
 * <p>To provide a type-safe API for capturing nodes, they cannot be directly returned to the user (that would only
 * support one specific node type, or only obtaining the nodes as general {@code TypedNode}).
 * Instead, the user provides a type-safe 'capture handler' implementation when they build the query. That
 * handler is then called with captured nodes while executing the query. To make capturing dependent on the
 * scope of a single query execution (and not the scope of the whole query) the user specifies a 'collector'
 * during query execution. That collector is passed to the 'capture handler' in addition to the captured node.
 * The different capture handler implementations can then call different methods on the collector depending
 * on the context. After query execution the user can retrieve the captured nodes from the collector.
 *
 * <p>The builder classes ...
 * <ul>
 *   <li>are immutable
 *   <li>cannot be extended by the user
 *   <li>only expose what is relevant for the user; e.g. {@code QWildcardNode} is not publicly visible to the user
 *       because it offers no additional API which the user can use, it is exposed only as {@code QQuantifiable}
 * </ul>
 *
 * <p>The hierarchy of the builder classes is:
 * <ul>
 *   <li>{@code QNode}
 *     <ul>
 *       <li>{@code QNodeImpl} (internal)
 *         <ul>
 *           <li>{@code QWildcardNode}
 *           <li>{@code QErrorNode}
 *           <li>...
 *           <li>{@code QTypedNode} (base type for all typed node query builder classes)
 *             <ul>
 *               <li><code><i>QNodeMyCustomNode</i></code></li>
 *               <li>...</li>
 *             </ul>
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 * Additionally, the node classes extend or implement the following classes and interfaces depending on which
 * additional functionality they provide:
 * <ul>
 *   <li>{@code QCapturable}: Matches can be captured as typed nodes
 *   <li>{@code QFilterable}: Query match predicates can be applied to the node
 *   <li>{@code QQuantifiable}: Query match quantifiers can be applied to the node
 * </ul>
 * In some cases builder classes extend or implement multiple of these classes or interfaces. However, to not make
 * the query builder code too complex but at the same time prevent generating invalid queries, some of these
 * operations can only be performed once for a node or in a certain order. For example, an already quantified node
 * is not quantifiable, and a node which is both capturable and quantifiable has to be first quantified and then
 * captured, not the other way around.
 */

package marcono1234.jtreesitter.type_gen.internal.gen.typed_query;
