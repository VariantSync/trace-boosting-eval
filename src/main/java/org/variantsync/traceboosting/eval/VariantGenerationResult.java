package org.variantsync.traceboosting.eval;

import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;

import java.nio.file.Path;
import java.util.Map;

public record VariantGenerationResult(Path variantGenerationDir, Map<String, GroundTruth> variantGroundTruthMap,
                                      Map<String, Path> variantConfigFileMap) {
}
