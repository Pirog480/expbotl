package com.pirog480.expbotl;

import net.minecraft.util.math.random.Random;

/**
 * Rolls the total experience granted by a stack of bottles o' enchanting.
 *
 * <p>Vanilla behaviour: every thrown bottle spawns an orb carrying
 * {@code random.nextInt(9) + 3} experience, i.e. a uniformly random amount
 * between 3 and 11 (average {@code ~7}). This roller reproduces the sum of
 * {@code n} independent uniform{3..11} rolls exactly.
 *
 * <p>Optimisation: instead of one RNG call per bottle, six bottles are rolled
 * per call. A single uniform integer in {@code [0, 9^6)} encodes six
 * independent base-9 digits (the mapping is a bijection), so every combination
 * stays equally likely - the distribution is mathematically identical to
 * rolling each bottle separately, while the RNG is exercised ~6x less often.
 * A 1024-bottle stack costs ~171 draws instead of 1024; the whole computation
 * takes microseconds.
 */
public final class ExperienceRoller {
	private ExperienceRoller() {
	}

	/** Bottles rolled per single RNG draw; keep in sync with {@link #BATCH_BOUND}. */
	private static final int BATCH = 6;

	/** {@code 9^6 = 531441} equally likely outcomes, one per 6-bottle combination. */
	private static final int BATCH_BOUND = 531_441;

	/**
	 * Hard cap on the number of bottles rolled at once. Guards against
	 * {@code int} overflow (at most 11 XP per bottle) if some mod/plugin ever
	 * produces an absurdly oversized stack. Real stacks are far below this.
	 */
	private static final int MAX_BOTTLES = 16_000_000;

	/**
	 * @param random  the world random, the same source vanilla uses for orbs
	 * @param bottles how many bottles are being consumed (not limited to 64)
	 * @return the summed experience, between {@code 3 * bottles} and {@code 11 * bottles}
	 */
	public static int roll(Random random, int bottles) {
		int count = Math.min(bottles, MAX_BOTTLES);

		// Sum of the uniform 0..8 bonus of every bottle; +3 is added once per bottle below.
		int bonus = 0;
		int remaining = count;

		while (remaining >= BATCH) {
			int chunk = random.nextInt(BATCH_BOUND);
			for (int i = 0; i < BATCH; i++) {
				bonus += chunk % 9;
				chunk /= 9;
			}
			remaining -= BATCH;
		}

		// Leftover bottles (< 6) are rolled the plain vanilla way.
		while (remaining-- > 0) {
			bonus += random.nextInt(9);
		}

		return 3 * count + bonus;
	}
}
