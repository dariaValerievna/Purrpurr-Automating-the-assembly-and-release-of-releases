package com.example.myapp.service;

import com.example.myapp.dto.youtrack.YouTrackIssueDTO;
import com.example.myapp.dto.youtrack.YouTrackLinkDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YouTrackService {

    private final WebClient webClient;
    private final RestTemplate restTemplate;
    private final List<String> releaseStatuses;
    private final String defaultFields;
    private final String youtrackUrl;
    private final String youtrackToken;

    public YouTrackService(@Value("${youtrack.url}") String youtrackUrl,
                           @Value("${youtrack.token}") String youtrackToken,
                           @Value("${youtrack.release-statuses}") List<String> releaseStatuses,
                           @Value("${youtrack.fields}") String fields) {
        this.youtrackUrl = youtrackUrl;
        this.youtrackToken = youtrackToken;
        this.webClient = WebClient.builder()
                .baseUrl(youtrackUrl)
                .defaultHeader("Authorization", "Bearer " + youtrackToken)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.restTemplate = new RestTemplate();
        this.releaseStatuses = releaseStatuses;
        this.defaultFields = fields;
    }

    /**
     * Получение задачи по ID
     */
    public YouTrackIssueDTO getIssue(String issueId) {
        try {
            String uri = UriComponentsBuilder.fromPath("/api/issues/{issueId}")
                    .queryParam("fields", defaultFields)
                    .buildAndExpand(issueId)
                    .toUriString();

            YouTrackIssueDTO issue = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(YouTrackIssueDTO.class)
                    .block();

            log.debug("Retrieved issue: {}", issueId);
            return issue;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("Issue not found: {}", issueId);
                return null;
            }
            log.error("Failed to get issue {}: {} - {}", issueId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get issue: " + issueId, e);
        }
    }

    /**
     * Получение связанных задач (для совместимости с ReleaseValidationService)
     */
    public List<YouTrackIssueDTO> getLinkedIssues(String issueId) {
        try {
            log.info("Getting linked issues for: {}", issueId);
            List<YouTrackIssueDTO> linkedIssues = new ArrayList<>();

            // Получаем подзадачи
            List<YouTrackIssueDTO> subtasks = getSubtasks(issueId);
            linkedIssues.addAll(subtasks);

            // Получаем связанные через links задачи
            List<YouTrackLinkDTO> links = getIssueLinks(issueId);
            for (YouTrackLinkDTO link : links) {
                if (isReleaseRelatedLink(link)) {
                    String relatedIssueId = getRelatedIssueId(link, issueId);
                    if (relatedIssueId != null) {
                        YouTrackIssueDTO relatedIssue = getIssue(relatedIssueId);
                        if (relatedIssue != null) {
                            linkedIssues.add(relatedIssue);
                        }
                    }
                }
            }

            return linkedIssues;
        } catch (Exception e) {
            log.error("Failed to get linked issues for {}: {}", issueId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Получение связанных задач как Map для совместимости
     */
    public List<Map<String, Object>> getLinkedIssuesAsMap(String issueId) {
        List<YouTrackIssueDTO> issues = getLinkedIssues(issueId);
        return issues.stream()
                .map(this::convertIssueToMap)
                .collect(Collectors.toList());
    }

    /**
     * Получение задачи как Map для совместимости
     */
    public Map<String, Object> getIssueAsMap(String issueId) {
        YouTrackIssueDTO issue = getIssue(issueId);
        return convertIssueToMap(issue);
    }

    private Map<String, Object> convertIssueToMap(YouTrackIssueDTO issue) {
        if (issue == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", issue.getId());
        map.put("summary", issue.getSummary());
        map.put("description", issue.getDescription());

        // Статус
        if (issue.getStatus() != null) {
            map.put("status", Map.of("name", issue.getStatus()));
        }

        // Assignee
        if (issue.getAssignee() != null) {
            map.put("assignee", Map.of("name", issue.getAssignee().getName()));
        }

        // Reporter
        if (issue.getReporter() != null) {
            map.put("reporter", Map.of("name", issue.getReporter().getName()));
        }

        // Tags
        map.put("tags", issue.getTagNames() != null ? issue.getTagNames() : new ArrayList<>());

        return map;
    }

    /**
     * Получение зависимостей задачи (для совместимости с ReleaseValidationService)
     */
    public List<YouTrackIssueDTO> getTaskDependencies(String issueId) {
        try {
            log.info("Getting task dependencies for: {}", issueId);
            List<YouTrackIssueDTO> dependencies = new ArrayList<>();

            List<YouTrackLinkDTO> links = getIssueLinks(issueId);
            for (YouTrackLinkDTO link : links) {
                if (isDependencyLink(link)) {
                    String dependentIssueId = getDependentIssueId(link, issueId);
                    if (dependentIssueId != null) {
                        YouTrackIssueDTO dependentIssue = getIssue(dependentIssueId);
                        if (dependentIssue != null) {
                            dependencies.add(dependentIssue);
                        }
                    }
                }
            }

            return dependencies;
        } catch (Exception e) {
            log.error("Failed to get task dependencies for {}: {}", issueId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Получение подзадач для релизной задачи
     */
    public List<YouTrackIssueDTO> getSubtasks(String parentIssueId) {
        try {
            String query = String.format("parent: %s", parentIssueId);
            return searchIssues(query);
        } catch (Exception e) {
            log.error("Failed to get subtasks for issue {}: {}", parentIssueId, e.getMessage());
            throw new RuntimeException("Failed to get subtasks for issue: " + parentIssueId, e);
        }
    }

    /**
     * Поиск задач по запросу
     */
    public List<YouTrackIssueDTO> searchIssues(String query) {
        try {
            log.info("Searching issues with query: {}", query);

            String uri = UriComponentsBuilder.fromPath("/api/issues")
                    .queryParam("query", query)
                    .queryParam("fields", defaultFields)
                    .queryParam("$top", 1000)
                    .build()
                    .toUriString();

            log.debug("Request URI: {}", uri);

            List<YouTrackIssueDTO> issues = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<YouTrackIssueDTO>>() {})
                    .block();

            log.info("Found {} issues for query: {}", issues != null ? issues.size() : 0, query);
            return issues != null ? issues : new ArrayList<>();

        } catch (WebClientResponseException e) {
            log.error("YouTrack API error for query '{}': {} - {}",
                    query, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("YouTrack API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to search issues with query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Failed to search issues", e);
        }
    }


    /**
     * Получение связей задачи (зависимости)
     */
    public List<YouTrackLinkDTO> getIssueLinks(String issueId) {
        try {
            String uri = String.format("/api/issues/%s/links", issueId);

            List<YouTrackLinkDTO> links = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<YouTrackLinkDTO>>() {})
                    .block();

            log.debug("Found {} links for issue: {}", links != null ? links.size() : 0, issueId);
            return links != null ? links : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get links for issue {}: {}", issueId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Проверка статусов задач для релиза
     */
    public Map<String, List<YouTrackIssueDTO>> validateIssuesForRelease(List<String> issueIds) {
        log.info("Validating {} issues for release", issueIds.size());

        Map<String, List<YouTrackIssueDTO>> result = new HashMap<>();
        result.put("valid", new ArrayList<>());
        result.put("invalid", new ArrayList<>());
        result.put("notFound", new ArrayList<>());

        for (String issueId : issueIds) {
            try {
                YouTrackIssueDTO issue = getIssue(issueId);
                if (issue == null) {
                    // Создаем пустую задачу для отображения ошибки
                    YouTrackIssueDTO notFoundIssue = new YouTrackIssueDTO();
                    notFoundIssue.setId(issueId);
                    notFoundIssue.setSummary("Issue not found");
                    result.get("notFound").add(notFoundIssue);
                    continue;
                }

                String status = issue.getStatus();
                if (status != null && releaseStatuses.contains(status)) {
                    result.get("valid").add(issue);
                } else {
                    result.get("invalid").add(issue);
                }
            } catch (Exception e) {
                log.error("Error validating issue {}: {}", issueId, e.getMessage());
                YouTrackIssueDTO errorIssue = new YouTrackIssueDTO();
                errorIssue.setId(issueId);
                errorIssue.setSummary("Error retrieving issue: " + e.getMessage());
                result.get("notFound").add(errorIssue);
            }
        }

        log.info("Validation result: {} valid, {} invalid, {} not found",
                result.get("valid").size(),
                result.get("invalid").size(),
                result.get("notFound").size());

        return result;
    }

    /**
     * Проверка зависимостей задач
     */
    public Map<String, Object> checkIssueDependencies(List<String> releaseIssueIds) {
        log.info("Checking dependencies for {} issues", releaseIssueIds.size());

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> missingDependencies = new ArrayList<>();
        List<Map<String, Object>> externalDependencies = new ArrayList<>();
        Set<String> releaseIssueSet = new HashSet<>(releaseIssueIds);

        for (String issueId : releaseIssueIds) {
            try {
                List<YouTrackLinkDTO> links = getIssueLinks(issueId);
                for (YouTrackLinkDTO link : links) {
                    // Проверяем зависимости типа "depends on", "blocked by" и т.д.
                    if (isDependencyLink(link)) {
                        String dependentIssueId = getDependentIssueId(link, issueId);
                        if (dependentIssueId != null && !releaseIssueSet.contains(dependentIssueId)) {
                            YouTrackIssueDTO dependentIssue = getIssue(dependentIssueId);
                            if (dependentIssue != null) {
                                Map<String, Object> dependency = new HashMap<>();
                                dependency.put("sourceIssue", issueId);
                                dependency.put("dependentIssue", dependentIssueId);
                                dependency.put("dependentIssueTitle", dependentIssue.getSummary());
                                dependency.put("linkType", link.getLinkType().getName());

                                // Проверяем, является ли это зависимостью от другого проекта
                                if (isExternalProject(dependentIssueId, issueId)) {
                                    externalDependencies.add(dependency);
                                } else {
                                    missingDependencies.add(dependency);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error checking dependencies for issue {}: {}", issueId, e.getMessage());
            }
        }

        result.put("missingDependencies", missingDependencies);
        result.put("externalDependencies", externalDependencies);

        log.info("Dependencies check result: {} missing, {} external",
                missingDependencies.size(), externalDependencies.size());

        return result;
    }

    /**
     * Получение всех задач, связанных с релизной задачей
     */
    public List<YouTrackIssueDTO> getAllReleaseIssues(String releaseIssueId) {
        log.info("Getting all issues for release: {}", releaseIssueId);
        Set<YouTrackIssueDTO> allIssues = new HashSet<>();

        try {
            // Получаем основную релизную задачу
            YouTrackIssueDTO releaseIssue = getIssue(releaseIssueId);
            if (releaseIssue != null) {
                allIssues.add(releaseIssue);

                // Получаем подзадачи
                List<YouTrackIssueDTO> subtasks = getSubtasks(releaseIssueId);
                allIssues.addAll(subtasks);

                // Получаем связанные задачи через links
                List<YouTrackLinkDTO> links = getIssueLinks(releaseIssueId);
                for (YouTrackLinkDTO link : links) {
                    if (isReleaseRelatedLink(link)) {
                        String relatedIssueId = getRelatedIssueId(link, releaseIssueId);
                        if (relatedIssueId != null) {
                            YouTrackIssueDTO relatedIssue = getIssue(relatedIssueId);
                            if (relatedIssue != null) {
                                allIssues.add(relatedIssue);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting all release issues for {}: {}", releaseIssueId, e.getMessage());
            throw new RuntimeException("Failed to get all release issues", e);
        }

        List<YouTrackIssueDTO> result = new ArrayList<>(allIssues);
        log.info("Found {} total issues for release {}", result.size(), releaseIssueId);
        return result;
    }

    /**
     * Получение статистики по задачам релиза
     */
    public Map<String, Object> getReleaseStatistics(List<String> issueIds) {
        log.info("Calculating statistics for {} issues", issueIds.size());

        Map<String, Object> stats = new HashMap<>();
        Map<String, Integer> statusCounts = new HashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();
        Map<String, Integer> priorityCounts = new HashMap<>();
        Map<String, Integer> assigneeCounts = new HashMap<>();

        int totalIssues = 0;
        int validForRelease = 0;

        for (String issueId : issueIds) {
            try {
                YouTrackIssueDTO issue = getIssue(issueId);
                if (issue != null) {
                    totalIssues++;

                    // Подсчет по статусам
                    String status = issue.getStatus();
                    if (status != null) {
                        statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
                        if (releaseStatuses.contains(status)) {
                            validForRelease++;
                        }
                    }

                    // Подсчет по типам
                    String type = issue.getType();
                    if (type != null) {
                        typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
                    }

                    // Подсчет по приоритетам
                    String priority = issue.getPriority();
                    if (priority != null) {
                        priorityCounts.put(priority, priorityCounts.getOrDefault(priority, 0) + 1);
                    }

                    // Подсчет по исполнителям
                    if (issue.getAssignee() != null && issue.getAssignee().getName() != null) {
                        String assignee = issue.getAssignee().getName();
                        assigneeCounts.put(assignee, assigneeCounts.getOrDefault(assignee, 0) + 1);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing issue {} for statistics: {}", issueId, e.getMessage());
            }
        }

        stats.put("totalIssues", totalIssues);
        stats.put("validForRelease", validForRelease);
        stats.put("invalidForRelease", totalIssues - validForRelease);
        stats.put("statusDistribution", statusCounts);
        stats.put("typeDistribution", typeCounts);
        stats.put("priorityDistribution", priorityCounts);
        stats.put("assigneeDistribution", assigneeCounts);

        log.info("Statistics calculated: {} total, {} valid for release", totalIssues, validForRelease);
        return stats;
    }

    /**
     * Поиск релизных задач по версии или тегу
     */
    public List<YouTrackIssueDTO> findReleaseIssues(String version) {
        log.info("Searching for release issues with version: {}", version);

        // Создаем запрос для поиска релизных задач
        String query = String.format("(Fix versions: %s OR tag: release-%s OR summary: %s) and State: {%s}",
                version,
                version,
                version,
                String.join(",", releaseStatuses));

        return searchIssues(query);
    }

    /**
     * Обновление статуса задачи
     */
    public boolean updateIssueStatus(String issueId, String newStatus) {
        try {
            log.info("Updating issue {} status to: {}", issueId, newStatus);

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("State", newStatus);

            webClient.post()
                    .uri("/api/issues/{issueId}", issueId)
                    .bodyValue(updateData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully updated issue {} status to {}", issueId, newStatus);
            return true;
        } catch (Exception e) {
            log.error("Failed to update issue {} status: {}", issueId, e.getMessage());
            return false;
        }
    }

    /**
     * Добавление комментария к задаче
     */
    public boolean addComment(String issueId, String comment) {
        try {
            log.info("Adding comment to issue: {}", issueId);

            Map<String, Object> commentData = new HashMap<>();
            commentData.put("text", comment);

            webClient.post()
                    .uri("/api/issues/{issueId}/comments", issueId)
                    .bodyValue(commentData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully added comment to issue: {}", issueId);
            return true;
        } catch (Exception e) {
            log.error("Failed to add comment to issue {}: {}", issueId, e.getMessage());
            return false;
        }
    }

    /**
     * Получение списка всех проектов YouTrack
     */
    public List<Map<String, Object>> getAllProjects() {
        try {
            log.info("Fetching all YouTrack projects");

            // Используем правильный endpoint для получения проектов
            String uri = "/api/admin/projects?fields=id,name,shortName,description,archived";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projects = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (projects == null) {
                log.warn("No projects returned from YouTrack");
                return new ArrayList<>();
            }

            log.info("Successfully fetched {} projects from YouTrack", projects.size());

            return projects.stream()
                    .filter(project -> {
                        // Фильтруем неархивированные проекты
                        Boolean archived = (Boolean) project.get("archived");
                        return archived == null || !archived;
                    })
                    .map(project -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", project.getOrDefault("shortName", ""));
                        result.put("shortName", project.getOrDefault("shortName", ""));
                        result.put("name", project.getOrDefault("name", ""));
                        result.put("description", project.getOrDefault("description", ""));
                        return result;
                    })
                    .collect(Collectors.toList());

        } catch (WebClientResponseException e) {
            log.error("YouTrack API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            // Если нет прав на admin API, попробуем альтернативный способ
            if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                log.info("Admin API not accessible, trying alternative approach");
                return getProjectsAlternative();
            }

            throw new RuntimeException("Failed to get YouTrack projects: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get YouTrack projects", e);
            throw new RuntimeException("Failed to get YouTrack projects: " + e.getMessage());
        }
    }

    /**
     * Альтернативный способ получения проектов (без admin API)
     */
    private List<Map<String, Object>> getProjectsAlternative() {
        try {
            log.info("Using alternative method to get projects");

            // Получаем проекты через поиск задач
            String uri = "/api/issues?fields=project(id,name,shortName)&$top=1000";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (issues == null) {
                return new ArrayList<>();
            }

            // Извлекаем уникальные проекты из задач
            Set<Map<String, Object>> uniqueProjects = new HashSet<>();

            for (Map<String, Object> issue : issues) {
                @SuppressWarnings("unchecked")
                Map<String, Object> project = (Map<String, Object>) issue.get("project");
                if (project != null) {
                    Map<String, Object> projectInfo = new HashMap<>();
                    projectInfo.put("id", project.getOrDefault("shortName", ""));
                    projectInfo.put("shortName", project.getOrDefault("shortName", ""));
                    projectInfo.put("name", project.getOrDefault("name", ""));
                    projectInfo.put("description", "");
                    uniqueProjects.add(projectInfo);
                }
            }

            List<Map<String, Object>> result = new ArrayList<>(uniqueProjects);
            log.info("Found {} unique projects using alternative method", result.size());
            return result;

        } catch (Exception e) {
            log.error("Alternative method also failed", e);
            return new ArrayList<>();
        }
    }

    /**
     * Поиск проектов YouTrack по названию
     */
    public List<Map<String, Object>> searchProjects(String search) {
        try {
            // Получаем все проекты и фильтруем их локально
            List<Map<String, Object>> allProjects = getAllProjects();

            return allProjects.stream()
                    .filter(project -> {
                        String name = (String) project.get("name");
                        String shortName = (String) project.get("shortName");
                        String searchLower = search.toLowerCase();

                        return (name != null && name.toLowerCase().contains(searchLower)) ||
                                (shortName != null && shortName.toLowerCase().contains(searchLower));
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to search YouTrack projects", e);
            throw new RuntimeException("Failed to search YouTrack projects: " + e.getMessage());
        }
    }

    // Вспомогательные методы

    private boolean isDependencyLink(YouTrackLinkDTO link) {
        if (link.getLinkType() == null || link.getLinkType().getName() == null) {
            return false;
        }
        String linkTypeName = link.getLinkType().getName().toLowerCase();
        return linkTypeName.contains("depend") ||
                linkTypeName.contains("block") ||
                linkTypeName.contains("require");
    }

    private boolean isReleaseRelatedLink(YouTrackLinkDTO link) {
        if (link.getLinkType() == null || link.getLinkType().getName() == null) {
            return false;
        }
        String linkTypeName = link.getLinkType().getName().toLowerCase();
        return linkTypeName.contains("relate") ||
                linkTypeName.contains("include") ||
                linkTypeName.contains("subtask") ||
                linkTypeName.contains("parent");
    }

    private String getDependentIssueId(YouTrackLinkDTO link, String currentIssueId) {
        if (link.getSource() != null && currentIssueId.equals(link.getSource().getId())) {
            return link.getTarget() != null ? link.getTarget().getId() : null;
        } else if (link.getTarget() != null && currentIssueId.equals(link.getTarget().getId())) {
            return link.getSource() != null ? link.getSource().getId() : null;
        }
        return null;
    }

    private String getRelatedIssueId(YouTrackLinkDTO link, String currentIssueId) {
        return getDependentIssueId(link, currentIssueId);
    }

    private boolean isExternalProject(String issueId1, String issueId2) {
        // Простая проверка по префиксу проекта (например, PROJ-123)
        if (issueId1 == null || issueId2 == null) {
            return false;
        }

        String[] parts1 = issueId1.split("-");
        String[] parts2 = issueId2.split("-");

        if (parts1.length == 0 || parts2.length == 0) {
            return false;
        }

        String project1 = parts1[0];
        String project2 = parts2[0];
        return !project1.equals(project2);
    }
}

