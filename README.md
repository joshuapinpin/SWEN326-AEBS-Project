# AEBS

Java AEBS simulation with an existing CLI mode and a new Swing monitor UI.

## Build

```powershell
cd "C:\Users\joshu\OneDrive - Victoria University of Wellington - STUDENT\Classes - Year 3\SWEN326\abes\AEBS"
mvn test
```

## Run the Swing UI

Launch the new UI entry point:

```powershell
cd "C:\Users\joshu\OneDrive - Victoria University of Wellington - STUDENT\Classes - Year 3\SWEN326\abes\AEBS"
mvn exec:java -Dexec.mainClass=com.team30.ui.SimulationLauncher
```

If you prefer to run from your IDE, use `com.team30.ui.SimulationLauncher`.

## What the UI shows

- mirrored console output, including simulation alerts and tick summaries
- scenario loading through the existing JSON file chooser
- AEBS on/off toggle
- live time, speed, mode, threat level, and brake-attempt counters

## Notes

- The original CLI logging/output remains unchanged.
- The Swing UI only mirrors the simulation output; it does not replace the existing logging system.
