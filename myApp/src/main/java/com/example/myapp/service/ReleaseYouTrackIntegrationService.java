package com.example.myapp.service;

import com.example.myapp.dto.youtrack.YouTrackIssueDTO;
import com.example.myapp.model.Release;
import com.example.myapp.model.Project;
import com.example.myapp.model.ReleaseIssue;
import com.example.myapp.repository.ReleaseIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseYouTrackIntegrationService {

    private final YouTrackService youTrackService;
    private final ReleaseIssueRepository releaseIssueRepository;
    private final ReleaseService releaseService;
    private final ProjectService projectService;

    /**
     * Синхронизация задач YouTrack с релизом
     */
    @Transactional
    public Map<String, Object> syncReleaseWithYouTrack(Release release, List<String> youTrackIssueIds) {
        log.info("Syncing release {} with {} YouTrack issues", release.getId(), youTrackIssueIds.size());

        Map<String, Object> result = new HashMap<>();
        List<String> syncedIssues = new ArrayList<>();
        List<String> failedIssues = new ArrayList<>();

        // Удаляем старые связи с задачами
        releaseIssueRepository.deleteByReleaseId(release.getId());

        for (String issueId : youTrackIssueIds) {
            try {
                YouTrackIssueDTO issue = youTrackService.getIssue(issueId);
                if (issue != null) {
                    ReleaseIssue releaseIssue = createReleaseIssue(release, issue);
                    releaseIssueRepository.save(releaseIssue);
                    syncedIssues.add(issueId);
                    log.debug("Synced issue: {}", issueId);
                } else {
                    failedIssues.add(issueId);
                    log.warn("Issue not found: {}", issueId);
                }
            } catch (Exception e) {
                failedIssues.add(issueId);
                log.error("Failed to sync issue {}: {}", issueId, e.getMessage());
            }
        }

        result.put("syncedIssues", syncedIssues);
        result.put("failedIssues", failedIssues);
        result.put("totalSynced", syncedIssues.size());
        result.put("totalFailed", failedIssues.size());

        log.info("Sync completed: {} synced, {} failed", syncedIssues.size(), failedIssues.size());
        return result;
    }

    /**
     * Валидация релиза через YouTrack
     */
    public Map<String, Object> validateReleaseIssues(Release release) {
        log.info("Validating release {} issues in YouTrack", release.getId());

        List<ReleaseIssue> releaseIssues = releaseIssueRepository.findByReleaseId(release.getId());
        List<String> issueIds = releaseIssues.stream()
                .map(ReleaseIssue::getIssueId)
                .collect(Collectors.toList());

        if (issueIds.isEmpty()) {
            return Map.of(
                    "valid", true,
                    "message", "No issues to validate",
                    "issues", Collections.emptyMap()
            );
        }

        // Проверяем статусы задач
        Map<String, List<YouTrackIssueDTO>> validation = youTrackService.validateIssuesForRelease(issueIds);

        // Проверяем зависимости
        Map<String, Object> dependencies = youTrackService.checkIssueDependencies(issueIds);

        // Получаем статистику
        Map<String, Object> statistics = youTrackService.getReleaseStatistics(issueIds);

        Map<String, Object> result = new HashMap<>();
        result.put("validation", validation);
        result.put("dependencies", dependencies);
        result.put("statistics", statistics);

        boolean isValid = validation.get("invalid").isEmpty() &&
                validation.get("notFound").isEmpty() &&
                ((List<?>) dependencies.get("missingDependencies")).isEmpty();

        result.put("valid", isValid);
        result.put("validationDate", LocalDateTime.now());

        log.info("Release {} validation completed: {}", release.getId(), isValid ? "VALID" : "INVALID");
        return result;
    }

    /**
     * Автоматическое создание релиза на основе YouTrack задачи
     */
    @Transactional
    public Release createReleaseFromYouTrackIssue(String releaseIssueId) {
        log.info("Creating release from YouTrack issue: {}", releaseIssueId);

        YouTrackIssueDTO releaseIssue = youTrackService.getIssue(releaseIssueId);
        if (releaseIssue == null) {
            throw new RuntimeException("Release issue not found: " + releaseIssueId);
        }

        // Получаем все связанные задачи
        List<YouTrackIssueDTO> allIssues = youTrackService.getAllReleaseIssues(releaseIssueId);

        // Создаем релиз
        Release release = new Release();
        release.setName(releaseIssue.getSummary());
        release.setDescription("Auto-created from YouTrack issue: " + releaseIssueId);
        release.setStatusFromEnum(Release.Status.PLANNING);
        release.setCreatedAt(LocalDateTime.now());

        // ✅ ИСПРАВЛЕНИЕ: Устанавливаем проект (по умолчанию проект с ID=1)
        Project defaultProject = projectService.findById(1L); // Добавь ProjectService
        release.setProject(defaultProject);

        // Извлекаем версию из задачи, если есть
        String version = extractVersionFromIssue(releaseIssue);
        if (version != null) {
            release.setVersion(version);
        } else {
            release.setVersion("1.0.0"); // Версия по умолчанию
        }

        // Устанавливаем обязательные поля
        release.setYoutrackReleaseTaskId(releaseIssueId);
        release.setReleaseBranch("release/" + version);
        release.setSourceBranch("main");
        release.setCreatedBy("youtrack-integration"); // Добавляем создателя

        // Сохраняем релиз
        release = releaseService.save(release);

        // Синхронизируем задачи
        List<String> issueIds = allIssues.stream()
                .map(YouTrackIssueDTO::getId)
                .collect(Collectors.toList());
        syncReleaseWithYouTrack(release, issueIds);

        log.info("Created release {} from YouTrack issue {} with {} issues",
                release.getId(), releaseIssueId, issueIds.size());
        return release;
    }


    /**
     * Обновление статусов задач в YouTrack при изменении статуса релиза
     */
    public void updateYouTrackIssuesStatus(Release release, String newStatus) {
        log.info("Updating YouTrack issues status for release {} to: {}", release.getId(), newStatus);

        List<ReleaseIssue> releaseIssues = releaseIssueRepository.findByReleaseId(release.getId());

        for (ReleaseIssue releaseIssue : releaseIssues) {
            try {
                boolean updated = youTrackService.updateIssueStatus(releaseIssue.getIssueId(), newStatus);
                if (updated) {
                    log.debug("Updated issue {} status to {}", releaseIssue.getIssueId(), newStatus);
                } else {
                    log.warn("Failed to update issue {} status", releaseIssue.getIssueId());
                }
            } catch (Exception e) {
                log.error("Error updating issue {} status: {}", releaseIssue.getIssueId(), e.getMessage());
            }
        }
    }

    /**
     * Добавление комментариев в YouTrack задачи о релизе
     */
    public void addReleaseCommentsToIssues(Release release, String comment) {
        log.info("Adding release comments to YouTrack issues for release: {}", release.getId());

        List<ReleaseIssue> releaseIssues = releaseIssueRepository.findByReleaseId(release.getId());
        String fullComment = String.format("Release %s: %s", release.getName(), comment);

        for (ReleaseIssue releaseIssue : releaseIssues) {
            try {
                boolean added = youTrackService.addComment(releaseIssue.getIssueId(), fullComment);
                if (added) {
                    log.debug("Added comment to issue: {}", releaseIssue.getIssueId());
                } else {
                    log.warn("Failed to add comment to issue: {}", releaseIssue.getIssueId());
                }
            } catch (Exception e) {
                log.error("Error adding comment to issue {}: {}", releaseIssue.getIssueId(), e.getMessage());
            }
        }
    }

    /**
     * Получение отчета по задачам релиза из YouTrack
     */
    public Map<String, Object> generateReleaseReport(Release release) {
        log.info("Generating YouTrack report for release: {}", release.getId());

        List<ReleaseIssue> releaseIssues = releaseIssueRepository.findByReleaseId(release.getId());
        List<String> issueIds = releaseIssues.stream()
                .map(ReleaseIssue::getIssueId)
                .collect(Collectors.toList());

        if (issueIds.isEmpty()) {
            return Map.of("message", "No issues found for release");
        }

        Map<String, Object> report = new HashMap<>();

        // Получаем детальную информацию по всем задачам
        List<YouTrackIssueDTO> issues = new ArrayList<>();
        for (String issueId : issueIds) {
            try {
                YouTrackIssueDTO issue = youTrackService.getIssue(issueId);
                if (issue != null) {
                    issues.add(issue);
                }
            } catch (Exception e) {
                log.error("Error getting issue {} for report: {}", issueId, e.getMessage());
            }
        }

        // Статистика
        Map<String, Object> statistics = youTrackService.getReleaseStatistics(issueIds);

        // Валидация
        Map<String, List<YouTrackIssueDTO>> validation = youTrackService.validateIssuesForRelease(issueIds);

        // Зависимости
        Map<String, Object> dependencies = youTrackService.checkIssueDependencies(issueIds);

        report.put("release", Map.of(
                "id", release.getId(),
                "name", release.getName(),
                "version", release.getVersion(),
                "status", release.getStatus()
        ));
        report.put("issues", issues);
        report.put("statistics", statistics);
        report.put("validation", validation);
        report.put("dependencies", dependencies);
        report.put("generatedAt", LocalDateTime.now());

        log.info("Generated report for release {} with {} issues", release.getId(), issues.size());
        return report;
    }

    // Вспомогательные методы

    private ReleaseIssue createReleaseIssue(Release release, YouTrackIssueDTO issue) {
        ReleaseIssue releaseIssue = new ReleaseIssue();
        releaseIssue.setRelease(release);
        releaseIssue.setIssueId(issue.getId());
        releaseIssue.setTitle(issue.getSummary());
        releaseIssue.setStatus(issue.getStatus());
        releaseIssue.setType(issue.getType());
        releaseIssue.setPriority(issue.getPriority());

        if (issue.getAssignee() != null) {
            releaseIssue.setAssignee(issue.getAssignee().getName());
        }

        releaseIssue.setCreatedAt(LocalDateTime.now());
        releaseIssue.setUpdatedAt(LocalDateTime.now());

        return releaseIssue;
    }

    private String extractVersionFromIssue(YouTrackIssueDTO issue) {
        // Пытаемся извлечь версию из custom fields
        if (issue.getCustomFields() != null) {
            for (YouTrackIssueDTO.CustomFieldDTO field : issue.getCustomFields()) {
                if ("Fix versions".equals(field.getName()) || "Version".equals(field.getName())) {
                    return field.getValue() != null ? field.getValue().toString() : null;
                }
            }
        }

        // Пытаемся извлечь из заголовка
        String summary = issue.getSummary();
        if (summary != null && summary.matches(".*\\d+\\.\\d+.*")) {
            // Простое извлечение версии из заголовка (например, "Release 1.2.3")
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher matcher = pattern.matcher(summary);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }
}
