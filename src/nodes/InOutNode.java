package nodes;

import java.util.Set;

/**
 * This interface indicates a tree node that has knowledge
 * of its required inputs and produced outputs.
 *
 * @author sawczualex
 */
public interface InOutNode {
	/**
	 * Gets this node's set of required inputs.
	 *
	 * @return inputs
	 */
	Set<String> getInputs();

	/**
	 * Gets this node's set of produced outputs.
	 *
	 * @return outputs
	 */
	Set<String> getOutputs();
}
