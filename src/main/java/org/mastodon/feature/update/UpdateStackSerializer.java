package org.mastodon.feature.update;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.io.FeatureSerializer;
import org.mastodon.feature.update.UpdateStack.SerialisationAccess;
import org.mastodon.feature.update.UpdateStack.UpdateState;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.io.ObjectToFileIdMap;

public abstract class UpdateStackSerializer< O, F extends UpdateStack< O > > implements FeatureSerializer< O, F >
{

	@Override
	public void serialize( final F feature, final ObjectToFileIdMap< O > idmap, final ObjectOutputStream oos ) throws IOException
	{
		// Write current update.
		final SerialisationAccess< O > serialisationAccess = new SerialisationAccess<>( feature );
		UpdateSerialization.serialize( serialisationAccess.getCurrentUpate(), idmap, oos );
		final SizedDeque< UpdateState< O > > stack = serialisationAccess.getStateStack();
		// Write size of stack.
		oos.writeInt( stack.size() );
		for ( final UpdateState< O > updateState : stack )
		{
			final Collection< FeatureSpec< ?, ? > > featureKeys = updateState.getFeatureKeys();
			// Write number of keys.
			oos.writeInt( featureKeys.size() );
			// Write each key.
			for ( final FeatureSpec< ?, ? > featureKey : featureKeys )
				FeatureSpecSerialization.serialize( featureKey, oos );

			// Write changes.
			UpdateSerialization.serialize( updateState.getChanges(), idmap, oos );
		}
	}

	public DeserializedStruct< O > deserializeStruct( final FileIdToObjectMap< O > idmap, final RefCollection< O > pool, final String spaceUnits, final String timeUnits, final ObjectInputStream ois ) throws IOException, ClassNotFoundException
	{
		// Read current update.
		final Update< O > currentUpdate = UpdateSerialization.deserialize( pool, idmap, ois );

		final SizedDeque< UpdateState< O > > stateStack = new SizedDeque<>( UpdateStack.BUFFER_SIZE );
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
			final Update< O > changes = UpdateSerialization.deserialize( pool, idmap, ois );
			stateStack.add( new UpdateState<>( featureKeys, changes ) );
		}

		return new DeserializedStruct<>( stateStack, currentUpdate );
	}

	protected static class DeserializedStruct< O >
	{

		public final SizedDeque< UpdateState< O > > stateStack;

		public final Update< O > currentUpdate;

		public DeserializedStruct( final SizedDeque< UpdateState< O > > stateStack, final Update< O > currentUpdate )
		{
			this.stateStack = stateStack;
			this.currentUpdate = currentUpdate;
		}

	}
}
