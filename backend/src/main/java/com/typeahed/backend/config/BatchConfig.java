package com.typeahed.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BatchConfig {
    // Enables scheduling support for the application's background jobs.
}
