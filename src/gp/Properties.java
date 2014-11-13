package gp;

import java.util.List;
import java.util.Set;

/**
 * Holds the properties of a Web service: required inputs,
 * produced outputs, and QoS measures.
 *
 * @author sawczualex
 */
public class Properties {
	private Set<String> inputs;
	private List<List<String>> outputPossibilities;
	private List<Float> probabilities;
	private double[] qos = new double[4];

	/**
	 * Creates a new Properties instance.
	 *
	 * @param inputs
	 * @param outputs
	 * @param qos
	 */
	public Properties(Set<String> inputs, List<List<String>> outputPossibilities, List<Float> probabilities, double[] qos) {
		this.inputs = inputs;
		this.outputPossibilities = outputPossibilities;
		this.probabilities = probabilities;
		this.qos = qos;
	}

	/**
	 * Returns the inputs in this set of properties.
	 *
	 * @return inputs
	 */
	public Set<String> getInputs() {
		return inputs;
	}

	/**
	 * Returns the output possibilities in this set of properties.
	 *
	 * @return outputPossibilities
	 */
	public List<List<String>> getOutputPossibilities() {
		return outputPossibilities;
	}

	public List<Float> getProbabilities() {
		return probabilities;
	}

	/**
	 * Returns the QoS measures in this set of properties.
	 *
	 * @return qos
	 */
	public double[] getQoS() {
		return qos;
	}
}
