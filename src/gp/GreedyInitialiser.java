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
		// Create subtree from input to condition type
		Set<String> inputs = new HashSet<String>();
		for (String s: QoSModel.INPUT)
			inputs.add(s);

		Set<String> outputs = new HashSet<String>();
		outputs.add(QoSModel.condition.general);

		model.updateInputAndOutput(inputs, outputs);

		Graph initialGraph = null;
		while (initialGraph == null) {
			initialGraph = model.createGraph(model.getRelevantServices(model.getServices(), inputs, outputs), random);
		}
        Node initialTree = initialGraph.nodeMap.get("Input").toTree(model.getInputs());
        Set<String> correctionSet = new HashSet<String>();
        correctionSet.add(QoSModel.condition.general);
        model.adjustTreeOutputs(initialTree, correctionSet);

        // Create subtree from the condition's specific value to if-output
        inputs = new HashSet<String>();
        outputs = new HashSet<String>();
        inputs.add(QoSModel.condition.specific);

		for (String s: QoSModel.OUTPUT_IF)
			outputs.add(s);

		model.updateInputAndOutput(inputs, outputs);

		Graph ifGraph = null;
		while (ifGraph == null) {
			ifGraph = model.createGraph(model.getRelevantServices(model.getServices(), inputs, outputs), random);
		}
        Node ifTree = ifGraph.nodeMap.get("Input").toTree(model.getInputs());
        correctionSet = new HashSet<String>();
        for (String s : QoSModel.OUTPUT_IF)
        	correctionSet.add(s);
        model.adjustTreeOutputs(ifTree, correctionSet);

        // Create subtree from the condition's general value to else-output
        inputs = new HashSet<String>();
        outputs = new HashSet<String>();
        inputs.add(QoSModel.condition.general);

		for (String s: QoSModel.OUTPUT_ELSE)
			outputs.add(s);

		model.updateInputAndOutput(inputs, outputs);

		Graph elseGraph = null;
		while (elseGraph == null) {
			elseGraph = model.createGraph(model.getRelevantServices(model.getServices(), inputs, outputs), random);
		}
        Node elseTree = elseGraph.nodeMap.get("Input").toTree(model.getInputs());
        correctionSet = new HashSet<String>();
        for (String s : QoSModel.OUTPUT_ELSE)
        	correctionSet.add(s);
        model.adjustTreeOutputs(elseTree, correctionSet);

        // Assemble conditional part of the tree
        // XXX: Assumption that there is only one node that produces the output for the condition,
        // and that this node has probabilities that match the specific/general alternatives of condition.

        ConditionalNode conditionalTree = new ConditionalNode(QoSModel.condition, initialGraph.nodeMap.get("Output").from.get(0).from.node);
        conditionalTree.setChild(0, ifTree);
        conditionalTree.setChild(1, elseTree);

        conditionalTree.getInputs().clear();
        conditionalTree.getInputs().add(QoSModel.condition.general);

        Set<String> ifOutputs = new HashSet<String>();
        for (String s: QoSModel.OUTPUT_IF)
        	ifOutputs.add(s);
        Set<String> elseOutputs = new HashSet<String>();
        for (String s: QoSModel.OUTPUT_ELSE)
        	elseOutputs.add(s);

        conditionalTree.getOutputs().add(ifOutputs);
        conditionalTree.getOutputs().add(elseOutputs);

        // Assemble overall tree
        SequenceNode tree = new SequenceNode();
        tree.setChild(0, initialTree);
        tree.setChild(1, conditionalTree);
        tree.getInputs().clear();
        for (String s: QoSModel.INPUT)
        	tree.getInputs().add(s);

        tree.getOutputs().add(ifOutputs);
        tree.getOutputs().add(elseOutputs);
		return tree;
	}
}
