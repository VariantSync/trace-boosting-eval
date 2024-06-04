package org.variantsync.boosting.eval;

import de.ovgu.featureide.fm.core.base.IFeature;
import org.tinylog.Logger;
import org.variantsync.diffdetective.datasets.DatasetDescription;
import org.variantsync.diffdetective.load.GitLoader;
import org.variantsync.functjonal.Lazy;
import org.variantsync.vevos.simulation.VEVOS;
import org.variantsync.vevos.simulation.feature.SimpleFeature;
import org.variantsync.vevos.simulation.feature.Variant;
import org.variantsync.vevos.simulation.feature.config.IConfiguration;
import org.variantsync.vevos.simulation.feature.sampling.Sample;
import org.variantsync.vevos.simulation.feature.sampling.SimpleSampler;
import org.variantsync.vevos.simulation.io.Resources;
import org.variantsync.vevos.simulation.io.data.VariabilityDatasetLoader;
import org.variantsync.vevos.simulation.util.io.CaseSensitivePath;
import org.variantsync.vevos.simulation.variability.SPLCommit;
import org.variantsync.vevos.simulation.variability.VariabilityDataset;
import org.variantsync.vevos.simulation.variability.pc.Artefact;
import org.variantsync.vevos.simulation.variability.pc.SourceCodeFile;
import org.variantsync.vevos.simulation.variability.pc.groundtruth.GroundTruth;
import org.variantsync.vevos.simulation.variability.pc.options.ArtefactFilter;
import org.variantsync.vevos.simulation.variability.pc.options.VariantGenerationOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for using the VEVOS benchmark generator.
 */
public class VEVOSUtilities {

    static {
        // Initialize must be called once to initialize vevos internally.
        VEVOS.Initialize();
    }

    private static final Path DATASET_FILE = Path.of("data/datasets.md");

    /**
     * Clones all datasets listed in the default dataset file to the cloneBaseDir.
     * Returns the paths
     * to which the SPLs where cloned.
     **/
    public List<Path> cloneRepositories(Path cloneBaseDir) {
        return cloneRepositories(DATASET_FILE, cloneBaseDir);
    }

    /**
     * Clones all datasets listed in the given dataset file to the cloneBaseDir.
     * Returns the paths
     * to which the SPLs where cloned.
     **/
    public List<Path> cloneRepositories(Path datasetFile, Path cloneBaseDir) {
        Logger.info("Starting experiment initialization.");
        List<DatasetDescription> datasets;
        try {
            datasets = DatasetDescription.fromMarkdown(datasetFile);
        } catch (IOException e) {
            Logger.error("Was not able to load markdown file with the datasets from '" + datasetFile
                    + "'");
            throw new UncheckedIOException(e);
        }

        try {
            Files.createDirectories(cloneBaseDir);
        } catch (IOException e) {
            Logger.error("was not able to create directory for repositories");
            Logger.error(e);
            throw new UncheckedIOException(e);
        }

        List<Path> datasetDirectories = new ArrayList<>();
        for (var dataset : datasets) {
            Path repoDir = cloneBaseDir.resolve(dataset.name());

            if (!repoDir.toFile().exists()) {
                try (var ignored = GitLoader.fromRemote(repoDir, URI.create(dataset.repoURL()))) {
                    Logger.info(String.format("Cloned %s into %s", dataset.name(), repoDir));
                }
                ;
            }
            datasetDirectories.add(repoDir);
        }
        return datasetDirectories;
    }

    /**
     * Loads the ground truth from the given directory and returns all SPLCommit
     * objects that
     * represent successfully loaded ground truths for specific commits.
     **/
    public List<SPLCommit> loadGroundTruth(Path groundTruthPath) {
        // Load VariabilityDataset
        Logger.info("Loading variability dataset.");
        if (!Files.exists(groundTruthPath)) {
            Logger.info(String.format("Found no ground truth for %s", groundTruthPath));
            return new ArrayList<>();
        }

        VariabilityDataset dataset;
        try {
            Resources resourcesInstance = Resources.Instance();
            VariabilityDatasetLoader datasetLoader = new VariabilityDatasetLoader();
            resourcesInstance.registerLoader(VariabilityDataset.class, datasetLoader);
            dataset = resourcesInstance.load(VariabilityDataset.class, groundTruthPath);
            Logger.info("Dataset loaded.");
        } catch (Resources.ResourceIOException e) {
            throw new RuntimeException("Was not able to load dataset.", e);
        }

        // Retrieve pairs/sequences of usable commits
        Logger.info("Retrieving commits");
        return Objects.requireNonNull(dataset).getSuccessCommits();
    }

    /**
     * Sample a desired number of variants for a given SPLCommit object
     **/
    public Sample sampleVariants(SPLCommit commit, int variantCount, int maxFeatures) {
        Logger.debug("Loading feature model.");
        try {
            List<IFeature> features = Files.readAllLines(commit.getFeatureModelPath()).stream().map(SimpleFeature::new)
                    .collect(Collectors.toList());
            Logger.debug("Sampling variants");
            return SimpleSampler.CreateRandomSampler(variantCount, maxFeatures / 2, maxFeatures).sample(features);
        } catch (IOException e) {
            Logger.error("Was not able to read feature names from feature model file.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate all variants in the given sample to variantGenerationDir. The
     * generation is done for a specific SPL of which we require the source code.
     * This SPL is given as a Path to its root directory (i.e., the path to which it
     * was cloned). The generation is done for one commit at a time. Note that the
     * generation directory has to be cleaned before new variants are generated.
     * <p>
     * The method returns a map of variants to their ground truth. The ground truth
     * can be accessed via the artefact() method and contains the presence
     * conditions, feature mappings, etc.
     **/
    public Map<String, GroundTruth> generateVariants(Path pathToSPL, Path variantGenerationDir,
            SPLCommit commit, Sample sample, ArtefactFilter<SourceCodeFile> fileFilter) {
        Map<String, GroundTruth> variantGroundTruths = new HashMap<>();

        for (Variant variant : sample.variants()) {
            try {
                variantGroundTruths.put(variant.getName(),
                        generateVariant(pathToSPL, variantGenerationDir, commit, variant, fileFilter));
            } catch (Exception e) {
                Logger.warn(
                        "Was not able to generate all variants for commit ${currentCommit!!.id()}:\n"
                                + "{}",
                        e);
                Logger.warn("Skipping commit.");
            }
        }
        return variantGroundTruths;
    }

    /**
     * Writes the configurations of all variants in the sample to individual files
     * that are created in the given directory.
     *
     * @param pathToConfigurationDir The directory in which the configuration files
     *                               are to be created
     * @param variantSample          The sample of variants for which the
     *                               configuration files are to be created
     * @return A map of variant to configuration file
     * @throws IOException If creating or writing the files is not possible
     */
    public Map<String, Path> configurationsToFile(Path pathToConfigurationDir, Sample variantSample)
            throws IOException {
        Files.createDirectories(pathToConfigurationDir);

        Map<String, Path> fileMap = new HashMap<>();
        for (Variant variant : variantSample) {
            // We usually work with FeatureIDEConfigurations, which offer details about the
            // features
            IConfiguration configuration = variant.getConfiguration();
            StringBuilder sb = new StringBuilder();
            for (IFeature feature : configuration.getFeatures()) {
                sb.append(feature.getName()).append("\n");
            }

            Path pathToConfigFile = pathToConfigurationDir.resolve(variant.getName() + ".config");
            Files.writeString(pathToConfigFile, sb);
            fileMap.put(variant.getName(), pathToConfigFile);
        }

        return fileMap;
    }

    /**
     * Generate a single variant and returns its ground truth.
     **/
    private GroundTruth generateVariant(Path pathToSPL, Path variantGenerationDir, SPLCommit commit,
            Variant variant, ArtefactFilter<SourceCodeFile> fileFilter) {
        Logger.debug("Generating variant " + variant.getName());
        try {
            Files.createDirectories(variantGenerationDir.resolve(variant.getName()));
        } catch (IOException e) {
            Logger.error("Was not able to create directory for variant: " + variant.getName());
            Logger.error(e);
            throw new RuntimeException();
        }
        final Lazy<Optional<Artefact>> loadPresenceConditions = commit.presenceConditionsFallback();
        final VariantGenerationOptions generationOptions = VariantGenerationOptions.ExitOnError(true, fileFilter);
        final Artefact pcs = loadPresenceConditions.run().orElseThrow();
        var result = pcs.generateVariant(variant, new CaseSensitivePath(pathToSPL),
                new CaseSensitivePath(variantGenerationDir.resolve(variant.getName())),
                generationOptions);

        if (result.isFailure()) {
            Logger.error("Was not able to generate V0 of $variant:\n{}");
            throw new RuntimeException(result.getFailure());

        }

        // writing pcs of the variant
        GroundTruth groundTruth = result.getSuccess();
        return groundTruth;
    }
}
