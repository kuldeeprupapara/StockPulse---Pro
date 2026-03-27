package com.stockmarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration.
 *
 * WHY NEEDED?
 * ─────────────────────────────────────────────────────────────────────────
 * Angular (localhost:4200) connects to this service (localhost:8081).
 * Browser blocks cross-origin requests by default.
 * CORS headers tell browser: "localhost:4200 is allowed to connect".
 *
 * WITHOUT THIS:
 *   Browser blocks SSE connection → no live updates in Angular
 *   Error: "CORS policy: No Access-Control-Allow-Origin header"
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/stream/**")
                        .allowedOrigins(
                                "http://localhost:4200",         // Angular dev
                                "https://your-prod-domain.com"  // production
                        )
                        .allowedMethods("GET")   // SSE only needs GET
                        .allowCredentials(true);
            }
        };
    }
}
