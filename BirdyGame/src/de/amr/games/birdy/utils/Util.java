package de.amr.games.birdy.utils;

import java.util.Random;

public class Util {

	private static final Random rnd = new Random();

	/**
	 * @param min
	 *              lower bound (inclusive)
	 * @param max
	 *              upper bound (inclusive)
	 * @return random integer from given closed interval
	 */
	public static int randomInt(int min, int max) {
		return min + rnd.nextInt(max - min + 1);
	}
}
