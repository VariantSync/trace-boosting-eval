package org.variantsync.traceboosting.eval;

import org.tinylog.Logger;
import org.variantsync.vevos.simulation.variability.SPLCommit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
        Path configPath = Path.of(System.getProperty("user.dir") + "/data/evaluation.properties");
        // Load the main config to determine the subjects
        List<Path> repositoryPaths = prepareRepos(new Config(configPath));

        try (ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            List<Future<String>> futures = new ArrayList<>();
            for (Path splRepoPath : repositoryPaths) {
                futures.add(threadPool.submit(() -> {
                    Config rqConfig = ExperimentRunner.loadSubjectSpecificConfig(splRepoPath.getFileName().toString(),
                            configPath);
                    Path splName = splRepoPath.getFileName();
                    Path groundTruthDir = rqConfig.groundTruthDir().resolve(splName);
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

    public static List<Path> prepareRepos(Config config) throws IOException {
        Path repoDir = config.repoDir();
        var utilities = new VEVOSUtilities();
        return utilities.cloneRepositories(repoDir);
    }
}
