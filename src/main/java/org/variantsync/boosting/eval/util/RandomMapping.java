package org.variantsync.boosting.eval.util;

import org.variantsync.boosting.TraceBoosting;
import org.variantsync.boosting.datastructure.*;
import org.variantsync.boosting.product.Variant;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.prop4j.Node;
import org.tinylog.Logger;
import org.variantsync.vevos.simulation.util.io.CaseSensitivePath;
import org.variantsync.vevos.simulation.variability.pc.Artefact;
import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;

import java.util.*;

/**
 * Class for determining random mapping distributions.
 */
public class RandomMapping {
    /**
     * Distributes mappings to products based on a given percentage.
     *
     * This method takes a list of preproducts, a map of ground truth data, a
     * percentage value, and a strip value as input parameters.
     * It then iterates through each product in the preproducts list, retrieves the
     * ground truth data for that product from the gtMap,
     * and applies a distribution based on the given percentage and strip values.
     *
     * @param preproducts a list of products to distribute mappings to
     * @param gtMap       a map containing ground truth data for each product
     * @param percentage  the percentage of mapping to apply
     * @param strip       the strip value to use for distribution
     */
    public static void distributeMappings(List<Variant> preproducts, Map<String, GroundTruth> gtMap, int percentage,
            int strip) {
        for (Variant product : preproducts) {
            // loading PCs for a certain product, PC from VEVOS
            Artefact groundTruth = gtMap.get(product.getName()).variant();
            // get Distribution
            applyDistribution(product, groundTruth, percentage, strip);
        }
    }

    /**
     * Generates a mapping distribution for a given product variant.
     *
     * This method takes a Product object as input and calculates a mapping
     * distribution for the variant based on the number of AST nodes in the product.
     * The distribution is represented as an array of integers, where each element
     * corresponds to a node in the AST and contains a randomly generated value of
     * either 0 or 1.
     *
     * @param product The Product object for which the mapping distribution is to be
     *                generated.
     * @return An array of integers representing the mapping distribution for the
     *         variant, with each element containing a randomly generated value of 0
     *         or 1.
     */
    private static Integer[] getDistribution(Variant product) {
        int nodesN = product.getProductAst().getAstNodes().size();
        Integer[] dist = new Integer[nodesN];
        Arrays.fill(dist, 0);
        Random random = new Random();

        // Populate the array with random values (0 or 1)
        for (int i = 0; i < nodesN; i++) {
            dist[i] = random.nextInt(2); // Generates either 0 or 1
        }

        return dist;
    }

    /**
     * Applies a distribution of mappings to a given product based on a ground truth
     * artefact.
     *
     * This method iterates through the ASTNodes of the given Product, filters nodes
     * with line numbers less than 1, and retrieves the presence condition of each
     * node from the ground truth.
     * Then, it shuffles the candidate nodes (whose presence condition is not 'true')
     * to randomize them and assigns mappings to a given percentage of the nodes.
     * The mappings are populated with random binary values (0 or 1) based on the
     * presence condition of each node.
     *
     * @param product     The Product object to apply the distribution to
     * @param groundTruth The Artefact object representing the ground truth
     * @param percentage  The percentage of nodes to assign mappings to
     * @param strip       The number of path elements to strip from the file path
     * @throws IllegalArgumentException if the percentage is not within the range of
     *                                  0 to 100
     * @throws NullPointerException     if either product or groundTruth is null
     */
    private static void applyDistribution(Variant product, Artefact groundTruth, int percentage, int strip) {
        // Nodes that can be mapped
        List<ASTNode> candidateNodes = new ArrayList<>();
        Map<ASTNode, Node> pcMap = new HashMap<>();
        for (var astNode : product.getProductAst().getAstNodes()) {
            var position = astNode.getStartPosition();
            if (position.lineNumber() < 1) {
                continue;
            }
            var path = new CaseSensitivePath(position.filePath().subpath(strip, position.filePath().getNameCount()));
            var result = groundTruth.getPresenceConditionOf(path, position.lineNumber());

            if (result.isFailure()) {
                Logger.warn(result.getFailure());
                continue;
            }

            var pc = result.getSuccess().toCNF();
            if (!pc.toString().equalsIgnoreCase("true")) {
                pcMap.put(astNode, pc);
                candidateNodes.add(astNode);
            }
        }

        // shuffle the candidate nodes to randomly assign mappings
        Collections.shuffle(candidateNodes);

        int numberOfMappedNodes = (percentage * candidateNodes.size()) / 100;
        Logger.info("Assigning mappings to "
                + numberOfMappedNodes + " of "
                + candidateNodes.size()
                + " variable nodes (" + percentage + "%)");

        // Assign a mapping to the first number of nodes in the randomly shuffled set
        // according to the determined percentage.
        // The mapping simulates the proactive knowledge
        int brokenFormulas = 0;
        for (int i = 0; i < numberOfMappedNodes; i++) {
            var node = candidateNodes.get(i);
            var pc = pcMap.get(node);
            try {
                Formula formula = new PropositionalParser(TraceBoosting.f)
                        .parse(pc.toCNF().toString().replaceAll("-", "~"));
                node.setMapping(formula);
            } catch (ParserException e) {
                // Few formulas are broken, we skip them. This is a minor bias in the eval
                brokenFormulas++;
            }
        }
        Logger.debug("Found " + brokenFormulas + " broken formulas.");
    }

}
