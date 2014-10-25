package gp;

import java.util.Set;

/**
 * Holds the properties of a Web service: required inputs,
 * produced outputs, and QoS measures.
 *
 * @author sawczualex
 */
public class Properties {
	private Set<String> inputs;
	private Set<String> outputs;
	private double[] qos = new double[4];

	/**
	 * Creates a new Properties instance.
	 *
	 * @param inputs
	 * @param outputs
	 * @param qos
	 */
	public Properties(Set<String> inputs, Set<String> outputs, double[] qos) {
		this.inputs = inputs;
		this.outputs = outputs;
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
	 * Returns the outputs in this set of properties.
	 *
	 * @return outputs
	 */
	public Set<String> getOutputs() {
		return outputs;
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
