package com.aiweather.controller;

import com.aiweather.dto.ApiDtos.*;
import com.aiweather.service.HistoryService;
import com.aiweather.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {
    private final WeatherService weather;
    private final HistoryService history;

    public WeatherController(WeatherService weather, HistoryService history) {
        this.weather = weather;
        this.history = history;
    }

    @GetMapping("/forecast")
    public ForecastResponse forecast(@RequestParam double latitude, @RequestParam double longitude) {
        return weather.forecast(Coordinates.of(latitude, longitude));
    }

    @GetMapping("/current")
    public CurrentResponse current(@RequestParam double latitude, @RequestParam double longitude) {
        return weather.current(Coordinates.of(latitude, longitude));
    }

    @GetMapping("/hourly")
    public HourlyResponse hourly(@RequestParam double latitude, @RequestParam double longitude) {
        return weather.hourly(Coordinates.of(latitude, longitude));
    }

    @GetMapping("/history")
    public HistoryResponse history(@RequestParam double latitude, @RequestParam double longitude,
                                   @RequestParam(defaultValue = "30") int days) {
        return history.history(Coordinates.of(latitude, longitude), days);
    }
}
