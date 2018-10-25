package org.mastodon.feature.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Spot;

public class FeatureTableDialogExample
{
	private static class MyListSelectionAggregator implements ListSelectionListener
	{

		private final Set< ListSelectionModel > selectionModels = new HashSet<>();

		public void add( final ListSelectionModel listSelectionModel )
		{
			selectionModels.add( listSelectionModel );
			listSelectionModel.addListSelectionListener( this );
		}

		@Override
		public void valueChanged( final ListSelectionEvent event )
		{
			System.out.println( event ); // DEBUG
			final ListSelectionModel source = ( ListSelectionModel ) event.getSource();
			for ( final ListSelectionModel lsm : selectionModels )
			{
				if ( lsm.equals( source ) )
					continue;

				lsm.removeListSelectionListener( this );
				lsm.clearSelection();
				lsm.addListSelectionListener( this );
			}
		}
	}

	static class MyElement
	{
		private final String name;

		private boolean selected;

		private final boolean uptodate;

		public MyElement( final String name, final boolean selected, final boolean uptodate )
		{
			this.name = name;
			this.selected = selected;
			this.uptodate = uptodate;
		}

		public String getName()
		{
			return name;
		}

		public boolean isSelected()
		{
			return selected;
		}

		public void setSelected( final boolean selected )
		{
			this.selected = selected;
		}

		public boolean isUptodate()
		{
			return uptodate;
		}

//		public void setUptodate( final boolean uptodate )
//		{
//			this.uptodate = uptodate;
//		}

		@Override
		public String toString()
		{
			return name + " isSelected=" + selected + " isUptodate=" + uptodate;
		}
	}

	private static FeatureTable< List< MyElement >, MyElement > createFeatureTable()
	{
		final FeatureTable< List< MyElement >, MyElement > featureTable = new FeatureTable<>(
				null,
				List::size,
				List::get,
				MyElement::getName,
				MyElement::isSelected,
				MyElement::setSelected,
				MyElement::isUptodate );
		return featureTable;
	}

	static class FeatureTableDialog extends JDialog
	{
		private static final long serialVersionUID = 1L;

		public FeatureTableDialog( final Frame owner, final List< MyElement > elements1, final List< MyElement > elements2 )
		{
			super( owner, "Feature computation", false );

			final List< Class< ? > > targets = Arrays.asList( Spot.class, Link.class );

			final FeatureTable< List< MyElement >, MyElement > ft1 = createFeatureTable();
			ft1.setElements( elements1 );

			final FeatureTable< List< MyElement >, MyElement > ft2 = createFeatureTable();
			ft2.setElements( elements2 );

			final List< FeatureTable< List< MyElement >, MyElement > > featureTables = new ArrayList<>();
			featureTables.add( ft1 );
			featureTables.add( ft2 );

			final JPanel tablePanel = new JPanel();
			tablePanel.setLayout( new BoxLayout( tablePanel, BoxLayout.PAGE_AXIS ) );
			tablePanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );

			final FeatureTable.SelectionListener< MyElement > sl = ( t ) -> System.out.println( "Element " + t + " changed." );

			final MyListSelectionAggregator aggregator = new MyListSelectionAggregator();

			for ( int i = 0; i < targets.size(); i++ )
			{
				final FeatureTable< List< MyElement >, MyElement > featureTable = featureTables.get( i );
				featureTable.getComponent().setAlignmentX( Component.LEFT_ALIGNMENT );
				featureTable.getComponent().setBackground( tablePanel.getBackground() );
				aggregator.add( featureTable.getListSelectionModel() );

				final JLabel lbl = new JLabel( targets.get( i ).getSimpleName() );
				lbl.setFont( tablePanel.getFont().deriveFont( Font.BOLD ).deriveFont( tablePanel.getFont().getSize2D() + 2f ) );
				lbl.setHorizontalTextPosition( SwingConstants.LEADING );
				lbl.setHorizontalAlignment( SwingConstants.LEADING );
				lbl.setAlignmentX( Component.LEFT_ALIGNMENT );
				tablePanel.add( lbl );
				tablePanel.add( Box.createVerticalStrut( 5 ) );
				tablePanel.add( featureTable.getComponent() );
				tablePanel.add( Box.createVerticalStrut( 10 ) );

				featureTable.selectionListeners().add( sl );
			}


			final JPanel featureTablePanel = new JPanel( new BorderLayout( 0, 0 ) );
			featureTablePanel.add( new JScrollPane( tablePanel ), BorderLayout.CENTER );

			featureTablePanel.setPreferredSize( new Dimension( 400, 500 ) );
			getContentPane().add( featureTablePanel );
			pack();
		}
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, InterruptedException
	{

		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final List< MyElement > elements1 = new ArrayList<>();
		elements1.add( new MyElement( "Spot N links", true, true ) );
		elements1.add( new MyElement( "Spot frame", true, true ) );
		elements1.add( new MyElement( "Spot gaussian-filtered intensity", false, false ) );
		elements1.add( new MyElement( "Spot track ID", true, true ) );
		elements1.add( new MyElement( "Track N spots", true, false ) );

		final List< MyElement > elements2 = new ArrayList<>();
		elements2.add( new MyElement( "Link displacement", true, true ) );
		elements2.add( new MyElement( "Link velocity", true, true ) );

		final FeatureTableDialog frame = new FeatureTableDialog( null, elements1, elements2 );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setLocationByPlatform( true );
		frame.setVisible( true );
	}
}
