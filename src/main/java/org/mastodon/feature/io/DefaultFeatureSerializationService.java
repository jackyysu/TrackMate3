package org.mastodon.feature.io;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.feature.Feature;
import org.scijava.InstantiableException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

@Plugin( type = FeatureSerializationService.class )
public class DefaultFeatureSerializationService extends AbstractService implements FeatureSerializationService
{

	@Parameter
	private PluginService plugins;

	private final Map< String, FeatureSerializer< ?, ? > > serializers = new HashMap<>();

	@Override
	public void initialize()
	{
		serializers.clear();
		discover();
	}

	private void discover()
	{
		@SuppressWarnings( "rawtypes" )
		final List< PluginInfo< FeatureSerializer > > infos = plugins.getPluginsOfType( FeatureSerializer.class );
		for ( @SuppressWarnings( "rawtypes" )
		final PluginInfo< FeatureSerializer > info : infos )
		{
			try
			{
				@SuppressWarnings( "rawtypes" )
				final FeatureSerializer fs = info.createInstance();
				final String featureKey = fs.getFeatureKey();
				serializers.put( featureKey, fs );
			}
			catch ( final InstantiableException e )
			{
				/*
				 * TODO: instead of printing the messages, they should be
				 * collected into one big message that can then be obtained from
				 * the Service and presented to the user in some way (the
				 * "presenting to the user" part we can decide on later...).
				 */
				System.out.println( e.getMessage() );
			}
		}
	}

	@Override
	public < O, F extends Feature< O > > FeatureSerializer< O, F > serializerFor( final F feature )
	{
		@SuppressWarnings( "unchecked" )
		final FeatureSerializer< O, F > serializer = ( FeatureSerializer< O, F > ) serializerFor( feature.getSpec().getKey() );
		return serializer;
	}

	@Override
	public < O, F extends Feature< O > > FeatureSerializer< O, F > serializerFor( final String featureKey )
	{
		@SuppressWarnings( "unchecked" )
		final FeatureSerializer< O, F > serializer = ( FeatureSerializer< O, F > ) serializers.get( featureKey );
		return serializer;
	}
}
