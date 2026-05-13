package com.team30.ui;

import com.team30.core.datalayer.enums.DrivingMode;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.sensors.CameraSensor;
import com.team30.core.datalayer.sensors.LidarSensor;
import com.team30.core.datalayer.sensors.RadarSensor;
import com.team30.core.datalayer.sensors.Sensor;
import com.team30.core.datalayer.sensors.WheelSpeedSensor;
import com.team30.core.logic.AEBSSoftwareSystem;
import com.team30.core.logic.BrakeSystemController;
import com.team30.core.logic.CollisionDetector;
import com.team30.core.logic.FaultHandler;
import com.team30.core.logic.RedundancyChecker;
import com.team30.core.logic.SensorInputHandler;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.engine.ScenarioLoader;
import com.team30.simulation.engine.SimulatorEngine;
import com.team30.simulation.scenario.Scenario;
import com.team30.simulation.state.CarState;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SimulationFrame extends JFrame {
    private static final Pattern TICK_SUMMARY = Pattern.compile(
            "t=\\s*(\\d+)ms \\| speed=\\s*([0-9.+-]+)m/s \\| mode=([A-Z_]+)\\s*\\| threat=([A-Z_]+)\\s*\\| result=([A-Z_]+|null)\\s*\\| attempts=(\\d+)"
    );

    private static final Pattern CAR_STATE_TIME = Pattern.compile(
            "\\[CAR STATE\\].*Time: *(\\d+)ms"
    );

    private final JTextArea logArea = new JTextArea();
    private final JLabel scenarioValue = new JLabel("No scenario loaded");
    private final JLabel timeValue = new JLabel("—");
    private final JLabel speedValue = new JLabel("—");
    private final JLabel modeValue = new JLabel("—");
    private final JLabel threatValue = new JLabel("—");
    private final JLabel attemptsValue = new JLabel("—");
    private final JLabel activeValue = new JLabel("ON");

    private final JButton loadButton = new JButton("Load Scenario");
    private final JToggleButton aebsToggle = new JToggleButton("AEBS: ON", true);
    private final JButton runButton = new JButton("Start Simulation");
    private final JButton clearButton = new JButton("Clear Log");

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private SimulationSession session;
    private boolean running;
    private SwingWorker<Void, Void> worker;

    private long lastKnownSimTimeMs = 0L;

    public SimulationFrame() {
        super("AEBS Simulation Monitor");
        installConsoleMirrors();
        buildUi();
        setStatusEmpty();
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                restoreConsoleStreams();
            }
        });
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildStatusPanel(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        loadButton.addActionListener(e -> loadScenario());
        aebsToggle.addActionListener(e -> toggleAebs());
        runButton.addActionListener(e -> startSimulation());
        clearButton.addActionListener(e -> logArea.setText(""));

        runButton.setEnabled(false);
        aebsToggle.setEnabled(false);

        toolbar.add(loadButton);
        toolbar.add(aebsToggle);
        toolbar.add(runButton);
        toolbar.add(clearButton);
        return toolbar;
    }

    private JComponent buildCenterPanel() {
        logArea.setEditable(false);
        logArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        logArea.setLineWrap(false);
        logArea.setWrapStyleWord(false);

        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Console mirror"));
        return scrollPane;
    }

    private JComponent buildStatusPanel() {
        JPanel statusPanel = new JPanel(new GridLayout(0, 2, 12, 8));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Live simulation state"));

        statusPanel.add(new JLabel("Scenario:"));
        statusPanel.add(scenarioValue);
        statusPanel.add(new JLabel("Time:"));
        statusPanel.add(timeValue);
        statusPanel.add(new JLabel("Speed:"));
        statusPanel.add(speedValue);
        statusPanel.add(new JLabel("Mode:"));
        statusPanel.add(modeValue);
        statusPanel.add(new JLabel("Threat level:"));
        statusPanel.add(threatValue);
        statusPanel.add(new JLabel("Brake attempts:"));
        statusPanel.add(attemptsValue);
        statusPanel.add(new JLabel("AEBS active:"));
        statusPanel.add(activeValue);

        return statusPanel;
    }

    private void loadScenario() {
        Scenario scenario = ScenarioLoader.of().load();
        if (scenario == null) {
            return;
        }

        session = SimulationSession.create(scenario);
        scenarioValue.setText(scenario.getScenarioName());
        aebsToggle.setSelected(true);
        aebsToggle.setText("AEBS: ON");
        aebsToggle.setEnabled(true);
        runButton.setEnabled(true);
        activeValue.setText("ON");
        setTickStatus(0L, session.carState.getCarSpeed(), session.carState.getDrivingMode().toString(), "NONE", 0);
        System.out.println("[APP] Loaded scenario: " + scenario.getScenarioName());
    }

    private void toggleAebs() {
        if (session == null) {
            return;
        }

        boolean active = aebsToggle.isSelected();
        session.driverInterface.toggleAEBS(active);
        aebsToggle.setText(active ? "AEBS: ON" : "AEBS: OFF");
        activeValue.setText(active ? "ON" : "OFF");
    }

    private void startSimulation() {
        if (session == null) {
            JOptionPane.showMessageDialog(this,
                    "Load a scenario first.",
                    "No scenario loaded",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (running) {
            return;
        }

        SimulatorEngine engine = new SimulatorEngine(
                session.carState,
                session.scenario,
                session.allSensors,
                session.aebs,
                session.driverInterface.isAesbActive()
        );

        running = true;
        setControlsEnabled(false);
        runButton.setText("Running...");
        System.out.println("[APP] Starting simulation...");

        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                engine.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    System.out.println("[APP] Simulation complete.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    appendLine("[APP] Simulation interrupted.");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String message = (cause != null && cause.getMessage() != null)
                            ? cause.getMessage()
                            : ex.getMessage();
                    appendLine("[APP] Simulation failed: " + message);
                    JOptionPane.showMessageDialog(
                            SimulationFrame.this,
                            message,
                            "Simulation error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    running = false;
                    session = null;
                    runButton.setText("Start Simulation");
                    loadButton.setEnabled(true);
                    aebsToggle.setEnabled(false);
                    runButton.setEnabled(false);
                }
            }
        };
        worker.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        loadButton.setEnabled(enabled);
        aebsToggle.setEnabled(enabled && session != null);
        runButton.setEnabled(enabled && session != null);
        clearButton.setEnabled(true);
    }

    private void setStatusEmpty() {
        setTickStatus(null, null, null, null, null);
    }

    private void setTickStatus(Long timeMs, Double speed, String mode, String threat, Integer attempts) {
        if (timeMs != null) {
            lastKnownSimTimeMs = timeMs;
            timeValue.setText(timeMs + " ms");
        }
        if (speed != null) {
            speedValue.setText(String.format("%.2f m/s", speed));
        }
        if (mode != null) {
            modeValue.setText(mode);
        }
        if (threat != null) {
            threatValue.setText(threat);
        }
        if (attempts != null) {
            attemptsValue.setText(String.valueOf(attempts));
        }
    }

    private void appendLine(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            extractTimeFromCarState(text);
            updateStatusFromText(text);
            appendImportantLinesOnly(text);
        });
    }

    private void extractTimeFromCarState(String text) {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String trimmed = line.stripTrailing();
            Matcher matcher = CAR_STATE_TIME.matcher(trimmed);
            if (matcher.matches()) {
                lastKnownSimTimeMs = Long.parseLong(matcher.group(1));
                return;
            }
        }
    }

    private void appendImportantLinesOnly(String text) {
        StringBuilder filtered = new StringBuilder();
        String[] lines = text.split("\\R", -1);

        extractAllTimesFromText(lines);

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String trimmed = line.stripTrailing();
            if (isImportantLine(trimmed)) {
                String decoratedLine = prependTimeToAlert(trimmed);
                filtered.append(decoratedLine).append(System.lineSeparator());
            }
        }

        if (!filtered.isEmpty()) {
            logArea.append(filtered.toString());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private void extractAllTimesFromText(String[] lines) {
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String trimmed = line.stripTrailing();
            Matcher tickMatcher = TICK_SUMMARY.matcher(trimmed);
            if (tickMatcher.matches()) {
                lastKnownSimTimeMs = Long.parseLong(tickMatcher.group(1));
                return;
            }
            Matcher timeMatcher = CAR_STATE_TIME.matcher(trimmed);
            if (timeMatcher.find()) {
                lastKnownSimTimeMs = Long.parseLong(timeMatcher.group(1));
                return;
            }
        }
    }

    private String prependTimeToAlert(String line) {
        return String.format("[%dms] %s", lastKnownSimTimeMs, line);
    }

    private boolean isImportantLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }

        if (line.startsWith("[APP] Loaded scenario:")) {
            return false;
        }
        if (line.startsWith("[APP] Starting simulation")) {
            return false;
        }
        if (line.startsWith("[APP] Simulation complete")) {
            return false;
        }
        if (TICK_SUMMARY.matcher(line).matches()) {
            return false;
        }

        return line.contains("[AUDITORY ALERT]")
                || line.contains("[VISUAL ALERT]")
                || line.contains("[ESCALATION ALERT]")
                || line.contains("[MAINTENANCE WARNING]")
                || line.contains("[CRITICAL ALERT]")
                || line.contains("[AEBS] System ACTIVATED")
                || line.contains("[AEBS] System DEACTIVATED")
                || line.contains("[AEBS] Current status:")
                || line.contains("Critical AEBS failure detected")
                || line.contains("[WARN]")
                || line.contains("[ERROR]");
    }

    private void updateStatusFromText(String text) {
        String[] lines = text.split("\\R");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String trimmed = line.stripTrailing();
            Matcher matcher = TICK_SUMMARY.matcher(trimmed);
            if (matcher.matches()) {
                Long timeMs = Long.parseLong(matcher.group(1));
                lastKnownSimTimeMs = timeMs;
                setTickStatus(
                        timeMs,
                        Double.parseDouble(matcher.group(2)),
                        matcher.group(3),
                        matcher.group(4),
                        Integer.parseInt(matcher.group(6))
                );
                continue;
            }

            if (trimmed.contains("[AEBS] Current status: ACTIVE")
                    || trimmed.contains("[AEBS] System ACTIVATED by driver.")) {
                aebsToggle.setSelected(true);
                aebsToggle.setText("AEBS: ON");
                activeValue.setText("ON");
            } else if (trimmed.contains("[AEBS] Current status: INACTIVE")
                    || trimmed.contains("[AEBS] System DEACTIVATED by driver.")) {
                aebsToggle.setSelected(false);
                aebsToggle.setText("AEBS: OFF");
                activeValue.setText("OFF");
            }
        }
    }

    private void installConsoleMirrors() {
        Charset charset = Charset.defaultCharset();
        System.setOut(new PrintStream(new MirrorOutputStream(originalOut, this::appendLine, charset), true, charset));
        System.setErr(new PrintStream(new MirrorOutputStream(originalErr, this::appendLine, charset), true, charset));
    }

    private void restoreConsoleStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private static final class SimulationSession {
        private final Scenario scenario;
        private final CarState carState;
        private final DriverInterface driverInterface;
        private final AEBSSoftwareSystem aebs;
        private final List<Sensor> allSensors;

        private SimulationSession(Scenario scenario,
                                  CarState carState,
                                  DriverInterface driverInterface,
                                  AEBSSoftwareSystem aebs,
                                  List<Sensor> allSensors) {
            this.scenario = scenario;
            this.carState = carState;
            this.driverInterface = driverInterface;
            this.aebs = aebs;
            this.allSensors = allSensors;
        }

        private static SimulationSession create(Scenario scenario) {
            double[] initialRPM = new double[]{500, 500, 500, 500};
            CarState carState = new CarState(
                    scenario.getInitialCarSpeed(),
                    scenario.getInitialCarSpeed(),
                    initialRPM,
                    DrivingMode.CRUISING,
                    0.0,
                    2.0,
                    scenario.getInitialWeather(),
                    scenario.getInitialLight()
            );

            DriverInterface driverInterface = new DriverInterface(carState);
            SensorInputHandler inputHandler = new SensorInputHandler();
            RedundancyChecker redundancyChecker = new RedundancyChecker();
            CollisionDetector collisionDetector = new CollisionDetector();
            BrakeSystemController brakeController = new BrakeSystemController(carState);
            FaultHandler faultHandler = new FaultHandler(driverInterface, carState);

            AEBSSoftwareSystem aebs = new AEBSSoftwareSystem(
                    inputHandler,
                    redundancyChecker,
                    collisionDetector,
                    brakeController,
                    faultHandler,
                    driverInterface
            );

            RadarSensor primaryRadar = new RadarSensor(SensorId.PRIMARY, carState);
            RadarSensor redundantRadar = new RadarSensor(SensorId.REDUNDANT, carState);
            LidarSensor primaryLidar = new LidarSensor(SensorId.PRIMARY, carState);
            LidarSensor redundantLidar = new LidarSensor(SensorId.REDUNDANT, carState);
            CameraSensor primaryCamera = new CameraSensor(SensorId.PRIMARY, carState);
            CameraSensor redundantCamera = new CameraSensor(SensorId.REDUNDANT, carState);
            WheelSpeedSensor primaryWheel = new WheelSpeedSensor(SensorId.PRIMARY, carState);
            WheelSpeedSensor redundantWheel = new WheelSpeedSensor(SensorId.REDUNDANT, carState);

            List<Sensor> allSensors = List.of(
                    primaryRadar, redundantRadar,
                    primaryLidar, redundantLidar,
                    primaryCamera, redundantCamera,
                    primaryWheel, redundantWheel
            );
            allSensors.forEach(sensor -> sensor.attachObserver(aebs));

            return new SimulationSession(scenario, carState, driverInterface, aebs, allSensors);
        }
    }

    private static final class MirrorOutputStream extends OutputStream {
        private final OutputStream delegate;
        private final java.util.function.Consumer<String> mirror;
        private final Charset charset;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private MirrorOutputStream(OutputStream delegate,
                                   java.util.function.Consumer<String> mirror,
                                   Charset charset) {
            this.delegate = delegate;
            this.mirror = mirror;
            this.charset = charset;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            delegate.write(b);
            buffer.write(b);
            if (b == '\n') {
                drainCompleteLines(false);
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            buffer.write(b, off, len);
            drainCompleteLines(false);
        }

        @Override
        public synchronized void flush() throws IOException {
            delegate.flush();
            drainCompleteLines(true);
        }

        @Override
        public synchronized void close() throws IOException {
            flush();
        }

        private void drainCompleteLines(boolean flushRemaining) {
            byte[] bytes = buffer.toByteArray();
            int start = 0;
            boolean emitted = false;

            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] == '\n') {
                    mirror.accept(new String(bytes, start, i - start + 1, charset));
                    start = i + 1;
                    emitted = true;
                }
            }

            if (start > 0) {
                buffer.reset();
                if (start < bytes.length) {
                    buffer.write(bytes, start, bytes.length - start);
                }
            }

            if (flushRemaining && buffer.size() > 0 && !emitted) {
                mirror.accept(new String(buffer.toByteArray(), charset));
                buffer.reset();
            }
        }
    }
}










