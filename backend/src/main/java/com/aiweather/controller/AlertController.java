package com.aiweather.controller;

import com.aiweather.dto.ApiDtos.*;
import com.aiweather.service.AlertService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AlertController {
    private final AlertService alerts;

    public AlertController(AlertService alerts) {
        this.alerts = alerts;
    }

    @GetMapping("/alerts")
    public AlertsResponse alerts(@RequestParam double latitude, @RequestParam double longitude) {
        return alerts.forLocation(Coordinates.of(latitude, longitude));
    }

    // ---- administrator only (SecurityConfig requires ROLE_ADMIN for /api/v1/admin/**)
    @GetMapping("/admin/alerts")
    public List<AlertDto> list() {
        return alerts.listAll();
    }

    @PostMapping("/admin/alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public AlertDto create(@Valid @RequestBody AlertRequest body, Authentication auth) {
        return alerts.create(body, auth.getName());
    }

    @PutMapping("/admin/alerts/{id}")
    public AlertDto update(@PathVariable Long id, @Valid @RequestBody AlertRequest body, Authentication auth) {
        return alerts.update(id, body, auth.getName());
    }

    @DeleteMapping("/admin/alerts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        alerts.delete(id, auth.getName());
    }
}
