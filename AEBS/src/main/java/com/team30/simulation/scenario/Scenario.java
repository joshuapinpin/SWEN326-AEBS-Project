package com.team30.simulation.scenario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team30.core.datalayer.enums.LightCondition;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.simulation.state.WorldObject;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scenario {
    private String scenarioName;
    private double initialCarSpeed;
    private long durationMs;
    private WeatherCondition initialWeather;
    private LightCondition initialLight;
    private List<HazardEvent> hazardEvents = new ArrayList<>();
    private List<WorldObject> initialObjects = new ArrayList<>();

    // Default constructor required by Jackson
    public Scenario() {}

    public String getScenarioName()             { return scenarioName; }
    public double getInitialCarSpeed()          { return initialCarSpeed; }
    public long getDurationMs()                 { return durationMs; }
    public WeatherCondition getInitialWeather() { return initialWeather; }
    public LightCondition getInitialLight()     { return initialLight; }
    public List<HazardEvent> getHazardEvents()  { return hazardEvents; }
    public List<WorldObject> getInitialObjects(){ return initialObjects; }

    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public void setInitialCarSpeed(double initialCarSpeed) { this.initialCarSpeed = initialCarSpeed; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public void setInitialWeather(WeatherCondition initialWeather) { this.initialWeather = initialWeather; }
    public void setInitialLight(LightCondition initialLight) { this.initialLight = initialLight; }
    public void setHazardEvents(List<HazardEvent> hazardEvents) { this.hazardEvents = hazardEvents; }
    public void setInitialObjects(List<WorldObject> initialObjects) { this.initialObjects = initialObjects; }

}