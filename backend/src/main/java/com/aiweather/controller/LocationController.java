package com.aiweather.controller;

import com.aiweather.dto.ApiDtos.GeoResult;
import com.aiweather.exception.ApiException;
import com.aiweather.service.WeatherService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {
    private final WeatherService weather;

    public LocationController(WeatherService weather) {
        this.weather = weather;
    }

    @GetMapping("/search")
    public List<GeoResult> search(@RequestParam("q") String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2 || query.length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_input", "q must be 2 to 80 characters long.");
        }
        return weather.search(query);
    }
}
