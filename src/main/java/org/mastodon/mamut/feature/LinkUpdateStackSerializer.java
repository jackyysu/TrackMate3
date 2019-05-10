package org.mastodon.mamut.feature;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.update.UpdateStackSerializer;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.revised.model.mamut.Link;
import org.scijava.plugin.Plugin;

@Plugin( type = LinkUpdateStackSerializer.class )
public class LinkUpdateStackSerializer extends UpdateStackSerializer< Link, LinkUpdateStack >
{

	@Override
	public String getFeatureKey()
	{
		return LinkUpdateStack.KEY;
	}

	@Override
	public Class< Link > getTargetClass()
	{
		return Link.class;
	}

	@Override
	public LinkUpdateStack deserialize( final FileIdToObjectMap< Link > idmap, final RefCollection< Link > pool, final String spaceUnits, final String timeUnits, final ObjectInputStream ois ) throws IOException, ClassNotFoundException
	{
		final DeserializedStruct< Link > struct = deserializeStruct( idmap, pool, spaceUnits, timeUnits, ois );
		return new LinkUpdateStack( pool, struct.stateStack, struct.currentUpdate );
	}
}
