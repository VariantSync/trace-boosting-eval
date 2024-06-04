package org.variantsync.traceboosting.eval;

import java.util.Arrays;
import java.util.Random;


public class testClass {
    public static void main(String... args) {

        System.out.println(Integer.parseInt(" 5"));
        int n = 100; // Size of the arrays
        double truePercentage = 25; // Percentage of true values
        // double standardDeviation = 0.3; // % of percentage Standard deviation for normal distribution
        double[] dl = new double[]{0.1, 0.4, 0.8, 1.2, 1.6, 2.0, 2.4, 2.8, 3.2, 3.6, 4.0};
        for (double d : dl) {
            int[] array = getDistribution(n, truePercentage, d);
            // Print the generated arrays
            System.out.println(Arrays.toString(Arrays.copyOfRange(array, 0, 20)));
            System.out.println(Arrays.toString(Arrays.copyOfRange(array, 20, 40)));
            System.out.println(Arrays.toString(Arrays.copyOfRange(array, 40, 60)));
            System.out.println(Arrays.toString(Arrays.copyOfRange(array, 60, 80)));
            System.out.println(Arrays.toString(Arrays.copyOfRange(array, 80, 100)));
            System.out.println("----------------------------------------------------------------------");
        }
    }

    private static int[] getDistribution(int NumberOfAllNodes, double Percentage, double standardDeviation) {
        // adapt the standard diviation with the percentage of mapping and the number of nodes
        standardDeviation = standardDeviation * Percentage * (NumberOfAllNodes / 100);
        // build the array
        int[] distArray = new int[NumberOfAllNodes];
        Random random = new Random();
        // Calculate the number of true values based on the percentage
        int trueCount = (int) Math.round(NumberOfAllNodes * (Percentage / 100));
        // Generate indices for true values according to normal distribution
        for (int i = 0; i < trueCount; i++) {
            int index = (int) Math.round(random.nextGaussian() * standardDeviation + NumberOfAllNodes / 2);
            if (index >= 0 && index < NumberOfAllNodes && distArray[index] == 0) {
                distArray[index] = 1;
            } else {
                // to regenerate a new index in range of array size to not lossing the percentage
                i--;
            }
        }
        return distArray;
    }
}
