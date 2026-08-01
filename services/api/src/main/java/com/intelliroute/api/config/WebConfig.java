package com.intelliroute.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for local frontend development.
 *
 * <p>Allowed origins are externalized via {@code intelliroute.cors.allowed-origins}
 * so production deployments can restrict them without code changes.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${intelliroute.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
