package org.variantsync.boosting.eval.util;

import org.variantsync.boosting.datastructure.ASTNode;
import org.variantsync.boosting.position.ProductPosition;
import org.variantsync.boosting.product.Product;
import org.variantsync.vevos.simulation.variability.pc.Artefact;

import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

/**
 * Utilities class for distributing mappings randomly
 */
public class NormalDistributionMapping {
    /**
     * Distributes mappings for products based on a given percentage and standard
     * deviation.
     *
     * This function takes in an array of preproducts, a path to the variants
     * directory, a map of product PCs,
     * a percentage value for mapping, and a standard deviation for normal
     * distribution. It calculates the total
     * number of nodes in all preproducts, generates a distribution array based on
     * the percentage and standard deviation,
     * and then maps nodes in each product based on the distribution.
     *
     * @param preproducts       The products before mapping
     * @param variantsDirectory The path to the variants directory
     * @param product_pc        A map to save the PCs of each variant to avoid
     *                          loading later
     * @param percentage        The percentage of mapping to be applied
     * @param standardDeviation The standard deviation for normal distribution (used
     *                          for RQ3)
     * @return The products with mapped nodes
     */
    public static Product[] distributeMappings(Product[] preproducts, Path variantsDirectory,
            Map<String, Artefact> product_pc, double percentage, double standardDeviation) {
        // Calculate the total number of nodes in all preproducts
        int numberOfAllNodes = 0;
        for (Product product : preproducts) {
            numberOfAllNodes += product.getProductAst().getAstNodes().size();
        }

        // Generate distribution array based on the percentage and standard deviation
        int[] distribution = getDistribution(numberOfAllNodes, percentage, standardDeviation);

        int j = 0;
        for (Product product : preproducts) {
            // Load PCs for a certain product from VEVOS
            Artefact result = Mapping.findPC(product, variantsDirectory);
            // Store PCs in the product_pc map
            product_pc.put(product.getName(), result);

            // Map nodes in the product based on the distribution
            for (ASTNode node : product.getProductAst().getAstNodes()) {
                if (distribution[j] == 0) {
                    node.setMapping(null);
                } else {
                    ProductPosition position = new ProductPosition(product, node.getStartPosition());
                    Mapping.mapNode(result, node, position);
                }
                j++;
            }
        }

        return preproducts;
    }

    /**
     * Generates an array representing the mapping for all nodes based on a
     * configurable normal distribution.
     *
     * @param NumberOfAllNodes  The number of all nodes in all variants.
     * @param Percentage        The percentage of mapping.
     * @param standardDeviation The standard deviation value (percentage of
     *                          Percentage * NumberOfAllNodes / 100).
     * @return An array of integers representing the mapping for all nodes.
     */
    private static int[] getDistribution(int NumberOfAllNodes, double Percentage, double standardDeviation) {
        // adapt the standard deviation with the percentage of mapping and the number of
        // nodes
        standardDeviation = standardDeviation * Percentage * (NumberOfAllNodes / 100);

        // build the array
        int[] distArray = new int[NumberOfAllNodes];
        Random random = new Random();

        // Calculate the number of true values based on the percentage
        int numberOfMappedNodes = (int) Math.round(NumberOfAllNodes * (Percentage / 100));

        // Generate indices for true values according to normal distribution
        for (int i = 0; i < numberOfMappedNodes; i++) {
            int index = (int) Math.round(random.nextGaussian() * standardDeviation + NumberOfAllNodes / 2);
            if (index >= 0 && index < NumberOfAllNodes && distArray[index] == 0) {
                distArray[index] = 1;
            } else {
                // to regenerate a new index in range of array size to not lose the percentage
                i--;
            }
        }

        return distArray;
    }

}
