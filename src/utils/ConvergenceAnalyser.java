package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConvergenceAnalyser {
	public static final String[] FILENAMES = {"gp_19-08-2014_09-39-04.txt", "oldPso_18-08-2014_16-40-19.txt", "pso_19-08-2014_09-39-04.txt"};
	public static final String[] APPROACHES = {"GP", "Greedy-based", "Graph-based"};
	public static final int NUM_ITERATIONS = 100;
	public static final int NUM_RUNS = 50;
	public static final String OUTPUT_FILE = "analysis.csv";

	public static void main(String[] args) {
		Map<String, float[]> fitnessAverages = new HashMap<String, float[]>();
		// Initialise map
		for (String s : APPROACHES) {
			fitnessAverages.put(s, new float[NUM_ITERATIONS]);
		}

		Scanner scan;
		for (int i = 0; i < FILENAMES.length; i++) {
			// Create data structure to store all fitness values
			ArrayList<ArrayList<Float>> fitnessPerIteration = new ArrayList<ArrayList<Float>>();
			// Initialize data structure
			for (int iteration = 0; iteration < NUM_ITERATIONS; iteration++) {
				fitnessPerIteration.add(iteration, new ArrayList<Float>());
			}
			// Read fitness values into data structure
			try {
				scan = new Scanner(new File(FILENAMES[i]));
				String line;

				while (scan.hasNext()) {
					line = scan.nextLine();

					if (line.startsWith("DETAILS") && !line.contains("done") && !line.contains("Done")) {
						String[] tokens = line.split("[ \t]");
						int it = Integer.valueOf(tokens[2]);
						float fitness = Float.valueOf(tokens[3]);
						if (APPROACHES[i].equals("GP"))
							fitness = 1 - fitness;
						fitnessPerIteration.get(it).add(fitness);
					}
				}

				scan.close();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			// Calculate averages for each iteration and store result
			for (int iteration = 0; iteration < NUM_ITERATIONS; iteration++) {
				float avg = average(fitnessPerIteration.get(iteration));
				fitnessAverages.get(APPROACHES[i])[iteration] = avg;
			}
		}

		writeToFile(fitnessAverages);
		System.out.println("Done");
	}

	public static float average(List<Float> l) {
		float sum = 0f;
		for (float f : l)
			sum += f;
		return sum / l.size();
	}

	public static void writeToFile(Map<String, float[]> map) {
		try {
			Writer writer = new FileWriter(new File(OUTPUT_FILE));
			// Write table header
			writeLine(writer, "", APPROACHES[0], APPROACHES[1], APPROACHES[2]);

			for (int i = 0; i < NUM_ITERATIONS; i++) {
				writeLine(writer, String.valueOf(i), String.valueOf(map.get(APPROACHES[0])[i]),
								String.valueOf(map.get(APPROACHES[1])[i]), String.valueOf(map.get(APPROACHES[2])[i]));
			}
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeLine(Writer writer, String ... values) {
		String suffix = ",";
		try {
			for (int i = 0; i < values.length; i++) {
				if (i == values.length - 1)
					suffix = "";
				writer.append(values[i]);
				writer.append(suffix);
			}
			writer.append("\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
