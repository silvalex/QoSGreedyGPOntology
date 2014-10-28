package gp;

import graph.GraphNode;

import java.util.List;

import org.epochx.epox.Node;

/**
 * Tuple class for returning the result of selecting the next service
 * (either a service -- if there is one which can fulfil an input --,
 * or no service and a path -- the path indicates a connection already exists
 * between these two nodes).
 * @author Alex
 */
public class PickServiceResult {
    public Node chosen;
    public List<GraphNode> path;
    
    public PickServiceResult(Node chosen, List<GraphNode> path) {
        this.chosen = chosen;
        this.path = path;
    }
}
