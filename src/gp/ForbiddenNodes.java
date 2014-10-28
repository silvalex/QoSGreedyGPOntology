package gp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodes.ServiceNode;

/**
 * Data structure to hold which nodes should not be used to
 * fulfil inputs.
 * 
 * @author Alex
 */
public class ForbiddenNodes {
    private Map<ServiceNode, List<ServiceNode>> forbiddenMap = new HashMap<ServiceNode, List<ServiceNode>>();
    
    public List<ServiceNode> getForbiddenNodes(ServiceNode n) {
        return forbiddenMap.get( n );
    }
    
    public void addForbiddenNode(ServiceNode n, ServiceNode forbidden) {
        List<ServiceNode> list = getForbiddenNodes(n);
        if (list == null) {
            list = new ArrayList<ServiceNode>();
            forbiddenMap.put(n, list);
        }
        list.add( forbidden );
    }
    
}
