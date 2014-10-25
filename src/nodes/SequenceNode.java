package nodes;

import java.util.HashSet;
import java.util.Set;

import gp.Properties;

import org.epochx.epox.Node;

/**
 * A SequenceNode indicates that its children are executed
 * sequentially in the Web service composition, from the leftmost
 * child (index 0) to the rightmost child.
 *
 * @author sawczualex
 */
public class SequenceNode extends Node implements InOutNode {
	private Set<String> inputs;
	private Set<String> outputs;

	/**
	 * Creates an empty SequenceNode instance.
	 */
	public SequenceNode() {
		this(null, null);
	}

	/**
	 * Creates a new SequenceNode instance containing the
	 * children provided.
	 *
	 * @param child1
	 * @param child2
	 */
	public SequenceNode(final Node child1, final Node child2) {
		super(child1, child2);
		inputs = new HashSet<String>();
		outputs = new HashSet<String>();
	}

	@Override
	/**
	 * Returns the execution time and the set of the
	 * names of the services in this sequence tree.
	 *
	 * @return evaluation results
	 */
	public Object evaluate() {
		double longestTime = 0.0;
		Set<String> servicesInTree = new HashSet<String>();

		for (Node child : getChildren()) {
			EvaluationResults results = (EvaluationResults) child.evaluate();
			longestTime += results.longestTime;
			servicesInTree.addAll(results.servicesInTree);
		}

		return new EvaluationResults(longestTime, servicesInTree);
	}

	@Override
	/**
	 * Returns a String with which to identify this node type.
	 *
	 * @return string
	 */
	public String getIdentifier() {
		return "SEQUENCE";
	}

	@Override
	/**
	 * This method is used by the EpochX framework to
	 * determine whether this node is terminal.
	 *
	 * @param inputTypes
	 * @return class
	 */
	public Class<?> getReturnType(final Class<?> ... inputTypes) {
		Properties p = new Properties(null, null, null);
		return p.getClass();
	}

	@Override
	/**
	 * Returns a nested String representation of this node.
	 *
	 * @return string
	 */
	public String toString() {
		return String.format("SEQUENCE (%s %s)", getChildren()[0].toString(), getChildren()[1].toString());
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public Set<String> getInputs() {
		return inputs;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public Set<String> getOutputs() {
		return outputs;
	}
}