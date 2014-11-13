package nodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gp.Properties;

import org.epochx.epox.Node;

import static gp.QoSModel.TIME;

/**
 * A ServiceNode is a leaf node representing an
 * atomic Web service from a repository.
 *
 * @author sawczualex
 */
public class ServiceNode extends Node implements InOutNode {
	private String name;
	private Set<String> inputs;
	private Set<String> outputs;
	private List<List<String>> outputPossibilities;
	private List<Float> probabilities;
	private double[] qos;

	/**
	 * Creates a new ServiceNode with the given name
	 * and properties.
	 *
	 * @param name
	 * @param p
	 */
	public ServiceNode(String name, Properties p) {
		this.name = name;
		inputs = p.getInputs();
		outputPossibilities = p.getOutputPossibilities();
		probabilities = p.getProbabilities();
		qos = p.getQoS();
	}

	/**
	 * Gets this ServiceNode's name.
	 *
	 * @return name
	 */
	public String getName() {
		return name;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public Set<String> getInputs() {
		return inputs;
	}

	/**
	 * Sets this node's inputs as the
	 * provided argument.
	 *
	 * @param inputs
	 */
	public void setInputs(Set<String> inputs) {
		this.inputs = inputs;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public Set<String> getOutputs() {
		return outputs;
	}

	/**
	 * Sets this node's outputs as the
	 * provided argument.
	 *
	 * @param outputs
	 */
	public void setOutputs(Set<String> outputs) {
		this.outputs = outputs;
	}

	public List<List<String>> getOutputPossibilities() {
		return outputPossibilities;
	}

	/**
	 * Gets this ServiceNode's QoS attributes.
	 *
	 * @return qos
	 */
	public double[] getQos(){
		return qos;
	}

	@Override
	/**
	 * Returns this service's name and execution time.
	 *
	 * @return evaluation results
	 */
	public Object evaluate() {
		double longestTime = qos[TIME];
		Set<String> servicesInTree = new HashSet<String>();
		servicesInTree.add(name);

		return new EvaluationResults(longestTime, servicesInTree);
	}

	@Override
	/**
	 * Returns a String with which to identify this node.
	 *
	 * @return string
	 */
	public String getIdentifier() {
		return name;
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
		// No inputs should be provided for a terminal
		if (inputTypes.length != 0) {
			throw new IllegalArgumentException("variables have no input types");
		}
		return this.getClass();
	}

	@Override
	/**
	 * Produces a replica of this ServiceNode with the same
	 * input, output, and QoS attributes.
	 *
	 * @return clone
	 */
	public ServiceNode clone() {
		Properties p = new Properties(inputs, outputPossibilities, probabilities, qos);
		return new ServiceNode(name, p);
	}

	@Override
	/**
	 * Returns a nested String representation of this node.
	 *
	 * @return string
	 */
	public String toString() {
		return String.format("%s", name);
	}
}
