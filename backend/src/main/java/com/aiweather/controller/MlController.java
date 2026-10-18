package com.aiweather.controller;

import com.aiweather.dto.ApiDtos.*;
import com.aiweather.service.MlService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ml")
public class MlController {
    private final MlService ml;

    public MlController(MlService ml) {
        this.ml = ml;
    }

    @GetMapping("/status")
    public MlStatusResponse status() {
        return ml.status();
    }

    @PostMapping("/predict")
    public MlPredictionResponse predict(@RequestParam double latitude, @RequestParam double longitude) {
        return ml.predict(Coordinates.of(latitude, longitude));
    }
}
