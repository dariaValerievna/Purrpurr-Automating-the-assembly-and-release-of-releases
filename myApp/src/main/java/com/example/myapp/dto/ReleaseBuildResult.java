package com.example.myapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseBuildResult {
    private String buildId; // Уникальный ID процесса сборки
    private BuildStatus status; // VALIDATING, BUILDING, SUCCESS, FAILED
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Результаты валидации
    private ReleaseValidationResult validationResult;

    // Информация о созданном релизе
    private Long releaseId;
    private String releaseBranch;
    private String releaseVersion;

    // Ошибки процесса
    private List<String> errors;
    private Map<String, Object> metadata;

    public enum BuildStatus {
        VALIDATING,
        BUILDING,
        SUCCESS,
        FAILED
    }
}