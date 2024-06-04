package org.variantsync.boosting.eval;

import de.hub.mse.variantsync.boosting.TraceBoosting;
import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.ecco.Feature;
import de.hub.mse.variantsync.boosting.ecco.MainTree;
import de.hub.mse.variantsync.boosting.position.ProductPosition;
import de.hub.mse.variantsync.boosting.product.Product;
import org.logicng.formulas.Formula;
import org.prop4j.Node;
import org.tinylog.Logger;
import org.variantsync.functjonal.Result;
import org.variantsync.vevos.simulation.util.io.CaseSensitivePath;
import org.variantsync.vevos.simulation.variability.pc.Artefact;
import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author sandra
 */
public class Evaluator {
    Map<Integer, char[]> binaryMap = new HashMap<>();
    Map<String, boolean[]> truthTable = new HashMap<>();
    Map<String, Double> truePositiveAmount = new HashMap<>();
    private final TraceBoosting ecco_light;
    final int noOfFeatures; // where does this come from?
    final int tableSize;

    public Evaluator(final TraceBoosting ecco_light) {
        this.ecco_light = ecco_light;
        this.noOfFeatures = this.ecco_light.getAllFeatures().size();
        tableSize = (int) Math.pow(2, noOfFeatures);
        for (int i = 0; i < tableSize; i++) {
            binaryMap.put(i, String.format("%0" + noOfFeatures + "d", Integer.parseInt(Integer.toBinaryString(i)))
                    .toCharArray());
        }
    }

    public double[] compare(MainTree maintree, Map<String, GroundTruth> product_pc, Set<String> relevantFeatures,
            int strip) {
        double[] return_value = { 0.0, 0.0, 0.0, 0.0 };
        int counter = 0;
        String groundTruthMapping;
        String eccoMapping;
        System.out.println("number of nodes: " + maintree.getTree().getAstNodes().size());

        long evaluatedNodes = 0;
        for (ASTNode node : maintree.getTree().getAstNodes()) {
            for (ProductPosition productPosition : maintree.getProductPositions(node)) {
                Product product = productPosition.product;
                assert product != null;

                Artefact result = product_pc.get(product.getName()).variant();
                Path positionPath = productPosition.filePath().subpath(strip,
                        productPosition.filePath().getNameCount());

                Node pc;
                if (productPosition.lineNumber() < 1) {
                    Result<Node, Exception> r = result.getPresenceConditionOf(new CaseSensitivePath(positionPath));
                    if (r.isFailure()) {
                        // Ecco also considers folders as nodes
                        // Folders have no value in the ground truth and thus no presence condition
                        // We simply skip these cases
                        continue;
                    } else {
                        pc = r.getSuccess();
                    }
                } else {
                    // positionPath =
                    // Path.of(positionPathString.substring(positionPathString.indexOf("src")));
                    pc = result.getPresenceConditionOf(new CaseSensitivePath(positionPath),
                            productPosition.lineNumber())
                            .expect("Not able to load PC for " + positionPath + " line "
                                    + productPosition.lineNumber());
                }

                Set<String> undesiredFeatures = pc.getUniqueContainedFeatures();
                int undesiredFeaturesBefore = undesiredFeatures.size();
                if (undesiredFeaturesBefore == 1 && undesiredFeatures.stream().allMatch(f -> f.equals("True"))) {
                    continue;
                }
                // Determine which
                undesiredFeatures.removeAll(relevantFeatures);
                if (!undesiredFeatures.isEmpty()) {
                    // We skip the evaluation of this node, because features in its PC are undesired
                    // for the evaluation
                    continue;
                }

                groundTruthMapping = pc.toCNF().toString();
                Formula f = maintree.getMapping(productPosition);
                if (f != null) {
                    if (groundTruthMapping.equals("$true") | groundTruthMapping.equals("True")) {
                        groundTruthMapping = "true";
                    }
                    if (groundTruthMapping.equals("true")) {
                        // Skip all root mappings
                        continue;
                    }
                    groundTruthMapping = groundTruthMapping.replaceAll("-", "!");

                    eccoMapping = f.cnf().toString();
                    eccoMapping = eccoMapping.replaceAll("~", "!");

                    if (eccoMapping.equals("$true") | eccoMapping.equals("True")) {
                        eccoMapping = "true";
                    }

                    double[] results = compareMappings(groundTruthMapping, eccoMapping);
                    return_value = new double[] { return_value[0] + results[0], return_value[1] + results[1],
                            return_value[2] + results[2], return_value[3] + results[3] };

                }
                counter++;

                if ((counter % 20000) == 0)
                    System.out.println("counter " + counter);
            }
            evaluatedNodes++;
        }

        Logger.info("Evaluated the mapping of " + evaluatedNodes + " nodes.");
        return new double[] { return_value[0] / counter, return_value[1] / counter,
                return_value[2] / counter, return_value[3] / counter };
    }

    public double[] compareMappings(String groundTruth, String mapping) {
        // if (!groundTruth.equals(mapping))
        // System.out.println("they are not the same: " + groundTruth + " and " +
        // mapping);
        // if (mapping.equals("Root"))
        // mapping = "true";

        // if (groundTruth.equals(mapping) && truePositiveAmount.get(mapping) !=null){
        // double tps = truePositiveAmount.get(mapping);
        // return new double[] {tps/tableSize, tableSize-tps/tableSize, 0, 0};
        // }

        boolean[] gt_bool = eval_truth_table(groundTruth);
        boolean[] mp_bool = eval_truth_table(mapping);

        double tp = 0.0;
        double tn = 0.0;
        double fp = 0.0;
        double fn = 0.0;

        double gtSize = gt_bool.length;
        for (int i = 0; i < gtSize; i++) {
            if (gt_bool[i] == mp_bool[i]) {
                if (gt_bool[i]) {
                    tp += 1.0;
                } else {
                    tn += 1.0;
                }
            } else {
                if (gt_bool[i]) {
                    fn += 1.0;
                } else
                    fp += 1.0;
            }
        }
        truePositiveAmount.putIfAbsent(mapping, tp);
        return new double[] { tp / gtSize, tn / gtSize, fp / gtSize, fn / gtSize };
    }

    public boolean[] eval_truth_table(String mapping) {
        if (truthTable.get(mapping) != null)
            return truthTable.get(mapping);
        else {
            boolean[] retList = new boolean[tableSize];
            for (int i = 0; i < tableSize; i++) {
                retList[i] = eval_truth_line(mapping, i);
            }
            truthTable.put(mapping, retList);
            return retList;
        }
    }

    /**
     * Checks for the mapping in a line of the truth table whehter it evaluates to
     * true
     * Truth table looks like this, for instance, line and binary represenation
     * 0 00000
     * 1 00001
     * 2 00010
     * 3 00011
     * 4 ...
     * And the mapping may be string like this "!Root and FeatureA"
     */
    public boolean eval_truth_line(String mapping, int lineNumber) {
        char[] bin = binaryMap.get(lineNumber);
        int i = -1;
        for (Feature feature : this.ecco_light.getAllFeatures()) {
            String featureName = feature.getName();
            String negatedFeatureName = "!" + featureName;
            i++;
            if (bin[i] == '0') {
                if (!mapping.contains(negatedFeatureName) && mapping.contains(featureName)) {
                    // The mapping relies on this feature and the feature is set to false, thus the
                    // mapping evaluates to false
                    return false;
                }
            } else if (bin[i] == '1') {
                if (mapping.contains(negatedFeatureName)) {
                    // The mapping expects that this feature is false, but the feature is set to
                    // true, thus, the mapping evaluates to false
                    return false;
                }
            }
        }

        // In all other cases, the mapping evaluates to true
        return true;
    }

    public static boolean eval_expression(String expression) {
        int j = expression.indexOf('&', 0);
        int i = 0;
        while (j >= 0) {
            if (expression.substring(i, j).contains("true")) {
                // String[] split_and = expression.split(" & ");
                // for (int k = 0; k < split_and.length; k++) {
                // if (split_and[k].contains("true")) { // in case of a disjunction, one true is
                // enough to make mapping true
                // split_and[k] = "true"; // not necessary
                // continue;
                i = j + 1;
                j = expression.indexOf('&', i);
            } else {
                return false;
            }
        }
        return true;
    }

    public static double precision(double tp, double fp) {
        return tp / (tp + fp);
    }

    public static double recall(double tp, double fn) {
        return tp / (tp + fn);
    }

    public static double[] scoresFunc(double tp, double fp, double fn) {
        double[] ret_list = new double[3];
        ret_list[0] = precision(tp, fp);
        ret_list[1] = recall(tp, fn);
        ret_list[2] = f1score(ret_list[0], ret_list[1]);
        return ret_list;
    }

    public static double f1score(double prec, double rec) {
        return 2 * prec * rec / (prec + rec);
    }

}
