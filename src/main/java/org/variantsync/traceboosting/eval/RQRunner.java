package org.variantsync.traceboosting.eval;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.tinylog.Logger;
import org.variantsync.vevos.simulation.variability.SPLCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class RQRunner extends ExperimentRunner {

    protected RQRunner(Config config) {
        super(config);
        configPath = System.getProperty("user.dir") + "/data/evaluation.properties";
        FilesFunc.clearDebugFolders(System.getProperty("user.dir") + "/data");
    }

    // public static void main(String... args) throws IOException {
    // String[] configPath = {Path.of(System.getProperty("user.dir") +
    // "/data/RQ1.properties").toString()};
    // Config config = ExperimentRunner.configFromArgs(configPath);
    // RQ1 rq1 = new RQ1(config);
    //
    // List<Path> repositoryPaths = prepareRepos(config);
    // for (Path splRepoPath : repositoryPaths) {
    // try {
    // rq1.run(splRepoPath);
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }
    //
    // System.exit(0);
    // }

    public void run(Path splRepoPath, List<SPLCommit> commitGroundTruths) throws IOException {

        JsonObject experimentjson = createJSONProperties();

        for (int sampleSize : sampleSizes) { // loop on number of sample scenarios
            Logger.info(sampleSize + " size of variants out of " + this.sampleSizes.length + " samples");
            final JsonObject samplejson = new JsonObject();
            for (int runID = 1; runID <= ex_repeat; runID++) { // loop on runs
                final JsonObject runjson = new JsonObject();
                // Generate the same set of variants for all percentages
                VariantGenerationResult variantGenerationResult = prepareVariants(splRepoPath, commitGroundTruths,
                        sampleSize);
                for (int mappingPercentage : percentages) { // loop on percentage scenarios
                    // do the run
                    Logger.info("loop mappingPercentage:" + mappingPercentage + " runID=" + runID);
                    // build runner object and start experiment
                    double[] score = null;
                    while (score == null) { // repeat while any exception happen in the experiment run
                        try { // handle some exception that network cause
                            score = conductExperiment(variantGenerationResult, mappingPercentage, 0.0);
                        } catch (Exception ex) {
                            Logger.error(ex);
                        }
                    }
                    // save the result to json object
                    runjson.add(mappingPercentage + "%", createJSONSingleRun(score));
                }
                samplejson.add(runID + "run", runjson);
            }
            experimentjson.add(sampleSize + " samples", samplejson);
        }
        // convert json object into string
        Gson gson = new Gson();
        String jsonString = gson.toJson(experimentjson);

        // save to file
        FilesFunc.writeFiles(jsonString, System.getProperty("user.dir") + "/data/result/rq1/",
                splRepoPath.getFileName().toString());

        System.out.println("End RQ1 Experiment .....");
    }
}
