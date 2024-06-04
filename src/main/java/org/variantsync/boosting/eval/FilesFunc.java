package org.variantsync.boosting.eval;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;

/**
 * Utilities class for dealing with files.
 */
public class FilesFunc {

    /**
     * Changes the debug folder path in the properties file after each experiment.
     *
     * @param filePath The file path of the properties file to be modified
     * @param count    The count of experiments. If set to -1, the folder path will
     *                 be reset to its original value
     */
    public static void changeProperties(String filePath, int count) {
        // Load the properties from the file
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(filePath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (count == -1) {
            // Update the value for key1
            properties.setProperty("experiment.work-dir-vevos", "data/debug");
            properties.setProperty("experiment.work-dir-boosting", "data/debug/boosting");
        } else {

            properties.setProperty("experiment.work-dir-vevos", "data/debug" + count);
            properties.setProperty("experiment.work-dir-boosting", "data/debug" + count + "/boosting");
        }

        // Save the updated properties to the file
        try (OutputStream outputStream = new FileOutputStream(filePath)) {
            properties.store(outputStream, null);
            // System.out.println("Updated config.properties file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes all product files in the specified input folder to prepare for the
     * next test.
     * 
     * @param inputFolder The path to the folder containing the product files to be
     *                    deleted.
     * 
     * @throws IllegalArgumentException if the inputFolder is null or empty.
     * @throws IOException              if an I/O error occurs while deleting the
     *                                  files.
     */
    public static void deleteInputfiles(String inputFolder) throws IOException {
        File folder = new File(inputFolder);
        for (File file : folder.listFiles()) {
            file.delete();
        }
    }

    /**
     * Recursively deletes a folder and all its subdirectories and files.
     * 
     * @param folder the folder to be deleted
     * @return true if the folder and all its contents were successfully deleted,
     *         false otherwise
     * @throws SecurityException    if a security manager exists and denies delete
     *                              access to the file
     * @throws NullPointerException if the folder is null
     */
    public static boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    boolean success = deleteFolder(file);
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return folder.delete();
    }

    /**
     * Writes the given JSON data to a specified file.
     * 
     * @param jsonstring The JSON data to be written to the file
     * @param filepath   The path to the file where the JSON data will be saved
     * @param splName    The name of the file to be saved
     * @throws IOException If an I/O error occurs while writing the file
     */
    public static void writeFiles(String jsonstring, String filepath, String splName) throws IOException {
        String filename = filepath + "/experiment_result_" + splName + ".json";
        File file = new File(filename);
        // check the existance of the file
        if (file.exists()) {
            // if exist then change the name of the file to be add
            filename = filepath + "/" + file.getParentFile().listFiles().length + "_experiment_result_" + splName
                    + ".json";
            file = new File(filename);
        }
        file.getParentFile().mkdirs();
        file.createNewFile();

        FileWriter fileStream = new FileWriter(filename);
        fileStream.write(jsonstring);
        fileStream.close();
    }

    /**
     * Saves the results to a file in the specified filepath.
     * 
     * @param scores          a 2D array of double values representing the scores
     *                        for each scenario
     * @param scenarioSize    the size of each scenario
     * @param percentMappings an array of integers representing the percentage
     *                        mappings
     * @param scenario        the name of the scenario
     * @param runs            the number of runs
     * @param filepath        the filepath where the results will be saved
     * @throws IOException if an I/O error occurs while writing to the file
     */
    public static void writeFiles(double[][] scores, int scenarioSize, int[] percentMappings, String scenario, int runs,
            String filepath) throws IOException {
        for (int i = 0; i < percentMappings.length; i++) {
            String filename = filepath + "/0" + scenario + scenarioSize + "variants" + percentMappings[i]
                    + "percent.dat";
            File file = new File(filename);
            // check the existance of the file
            if (file.exists()) {
                // if exist then change the name of the file to be add
                filename = filepath + "/" + file.getParentFile().listFiles().length + scenario + scenarioSize
                        + "variants" + percentMappings[i] + "percent.dat";
                file = new File(filename);
            }
            file.getParentFile().mkdirs();
            file.createNewFile();
            Scanner input = new Scanner(file);

            FileWriter fileStream = new FileWriter(filename);
            for (int j = 0; j < runs; j++) {
                fileStream.write(
                        scores[j + i * runs][0] * 100
                                + "%, precision: " + scores[j + i * runs][1]
                                + ", recall: " + scores[j + i * runs][2]
                                + ", f1-score: " + scores[j + i * runs][3]
                                + ", time: "
                                + scores[j + i * runs][4] + "ms \n");
            }
            input.close();
            fileStream.close();
        }
    }

    /**
     * Clears all debug folders at the start of the experiment.
     * 
     * This function takes a path as input and recursively deletes all debug folders
     * found within that path.
     * Debug folders are typically used for storing temporary files or logs during
     * the debugging process.
     * 
     * @param path The path to the directory where debug folders are located.
     * @throws IllegalArgumentException if the path is null or empty.
     * @throws SecurityException        if a security manager exists and denies
     *                                  delete access to the files.
     */
    public static void clearDebugFolders(String path) {
        File directory = new File(path);
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && file.getName().startsWith("debug")) {
                        deleteFolder(file);
                    }
                }
            }
        }
    }
}
