package automatedRun;

import gp.QoSModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

/**
 * A small program for statistically comparing
 * the performance of two programs that use
 * the same dataset, same fitness function and
 * composition tasks. The values are read from
 * execution logs.
 *
 * @author sawczualex
 *
 */
public class StatComparison {
	public static final boolean CONSIDER_SETUP_TIME = false;
	public static final String ANALYSIS_LOG_PREFIX = "overallStats";
	public static final String LOG_PREFIX = "gp";
	public static final String STAT_LOG_PREFIX = "gpStats";

	public static final String[] TASK_FILE_LIST = {/*"../wsc2008/Set01MetaData/problem.xml", "../wsc2008/Set02MetaData/problem.xml", */"../wsc2008/Set03MetaData/problem.xml"/*,"../wsc2008/Set04MetaData/problem.xml", "../wsc2008/Set05MetaData/problem.xml", "../wsc2008/Set06MetaData/problem.xml", "../wsc2008/Set07MetaData/problem.xml", "../wsc2008/Set08MetaData/problem.xml"*/};
	public static final String[] SERVICE_FILE_LIST = {/*"../wsc2008/Set01MetaData/services-output.xml", "../wsc2008/Set02MetaData/services-output.xml",*/ "../wsc2008/Set03MetaData/services-output.xml"/*, "../wsc2008/Set04MetaData/services-output.xml", "../wsc2008/Set05MetaData/services-output.xml", "../wsc2008/Set06MetaData/services-output.xml", "../wsc2008/Set07MetaData/services-output.xml", "../wsc2008/Set08MetaData/services-output.xml"*/};
	public static final String[] TAXONOMY_FILE_LIST = {/*"../wsc2008/Set01MetaData/taxonomy.xml", "../wsc2008/Set02MetaData/taxonomy.xml", */"../wsc2008/Set03MetaData/taxonomy.xml"/*, "../wsc2008/Set04MetaData/taxonomy.xml", "../wsc2008/Set05MetaData/taxonomy.xml", "../wsc2008/Set06MetaData/taxonomy.xml", "../wsc2008/Set07MetaData/taxonomy.xml", "../wsc2008/Set08MetaData/taxonomy.xml"*/};

//	public static final String[] TASK_FILE_LIST = {"problem.xml"};
//	public static final String[] SERVICE_FILE_LIST = {"services-output.xml"};
//	public static final String[] TAXONOMY_FILE_LIST = {"taxonomy.xml"};

	public static final int NUM_RUNS = 50;

	public static void main(String[] main) {

		for (int j = 0; j < SERVICE_FILE_LIST.length; j++) {
			System.out.printf("Testing with file '%s'...", SERVICE_FILE_LIST[j]);
			String dateTime = new SimpleDateFormat("_dd-MM-yyyy_HH-mm-ss").format(Calendar.getInstance().getTime());

//			try { TODO
//				Thread.sleep(1000);
//			} catch (InterruptedException e1) {
//				e1.printStackTrace();
//			}

			// Construct new model (constructor will invoke runs)
			new QoSModel(LOG_PREFIX + dateTime, STAT_LOG_PREFIX + dateTime, SERVICE_FILE_LIST[j], TASK_FILE_LIST[j], TAXONOMY_FILE_LIST[j]);

			try {
				double[] time = new double[NUM_RUNS];
				Pattern timePattern = Pattern.compile("time\\[(\\d+)\\]");
				Pattern fitnessPattern = Pattern.compile("([0-9]+.[0-9]+)");
				double setupTime = 0.0;

				double[] fitness = new double[NUM_RUNS];

				Scanner scan = new Scanner(new File(LOG_PREFIX + dateTime + ".txt"));

				while(scan.hasNext("SETUP")) {
					String line = scan.nextLine();

					if (line.contains("SetupTime")) {
						Matcher m = timePattern.matcher(line);
						m.find();
						setupTime = Double.valueOf(m.group(1));
					}
				}

				int timeIdx = 0;
				int fitnessIdx = 0;
				while(scan.hasNext("RUN") || scan.hasNext("DETAILS")) {
					if (scan.hasNext("DETAILS")) {
						scan.nextLine();
						continue;
					}
					String line = scan.nextLine();

					if (line.contains("RunTime")) {
						Matcher m = timePattern.matcher(line);
						m.find();
						double timeInst = Double.valueOf(m.group(1));
						if (CONSIDER_SETUP_TIME)
							time[timeIdx++] = timeInst + setupTime;
						else
							time[timeIdx++] = timeInst;
					}
					else if (line.contains("GlobalBestFitness")) {
						Matcher m = fitnessPattern.matcher(line);
						m.find();
						String s = m.group(1);
						double fitnessInst = Double.valueOf(m.group(1));
						fitness[fitnessIdx++] = fitnessInst;
					}
				}

				scan.close();


				FileWriter writer = new FileWriter(new File(ANALYSIS_LOG_PREFIX + dateTime + ".htm"));
				writer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html><head /><body>\n");

				// Write file
				writer.append(String.format("<h2>Log file: %s</h2>\n", LOG_PREFIX + dateTime + ".txt"));
				writer.append("<table border=\"1\"><tr><th>Run</th><th>Time</th><th>Fitness</th></tr>\n");
				for(int i = 0; i < NUM_RUNS; i++) {
					writer.append(String.format("<tr><td>%d</td><td>%f</td><td>%f</td></tr>\n", i, time[i], fitness[i]));
				}
				writer.append("</table>");

				scan = new Scanner(new File(STAT_LOG_PREFIX + dateTime + ".txt"));
				writer.append("<p>\n");
				while(scan.hasNext("SETUP")) {
					String line = scan.nextLine();
					if (!line.contains("BestDimensions"))
						writer.append(line + "<br>\n");
				}

				while(scan.hasNext("POSTRUN")) {
					String line = scan.nextLine();
					if (!line.contains("BestDimensions"))
						writer.append(line + "<br>\n");
				}
				writer.append("</p>\n");

				// Throw away first two lines

				while(scan.hasNext("Performance") || scan.hasNext("Tag")) {
					scan.nextLine();
				}

				writer.append("<table border=\"1\"><tr><th>Avg(ms)</th><th>Min</th><th>Max</th><th>Std Dev</th><th>Count</th></tr>\n");
				// Throw away next token
				scan.next();
				writer.append(String.format("<tr><td>%f</td><td>%d</td><td>%d</td><td>%f</td><td>%d</td></tr></table>\n",
						scan.nextDouble(), scan.nextLong(), scan.nextLong(), scan.nextDouble(), scan.nextInt()));
				scan.close();


				writer.append(String.format("<p>Average fitness: %f , Standard deviation: %f<br>\n", new Mean().evaluate(fitness), new StandardDeviation().evaluate(fitness)));

				writer.append("</body></html>");
				writer.close();

				System.out.println("...Done.");

			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("All done!");
	}
}
