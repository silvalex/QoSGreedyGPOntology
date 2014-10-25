package tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import gp.QoSModel;
import gp.MyRand;
import graph.Graph;
import graph.GraphEdge;
import graph.GraphNode;
import nodes.ParallelNode;
import nodes.SequenceNode;
import nodes.ServiceNode;

import org.epochx.epox.Node;
import org.epochx.tools.random.RandomNumberGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

public class QoSModelTests {
	private static QoSModel model;
	private static GraphNode graphInput;

	@BeforeClass
	public static void setUp() {
		QoSModel.NUM_RUNS = 0;
		model = new QoSModel(null, null, "src/tests/testDataset.xml", "src/tests/testTaskSet.xml", "src/tests/testTaxonomySet.xml");
	}

	@Test
	public void testIsSubsumed() {
		// A concept should always subsume itself
		Set<String> a = new HashSet<String>();
		a.add("HomeCity");
		Set<String> b = new HashSet<String>();
		b.add("HomeCity");
		assertTrue(model.isSubsumed(a, b));

		// A more general concept should subsume a more specific concept
		a = new HashSet<String>();
		a.add("Location");
		b = new HashSet<String>();
		b.add("HomeCity");
		assertTrue(model.isSubsumed(a, b));

		// A more specific concepts should not subsume a more general concept
		a = new HashSet<String>();
		a.add("TravelArrivalDate");
		b = new HashSet<String>();
		b.add("Date");
		assertFalse(model.isSubsumed(a, b));

		// This subsumption with multiple concepts should be valid
		a = new HashSet<String>();
		a.add("Date");
		a.add("Location");
		b = new HashSet<String>();
		b.add("HomeCity");
		b.add("TravelArrivalDate");
		assertTrue(model.isSubsumed(a, b));

		// This subsumption with multiple concepts should be invalid
		a = new HashSet<String>();
		a.add("Date");
		a.add("HomeCity");
		b = new HashSet<String>();
		b.add("Location");
		b.add("TravelArrivalDate");
		assertFalse(model.isSubsumed(a, b));
	}
	@Test
	public void testGetSatisfiedInputs() {

		// All provided inputs should be satisfied
		Set<String> inputs = new HashSet<String>();
		inputs.add("ArrivalDate");
		inputs.add("DepartureDate");
		Set<String> searchSet = new HashSet<String>();
		searchSet.add("TravelArrivalDate");
		searchSet.add("TravelDepartureDate");
		Set<String> expected = new HashSet<String>();
		expected.add("DepartureDate");
		expected.add("ArrivalDate");
		Set<String> obtained = model.getSatisfiedInputs(inputs, searchSet);

		assertEquals(expected.size(), obtained.size());
		for (String e : expected) {
			assertTrue(obtained.contains(e));
		}

		// None of the provided inputs should be satisfied
		inputs = new HashSet<String>();
		inputs.add("FlightOriginCity");
		inputs.add("FlightDestinationCity");
		searchSet = new HashSet<String>();
		searchSet.add("TravelArrivalDate");
		searchSet.add("TravelDepartureDate");
		expected = new HashSet<String>(); // empty
		obtained = model.getSatisfiedInputs(inputs, searchSet);

		assertEquals(expected.size(), obtained.size());

		// Some of the provided inputs should be satisfied
		inputs = new HashSet<String>();
		inputs.add("ArrivalDate");
		inputs.add("FlightDestinationCity");
		searchSet = new HashSet<String>();
		searchSet.add("TravelArrivalDate");
		searchSet.add("TravelDepartureDate");
		expected = new HashSet<String>();
		expected.add("ArrivalDate");
		obtained = model.getSatisfiedInputs(inputs, searchSet);

		assertEquals(expected.size(), obtained.size());
		for (String e : expected) {
			assertTrue(obtained.contains(e));
		}
	}

	@Test
	public void testCreateGraph() {
		List<ServiceNode> relevantServices = model.getRelevantServices();
		Graph g = model.createGraph(relevantServices, new MyRand(333));
		String s = g.toString();

		// Ensure that graph contains input and output nodes
		assertNotEquals(g.nodeMap.get("Input"), null);
		assertNotEquals(g.nodeMap.get("Output"), null);

		// Ensure that graph contains all four expected services
		assertNotEquals(g.nodeMap.get("GenerateMapService"), null);
		assertNotEquals(g.nodeMap.get("BusService"), null);
		assertNotEquals(g.nodeMap.get("HotelFunctionsService"), null);
		assertNotEquals(g.nodeMap.get("FlightInformation"), null);

		// Check that input node is indeed the origin
		GraphNode input = g.nodeMap.get("Input");
		assertTrue(input.from.isEmpty());

		graphInput = input;

		// Check that the input node connects to three other services
		Set<String> toNames = new HashSet<String>();
		for (GraphEdge e: input.to) {
			toNames.add(e.to.node.getName());
		}
		assertEquals(3, toNames.size());
		assertTrue(toNames.contains("BusService"));
		assertTrue(toNames.contains("GenerateMapService"));
		assertTrue(toNames.contains("FlightInformation"));

		// Check that BusService is only connected to the input and to the output
		GraphNode busService = g.nodeMap.get("BusService");
		assertEquals(1, busService.from.size());
		assertEquals("Input", busService.from.get(0).from.node.getName());
		assertEquals(1, busService.to.size());
		assertEquals("Output", busService.to.get(0).to.node.getName());

		// Check that GenerateMapService is only connected to the input and to the output
		GraphNode mapService = g.nodeMap.get("GenerateMapService");
		assertEquals(1, mapService.from.size());
		assertEquals("Input", mapService.from.get(0).from.node.getName());
		assertEquals(1, mapService.to.size());
		assertEquals("Output", mapService.to.get(0).to.node.getName());

		// Check that HotelFunctionsService is only connected to FlightInformation and to the output
		GraphNode hotelService = g.nodeMap.get("HotelFunctionsService");
		assertEquals(1, hotelService.from.size());
		assertEquals("FlightInformation", hotelService.from.get(0).from.node.getName());
		assertEquals(1, hotelService.to.size());
		assertEquals("Output", hotelService.to.get(0).to.node.getName());


		// Check that FlightInformation is only connected to the input, the output, and HotelFunctionsService
		GraphNode flightService = g.nodeMap.get("FlightInformation");
		assertEquals(1, flightService.from.size());
		assertEquals("Input", flightService.from.get(0).from.node.getName());
		toNames = new HashSet<String>();
		for (GraphEdge e: flightService.to) {
			toNames.add(e.to.node.getName());
		}
		assertEquals(2, toNames.size());
		assertTrue(toNames.contains("Output"));
		assertTrue(toNames.contains("HotelFunctionsService"));

		// Check that output node is indeed the end
		GraphNode output = g.nodeMap.get("Output");
		assertTrue(output.to.isEmpty());

		// Check that the output node is connected to all other services
		Set<String> fromNames = new HashSet<String>();
		for (GraphEdge e: output.from) {
			fromNames.add(e.from.node.getName());
		}
		assertEquals(4, fromNames.size());
		assertTrue(fromNames.contains("BusService"));
		assertTrue(fromNames.contains("GenerateMapService"));
		assertTrue(fromNames.contains("FlightInformation"));
		assertTrue(fromNames.contains("HotelFunctionsService"));
	}

	@Test
	public void testToTree() {
		Node root = graphInput.toTree(model.getInputs());
		// Root must be a parallel node
		assertTrue(root instanceof ParallelNode);

		// Root must have three children
		Node[] children = root.getChildren();
		assertEquals(3, children.length);

		// Exactly one of the children must be BusService
		int occurrences = 0;
		for (Node child : children) {
			if (child.getIdentifier().equals("BusService"))
				occurrences++;
		}
		assertEquals(1, occurrences);

		// Exactly one of the children must be GenerateMapService
		occurrences = 0;
		for (Node child : children) {
			if (child.getIdentifier().equals("GenerateMapService"))
				occurrences++;
		}
		assertEquals(1, occurrences);

		// Exactly one of the children must be a SequenceNode
		occurrences = 0;
		int index = -1;
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof SequenceNode) {
				occurrences++;
				index = i;
			}
		}
		assertEquals(1, occurrences);

		/* The SequenceNode must have two children, the left one being FlightInformation
		 * and the right one being HotelFunctionsService*/
		Node sequence = children[index];
		children = sequence.getChildren();

		assertEquals(2, children.length);
		assertEquals("FlightInformation", children[0].getIdentifier());
		assertEquals("HotelFunctionsService", children[1].getIdentifier());
	}
}
