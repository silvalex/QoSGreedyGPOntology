package graph;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an edge in the service
 * graph.
 *
 * @author sawczualex
 */
public class GraphEdge {
	public GraphNode from;
	public GraphNode to;
	public Set<String> overlap = new HashSet<String>();

	/**
	 * Creates a new GraphEdge instance.
	 *
	 * @param overlap
	 */
	public GraphEdge(Set<String> overlap) {
		this.overlap = overlap;
	}

	@Override
	/**
	 * Shows a String represetation of the two services
	 * connected by this edge.
	 *
	 * @return string
	 */
	public String toString() {
		return String.format("%s --> %s", from.toString(), to.toString());
	}
}
