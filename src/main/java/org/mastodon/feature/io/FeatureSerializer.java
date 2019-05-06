package org.mastodon.feature.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.mastodon.collection.RefCollection;
import org.mastodon.feature.Feature;
import org.mastodon.io.FileIdToObjectMap;
import org.mastodon.io.ObjectToFileIdMap;
import org.scijava.plugin.SciJavaPlugin;

public interface FeatureSerializer< O, F extends Feature< O > > extends SciJavaPlugin
{

	/**
	 * Returns the key of the feature this {@link FeatureSerializer} can
	 * de/serialize.
	 *
	 * @return the key of the feature this {@link FeatureSerializer} can
	 *         de/serialize.
	 */
	public String getFeatureKey();

	/**
	 * Returns the target class of the feature this serializer can de/serialize.
	 *
	 * @return the target class.
	 */
	public Class< O > getTargetClass();

	/**
	 * Serializes the feature to the specified output stream.
	 *
	 * @param feature
	 *            the feature to serialize.
	 * @param idmap
	 *            the {@link ObjectToFileIdMap}.
	 * @param oos
	 *            the output stream.
	 * @throws IOException
	 */
	public void serialize( F feature, ObjectToFileIdMap< O > idmap, ObjectOutputStream oos ) throws IOException;

	/**
	 * Deserializes a feature from the specified input stream.
	 *
	 * @param idmap
	 *            the {@link FileIdToObjectMap}.
	 * @param pool
	 *            the {@link RefCollection} used to create property maps inside
	 *            the feature.
	 * @param spaceUnits
	 *            space units used to set feature units.
	 * @param timeUnits
	 *            time units used to set feature units.
	 * @param ois
	 *            the input stream.
	 * @return a new feature instance.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public F deserialize( FileIdToObjectMap< O > idmap, RefCollection< O > pool, String spaceUnits, String timeUnits, ObjectInputStream ois ) throws IOException, ClassNotFoundException;

}
