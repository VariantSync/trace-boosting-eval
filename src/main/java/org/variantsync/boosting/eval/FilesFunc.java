package org.variantsync.boosting.eval;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;

public class FilesFunc {

    /**
     * the function is used to change the debug folder path in properties file after
     * each experiment
     * because we cant delete the folder due to open files in Vevo
     *
     * @param count if -1 reset to origin
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
     * delete all product in input file for next test
     */
    public static void deleteInputfiles(String inputFolder) {
        File folder = new File(inputFolder);
        for (File file : folder.listFiles()) {
            file.delete();
        }
    }

    /**
     * delete a folder with Recursively delete all subdirectories and files
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
     * save json data to json file
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
     * save result to file old version
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
                // total percentage of correct mappings = " + results[0] + ", precision = " +
                // scores[0] + ", recall = " + scores[1] + " and f1-score = " + scores[2] + "."
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
     * clear all debug folder in the start of the experiment
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
