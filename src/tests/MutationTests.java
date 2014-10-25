package tests;

import static org.junit.Assert.*;
import gp.GreedyInitialiser;
import gp.GreedyMutation;
import gp.MyRand;
import gp.QoSModel;
import nodes.InOutNode;

import org.epochx.gp.representation.GPCandidateProgram;
import org.epochx.representation.CandidateProgram;
import org.junit.BeforeClass;
import org.junit.Test;

public class MutationTests {
	private static QoSModel model;

	@BeforeClass
	public static void setUp() {
		QoSModel.NUM_RUNS = 0;
		model = new QoSModel(null, null, "services-output.xml", "problem.xml", "taxonomy.xml");
	}

	@Test
	public void testMutation() {
		MyRand rand = new MyRand(333);
		GreedyInitialiser init = new GreedyInitialiser(model, rand);
		GPCandidateProgram solution = (GPCandidateProgram) init.getInitialPopulation().get(2);

		GreedyMutation mut = new GreedyMutation(model, rand);

		for (int i = 0; i < 10; i++) {
			solution = (GPCandidateProgram) mut.mutate(solution);
			InOutNode root = (InOutNode) solution.getRootNode();
			System.out.println("Number: " + i + ", Terminals: " + solution.getNoTerminals() + ", Input: " + root.getInputs() + ", Output: " + root.getOutputs());
		}
		// If execution does not stop, mutation works
		assertTrue(true);
	}

}
