package com.example.myapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseValidationResult {
    private boolean isValid;
    private TaskStatusValidation taskStatusValidation;
    private TaskDependencyValidation taskDependencyValidation;
    private ExternalDependencyValidation externalDependencyValidation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatusValidation {
        private boolean passed;
        private List<InvalidTaskInfo> invalidTasks;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDependencyValidation {
        private boolean passed;
        private List<MissingDependencyInfo> missingDependencies;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalDependencyValidation {
        private boolean passed;
        private List<ExternalDependencyInfo> externalDependencies;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvalidTaskInfo {
        private String taskId;
        private String title;
        private String currentStatus;
        private String expectedStatus;
        private List<String> tags;
        private String author;
        private String assignee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingDependencyInfo {
        private String taskId;
        private String title;
        private String dependsOnTaskId;
        private String dependsOnTitle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalDependencyInfo {
        private String taskId;
        private String title;
        private String externalProject;
        private String dependencyDescription;
    }
}