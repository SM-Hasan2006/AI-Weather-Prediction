package com.aiweather.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aiweather.config.AppProperties;
import com.aiweather.config.SecurityConfig;
import com.aiweather.dto.ApiDtos.*;
import com.aiweather.entity.AlertOrigin;
import com.aiweather.entity.AlertSeverity;
import com.aiweather.exception.GlobalExceptionHandler;
import com.aiweather.provider.ProviderUnavailableException;
import com.aiweather.service.AlertService;
import com.aiweather.service.HistoryService;
import com.aiweather.service.WeatherService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({WeatherController.class, AlertController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiSecurityAndErrorsTest.Props.class})
class ApiSecurityAndErrorsTest {

    @TestConfiguration
    static class Props {
        @Bean
        @org.springframework.context.annotation.Primary
        AppProperties appProperties() {
            return new AppProperties(new AppProperties.Cors(List.of("http://localhost:5173")), null, null, null, null,
                    new AppProperties.Alerts(List.of()), null, new AppProperties.RateLimit(1000),
                    new AppProperties.Admin("admin", new BCryptPasswordEncoder().encode("s3cret-pass")));
        }
    }

    @Autowired MockMvc mvc;
    @MockBean WeatherService weather;
    @MockBean HistoryService history;
    @MockBean AlertService alerts;

    private static final String BODY = """
            {"title":"Heavy rain","message":"m","severity":"ORANGE","sourceName":"IMD","sourceUrl":"https://mausam.imd.gov.in/",
             "issuedAt":"2026-10-18T00:00:00Z","validFrom":"2026-10-18T00:00:00Z","validTo":"2026-10-19T00:00:00Z","affectedAreas":"Chennai","isTest":true}
            """;

    @Test
    void adminEndpointsRejectAnonymousUsersWithJson401AndNoBrowserPrompt() throws Exception {
        mvc.perform(get("/api/v1/admin/alerts")).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate")).andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post("/api/v1/admin/alerts").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/admin/alerts/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/alerts").with(httpBasic("admin", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListAlertsWithCorrectCredentials() throws Exception {
        when(alerts.listAll()).thenReturn(List.of());
        mvc.perform(get("/api/v1/admin/alerts").with(httpBasic("admin", "s3cret-pass"))).andExpect(status().isOk());
    }

    @Test
    void adminCreateValidatesTheBody() throws Exception {
        mvc.perform(post("/api/v1/admin/alerts").with(httpBasic("admin", "s3cret-pass")).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("https://mausam.imd.gov.in/", "javascript:alert(1)")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_input"))
                .andExpect(jsonPath("$.details[0].field").value("sourceUrl"));
    }

    @Test
    void adminCreateReturns201() throws Exception {
        when(alerts.create(any(), any())).thenReturn(new AlertDto(1L, AlertOrigin.ADMIN_ENTERED_OFFICIAL, "t", "m", AlertSeverity.ORANGE, "IMD",
                "https://mausam.imd.gov.in/", Instant.now(), Instant.now(), Instant.now(), "Chennai", null, null, null, true));
        mvc.perform(post("/api/v1/admin/alerts").with(httpBasic("admin", "s3cret-pass")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.origin").value("ADMIN_ENTERED_OFFICIAL")).andExpect(jsonPath("$.isTest").value(true));
    }

    @Test
    void publicEndpointsNeedNoLogin() throws Exception {
        when(weather.forecast(any())).thenThrow(new ProviderUnavailableException("The weather provider is not reachable right now."));
        mvc.perform(get("/api/v1/weather/forecast").param("latitude", "13.08").param("longitude", "80.27"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("weather_provider_unavailable"));
    }

    @Test
    void invalidCoordinatesGive400() throws Exception {
        mvc.perform(get("/api/v1/weather/forecast").param("latitude", "95").param("longitude", "80"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_input"));
        mvc.perform(get("/api/v1/weather/forecast").param("latitude", "abc").param("longitude", "80")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/weather/forecast").param("latitude", "13")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownWritesOutsideTheApiAreDenied() throws Exception {
        mvc.perform(post("/api/v1/weather/forecast")).andExpect(status().is4xxClientError());
    }
}
