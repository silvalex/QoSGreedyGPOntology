package nodes;

import gp.Condition;
import gp.Properties;
import gp.QoSModel;

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

	public ConditionalNode() {
		this(null, null, null);
	}

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

		EvaluationResults ifRes = (EvaluationResults) getChildren()[QoSModel.COND_IF].evaluate();
		EvaluationResults elseRes = (EvaluationResults) getChildren()[QoSModel.COND_ELSE].evaluate();

		res.time = (ifRes.time * probabilities[QoSModel.COND_IF]) + (elseRes.time * probabilities[QoSModel.COND_ELSE]);
		res.cost = (ifRes.cost * probabilities[QoSModel.COND_IF]) + (elseRes.cost * probabilities[QoSModel.COND_ELSE]);
		res.reliability = (ifRes.reliability * probabilities[QoSModel.COND_IF]) + (elseRes.reliability * probabilities[QoSModel.COND_ELSE]);
		res.availability = (ifRes.availability * probabilities[QoSModel.COND_IF]) + (elseRes.availability * probabilities[QoSModel.COND_ELSE]);

		return res;
	}

	@Override
	public String getIdentifier() {
		return "CONDITIONAL";
	}

}
