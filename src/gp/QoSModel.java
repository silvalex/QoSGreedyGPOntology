package gp;

import graph.Graph;
import graph.GraphEdge;
import graph.GraphNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nodes.EvaluationResults;
import nodes.ParallelNode;
import nodes.SequenceNode;
import nodes.ServiceNode;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.epochx.epox.Node;
import org.epochx.gp.model.GPModel;
import org.epochx.gp.representation.GPCandidateProgram;
import org.epochx.life.GenerationAdapter;
import org.epochx.life.Life;
import org.epochx.op.selection.TournamentSelector;
import org.epochx.representation.CandidateProgram;
import org.epochx.stats.StatField;
import org.epochx.stats.Stats;
import org.epochx.tools.random.RandomNumberGenerator;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Arrays;
import org.perf4j.LogParser;
import org.perf4j.StopWatch;
import org.perf4j.helpers.GroupedTimingStatisticsTextFormatter;
import org.perf4j.log4j.Log4JStopWatch;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import taxonomy.TaxonomyNode;

/**
 * Model implementation for performing tree-based Web
 * service composition. Evolutionary parameters, tasks,
 * and available Web services are all set within this task.
 *
 * @author sawczualex
 */
public class QoSModel extends GPModel {
	private static int removedNodes = 0;
    // Logging
    private Logger _logger;
    private PrintStream stdout = System.out;
    private static FileAppender _appender;
    private static String _logFileName;
    private static String _statLogFileName;
    private static final Level SETUP = new CustomLevel("SETUP");
    private static final Level RUN = new CustomLevel("RUN");
    private static final Level DETAILS = new CustomLevel("DETAILS");
    private static final Level POSTRUN = new CustomLevel("POSTRUN");

	// Constants with of order of QoS attributes
	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;

	// Node data structures for composition generation
	private Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	private List<ServiceNode> relevantServices;
//	private Map<String, List<Node>> outputMap = new HashMap<String, List<Node>>();
	private Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();

	// Fitness function weights
    private static final double w1 = 0.25;
    private static final double w2 = 0.25;
    private static final double w3 = 0.25;
    private static final double w4 = 0.25;
	public static final int POPULATION = 30;
	public static final int MAX_NUM_ITERATIONS = 50;
	public static final int MAX_INIT_DEPTH = -1;
	public static final int MAX_DEPTH = -1;
	public static final int NO_ELITES = 1;
	public static final double CROSSOVER_PROB = 0.9;
	public static final double MUTATION_PROB = 0.1;

    /* Available inputs, required outputs, and the dummy service
     * nodes representing them. */
    private static Set<String> availableInputs;
    private static Set<String> requiredOutputs;
    private ServiceNode inputNode;
    private ServiceNode outputNode;

    // Variables for normalisation
	private double _totalTime = 0.0;
	private double _totalCost = 0.0;
	private boolean normaliseTotals = true;

	// Run settings
	private static String _servFilename = "services-output.xml";
	private static String _taskFilename = "problem.xml";
	private static String _taxonomyFilename = "taxonomy.xml";

	public static String[] INPUT;
	public static String[] OUTPUT;

	public static int NUM_RUNS = 50;
	public static int numServices;
	private static final int SEED_COEFFICIENT = 3130;

	/**
	 * Sets up logging for this session.
	 */
	public void setupLogging() {
		try {
			_logger = Logger.getLogger(QoSModel.class);
			SimpleLayout layout = new SimpleLayout();
			_appender = new FileAppender(layout,_logFileName+".txt",false);
			_logger.addAppender(_appender);
			_logger.setLevel(Level.ALL);
			System.setOut(createLoggingProxy(System.out));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a PrintStream that logs all of its writes to the log. In the context of this program,
	 * this PrintStream is used to intercept the information being sent to System.out and log it using
	 * Log4J.
	 *
	 * @param realPrintStream
	 * @return print stream
	 */
    public PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
        	private int count = 1;
        	private String s = "";
        	@Override
    		public void write(byte buf[], int off, int len) {
    			if (count < 6) {
    				s += String.format("%s", StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(buf, off, len)));
    				count++;
    			}
    			else {
	    	        try {
	    	        	_logger.log(DETAILS, s);
	    	        	s = "";
	    	        	count = 1;
	    	        }
	    	        catch (Exception e) {
	    	       }
    			}
    	    }
//    		@Override
//    		public void println(String s) {
//    			_logger.log(DETAILS, s);
//    		}
        };
    }

	/**
	 * Creates a new QoSModel, setting the terminal and
	 * function nodes for the tree as well as the composition
	 * task and crossover/mutation probabilities.
	 */
	public QoSModel(String logName, String statLogName, String dataset, String taskSet, String taxonomySet) {
		if (dataset != null)
			_servFilename = dataset;
		if (taskSet != null)
			_taskFilename = taskSet;
		if (taxonomySet != null)
			_taxonomyFilename = taxonomySet;

		//String dateTime
		if (logName != null && statLogName != null) {
			_logFileName = logName;
			_statLogFileName = statLogName;
		}
		// Create your own names
		else {
			String timeDate = new SimpleDateFormat("_dd-MM-yyyy_HH-mm-ss").format(Calendar.getInstance().getTime());
			_logFileName = "gp" + timeDate;
			_statLogFileName = "gpStats" + timeDate;
		}
		setupLogging();

		parseWSCServiceFile(_servFilename);
		parseWSCTaskFile(_taskFilename);
		parseWSCTaxonomyFile(_taxonomyFilename);
		findConceptsForInstances();

    	inputNode = new ServiceNode("Input", new Properties(new HashSet<String>(), availableInputs, new double[]{0,0,1,1}));
    	outputNode = new ServiceNode("Output", new Properties(requiredOutputs, new HashSet<String>(), new double[]{0,0,1,1}));

    	populateOutputsInTree();

        // Set terminals and function nodes
		List<Node> syntax = new ArrayList<Node>();

		// Function nodes
		syntax.add(new SequenceNode());
		syntax.add(new ParallelNode());

		relevantServices = getRelevantServices(serviceMap, availableInputs, requiredOutputs);
		eliminateCycleCausingServices(); // XXX

		// Terminal nodes
		for (ServiceNode s : relevantServices) {
			syntax.add(s);
		}


        _logger.log(SETUP, "Filename: " + _servFilename);
        _logger.log(SETUP, "NumServices: " + numServices);
        _logger.log(SETUP, "TaskInput: " + Arrays.toString(INPUT));
        _logger.log(SETUP, "TaskOutput: " + Arrays.toString(OUTPUT));
        _logger.log(SETUP, "NumRuns: " + NUM_RUNS);
        _logger.log(SETUP, "populationSize: " + POPULATION);
        _logger.log(SETUP, "MaxNumIterations: " + MAX_NUM_ITERATIONS);
        _logger.log(SETUP, "MaxInitialDepth: " + MAX_INIT_DEPTH);
        _logger.log(SETUP, "MaxDepth: " + MAX_DEPTH);
        _logger.log(SETUP, "CrossoverProb: " + CROSSOVER_PROB);
        _logger.log(SETUP, "MutationProb: " + MUTATION_PROB);
        _logger.log(SETUP, "NoElites: " + NO_ELITES);
        _logger.log(SETUP, "fitness_W1: " + w1);
        _logger.log(SETUP, "fitness_W2: " + w2);
        _logger.log(SETUP, "fitness_W3: " + w3);
        _logger.log(SETUP, "fitness_W4: " + w4);

		setSyntax(syntax);
        setCrossoverProbability(CROSSOVER_PROB);
        setMutationProbability(MUTATION_PROB);

        // Set parameters
		setPopulationSize(POPULATION);
		setNoGenerations(MAX_NUM_ITERATIONS);
		setMaxInitialDepth(MAX_INIT_DEPTH);
		setMaxDepth(MAX_DEPTH);
		setCrossoverProbability(CROSSOVER_PROB);
		setMutationProbability(MUTATION_PROB);
		setNoElites(NO_ELITES);

        // Request statistics every generation
        Life.get().addGenerationListener(new GenerationAdapter(){
            @Override
            public void onGenerationEnd() {
            	Stats.get().print(StatField.GEN_NUMBER, StatField.GEN_FITNESS_MIN, StatField.GEN_FITTEST_PROGRAM);
            }
        });

        double bestOverallFitness = Double.POSITIVE_INFINITY;
        GPCandidateProgram bestOverallProgram = null;

        for (int i = 0; i < NUM_RUNS; i++) {

			long seed = i * SEED_COEFFICIENT;
        	setRNG(new MyRand(seed));

        	// Set operators and components
        	setInitialiser(new GreedyInitialiser(this, getRNG()));
        	setProgramSelector(new TournamentSelector(this, 7));
        	setCrossover(new ServiceCrossover(this, getRNG()));
        	setMutation(new GreedyMutation(this, getRNG()));

			_logger.log(RUN, "Run: " + i);
			_logger.log(RUN, "Seed: " + seed);

			StopWatch runWatch = new Log4JStopWatch("RunTime",_logger, RUN);
			run();
			runWatch.stop();

			double fitness = (Double) Stats.get().getStat(StatField.GEN_FITNESS_MIN);
			GPCandidateProgram program = (GPCandidateProgram) Stats.get().getStat(StatField.GEN_FITTEST_PROGRAM);

			if (fitness < bestOverallFitness) {
				bestOverallFitness = fitness;
				bestOverallProgram = program;
			}

			_logger.log(RUN, "BestFitness: " + fitness);
			_logger.log(RUN, "BestProgram: " + program);
			reset();
        }
		_logger.log(POSTRUN, "BestOverallFitness: " + bestOverallFitness);
		_logger.log(POSTRUN, "BestOverallProgram: " + bestOverallProgram);
		_logger.log(POSTRUN, "RemovedNodes: " + removedNodes);
		_generateStatistics();
		_logger.removeAppender(_appender);
		_appender.close();
		System.setOut(stdout);
	}

	public int getRemovedNodes() {
		return removedNodes;
	}

	@Override
	/**
	 * Calculates the fitness for the given program. The fitness function
	 * ranges from 0 to 1, with 0 representing the best possible fitness.
	 *
	 * @param program
	 * @return fitness
	 */
	public double getFitness(CandidateProgram program) {

        final GPCandidateProgram p = (GPCandidateProgram) program;
        EvaluationResults results = (EvaluationResults) p.evaluate();

        double T = results.longestTime;
        double C = 0;
        double A = 1;
        double R = 1;

        for (String name : results.servicesInTree) {
        	ServiceNode service = serviceMap.get(name);

        	if (service != null) {
        		double[] qos = service.getQos();

        		C += qos[COST];
        		A *= qos[AVAILABILITY];
        		R *= qos[RELIABILITY];
        	}
        }

        if (normaliseTotals) {
	        // Normalise C and T (values between [0,1]) using the sums of all values from services
	        // that could be possibly in the composition.
			T = T/_totalTime;
			C = C/_totalCost;
        }

		return (w1 * (1 - A) + w2 * (1 - R) + w3 * T + w4 * C);
	}

    @Override
	/**
	 * This method is used by the EpochX framework.
	 *
	 * @return class
	 */
    public Class<?> getReturnType() {
        Properties p = new Properties(null, null, null);
        return p.getClass();
    }

    /**
     * Returns the map of all Web services currently loaded.
     *
     * @return serviceMap
     */
	public Map<String, ServiceNode> getServices() {
		return serviceMap;
	}

	/**
	 * Returns the composition task's available inputs.
	 *
	 * @return availableInputs
	 */
	public static Set<String> getInputs() {
		return availableInputs;
	}

	/**
	 * Returns the composition task's required outputs.
	 *
	 * @return requiredOutputs
	 */
	public static Set<String> getOutputs() {
		return requiredOutputs;
	}

	/**
	 * Goes through the service list and retrieves only those services which
	 * could be part of the composition task requested by the user.
	 *
	 * @param serviceMap
	 * @return relevant services
	 */
	private List<ServiceNode> getRelevantServices(Map<String,ServiceNode> serviceMap, Set<String> inputs, Set<String> outputs) {
		// If we are counting total time and total cost from scratch, reset them
		if (normaliseTotals) {
			_totalCost = 0.0;
			_totalTime = 0.0;
		}

		// Copy service map values to retain original
		Collection<ServiceNode> services = new ArrayList<ServiceNode>(serviceMap.values());

		Set<String> cSearch = new HashSet<String>(inputs);
		List<ServiceNode> sList = new ArrayList<ServiceNode>();
		Set<ServiceNode> sFound = discoverService(services, cSearch);
		while (!sFound.isEmpty()) {
			sList.addAll(sFound);
			services.removeAll(sFound);
			for (ServiceNode s: sFound) {
				cSearch.addAll(s.getOutputs());
				if (normaliseTotals) {
					_totalCost += s.getQos()[COST];
					_totalTime += s.getQos()[TIME];
				}
			}
			sFound.clear();
			sFound = discoverService(services, cSearch);
		}

		if (isSubsumed(outputs, cSearch)) {
			return sList;
		}
		else {
			String message = "It is impossible to perform a composition using the services and settings provided.";
			System.out.println(message);
			System.exit(0);
			return null;
		}
	}

	private void eliminateCycleCausingServices() {
		Set<ServiceNode> excluded = new HashSet<ServiceNode>();

		Graph g = new Graph();
		GraphNode output = getGraphNode(outputNode.getName(), g);

		Queue<GraphNode> queue = new LinkedList<GraphNode>();
		queue.offer(output);

		Set<GraphNode> visited = new HashSet<GraphNode>();

		while(!queue.isEmpty()) {
			GraphNode current = queue.poll();
			if (!visited.contains(current) && !current.getName().equals("Input")) {
				visited.add(current);
				for (String input : current.getInputs()) {
					// Get services whose output can fulfil this input
					List<ServiceNode> sList = new ArrayList<ServiceNode>(taxonomyMap.get(input).getServicesWithOutput());
					for (ServiceNode s: sList) {
						if (s != current.node) {
							GraphNode gNode = g.nodeMap.get(s.getName());
							if(gNode == null) {
								addEdgeToGraph(g, gNode, current, s, queue);
							}
							else {
								// If there is a path, exclude this service from consideration
								if (hasPath(current, gNode)) {
									excluded.add(gNode.node);
								}
								else {
									addEdgeToGraph(g, gNode, current, s, queue);
								}
							}
						}
					}
				}
			}
		}

		for (ServiceNode s: excluded) {
			relevantServices.remove(s);
			// Remove from taxonomy

			for (String o: s.getOutputs())
				taxonomyMap.get(o).services.remove(s);
		}
	}

	private void addEdgeToGraph(Graph g, GraphNode gNode, GraphNode current, ServiceNode s, Queue<GraphNode> queue) {
		// Add to graph
		gNode = getGraphNode(s.getName(), g);
		// Add to queue
		queue.offer(gNode);
		// Create connecting edges
		GraphEdge e = new GraphEdge(null);
		e.from = gNode;
		e.to = current;
		gNode.to.add(e);
		current.from.add(e);
		g.edges.add(e);
	}

	/**
	 * Retrieves the list of services that can possibly
	 * be used in the current composition.
	 *
	 * @return relevantServices
	 */
	public List<ServiceNode> getRelevantServices() {
		return relevantServices;
	}

	/**
	 * Replaces a subtree in the provided tree with a replacement
	 * node.
	 *
	 * @param root Tree root
	 * @param toReplace The subtree to be removed
	 * @param replacement The subtree to be added in
	 * @return Updated tree
	 */
	public Node replaceSubtree(Node root, Node toReplace, Node replacement) {
		// If root is the node to be replaced
		if (root == toReplace) {
			return replacement;
		}
		// Else try to replace the children of this node, if any
		else {
			Node[] children = root.getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i] = replaceSubtree(children[i], toReplace, replacement);
			}
			return root;
		}
	}

	/**
	 * Discovers all services from the provided collection whose
	 * input can be satisfied either (a) by the input provided in
	 * searchSet or (b) by the output of services whose input is
	 * satisfied by searchSet (or a combination of (a) and (b)).
	 *
	 * @param services
	 * @param searchSet
	 * @return set of discovered services
	 */
	private Set<ServiceNode> discoverService(Collection<ServiceNode> services, Set<String> searchSet) {
		Set<ServiceNode> found = new HashSet<ServiceNode>();
		for (ServiceNode s: services) {
			if (isSubsumed(s.getInputs(), searchSet))
				found.add(s);
		}
		return found;
	}

	/**
	 * Checks whether set of inputs can be completely satisfied by the search
	 * set, making sure to check descendants of input concepts for the subsumption.
	 *
	 * @param inputs
	 * @param searchSet
	 * @return true if search set subsumed by input set, false otherwise.
	 */
	public boolean isSubsumed(Set<String> inputs, Set<String> searchSet) {
		boolean satisfied = true;
		for (String input : inputs) {
			Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
			if (doIntersection(searchSet, subsumed).isEmpty()) {
				satisfied = false;
				break;
			}
		}
		return satisfied;
	}

	/**
	 * Checks which inputs provided can be satisfied using the search set, according
	 * to the principle of subsumption. A list of the satisfied inputs is returned.
	 *
	 * @param inputs
	 * @param searchSet
	 * @return satisfied inputs
	 */
	public Set<String> getSatisfiedInputs(Set<String> inputs, Set<String> searchSet) {
		Set<String> satisfied = new HashSet<String>();
		for (String input : inputs) {
			Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
			if (!doIntersection(searchSet, subsumed).isEmpty())
				satisfied.add(input);
		}
		return satisfied;
	}

	/**
	 * Executes a greedy algorithm to create a graph representing a
	 * functionally correct, non-cyclic representation of a web service
	 * composition.
	 *
	 * @param services The services that are allowed to be included
	 * in the graph
	 * @param random
	 * @return graph (null if there was a problem building the graph)
	 */
	public Graph createGraph(List<ServiceNode> services, RandomNumberGenerator random) {
		Graph graph = new Graph();

		// Queue for services to be matched
		Queue<GraphNode> queue = new LinkedList<GraphNode>();
		GraphNode end = new GraphNode(outputNode);
		queue.offer(end);
		graph.nodeMap.put(end.getName(), end);

		// Set to record visits
		Set<String> visited = new HashSet<String>();

		Set<String> sInput;
		// While there are more services to connect

		while(!queue.isEmpty()) {
			// Poll next from queue
			GraphNode nodeTo = queue.poll();

				if (!visited.contains(nodeTo.getName())) {
					visited.add(nodeTo.getName());

				// Retrieve graph node (adds node to graph if not already there)
				sInput = new HashSet<String>(nodeTo.getInputs());

				// While all of its inputs haven't been matched
				while (!sInput.isEmpty()) {
					// Pick a random service
					Node sn = pickRandomService(nodeTo, graph, relevantServices, sInput, random);

					// If there are no options to continue building this graph
					if (sn == null){
						// Abort
						System.out.println("Abort");
						return null;
					}

					GraphNode nodeFrom = getGraphNode(sn.getIdentifier(), graph);
					if (nodeFrom == null || nodeFrom.equals(nodeTo))
						return null;
					else {
						// Check if its output feeds the input of the service we want to connect
						Set<String> intersect = getSatisfiedInputs(sInput, new HashSet<String>(nodeFrom.getOutputs()));

						/* If it does, remove output that is fed from list of needed inputs,
						/* add connection to particle, and add service to queue (if it is not the
						/* start service) */
						if (!intersect.isEmpty()) {
							sInput.removeAll(intersect);

							// Create graph connections
							GraphEdge edge = new GraphEdge(intersect);
							graph.edges.add(edge);
							edge.to = nodeTo;
							edge.from = nodeFrom;
							nodeTo.from.add(edge);
							nodeFrom.to.add(edge);

							// Put service in queue if it has not already been visited
							if (!nodeFrom.getName().equals(inputNode.getName()) && !visited.contains(nodeFrom.getName())) {
								queue.offer(nodeFrom);
							}
						}
					}
				}
			}
		}
		return graph;
	}

	/**
	 * Checks whether there is a path between two graph nodes
	 * in a DAG, using a basic depth-first traversal.
	 *
	 * @param origin
	 * @param destination
	 * @return true if there is a path, false otherwise
	 */
	private boolean hasPath(GraphNode origin, GraphNode destination) {
		/* The end service in the graph is never the origin of any edges,
		 * and the start service is never the destination.*/
		if (origin.node.getName().equals(outputNode.getName()) || destination.node.getName().equals(inputNode.getName()))
			return false;

		Queue<GraphNode> queue = new LinkedList<GraphNode>();
		queue.offer(origin);

		while(!queue.isEmpty()) {
			GraphNode current = queue.poll();
			if (current == destination) {
				return true;
			}
			else {
				for (GraphEdge e : current.to) {
					queue.offer(e.to);
				}
			}
		}
		return false;
	}

	/**
	 * Given a service name, this method returns the corresponding
	 * graph node from the provided graph. Note that this method
	 * will create a node for a service if one currently does
	 * not exist.
	 *
	 * @param name
	 * @param graph
	 * @return node
	 */
	private GraphNode getGraphNode(String name, Graph graph) {
		GraphNode node = graph.nodeMap.get(name);
		if (node == null) {
			ServiceNode n;
			if (name.equals("Input"))
				n = inputNode;
			else if (name.equals("Output"))
				n = outputNode;
			else
				n = serviceMap.get(name);
			node = new GraphNode(n);
			graph.nodeMap.put(name, node);
		}
		return node;
	}

	/**
	 * Randomly selects a service (from the list provided) that satisfies at least one
	 * input from the set provided. A service is only selected if it is not already within
	 * reach for that path, thus preventing the formation of cycles.
	 *
	 * @param services
	 * @param input The selected service's output satisfies
	 * at least one of the inputs in this set.
	 * @param random
	 * @return selected service (will be null only if there are no suitable service options)
	 */
	private Node pickRandomService(GraphNode node, Graph graph, List<ServiceNode> services, Set<String> input, RandomNumberGenerator random) {
		Node selected = null;
		Iterator<String> it = input.iterator();
		GraphNode origin = graph.nodeMap.get(node.getName());
		if (origin == null) {
			System.err.println("Origin node should not be null.");
			System.exit(1);
		}

		while (selected == null && it.hasNext()) {
			String next = it.next();
			List<Node> sList = new ArrayList<Node>(taxonomyMap.get(next).getServicesWithOutput());

			if (sList != null && !sList.isEmpty()) {
				Collections.shuffle(sList, ((MyRand)random).getRandom());
				for (int i = 0; i < sList.size(); i++) {
					Node s = sList.get(i);
					GraphNode destination = graph.nodeMap.get(s.getIdentifier());
					if (services.contains(s) || s.getIdentifier().equals(inputNode.getName())) {
						if (destination == null) {
							selected = s;
							break;
						}
						else {
							if (!hasPath(origin, destination)) {
								selected = s;
								break;
							}
							else {
								// If input satisfaction of service in node can only be performed
								// by a single other service, remove the service in node from future
								// consideration
								if (sList.size() == 1) {
									//services.remove(node.node); XXX
									//removedNodes++;
								}
							}
						}
					}
				}
			}
		}
		return selected;
	}

	/**
	 * Calculates the intersection of two sets without
	 * destroying the original sets.
	 *
	 * @param a
	 * @param b
	 * @return intersection
	 */
	private Set<String> doIntersection(Set<String> a, Set<String> b) {
		Set<String> intersection = new HashSet<String>(a);
		intersection.retainAll(b);
		return intersection;
	}

	/**
	 * Populates the taxonomy tree by associating services to the
	 * nodes in the tree. Services are added if
	 * The output map allows services to be filtered by the output
	 * they produce.
	 */
	private void populateOutputsInTree() {
		for (ServiceNode s: serviceMap.values()) {
			for (String outputVal : s.getOutputs())
				taxonomyMap.get(outputVal).services.add(s);
		}

		// Now add the outputs of the input node
		for (String outputVal : inputNode.getOutputs())
			taxonomyMap.get(outputVal).services.add(inputNode);
	}

	/**
	 * Updates the composition task's available inputs and required
	 * outputs, also updating the output map and rediscovering the
	 * services relevant for the composition.
	 *
	 * @param inputs
	 * @param outputs
	 */
	public void updateInputAndOutput(Set<String> inputs, Set<String> outputs) {
		Set<String> oldInputs = inputNode.getOutputs();

		// Remove input node from the old places in the input nodes
		for (String s : oldInputs)
			taxonomyMap.get(s).services.remove(inputNode);

		inputNode = inputNode.clone();
		outputNode = outputNode.clone();
		inputNode.setOutputs(inputs);
		outputNode.setInputs(outputs);
		availableInputs = inputs;
		requiredOutputs = outputs;

		// Now add the outputs of the input node
		for (String outputVal : inputNode.getOutputs())
			taxonomyMap.get(outputVal).services.add(inputNode);

		// Rediscover services fit for the composition
		//relevantServices = getRelevantServices(serviceMap, availableInputs, requiredOutputs); XXX
	}

	/**
	 * Parses the file with the given name, creating Web
	 * services based on this information and saving them
	 * to the service map.
	 *
	 * @param fileName
	 */
	private void parseFile(String fileName) {
        Set<String> inputs = new HashSet<String>();
        Set<String> outputs = new HashSet<String>();
        double[] qos = new double[4];

        Properties p = new Properties(inputs, outputs, qos);

        try {
            Scanner scan = new Scanner(new File(fileName));
            while(scan.hasNext()) {
				String name = scan.next();
				String inputBlock = scan.next();
				String outputBlock = scan.next();

				qos[TIME] = scan.nextDouble();
				qos[COST] = scan.nextDouble();
				qos[AVAILABILITY] = scan.nextDouble()/100;
				qos[RELIABILITY] = scan.nextDouble()/100;
				// Throw the other two away;
				scan.nextDouble();
				scan.nextDouble();

				for (String s :inputBlock.split(","))
					inputs.add(s);
				for (String s :outputBlock.split(","))
				outputs.add(s);

                p = new Properties(inputs, outputs, qos);

                ServiceNode ws = new ServiceNode(name, p);
                serviceMap.put(name, ws);
                inputs = new HashSet<String>();
                outputs = new HashSet<String>();
                qos = new double[4];
            }
            scan.close();
        }
        catch(IOException ioe) {
            System.out.println("File parsing failed...");
        }
		numServices = serviceMap.size();
    }

	/**
	 * Parses the WSC Web service file with the given name, creating Web
	 * services based on this information and saving them to the service map.
	 *
	 * @param fileName
	 */
	private void parseWSCServiceFile(String fileName) {
        Set<String> inputs = new HashSet<String>();
        Set<String> outputs = new HashSet<String>();
        double[] qos = new double[4];

        Properties p = new Properties(inputs, outputs, qos);

        try {
        	File fXmlFile = new File(fileName);
        	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        	Document doc = dBuilder.parse(fXmlFile);

        	NodeList nList = doc.getElementsByTagName("service");

        	for (int i = 0; i < nList.getLength(); i++) {
        		org.w3c.dom.Node nNode = nList.item(i);
        		Element eElement = (Element) nNode;

        		String name = eElement.getAttribute("name");
				qos[TIME] = Double.valueOf(eElement.getAttribute("Res"));
				qos[COST] = Double.valueOf(eElement.getAttribute("Pri"));
				qos[AVAILABILITY] = Double.valueOf(eElement.getAttribute("Ava"));
				qos[RELIABILITY] = Double.valueOf(eElement.getAttribute("Rel"));

				// Get inputs
				org.w3c.dom.Node inputNode = eElement.getElementsByTagName("inputs").item(0);
				NodeList inputNodes = ((Element)inputNode).getElementsByTagName("instance");
				for (int j = 0; j < inputNodes.getLength(); j++) {
					org.w3c.dom.Node in = inputNodes.item(j);
					Element e = (Element) in;
					inputs.add(e.getAttribute("name"));
				}

				// Get outputs
				org.w3c.dom.Node outputNode = eElement.getElementsByTagName("outputs").item(0);
				NodeList outputNodes = ((Element)outputNode).getElementsByTagName("instance");
				for (int j = 0; j < outputNodes.getLength(); j++) {
					org.w3c.dom.Node out = outputNodes.item(j);
					Element e = (Element) out;
					outputs.add(e.getAttribute("name"));
				}

                p = new Properties(inputs, outputs, qos);

                ServiceNode ws = new ServiceNode(name, p);
                serviceMap.put(name, ws);
                inputs = new HashSet<String>();
                outputs = new HashSet<String>();
                qos = new double[4];
        	}
    		numServices = serviceMap.size();
        }
        catch(IOException ioe) {
            System.out.println("Service file parsing failed...");
        }
        catch (ParserConfigurationException e) {
            System.out.println("Service file parsing failed...");
		}
        catch (SAXException e) {
            System.out.println("Service file parsing failed...");
		}
		numServices = serviceMap.size();
    }

	/**
	 * Parses the WSC task file with the given name, extracting input and
	 * output values to be used as the composition task.
	 *
	 * @param fileName
	 */
	private void parseWSCTaskFile(String fileName) {
		try {
	    	File fXmlFile = new File(fileName);
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(fXmlFile);

	    	org.w3c.dom.Node provided = doc.getElementsByTagName("provided").item(0);
	    	NodeList providedList = ((Element) provided).getElementsByTagName("instance");
	    	INPUT = new String[providedList.getLength()];
	    	for (int i = 0; i < providedList.getLength(); i++) {
				org.w3c.dom.Node item = providedList.item(i);
				Element e = (Element) item;
				INPUT[i] = e.getAttribute("name");
	    	}

	    	org.w3c.dom.Node wanted = doc.getElementsByTagName("wanted").item(0);
	    	NodeList wantedList = ((Element) wanted).getElementsByTagName("instance");
	    	OUTPUT = new String[wantedList.getLength()];
	    	for (int i = 0; i < wantedList.getLength(); i++) {
				org.w3c.dom.Node item = wantedList.item(i);
				Element e = (Element) item;
				OUTPUT[i] = e.getAttribute("name");
	    	}

			availableInputs = new HashSet<String>(Arrays.asList(INPUT));
			requiredOutputs = new HashSet<String>(Arrays.asList(OUTPUT));
		}
		catch (ParserConfigurationException e) {
            System.out.println("Task file parsing failed...");
		}
		catch (SAXException e) {
            System.out.println("Task file parsing failed...");
		}
		catch (IOException e) {
            System.out.println("Task file parsing failed...");
		}
	}

	/**
	 * Parses the WSC taxonomy file with the given name, building a
	 * tree-like structure.
	 *
	 * @param fileName
	 */
	private void parseWSCTaxonomyFile(String fileName) {
		try {
	    	File fXmlFile = new File(fileName);
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(fXmlFile);
	    	Element taxonomy = (Element) doc.getChildNodes().item(0);

	    	processTaxonomyChildren(null, taxonomy.getChildNodes());
		}

		catch (ParserConfigurationException e) {
            System.out.println("Taxonomy file parsing failed...");
		}
		catch (SAXException e) {
            System.out.println("Taxonomy file parsing failed...");
		}
		catch (IOException e) {
            System.out.println("Taxonomy file parsing failed...");
		}
	}

	/**
	 * Recursive function for recreating taxonomy structure from file.
	 *
	 * @param parent - Nodes' parent
	 * @param nodes
	 */
	private void processTaxonomyChildren(TaxonomyNode parent, NodeList nodes) {
		if (nodes != null && nodes.getLength() != 0) {
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Node ch = nodes.item(i);

				if (!(ch instanceof Text)) {
					Element currNode = (Element) nodes.item(i);

					TaxonomyNode taxNode = new TaxonomyNode(currNode.getAttribute("name"), parent);
					taxonomyMap.put(taxNode.value, taxNode);
					if (parent != null)
						parent.children.add(taxNode);

					NodeList children = currNode.getChildNodes();
					processTaxonomyChildren(taxNode, children);
				}
			}
		}
	}

	/**
	 * Converts input, output, and service instance values to their corresponding
	 * ontological parent.
	 */
	private void findConceptsForInstances() {
		for (int i = 0; i < INPUT.length; i++)
			INPUT[i] = taxonomyMap.get(INPUT[i]).parent.value;

		for (int i = 0; i < OUTPUT.length; i++)
			OUTPUT[i] = taxonomyMap.get(OUTPUT[i]).parent.value;

		availableInputs = new HashSet<String>(Arrays.asList(INPUT));
		requiredOutputs = new HashSet<String>(Arrays.asList(OUTPUT));

		for (ServiceNode s : serviceMap.values()) {
			Set<String> inputs = s.getInputs();
			Set<String> newInputs = new HashSet<String>();

			for (String i : inputs)
				newInputs.add(taxonomyMap.get(i).parent.value);
			s.setInputs(newInputs);

			Set<String> outputs = s.getOutputs();
			Set<String> newOutputs = new HashSet<String>();

			for (String i : outputs)
				newOutputs.add(taxonomyMap.get(i).parent.value);
			s.setOutputs(newOutputs);
		}
	}

	/**
	 * Generates a summarised file with statistics and info
	 * extracted from the corresponding pso log.
	 */
	public static void _generateStatistics() {
		try {
			// Setup info
			PrintStream stats = new PrintStream(new File (_statLogFileName + ".txt"));

			Scanner scan = new Scanner(new File(_logFileName+".txt"));
			while (scan.hasNext(SETUP.toString())) {
				// Append setup info
				stats.append(scan.nextLine()+"\n");
			}
			stats.append("\n");

			while (scan.hasNext(DETAILS.toString()) || scan.hasNext(RUN.toString())) {
				// Throw detailed info away
				scan.nextLine();
			}

			while (scan.hasNext()) {
				// Append post-run info
				stats.append(scan.nextLine()+"\n");
			}
			scan.close();
			stats.append("\n");

			// Perf4J info
			Reader reader = new FileReader(_logFileName+".txt");
			LogParser parser = new LogParser(reader, stats, null, 10800000, true, new GroupedTimingStatisticsTextFormatter());
			parser.parseLog();
			stats.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Resets data structures in the model to ready
	 * it for another run.
	 */
	private void reset() {
	    availableInputs = new HashSet<String>(Arrays.asList(INPUT));
	    requiredOutputs = new HashSet<String>(Arrays.asList(OUTPUT));
	    updateInputAndOutput(availableInputs, requiredOutputs);
	}

	public static void main(String[] args) {
		final GPModel model = new QoSModel(null, null, null, null, null);
	}
}
