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
	public double time = 0.0;
	public double cost = 0.0;
	public double reliability = 1.0;
	public double availability = 1.0;

	/**
	 * Creates an empty instance of EvaluationResults.
	 */
	public EvaluationResults() {}

	/**
	 * Creates an EvaluationResults instance that holds
	 * the values provided as arguments.
	 */
	public EvaluationResults(double time, double cost, double reliability, double availability) {
		this.time = time;
		this.cost = cost;
		this.reliability = reliability;
		this.availability = availability;
	}
}
