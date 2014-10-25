package taxonomy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodes.ServiceNode;

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
		concepts.add(value);

		for (TaxonomyNode child : children)
			concepts.addAll(child.getSubsumedConcepts());

		return concepts;
	}

	/**
	 * Get services whose output is subsumed by this concept.
	 *
	 * @return Set of services
	 */
	public Set<ServiceNode> getServicesWithOutput() {
		Set<ServiceNode> serviceSet = new HashSet<ServiceNode>();

		serviceSet.addAll(services);

		for (TaxonomyNode child : children)
			serviceSet.addAll(child.getServicesWithOutput());

		return serviceSet;
	}
}
