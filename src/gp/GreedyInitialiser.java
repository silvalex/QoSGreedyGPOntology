package gp;

import graph.Graph;
import nodes.ConditionalNode;
import nodes.SequenceNode;

import org.epochx.epox.Node;
import org.epochx.gp.representation.GPCandidateProgram;
import org.epochx.op.Initialiser;
import org.epochx.representation.CandidateProgram;
import org.epochx.tools.random.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates initial composition candidates by using the greedy graph creation
 * algorithm and translating the resulting graph into a tree.
 *
 * @author sawczualex
 */
public class GreedyInitialiser implements Initialiser {
	private QoSModel model;
	private RandomNumberGenerator random;

	/**
	 * Creates a new GreedyInitialiser instance.
	 *
	 * @param model
	 * @param random
	 */
	public GreedyInitialiser(QoSModel model, RandomNumberGenerator random){
		this.model = model;
		this.random = random;
	}

	@Override
	/**
	 * Generates the initial population of Web service compositions. This
	 * method is called by the framework before the evolutionary process.
	 *
	 * @return list with initial population
	 */
	public List<CandidateProgram> getInitialPopulation() {
		List<CandidateProgram> programs = new ArrayList<CandidateProgram>();

		for(int i = 0; i < model.getPopulationSize(); i++) {
			GPCandidateProgram candidate = new GPCandidateProgram(createCandidate(), model);
			programs.add(candidate);
            System.err.println("Prog: " + i);
		}

		return programs;
	}

	/**
	 * Creates a candidate using the greedy graph creation algorithm and
	 * translates it into a tree.
	 *
	 * @return root node of candidate tree
	 */
	public Node createCandidate() {
		Set<String> inputs = new HashSet<String>();
		for (String s: QoSModel.INPUT)
			inputs.add(s);

		List<Set<String>> outputs = new ArrayList<Set<String>>();
		Set<String> ifOutputs = new HashSet<String>();
		for (String s : model.OUTPUT_IF)
			ifOutputs.add(s);
		Set<String> elseOutputs = new HashSet<String>();
		for (String s : model.OUTPUT_ELSE)
			elseOutputs.add(s);

		outputs.add(elseOutputs);
		if (!ifOutputs.isEmpty())
			outputs.add(ifOutputs);

		return model.createTree(inputs, outputs, random, null);
	}
}
