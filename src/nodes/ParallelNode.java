package nodes;

import java.util.HashSet;
import java.util.Set;

import gp.Properties;

import org.epochx.epox.Node;

/**
 * A ParallelNode indicates that its children are executed in
 * parallel in the Web service composition.
 *
 * @author sawczualex
 */
public class ParallelNode extends Node implements InOutNode {
	private Set<String> inputs;
	private Set<String> outputs;

	/**
	 * Creates an empty ParallelNode instance.
	 */
	public ParallelNode() {
		this(null, null);
	}

	/**
	 * Creates a new ParallelNode instance containing the
	 * children provided.
	 *
	 * @param child1
	 * @param child2
	 */
	public ParallelNode(final Node child1, final Node child2) {
		super(child1, child2);
		inputs = new HashSet<String>();
		outputs = new HashSet<String>();
	}

	@Override
	/**
	 * Returns the branch with the longest execution time
	 * and the set of the names of the services in this
	 * parallel tree.
	 *
	 * @return evaluation results
	 */
	public Object evaluate() {
		double longestTime = -1.0;
		Set<String> servicesInTree = new HashSet<String>();

		for (Node child : getChildren()) {
			EvaluationResults results = (EvaluationResults) child.evaluate();
			if (results.longestTime > longestTime)
				longestTime = results.longestTime;
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
		return "PARALLEL";
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
		Properties p = new Properties(null, null, null, null);
		return p.getClass();
	}

	@Override
	/**
	 * Returns a nested String representation of this node.
	 *
	 * @return string
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PARALLEL (");

		for (int i = 0; i < getChildren().length; i++) {
			builder.append(getChildren()[i].toString());
			if (i != getChildren().length - 1)
				builder.append(" ");
		}
		builder.append(")");
		return builder.toString();
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
	 * {@InheritDoc}
	 */
	public Set<String> getOutputs() {
		return outputs;
	}
}
