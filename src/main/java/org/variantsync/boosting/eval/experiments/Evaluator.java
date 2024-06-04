package org.variantsync.boosting.eval.experiments;

import org.logicng.formulas.Formula;
import org.prop4j.Node;
import org.tinylog.Logger;
import org.variantsync.boosting.TraceBoosting;
import org.variantsync.boosting.datastructure.ASTNode;
import org.variantsync.boosting.datastructure.Feature;
import org.variantsync.boosting.datastructure.MainTree;
import org.variantsync.boosting.position.ProductPosition;
import org.variantsync.boosting.product.Product;
import org.variantsync.functjonal.Result;
import org.variantsync.vevos.simulation.util.io.CaseSensitivePath;
import org.variantsync.vevos.simulation.variability.pc.Artefact;
import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class for evaluating the result of a feature trace run.
 * 
 */
public class Evaluator {
    Map<Integer, char[]> binaryMap = new HashMap<>();
    Map<String, boolean[]> truthTable = new HashMap<>();
    Map<String, Double> truePositiveAmount = new HashMap<>();
    private final TraceBoosting traceBoosting;
    final int noOfFeatures;
    final int tableSize;

    /**
     * Constructor for the Evaluator class.
     * Initializes the Evaluator with the given TraceBoosting object.
     * Calculates the number of features in the TraceBoosting object and sets the
     * table size accordingly.
     * Creates a binary map with keys representing binary numbers up to the table
     * size.
     * 
     * @param traceBoosting The TraceBoosting object to initialize the Evaluator
     *                      with.
     */
    public Evaluator(final TraceBoosting traceBoosting) {
        this.traceBoosting = traceBoosting;
        this.noOfFeatures = this.traceBoosting.getAllFeatures().size();
        tableSize = (int) Math.pow(2, noOfFeatures);
        for (int i = 0; i < tableSize; i++) {
            binaryMap.put(i,
                    String.format("{{input}}" + noOfFeatures + "d", Integer.parseInt(Integer.toBinaryString(i)))
                            .toCharArray());
        }
    }

    /**
     * Compares the given MainTree with the ground truth and evaluates the boosted
     * traces.
     * 
     * @param maintree         The MainTree to compare with the ground truth
     * @param productPC        A map containing product names as keys and
     *                         corresponding GroundTruth objects as values
     * @param relevantFeatures A set of relevant features to consider during
     *                         comparison
     * @param strip            the number of path elements to cut off at the start
     *                         of paths
     * @return An array of doubles representing the evaluation results for the
     *         boosted traces
     * @throws IllegalArgumentException if maintree is null or productPC is empty
     */
    public double[] compare(MainTree maintree, Map<String, GroundTruth> productPC, Set<String> relevantFeatures,
            int strip) {
        double[] return_value = { 0.0, 0.0, 0.0, 0.0 };
        int counter = 0;
        String groundTruthMapping;
        String boostedMapping;
        System.out.println("number of nodes: " + maintree.getTree().getAstNodes().size());

        long evaluatedNodes = 0;
        for (ASTNode node : maintree.getTree().getAstNodes()) {
            for (ProductPosition productPosition : maintree.getProductPositions(node)) {
                Product product = productPosition.product;
                assert product != null;

                Artefact result = productPC.get(product.getName()).variant();
                Path positionPath = productPosition.filePath().subpath(strip,
                        productPosition.filePath().getNameCount());

                Node pc;
                if (productPosition.lineNumber() < 1) {
                    Result<Node, Exception> r = result.getPresenceConditionOf(new CaseSensitivePath(positionPath));
                    if (r.isFailure()) {
                        // Our algorithm also considers folders as nodes Folders have no value in the
                        // ground truth and thus no presence condition We simply skip these cases
                        continue;
                    } else {
                        pc = r.getSuccess();
                    }
                } else {
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

                    boostedMapping = f.cnf().toString();
                    boostedMapping = boostedMapping.replaceAll("~", "!");

                    if (boostedMapping.equals("$true") | boostedMapping.equals("True")) {
                        boostedMapping = "true";
                    }

                    double[] results = compareMappings(groundTruthMapping, boostedMapping);
                    return_value = new double[] { return_value[0] + results[0], return_value[1] + results[1],
                            return_value[2] + results[2], return_value[3] + results[3] };

                }
                counter++;

                if ((counter % 20000) == 0)
                    System.out.println("still in progress... ");
            }
            evaluatedNodes++;
        }

        Logger.info("Evaluated the mapping of " + evaluatedNodes + " nodes.");
        return new double[] { return_value[0] / counter, return_value[1] / counter,
                return_value[2] / counter, return_value[3] / counter };
    }

    /**
     * Compares two mappings and calculates the true positive, true negative, false
     * positive, and false negative rates.
     * 
     * @param groundTruth The ground truth mapping as a string
     * @param mapping     The mapping to be compared against the ground truth
     * @return An array of doubles containing the true positive rate, true negative
     *         rate, false positive rate, and false negative rate
     * 
     * @throws IllegalArgumentException if the groundTruth and mapping strings are
     *                                  not of the same length
     */
    public double[] compareMappings(String groundTruth, String mapping) {
        boolean[] gt_bool = evalTruthTable(groundTruth);
        boolean[] mp_bool = evalTruthTable(mapping);

        double tp = 0.0;
        double tn = 0.0;
        double fp = 0.0;
        double fn = 0.0;

        double gtSize = gt_bool.length;
        if (gtSize != mp_bool.length) {
            throw new IllegalArgumentException("Ground truth and mapping strings must be of the same length");
        }

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
                } else {
                    fp += 1.0;
                }
            }
        }

        truePositiveAmount.putIfAbsent(mapping, tp);
        return new double[] { tp / gtSize, tn / gtSize, fp / gtSize, fn / gtSize };
    }

    /**
     * Evaluates the truth table for a given mapping and returns the corresponding
     * boolean array.
     * If the truth table for the given mapping has already been computed, it is
     * retrieved from a cache.
     * If not, the truth table is computed and stored in the cache for future use.
     *
     * @param mapping the mapping for which the truth table needs to be evaluated
     * @return the boolean array representing the truth table for the given mapping
     */
    public boolean[] evalTruthTable(String mapping) {
        if (truthTable.get(mapping) != null)
            return truthTable.get(mapping);
        else {
            boolean[] retList = new boolean[tableSize];
            for (int i = 0; i < tableSize; i++) {
                retList[i] = evalTruthLine(mapping, i);
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
    public boolean evalTruthLine(String mapping, int lineNumber) {
        char[] bin = binaryMap.get(lineNumber);
        int i = -1;
        for (Feature feature : this.traceBoosting.getAllFeatures()) {
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

    /**
     * Evaluates a logical expression containing only the 'AND' operator.
     * 
     * @param expression a String representing the logical expression to be
     *                   evaluated
     * @return true if all sub-expressions separated by the 'AND' operator evaluate
     *         to true, false otherwise
     * @throws IllegalArgumentException if the input expression is null or empty
     */
    public static boolean evalExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("Input expression cannot be null or empty");
        }

        int j = expression.indexOf('&', 0);
        int i = 0;
        while (j >= 0) {
            if (expression.substring(i, j).contains("true")) {
                i = j + 1;
                j = expression.indexOf('&', i);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the precision of a model based on the true positives (tp) and
     * false positives (fp).
     * Precision is defined as the ratio of true positives to the sum of true
     * positives and false positives.
     *
     * @param tp The number of true positive predictions.
     * @param fp The number of false positive predictions.
     * @return The precision of the model as a double value.
     * @throws IllegalArgumentException if tp or fp is negative.
     */
    public static double precision(double tp, double fp) {
        if (tp < 0 || fp < 0) {
            throw new IllegalArgumentException("True positives (tp) and false positives (fp) must be non-negative.");
        }

        return tp / (tp + fp);
    }

    /**
     * Calculates the recall score for a binary classification model.
     * 
     * Recall, also known as sensitivity, is the ratio of true positives to the sum
     * of true positives and false negatives.
     * 
     * @param tp The number of true positive predictions made by the model.
     * @param fn The number of false negative predictions made by the model.
     * @return The recall score, a value between 0 and 1.
     * @throws IllegalArgumentException if tp or fn is negative.
     */
    public static double recall(double tp, double fn) {
        if (tp < 0 || fn < 0) {
            throw new IllegalArgumentException("True positives and false negatives must be non-negative.");
        }

        return tp / (tp + fn);
    }

    /**
     * Calculates precision, recall, and F1 score based on true positives, false
     * positives, and false negatives.
     * 
     * @param tp The number of true positives.
     * @param fp The number of false positives.
     * @param fn The number of false negatives.
     * @return An array of doubles containing precision, recall, and F1 score in
     *         that order.
     * @throws IllegalArgumentException if any of the input values are negative.
     */
    public static double[] scoresFunc(double tp, double fp, double fn) {
        if (tp < 0 || fp < 0 || fn < 0) {
            throw new IllegalArgumentException("Input values cannot be negative.");
        }

        double[] ret_list = new double[3];
        ret_list[0] = precision(tp, fp);
        ret_list[1] = recall(tp, fn);
        ret_list[2] = f1score(ret_list[0], ret_list[1]);
        return ret_list;
    }

    /**
     * Calculates the F1 score based on precision and recall values.
     * 
     * @param prec The precision value, a double between 0 and 1.
     * @param rec  The recall value, a double between 0 and 1.
     * @return The F1 score, a double between 0 and 1.
     * @throws IllegalArgumentException if prec or rec is not between 0 and 1.
     */
    public static double f1score(double prec, double rec) {
        if (prec < 0 || prec > 1 || rec < 0 || rec > 1) {
            throw new IllegalArgumentException("Precision and recall values must be between 0 and 1.");
        }

        return 2 * prec * rec / (prec + rec);
    }

}
