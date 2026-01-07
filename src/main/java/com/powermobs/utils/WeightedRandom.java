package com.powermobs.utils;

import java.util.Random;

/**
 * Utility class for weighted random number generation
 */
public class WeightedRandom {
    private static final Random random = new Random();

    /**
     * Generates a random integer within a range with weighted distribution
     *
     * @param min    The minimum value (inclusive)
     * @param max    The maximum value (inclusive)
     * @param weight The weight value (1-200):
     *               1 = heavily favors lower values
     *               100 = equal distribution
     *               200 = heavily favors higher values
     * @return A randomly selected value within the range
     */
    public static int getWeightedRandom(int min, int max, int weight) {
        // Clamp weight to valid range
        weight = Math.max(1, Math.min(200, weight));

        if (min == max) {
            return min; // No range, just return the value
        }

        if (weight == 100) {
            // Equal distribution (simple random)
            return min + random.nextInt(max - min + 1);
        }

        // Calculate how much to skew the distribution
        double skew;
        if (weight < 100) {
            // Favor lower values
            skew = (100 - weight) / 99.0;
        } else {
            // Favor higher values
            skew = (weight - 100) / 100.0;
        }

        if (weight < 100) {
            // For lower values, we use a distribution that skews towards min
            double rand = Math.pow(random.nextDouble(), 1 + skew * 3);
            return min + (int) (rand * (max - min + 1));
        } else {
            // For higher values, we use a distribution that skews towards max
            double rand = 1 - Math.pow(random.nextDouble(), 1 + skew * 3);
            return min + (int) (rand * (max - min + 1));
        }
    }

    /**
     * Generates a weighted random double within a range.
     *
     * @param min    The minimum value (inclusive)
     * @param max    The maximum value (inclusive)
     * @param weight The weight (1-200):
     *               1 = heavily favors lower values
     *               100 = equal distribution
     *               200 = heavily favors higher values
     * @return A weighted random double between min and max
     */
    public static double getWeightedRandom(double min, double max, int weight) {
        weight = Math.max(1, Math.min(200, weight));

        if (min == max) {
            return min;
        }

        if (weight == 100) {
            return min + random.nextDouble() * (max - min);
        }

        double skew;
        if (weight < 100) {
            skew = (100 - weight) / 99.0;
            double rand = Math.pow(random.nextDouble(), 1 + skew * 3);
            return min + rand * (max - min);
        } else {
            skew = (weight - 100) / 100.0;
            double rand = 1 - Math.pow(random.nextDouble(), 1 + skew * 3);
            return min + rand * (max - min);
        }
    }
}
