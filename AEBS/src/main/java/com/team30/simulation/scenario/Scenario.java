package com.team30.simulation.scenario;
import com.team30.core.datalayer.enums.LightCondition;
import com.team30.core.datalayer.enums.WeatherCondition;
import lombok.Getter;

import java.util.List;

@Getter
public class Scenario {
    String scenarioName;
    double initialCarSpeed;
    long durationMs;
    WeatherCondition initialWeather;
    LightCondition initialLight;
    List<HazardEvent> hazardEvents;
}
