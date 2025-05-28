package com.example.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectDTO {
    private Long id;
    @NotBlank
    private String name;
    @NotBlank
    private String gitlabProjectId;
    @NotBlank
    private String youtrackProjectId;
    private String description;
    @NotBlank
    private String defaultBranch;
}
