package com.constructiq.backendjava.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final ConstructIQProperties properties;

    public WebConfig(ConstructIQProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = properties.getCorsOrigins().split(",");
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
