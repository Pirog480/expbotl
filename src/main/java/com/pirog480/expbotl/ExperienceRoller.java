package com.pirog480.expbotl;

import net.minecraft.util.math.random.Random;

/**
 * Rolls the total experience granted by a stack of bottles o' enchanting.
 *
 * <p>Vanilla behaviour (1.21.11, {@code ExperienceBottleEntity#onCollision}): every thrown
 * bottle spawns orbs carrying {@code 3 + random.nextInt(5) + random.nextInt(5)} experience -
 * the sum of two independent uniform{0..4} dice plus 3. That is a triangular distribution
 * over 3..11 with the peak and mean at 7. This roller reproduces the sum of {@code n}
 * such bottle rolls exactly.
 *
 * <p>Optimisation: instead of two RNG calls per bottle, six bottles (12 dice) are rolled
 * per call. A single uniform integer in {@code [0, 5^12)} encodes twelve independent
 * base-5 digits (the mapping is a bijection), so every combination stays equally likely -
 * the distribution is mathematically identical to rolling each bottle separately, while
 * the RNG is exercised ~11x less often. A 1024-bottle stack costs ~187 draws instead of
 * 2048; the whole computation takes microseconds.
 */
public final class ExperienceRoller {
	private ExperienceRoller() {
	}

	/** Bottles rolled per single RNG draw; keep in sync with {@link #DIGITS_PER_BATCH} and {@link #BATCH_BOUND}. */
	private static final int BATCH = 6;

	/** Two base-5 dice per bottle, six bottles per draw. */
	private static final int DIGITS_PER_BATCH = 12;

	/** {@code 5^12 = 244140625} equally likely outcomes, one per 12-dice combination. */
	private static final int BATCH_BOUND = 244_140_625;

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

		// Sum of the uniform 0..4 dice; +3 is added once per bottle below.
		int dice = 0;
		int remaining = count;

		while (remaining >= BATCH) {
			int chunk = random.nextInt(BATCH_BOUND);
			for (int i = 0; i < DIGITS_PER_BATCH; i++) {
				dice += chunk % 5;
				chunk /= 5;
			}
			remaining -= BATCH;
		}

		// Leftover bottles (< 6) are rolled the plain vanilla way.
		while (remaining-- > 0) {
			dice += random.nextInt(5);
			dice += random.nextInt(5);
		}

		return 3 * count + dice;
	}
}
