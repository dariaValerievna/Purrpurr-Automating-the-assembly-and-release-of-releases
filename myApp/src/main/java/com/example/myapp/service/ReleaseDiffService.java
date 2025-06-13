package com.example.myapp.service;

import com.example.myapp.model.Release;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseDiffService {

    private final ReleaseService releaseService;
    private final GitLabService gitLabService;
    private final YouTrackService youTrackService;

    private static final Pattern TASK_PATTERN = Pattern.compile("([A-Z]+-\\d+)");

    public Map<String, Object> getReleaseDiff(Long releaseId) {
        Release release = releaseService.findById(releaseId);
        try {
            Long gitlabProjectId = Long.valueOf(release.getProject().getGitlabProjectId());

            // Проверяем, что у релиза есть ветка
            String releaseBranch = release.getReleaseBranch();
            if (releaseBranch == null || releaseBranch.isEmpty()) {
                throw new RuntimeException("Release branch is not set for release: " + releaseId);
            }

            // Находим предыдущую версию для сравнения
            String previousVersion = findPreviousVersion(gitlabProjectId, release.getVersion());

            // ИСПРАВЛЕНИЕ: Если предыдущая версия не найдена, используем sourceBranch из релиза
            if (previousVersion == null) {
                previousVersion = release.getSourceBranch();
                if (previousVersion == null || previousVersion.isEmpty()) {
                    // Fallback логика для определения основной ветки
                    if (gitLabService.branchExists(gitlabProjectId, "main")) {
                        previousVersion = "main";
                    } else if (gitLabService.branchExists(gitlabProjectId, "master")) {
                        previousVersion = "master";
                    } else {
                        throw new RuntimeException("Cannot determine base branch for comparison");
                    }
                }
            }

            log.info("Comparing branches: {} -> {} for project {}", previousVersion, releaseBranch, gitlabProjectId);

            // Проверяем существование веток перед сравнением
            if (!gitLabService.branchExists(gitlabProjectId, previousVersion)) {
                throw new RuntimeException("Source branch does not exist: " + previousVersion);
            }

            if (!gitLabService.branchExists(gitlabProjectId, releaseBranch)) {
                throw new RuntimeException("Release branch does not exist: " + releaseBranch);
            }

            // Получаем коммиты между версиями
            List<Map<String, Object>> commits = gitLabService.getCommitsDiff(
                    gitlabProjectId, previousVersion, releaseBranch
            );

            // Извлекаем задачи из коммитов
            Set<String> tasksFromCommits = extractTasksFromCommits(commits);

            return Map.of(
                    "releaseId", releaseId,
                    "previousVersion", previousVersion,
                    "currentVersion", release.getVersion(),
                    "releaseBranch", releaseBranch,
                    "commits", commits,
                    "commitsCount", commits.size(),
                    "tasksFromCommits", tasksFromCommits,
                    "tasksCount", tasksFromCommits.size()
            );
        } catch (Exception e) {
            log.error("Failed to get release diff for release: {}", releaseId, e);
            throw new RuntimeException("Failed to get release diff: " + e.getMessage());
        }
    }

    /**
     * Получение задач релиза с анализом
     */
    public Map<String, Object> getReleaseTasks(Long releaseId) {
        Release release = releaseService.findById(releaseId);
        try {
            // Получаем задачи из YouTrack (связанные с релизной задачей) как Map
            List<Map<String, Object>> youtrackTasks = youTrackService.getLinkedIssuesAsMap(
                    release.getYoutrackReleaseTaskId()
            );

            Set<String> youtrackTaskIds = youtrackTasks.stream()
                    .map(task -> (String) task.get("id"))
                    .collect(Collectors.toSet());

            // Получаем задачи из коммитов
            Map<String, Object> diffInfo = getReleaseDiff(releaseId);
            Set<String> commitTaskIds = (Set<String>) diffInfo.get("tasksFromCommits");

            // Анализируем расхождения
            Set<String> onlyInYoutrack = new HashSet<>(youtrackTaskIds);
            onlyInYoutrack.removeAll(commitTaskIds);

            Set<String> onlyInCommits = new HashSet<>(commitTaskIds);
            onlyInCommits.removeAll(youtrackTaskIds);

            Set<String> inBoth = new HashSet<>(youtrackTaskIds);
            inBoth.retainAll(commitTaskIds);

            return Map.of(
                    "releaseId", releaseId,
                    "youtrackTasks", youtrackTasks,
                    "youtrackTaskIds", youtrackTaskIds,
                    "commitTaskIds", commitTaskIds,
                    "onlyInYoutrack", onlyInYoutrack,
                    "onlyInCommits", onlyInCommits,
                    "inBoth", inBoth,
                    "hasDiscrepancies", !onlyInYoutrack.isEmpty() || !onlyInCommits.isEmpty()
            );
        } catch (Exception e) {
            log.error("Failed to get release tasks for release: {}", releaseId, e);
            throw new RuntimeException("Failed to get release tasks: " + e.getMessage());
        }
    }

    /**
     * Добавление задачи в релиз
     */
    public Map<String, Object> addTaskToRelease(Long releaseId, String taskId) {
        Release release = releaseService.findById(releaseId);
        try {
            Long gitlabProjectId = Long.valueOf(release.getProject().getGitlabProjectId());

            // Находим ветку задачи
            String taskBranch = findTaskBranch(gitlabProjectId, taskId);
            if (taskBranch == null) {
                throw new RuntimeException("Branch not found for task: " + taskId);
            }

            // Получаем информацию о задаче как Map
            Map<String, Object> taskInfo = youTrackService.getIssueAsMap(taskId);

            // Создаем Merge Request
            String title = String.format("Add %s to release: %s", taskId, taskInfo.get("summary"));
            String description = String.format("Добавление задачи %s в релиз %s", taskId, release.getVersion());

            Map<String, Object> mergeRequest = gitLabService.createMergeRequest(
                    gitlabProjectId, taskBranch, release.getBranchName(), title, description
            );

            Long mrIid = ((Number) mergeRequest.get("iid")).longValue();

            // Проверяем возможность автоматического слияния
            boolean canAutoMerge = checkAutoMergePossibility(gitlabProjectId, mrIid);

            if (canAutoMerge) {
                gitLabService.acceptMergeRequest(gitlabProjectId, mrIid);
                log.info("Auto-merged task {} into release {}", taskId, releaseId);

                return Map.of(
                        "success", true,
                        "message", "Task successfully added and merged",
                        "taskId", taskId,
                        "mergeRequestIid", mrIid,
                        "autoMerged", true
                );
            } else {
                log.info("Created MR for task {} into release {}, manual merge required", taskId, releaseId);

                return Map.of(
                        "success", true,
                        "message", "Merge request created, manual merge required",
                        "taskId", taskId,
                        "mergeRequestIid", mrIid,
                        "autoMerged", false,
                        "mergeRequestUrl", mergeRequest.get("web_url")
                );
            }
        } catch (Exception e) {
            log.error("Failed to add task {} to release {}", taskId, releaseId, e);
            throw new RuntimeException("Failed to add task to release: " + e.getMessage());
        }
    }

    /**
     * Полный анализ релиза
     */
    public Map<String, Object> analyzeRelease(Long releaseId) {
        Release release = releaseService.findById(releaseId);
        try {
            // Получаем информацию о задачах
            Map<String, Object> tasksInfo = getReleaseTasks(releaseId);

            // Проверяем перенос коммитов для каждой задачи
            Map<String, Object> commitTransferAnalysis = analyzeCommitTransfer(release, tasksInfo);

            // Анализируем статусы задач
            Map<String, Object> statusAnalysis = analyzeTaskStatuses(tasksInfo);

            return Map.of(
                    "releaseId", releaseId,
                    "releaseVersion", release.getVersion(),
                    "tasksAnalysis", tasksInfo,
                    "commitTransferAnalysis", commitTransferAnalysis,
                    "statusAnalysis", statusAnalysis,
                    "overallHealth", calculateOverallHealth(tasksInfo, commitTransferAnalysis, statusAnalysis)
            );
        } catch (Exception e) {
            log.error("Failed to analyze release: {}", releaseId, e);
            throw new RuntimeException("Failed to analyze release: " + e.getMessage());
        }
    }

    // Вспомогательные методы
    private String findPreviousVersion(Long gitlabProjectId, String currentVersion) {
        try {
            // Получаем все теги (версии) проекта
            List<Map<String, Object>> tags = gitLabService.getTags(gitlabProjectId);

            // Сортируем теги по дате создания
            tags.sort((a, b) -> {
                String dateA = (String) ((Map<String, Object>) a.get("commit")).get("created_at");
                String dateB = (String) ((Map<String, Object>) b.get("commit")).get("created_at");
                return dateB.compareTo(dateA); // Обратная сортировка (новые первые)
            });

            // Находим предыдущую версию
            for (int i = 0; i < tags.size(); i++) {
                String tagName = (String) tags.get(i).get("name");
                if (tagName.equals(currentVersion) && i + 1 < tags.size()) {
                    return (String) tags.get(i + 1).get("name");
                }
            }

            // ИСПРАВЛЕНИЕ: Если не нашли предыдущую версию, используем sourceBranch из релиза
            // вместо хардкода "master"
            return null; // Вернем null, чтобы обработать это в основном методе

        } catch (Exception e) {
            log.warn("Failed to find previous version: {}", e.getMessage());
            return null;
        }
    }

    private Set<String> extractTasksFromCommits(List<Map<String, Object>> commits) {
        Set<String> tasks = new HashSet<>();
        for (Map<String, Object> commit : commits) {
            String message = (String) commit.get("message");
            if (message != null) {
                Matcher matcher = TASK_PATTERN.matcher(message);
                while (matcher.find()) {
                    tasks.add(matcher.group(1));
                }
            }
        }
        return tasks;
    }

    private String findTaskBranch(Long gitlabProjectId, String taskId) {
        try {
            List<String> possibleBranchNames = Arrays.asList(
                    "feature/" + taskId,
                    "feature/" + taskId.toLowerCase(),
                    taskId,
                    taskId.toLowerCase(),
                    "bugfix/" + taskId,
                    "hotfix/" + taskId
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

    private boolean checkAutoMergePossibility(Long gitlabProjectId, Long mrIid) {
        try {
            Map<String, Object> mrInfo = gitLabService.getMergeRequest(gitlabProjectId, mrIid);
            String mergeStatus = (String) mrInfo.get("merge_status");
            boolean hasConflicts = "cannot_be_merged".equals(mergeStatus);

            if (hasConflicts) {
                return false;
            }

            // Проверяем pipeline если есть
            Map<String, Object> pipeline = (Map<String, Object>) mrInfo.get("pipeline");
            if (pipeline != null) {
                String pipelineStatus = (String) pipeline.get("status");
                return "success".equals(pipelineStatus);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to check auto-merge possibility", e);
            return false;
        }
    }

    private Map<String, Object> analyzeCommitTransfer(Release release, Map<String, Object> tasksInfo) {
        Map<String, List<String>> missingCommitsByTask = new HashMap<>();
        try {
            Long gitlabProjectId = Long.valueOf(release.getProject().getGitlabProjectId());
            Set<String> taskIds = (Set<String>) tasksInfo.get("youtrackTaskIds");

            for (String taskId : taskIds) {
                String taskBranch = findTaskBranch(gitlabProjectId, taskId);
                if (taskBranch != null) {
                    // Получаем коммиты из ветки задачи
                    List<Map<String, Object>> taskCommits = gitLabService.getBranchCommits(gitlabProjectId, taskBranch);
                    // Получаем коммиты из релизной ветки
                    List<Map<String, Object>> releaseCommits = gitLabService.getBranchCommits(gitlabProjectId, release.getBranchName());

                    // Находим коммиты, которые есть в задаче, но нет в релизе
                    Set<String> taskCommitIds = taskCommits.stream()
                            .map(commit -> (String) commit.get("id"))
                            .collect(Collectors.toSet());

                    Set<String> releaseCommitIds = releaseCommits.stream()
                            .map(commit -> (String) commit.get("id"))
                            .collect(Collectors.toSet());

                    List<String> missingCommits = taskCommitIds.stream()
                            .filter(commitId -> !releaseCommitIds.contains(commitId))
                            .collect(Collectors.toList());

                    if (!missingCommits.isEmpty()) {
                        missingCommitsByTask.put(taskId, missingCommits);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to analyze commit transfer", e);
        }

        return Map.of(
                "missingCommitsByTask", missingCommitsByTask,
                "hasIncompleteTransfers", !missingCommitsByTask.isEmpty(),
                "incompleteTasksCount", missingCommitsByTask.size()
        );
    }

    private Map<String, Object> analyzeTaskStatuses(Map<String, Object> tasksInfo) {
        List<Map<String, Object>> youtrackTasks = (List<Map<String, Object>>) tasksInfo.get("youtrackTasks");
        Map<String, Integer> statusCounts = new HashMap<>();
        List<Map<String, Object>> nonReleaseStatusTasks = new ArrayList<>();

        for (Map<String, Object> task : youtrackTasks) {
            Map<String, Object> status = (Map<String, Object>) task.get("status");
            String statusName = status != null ? (String) status.get("name") : "Unknown";

            statusCounts.put(statusName, statusCounts.getOrDefault(statusName, 0) + 1);

            if (!"For Release".equals(statusName) && !"In Release".equals(statusName)) {
                nonReleaseStatusTasks.add(Map.of(
                        "taskId", task.get("id"),
                        "title", task.get("summary"),
                        "status", statusName,
                        "assignee", getTaskAssignee(task)
                ));
            }
        }

        return Map.of(
                "statusCounts", statusCounts,
                "nonReleaseStatusTasks", nonReleaseStatusTasks,
                "hasStatusIssues", !nonReleaseStatusTasks.isEmpty(),
                "totalTasks", youtrackTasks.size(),
                "readyForRelease", statusCounts.getOrDefault("For Release", 0) + statusCounts.getOrDefault("In Release", 0)
        );
    }

    private String calculateOverallHealth(Map<String, Object> tasksInfo,
                                          Map<String, Object> commitAnalysis,
                                          Map<String, Object> statusAnalysis) {
        boolean hasDiscrepancies = (Boolean) tasksInfo.get("hasDiscrepancies");
        boolean hasIncompleteTransfers = (Boolean) commitAnalysis.get("hasIncompleteTransfers");
        boolean hasStatusIssues = (Boolean) statusAnalysis.get("hasStatusIssues");

        if (!hasDiscrepancies && !hasIncompleteTransfers && !hasStatusIssues) {
            return "HEALTHY";
        } else if (hasDiscrepancies || hasIncompleteTransfers) {
            return "CRITICAL";
        } else if (hasStatusIssues) {
            return "WARNING";
        } else {
            return "UNKNOWN";
        }
    }

    private String getTaskAssignee(Map<String, Object> task) {
        Map<String, Object> assignee = (Map<String, Object>) task.get("assignee");
        return assignee != null ? (String) assignee.get("name") : "Unassigned";
    }
}

