package nodes;

import gp.Condition;
import gp.Properties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.epochx.epox.Node;

public class ConditionalNode extends Node implements InOutNode {
	private Condition condition;
	private float[] probabilities;
	private Set<String> inputs;
	private List<Set<String>> outputs;

	/**
	 * Creates an empty ParallelNode instance.
	 */
	public ConditionalNode(Condition condition) {
		this(condition, null, null);
	}

	/**
	 * Creates a new ParallelNode instance containing the
	 * children provided.
	 *
	 * @param child1
	 * @param child2
	 */
	public ConditionalNode(Condition condition, final Node child1, final Node child2) {
		super(child1, child2);
		this.condition = condition;
		inputs = new HashSet<String>();
		outputs = new ArrayList<Set<String>>();
	}

	public void setProbabilities(float[] probabilities) {
		this.probabilities = probabilities;
	}

	public float[] getProbabilities() {
		return probabilities;
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
		builder.append(String.format("CONDITIONAL{%s=%s} (", condition.general, condition.specific));

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
	public List<Set<String>> getOutputs() {
		return outputs;
	}

	@Override
	public Object evaluate() {
		EvaluationResults res = new EvaluationResults();

		EvaluationResults ifRes = (EvaluationResults) getChildren()[0].evaluate();
		EvaluationResults elseRes = (EvaluationResults) getChildren()[1].evaluate();

		res.time = (ifRes.time * probabilities[0]) + (elseRes.time * probabilities[1]);
		res.cost = (ifRes.cost * probabilities[0]) + (elseRes.cost * probabilities[1]);
		res.reliability = (ifRes.reliability * probabilities[0]) + (elseRes.reliability * probabilities[1]);
		res.availability = (ifRes.availability * probabilities[0]) + (elseRes.availability * probabilities[1]);

		return res;
	}

	@Override
	public String getIdentifier() {
		return "CONDITIONAL";
	}

}
