package org.mastodon.tomancak;

import org.mastodon.graph.ref.AbstractEdge;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.tomancak.MatchingGraph.MatchingEdgePool;

public class MatchingEdge extends AbstractEdge< MatchingEdge, MatchingVertex, MatchingEdgePool, ByteMappedElement >
{
	MatchingEdge( final MatchingEdgePool pool )
	{
		super( pool );
	}
}
