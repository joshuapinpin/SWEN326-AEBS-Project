package com.team30.ui;

import javax.swing.SwingUtilities;

public final class SimulationLauncher {
    private SimulationLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimulationFrame().setVisible(true));
    }
}

