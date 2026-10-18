package com.aiweather.provider;

import com.aiweather.exception.ApiException;
import org.springframework.http.HttpStatus;

/** The weather provider could not deliver usable data (network, rate limit, bad response). */
public class ProviderUnavailableException extends ApiException {
    public ProviderUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "weather_provider_unavailable", message);
    }

    public ProviderUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "weather_provider_unavailable", message, cause);
    }
}
