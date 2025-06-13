package com.example.myapp.service;

import com.example.myapp.dto.ReleaseValidationResult;
import com.example.myapp.dto.ReleaseValidationResult.*;
import com.example.myapp.dto.youtrack.YouTrackIssueDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseValidationService {

    private final YouTrackService youTrackService;
    private static final List<String> VALID_RELEASE_STATUSES = Arrays.asList("For Release", "In Release");

    /**
     * Полная валидация релиза
     */
    public ReleaseValidationResult validateRelease(Long projectId, String releaseTaskId) {
        log.info("Starting validation for release task: {}", releaseTaskId);

        try {
            // Получаем релизную задачу и связанные с ней задачи
            YouTrackIssueDTO releaseTask = youTrackService.getIssue(releaseTaskId);
            List<YouTrackIssueDTO> linkedTasks = youTrackService.getLinkedIssues(releaseTaskId);

            // Выполняем все проверки
            TaskStatusValidation statusValidation = validateTaskStatuses(linkedTasks);
            TaskDependencyValidation dependencyValidation = validateTaskDependencies(linkedTasks);
            ExternalDependencyValidation externalValidation = validateExternalDependencies(linkedTasks);

            boolean isValid = statusValidation.isPassed() &&
                    dependencyValidation.isPassed() &&
                    externalValidation.isPassed();

            return ReleaseValidationResult.builder()
                    .isValid(isValid)
                    .taskStatusValidation(statusValidation)
                    .taskDependencyValidation(dependencyValidation)
                    .externalDependencyValidation(externalValidation)
                    .build();

        } catch (Exception e) {
            log.error("Validation failed for release task: {}", releaseTaskId, e);
            throw new RuntimeException("Validation failed: " + e.getMessage());
        }
    }

    /**
     * Проверка статусов задач
     */
    private TaskStatusValidation validateTaskStatuses(List<YouTrackIssueDTO> tasks) {
        List<InvalidTaskInfo> invalidTasks = new ArrayList<>();

        for (YouTrackIssueDTO task : tasks) {
            String taskId = task.getId();
            String status = task.getStatus();

            if (!VALID_RELEASE_STATUSES.contains(status)) {
                invalidTasks.add(InvalidTaskInfo.builder()
                        .taskId(taskId)
                        .title(task.getSummary())
                        .currentStatus(status)
                        .expectedStatus("For Release или In Release")
                        .tags(task.getTagNames()) // Используем getTagNames() вместо getTags()
                        .author(task.getReporter() != null ? task.getReporter().getName() : "Unknown")
                        .assignee(task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned")
                        .build());
            }
        }

        boolean passed = invalidTasks.isEmpty();
        String message = passed ?
                "Все задачи имеют корректные статусы" :
                String.format("Найдено %d задач с некорректными статусами", invalidTasks.size());

        return TaskStatusValidation.builder()
                .passed(passed)
                .invalidTasks(invalidTasks)
                .message(message)
                .build();
    }

    /**
     * Проверка зависимостей задач
     */
    private TaskDependencyValidation validateTaskDependencies(List<YouTrackIssueDTO> tasks) {
        List<MissingDependencyInfo> missingDependencies = new ArrayList<>();

        // Создаем список ID всех задач в релизе
        List<String> taskIds = tasks.stream()
                .map(YouTrackIssueDTO::getId)
                .toList();

        for (YouTrackIssueDTO task : tasks) {
            String taskId = task.getId();
            try {
                // Получаем зависимости задачи
                List<YouTrackIssueDTO> dependencies = youTrackService.getTaskDependencies(taskId);

                for (YouTrackIssueDTO dependency : dependencies) {
                    String dependencyId = dependency.getId();
                    // Если зависимая задача не включена в релиз
                    if (!taskIds.contains(dependencyId)) {
                        missingDependencies.add(MissingDependencyInfo.builder()
                                .taskId(taskId)
                                .title(task.getSummary())
                                .dependsOnTaskId(dependencyId)
                                .dependsOnTitle(dependency.getSummary())
                                .build());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to check dependencies for task: {}", taskId, e);
            }
        }

        boolean passed = missingDependencies.isEmpty();
        String message = passed ?
                "Все зависимости задач включены в релиз" :
                String.format("Найдено %d отсутствующих зависимостей", missingDependencies.size());

        return TaskDependencyValidation.builder()
                .passed(passed)
                .missingDependencies(missingDependencies)
                .message(message)
                .build();
    }

    /**
     * Проверка внешних зависимостей
     */
    private ExternalDependencyValidation validateExternalDependencies(List<YouTrackIssueDTO> tasks) {
        List<ExternalDependencyInfo> externalDependencies = new ArrayList<>();

        for (YouTrackIssueDTO task : tasks) {
            String taskId = task.getId();
            try {
                // Проверяем теги и поля задачи на наличие внешних зависимостей
                List<String> tagNames = task.getTagNames(); // Используем getTagNames()
                String description = task.getDescription();

                // Ищем упоминания других проектов в тегах
                for (String tag : tagNames) {
                    if (tag.toLowerCase().contains("external") ||
                            tag.toLowerCase().contains("dependency") ||
                            tag.toLowerCase().contains("platform")) {

                        externalDependencies.add(ExternalDependencyInfo.builder()
                                .taskId(taskId)
                                .title(task.getSummary())
                                .externalProject("Неизвестный проект")
                                .dependencyDescription("Найден тег: " + tag)
                                .build());
                    }
                }

                // Дополнительная логика поиска внешних зависимостей
                // можно добавить анализ описания, связанных задач и т.д.

            } catch (Exception e) {
                log.warn("Failed to check external dependencies for task: {}", taskId, e);
            }
        }

        boolean passed = externalDependencies.isEmpty();
        String message = passed ?
                "Внешние зависимости не обнаружены" :
                String.format("Обнаружено %d потенциальных внешних зависимостей", externalDependencies.size());

        return ExternalDependencyValidation.builder()
                .passed(passed)
                .externalDependencies(externalDependencies)
                .message(message)
                .build();
    }
}
