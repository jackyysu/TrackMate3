package org.mastodon.app.ui.settings.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractStyleManager< M extends AbstractStyleManager< M, S >, S extends Style< S > > implements StyleManager< M, S >
{
	protected final List< S > builtinStyles;

	protected final List< S > userStyles;

	protected S defaultStyle;

	protected AbstractStyleManager()
	{
		builtinStyles = loadBuiltinStyles();
		userStyles = new ArrayList<>();
		defaultStyle = builtinStyles.get( 0 );
	}

	// ==== abstract methods =====================================================

	protected abstract List< S > loadBuiltinStyles();

	// ===========================================================================

	@Override
	public void set( final M other )
	{
		setSnapshot( other.getSnapshot() );
	}

	@Override
	public List< S > getBuiltinStyles()
	{
		return builtinStyles;
	}

	@Override
	public List< S > getUserStyles()
	{
		return Collections.unmodifiableList( userStyles );
	}

	@Override
	public S getDefaultStyle()
	{
		return defaultStyle;
	}

	@Override
	public synchronized void setDefaultStyle( final S style )
	{
		defaultStyle = style;
	}

	@Override
	public synchronized void remove( final S style )
	{
		if ( defaultStyle.equals( style ) )
			setDefaultStyle( builtinStyles.get( 0 ) );
		userStyles.remove( style );
	}

	@Override
	public synchronized void rename( final S style, final String newName )
	{
		if ( style.getName().equals( newName ) )
			return;

		if ( nameExists( newName ) )
			throw new IllegalArgumentException( style.getClass().getSimpleName() + " \"" + newName + "\" already exists.");

		style.setName( newName );
	}

	/**
	 * Returns a copy of the specified {@code style}, making sure that
	 * the copy receives a name not already present in this manager's list of
	 * styles.
	 *
	 * @param style
	 *            the style to copy.
	 * @return a new style
	 */
	@Override
	public synchronized S duplicate( final S style )
	{
		final String name = style.getName();
		final Pattern pattern = Pattern.compile( "(.+) \\((\\d+)\\)$" );
		final Matcher matcher = pattern.matcher( name );
		int n;
		String prefix;
		if ( matcher.matches() )
		{
			final String nstr = matcher.group( 2 );
			n = Integer.parseInt( nstr );
			prefix = matcher.group( 1 );
		}
		else
		{
			n = 1;
			prefix = name;
		}

		String newName;
		do
			newName = prefix + " (" + ( ++n ) + ")";
		while ( nameExists( newName ) );

		final S newStyle = style.copy( newName );
		userStyles.add( newStyle );
		return newStyle;
	}

	protected boolean nameExists( final String name )
	{
		return styleForName( name ).isPresent();
	}

	protected Optional< S > styleForName( final String name )
	{
		return Stream.concat( builtinStyles.stream(), userStyles.stream() ).filter( style -> style.getName().equals( name ) ).findFirst();
	}

	class Snapshot
	{
		private final List< S > userStyles;

		private final String defaultStyleName;

		public Snapshot( final AbstractStyleManager< M, S > manager )
		{
			this.userStyles = manager.getUserStyles().stream().map( s -> s.copy() ).collect( Collectors.toList() );
			this.defaultStyleName = manager.getDefaultStyle().getName();
		}
	}

	synchronized Snapshot getSnapshot()
	{
		return new Snapshot( this );
	}

	synchronized void setSnapshot( final Snapshot snapshot )
	{
		userStyles.clear();
		snapshot.userStyles.forEach( s -> userStyles.add( s.copy() ) );
		setDefaultStyle( styleForName( snapshot.defaultStyleName ).orElseGet( () -> builtinStyles.get( 0 ) ) );
	}
}
