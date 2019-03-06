package org.mastodon.revised.mamut;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import org.mastodon.app.ViewGraph;
import org.mastodon.app.ui.MastodonFrameView;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.feature.FeatureModel;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.model.tag.TagSetModel;
import org.mastodon.revised.trackscheme.display.ColorBarOverlay;
import org.mastodon.revised.trackscheme.display.ColorBarOverlay.Position;
import org.mastodon.revised.ui.coloring.ColoringMenu;
import org.mastodon.revised.ui.coloring.ColoringModel;
import org.mastodon.revised.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.revised.ui.coloring.TagSetGraphColorGenerator;
import org.mastodon.revised.ui.coloring.feature.FeatureColorModeManager;

public class MamutView< VG extends ViewGraph< Spot, Link, V, E >, V extends Vertex< E >, E extends Edge< V > >
		extends MastodonFrameView< MamutAppModel, VG, Spot, Link, V, E >
{
	public MamutView( final MamutAppModel appModel, final VG viewGraph, final String[] keyConfigContexts )
	{
		super( appModel, viewGraph, keyConfigContexts );
	}

	/**
	 * Sets up and registers the coloring menu item and related actions and
	 * listeners.
	 *
	 * @param colorGeneratorAdapter
	 *                                  adapts a (modifiable) model coloring to view
	 *                                  vertices/edges.
	 * @param menuHandle
	 *                                  handle to the JMenu corresponding to the
	 *                                  coloring submenu. Coloring options will be
	 *                                  installed here.
	 * @param refresh
	 *                                  triggers repaint of the graph (called when
	 *                                  coloring changes)
	 * @return the coloring model of the coloring submenu.
	 */
	protected ColoringModel registerColoring(
			final GraphColorGeneratorAdapter< Spot, Link, V, E > colorGeneratorAdapter,
			final JMenuHandle menuHandle,
			final Runnable refresh )
	{
		final TagSetModel< Spot, Link > tagSetModel = appModel.getModel().getTagSetModel();
		final FeatureModel featureModel = appModel.getModel().getFeatureModel();
		final FeatureColorModeManager featureColorModeManager = appModel.getFeatureColorModeManager();
		final ColoringModel coloringModel = new ColoringModel( tagSetModel, featureColorModeManager, featureModel );
		final ColoringMenu coloringMenu = new ColoringMenu( menuHandle.getMenu(), coloringModel );

		tagSetModel.listeners().add( coloringModel );
		onClose( () -> tagSetModel.listeners().remove( coloringModel ) );
		tagSetModel.listeners().add( coloringMenu );
		onClose( () -> tagSetModel.listeners().remove( coloringMenu ) );

		featureColorModeManager.listeners().add( coloringModel );
		onClose( () -> featureColorModeManager.listeners().remove( coloringModel ) );
		featureColorModeManager.listeners().add( coloringMenu );
		onClose( () -> featureColorModeManager.listeners().remove( coloringMenu ) );

		featureModel.listeners().add( coloringMenu );
		onClose( () -> featureModel.listeners().remove( coloringMenu ) );

		final ColoringModel.ColoringChangedListener coloringChangedListener = () -> {
			if ( coloringModel.noColoring() )
				colorGeneratorAdapter.setColorGenerator( null );
			else if ( coloringModel.getTagSet() != null )
				colorGeneratorAdapter.setColorGenerator( new TagSetGraphColorGenerator<>( tagSetModel, coloringModel.getTagSet() ) );
			else if ( coloringModel.getFeatureColorMode() != null )
				colorGeneratorAdapter.setColorGenerator( coloringModel.getFeatureGraphColorGenerator() );
			refresh.run();
		};
		coloringModel.listeners().add( coloringChangedListener );

		return coloringModel;
	}

	protected void registerColorbarOverlay(
			final ColorBarOverlay colorBarOverlay,
			final JMenuHandle menuHandle,
			final Runnable refresh )
	{
		menuHandle.getMenu().add( new JSeparator() );
		final JCheckBoxMenuItem toggleOverlay = new JCheckBoxMenuItem( "Show colorbar", ColorBarOverlay.DEFAULT_VISIBLE );
		toggleOverlay.addActionListener( ( l ) -> {
			colorBarOverlay.setVisible( toggleOverlay.isSelected() );
			refresh.run();
		} );
		menuHandle.getMenu().add( toggleOverlay );

		menuHandle.getMenu().add( new JSeparator() );
		menuHandle.getMenu().add( "Position:" ).setEnabled( false );

		final ButtonGroup buttonGroup = new ButtonGroup();
		for ( final Position position : Position.values() )
		{
			final JRadioButtonMenuItem positionItem = new JRadioButtonMenuItem( position.toString() );
			positionItem.addActionListener( ( l ) -> {
				if ( positionItem.isSelected() )
				{
					colorBarOverlay.setPosition( position );
					refresh.run();
				}
			} );
			buttonGroup.add( positionItem );
			menuHandle.getMenu().add( positionItem );

			if ( position.equals( ColorBarOverlay.DEFAULT_POSITION ) )
				positionItem.setSelected( true );
		}
	}

}
