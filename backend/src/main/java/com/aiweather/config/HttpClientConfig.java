package com.aiweather.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    private static JdkClientHttpRequestFactory factory(int connectMs, int readMs) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs)).build();
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(client);
        f.setReadTimeout(Duration.ofMillis(readMs));
        return f;
    }

    @Bean
    @Qualifier("weatherRestClient")
    public RestClient weatherRestClient(AppProperties props) {
        AppProperties.OpenMeteo c = props.providers().openMeteo();
        return RestClient.builder().requestFactory(factory(c.connectTimeoutMs(), c.readTimeoutMs())).build();
    }

    @Bean
    @Qualifier("mlRestClient")
    public RestClient mlRestClient(AppProperties props) {
        AppProperties.Ml ml = props.ml();
        RestClient.Builder b = RestClient.builder()
                .baseUrl(ml.baseUrl())
                .requestFactory(factory(ml.connectTimeoutMs(), ml.readTimeoutMs()));
        if (ml.apiKey() != null && !ml.apiKey().isBlank()) {
            b.defaultHeader("X-ML-Api-Key", ml.apiKey());
        }
        return b.build();
    }
}
