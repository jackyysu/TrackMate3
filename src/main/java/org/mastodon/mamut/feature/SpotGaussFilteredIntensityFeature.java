package org.mastodon.mamut.feature;

import static org.mastodon.feature.FeatureProjectionKey.key;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mastodon.RefPool;
import org.mastodon.feature.Dimension;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.feature.FeatureProjectionSpec;
import org.mastodon.feature.FeatureProjections;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.Multiplicity;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.plugin.Plugin;

public class SpotGaussFilteredIntensityFeature implements Feature< Spot >
{
	public static final String KEY = "Spot gaussian-filtered intensity";

	private static final String HELP_STRING =
			"Computes the average intensity and its standard deviation inside spots "
					+ "over all sources of the dataset. "
					+ "The average is calculated by a weighted mean over the pixels of the spot, "
					+ "weighted by a gaussian centered in the spot and with a sigma value equal "
					+ "to the minimal radius of the ellipsoid divided by "
					+ SpotGaussFilteredIntensityFeatureComputer.SIGMA_FACTOR + ".";

	public static final FeatureProjectionSpec MEAN_PROJECTION_SPEC = new FeatureProjectionSpec( "Mean", Dimension.INTENSITY );

	public static final FeatureProjectionSpec STD_PROJECTION_SPEC = new FeatureProjectionSpec( "Std", Dimension.INTENSITY );

	public static final Spec SPEC = new Spec();

	@Plugin( type = FeatureSpec.class )
	public static class Spec extends FeatureSpec< SpotGaussFilteredIntensityFeature, Spot >
	{
		public Spec()
		{
			super(
					KEY,
					HELP_STRING,
					SpotGaussFilteredIntensityFeature.class,
					Spot.class,
					Multiplicity.ON_SOURCES,
					MEAN_PROJECTION_SPEC, STD_PROJECTION_SPEC );
		}
	}

	final List< DoublePropertyMap< Spot > > means;

	final List< DoublePropertyMap< Spot > > stds;

	private final Map< FeatureProjectionKey, FeatureProjection< Spot > > projectionMap;

	SpotGaussFilteredIntensityFeature( final int nSources, final RefPool< Spot > pool )
	{
		this.means = new ArrayList<>( nSources );
		this.stds = new ArrayList<>( nSources );
		this.projectionMap = new LinkedHashMap<>( 2 * nSources );
		for ( int iSource = 0; iSource < nSources; iSource++ )
		{
			/*
			 * We use property maps, so that they are automatically cleaned if
			 * an object is removed from the graph.
			 */
			final DoublePropertyMap< Spot > m = new DoublePropertyMap<>( pool, Double.NaN );
			means.add( m );
			final FeatureProjectionKey mkey = key( MEAN_PROJECTION_SPEC, iSource );
			projectionMap.put( mkey, FeatureProjections.project( mkey, m, Dimension.COUNTS_UNITS ) );

			final DoublePropertyMap< Spot > s = new DoublePropertyMap<>( pool, Double.NaN );
			stds.add( s );
			final FeatureProjectionKey skey = key( STD_PROJECTION_SPEC, iSource );
			projectionMap.put( skey, FeatureProjections.project( skey, s, Dimension.COUNTS_UNITS ) );
		}
	}

	@Override
	public FeatureProjection< Spot > project( final FeatureProjectionKey key )
	{
		return projectionMap.get( key );
	}

	@Override
	public Set< FeatureProjection< Spot > > projections()
	{
		return new LinkedHashSet<>( projectionMap.values() );
	}

	@Override
	public Spec getSpec()
	{
		return SPEC;
	}
}
