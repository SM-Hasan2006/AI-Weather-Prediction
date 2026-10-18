package com.aiweather.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("AI Weather & Heavy Rain Prediction System API")
                .version("1.0.0")
                .description("Forecast, rule-based heavy-rain screening, administrator-entered alerts and ML estimates. "
                        + "Decision-support only; not an official warning service."));
    }
}
