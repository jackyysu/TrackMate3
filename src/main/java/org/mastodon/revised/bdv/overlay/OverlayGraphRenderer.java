package org.mastodon.revised.bdv.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.kdtree.ClipConvexPolytope;
import org.mastodon.kdtree.IncrementalNearestNeighborSearch;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.Util;
import org.mastodon.revised.bdv.overlay.ScreenVertexMath.Ellipse;
import org.mastodon.revised.bdv.overlay.util.BdvRendererUtil;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;

import bdv.util.Affine3DHelpers;
import bdv.viewer.TimePointListener;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;
import net.imglib2.util.LinAlgHelpers;

/**
 * Renderer for a time-resliced graph overlay on a BDV.
 * <p>
 * In this class, spatial coordinates are stored in local variables named
 * <code>gPos</code> and <code>lPos</code> of type <code>double[]</code> with 3
 * elements:
 * <ul>
 * <li><code>gPos</code> are world coordinates. It is used to store coordinates
 * in the global referential, that is the one with absolute, physical
 * coordinates. It is used <i>e.g.</i> to store vertex coordinates:
 * <code>vertex.localize(gPos)</code>.</li>
 * <li><code>lPos</code> are viewer coordinates. It is used to store coordinates
 * in the local referential, currently rendered in the BDV under a certain
 * orientation, zoom, etc. Mouse coordinates are typically stored in this
 * variable.
 * </ul>
 *
 * <p>
 *
 * TODO: Review and revise.
 *
 * @param <V>
 *            the type of model vertex.
 * @param <E>
 *            the type of model edge.
 *
 * @author Tobias Pietzsch
 */
public class OverlayGraphRenderer< V extends OverlayVertex< V, E >, E extends OverlayEdge< E, V > >
		implements OverlayRenderer, TransformListener< AffineTransform3D >, TimePointListener
{
	private int width;

	private int height;

	private final AffineTransform3D renderTransform;

	private int renderTimepoint;

	private final OverlayGraph< V, E > graph;

	private final SpatioTemporalIndex< V > index;

	private final HighlightModel< V, E > highlight;

	private final FocusModel< V, E > focus;

	private final SelectionModel< V, E > selection;

	public OverlayGraphRenderer(
			final OverlayGraph< V, E > graph,
			final HighlightModel< V, E > highlight,
			final FocusModel< V, E > focus,
			final SelectionModel< V, E > selection )
	{
		this.graph = graph;
		this.highlight = highlight;
		this.focus = focus;
		this.selection = selection;
		index = graph.getIndex();
		renderTransform = new AffineTransform3D();
		setRenderSettings( new RenderSettings() ); // default RenderSettings
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		synchronized ( renderTransform )
		{
			renderTransform.set( transform );
		}
	}

	@Override
	public void timePointChanged( final int timepoint )
	{
		renderTimepoint = timepoint;
	}

	public void setRenderSettings( final RenderSettings settings )
	{
		antialiasing = settings.getUseAntialiasing()
				? RenderingHints.VALUE_ANTIALIAS_ON
				: RenderingHints.VALUE_ANTIALIAS_OFF;
		useGradient = settings.getUseGradient();
		timeLimit = settings.getTimeLimit();
		drawLinks = settings.getDrawLinks();
		drawSpots = settings.getDrawSpots();
		drawEllipsoidSliceProjection = settings.getDrawEllipsoidSliceProjection();
		drawEllipsoidSliceIntersection = settings.getDrawEllipsoidSliceIntersection();
		drawPoints = settings.getDrawSpotCenters();
		drawPointsForEllipses = settings.getDrawSpotCentersForEllipses();
		drawSpotLabels = settings.getDrawSpotLabels();
		focusLimit = settings.getFocusLimit();
		isFocusLimitViewRelative = settings.getFocusLimitViewRelative();
		ellipsoidFadeDepth = settings.getEllipsoidFadeDepth();
		pointFadeDepth = settings.getPointFadeDepth();
	}

	public static final double pointRadius = 2.5;

	/**
	 * Antialiasing settings. Must be one of {@link RenderingHints} antialias
	 * field.
	 */
	private Object antialiasing;

	/**
	 * If {@code true}, draw links using a gradient from source color to target
	 * color. If {@code false}, draw links using the target color.
	 */
	private boolean useGradient;

	/**
	 * Maximum number of timepoints into the past for which outgoing edges
	 * should be drawn.
	 */
	private int timeLimit;

	/**
	 * Whether to draw links (at all). For specific settings, see
	 * {@link #useGradient} and {@link #timeLimit}.
	 */
	private boolean drawLinks;

	/**
	 * Whether to draw spots (at all). For specific settings, see
	 * {@link #drawEllipsoidSliceIntersection},
	 * {@link #drawEllipsoidSliceProjection}, {@link #drawPointsForEllipses},
	 * {@link #drawPoints} and {@link #drawSpotLabels}.
	 */
	private boolean drawSpots;

	/**
	 * Whether to draw the intersections of spot ellipsoids with the view plane.
	 */
	private boolean drawEllipsoidSliceProjection;

	/**
	 * Whether to draw the projections of spot ellipsoids onto the view plane.
	 */
	private boolean drawEllipsoidSliceIntersection;

	/**
	 * Whether to draw spot centers.
	 */
	private boolean drawPoints;

	/**
	 * Whether to draw spot centers also for those points that are visible as ellipses.
	 */
	private boolean drawPointsForEllipses;

	/**
	 * Whether to draw spot labels next to ellipses.
	 */
	private boolean drawSpotLabels;

	/**
	 * Maximum distance from view plane up to which to draw spots.
	 *
	 * <p>
	 * Depending on {@link #isFocusLimitViewRelative}, the distance is either in
	 * the current view coordinate system or in the global coordinate system. If
	 * {@code isFocusLimitViewRelative() == true} then the distance is in
	 * current view coordinates. For example, a value of 100 means that spots
	 * will be visible up to 100 pixel widths from the view plane. Thus, the
	 * effective focus range depends on the current zoom level. If
	 * {@code isFocusLimitViewRelative() == false} then the distance is in
	 * global coordinates. A value of 100 means that spots will be visible up to
	 * 100 units (of the global coordinate system) from the view plane.
	 *
	 * <p>
	 * Ellipsoids are drawn increasingly translucent the closer they are to
	 * {@link #focusLimit}. See {@link #ellipsoidFadeDepth}.
	 */
	private double focusLimit;

	/**
	 * Whether the {@link #focusLimit} is relative to the the current
	 * view coordinate system.
	 *
	 * <p>
	 * If {@code true} then the distance is in current view coordinates. For
	 * example, a value of 100 means that spots will be visible up to 100 pixel
	 * widths from the view plane. Thus, the effective focus range depends on
	 * the current zoom level. If {@code false} then the distance is in global
	 * coordinates. A value of 100 means that spots will be visible up to 100
	 * units (of the global coordinate system) from the view plane.
	 */
	private boolean isFocusLimitViewRelative;

	/**
	 * The ratio of {@link #focusLimit} at which ellipsoids start to
	 * fade. Ellipsoids are drawn increasingly translucent the closer they are
	 * to {@link #focusLimit}. Up to ratio {@link #ellipsoidFadeDepth}
	 * they are fully opaque, then their alpha value goes to 0 linearly.
	 */
	private double ellipsoidFadeDepth;

	/**
	 * The ratio of {@link #focusLimit} at which points start to
	 * fade. Points are drawn increasingly translucent the closer they are
	 * to {@link #focusLimit}. Up to ratio {@link #pointFadeDepth}
	 * they are fully opaque, then their alpha value goes to 0 linearly.
	 */
	private double pointFadeDepth;

	/**
	 * Return signed distance of p to z=0 plane, truncated at cutoff and scaled
	 * by 1/cutoff. A point on the plane has d=0. A Point that is at cutoff or
	 * farther behind the plane has d=1. A point that is at -cutoff or more in
	 * front of the plane has d=-1.
	 */
	private static double sliceDistance( final double z, final double cutoff )
	{
		if ( z > 0 )
			return Math.min( z, cutoff ) / cutoff;
		else
			return Math.max( z, -cutoff ) / cutoff;
	}

	/**
	 * Return signed distance of timepoint t to t0, truncated at cutoff and
	 * scaled by 1/cutoff. t=t0 has d=0. t&lt;=t0-cutoff has d=-1.
	 * t=&gt;t0+cutoff has d=1.
	 */
	private static double timeDistance( final double t, final double t0, final double cutoff )
	{
		final double d = t - t0;
		if ( d > 0 )
			return Math.min( d, cutoff ) / cutoff;
		else
			return Math.max( d, -cutoff ) / cutoff;
	}

	private static int trunc255( final int i )
	{
		return Math.min( 255, Math.max( 0, i ) );
	}

	private static int truncRGBA( final int r, final int g, final int b, final int a )
	{
		return ARGBType.rgba(
				trunc255( r ),
				trunc255( g ),
				trunc255( b ),
				trunc255( a ) );
	}

	private static int truncRGBA( final double r, final double g, final double b, final double a )
	{
		return truncRGBA(
				( int ) ( 255 * r ),
				( int ) ( 255 * g ),
				( int ) ( 255 * b ),
				( int ) ( 255 * a ) );
	}

	/**
	 * Generates a color suitable to paint an abject that might be away from the
	 * focus plane, or away in time.
	 *
	 * @param sd
	 *            sliceDistande, between -1 and 1. see
	 *            {@link #sliceDistance(double, double)}.
	 * @param td
	 *            timeDistande, between -1 and 1. see
	 *            {@link #timeDistance(double, double, double)}.
	 * @param sdFade
	 *            between 0 and 1, from which |sd| value color starts to fade
	 *            (alpha value decreases).
	 * @param tdFade
	 *            between 0 and 1, from which |td| value color starts to fade
	 *            (alpha value decreases).
	 * @param isSelected
	 *            whether to use selected or un-selected color scheme.
	 * @return vertex/edge color.
	 */
	private static Color getColor( final double sd, final double td, final double sdFade, final double tdFade, final boolean isSelected )
	{
		/*
		 * |sf| = {                  0  for  |sd| <= sdFade,
		 *          linear from 0 to 1  for  |sd| = sdFade to |sd| = 1 }
		 *
		 * sgn(sf) = sgn(sd)
		 */
		final double sf;
		if ( sd > 0 )
		{
			sf = Math.max( 0, ( sd - sdFade ) / ( 1 - sdFade ) );
		}
		else
		{
			sf = -Math.max( 0, ( -sd - sdFade ) / ( 1 - sdFade ) );
		}

		final double tf;
		if ( td > 0 )
		{
			tf = Math.max( 0, ( td - tdFade ) / ( 1 - tdFade ) );
		}
		else
		{
			tf = -Math.max( 0, ( -td - tdFade ) / ( 1 - tdFade ) );
		}

		final double a = -2 * td;
		final double b = 1 + 2 * td;
		final double r = isSelected ? b : a;
		final double g = isSelected ? a : b;
		return new Color( truncRGBA( r, g, 0.1, ( 1 + tf ) * ( 1 - Math.abs( sf ) ) ), true );
	}

	/**
	 * Get the {@link ConvexPolytope} around the specified viewer coordinate
	 * range that is large enough border to ensure that it contains center of
	 * every ellipsoid touching the specified coordinate range.
	 *
	 * @param xMin
	 *            minimum X position on the z=0 plane in viewer coordinates.
	 * @param xMax
	 *            maximum X position on the z=0 plane in viewer coordinates.
	 * @param yMin
	 *            minimum Y position on the z=0 plane in viewer coordinates.
	 * @param yMax
	 *            maximum Y position on the z=0 plane in viewer coordinates.
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	private ConvexPolytope getOverlappingPolytopeGlobal(
			final double xMin,
			final double xMax,
			final double yMin,
			final double yMax,
			final AffineTransform3D transform,
			final int timepoint )
	{
		final double globalToViewerScale = Affine3DHelpers.extractScale( transform, 0 );
		final double maxDepth = isFocusLimitViewRelative
				? focusLimit
				: focusLimit * globalToViewerScale;
		final double border = globalToViewerScale * Math.sqrt( graph.getMaxBoundingSphereRadiusSquared( timepoint ) );
		return BdvRendererUtil.getPolytopeGlobal( transform,
				xMin - border, xMax + border,
				yMin - border, yMax + border,
				-maxDepth - border, maxDepth + border );
	}

	/**
	 * Returns a copy of the {@link AffineTransform3D} used in this renderer.
	 *
	 * @return a new {@link AffineTransform3D} object.
	 */
	private AffineTransform3D getRenderTransformCopy()
	{
		final AffineTransform3D transform = new AffineTransform3D();
		synchronized ( renderTransform )
		{
			transform.set( renderTransform );
		}
		return transform;
	}

	/**
	 * Get the {@link ConvexPolytope} bounding the visible region of global
	 * space, extended by a large enough border to ensure that it contains the
	 * center of every ellipsoid that intersects the visible volume.
	 *
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	ConvexPolytope getVisiblePolytopeGlobal(
			final AffineTransform3D transform,
			final int timepoint )
	{
		return getOverlappingPolytopeGlobal( 0, width, 0, height, transform, timepoint );
	}

	/**
	 * Get the {@link ConvexPolytope} around the specified viewer coordinate
	 * that is large enough to ensure that it contains the center of every
	 * ellipsoid containing the specified coordinate.
	 *
	 * @param x
	 *            position on the z=0 plane in viewer coordinates.
	 * @param y
	 *            position on the z=0 plane in viewer coordinates.
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	private ConvexPolytope getSurroundingPolytopeGlobal(
			final double x,
			final double y,
			final AffineTransform3D transform,
			final int timepoint )
	{
		return getOverlappingPolytopeGlobal( x, x, y, y, transform, timepoint );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;
		final BasicStroke defaultVertexStroke = new BasicStroke();
		final BasicStroke highlightedVertexStroke = new BasicStroke( 4f );
		final BasicStroke focusedVertexStroke = new BasicStroke( 2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 8f, 3f }, 0 );
		final BasicStroke defaultEdgeStroke = new BasicStroke();
		final BasicStroke highlightedEdgeStroke = new BasicStroke( 3f );

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final double maxDepth = isFocusLimitViewRelative
				? focusLimit
				: focusLimit * Affine3DHelpers.extractScale( transform, 0 );

		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasing );

		final V target = graph.vertexRef();
		final V ref1 = graph.vertexRef();
		final V ref2 = graph.vertexRef();
		final E ref3 = graph.edgeRef();
		final double[] gPos = new double[ 3 ];
		final double[] lPos = new double[ 3 ];

		final double sliceDistanceFade = ellipsoidFadeDepth;
		final double timepointDistanceFade = 0.5;

		final ScreenVertexMath screenVertexMath = new ScreenVertexMath();
		final boolean drawPointAlways = drawPoints
				&& ( ( !drawEllipsoidSliceIntersection && !drawEllipsoidSliceProjection )
						|| drawPointsForEllipses );
		final boolean drawPointMaybe = drawPoints
				&& !drawEllipsoidSliceProjection && drawEllipsoidSliceIntersection;

		graph.getLock().readLock().lock();
		index.readLock().lock();
		try
		{
			if ( drawLinks )
			{
				final E highlighted = highlight.getHighlightedEdge( ref3 );

				graphics.setStroke( defaultEdgeStroke );

				for ( int t = Math.max( 0, currentTimepoint - timeLimit ); t < currentTimepoint; ++t )
				{
					final SpatialIndex< V > si = index.getSpatialIndex( t );
					final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
					ccp.clip( getVisiblePolytopeGlobal( transform, t ) );
					for ( final V vertex : ccp.getInsideValues() )
					{
						vertex.localize( gPos );
						transform.apply( gPos, lPos );
						final int x0 = ( int ) lPos[ 0 ];
						final int y0 = ( int ) lPos[ 1 ];
						final double z0 = lPos[ 2 ];
						for ( final E edge : vertex.outgoingEdges() )
						{
							final boolean isHighlighted = edge.equals( highlighted );

							edge.getTarget( target );
							target.localize( gPos );
							transform.apply( gPos, lPos );
							final int x1 = ( int ) lPos[ 0 ];
							final int y1 = ( int ) lPos[ 1 ];

							final double z1 = lPos[ 2 ];

							final double td0 = timeDistance( t, currentTimepoint, timeLimit );
							final double td1 = timeDistance( t + 1, currentTimepoint, timeLimit );
							final double sd0 = sliceDistance( z0, maxDepth );
							final double sd1 = sliceDistance( z1, maxDepth );

							if ( td0 > -1 )
							{
								if ( ( sd0 > -1 && sd0 < 1 ) || ( sd1 > -1 && sd1 < 1 ) )
								{
									final Color c1 = getColor( sd1, td1, sliceDistanceFade, timepointDistanceFade, selection.isSelected( edge ) );
									if ( useGradient )
									{
										final Color c0 = getColor( sd0, td0, sliceDistanceFade, timepointDistanceFade, selection.isSelected( edge ) );
										graphics.setPaint( new GradientPaint( x0, y0, c0, x1, y1, c1 ) );
									}
									else
									{
										graphics.setPaint( c1 );
									}
									if ( isHighlighted )
										graphics.setStroke( highlightedEdgeStroke );
									graphics.drawLine( x0, y0, x1, y1 );

									// Draw arrows for edge direction.
									/*
									final double dx = x1 - x0;
									final double dy = y1 - y0;
									final double alpha = Math.atan2( dy, dx );
									final double l = 5;
									final double theta = Math.PI / 6.;
									final int x1a = ( int ) Math.round( x1 - l * Math.cos( alpha - theta ) );
									final int x1b = ( int ) Math.round( x1 - l * Math.cos( alpha + theta ) );
									final int y1a = ( int ) Math.round( y1 - l * Math.sin( alpha - theta ) );
									final int y1b = ( int ) Math.round( y1 - l * Math.sin( alpha + theta ) );
									graphics.drawLine( x1, y1, x1a, y1a );
									graphics.drawLine( x1, y1, x1b, y1b );
									*/

									if ( isHighlighted )
										graphics.setStroke( defaultEdgeStroke );
								}
							}
						}
					}
				}
			}

			if ( drawSpots )
			{
				final V highlighted = highlight.getHighlightedVertex( ref1 );
				final V focused = focus.getFocusedVertex( ref2 );

				graphics.setStroke( defaultVertexStroke );
				final AffineTransform torig = graphics.getTransform();

				final SpatialIndex< V > si = index.getSpatialIndex( currentTimepoint );
				final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
				ccp.clip( getVisiblePolytopeGlobal( transform, currentTimepoint ) );
				for ( final V vertex : ccp.getInsideValues() )
				{
					final boolean isHighlighted = vertex.equals( highlighted );
					final boolean isFocused = vertex.equals( focused );

					screenVertexMath.init( vertex, transform );

					final double x = screenVertexMath.getViewPos()[ 0 ];
					final double y = screenVertexMath.getViewPos()[ 1 ];
					final double z = screenVertexMath.getViewPos()[ 2 ];
					final double sd = sliceDistance( z, maxDepth );

					if ( drawEllipsoidSliceIntersection )
					{
						if ( screenVertexMath.intersectsViewPlane() )
						{
							final Ellipse ellipse = screenVertexMath.getIntersectEllipse();

							graphics.setColor( getColor( 0, 0, ellipsoidFadeDepth, timepointDistanceFade, selection.isSelected( vertex ) ) );
							if ( isHighlighted )
								graphics.setStroke( highlightedVertexStroke );
							else if ( isFocused )
								graphics.setStroke( focusedVertexStroke );
							drawEllipse( graphics, ellipse, torig );
							if ( isHighlighted || isFocused )
								graphics.setStroke( defaultVertexStroke );

							if ( !drawEllipsoidSliceProjection && drawSpotLabels )
								drawEllipseLabel( graphics, ellipse, vertex.getLabel() );
						}
					}

					if ( sd > -1 && sd < 1 )
					{
						if ( drawEllipsoidSliceProjection )
						{
							final Ellipse ellipse = screenVertexMath.getProjectEllipse();

							graphics.setColor( getColor( sd, 0, ellipsoidFadeDepth, timepointDistanceFade, selection.isSelected( vertex ) ) );
							if ( isHighlighted )
								graphics.setStroke( highlightedVertexStroke );
							else if ( isFocused )
								graphics.setStroke( focusedVertexStroke );
							drawEllipse( graphics, ellipse, torig );
							if ( isHighlighted || isFocused )
								graphics.setStroke( defaultVertexStroke );

							if ( drawSpotLabels )
								drawEllipseLabel( graphics, ellipse, vertex.getLabel() );

							graphics.setTransform( torig );
						}

						if ( drawPointAlways || ( drawPointMaybe && !screenVertexMath.intersectsViewPlane() ) )
						{
							graphics.setColor( getColor( sd, 0, pointFadeDepth, timepointDistanceFade, selection.isSelected( vertex ) ) );
							double radius = pointRadius;
							if ( isHighlighted || isFocused )
								radius *= 2;
							final int ox = ( int ) ( x - radius );
							final int oy = ( int ) ( y - radius );
							final int ow = ( int ) ( 2 * radius );
							if ( isFocused )
								graphics.fillRect( ox, oy, ow, ow );
							else
								graphics.fillOval( ox, oy, ow, ow );
						}
					}
				}
			}
		}
		finally
		{
			graph.getLock().readLock().unlock();
			index.readLock().unlock();
		}
		graph.releaseRef( target );
		graph.releaseRef( ref1 );
		graph.releaseRef( ref2 );
	}

	static void drawEllipse( final Graphics2D graphics, final Ellipse ellipse, AffineTransform torig )
	{
		if ( torig == null )
			torig = graphics.getTransform();

		final double[] tr = ellipse.getCenter();
		final double theta = ellipse.getTheta();
		final double w = ellipse.getHalfWidth();
		final double h = ellipse.getHalfHeight();
		final Ellipse2D ellipse2D = new Ellipse2D.Double( -w, -h, 2. * w, 2. * h );

		graphics.translate( tr[ 0 ], tr[ 1 ] );
		graphics.rotate( theta );
		graphics.draw( ellipse2D );

		graphics.setTransform( torig );
	}

	// TODO: move to RenderSettings
	static final Font font = new Font( "SansSerif", Font.PLAIN, 9 );

	static void drawEllipseLabel( final Graphics2D graphics, final Ellipse ellipse, final String label )
	{
		final double[] tr = ellipse.getCenter();
		final FontRenderContext frc = graphics.getFontRenderContext();
		final TextLayout layout = new TextLayout( label, font, frc );
		final Rectangle2D bounds = layout.getBounds();
		final float tx = ( float ) ( tr[ 0 ] - bounds.getCenterX() );
		final float ty = ( float ) ( tr[ 1 ] - bounds.getCenterY() );
		layout.draw( graphics, tx, ty );
	}

	public E getEdgeAt( final int x, final int y, final double tolerance, final E ref )
	{
		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final ConvexPolytope visiblePolytopeGlobal = getVisiblePolytopeGlobal( transform, currentTimepoint );
		index.readLock().lock();
		try
		{
			final double[] gPosT = new double[ 3 ];
			final double[] lPosT = new double[ 3 ];
			final double[] gPosS = new double[ 3 ];
			final double[] lPosS = new double[ 3 ];
			final V vertexRef = graph.vertexRef();

			for ( int t = Math.max( 0, currentTimepoint - timeLimit ); t < currentTimepoint; ++t )
			{
				final SpatialIndex< V > si = index.getSpatialIndex( t );
				final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
				ccp.clip( visiblePolytopeGlobal );
				for ( final V source : ccp.getInsideValues() )
				{
					source.localize( lPosS );
					transform.apply( lPosS, gPosS );
					final double x1 = gPosS[ 0 ];
					final double y1 = gPosS[ 1 ];
					for ( final E edge : source.outgoingEdges() )
					{
						final V target = edge.getTarget( vertexRef );
						target.localize( lPosT );
						transform.apply( lPosT, gPosT );
						final double x2 = gPosT[ 0 ];
						final double y2 = gPosT[ 1 ];
						if ( Util.segmentDist( x, y, x1, y1, x2, y2 ) <= tolerance )
						{
							ref.refTo( edge );
							graph.releaseRef( vertexRef );
							return ref;
						}
					}
				}
			}
		}
		finally
		{
			index.readLock().unlock();
		}
		return null;
	}

	/**
	 * Transform viewer coordinates to global (world) coordinates.
	 *
	 * @param x
	 *            viewer X coordinate
	 * @param y
	 *            viewer Y coordinate
	 * @param gPos
	 *            receives global coordinates corresponding to viewer
	 *            coordinates <em>(x, y, 0)</em>.
	 */
	public void getGlobalPosition( final int x, final int y, final double[] gPos )
	{
		synchronized ( renderTransform )
		{
			renderTransform.applyInverse( gPos, new double[] { x, y, 0 } );
		}
	}

	/**
	 * Transform global (world) coordinates to viewer coordinates.
	 *
	 * @param gPos
	 *            global coordinates to transform.
	 * @param vPos
	 *            receives the viewer coordinates.
	 */
	public void getViewerPosition( final double[] gPos, final double[] vPos )
	{
		synchronized ( renderTransform )
		{
			renderTransform.apply( gPos, vPos );
		}
	}

	public int getCurrentTimepoint()
	{
		return renderTimepoint;
	}

	public V getVertexAt( final int x, final int y, final double tolerance, final V ref )
	{
		final AffineTransform3D transform = getRenderTransformCopy();
		final double maxDepth = isFocusLimitViewRelative
				? focusLimit
				: focusLimit * Affine3DHelpers.extractScale( transform, 0 );
		final int currentTimepoint = renderTimepoint;

		final double[] lPos = new double[] { x, y, 0 };
		final double[] gPos = new double[ 3 ];
		final ScreenVertexMath svm = new ScreenVertexMath();
		transform.applyInverse( gPos, lPos );

		boolean found = false;

		index.readLock().lock();

		// TODO: cache searches? --> take into account that indexdata might change

		if ( drawEllipsoidSliceProjection )
		{
			final ConvexPolytope cropPolytopeGlobal = getSurroundingPolytopeGlobal( x, y, transform, currentTimepoint );
			final ClipConvexPolytope< V > ccp = index.getSpatialIndex( currentTimepoint ).getClipConvexPolytope();
			ccp.clip( cropPolytopeGlobal );

			final double[] xy = new double[] { x, y };
			final double[] vPos = new double[ 3 ];
			double minDist = Double.MAX_VALUE;
			for ( final V v : ccp.getInsideValues() )
			{
				svm.init( v, transform );
				final double z = svm.getViewPos()[ 2 ];
				final double sd = sliceDistance( z, maxDepth );
				if ( sd > -1 && sd < 1 && svm.projectionContainsView( xy ) )
				{
					found = true;
					v.localize( vPos );
					final double d = LinAlgHelpers.squareDistance( vPos, gPos );
					if ( d < minDist )
					{
						minDist = d;
						ref.refTo( v );
					}
				}
			}
		}

		if ( !found && drawEllipsoidSliceIntersection )
		{
			final IncrementalNearestNeighborSearch< V > inns = index.getSpatialIndex( currentTimepoint ).getIncrementalNearestNeighborSearch();
			final double maxSquDist = graph.getMaxBoundingSphereRadiusSquared( currentTimepoint );
			inns.search( RealPoint.wrap( gPos ) );
			while ( inns.hasNext() )
			{
				final V v = inns.next();
				if ( inns.getSquareDistance() > maxSquDist )
					break;
				svm.init( v, transform );
				if ( svm.containsGlobal( gPos ) )
				{
					found = true;
					ref.refTo( v );
					break;
				}
			}
		}

		if ( !found && drawPoints )
		{
			final NearestNeighborSearch< V > nns = index.getSpatialIndex( currentTimepoint ).getNearestNeighborSearch();
			nns.search( RealPoint.wrap( gPos ) );
			final V v = nns.getSampler().get();
			if ( v != null )
			{
				svm.init( v, transform );
				final double z = svm.getViewPos()[ 2 ];
				final double sd = sliceDistance( z, maxDepth );
				if ( sd > -1 && sd < 1 )
				{
					final double[] p = svm.getViewPos();
					final double dx = p[ 0 ] - x;
					final double dy = p[ 1 ] - y;
					final double dr = pointRadius + tolerance;
					if ( dx * dx + dy * dy <= dr * dr )
					{
						found = true;
						ref.refTo( v );
					}
				}
			}
		}

		index.readLock().unlock();

		return found ? ref : null;
	}

	/**
	 * Get all vertices that would be visible with the current display settings
	 * and the specified {@code transform} and {@code timepoint}. This is used
	 * to compute {@link OverlayContext}.
	 * <p>
	 * Note, that it doesn't lock the {@link SpatioTemporalIndex}: we assumed,
	 * that this is already done by the caller.
	 * <p>
	 * TODO: The above means that the index is locked for longer than
	 * necessary.Revisit this and once it is clear how contexts are used in
	 * practice.
	 *
	 * @param transform
	 * @param timepoint
	 * @return vertices that would be visible with the current display settings
	 *         and the specified {@code transform} and {@code timepoint}.
	 */
	RefCollection< V > getVisibleVertices( final AffineTransform3D transform, final int timepoint )
	{
		final RefList< V > contextList = RefCollections.createRefList( graph.vertices() );
		final double maxDepth = isFocusLimitViewRelative
				? focusLimit
				: focusLimit * Affine3DHelpers.extractScale( transform, 0 );
		final boolean drawPointAlways = drawPoints
				&& ( ( !drawEllipsoidSliceIntersection && !drawEllipsoidSliceProjection )
						|| drawPointsForEllipses );
		final boolean drawPointMaybe = drawPoints
				&& !drawEllipsoidSliceProjection && drawEllipsoidSliceIntersection;
		final ScreenVertexMath svm = new ScreenVertexMath();

		final ClipConvexPolytope< V > ccp = index.getSpatialIndex( timepoint ).getClipConvexPolytope();
		final ConvexPolytope visiblePolytope = getVisiblePolytopeGlobal( transform, timepoint );
		ccp.clip( visiblePolytope );
		for ( final V vertex : ccp.getInsideValues() )
		{
			svm.init( vertex, transform );

			if ( drawEllipsoidSliceIntersection )
			{
				if ( svm.intersectsViewPlane()
						&& svm.intersectionIntersectsViewInterval( 0, width, 0, height ) )
				{
					contextList.add( vertex );
					continue;
				}
			}

			final double z = svm.getViewPos()[ 2 ];
			final double sd = sliceDistance( z, maxDepth );
			if ( -1 < sd && sd < 1 )
			{
				if ( drawEllipsoidSliceProjection
						&& svm.projectionIntersectsViewInterval( 0, width, 0, height ) )
				{
					contextList.add( vertex );
					continue;
				}

				if ( drawPointAlways || ( drawPointMaybe && !svm.intersectsViewPlane() ) )
				{
					final double x = svm.getViewPos()[ 0 ];
					final double y = svm.getViewPos()[ 1 ];
					if ( 0 <= x && x <= width && 0 <= y && y <= height )
						contextList.add( vertex );
				}
			}
		}

		return contextList;
	}
}
