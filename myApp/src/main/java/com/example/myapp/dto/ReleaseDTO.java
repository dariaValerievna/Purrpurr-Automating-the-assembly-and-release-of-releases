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

    private String name;
    private String description;

    @NotNull
    private ReleaseStatus status;

    private String version;
    private String branchName;

    // 🆕 Новые поля для GitLab интеграции
    private String gitlabReleaseUrl;
    private String gitlabMergeRequestUrl;
    private String gitlabTagName;
    private String releaseNotes;
    private String createdBy;

    // 🆕 Информация о проекте (для удобства)
    private String projectName;
}
