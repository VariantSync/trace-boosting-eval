package org.variantsync.boosting.eval;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * This class represents the configuration settings for the experiment.
 * It contains constants for various configuration keys used in the experiment.
 * 
 */
public class Config {
    private final static String EXPERIMENT_SAMPLE_SIZE = "experiment.sample-size";
    private final static String EXPERIMENT_REPEATS = "experiment.repeats";
    private final static String EXPERIMENT_WORK_DIR_VEVOS = "experiment.work-dir-vevos";
    private final static String EXPERIMENT_WORK_DIR_BOOSTING = "experiment.work-dir-boosting";
    private static final String GROUND_TRUTH_DIR = "ground-truth-dir";
    private static final String MAPPING_PERCENTAGE = "experiment.mapping-percentage";
    private static final String EXPERIMENT_REPO_DIR = "experiment.repo-dir";
    private static final String EXPERIMENT_MAX_FEATURES = "experiment.max-features";

    private int strip = 0;
    private final Properties properties;
    private final String splName;

    /**
     * Constructs a new Config object by loading properties from a specified file.
     *
     * @param pathToProperties the path to the properties file to load
     * @param splName          the name of the SPL (Stream Processing Language)
     *                         being used
     * @throws RuntimeException if an IOException occurs while loading the
     *                          properties file
     */
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

    /**
     * Constructs a new Config object by loading properties from a specified file.
     *
     * @param pathToProperties the path to the properties file to load
     * @throws RuntimeException if an IOException occurs while loading the
     *                          properties file
     */
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
     * Returns the list of number of variants that should be sampled for each
     * experiment run specified for RQ2.
     * 
     * @return an array of integers representing the sample sizes for each
     *         experiment run
     */
    public int[] sampleSize() {
        return parseListInt(this.properties.getProperty(EXPERIMENT_SAMPLE_SIZE));
    }

    /**
     * Returns the maximum number of features to consider during sampling.
     * 
     * @return an integer representing the maximum number of features
     * @throws NumberFormatException if the value retrieved from properties file
     *                               cannot be parsed as an integer
     */
    public int maxFeatures() throws NumberFormatException {
        return Integer.parseInt(this.properties.getProperty(EXPERIMENT_MAX_FEATURES));
    }

    /**
     * Returns the number of times an experiment should be repeated.
     * 
     * @return an integer representing the number of experiment repeats
     * @throws NumberFormatException if the experiment repeats property cannot be
     *                               parsed as an integer
     */
    public int experimentRepeats() {
        return Integer.parseInt(this.properties.getProperty(EXPERIMENT_REPEATS));
    }

    /**
     * Retrieves the list of mapping scenarios specified for Research Questions 1
     * and 2.
     * 
     * @return an array of integers representing the mapping scenarios
     * @throws NumberFormatException if the mapping percentage property cannot be
     *                               parsed as an integer
     */
    public int[] experimentMapping() {
        return parseListInt(this.properties.getProperty(MAPPING_PERCENTAGE));
    }

    /**
     * Returns the working directory in which all operations of VEVOS should take
     * place.
     * 
     * @return the Path object representing the working directory
     */
    public Path experimentWorkDirVevos() {
        return Path.of(this.properties.getProperty(EXPERIMENT_WORK_DIR_VEVOS)).resolve(this.splName);
    }

    /**
     * Returns the working directory in which all operations of the boosting
     * algorithm should take place.
     * 
     * @return the Path object representing the experiment work directory for the
     *         boosting algorithm
     * 
     * @throws NullPointerException if the properties object or splName is null
     * 
     * @throws InvalidPathException if the experiment work directory specified in
     *                              the properties file is invalid
     */
    public Path experimentWorkDirBoosting() {
        return Path.of(this.properties.getProperty(EXPERIMENT_WORK_DIR_BOOSTING)).resolve(this.splName);
    }

    /**
     * Returns the path to the ground truth directory.
     * 
     * @return The path to the ground truth directory
     */
    public Path groundTruthDir() throws IOException {
        return Path.of(this.properties.getProperty(GROUND_TRUTH_DIR));
    }

    /**
     * Returns the directory path of the experiment repository.
     *
     * @return The directory path of the experiment repository as a Path object.
     */
    public Path repoDir() {
        return Path.of(this.properties.getProperty(EXPERIMENT_REPO_DIR));
    }

    /**
     * Returns the current path strip value.
     * 
     * @return int - the current strip value
     */
    public int getStrip() {
        return strip;
    }

    /**
     * Sets the path strip value.
     * 
     * @param strip the strip value to set
     * @throws IllegalArgumentException if the strip value is negative
     */
    public void setStrip(int strip) {
        this.strip = strip;
    }

    /**
     * Parses a string representation of a list of integers and returns an array of
     * integers.
     * 
     * @param str the string representation of the list of integers, in the format
     *            "[1, 2, 3, 4]"
     * @return an array of integers parsed from the input string
     * @throws NumberFormatException if any of the tokens in the input string cannot
     *                               be parsed as integers
     */
    private static int[] parseListInt(String str) {
        String[] tokens = str.substring(1, str.length() - 1).split(",");
        int[] result = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = Integer.parseInt(tokens[i].trim());
        }
        return result;
    }
}
