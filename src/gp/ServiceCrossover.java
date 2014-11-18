package gp;

import java.util.List;
import java.util.Set;

import nodes.InOutNode;

import org.epochx.op.Crossover;
import org.epochx.representation.CandidateProgram;
import org.epochx.tools.random.RandomNumberGenerator;
import org.epochx.epox.Node;
import org.epochx.gp.representation.GPCandidateProgram;

/**
 * Performs the crossover of two candidate programs, ensuring that
 * the two resulting programs remain functionally correct.
 *
 * @author sawczualex
 */
public class ServiceCrossover implements Crossover {
	private QoSModel model;
	private RandomNumberGenerator random;

	/**
	 * Creates a new ServiceCrossover instance.
	 *
	 * @param model
	 * @param random
	 */
	public ServiceCrossover(QoSModel model, RandomNumberGenerator random){
		this.model = model;
		this.random = random;
	}

	@Override
	/**
	 * Performs the crossover operation for two programs. A given tree depth
	 * within the bounds of both programs is randomly selected, and this method
	 * attempts to swap a subtree from one program with one of the subtrees from
	 * the other. The swap only takes place if the inputs and outputs of the two
	 * subtrees are equivalent.
	 *
	 * @param program1
	 * @param program2
	 * @return resulting programs
	 */
	public CandidateProgram[] crossover(CandidateProgram program1, CandidateProgram program2) {
		// Retrieve a random depth for performing crossover (must be within the bounds for both programs)
		GPCandidateProgram p1 = (GPCandidateProgram) program1;
		GPCandidateProgram p2 = (GPCandidateProgram) program2;
		int depth = Math.min(p1.getProgramDepth(), p2.getProgramDepth());
		depth = random.nextInt(depth + 1);

		// Retrieve nodes at the given depth for both programs
		List<Node>p1Nodes = p1.getNodesAtDepth(depth);
		List<Node>p2Nodes = p2.getNodesAtDepth(depth);

		// Randomly select a node from the first program
		Node node1 = p1Nodes.get(random.nextInt(p1Nodes.size()));
		Set<String> inputs1 = ((InOutNode)node1).getInputs();
		List<Set<String>> outputs1 = ((InOutNode)node1).getOutputs();

		// Try to find an equivalent node in the other program
		Node equivalent = null;
		for (Node node2 : p2Nodes) {
			Set<String> inputs2 = ((InOutNode)node2).getInputs();
			List<Set<String>> outputs2 = ((InOutNode)node2).getOutputs();

			if (inputs1.size() == inputs2.size() && outputs1.size() == outputs2.size() &&
					inputs1.containsAll(inputs2)) {
				boolean allOutputsMatch = true;
				for (int i = 0; i < outputs1.size(); i++) {
					if (!outputs1.get(i).containsAll(outputs2.get(i))) {
						allOutputsMatch = false;
						break;
					}
				}
				if (allOutputsMatch) {
					equivalent = node2;
					break;
				}
			}
		}

		// If there is an equivalent node, perform crossover
		if (equivalent != null) {
			Node newRoot1 = model.replaceSubtree(p1.getRootNode(), node1, equivalent);
			Node newRoot2 = model.replaceSubtree(p2.getRootNode(), equivalent, node1);
			if (newRoot1.toString().contains("SEQUENCE (serv2 serv5) SEQUENCE (serv2 serv5)") ||
					newRoot2.toString().contains("SEQUENCE (serv2 serv5) SEQUENCE (serv2 serv5)")) {
				System.out.println();
			}
			return new CandidateProgram[]{new GPCandidateProgram(newRoot1, model), new GPCandidateProgram(newRoot2, model)};
		}
		else {
			// Return unchanged programs
			return new CandidateProgram[]{program1, program2};
		}
	}
}
