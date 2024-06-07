package org.variantsync.boosting.eval;

import org.tinylog.Logger;
import org.variantsync.boosting.eval.experiments.Config;
import org.variantsync.boosting.eval.experiments.ExperimentRunner;
import org.variantsync.boosting.eval.experiments.RQRunner;
import org.variantsync.boosting.eval.util.VEVOSUtilities;
import org.variantsync.vevos.simulation.variability.SPLCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The Main class for running the evaluation.
 */
public class Main {

    /**
     * This method is the main entry point for the program. It reads a configuration
     * file, prepares repositories, and runs experiments on each repository in
     * parallel using a thread pool.
     * 
     * @param args The command line arguments passed to the program
     * @throws IOException          If an I/O error occurs while reading the
     *                              configuration file or loading ground truth data
     * @throws InterruptedException If a thread is interrupted while waiting for a
     *                              task to complete
     * @throws ExecutionException   If an error occurs while executing a task in the
     *                              thread pool
     */
    public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
        if (args.length < 1) {
            System.err.println("Name of properties file required");
            System.exit(1);
        }
        String propertiesFilename = args[0];
        Path configPath = Path.of(System.getProperty("user.dir") + "/data/" + propertiesFilename);
        // Load the main config to determine the subjects
        Config config = new Config(configPath);
        List<Path> repositoryPaths = prepareRepos(new Config(configPath));

        try (ExecutorService threadPool = Executors.newFixedThreadPool(config.numThreads())) {
            List<Future<String>> futures = new ArrayList<>();
            for (Path splRepoPath : repositoryPaths) {
                Config rqConfig = ExperimentRunner.loadSubjectSpecificConfig(splRepoPath.getFileName().toString(),
                        configPath);
                Path splName = splRepoPath.getFileName();

                List<String> subjects = rqConfig.getSubjects();
                if (!subjects.isEmpty() && !subjects.contains(splName.toString())) {
                    Logger.info("Skipping " + splName);
                    continue;
                }

                Path groundTruthDir = rqConfig.groundTruthDir().resolve(splName);

                futures.add(threadPool.submit(() -> {
                    var utilities = new VEVOSUtilities();
                    List<SPLCommit> commitGroundTruths;
                    synchronized (Main.class) {
                        commitGroundTruths = utilities.loadGroundTruth(groundTruthDir);
                    }

                    RQRunner rqRunner = new RQRunner(rqConfig);
                    try {
                        rqRunner.run(splRepoPath, commitGroundTruths);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    return splRepoPath.getFileName().toString();
                }));
            }
            for (Future<String> future : futures) {
                Logger.info("Completed " + future.get());
            }
        }
        System.exit(0);
    }

    /**
     * Prepares the subject repositories by cloning them into the specified
     * directory.
     * 
     * @param config the configuration object containing the repository directory
     * @return a list of Paths representing the cloned repositories
     * @throws IOException if an I/O error occurs during the cloning process
     */
    public static List<Path> prepareRepos(Config config) throws IOException {
        Path repoDir = config.repoDir();
        var utilities = new VEVOSUtilities();
        return utilities.cloneRepositories(repoDir);
    }

}
