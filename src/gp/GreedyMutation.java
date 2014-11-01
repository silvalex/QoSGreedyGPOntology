package gp;

import graph.Graph;

import java.util.Set;

import nodes.InOutNode;

import org.epochx.epox.Node;
import org.epochx.gp.model.GPModel;
import org.epochx.gp.representation.GPCandidateProgram;
import org.epochx.op.Mutation;
import org.epochx.representation.CandidateProgram;
import org.epochx.tools.random.RandomNumberGenerator;

/**
 * Mutates a candidate by replacing a randomly selected
 * subtree with another with equivalent functionality.
 *
 * @author sawczualex
 */
public class GreedyMutation implements Mutation{
	private QoSModel model;
	private RandomNumberGenerator random;

	/**
	 * Creates a new GreedyMutation instance.
	 *
	 * @param model
	 * @param random
	 */
	public GreedyMutation(GPModel model, RandomNumberGenerator random) {
		this.model = (QoSModel) model;
		this.random = random;
	}

	/**
	 * Mutates a randomly selected subtree of the tree provided.
	 * A new subtree with the same functionality as the original
	 * is created using the greedy algorithm, and put in place of
	 * the original.
	 *
	 * @param program The program tree to be mutated
	 * @return The mutated candidate program
	 */
	@Override
	public CandidateProgram mutate(CandidateProgram program) {
		GPCandidateProgram p = (GPCandidateProgram) program;
		int index = random.nextInt(p.getNoTerminals() + p.getNoFunctions());

		// Node to replace
		InOutNode n = (InOutNode) p.getNthNode(index);

		Set<String> inputs = n.getInputs();
		Set<String> outputs = n.getOutputs();
		model.updateInputAndOutput(inputs, outputs);
		Graph g = null;
		ForbiddenNodes fn = new ForbiddenNodes();
		while (g == null) {
			g = model.createGraph(model.getRelevantServices(), random, fn);
		}
		// Replacement node
		Node subtree = g.nodeMap.get("Input").toTree(model.getInputs());
		model.adjustTreeOutputs(subtree, QoSModel.getOutputs());
		// Search for the node to replace with mutated subtree
		Node root = model.replaceSubtree(p.getRootNode(), (Node)n, subtree);

		return new GPCandidateProgram(root, model);
	}
}
