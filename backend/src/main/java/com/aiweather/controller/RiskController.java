package com.aiweather.controller;

import com.aiweather.dto.ApiDtos.Coordinates;
import com.aiweather.dto.ApiDtos.RiskResponse;
import com.aiweather.service.RiskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {
    private final RiskService risk;

    public RiskController(RiskService risk) {
        this.risk = risk;
    }

    @GetMapping
    public RiskResponse risk(@RequestParam double latitude, @RequestParam double longitude) {
        return risk.assess(Coordinates.of(latitude, longitude));
    }
}
