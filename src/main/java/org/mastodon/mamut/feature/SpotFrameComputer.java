package org.mastodon.mamut.feature;

import static org.scijava.ItemIO.OUTPUT;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotFrameComputer.class )
public class SpotFrameComputer implements MamutFeatureComputer
{

	@Parameter( type = OUTPUT )
	private SpotFrameFeature output;

	@Override
	public void createOutput()
	{
		output = new SpotFrameFeature();
	}

	@Override
	public void run()
	{
		// Nothing to do.
	}
}
