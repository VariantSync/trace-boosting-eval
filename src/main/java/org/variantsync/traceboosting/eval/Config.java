package org.variantsync.traceboosting.eval;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private final static String EXPERIMENT_SAMPLE_SIZE = "experiment.sample-size";
    private final static String EXPERIMENT_REPEATS = "experiment.repeats";
    private final static String EXPERIMENT_WORK_DIR_VEVOS = "experiment.work-dir-vevos";
    private final static String EXPERIMENT_WORK_DIR_ECCO = "experiment.work-dir-ecco";
    private static final String GROUND_TRUTH_DIR = "ground-truth-dir";
    private static final String MAPPING_PERCENTAGE = "experiment.mapping-percentage";
    private static final String STANDARD_DEVIATION = "experiment.standard-deviation";
    private static final String EXPERIMENT_REPO_DIR = "experiment.repo-dir";
    private static final String EXPERIMENT_MAX_FEATURES = "experiment.max-features";

    private int strip = 0;
    private final Properties properties;
    private final String splName;

    public Config(Path pathToProperties, String splName) {
        properties = new Properties();
        this.splName = splName;
        try (final Reader reader = Files.newBufferedReader(pathToProperties)) {
            properties.load(reader);
        } catch (IOException e) {
            Logger.error("Was not able to load properties from file " + pathToProperties);
            throw new RuntimeException(e);
        }
    }

    public Config(Path pathToProperties) {
        properties = new Properties();
        this.splName = "";
        try (final Reader reader = Files.newBufferedReader(pathToProperties)) {
            properties.load(reader);
        } catch (IOException e) {
            Logger.error("Was not able to load properties from file " + pathToProperties);
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the list of number of variants that should be sampled for each experiment run specified for RQ2.
     */
    public int[] sampleSize() {
        return parseListInt(this.properties.getProperty(EXPERIMENT_SAMPLE_SIZE));
    }

    /**
     * @return the maximum number of features to consider during sampling
     */
    public int maxFeatures() {
        return Integer.parseInt(this.properties.getProperty(EXPERIMENT_MAX_FEATURES));
    }

    /**
     * @return the number of times an experiment should be repeated.
     */
    public int experimentRepeats() {
        return Integer.parseInt(this.properties.getProperty(EXPERIMENT_REPEATS));
    }

    /**
     * @return the list of mapping scenarios specified for RQ1 & RQ2
     */
    public int[] experimentMapping() {
        return parseListInt(this.properties.getProperty(MAPPING_PERCENTAGE));
    }

    /**
     * @return the working directory in which all operations of VEVOS should take place
     */
    public Path experimentWorkDirVevos() {
        return Path.of(this.properties.getProperty(EXPERIMENT_WORK_DIR_VEVOS)).resolve(this.splName);
    }

    /**
     * @return the working directory in which all operations of ECCO should take place
     */
    public Path experimentWorkDirEcco() {
        return Path.of(this.properties.getProperty(EXPERIMENT_WORK_DIR_ECCO)).resolve(this.splName);
    }

    /**
     * @return The path to the ground truth
     */
    public Path groundTruthDir() {
        return Path.of(this.properties.getProperty(GROUND_TRUTH_DIR));
    }

    public Path repoDir() {
        return Path.of(this.properties.getProperty(EXPERIMENT_REPO_DIR));
    }

    /**
     * @return The Normal Distribution standard deviation specified for RQ3
     */
    public double[] experimentStandardDeviation() {
        return parseListDouble(this.properties.getProperty(STANDARD_DEVIATION));
    }

    public int getStrip() {
        return strip;
    }

    public void setStrip(int strip) {
        this.strip = strip;
    }

    // Helper method to parse list values
    private static int[] parseListInt(String str) {
        String[] tokens = str.substring(1, str.length() - 1).split(",");
        int[] result = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = Integer.parseInt(tokens[i].trim());
        }
        return result;
    }

    // Helper method to parse list values
    private static double[] parseListDouble(String str) {
        String[] tokens = str.substring(1, str.length() - 1).split(",");
        double[] result = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = Double.parseDouble(tokens[i].trim());
        }
        return result;
    }
}
