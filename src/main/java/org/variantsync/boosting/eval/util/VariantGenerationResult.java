package org.variantsync.boosting.eval.util;

import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;

import java.nio.file.Path;
import java.util.Map;

/**
 * Represents the result of variant generation, including the directory where
 * the variants were generated, a map of variant names to their corresponding
 * ground truth objects, and a map of variant names to their corresponding
 * configuration file paths.
 *
 * @param variantGenerationDir  The directory where the variants were generated
 * @param variantGroundTruthMap A map of variant names to their corresponding
 *                              ground truth objects
 * @param variantConfigFileMap  A map of variant names to their corresponding
 *                              configuration file paths
 */
public record VariantGenerationResult(Path variantGenerationDir, Map<String, GroundTruth> variantGroundTruthMap,
        Map<String, Path> variantConfigFileMap) {
}
