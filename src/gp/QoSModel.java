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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nodes.ConditionalNode;
import nodes.EvaluationResults;
import nodes.InOutNode;
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

import sun.security.krb5.Config;
import sun.security.krb5.KrbException;
import taxonomy.TaxonomyNode;

/**
 * Model implementation for performing tree-based Web
 * service composition. Evolutionary parameters, tasks,
 * and available Web services are all set within this task.
 *
 * @author sawczualex
 */
public class QoSModel extends GPModel {
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

	public static final int IF = 1;
	public static final int ELSE = 0;
	public static final int COND_IF = 1;
	public static final int COND_ELSE = 0;

	// Node data structures for composition generation
	private Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	private List<ServiceNode> relevantServices;
	private Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();

	// Fitness function weights
    private static final double w1 = 0.25;
    private static final double w2 = 0.25;
    private static final double w3 = 0.25;
    private static final double w4 = 0.25;
	public static final int POPULATION = 500;
	public static final int MAX_NUM_ITERATIONS = 51;
	public static final int MAX_INIT_DEPTH = -1;
	public static final int MAX_DEPTH = -1;
	public static final int NO_ELITES = 0;
	public static final double CROSSOVER_PROB = 0.8;
	public static final double MUTATION_PROB = 0.1;
	public static final double REPRODUCTION_PROB = 0.1;
	public static final int TOURNAMENT_SIZE = 2;

    /* Available inputs, required outputs, and the dummy service
     * nodes representing them. */
    private static Set<String> availableInputs = new HashSet<String>();
    private static Set<String> requiredOutputs = new HashSet<String>();
    public static Condition condition;
    private ServiceNode inputNode;
    private ServiceNode outputNode;

    // Variables for normalisation
	private double _totalTime = 0.0;
	private double _totalCost = 0.0;
	private boolean normaliseTotals = true;
	private boolean recalculateTotals = true;

	// Run settings
	private static String _servFilename = "services-prob-test.xml";
	private static String _taskFilename = "problem-prob.xml";
	//private static String _taskFilename = "taskSet.xml";
	//private static String _taskFilename = "taskSetNoCondition2.xml";
	private static String _taxonomyFilename = "taxonomy.xml";

	public static String[] INPUT;
	public static String[] OUTPUT_IF;
	public static String[] OUTPUT_ELSE;
	public static double MINIMUM_COST = Double.MAX_VALUE;
	public static double MINIMUM_TIME = Double.MAX_VALUE;
	public static final double MINIMUM_RELIABILITY = 0;
	public static final double MINIMUM_AVAILABILITY = 0;
	public static double MAXIMUM_COST = Double.MIN_VALUE;
	public static double MAXIMUM_TIME = Double.MIN_VALUE;
	public static double MAXIMUM_RELIABILITY = Double.MIN_VALUE;
	public static double MAXIMUM_AVAILABILITY = Double.MIN_VALUE;


	public static int NUM_RUNS = 1;
	public static int numServices;
	private static int SEED_COEFFICIENT = 1;

	/**
	 * Sets up logging for this session.
	 */
	public void setupLogging() {
		try {
			_logger = Logger.getLogger(QoSModel.class);
			SimpleLayout layout = new SimpleLayout();
			_appender = new FileAppender(layout,_logFileName,false);
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

    static String sep = System.getProperty("line.separator");

    public PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
        	private int count = 1;
        	private String s = "";
        	@Override
    		public void write(byte buf[], int off, int len) {
//                if (len > 0 && buf[len-1] != sep.charAt( 0 )) { XXX
//    				s += String.format("%s", StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(buf, off, len)));
//    			}
//    			else {
//	    	        try {
//	    	        	_logger.log(DETAILS, s);
//	    	        	s = "";
//	    	        }
//	    	        catch (Exception e) {
//	    	       }
//    			}
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
	public QoSModel(Integer argSeed, String logName, String statLogName, String dataset, String taskSet, String taxonomySet) {

		if (argSeed != null) {
			SEED_COEFFICIENT = argSeed;
		}
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

//		List<List<String>> outList = new ArrayList<List<String>>();
//		List<String> l = new ArrayList<String>();
//		for (String s : INPUT)
//			l.add(s);
//		outList.add(l);
//		List<Float> outProbList = new ArrayList<Float>();
//		outProbList.add(1.0f);
//    	inputNode = new ServiceNode("Input", new Properties(new HashSet<String>(), outList, outProbList, new double[]{0,0,1,1}));
//    	inputNode.getOutputs().add(new HashSet<String>(outList.get(0)));
//    	Set<String> l2 = new HashSet<String>();
//    	l2.add(condition.general);
//    	outputNode = new ServiceNode("Output", new Properties(l2, new ArrayList<List<String>>(), new ArrayList<Float>(), new double[]{0,0,1,1}));
//    	outputNode.getOutputs().add(new HashSet<String>());

		List<Float> probList = new ArrayList<Float>();
		probList.add(1.0f);
    	inputNode = new ServiceNode("Input", new Properties(new HashSet<String>(), new ArrayList<List<String>>(), probList, new double[]{0,0,1,1}));
    	inputNode.getOutputs().add(new HashSet<String>());
    	outputNode = new ServiceNode("Output", new Properties(new HashSet<String>(), new ArrayList<List<String>>(), probList, new double[]{0,0,1,1}));
    	outputNode.getOutputs().add(new HashSet<String>());

    	populateOutputsInTree();

        // Set terminals and function nodes
		List<Node> syntax = new ArrayList<Node>();

		// Function nodes
		syntax.add(new SequenceNode());
		syntax.add(new ParallelNode());
		syntax.add(new ConditionalNode());

//		relevantServices = getRelevantServices(serviceMap, new HashSet<String>(l), new HashSet<String>(l2));
		recalculateTotals = false;

		// Terminal nodes
		for (ServiceNode s : serviceMap.values()) {
			syntax.add(s);
		}


        _logger.log(SETUP, "Filename: " + _servFilename);
        _logger.log(SETUP, "NumServices: " + numServices);
        _logger.log(SETUP, "TaskInput: " + Arrays.toString(INPUT));
        _logger.log(SETUP, "Condition: " + condition.general + " == " + condition.specific);
        _logger.log(SETUP, "TaskOutputIf: " + Arrays.toString(OUTPUT_IF));
        _logger.log(SETUP, "TaskOutputElse: " + Arrays.toString(OUTPUT_ELSE));
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
		setReproductionProbability(REPRODUCTION_PROB);
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

			long seed = (i + 1) * SEED_COEFFICIENT;
        	setRNG(new MyRand(seed));

        	// Set operators and components
        	setInitialiser(new GreedyInitialiser(this, getRNG()));
        	setProgramSelector(new TournamentSelector(this, TOURNAMENT_SIZE));
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
//			reset();
        }
		_logger.log(POSTRUN, "BestOverallFitness: " + bestOverallFitness);
		_logger.log(POSTRUN, "BestOverallProgram: " + bestOverallProgram);
		_generateStatistics();
		_logger.removeAppender(_appender);
		_appender.close();
		System.setOut(stdout);
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
        EvaluationResults res = (EvaluationResults) p.evaluate();

//        System.err.println("a: "+ res.availability + ", r: " + res.reliability + ", t: " + res.time + ", c: " + res.cost);
		return w1 * (1.0 - normaliseAvailability(res.availability)) +
			   w2 * (1.0 - normaliseReliability(res.reliability)) +
			   w3 * normaliseTime(res.time) +
			   w4 * normaliseCost(res.cost);
	}

	private double normaliseAvailability(double availability) {
		if (MAXIMUM_AVAILABILITY - MINIMUM_AVAILABILITY == 0.0)
			return 1.0;
		else
			return (availability - MINIMUM_AVAILABILITY)/(MAXIMUM_AVAILABILITY - MINIMUM_AVAILABILITY);
	}

	private double normaliseReliability(double reliability) {
		if (MAXIMUM_RELIABILITY - MINIMUM_RELIABILITY == 0.0)
			return 1.0;
		else
			return (reliability - MINIMUM_RELIABILITY)/(MAXIMUM_RELIABILITY - MINIMUM_RELIABILITY);
	}

	private double normaliseTime(double time) {
		//double numEnds = init.endNodes.size();

		if ((MAXIMUM_TIME * serviceMap.size()) - MINIMUM_TIME == 0.0)
			return 0.0;
		else
			return (time - MINIMUM_TIME)/((MAXIMUM_TIME * serviceMap.size()) - MINIMUM_TIME);
	}

	private double normaliseCost(double cost) {
		//double numEnds = init.endNodes.size();

		if ((MAXIMUM_COST * serviceMap.size()) - MINIMUM_COST == 0.0)
			return 0.0;
		else
			return (cost - MINIMUM_COST)/((MAXIMUM_COST * serviceMap.size()) - MINIMUM_COST);
	}

    @Override
	/**
	 * This method is used by the EpochX framework.
	 *
	 * @return class
	 */
    public Class<?> getReturnType() {
        Properties p = new Properties(null, null, null, null);
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
	public List<ServiceNode> getRelevantServices(Map<String,ServiceNode> serviceMap, Set<String> inputs, Set<String> outputs) {
		// If we are counting total time and total cost from scratch, reset them
		if (normaliseTotals && recalculateTotals) {
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
				cSearch.addAll(s.getOutputs().get(ELSE));
				if (normaliseTotals && recalculateTotals) {
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
			System.err.println(message);
			System.exit(0);
			return null;
		}
	}

//	/**
//	 * Goes through the service list and retrieves only those services which
//	 * could be part of the composition task requested by the user.
//	 *
//	 * @param serviceMap
//	 * @return relevant services
//	 */
//	private Set<ServiceNode> getGeneralRelevantServices(Map<String,ServiceNode> serviceMap, Set<String> inputs, Set<String> outputs) {
//		// Copy service map values to retain original
//		Collection<ServiceNode> services = new ArrayList<ServiceNode>(serviceMap.values());
//
//		Set<String> cSearch = new HashSet<String>(inputs);
//		Set<ServiceNode> sSet = new HashSet<ServiceNode>();
//		Set<ServiceNode> sFound = discoverService(services, cSearch);
//		while (!sFound.isEmpty()) {
//			sSet.addAll(sFound);
//			services.removeAll(sFound);
//			for (ServiceNode s: sFound) {
//				for (List<String> outPoss : s.getOutputPossibilities()) {
//					cSearch.addAll(outPoss);
//				}
//			}
//			sFound.clear();
//			sFound = discoverService(services, cSearch);
//		}
//
//		if (isSubsumed(outputs, cSearch)) {
//			return sSet;
//		}
//		else {
//			String message = "It is impossible to perform a composition using the services and settings provided.";
//			System.out.println(message);
//			System.exit(0);
//			return null;
//		}
//	}

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

	public Node replaceServicesInTree(Node root, ServiceNode toReplace, ServiceNode replacement) {
		// If root is the node to be replaced
		if (root.getIdentifier().equals(toReplace.getName())) {
			return replacement;
		}
		// Else try to replace the children of this node, if any
		else {
			Node[] children = root.getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i] = replaceServicesInTree(children[i], toReplace, replacement);
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
			if (!isIntersection( searchSet, subsumed )) {
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
			if (isIntersection( searchSet, subsumed ))
				satisfied.add(input);
		}
		return satisfied;
	}

	public Graph createGraph(List<ServiceNode> services, RandomNumberGenerator random, boolean exactOutput) {
	    Graph graph = new Graph();
	    Set<ServiceNode> unused = new HashSet<ServiceNode>(services);
	    GraphNode start = new GraphNode(inputNode, this);
	    graph.nodeMap.put(start.getName(), start);
	    Set<String> availableInputs = new HashSet<String>();
	    availableInputs.addAll(start.getOutputs());

	    while(!isSubsumed(requiredOutputs,availableInputs)) {
	        ServiceNode chosen = chooseServiceNode(availableInputs, new ArrayList<ServiceNode>(unused), random);
	        unused.remove( chosen );
	        availableInputs.addAll(chosen.getOutputs().get(ELSE));
	        connectChosenNode(chosen, graph);
	    }
	    connectChosenNode(outputNode, graph);
	    removeDanglingNodes(graph);
	    return graph;
	}

	private ServiceNode chooseServiceNode(Set<String> outputs, List<ServiceNode> services, RandomNumberGenerator r) {
		Collections.shuffle(services, ((MyRand) r).getRandom());
	    for (ServiceNode n : services) {
	        if (isSubsumed(n.getInputs(), outputs)) {
	            return n;
	        }
	    }
	    return null;
	}

	private void connectChosenNode(ServiceNode node, Graph graph) {
	    GraphNode graphNode = getGraphNode(node.getName(), graph);
	    Set<String> inputs = new HashSet<String>(node.getInputs());

	    while (!inputs.isEmpty()) {
	        for (GraphNode gn : graph.nodeMap.values()) {
	            if (gn != graphNode) {
    	            Set<String> intersect = getSatisfiedInputs(inputs, new HashSet<String>(gn.getOutputs()));
    	            if (!intersect.isEmpty()) {
    	            	if (!(gn.getName().equals("Input") && node.getName().equals("Output"))) {
    	            		GraphEdge edge = new GraphEdge(intersect);
    	            		edge.from = gn;
    	            		edge.to = graphNode;
    	            		gn.to.add( edge );
    	            		graphNode.from.add( edge );
    	            		graph.edges.add( edge );
    	            	}
    	            	inputs.removeAll(intersect);
    	            }
	            }
	        }
	    }
	}

	private void removeDanglingNodes(Graph graph) {
	    List<GraphNode> dangling = new ArrayList<GraphNode>();
	    for (GraphNode g : graph.nodeMap.values()) {
	        if (!g.getName().equals("Output") && g.to.isEmpty())
	            dangling.add( g );
	    }

	    for (GraphNode d: dangling) {
	        removeDangling(d, graph);
	    }
	}

	private void removeDangling(GraphNode n, Graph graph) {
	    if (n.to.isEmpty()) {
	        graph.nodeMap.remove( n.getName() );
	        for (GraphEdge e : n.from) {
	            e.from.to.remove( e );
	            graph.edges.remove( e );
	            removeDangling(e.from, graph);
	        }
	    }
	}

	public Node createTree(Set<String> inputs, List<Set<String>> outputPossibilities, RandomNumberGenerator random, float[] probabilities) {
		// Create a conditional node if necessary
		if (outputPossibilities.size() > 1) {
	        // Create subtree from the condition's specific value to if-output
	        Set<String> condInputs = new HashSet<String>();
	        condInputs.add(QoSModel.condition.specific);
	        condInputs.addAll(inputs);
			updateInputAndOutput(condInputs, outputPossibilities.get(IF));

			Graph ifGraph = null;
			while (ifGraph == null) {
				ifGraph = createGraph(getRelevantServices(getServices(), condInputs, outputPossibilities.get(IF)), random, false);
			}
	        Node ifTree = ifGraph.nodeMap.get("Input").toTree(getInputs());
	        adjustTreeOutputs(ifTree, outputPossibilities.get(IF));

	        // Create subtree from the condition's general value to else-output
	        condInputs = new HashSet<String>();
	        condInputs.add(QoSModel.condition.general);
	        condInputs.addAll(inputs);
			updateInputAndOutput(condInputs, outputPossibilities.get(ELSE));

			Graph elseGraph = null;
			while (elseGraph == null) {
				elseGraph = createGraph(getRelevantServices(getServices(), condInputs, outputPossibilities.get(ELSE)), random, false);
			}
	        Node elseTree = elseGraph.nodeMap.get("Input").toTree(getInputs());
	        adjustTreeOutputs(elseTree, outputPossibilities.get(ELSE));


			// XXX: Assumption that ServiceNode outputs and probabilities are ordered from the most specific to the most
			// general option.

	        ConditionalNode conditionalTree = new ConditionalNode(QoSModel.condition);
	        conditionalTree.setChild(COND_IF, ifTree);
	        conditionalTree.setChild(COND_ELSE, elseTree);

	        conditionalTree.getInputs().clear();
	        conditionalTree.getInputs().add(QoSModel.condition.general);
	        conditionalTree.getInputs().addAll(inputs);
	        conditionalTree.getOutputs().add(outputPossibilities.get(ELSE));
	        conditionalTree.getOutputs().add(outputPossibilities.get(IF));

	        // Create a non-conditional part to the tree if necessary
	        condInputs = new HashSet<String>();
	        condInputs.add(QoSModel.condition.general);
	        if (isSubsumed(condInputs, inputs)) {
	        	conditionalTree.setProbabilities(probabilities);
	        	return conditionalTree;
	        }
	        else {
	        	updateInputAndOutput(inputs, condInputs);

	        	Graph initialGraph = null;
	        	while (initialGraph == null) {
	        		initialGraph = createGraph(getRelevantServices(getServices(), inputs, condInputs), random, false);
	        	}
	        	Node initialTree = initialGraph.nodeMap.get("Input").toTree(getInputs());
	        	adjustTreeOutputs(initialTree, condInputs);

	        	// XXX: Assumption that there is only one node that produces the output for the condition,
	        	// and that this node has probabilities that match the specific/general alternatives of condition.
	        	List<Float> probs = initialGraph.nodeMap.get("Output").from.get(0).from.node.getProbabilities();
	        	probabilities = new float[2];
	        	probabilities[COND_IF] = probs.get(IF);
	        	probabilities[COND_ELSE] = probs.get(ELSE);
	        	conditionalTree.setProbabilities(probabilities);

	        	// Assemble overall tree
	        	SequenceNode tree = new SequenceNode();
	        	tree.setChild(0, initialTree);
	        	tree.setChild(1, conditionalTree);
	        	tree.getInputs().clear();
	        	tree.getInputs().addAll(inputs);
	        	tree.getOutputs().add(outputPossibilities.get(ELSE));
	        	tree.getOutputs().add(outputPossibilities.get(IF));
	        	return tree;
	        }
		}
		else {
        	updateInputAndOutput(inputs, outputPossibilities.get(ELSE));

        	Graph simpleGraph = null;
        	while (simpleGraph == null) {
        		simpleGraph = createGraph(getRelevantServices(getServices(), inputs, outputPossibilities.get(ELSE)), random, false);
        	}
        	Node simpleTree = simpleGraph.nodeMap.get("Input").toTree(getInputs());
        	Set<String> correctionSet = new HashSet<String>();
	        correctionSet.addAll(outputPossibilities.get(ELSE));
        	adjustTreeOutputs(simpleTree, correctionSet);
        	return simpleTree;
		}


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
			node = new GraphNode(n, this);
			graph.nodeMap.put(name, node);
		}
		return node;
	}

    private boolean isIntersection( Set<String> a, Set<String> b ) {
        for ( String v1 : a ) {
            if ( b.contains( v1 ) )
                return true;
        }
        return false;
    }

	/**
	 * Populates the taxonomy tree by associating services to the
	 * nodes in the tree. Services are added if
	 * The output map allows services to be filtered by the output
	 * they produce.
	 */
	private void populateOutputsInTree() {
		for (ServiceNode s: serviceMap.values()) {
			for (String outputVal : s.getOutputs().get(ELSE))
				taxonomyMap.get(outputVal).services.add(s);
		}

		// Now add the outputs of the input node
		for (String outputVal : inputNode.getOutputs().get(ELSE))
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
		Set<String> oldInputs = inputNode.getOutputs().get(ELSE);

		// Remove input node from the old places in the input nodes
		for (String s : oldInputs)
			taxonomyMap.get(s).services.remove(inputNode);

		inputNode = inputNode.clone();
		outputNode = outputNode.clone();
		inputNode.getOutputs().clear();
		inputNode.getOutputs().add(inputs);
		outputNode.setInputs(outputs);
		availableInputs.clear();
		availableInputs.addAll(inputs);
		requiredOutputs.clear();
		requiredOutputs.addAll(outputs);

		// Now add the outputs of the input node
		for (String outputVal : inputNode.getOutputs().get(ELSE))
			taxonomyMap.get(outputVal).services.add(inputNode);

		// Rediscover services fit for the composition
		relevantServices = getRelevantServices(serviceMap, availableInputs, requiredOutputs);
	}

	/**
	 * Parses the WSC Web service file with the given name, creating Web
	 * services based on this information and saving them to the service map.
	 *
	 * @param fileName
	 */
	private void parseWSCServiceFile(String fileName) {
        Set<String> inputs = new HashSet<String>();
        List<List<String>> outputPossibilities = new ArrayList<List<String>>();
        List<Float> probabilities = new ArrayList<Float>();
        double[] qos = new double[4];

        Properties p;

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
				if (qos[TIME] > MAXIMUM_TIME)
					MAXIMUM_TIME = qos[TIME];
				if (qos[TIME] < MINIMUM_TIME)
					MINIMUM_TIME = qos[TIME];
				qos[COST] = Double.valueOf(eElement.getAttribute("Pri"));
				if (qos[COST] > MAXIMUM_COST)
					MAXIMUM_COST = qos[COST];
				if (qos[COST] < MINIMUM_COST)
					MINIMUM_COST = qos[COST];
				qos[AVAILABILITY] = Double.valueOf(eElement.getAttribute("Ava"));
				if (qos[AVAILABILITY] > MAXIMUM_AVAILABILITY)
					MAXIMUM_AVAILABILITY = qos[AVAILABILITY];
//				if (qos[AVAILABILITY] < MINIMUM_AVAILABILITY)
//					MINIMUM_AVAILABILITY = qos[AVAILABILITY];
				qos[RELIABILITY] = Double.valueOf(eElement.getAttribute("Rel"));
				if (qos[RELIABILITY] > MAXIMUM_RELIABILITY)
					MAXIMUM_RELIABILITY = qos[RELIABILITY];
//				if (qos[RELIABILITY] < MINIMUM_RELIABILITY)
//					MINIMUM_RELIABILITY = qos[RELIABILITY];

				// Get inputs
				org.w3c.dom.Node inputNode = eElement.getElementsByTagName("inputs").item(0);
				NodeList inputNodes = ((Element)inputNode).getElementsByTagName("instance");
				for (int j = 0; j < inputNodes.getLength(); j++) {
					org.w3c.dom.Node in = inputNodes.item(j);
					Element e = (Element) in;
					inputs.add(e.getAttribute("name"));
				}

				// Get outputs
				org.w3c.dom.Node outputNode = eElement.getElementsByTagName("outputs-possibilities").item(0);
				NodeList possList = ((Element)outputNode).getElementsByTagName("outputs");
				for (int j = 0; j < possList.getLength(); j++) {
					org.w3c.dom.Node out = possList.item(j);
					Element e = (Element) out;
					probabilities.add(Float.valueOf(e.getAttribute("prob")));

					List<String> outputs = new ArrayList<String>();
					NodeList valueList = e.getElementsByTagName("instance");
					for (int k = 0; k < valueList.getLength(); k++) {
						org.w3c.dom.Node outVal = valueList.item(k);
						outputs.add(((Element)outVal).getAttribute("name"));
					}
					outputPossibilities.add(outputs);
				}

                p = new Properties(inputs, outputPossibilities, probabilities, qos);

                ServiceNode ws = new ServiceNode(name, p);
                serviceMap.put(name, ws);
                inputs = new HashSet<String>();
                outputPossibilities = new ArrayList<List<String>>();
                probabilities = new ArrayList<Float>();
                qos = new double[4];
        	}
    		numServices = serviceMap.size();
        }
        catch(IOException ioe) {
            System.err.println("Service file parsing failed...");
        }
        catch (ParserConfigurationException e) {
            System.err.println("Service file parsing failed...");
		}
        catch (SAXException e) {
            System.err.println("Service file parsing failed...");
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

	    	org.w3c.dom.Node wanted = doc.getElementsByTagName("options").item(0);
	    	org.w3c.dom.Node conditionNode = ((Element) wanted).getElementsByTagName("condition").item(0);
	    	org.w3c.dom.Node ifNode = ((Element) wanted).getElementsByTagName("if").item(0);
	    	org.w3c.dom.Node elseNode = ((Element) wanted).getElementsByTagName("else").item(0);

	    	String general = ((Element)((Element) conditionNode).getElementsByTagName("general").item(0)).getAttribute("concept");
	    	String specific = ((Element)((Element) conditionNode).getElementsByTagName("specific").item(0)).getAttribute("concept");
	    	condition = new Condition(general, specific);

	    	NodeList wantedList = ((Element) ifNode).getElementsByTagName("instance");
	    	OUTPUT_IF = new String[wantedList.getLength()];
	    	for (int i = 0; i < wantedList.getLength(); i++) {
				org.w3c.dom.Node item = wantedList.item(i);
				Element e = (Element) item;
				OUTPUT_IF[i] = e.getAttribute("name");
	    	}

	    	wantedList = ((Element) elseNode).getElementsByTagName("instance");
	    	OUTPUT_ELSE = new String[wantedList.getLength()];
	    	for (int i = 0; i < wantedList.getLength(); i++) {
				org.w3c.dom.Node item = wantedList.item(i);
				Element e = (Element) item;
				OUTPUT_ELSE[i] = e.getAttribute("name");
	    	}
		}
		catch (ParserConfigurationException e) {
            System.err.println("Task file parsing failed...");
		}
		catch (SAXException e) {
            System.err.println("Task file parsing failed...");
		}
		catch (IOException e) {
            System.err.println("Task file parsing failed...");
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
            System.err.println("Taxonomy file parsing failed...");
		}
		catch (SAXException e) {
            System.err.println("Taxonomy file parsing failed...");
		}
		catch (IOException e) {
            System.err.println("Taxonomy file parsing failed...");
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

		for (int i = 0; i < OUTPUT_IF.length; i++)
			OUTPUT_IF[i] = taxonomyMap.get(OUTPUT_IF[i]).parent.value;

		for (int i = 0; i < OUTPUT_ELSE.length; i++)
			OUTPUT_ELSE[i] = taxonomyMap.get(OUTPUT_ELSE[i]).parent.value;

		for (ServiceNode s : serviceMap.values()) {
			Set<String> inputs = s.getInputs();
			Set<String> newInputs = new HashSet<String>();

			for (String i : inputs)
				newInputs.add(taxonomyMap.get(i).parent.value);
			s.setInputs(newInputs);

			List<List<String>> outputs = s.getOutputPossibilities();
			for(int i = 0; i < outputs.size(); i++) {
				List<String> oldOutputs = outputs.get(i);
				List<String> newOutputs = new ArrayList<String>();
				for (String out : oldOutputs) {
					newOutputs.add(taxonomyMap.get(out).parent.value);
				}
				outputs.set(i, newOutputs);
			}

			// Set the most general concepts of outputs as the non-conditional outputs of a node
			if (outputs.size() == 1) {
				s.getOutputs().clear();
				s.getOutputs().add(new HashSet<String>(outputs.get(ELSE)));
			}
			else {
				Set<String> generalOutput = new HashSet<String>();
				for (int h = 0; h < outputs.get(ELSE).size(); h++) {
					List<String> conceptList = new ArrayList<String>();
					for (int i = 0; i < outputs.size(); i++) {
						conceptList.add(outputs.get(i).get(h));
					}
					generalOutput.add(findMostGeneral(conceptList));
				}
				s.getOutputs().clear();
				s.getOutputs().add(new HashSet<String>(generalOutput));
			}
		}
	}

	public String findMostGeneral(List<String> conceptList) {
		List<List<String>> parentSets = new ArrayList<List<String>>();

		for (String concept : conceptList) {
			parentSets.add(getAncestors(concept));
		}

		List<String> retained = parentSets.get(0);
		for (int i = 1; i < parentSets.size(); i++) {
			retained.retainAll(parentSets.get(i));
		}

		return retained.get(0);
	}

	public List<String> getAncestors(String concept) {
		List<String> parents = new ArrayList<String>();
		parents.add(concept);
		TaxonomyNode parent = taxonomyMap.get(concept).parent;
		while (parent != null) {
			parents.add(parent.value);
			parent = parent.parent;
		}
		return parents;
	}

	/**
	 * Generates a summarised file with statistics and info
	 * extracted from the corresponding pso log.
	 */
	public static void _generateStatistics() {
		try {
			// Setup info
			PrintStream stats = new PrintStream(new File (_statLogFileName));

			Scanner scan = new Scanner(new File(_logFileName));
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
			Reader reader = new FileReader(_logFileName);
			LogParser parser = new LogParser(reader, stats, null, 10800000, true, new GroupedTimingStatisticsTextFormatter());
			parser.parseLog();
			stats.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

    int counter = 0;

    public void adjustTreeOutputs( Node n, Set< String > requiredOutputs ) {
        if ( !( n instanceof ServiceNode ) ) {

            InOutNode ioN = ( InOutNode )n;
            Set< String > outputs = ioN.getOutputs().get(ELSE);
            Set< String > satisfied = getSatisfiedInputs( requiredOutputs, outputs );
            ioN.getOutputs().clear();
            ioN.getOutputs().add( satisfied );
            for ( Node child : n.getChildren() ) {
                adjustTreeOutputs( child, satisfied );
            }
        }
    }

	public static void main(String[] args) {
		if (args.length == 0) {
			final GPModel model = new QoSModel(null, null, null, null, null, null);
		}
		else {
			int seed = Integer.parseInt(args[0]);
			final GPModel model = new QoSModel(seed, args[1], args[2], args[3], args[4], args[5]);
		}
	}
}
