package gp;

import java.util.Random;

import org.epochx.tools.random.RandomNumberGenerator;

/**
 * A convenience class that allows us to change
 * the seed of a random number generator without
 * invalidating references to it throughout the program.
 *
 * @author sawczualex
 */
public class MyRand implements RandomNumberGenerator {
	private Random random;

	public MyRand(long seed) {
		random = new Random(seed);
	}
	@Override
	public int nextInt(int n) {
		return random.nextInt(n);
	}
	@Override
	public int nextInt() {
		return random.nextInt();
	}
	@Override
	public double nextDouble() {
		return random.nextDouble();
	}
	@Override
	public boolean nextBoolean() {
		return random.nextBoolean();
	}
	@Override
	public void setSeed(long seed) {
		random = new Random(seed);
	}
	public Random getRandom() {
		return random;
	}
}
