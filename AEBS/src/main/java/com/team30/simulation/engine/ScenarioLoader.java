package com.team30.simulation.engine;
import com.team30.simulation.scenario.Scenario;

import javax.swing.*;
import java.io.File;

public class ScenarioLoader {
    /**
     * Prompts the user to select a file location for saving.
     * Opens a file chooser dialog filtered for JSON files and automatically
     * appends the .json extension if not provided.
     *
     * @return the selected File object, or null if the user cancels
     */
    public File chooseFile(){
        File fileChoice = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save JSON File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        // parent is null, the dialog is displayed in a default position, centered on the screen
        // specifically for saving
        int userChoice = fileChooser.showSaveDialog(null);
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            fileChoice = fileChooser.getSelectedFile();
            if (!fileChoice.getName().toLowerCase().endsWith(".json")) {
                fileChoice = new File(fileChoice.getParentFile(), fileChoice.getName() + ".json");
            }
        }
        else {
            // user canceled or closed dialog
            JOptionPane.showMessageDialog(null, "File selection canceled.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        // may be null
        return fileChoice;
    }
    // reads JSON and represents it as a scenario object
    // will find hazads and represent them as hazard events
    Scenario load(){return new Scenario();}
}
