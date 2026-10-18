package com.aiweather.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Public read endpoints; every /api/v1/admin/** route needs HTTP Basic credentials of the single
 * ADMIN user (BCrypt hash supplied through the environment). If no hash is configured the admin
 * API is effectively disabled because no user exists.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        // JSON 401 WITHOUT a WWW-Authenticate header, so browsers do not open their own login dialog.
        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), Map.of(
                    "timestamp", Instant.now().toString(), "status", 401, "code", "unauthorized",
                    "message", "Authentication is required."));
        };
        http.csrf(csrf -> csrf.disable()) // stateless API; credentials are sent explicitly, never via cookies
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/**", "/actuator/health", "/actuator/health/**",
                            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/ml/predict").permitAll()
                    .anyRequest().denyAll())
            .httpBasic(b -> b.authenticationEntryPoint(entryPoint))
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.cors().allowedOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppProperties props) {
        AppProperties.Admin admin = props.admin();
        if (admin.passwordHash() == null || admin.passwordHash().isBlank()) {
            return new InMemoryUserDetailsManager(); // no users: admin API unusable
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(admin.username()).password(admin.passwordHash()).roles("ADMIN").build());
    }
}
