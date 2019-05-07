package org.mastodon.feature.update;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.update.GraphUpdateStack.SerialisationAccess;
import org.mastodon.graph.Edge;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.io.RawGraphIO.FileIdToGraphMap;
import org.mastodon.graph.io.RawGraphIO.GraphToFileIdMap;

public class GraphUpdateStackSerialization
{

	public static < V extends Vertex< E >, E extends Edge< V > > void serialize(
			final GraphUpdateStack< V, E > graphUpdateStack,
			final GraphToFileIdMap< V, E > idmap,
			final ObjectOutputStream oos )
			throws IOException
	{
		// Write current update.
		final SerialisationAccess< V, E > serialisationAccess = new SerialisationAccess<>( graphUpdateStack );
		GraphUpdateSerialization.serialize( serialisationAccess.getCurrentUpate(), idmap, oos );
		final SizedDeque< UpdateState< V, E > > stack = serialisationAccess.getStateStack();
		// Write size of stack.
		oos.writeInt( stack.size() );
		for ( final UpdateState< V, E > updateState : stack )
		{
			final Collection< FeatureSpec< ?, ? > > featureKeys = updateState.getFeatureKeys();
			// Write number of keys.
			oos.writeInt( featureKeys.size() );
			// Write each key.
			for ( final FeatureSpec< ?, ? > featureKey : featureKeys )
				FeatureSpecSerialization.serialize( featureKey, oos );

			// Write changes.
			GraphUpdateSerialization.serialize( updateState.getChanges(), idmap, oos );
		}
	}

	public static < V extends Vertex< E >, E extends Edge< V > > GraphUpdateStack< V, E > deserialize(
			final ReadOnlyGraph< V, E > graph,
			final FileIdToGraphMap< V, E > idmap,
			final ObjectInputStream ois )
			throws IOException, ClassNotFoundException
	{
		// Read current update.
		final GraphUpdate< V, E > currentUpdate = GraphUpdateSerialization.deserialize( graph, idmap, ois );

		final SizedDeque< UpdateState< V, E > > stateStack = new SizedDeque<>( GraphUpdateStack.BUFFER_SIZE );
		// Read size of stack.
		final int size = ois.readInt();
		for ( int i = 0; i < size; i++ )
		{
			// Read number of keys.
			final int nKeys = ois.readInt();
			final Collection< FeatureSpec< ?, ? > > featureKeys = new ArrayList<>( nKeys );
			// Read each key.
			for ( int j = 0; j < nKeys; j++ )
				featureKeys.add( FeatureSpecSerialization.deserialize( ois ) );

			// Read changes.
			final GraphUpdate< V, E > changes = GraphUpdateSerialization.deserialize( graph, idmap, ois );
			stateStack.add( new UpdateState<>( featureKeys, changes ) );
		}

		return new GraphUpdateStack<>( graph, stateStack, currentUpdate );
	}
}
