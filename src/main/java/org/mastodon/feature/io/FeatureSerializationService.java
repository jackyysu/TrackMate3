package org.mastodon.feature.io;

import org.mastodon.feature.Feature;
import org.scijava.service.SciJavaService;

public interface FeatureSerializationService extends SciJavaService
{

	public < O, F extends Feature< O > > FeatureSerializer< O, F > serializerFor( F feature );

	public < O, F extends Feature< O > > FeatureSerializer< O, F > serializerFor( String featureKey );

}
