package org.mastodon.mamut.feature;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.update.UpdateStackSerializer;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotUpdateStackSerializer.class )
public class SpotUpdateStackSerializer extends UpdateStackSerializer< Spot, SpotUpdateStack >
{

	@Override
	public String getFeatureKey()
	{
		return SpotUpdateStack.KEY;
	}

	@Override
	public Class< Spot > getTargetClass()
	{
		return Spot.class;
	}


	@Override
	public SpotUpdateStack deserialize( final FileIdToObjectMap< Spot > idmap, final RefCollection< Spot > pool, final String spaceUnits, final String timeUnits, final ObjectInputStream ois ) throws IOException, ClassNotFoundException
	{
		final DeserializedStruct< Spot > struct = deserializeStruct( idmap, pool, spaceUnits, timeUnits, ois );
		return new SpotUpdateStack( pool, struct.stateStack, struct.currentUpdate );
	}
}
