package net.trackmate.util.mempool;

/**
 * TODO: implement
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class MultiArrayPool< T extends MappedElement, A extends MappedElementArray< T > > extends Pool< T >
{
	public MultiArrayPool( final int capacity, final int bytesPerElement )
	{
		super( capacity, bytesPerElement );
		// TODO Auto-generated constructor stub
	}

	@Override
	public T createAccess()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateAccess( final T access, final int index )
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected int append()
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
