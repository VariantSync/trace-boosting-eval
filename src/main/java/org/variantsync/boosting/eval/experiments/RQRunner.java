package org.variantsync.boosting.eval.experiments;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.tinylog.Logger;
import org.variantsync.boosting.eval.util.FilesFunc;
import org.variantsync.boosting.eval.util.VariantGenerationResult;
import org.variantsync.vevos.simulation.variability.SPLCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * This class represents a runner for answering our research questions.
 */
public class RQRunner extends ExperimentRunner {

    /**
     * Constructor for the RQRunner class.
     * 
     * @param config The configuration object for the RQRunner.
     * 
     *               This constructor initializes the RQRunner with the provided
     *               configuration object. It sets the configPath to the path of the
     *               evaluation properties file and clears the debug folders in the
     *               data directory.
     */
    public RQRunner(Config config) {
        super(config);
        configPath = System.getProperty("user.dir") + "/data/evaluation.properties";
        FilesFunc.clearDebugFolders(System.getProperty("user.dir") + "/data");
    }

    /**
     * Conducts the experiment by running the boosted feature tracing with different sample
     * sizes and mapping percentages as specified in the given experiment configuration.
     * The experiment results of each run are collected and added in JSON format to a file
     * which serves as outpot.
     *
     * @param splRepoPath        The path to the SPL repository
     * @param commitGroundTruths The list of SPL commits for ground truth comparison
     * @throws IOException If an I/O error occurs while reading or writing files
     */
    public void run(Path splRepoPath, List<SPLCommit> commitGroundTruths) throws IOException {

        // Create JSON properties for the entire experiment
        JsonObject experimentjson = createJSONProperties();

        // Loop on number of sample scenarios
        for (int sampleSize : sampleSizes) {
            Logger.info(sampleSize + " size of variants out of " + this.sampleSizes.length + " samples");
            final JsonObject samplejson = new JsonObject();

            // Loop on repetitions
            for (int runID = 1; runID <= ex_repeat; runID++) {
                final JsonObject runjson = new JsonObject();

                // Generate the same set of variants for all percentages
                VariantGenerationResult variantGenerationResult = prepareVariants(splRepoPath, commitGroundTruths,
                        sampleSize);

                // Loop on percentage scenarios
                for (int mappingPercentage : percentages) {
                    Logger.info("loop mappingPercentage:" + mappingPercentage + " runID=" + runID);

                    // Build runner object and start experiment
                    double[] score = null;
                    while (score == null) {
                        try {
                            score = conductExperiment(variantGenerationResult, mappingPercentage, 0.0);
                        } catch (Exception ex) {
                            Logger.error(ex);
                        }
                    }

                    // Save the result to JSON object
                    runjson.add(mappingPercentage + "%", createJSONSingleRun(score));
                }
                samplejson.add(runID + "run", runjson);
                Gson gson = new Gson();
                String intermediaryJsonString = gson.toJson(runjson);

                // Save intermediary result to file
                FilesFunc.writeFiles(intermediaryJsonString, System.getProperty("user.dir") + "/results/intermediary",
                        splRepoPath.getFileName().toString() + runID);
            }
            experimentjson.add(sampleSize + " samples", samplejson);
        }

        // Convert JSON object into string
        Gson gson = new Gson();
        String jsonString = gson.toJson(experimentjson);

        // Save to file
        FilesFunc.writeFiles(jsonString, System.getProperty("user.dir") + "/results",
                splRepoPath.getFileName().toString());

        System.out.println("End Experiment .....");
    }

}
