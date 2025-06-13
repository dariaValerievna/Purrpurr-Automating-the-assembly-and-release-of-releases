package com.example.myapp.service;

import com.example.myapp.dto.ReleaseBuildRequest;
import com.example.myapp.dto.ReleaseBuildResult;
import com.example.myapp.dto.ReleaseValidationResult;
import com.example.myapp.dto.youtrack.YouTrackIssueDTO;
import com.example.myapp.model.Project;
import com.example.myapp.model.Release;
import com.example.myapp.model.ReleaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseBuilderService {

    private final ReleaseValidationService validationService;
    private final GitLabService gitLabService;
    private final YouTrackService youTrackService;
    private final ProjectService projectService;
    private final ReleaseService releaseService;

    private final Map<String, ReleaseBuildResult> buildProcesses = new ConcurrentHashMap<>();

    public ReleaseBuildResult startReleaseBuild(ReleaseBuildRequest request) {
        String buildId = UUID.randomUUID().toString();
        log.info("Starting release build process: {}", buildId);

        ReleaseBuildResult result = ReleaseBuildResult.builder()
                .buildId(buildId)
                .status(ReleaseBuildResult.BuildStatus.VALIDATING)
                .startedAt(LocalDateTime.now())
                .errors(new ArrayList<>())
                .metadata(new HashMap<>())
                .build();

        buildProcesses.put(buildId, result);
        CompletableFuture.runAsync(() -> executeReleaseBuild(buildId, request));

        return result;
    }

    public ReleaseBuildResult getBuildStatus(String buildId) {
        ReleaseBuildResult result = buildProcesses.get(buildId);
        if (result == null) {
            throw new RuntimeException("Build process not found: " + buildId);
        }
        return result;
    }

    private void executeReleaseBuild(String buildId, ReleaseBuildRequest request) {
        ReleaseBuildResult result = buildProcesses.get(buildId);

        try {
            log.info("Build {}: Starting validation", buildId);
            result.setStatus(ReleaseBuildResult.BuildStatus.VALIDATING);

            ReleaseValidationResult validationResult = validationService.validateRelease(
                    request.getProjectId(),
                    request.getReleaseTaskId()
            );
            result.setValidationResult(validationResult);

            if (!validationResult.isValid()) {
                result.setStatus(ReleaseBuildResult.BuildStatus.FAILED);
                result.getErrors().add("Validation failed. Check validation results for details.");
                result.setCompletedAt(LocalDateTime.now());
                return;
            }

            log.info("Build {}: Starting release creation", buildId);
            result.setStatus(ReleaseBuildResult.BuildStatus.BUILDING);

            Release release = createRelease(request);
            result.setReleaseId(release.getId());
            result.setReleaseBranch(release.getReleaseBranch()); // Используем правильное поле
            result.setReleaseVersion(release.getVersion());

            createReleaseBranch(request, release);

            if (!"develop".equals(request.getSourceBranch())) {
                processReleaseTasksFromNonDevelop(request, release);
            }

            result.setStatus(ReleaseBuildResult.BuildStatus.SUCCESS);
            result.setCompletedAt(LocalDateTime.now());
            log.info("Build {}: Completed successfully", buildId);

        } catch (Exception e) {
            log.error("Build {}: Failed with error", buildId, e);
            result.setStatus(ReleaseBuildResult.BuildStatus.FAILED);
            result.getErrors().add("Failed to create release: " + e.getMessage());
            result.setCompletedAt(LocalDateTime.now());
        }
    }

    private Release createRelease(ReleaseBuildRequest request) {
        try {
            YouTrackIssueDTO releaseTask = youTrackService.getIssue(request.getReleaseTaskId());
            String version = extractVersionFromTask(releaseTask);
            String releaseBranchName = "release/" + version;

            Project project = projectService.findById(request.getProjectId());

            Release release = new Release();
            release.setProject(project);
            release.setYoutrackReleaseTaskId(request.getReleaseTaskId());
            release.setSourceBranch(request.getSourceBranch());
            release.setReleaseBranch(releaseBranchName); // Используем правильное поле
            release.setVersion(version);
            release.setBranchName(releaseBranchName); // Заполняем и это поле для совместимости
            release.setStatus(ReleaseStatus.IN_PROGRESS);
            release.setCreatedBy(request.getCreatedBy());
            release.setCreatedAt(LocalDateTime.now());

            // Заполняем новые поля
            release.setName("Release " + version);
            release.setDescription("Автоматически созданный релиз для задачи " + request.getReleaseTaskId());

            return releaseService.save(release);

        } catch (Exception e) {
            log.error("Failed to create release", e);
            throw new RuntimeException("Failed to create release: " + e.getMessage(), e);
        }
    }

    private String extractVersionFromTask(YouTrackIssueDTO task) {
        String summary = task.getSummary();
        if (summary != null && summary.toLowerCase().contains("release")) {
            // Ищем версию в формате x.y.z
            String[] parts = summary.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+\\.\\d+\\.\\d+")) {
                    return part;
                }
            }

            // Ищем версию в формате vx.y.z
            for (String part : parts) {
                if (part.matches("v\\d+\\.\\d+\\.\\d+")) {
                    return part.substring(1); // убираем 'v'
                }
            }
        }

        // Генерируем версию на основе времени
        long timestamp = System.currentTimeMillis() / 1000;
        return "1.0." + timestamp;
    }

    private void createReleaseBranch(ReleaseBuildRequest request, Release release) {
        try {
            Long gitlabProjectId = Long.valueOf(release.getProject().getGitlabProjectId());
            String branchName = release.getReleaseBranch(); // Используем правильное поле

            if (!gitLabService.branchExists(gitlabProjectId, branchName)) {
                gitLabService.createBranch(gitlabProjectId, branchName, request.getSourceBranch());
                log.info("Created release branch: {}", branchName);
            } else {
                log.info("Release branch already exists: {}", branchName);
            }

        } catch (Exception e) {
            log.error("Failed to create release branch", e);
            throw new RuntimeException("Failed to create release branch: " + e.getMessage(), e);
        }
    }

    private void processReleaseTasksFromNonDevelop(ReleaseBuildRequest request, Release release) {
        try {
            List<YouTrackIssueDTO> forReleaseTasks = getTasksInStatus(
                    request.getReleaseTaskId(),
                    "For Release"
            );

            Long gitlabProjectId = Long.valueOf(release.getProject().getGitlabProjectId());

            for (YouTrackIssueDTO task : forReleaseTasks) {
                String taskId = task.getId();
                String taskBranch = findTaskBranch(gitlabProjectId, taskId);

                if (taskBranch != null) {
                    createMergeRequestForTask(
                            gitlabProjectId,
                            taskBranch,
                            release.getReleaseBranch(), // Используем правильное поле
                            task,
                            request.isSkipPipeline()
                    );
                } else {
                    log.warn("Branch not found for task: {}", taskId);
                }
            }

        } catch (Exception e) {
            log.error("Failed to process release tasks: {}", e.getMessage());
        }
    }

    private void createMergeRequestForTask(Long gitlabProjectId, String sourceBranch, String targetBranch,
                                           YouTrackIssueDTO task, boolean skipPipeline) {
        try {
            String taskId = task.getId();
            String title = String.format("Merge %s: %s", taskId, task.getSummary());
            String description = String.format("Автоматический MR для задачи %s в релиз", taskId);

            Map<String, Object> mergeRequest = gitLabService.createMergeRequest(
                    gitlabProjectId, sourceBranch, targetBranch, title, description
            );

            Long mrIid = ((Number) mergeRequest.get("iid")).longValue();

            if (canAutoMergeMR(gitlabProjectId, mrIid, skipPipeline)) {
                gitLabService.acceptMergeRequest(gitlabProjectId, mrIid);
                log.info("Auto-merged MR for task: {}", taskId);
            } else {
                assignMRToReleaseManager(gitlabProjectId, mrIid);
                log.info("Assigned MR to release manager for task: {}", taskId);
            }

        } catch (Exception e) {
            log.error("Failed to create MR for task: {}", task.getId(), e);
        }
    }

    private boolean canAutoMergeMR(Long gitlabProjectId, Long mrIid, boolean skipPipeline) {
        try {
            Map<String, Object> mrInfo = gitLabService.getMergeRequest(gitlabProjectId, mrIid);
            String mergeStatus = (String) mrInfo.get("merge_status");
            boolean hasConflicts = "cannot_be_merged".equals(mergeStatus);

            if (hasConflicts) return false;
            if (skipPipeline) return true;

            String pipelineStatus = getPipelineStatus(gitlabProjectId, mrIid);
            return "success".equals(pipelineStatus);

        } catch (Exception e) {
            log.error("Failed to check MR auto-merge possibility: {}", e.getMessage());
            return false;
        }
    }

    private List<YouTrackIssueDTO> getTasksInStatus(String releaseTaskId, String status) {
        try {
            List<YouTrackIssueDTO> allTasks = youTrackService.getLinkedIssues(releaseTaskId);
            return allTasks.stream()
                    .filter(task -> {
                        String taskStatus = extractFieldValue(task.getStatus());
                        return status.equals(taskStatus);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get tasks in status: {}", status, e);
            return new ArrayList<>();
        }
    }

    private String findTaskBranch(Long gitlabProjectId, String taskId) {
        try {
            List<String> possibleBranchNames = Arrays.asList(
                    "feature/" + taskId,
                    "feature/" + taskId.toLowerCase(),
                    taskId,
                    taskId.toLowerCase()
            );

            for (String branchName : possibleBranchNames) {
                if (gitLabService.branchExists(gitlabProjectId, branchName)) {
                    return branchName;
                }
            }
            return null;

        } catch (Exception e) {
            log.error("Failed to find branch for task: {}", taskId, e);
            return null;
        }
    }

    private void assignMRToReleaseManager(Long gitlabProjectId, Long mrIid) {
        try {
            log.info("MR {} should be assigned to release manager", mrIid);
            // TODO: Реализовать назначение MR менеджеру релизов
        } catch (Exception e) {
            log.error("Failed to assign MR to release manager: {}", e.getMessage());
        }
    }

    private String getPipelineStatus(Long gitlabProjectId, Long mrIid) {
        try {
            Map<String, Object> mrInfo = gitLabService.getMergeRequest(gitlabProjectId, mrIid);
            Map<String, Object> pipeline = (Map<String, Object>) mrInfo.get("pipeline");
            if (pipeline != null) {
                return (String) pipeline.get("status");
            }
            return "unknown";
        } catch (Exception e) {
            log.error("Failed to get pipeline status: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Извлечение значения из YouTrack поля (используем тот же метод, что и в DataController)
     */
    private String extractFieldValue(Object field) {
        if (field == null) {
            return null;
        }

        if (field instanceof String) {
            return (String) field;
        }

        try {
            Class<?> clazz = field.getClass();

            // Пробуем getName()
            try {
                var getNameMethod = clazz.getMethod("getName");
                Object result = getNameMethod.invoke(field);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception ignored) {}

            // Пробуем getPresentation()
            try {
                var getPresentationMethod = clazz.getMethod("getPresentation");
                Object result = getPresentationMethod.invoke(field);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception ignored) {}

            return field.toString();

        } catch (Exception e) {
            log.warn("Failed to extract field value from: {}", field.getClass().getSimpleName());
            return field.toString();
        }
    }

    public ReleaseBuildResult validateOnly(ReleaseBuildRequest request) {
        log.info("Validating release build request");
        try {
            ReleaseValidationResult validationResult = validationService.validateRelease(
                    request.getProjectId(),
                    request.getReleaseTaskId()
            );

            return ReleaseBuildResult.builder()
                    .buildId("validation-" + UUID.randomUUID().toString())
                    .status(validationResult.isValid() ?
                            ReleaseBuildResult.BuildStatus.SUCCESS :
                            ReleaseBuildResult.BuildStatus.FAILED)
                    .startedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .validationResult(validationResult)
                    .errors(validationResult.isValid() ?
                            new ArrayList<>() :
                            List.of("Validation failed"))
                    .metadata(new HashMap<>())
                    .build();

        } catch (Exception e) {
            log.error("Validation failed", e);
            throw new RuntimeException("Validation failed: " + e.getMessage(), e);
        }
    }
}

