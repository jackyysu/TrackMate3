package net.trackmate.graph.zzgraphinterfaces;

/**
 * A graph consisting of vertices of type {@code V} and edges of type {@code E}.
 *
 * @param <V>
 *            the {@link Vertex} type of the {@link Graph}.
 * @param <E>
 *            the {@link Edge} type of the {@link Graph}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface Graph< V extends Vertex< E >, E extends Edge< V > > extends ReadOnlyGraph< V, E >
{
	public V addVertex();

	public V addVertex( final V vertex );

	public E addEdge( final V source, final V target );

	public E addEdge( final V source, final V target, final E edge );

	/**
	 * Add a new {@link Edge} between {@code source} and {@code target}. The new
	 * edge is inserted in the source and target edge lists such that
	 * {@link Edge#getSourceOutIndex()}{@code == sourceOutIndex} and
	 * {@link Edge#getTargetInIndex()}{@code == targetInIndex}.
	 * <p>
	 * Optional operation implemented by graphs that maintain edge order.
	 *
	 * @param source
	 * @param sourceOutIndex
	 * @param target
	 * @param targetInIndex
	 * @return the newly created edge.
	 */
	public E insertEdge( final V source, final int sourceOutIndex, final V target, final int targetInIndex );

	/**
	 * Add a new {@link Edge} between {@code source} and {@code target}. The new
	 * edge is inserted in the source and target edge lists such that
	 * {@link Edge#getSourceOutIndex()}{@code == sourceOutIndex} and
	 * {@link Edge#getTargetInIndex()}{@code == targetInIndex}.
	 * <p>
	 * Optional operation implemented by graphs that maintain edge order.
	 *
	 * @param source
	 * @param sourceOutIndex
	 * @param target
	 * @param targetInIndex
	 * @param edge
	 *            an {@link Edge} reference that can be used for returning the
	 *            newly created edge. Depending on concrete implementation, this
	 *            object can be cleared, ignored or re-used.
	 * @return the newly created edge. The object actually returned might be the
	 *         reference {@code edge}, depending on concrete implementation.
	 */
	public E insertEdge( final V source, final int sourceOutIndex, final V target, final int targetInIndex, final E edge );

	public void remove( final V vertex );

	public void remove( final E edge );

	public void removeAllLinkedEdges( final V vertex );
}