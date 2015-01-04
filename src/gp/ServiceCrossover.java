package gp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodes.InOutNode;
import nodes.ServiceNode;

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

//	@Override
//	/**
//	 * Performs the crossover operation for two programs. A given tree depth
//	 * within the bounds of both programs is randomly selected, and this method
//	 * attempts to swap a subtree from one program with one of the subtrees from
//	 * the other. The swap only takes place if the inputs and outputs of the two
//	 * subtrees are equivalent.
//	 *
//	 * @param program1
//	 * @param program2
//	 * @return resulting programs
//	 */
//	public CandidateProgram[] crossover(CandidateProgram program1, CandidateProgram program2) {
//		// Retrieve a random depth for performing crossover (must be within the bounds for both programs)
//		GPCandidateProgram p1 = (GPCandidateProgram) program1;
//		GPCandidateProgram p2 = (GPCandidateProgram) program2;
//		int depth = Math.min(p1.getProgramDepth(), p2.getProgramDepth());
//		depth = random.nextInt(depth + 1);
//
//		// Retrieve nodes at the given depth for both programs
//		List<Node>p1Nodes = p1.getNodesAtDepth(depth);
//		List<Node>p2Nodes = p2.getNodesAtDepth(depth);
//
//		// Randomly select a node from the first program
//		Node node1 = p1Nodes.get(random.nextInt(p1Nodes.size()));
//		Set<String> inputs1 = ((InOutNode)node1).getInputs();
//		List<Set<String>> outputs1 = ((InOutNode)node1).getOutputs();
//
//		// Try to find an equivalent node in the other program
//		Node equivalent = null;
//		for (Node node2 : p2Nodes) {
//			Set<String> inputs2 = ((InOutNode)node2).getInputs();
//			List<Set<String>> outputs2 = ((InOutNode)node2).getOutputs();
//
//			if (inputs1.size() == inputs2.size() && outputs1.size() == outputs2.size() &&
//					inputs1.containsAll(inputs2)) {
//				boolean allOutputsMatch = true;
//				for (int i = 0; i < outputs1.size(); i++) {
//					if (!outputs1.get(i).containsAll(outputs2.get(i))) {
//						allOutputsMatch = false;
//						break;
//					}
//				}

//			}
//		}
//
//		// If there is an equivalent node, perform crossover
//		if (equivalent != null) {
//			Node newRoot1 = model.replaceSubtree(p1.getRootNode(), node1, equivalent);
//			Node newRoot2 = model.replaceSubtree(p2.getRootNode(), equivalent, node1);

//			return new CandidateProgram[]{new GPCandidateProgram(newRoot1, model), new GPCandidateProgram(newRoot2, model)};
//		}
//		else {
//			// Return unchanged programs
//			return new CandidateProgram[]{program1, program2};
//		}
//	}

//	@Override
	public CandidateProgram[] crossover(CandidateProgram program1, CandidateProgram program2) {
		CandidateProgram[] result = new CandidateProgram[2];
		GPCandidateProgram p1 = (GPCandidateProgram) program1;
		GPCandidateProgram p2 = (GPCandidateProgram) program2;
		Set<ServiceNode> leafNodes1 = new HashSet<ServiceNode>();
		Set<ServiceNode> leafNodes2 = new HashSet<ServiceNode>();
		getLeafNodes(leafNodes1, p1.getRootNode());
		getLeafNodes(leafNodes2, p2.getRootNode());

		ServiceNode selected1 = null;
		ServiceNode selected2 = null;

		for(ServiceNode s1 : leafNodes1) {
			for(ServiceNode s2 : leafNodes2) {
				if (!s1.getName().equals(s2.getName())) {
					if (s1.getInputs().size() == s2.getInputs().size() && s1.getOutputs().size() == s2.getOutputs().size() &&
							s1.getInputs().containsAll(s2.getInputs())) {
						boolean allOutputsMatch = true;
						for (int i = 0; i < s1.getOutputs().size(); i++) {
							if (!s1.getOutputs().get(i).containsAll(s2.getOutputs().get(i))) {
								allOutputsMatch = false;
								break;
							}
						}
						if (allOutputsMatch) {
							selected1 = s1;
							selected2 = s2;
							break;
						}
					}
				}
			}
		}

		if (selected1 == null || selected2 == null) {
			result[0] = p1;
			result[1] = p2;
			return result;
		}
		else {
			// Replace services in both trees
			Node newRoot1 = model.replaceServicesInTree(p1.getRootNode(), selected1, selected2);
			Node newRoot2 = model.replaceServicesInTree(p2.getRootNode(), selected2, selected1);

			result[0] = new GPCandidateProgram(newRoot1, model);
			result[1] = new GPCandidateProgram(newRoot2, model);
			return result;
		}
	}

	private void getLeafNodes(Set<ServiceNode> current, Node tree) {
		if (tree instanceof ServiceNode) {
			current.add((ServiceNode) tree);
		}
		else {
			for (Node child : tree.getChildren())
				getLeafNodes(current, child);
		}
	}

}
