package org.mastodon.revised.model.mamut;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.io.FeatureSerializationService;
import org.mastodon.feature.io.FeatureSerializer;
import org.mastodon.feature.update.GraphUpdateStack;
import org.mastodon.feature.update.GraphUpdateStackSerialization;
import org.mastodon.graph.io.RawGraphIO.FileIdToGraphMap;
import org.mastodon.graph.io.RawGraphIO.GraphToFileIdMap;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.io.ObjectToFileIdMap;
import org.mastodon.mamut.feature.MamutFeatureComputerService;
import org.mastodon.project.MamutProject.ProjectReader;
import org.mastodon.project.MamutProject.ProjectWriter;
import org.scijava.Context;

public class MamutRawFeatureModelIO
{

	public static void serialize(
			final Context context,
			final FeatureModel featureModel,
			final GraphToFileIdMap< Spot, Link > idmap,
			final ProjectWriter writer )
			throws IOException
	{
		final MamutFeatureComputerService computerService = context.getService( MamutFeatureComputerService.class );
		final GraphUpdateStack< Spot, Link > graphUpdateStack = computerService.getGraphUpdateStack();
		try (
				final OutputStream fos = writer.getFeatureOutputStream( "GraphUpdateStack" );
				final ObjectOutputStream oos = new ObjectOutputStream( new BufferedOutputStream( fos, 1024 * 1024 ) ))
		{
			GraphUpdateStackSerialization.serialize( graphUpdateStack, idmap, oos );
		}

		final FeatureSerializationService featureSerializationService = context.getService( FeatureSerializationService.class );
		for ( final FeatureSpec< ?, ? > spec : featureModel.getFeatureSpecs() )
		{
			final Feature< ? > rawFeature = featureModel.getFeature( spec );
			final FeatureSerializer< ?, ? > rawSerializer = featureSerializationService.serializerFor( rawFeature );
			if ( null == rawSerializer )
				continue;

			final Class< ? > specTargetClass = spec.getTargetClass();
			if ( specTargetClass == Spot.class )
				write( rawFeature, rawSerializer, idmap.vertices(), writer );
			else if ( specTargetClass == Link.class )
				write( rawFeature, rawSerializer, idmap.edges(), writer );
			else
				System.err.println( "Do not know how to serialize a feature that targets " + specTargetClass );
		}
	}

	public static void deserialize(
			final Context context,
			final Model model,
			final FileIdToGraphMap< Spot, Link > idmap,
			final ProjectReader reader ) throws ClassNotFoundException, IOException
	{
		final MamutFeatureComputerService computerService = context.getService( MamutFeatureComputerService.class );
		try (
				final InputStream fis = reader.getFeatureInputStream( "GraphUpdateStack" );
				final ObjectInputStream ois = new ObjectInputStream( new BufferedInputStream( fis, 1024 * 1024 ) ))
		{
			final GraphUpdateStack< Spot, Link > graphUpdateStack = GraphUpdateStackSerialization.deserialize( model.getGraph(), idmap, ois );
			computerService.setGraphUpdateStack( graphUpdateStack );
		}
		catch ( final FileNotFoundException fnfe )
		{
			// Do nothing special.
		}

		final FeatureSerializationService featureSerializationService = context.getService( FeatureSerializationService.class );
		final Collection< String > featureKeys = reader.getFeatureKeys();
		final FeatureModel featureModel = model.getFeatureModel();
		featureModel.pauseListeners();
		featureModel.clear();
		for ( final String featureKey : featureKeys )
		{
			@SuppressWarnings( "rawtypes" )
			final FeatureSerializer serializer = featureSerializationService.serializerFor( featureKey );
			if ( null == serializer )
			{
				System.err.println( "Do not know how to deserialize the feature with key: " + featureKey );
				continue;
			}

			final Class< ? > targetClass = serializer.getTargetClass();
			@SuppressWarnings( "rawtypes" )
			Feature feature;
			if ( targetClass == Spot.class )
				feature = read(
						serializer,
						idmap.vertices(),
						model.getGraph().vertices(),
						model.getSpaceUnits(),
						model.getTimeUnits(),
						reader );
			else if ( targetClass == Link.class )
				feature = read(
						serializer,
						idmap.edges(),
						model.getGraph().edges(),
						model.getSpaceUnits(),
						model.getTimeUnits(),
						reader );
			else
			{
				System.err.println( "Do not know how to deserialize a feature that targets " + targetClass );
				continue;
			}
			featureModel.declareFeature( feature );
		}
		featureModel.resumeListeners();
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private static void write( final Feature< ? > rawFeature, final FeatureSerializer< ?, ? > rawSerializer, final ObjectToFileIdMap< ? > idmap, final ProjectWriter writer ) throws IOException
	{
		final Feature feature = rawFeature;
		final FeatureSerializer serializer = rawSerializer;

		try (
				final OutputStream fos = writer.getFeatureOutputStream( rawFeature.getSpec().getKey() );
				final ObjectOutputStream oos = new ObjectOutputStream( new BufferedOutputStream( fos, 1024 * 1024 ) ))
		{
			serializer.serialize( feature, idmap, oos );
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private static Feature read( final FeatureSerializer< ?, ? > rawSerializer, final FileIdToObjectMap< ? > idmap, final RefCollection< ? > pool, final String spaceUnits, final String timeUnits, final ProjectReader reader ) throws IOException, ClassNotFoundException
	{
		final FeatureSerializer serializer = rawSerializer;
		try (
				final InputStream fis = reader.getFeatureInputStream( serializer.getFeatureKey() );
				final ObjectInputStream ois = new ObjectInputStream( new BufferedInputStream( fis, 1024 * 1024 ) ))
		{
			return serializer.deserialize( idmap, pool, spaceUnits, timeUnits, ois );
		}
	}
}
