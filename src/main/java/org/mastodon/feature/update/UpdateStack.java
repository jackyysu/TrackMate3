package org.mastodon.feature.update;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.DefaultFeatureComputerService;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.feature.FeatureSpec;

public abstract class UpdateStack< O > implements Feature< O >
{

	/**
	 * Number of graph update commits we store before triggering a full
	 * recalculation.
	 */
	static final int BUFFER_SIZE = 10;

	private final SizedDeque< UpdateState< O > > stateStack;

	private Update< O > currentUpdate;

	private final RefCollection< O > pool;

	protected UpdateStack( final RefCollection< O > pool )
	{
		this( pool, new SizedDeque<>( BUFFER_SIZE ), new Update<>( pool ) );
		commit( Collections.emptyList() );
	}

	/**
	 * For deserialization only.
	 */
	protected UpdateStack( final RefCollection< O > pool, final SizedDeque< UpdateState< O > > stateStack, final Update< O > currentUpdate )
	{
		this.pool = pool;
		this.stateStack = stateStack;
		this.currentUpdate = currentUpdate;
	}

	/**
	 * This method should only be called by the
	 * {@link DefaultFeatureComputerService} after the computation step.
	 * <p>
	 * It stacks the current changes and mark them for the specified feature
	 * keys, then starts building a new one.
	 *
	 * @param featureKeys
	 *            the keys of the features that were computed before this
	 *            commit.
	 */
	public void commit( final Collection< FeatureSpec< ?, ? > > featureKeys )
	{
		currentUpdate = new Update<>( pool );
		stateStack.push( new UpdateState<>( featureKeys, currentUpdate ) );
	}

	/**
	 * Returns the changes needed to update the feature with the specified key.
	 * A <code>null</code> value indicate that the feature should be re-computed
	 * for all the objects of the graph, without the possibility to use
	 * incremental updates. Otherwise, the objects to update can be retrieved
	 * with the {@link GraphUpdate#vertices(UpdateLocality)} and
	 * {@link GraphUpdate#edges(UpdateLocality)} methods.
	 *
	 * @param featureKey
	 *            the key of the feature to build a graph update for.
	 * @return a graph update object, or <code>null</code> if the full graph
	 *         needs to re-computed for this feature.
	 */
	public Update< O > changesFor( final FeatureSpec< ?, ? > featureKey )
	{
		final Update< O > changes = new Update<>( pool );
		for ( final UpdateState< O > updateState : stateStack )
		{
			changes.concatenate( updateState.getChanges() );
			if ( updateState.contains( featureKey ) )
				return changes;
		}
		return null;
	}

	public void clear()
	{
		stateStack.clear();
		currentUpdate.clear();
	}

	public void addModified( final O obj )
	{
		currentUpdate.add( obj );
	}

	public void addNeighbor( final O obj )
	{
		currentUpdate.addAsNeighbor( obj );
	}

	public void remove( final O obj )
	{
		currentUpdate.remove( obj );
		// Walk through the stack and remove trace of it.
		for ( final UpdateState< O > state : stateStack )
			state.getChanges().remove( obj );
	}

	/*
	 * Feature methods & fields.
	 */

	@Override
	public FeatureProjection< O > project( final FeatureProjectionKey key )
	{
		return null;
	}

	@Override
	public Set< FeatureProjection< O > > projections()
	{
		return Collections.emptySet();
	}

	/**
	 * Simple data class used to store graph updates in a stack along with the
	 * feature keys they are up-to-date for.
	 *
	 * @author Jean-Yves Tinevez
	 *
	 * @param <O>
	 *            the type of objects whose modification are tracked.
	 */
	public static final class UpdateState< O >
	{

		private final Collection< FeatureSpec< ?, ? > > featureKeys;

		private final Update< O > changes;

		public UpdateState( final Collection< FeatureSpec< ?, ? > > featureKeys, final Update< O > changes )
		{
			this.featureKeys = featureKeys;
			this.changes = changes;
		}

		public boolean contains( final FeatureSpec< ?, ? > featureKey )
		{
			return featureKeys.contains( featureKey );
		}

		@Override
		public String toString()
		{
			return super.toString() + " -> " + featureKeys.toString();
		}

		public Update< O > getChanges()
		{
			return changes;
		}

		Collection< FeatureSpec< ?, ? > > getFeatureKeys()
		{
			return featureKeys;
		}
	}

	static class SerialisationAccess< O >
	{

		private final UpdateStack< O > updateStack;

		SerialisationAccess( final UpdateStack< O > updateStack )
		{
			this.updateStack = updateStack;
		}

		SizedDeque< UpdateState< O > > getStateStack()
		{
			return updateStack.stateStack;
		}

		Update< O > getCurrentUpate()
		{
			return updateStack.currentUpdate;
		}
	}

}
