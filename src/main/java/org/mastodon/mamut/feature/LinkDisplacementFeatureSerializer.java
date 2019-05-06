package org.mastodon.mamut.feature;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.Dimension;
import org.mastodon.feature.io.FeatureSerializer;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.io.ObjectToFileIdMap;
import org.mastodon.io.properties.DoublePropertyMapSerializer;
import org.mastodon.properties.DoublePropertyMap;
import org.mastodon.revised.model.mamut.Link;
import org.scijava.plugin.Plugin;

@Plugin( type = LinkDisplacementFeatureSerializer.class )
public class LinkDisplacementFeatureSerializer implements FeatureSerializer< Link, LinkDisplacementFeature >
{

	@Override
	public String getFeatureKey()
	{
		return LinkDisplacementFeature.SPEC.getKey();
	}

	@Override
	public Class< Link > getTargetClass()
	{
		return Link.class;
	}

	@Override
	public void serialize( final LinkDisplacementFeature feature, final ObjectToFileIdMap< Link > idmap, final ObjectOutputStream oos ) throws IOException
	{
		final DoublePropertyMapSerializer< Link > propertyMapSerializer = new DoublePropertyMapSerializer<>( feature.map );
		propertyMapSerializer.writePropertyMap( idmap, oos );
	}

	@Override
	public LinkDisplacementFeature deserialize( final FileIdToObjectMap< Link > idmap, final RefCollection< Link > pool, final String spaceUnits, final String timeUnits, final ObjectInputStream ois ) throws IOException, ClassNotFoundException
	{
		final DoublePropertyMap< Link > map = new DoublePropertyMap<>( pool, Double.NaN );
		final DoublePropertyMapSerializer< Link > propertyMapSerializer = new DoublePropertyMapSerializer<>( map );
		propertyMapSerializer.readPropertyMap( idmap, ois );
		final String units = Dimension.LENGTH.getUnits( spaceUnits, timeUnits );
		return new LinkDisplacementFeature( map, units );
	}
}
