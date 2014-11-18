package graph;

import gp.QoSModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodes.InOutNode;
import nodes.ParallelNode;
import nodes.SequenceNode;
import nodes.ServiceNode;

import org.epochx.epox.Node;

/**
 * Represents a node in the service graph.
 *
 * @author sawczualex
 */
public class GraphNode {
    public QoSModel model;
	public ServiceNode node;
	public List<GraphEdge> from = new ArrayList<GraphEdge>();
	public List<GraphEdge> to = new ArrayList<GraphEdge>();

	/**
	 * Creates a new GraphNode instance that envelops
	 * the ServiceNode provided.
	 *
	 * @param node
	 */
	public GraphNode(ServiceNode node, QoSModel model) {
		this.node = node;
		this.model = model;
	}

	@Override
	/**
	 * Verifies whether this GraphNode is equal to
	 * another object.
	 *
	 * @return true if they are equal, false otherwise
	 */
	public boolean equals(Object other) {
		if (other == null || !(other instanceof GraphNode))
			return false;
		else {
			GraphNode o = (GraphNode) other;
			return node.equals(o.node);
		}
	}

	/**
	 * Returns this node's name.
	 *
	 * @return name
	 */
	public String getName() {
		return node.getIdentifier();
	}

	/**
	 * Return this node's set of inputs.
	 *
	 * @return inputs
	 */
	public Set<String> getInputs() {
		return node.getInputs();
	}

	/**
	 * Return this node's set of outputs.
	 *
	 * @return outputs
	 */
	public Set<String> getOutputs() {
		return node.getOutputs().get(0);
	}

	/**
	 * Indirectly recursive method that transforms this GraphNode
	 * and all nodes that directly or indirectly receive its
	 * output into a tree representation.
	 *
	 * @return Tree root
	 */
	public Node toTree(Set<String> parentInput) {
		Node root = null;
		if (node.getName().equals("Input")) {
			// Start with sequence
			if (to.size() == 1) {
				/*
				 * If the next node points to the output, this is a
				 * single-service composition, so return a service node
				 */
				GraphEdge next = to.get(0);
				root = getNode(next.to, parentInput);
			}
			// Start with parallel node
			else if (to.size() > 1)
				root = createParallelNode(this, to, parentInput);
		} else {
			// Begin by checking how many nodes are in the right child.
			Node rightChild;

			List<GraphEdge> children = new ArrayList<GraphEdge>(to);

			// Find the output node in the list, if it is contained there
			GraphEdge outputEdge = null;
			Set<String> o = new HashSet<String>();
			for (GraphEdge ch : children) {
				if (ch.to.getName().equals("Output")) {
					outputEdge = ch;
					o = ch.overlap;
					break;
				}
			}
			// Remove the output node from the children list
			children.remove(outputEdge);

			// If there is only one other child, create a sequence construct
			if (children.size() == 1) {
				rightChild = getNode(children.get(0).to, parentInput);
				root = createSequenceNode(node, rightChild, o, parentInput);
			}
			// Else, create a new parallel construct wrapped in a sequence construct
			else {
				rightChild = createParallelNode(this, children, parentInput);
				root = createSequenceNode(node, rightChild, o, parentInput);
			}

		}

		// Go down the tree once again adjusting outputs
		return root;
	}

	/**
	 * Verify whether the GraphNode provided translates into a
	 * leaf node when converting the graph into a tree.
	 *
	 * @param node
	 * @return True if it translates into a leaf node,
	 * false otherwise
	 */
	private boolean isLeaf(GraphNode node) {
		return node.to.size() == 1 && node.to.get(0).to.getName().equals("Output");
	}

	/**
	 * Represents a GraphNode with multiple outgoing edges as a ParallelNode in
	 * the tree. The children of this node are explicitly provided as a list.
	 *
	 * @param n
	 * @param childrenGraphNodes
	 * @return parallel node
	 */
	private Node createParallelNode(GraphNode n, List<GraphEdge> childrenGraphNodes, Set<String> parentInput) {
		Node root = new ParallelNode();
		Set<String> inputs = ((InOutNode) root).getInputs();
		List<Set<String>> outputs = ((InOutNode) root).getOutputs();
		if (outputs.size() == 0)
			outputs.add(new HashSet<String>(0));

		inputs.addAll(n.node.getInputs());

		// Create subtrees for children
		int length = childrenGraphNodes.size();
		Node[] children = new Node[length];

		for (int i = 0; i < length; i++) {
			GraphEdge child = childrenGraphNodes.get(i);
			children[i] = getNode(child.to, parentInput);
			//inputs.addAll(child.overlap);
			inputs.addAll(((InOutNode)children[i]).getInputs());
			outputs.get(0).addAll(((InOutNode)children[i]).getOutputs().get(0));
		}
		root.setChildren(children);

		return root;
	}

	/**
	 * Represents a GraphNode with a single outgoing edge as a SequenceNode in
	 * the tree (edges to the Output node are not counted). The left and right
	 * children of this node are provided as arguments. If the GraphNode also
	 * has an outgoing edge to the Output (i.e. the left child also contributes
	 * with its output to the overall sequence outputs), its values should be
	 * provided as the additionalOutput argument.
	 *
	 * @param leftChild
	 * @param rightChild
	 * @param additionalOutput
	 * @param parentInput
	 * @return sequence node
	 */
	private Node createSequenceNode(Node leftChild, Node rightChild, Set<String> additionalOutput, Set<String> parentInput) {
		if (additionalOutput == null)
			additionalOutput = new HashSet<String>();

		SequenceNode root = new SequenceNode(leftChild, rightChild);
		Set<String> inputs = ((InOutNode) root).getInputs();
		List<Set<String>> outputs = ((InOutNode) root).getOutputs();
		if (outputs.size() == 0)
			outputs.add(new HashSet<String>());

		inputs.addAll(((InOutNode)leftChild).getInputs());
		inputs.addAll(parentInput);

//		for (GraphEdge e : from) {
//			inputs.addAll(e.overlap);
//		}

		outputs.get(0).addAll(((InOutNode)rightChild).getOutputs().get(0));
		outputs.get(0).addAll(additionalOutput);

		return root;
	}

	/**
	 * Retrieves the tree representation for the provided GraphNode,
	 * also checking if should translate to a leaf.
	 *
	 * @param n
	 * @return root of tree translation
	 */
	private Node getNode(GraphNode n, Set<String> parentInput) {
		Node result;
		if (isLeaf(n))
			result = n.node;
		// Otherwise, make next node's subtree the right child
		else
			result = n.toTree(parentInput);
		return result;
	}

	@Override
	/**
	 * Returns a String representation of this
	 * GraphNode.
	 *
	 * @return string
	 */
	public String toString() {
		return node.toString();
	}
}
