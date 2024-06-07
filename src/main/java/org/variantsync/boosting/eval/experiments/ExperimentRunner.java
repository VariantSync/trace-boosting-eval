package org.variantsync.boosting.eval.experiments;

import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;
import org.variantsync.boosting.TraceBoosting;
import org.variantsync.boosting.datastructure.Feature;
import org.variantsync.boosting.datastructure.MainTree;
import org.variantsync.boosting.eval.util.RandomMapping;
import org.variantsync.boosting.eval.util.VEVOSUtilities;
import org.variantsync.boosting.eval.util.VariantGenerationResult;
import org.variantsync.boosting.parsing.ESupportedLanguages;
import org.variantsync.boosting.product.Product;
import org.variantsync.boosting.product.ProductPassport;
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

/**
 * This abstract class serves as a base class for running our experiments.
 */
public abstract class ExperimentRunner {
    protected Config config;
    private static final String VARIANT_GENERATION_DIR = "variants";
    private static final String CONFIG_GENERATION_DIR = "configs";
    private static final String ARGOUML_REPO_NAME = "argouml-spl";

    protected String configPath = "";

    protected int ex_repeat;
    protected int[] percentages;
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

    /**
     * Constructor for ExperimentRunner class.
     * Initializes ExperimentRunner object with the provided Config object.
     *
     * @param config Config object containing experiment parameters
     */
    protected ExperimentRunner(Config config) {
        this.config = config;
        ex_repeat = config.experimentRepeats();
        percentages = config.experimentMapping();
        sampleSizes = config.sampleSize();
        maxFeatures = config.maxFeatures();
    }

    /**
     * Loads a subject-specific configuration based on the given SPL name and path
     * to the configuration file.
     * 
     * @param splName      the SPL name for which the configuration is being loaded
     * @param pathToConfig the path to the configuration file
     * @return a Config object representing the loaded configuration
     */
    public static Config loadSubjectSpecificConfig(String splName, Path pathToConfig) {
        return new Config(pathToConfig, splName);
    }

    /**
     * Prepares a new set of variants for a given software product line (SPL)
     * repository, based on a list of SPL commits and a specified number of
     * variants.
     *
     * @param splRepoPath  the path to the SPL repository
     * @param splCommitGTs the list of SPL commits with ground truths
     * @param nVariants    the number of variants to generate
     * @return a VariantGenerationResult object containing the generated variants,
     *         ground truths, and configuration files
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Conducts an experiment using the given variant generation result, percentage,
     * and standard deviation.
     * 
     * @param variantGenerationResult The result of the variant generation process
     * @param percentage              The percentage of the experiment to be
     *                                conducted
     * @param standardDeviation       The standard deviation of the experiment
     * @return An array of double values representing the results of the experiment
     */
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

    /**
     * Initializes the TraceBoosting with the given parameters.
     * 
     * @param variantsDirectory the directory containing the variant files
     * @param gtMap             a map of variant IDs to their corresponding ground
     *                          truth objects
     * @param configFileMap     a map of configuration file names to their
     *                          corresponding paths
     * @return a TraceBoosting object initialized with the provided parameters
     */
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
        traceBoosting.setNumThreads(config.numThreads());
        System.out.println("initial done");

        return traceBoosting;
    }

    /**
     * Creates a JsonObject with properties for the current experiment.
     * 
     * @return JsonObject - a JsonObject containing the properties for the current
     *         experiment
     */
    protected JsonObject createJSONProperties() {

        JsonObject experimentprop = new JsonObject();
        experimentprop.addProperty("Percentage scenarios", Arrays.toString(percentages));
        experimentprop.addProperty("Variants", Arrays.toString(sampleSizes));
        experimentprop.addProperty("Runs", ex_repeat);

        final JsonObject experimentjson = new JsonObject();
        experimentjson.add("experiment_properties", experimentprop);
        return experimentjson;
    }

    /**
     * Creates a JSON object representing a single run with the given scores.
     *
     * @param score an array of double values representing the accuracy, precision,
     *              recall, f1-score, and time of the run
     * @return a JsonObject containing the scores for accuracy, precision, recall,
     *         f1-score, and time
     */
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
