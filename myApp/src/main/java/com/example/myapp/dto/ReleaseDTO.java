package com.example.myapp.dto;

import com.example.myapp.model.ReleaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReleaseDTO {
    private Long id;
    @NotNull
    private Long projectId;
    @NotBlank
    private String releaseBranch;
    @NotBlank
    private String sourceBranch;
    @NotBlank
    private String youtrackReleaseTaskId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    @NotNull
    private ReleaseStatus status;
    private String version;
    private String branchName;
}
