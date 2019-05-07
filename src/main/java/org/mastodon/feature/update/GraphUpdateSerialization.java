package org.mastodon.feature.update;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.feature.update.GraphUpdate.UpdateLocality;
import org.mastodon.graph.Edge;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.io.RawGraphIO.FileIdToGraphMap;
import org.mastodon.graph.io.RawGraphIO.GraphToFileIdMap;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.io.ObjectToFileIdMap;

class GraphUpdateSerialization
{

	static < V extends Vertex< E >, E extends Edge< V > > GraphUpdate< V, E > deserialize(
			final ReadOnlyGraph< V, E > graph,
			final FileIdToGraphMap< V, E > idmap,
			final ObjectInputStream ois )
			throws IOException, ClassNotFoundException
	{
		final RefSet< V > modifiedVerticesSelf = RefCollections.createRefSet( graph.vertices() );
		deserialize( modifiedVerticesSelf, idmap.vertices(), ois );
		final RefSet< E > modifiedEdgesSelf = RefCollections.createRefSet( graph.edges() );
		deserialize( modifiedEdgesSelf, idmap.edges(), ois );
		final RefSet< V > modifiedVerticesNeighbor = RefCollections.createRefSet( graph.vertices() );
		deserialize( modifiedVerticesNeighbor, idmap.vertices(), ois );
		final RefSet< E > modifiedEdgesNeighbor = RefCollections.createRefSet( graph.edges() );
		deserialize( modifiedEdgesNeighbor, idmap.edges(), ois );

		return new GraphUpdate<>(
				modifiedVerticesSelf,
				modifiedEdgesSelf,
				modifiedVerticesNeighbor,
				modifiedEdgesNeighbor );
	}

	private static < O > void deserialize(
			final RefSet< O > collection,
			final FileIdToObjectMap< O > idmap,
			final ObjectInputStream ois )
			throws IOException
	{
		// NUMBER OF ENTRIES
		final int size = ois.readInt();

		// ENTRIES
		final O ref = idmap.createRef();
		for ( int i = 0; i < size; i++ )
		{
			final int key = ois.readInt();
			collection.add( idmap.getObject( key, ref ) );
		}
		idmap.releaseRef( ref );
	}

	static < V extends Vertex< E >, E extends Edge< V > > void serialize(
			final GraphUpdate< V, E > graphUpdate,
			final GraphToFileIdMap< V, E > idmap,
			final ObjectOutputStream oos )
			throws IOException
	{
		serialize( graphUpdate.vertices( UpdateLocality.SELF ), idmap.vertices(), oos );
		serialize( graphUpdate.edges( UpdateLocality.SELF ), idmap.edges(), oos );
		serialize( graphUpdate.vertices( UpdateLocality.NEIGHBOR ), idmap.vertices(), oos );
		serialize( graphUpdate.edges( UpdateLocality.NEIGHBOR ), idmap.edges(), oos );
	}

	private static < O > void serialize(
			final RefSet< O > collection,
			final ObjectToFileIdMap< O > idmap,
			final ObjectOutputStream oos )
			throws IOException
	{
		// NUMBER OF ENTRIES
		oos.writeInt( collection.size() );

		// ENTRIES
		try
		{
			collection.forEach( ( final O key ) -> {
				try
				{
					oos.writeInt( idmap.getId( key ) );
				}
				catch ( final IOException e )
				{
					throw new UncheckedIOException( e );
				}
			} );
		}
		catch ( final UncheckedIOException e )
		{
			throw e.getCause();
		}
	}
}
