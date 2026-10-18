package com.aiweather.service;

import com.aiweather.dto.ApiDtos.*;
import com.aiweather.provider.ProviderUnavailableException;
import com.aiweather.provider.WeatherProvider;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Client for the Python ML service. Failures are reported honestly; nothing is ever guessed. */
@Service
public class MlService {
    static final int HISTORY_DAYS = 15;
    static final int MIN_HISTORY_DAYS = 14;

    private final RestClient ml;
    private final WeatherProvider provider;

    public MlService(@Qualifier("mlRestClient") RestClient ml, WeatherProvider provider) {
        this.ml = ml;
        this.provider = provider;
    }

    public MlStatusResponse status() {
        try {
            JsonNode info = ml.get().uri("/model/info").retrieve().body(JsonNode.class);
            boolean available = info != null && info.path("available").asBoolean(false);
            String reason = available || info == null ? null : info.path("reason").asText("The model is not available.");
            return new MlStatusResponse(available, reason, info);
        } catch (ResourceAccessException e) {
            return new MlStatusResponse(false, "The ML service is not reachable.", null);
        } catch (RestClientResponseException e) {
            return new MlStatusResponse(false, "The ML service answered with HTTP " + e.getStatusCode().value() + ".", null);
        }
    }

    public boolean reachable() {
        try {
            ml.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public MlPredictionResponse predict(Coordinates c) {
        List<String> messages = new ArrayList<>();
        List<DailyObservation> obs;
        try {
            obs = provider.fetchRecentDailyObservations(c, HISTORY_DAYS);
        } catch (ProviderUnavailableException e) {
            return new MlPredictionResponse(null, null, List.of("Recent observations could not be loaded: " + e.getMessage()));
        }
        if (obs.size() < MIN_HISTORY_DAYS) {
            return new MlPredictionResponse(null, null, List.of("Only " + obs.size() + " complete recent days of observations are available (at least "
                    + MIN_HISTORY_DAYS + " are needed), so no estimate is given."));
        }
        Map<String, Object> body = Map.of("latitude", c.latitude(), "longitude", c.longitude(), "daily", obs);
        JsonNode rainfall = call("/predict/rainfall", body, "Rainfall model", messages);
        JsonNode heavy = call("/predict/heavy-rain", body, "Heavy-rain model", messages);
        return new MlPredictionResponse(rainfall, heavy, messages);
    }

    private JsonNode call(String path, Map<String, Object> body, String label, List<String> messages) {
        try {
            return ml.post().uri(path).body(body).retrieve().body(JsonNode.class);
        } catch (ResourceAccessException e) {
            messages.add(label + ": the ML service is not reachable.");
        } catch (RestClientResponseException e) {
            String detail = e.getResponseBodyAsString();
            messages.add(label + " unavailable (HTTP " + e.getStatusCode().value() + "): " + (detail.length() > 300 ? detail.substring(0, 300) : detail));
        }
        return null;
    }
}
