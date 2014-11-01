package taxonomy;

import nodes.ServiceNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a node in the input/output taxonomy
 * used by the WSC dataset.
 *
 * @author sawczualex
 */
public class TaxonomyNode {
	public TaxonomyNode parent;
	public List<ServiceNode> services = new ArrayList<ServiceNode>();
	public String value;
	public List<TaxonomyNode> children = new ArrayList<TaxonomyNode>();

	public TaxonomyNode(String value) {
		this.value = value;
	}

	public TaxonomyNode(String value, TaxonomyNode parent) {
		this.value = value;
		this.parent = parent;
	}

	/**
	 * Gets all concepts subsumed by this node (i.e. all
	 * concepts in its subtree).
	 *
	 * @return Set of concepts
	 */
	public Set<String> getSubsumedConcepts() {
		Set<String> concepts = new HashSet<String>();
        _getSubsumedConcepts( concepts );
		return concepts;
	}

    private void _getSubsumedConcepts(Set<String> concepts) {
        concepts.add(value);
        for (TaxonomyNode child : children) {
            child._getSubsumedConcepts(concepts);
        }
    }

	/**
	 * Get services whose output is subsumed by this concept.
	 *
	 * @return Set of services
	 */
	public Set<ServiceNode> getServicesWithOutput() {
		Set<ServiceNode> serviceSet = new HashSet<ServiceNode>();
        _getServicesWithOutput(serviceSet);
		return serviceSet;
	}

    private void _getServicesWithOutput(Set<ServiceNode> serviceSet) {
        serviceSet.addAll(services);
        for (TaxonomyNode child : children) {
            child._getServicesWithOutput( serviceSet );
        }
    }

//    public boolean removeServiceFromSubtree(ServiceNode s) {
//        boolean containedInTree = false;
//
//        if (services.contains(s)) {
//            services.remove(s);
//            containedInTree = true;
//        }
//        else {
//            // Iterate down through the children
//            for (TaxonomyNode node : children) {
//                containedInTree = node.removeServiceFromSubtree(s);
//                if (containedInTree)
//                    break;
//            }
//        }
//        return containedInTree;
//    }
}
