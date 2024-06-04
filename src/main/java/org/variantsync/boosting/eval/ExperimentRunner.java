package org.variantsync.boosting.eval;

import com.google.gson.JsonObject;
import de.hub.mse.variantsync.boosting.TraceBoosting;
import de.hub.mse.variantsync.boosting.ecco.Feature;
import de.hub.mse.variantsync.boosting.ecco.MainTree;
import de.hub.mse.variantsync.boosting.parsing.ESupportedLanguages;
import de.hub.mse.variantsync.boosting.product.Product;
import de.hub.mse.variantsync.boosting.product.ProductPassport;
import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;
import org.variantsync.vevos.simulation.feature.sampling.Sample;
import org.variantsync.vevos.simulation.variability.SPLCommit;
import org.variantsync.vevos.simulation.variability.pc.SourceCodeFile;
import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;
import org.variantsync.vevos.simulation.variability.pc.options.ArtefactFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ExperimentRunner {
    protected Config config;
    private static final String REPO_CLONE_DIR = "repos";
    private static final String VARIANT_GENERATION_DIR = "variants";
    private static final String CONFIG_GENERATION_DIR = "configs";
    private static final String ARGOUML_REPO_NAME = "argouml-spl";

    protected String configPath = "";

    protected int ex_repeat;
    protected int[] percentages;
    // build variant sampling scenarios ref
    int[] sampleSizes;
    int maxFeatures;

    private static final ArtefactFilter<SourceCodeFile> ARGOUML_FILE_FILTER = sourceCodeFile -> sourceCodeFile.getFile()
            .path().startsWith("src");
    private static final ArtefactFilter<SourceCodeFile> OTHER_FILE_FILTER = sourceCodeFile -> {
        String[] validExtensions = new String[] { ".c", ".h", ".cpp", ".hpp" };
        String path = sourceCodeFile.getFile().path().getFileName().toString();
        for (String extension : validExtensions) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    };

    protected ExperimentRunner(Config config) {
        this.config = config;
        ex_repeat = config.experimentRepeats();
        percentages = config.experimentMapping();
        sampleSizes = config.sampleSize();
        maxFeatures = config.maxFeatures();
    }

    protected static Config loadSubjectSpecificConfig(String splName, Path pathToConfig) {
        return new Config(pathToConfig, splName);
    }

    public VariantGenerationResult prepareVariants(Path splRepoPath, List<SPLCommit> splCommitGTs, int nVariants)
            throws IOException {
        Path splName = splRepoPath.getFileName();
        Logger.info("Preparing new set of " + nVariants + " variants for " + splName);
        ArtefactFilter<SourceCodeFile> fileFilter;
        if (splName.startsWith(ARGOUML_REPO_NAME)) {
            fileFilter = ARGOUML_FILE_FILTER;
        } else {
            fileFilter = OTHER_FILE_FILTER;
        }

        var utilities = new VEVOSUtilities();
        Path variantGenerationDir = config.experimentWorkDirVevos().resolve(VARIANT_GENERATION_DIR);
        Path configGenerationDir = config.experimentWorkDirVevos().resolve(CONFIG_GENERATION_DIR);
        if (Files.exists(variantGenerationDir)) {
            FileUtils.deleteDirectory(variantGenerationDir.toFile());
        }
        if (Files.exists(configGenerationDir)) {
            FileUtils.deleteDirectory(configGenerationDir.toFile());
        }

        if (Files.exists(config.experimentWorkDirBoosting())) {
            FileUtils.deleteDirectory(config.experimentWorkDirBoosting().toFile());
        }

        // Sample and generate variants for each commit
        for (SPLCommit commitGT : splCommitGTs) {
            Sample variantSample = utilities.sampleVariants(commitGT, nVariants, maxFeatures);
            Map<String, GroundTruth> gtMap = utilities.generateVariants(splRepoPath, variantGenerationDir, commitGT,
                    variantSample, fileFilter);
            Map<String, Path> configFileMap = utilities.configurationsToFile(configGenerationDir, variantSample);
            Logger.info("Generated variants and received ground truths for " + gtMap.size() + " variants");

            config.setStrip(variantGenerationDir.getNameCount() + 1);
            // We can be sure that there is exactly one commit, because this is how we
            // defined our dataset
            return new VariantGenerationResult(variantGenerationDir, gtMap, configFileMap);
        }
        throw new IllegalStateException("UNREACHABLE");
    }

    public double[] conductExperiment(VariantGenerationResult variantGenerationResult, int percentage,
            double standardDeviation) {
        TraceBoosting traceBoosting = initBoosting(
                variantGenerationResult.variantGenerationDir(),
                variantGenerationResult.variantGroundTruthMap(),
                variantGenerationResult.variantConfigFileMap());

        List<Product> products = traceBoosting.getProducts();

        // mapping
        RandomMapping.distributeMappings(products, variantGenerationResult.variantGroundTruthMap(), percentage,
                config.getStrip());

        System.out.println("Boosting...");
        long start = System.currentTimeMillis();
        MainTree mainTree = traceBoosting.computeMappings();
        long elapsedTimeMillis = System.currentTimeMillis() - start;
        System.out.println("comparing... ");
        // Features that have been sampled for the products and are implemented by at
        // least one product. Only these features are relevant for the evaluation
        Set<String> relevantFeatures = products.stream().flatMap(p -> p.getFeatures().stream().map(Feature::getName))
                .collect(Collectors.toSet());
        Evaluator evaluator = new Evaluator(traceBoosting);
        double[] results = evaluator.compare(mainTree, variantGenerationResult.variantGroundTruthMap(),
                relevantFeatures, config.getStrip());
        System.out.println("scoring.... ");
        double[] funcScores = Evaluator.scoresFunc(results[0], results[2], results[3]);

        return new double[] { results[0] + results[1], funcScores[0], funcScores[1], funcScores[2], elapsedTimeMillis };
    }

    public TraceBoosting initBoosting(Path variantsDirectory, Map<String, GroundTruth> gtMap,
            Map<String, Path> configFileMap) {
        List<ProductPassport> productPassports = new ArrayList<>();
        for (Map.Entry<String, GroundTruth> gtEntry : gtMap.entrySet()) {
            String variantName = gtEntry.getKey();
            productPassports.add(new ProductPassport(variantName, variantsDirectory.resolve(variantName),
                    configFileMap.get(variantName)));
        }
        System.out.println("build TraceBoosting object");
        TraceBoosting traceBoosting = new TraceBoosting(productPassports, config.experimentWorkDirBoosting(),
                ESupportedLanguages.LINES);
        System.out.println("initial done");

        return traceBoosting;
    }

    protected JsonObject createJSONProperties() {
        JsonObject experimentprop = new JsonObject();
        experimentprop.addProperty("Percentage scenarios", Arrays.toString(percentages));
        experimentprop.addProperty("Variants", Arrays.toString(sampleSizes));
        experimentprop.addProperty("Runs", ex_repeat);

        final JsonObject experimentjson = new JsonObject();
        experimentjson.add("experiment_properties", experimentprop);
        return experimentjson;
    }

    protected JsonObject createJSONSingleRun(double[] score) {
        JsonObject runJson = new JsonObject();
        runJson.addProperty("accuracy", (score[0]));
        runJson.addProperty("precision", score[1]);
        runJson.addProperty("recall", score[2]);
        runJson.addProperty("f1-score", score[3]);
        runJson.addProperty("time", score[4]);
        return runJson;
    }

}
