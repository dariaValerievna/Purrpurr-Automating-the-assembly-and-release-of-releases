package com.example.myapp.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReleaseInfo {
    private String tagName;
    private String name;
    private String description;
    private String projectId;
    private String projectName;
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
    private String ref; // branch or commit
    private List<String> assets;
    private String gitlabReleaseUrl;
    private boolean prerelease;
}
