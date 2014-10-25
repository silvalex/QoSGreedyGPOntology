package nodes;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores results retrieved from evaluating a
 * candidate tree. More specifically, holds the
 * longest execution time for a tree branch and
 * a set of service names from that tree.
 *
 * @author sawczualex
 */
public class EvaluationResults {
	public double longestTime = 0.0;
	public Set<String> servicesInTree = new HashSet<String>();

	/**
	 * Creates an empty instance of EvaluationResults.
	 */
	public EvaluationResults() {}

	/**
	 * Creates an EvaluationResults instance that holds
	 * the values provided as arguments.
	 *
	 * @param longestTime
	 * @param servicesInTree
	 */
	public EvaluationResults(double longestTime, Set<String> servicesInTree) {
		this.longestTime = longestTime;
		this.servicesInTree = servicesInTree;
	}
}
