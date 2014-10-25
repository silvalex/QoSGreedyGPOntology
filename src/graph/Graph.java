package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a graph showing the input-output relationships
 * of the services in a composition.
 *
 * @author sawczualex
 */
public class Graph {
	public Map<String, GraphNode> nodeMap;
	public List<GraphEdge> edges;

	/**
	 * Creates a new Graph instance.
	 */
	public Graph() {
		nodeMap = new HashMap<String, GraphNode>();
		edges = new ArrayList<GraphEdge>();
	};

	@Override
	/**
	 * Provides a string representation of this graph, showing
	 * the two services that are connected by each edge.
	 *
	 * @return string
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String suffix = ", ";
		for (int i = 0; i < edges.size(); i++) {
			if (i == edges.size() - 1)
				suffix = "";
			builder.append(String.format("%s%s", edges.get(i).toString(), suffix));
		}
		return builder.toString();
	}
}
