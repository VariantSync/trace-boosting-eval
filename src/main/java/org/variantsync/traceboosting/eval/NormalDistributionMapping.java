package org.variantsync.traceboosting.eval;

import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.position.ProductPosition;
import de.hub.mse.variantsync.boosting.product.Product;
import org.variantsync.vevos.simulation.variability.pc.Artefact;

import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

public class NormalDistributionMapping {
    /**
     * Mapping function
     *
     * @param preproducts       products before mapping
     * @param variantsDirectory path to variants Directory
     * @param product_pc        to save the PCs of each variant to avoid loading
     *                          later
     * @param percentage        percentage of mapping
     * @param standardDeviation standard deviation for normal distribution RQ3
     * @return products with mapped nodes
     */
    public static Product[] distributeMappings(Product[] preproducts, Path variantsDirectory,
                                               Map<String, Artefact> product_pc, double percentage, double standardDeviation) {
        int numberOfAllNodes = 0;
        for (Product product : preproducts) {
            numberOfAllNodes += product.getProductAst().getAstNodes().size();
        }
        int[] distribution = getDistribution(numberOfAllNodes, percentage, standardDeviation);

        int j = 0;
        for (Product product : preproducts) {
            // loading PCs for a certen product, PC from VEVOS
            Artefact result = Mapping.findPC(product, variantsDirectory);
            // store PCs
            product_pc.put(product.getName(), result);
            for (ASTNode node : product.getProductAst().getAstNodes()) {
                if (distribution[j] == 0) {
                    node.setMapping(null);
                } else {
                    ProductPosition position = new ProductPosition(product, node.getStartPosition());
                    Mapping.mappNode(result, node, position);
                }
                j++;
            }
        }

        return preproducts;
    }

    /**
     * to check the output take a look at testClass.java play with the values there
     * generate an array represent the mapping for all nodes depending on
     * configurable normal distribution
     *
     * @param NumberOfAllNodes  number of all nodes in all variants
     * @param Percentage        percentage of mapping
     * @param standardDeviation standard deviation value (percentage
     *                          of @param(Percentage)
     *                          * @Param(NumberOfAllNodes)/100)
     */
    private static int[] getDistribution(int NumberOfAllNodes, double Percentage, double standardDeviation) {
        // adapt the standard diviation with the percentage of mapping and the number of
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
                // to regenerate a new index in range of array size to not lossing the
                // percentage
                i--;
            }
        }
        return distArray;
    }
}
