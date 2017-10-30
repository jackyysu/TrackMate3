package org.mastodon.revised.ui;

import java.awt.event.ActionEvent;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.GraphChangeNotifier;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.algorithm.traversal.DepthFirstSearch;
import org.mastodon.graph.algorithm.traversal.GraphSearch.SearchDirection;
import org.mastodon.graph.algorithm.traversal.SearchListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.undo.UndoPointMarker;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

/**
 * User-interface actions that are related to a model selection.
 *
 * @param <V>
 *            vertex type
 * @param <E>
 *            edge type
 *
 * @author Jean-Yves Tinevez
 * @author Tobias Pietzsch
 */
public class SelectionActions< V extends Vertex< E >, E extends Edge< V > >
{
	public static final String DELETE_SELECTION = "delete selection";
	public static final String SELECT_WHOLE_TRACK = "select whole track";
	public static final String SELECT_TRACK_DOWNWARD = "select track downward";
	public static final String SELECT_TRACK_UPWARD = "select track upward";

	public static final String[] DELETE_SELECTION_KEYS = new String[] { "shift DELETE" };
	public static final String[] SELECT_WHOLE_TRACK_KEYS = new String[] { "shift SPACE" };
	public static final String[] SELECT_TRACK_DOWNWARD_KEYS = new String[] { "shift PAGE_DOWN" };
	public static final String[] SELECT_TRACK_UPWARD_KEYS = new String[] { "shift PAGE_UP" };

	/**
	 * Create selection actions and install them in the specified
	 * {@link Actions}.
	 *
	 * @param actions
	 *            Actions are added here.
	 * @param graph
	 * @param lock
	 * @param notify
	 * @param selection
	 * @param undo
	 */
	public static < V extends Vertex< E >, E extends Edge< V > > void install(
			final Actions actions,
			final Graph< V, E > graph,
			final ReentrantReadWriteLock lock,
			final GraphChangeNotifier notify,
			final SelectionModel< V, E > selection,
			final UndoPointMarker undo )
	{
		final SelectionActions< V, E > sa = new SelectionActions<>( graph, lock, notify, selection, undo );
		actions.namedAction( sa.deleteSelectionAction, DELETE_SELECTION_KEYS );
		actions.namedAction( sa.selectWholeTrackAction, SELECT_WHOLE_TRACK_KEYS );
		actions.namedAction( sa.selectTrackDownwardAction, SELECT_TRACK_DOWNWARD_KEYS );
		actions.namedAction( sa.selectTrackUpwardAction, SELECT_TRACK_UPWARD_KEYS );
	}

	private final Graph< V, E > graph;

	private final ReentrantReadWriteLock lock;

	private final GraphChangeNotifier notify;

	private final SelectionModel< V, E > selection;

	private final UndoPointMarker undo;

	private final DeleteSelectionAction deleteSelectionAction;

	private final TrackSelectionAction selectWholeTrackAction;

	private final TrackSelectionAction selectTrackDownwardAction;

	private final TrackSelectionAction selectTrackUpwardAction;

	private SelectionActions(
			final Graph< V, E > graph,
			final ReentrantReadWriteLock lock,
			final GraphChangeNotifier notify,
			final SelectionModel< V, E > selection,
			final UndoPointMarker undo )
	{
		this.graph = graph;
		this.lock = lock;
		this.notify = notify;
		this.selection = selection;
		this.undo = undo;
		deleteSelectionAction = new DeleteSelectionAction( DELETE_SELECTION );
		selectWholeTrackAction = new TrackSelectionAction( SELECT_WHOLE_TRACK, SearchDirection.UNDIRECTED );
		selectTrackDownwardAction = new TrackSelectionAction( SELECT_TRACK_DOWNWARD, SearchDirection.DIRECTED );
		selectTrackUpwardAction = new TrackSelectionAction( SELECT_TRACK_UPWARD, SearchDirection.REVERSED );
	}

	class DeleteSelectionAction	extends AbstractNamedAction
	{
		private static final long serialVersionUID = 1L;

		DeleteSelectionAction( final String name )
		{
			super( name );
			setEnabled( !selection.isEmpty() );
			selection.listeners().add( () -> setEnabled( !selection.isEmpty() ) );
		}

		@Override
		public void actionPerformed( final ActionEvent event )
		{
			if ( selection.isEmpty() )
				return;

			lock.writeLock().lock();
			try
			{
				final RefSet< E > edges = selection.getSelectedEdges();
				final RefSet< V > vertices = selection.getSelectedVertices();

				selection.pauseListeners();

				for ( final E e : edges )
					graph.remove( e );

				for ( final V v : vertices )
					graph.remove( v );

				undo.setUndoPoint();
				notify.notifyGraphChanged();
				selection.resumeListeners();
			}
			finally
			{
				lock.writeLock().unlock();
			}
		}
	}

	class TrackSelectionAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 1L;

		private final SearchDirection directivity;

		TrackSelectionAction( final String name, final SearchDirection directivity )
		{
			super( name );
			this.directivity = directivity;
			setEnabled( !selection.isEmpty() );
			selection.listeners().add( () -> setEnabled( !selection.isEmpty() ) );
		}

		@Override
		public void actionPerformed( final ActionEvent event )
		{
			if ( selection.isEmpty() )
				return;

			lock.writeLock().lock();
			try
			{
				selection.pauseListeners();

				final RefSet< V > vertices = RefCollections.createRefSet( graph.vertices() );
				vertices.addAll( selection.getSelectedVertices() );
				final V ref = graph.vertexRef();
				for ( final E e : selection.getSelectedEdges() )
				{
					vertices.add( e.getSource( ref ) );
					vertices.add( e.getTarget( ref ) );
				}
				graph.releaseRef( ref );

				selection.clearSelection();

				// Prepare the iterator.
				final DepthFirstSearch< V, E > search = new DepthFirstSearch<>( graph, directivity );
				search.setTraversalListener( new SearchListener< V, E, DepthFirstSearch< V, E > >()
				{
					@Override
					public void processVertexLate( final V vertex, final DepthFirstSearch< V, E > search )
					{}

					@Override
					public void processVertexEarly( final V vertex, final DepthFirstSearch< V, E > search )
					{
						selection.setSelected( vertex, true );
					}

					@Override
					public void processEdge( final E edge, final V from, final V to, final DepthFirstSearch< V, E > search )
					{
						selection.setSelected( edge, true );
					}

					@Override
					public void crossComponent( final V from, final V to, final DepthFirstSearch< V, E > search )
					{}
				} );

				// Iterate from all vertices that were in the selection.
				for ( final V v : vertices )
					if ( !selection.isSelected( v ) )
						search.start( v );

				selection.resumeListeners();
			}
			finally
			{
				lock.writeLock().unlock();
			}
		}
	}
}
