package org.variantsync.boosting.eval;

import de.hub.mse.variantsync.boosting.TraceBoosting;
import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.product.Product;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.prop4j.Node;
import org.tinylog.Logger;
import org.variantsync.vevos.simulation.util.io.CaseSensitivePath;
import org.variantsync.vevos.simulation.variability.pc.Artefact;
import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;

import java.util.*;

public class RandomMapping {
    /**
     * Mapping function
     *
     * @param percentage percentage of mapping
     * @return products with mapped nodes
     */
    public static void distributeMappings(List<Product> preproducts, Map<String, GroundTruth> gtMap, int percentage,
            int strip) {
        for (Product product : preproducts) {
            // loading PCs for a certen product, PC from VEVOS
            Artefact groundTruth = gtMap.get(product.getName()).variant();
            // get Distribution
            applyDistribution(product, groundTruth, percentage, strip);
        }
    }

    /**
     * generate mapping Distribution for a variant
     */
    private static Integer[] getDistribution(Product product) {
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

    private static void applyDistribution(Product product, Artefact groundTruth, int percentage, int strip) {
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

        // shuffle the target nodes to randomize
        Collections.shuffle(candidateNodes);

        int numberOfMappedNodes = (percentage * candidateNodes.size()) / 100;
        Logger.info("Assigning mappings to "
                + numberOfMappedNodes + " of "
                + candidateNodes.size()
                + " variable nodes (" + percentage + "%)");

        // Populate the nodes with random values (0 or 1)
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
