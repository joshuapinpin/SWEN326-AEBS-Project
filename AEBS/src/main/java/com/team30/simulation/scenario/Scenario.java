package com.team30.simulation.scenario;
import com.team30.core.datalayer.enums.LightCondition;
import com.team30.core.datalayer.enums.WeatherCondition;

import java.util.List;

public class Scenario {
    String scenarioName;
    double initialCarSpeed;
    long durationMs;
    WeatherCondition initialWeather;
    LightCondition initialLight;
    List<HazardEvent> hazardEvents;
}
