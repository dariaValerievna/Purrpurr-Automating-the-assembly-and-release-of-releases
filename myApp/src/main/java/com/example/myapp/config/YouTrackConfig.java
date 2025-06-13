package com.example.myapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "youtrack")
public class YouTrackConfig {
    private String url;
    private String token;
    private List<String> releaseStatuses;
    private String fields;
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
}
