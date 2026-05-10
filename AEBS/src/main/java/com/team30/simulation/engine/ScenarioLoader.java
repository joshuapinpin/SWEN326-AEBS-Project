package com.team30.simulation.engine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team30.simulation.scenario.Scenario;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ScenarioLoader {
    private final ObjectMapper mapper;
    private File file;
    private static final ScenarioLoader loader = new ScenarioLoader();
    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the replay state with default values.
     */
    private ScenarioLoader(){
        mapper = new ObjectMapper();
    }
    /**
     * Returns the singleton instance of ScenarioLoader
     *
     * @return the single ScenarioLoader instance
     */
    public static ScenarioLoader of() {
        return loader;
    }
    /**
     * Prompts the user to select a JSON file to read.
     * Opens a file chooser dialog filtered for JSON files.
     *
     * @return the selected File object, or null if the user cancels
     */
    public File getFile() {
        file = null;
        JFileChooser fileChooser = new JFileChooser();

        // Open directly to the scenarios folder in the repo
        File scenariosDir = new File("AEBS/src/main/resources/scenarios");
        if (scenariosDir.exists()) {
            fileChooser.setCurrentDirectory(scenariosDir);
        }

        fileChooser.setDialogTitle("Select Scenario");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));

        int userChoice = fileChooser.showOpenDialog(null);
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
        } else {
            JOptionPane.showMessageDialog(null, "File selection canceled.", "Warning", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return file;
    }

    /**
     * Reads a complete scenario from a JSON file.
     * Deserialises the file into a Scenario object.
     *
     * @return the Scenario object or null if loading fails
     */
    public Scenario load() {
        Scenario scen = null;
        getFile();
        if (file == null) {
            // return null scenario
            return scen;
        }
        try {
            scen = mapper.readValue(file, Scenario.class);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to get scenario: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return scen;
    }
}
