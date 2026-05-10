package com.team30.simulation.scenario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team30.core.datalayer.enums.LightCondition;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.simulation.state.WorldObject;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scenario {
    public String scenarioName;
    public double initialCarSpeed;
    public long durationMs;
    public WeatherCondition initialWeather;
    public LightCondition initialLight;
    public List<HazardEvent> hazardEvents = new ArrayList<>();
    public List<WorldObject> initialObjects = new ArrayList<>();

    // Default constructor required by Jackson
    public Scenario() {}

    public String getScenarioName()             { return scenarioName; }
    public double getInitialCarSpeed()          { return initialCarSpeed; }
    public long getDurationMs()                 { return durationMs; }
    public WeatherCondition getInitialWeather() { return initialWeather; }
    public LightCondition getInitialLight()     { return initialLight; }
    public List<HazardEvent> getHazardEvents()  { return hazardEvents; }
    public List<WorldObject> getInitialObjects(){ return initialObjects; }
}