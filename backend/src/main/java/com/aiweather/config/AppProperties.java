package com.aiweather.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** All tunable settings, bound from application.yml / environment variables. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors, Providers providers, Cache cache, Risk risk, Ml ml, Alerts alerts,
        Retention retention, RateLimit rateLimit, Admin admin) {

    public record Cors(List<String> allowedOrigins) {}

    public record Providers(OpenMeteo openMeteo) {}

    public record OpenMeteo(String forecastUrl, String archiveUrl, String geocodingUrl, String publicUrl,
                            int connectTimeoutMs, int readTimeoutMs, int maxAttempts, int retryBackoffMs) {}

    public record Cache(int forecastTtlMinutes, int forecastMaxStaleHours) {}

    public record Risk(String methodVersion, double moderateDailyMm, double highDailyMm, double veryHighDailyMm,
                       double highHourlyMm, int minProbabilityPct, double strongWindKmh) {}

    public record Ml(String baseUrl, String apiKey, int connectTimeoutMs, int readTimeoutMs) {}

    public record Alerts(List<Link> officialLinks) {}

    public record Link(String label, String url) {}

    public record Retention(int snapshotDays) {}

    public record RateLimit(int requestsPerMinute) {}

    public record Admin(String username, String passwordHash) {}
}
