package com.constructiq.backendjava;

import com.constructiq.backendjava.config.ConstructIQProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConstructIQProperties.class)
public class BackendJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendJavaApplication.class, args);
    }
}
