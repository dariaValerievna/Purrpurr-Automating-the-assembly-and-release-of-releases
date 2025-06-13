package com.example.myapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;  // ← Правильный импорт
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;  // ← Добавить импорт
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitLabService {

    private final WebClient webClient;
    private final RestTemplate restTemplate;  // ← Добавить поле
    private final ObjectMapper objectMapper;
    private final String gitlabUrl;
    private final String gitlabToken;  // ← Добавить поле

    public GitLabService(@Value("${gitlab.url}") String gitlabUrl,
                         @Value("${gitlab.token}") String gitlabToken) {
        this.gitlabUrl = gitlabUrl;
        this.gitlabToken = gitlabToken;  // ← Инициализировать
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();  // ← Инициализировать

        this.webClient = WebClient.builder()
                .baseUrl(gitlabUrl)
                .defaultHeader("PRIVATE-TOKEN", gitlabToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Создание ветки
     */
    public Map<String, Object> createBranch(Long projectId, String branchName, String ref) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("branch", branchName);
            requestBody.put("ref", ref);

            String response = webClient.post()
                    .uri("/api/v4/projects/{id}/repository/branches", projectId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Branch created: {} from {}", branchName, ref);
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (WebClientResponseException e) {
            log.error("Failed to create branch: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create branch: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to parse branch creation response", e);
            throw new RuntimeException("Failed to create branch: " + e.getMessage(), e);
        }
    }

    /**
     * Проверка существования ветки
     */
    public boolean branchExists(Long projectId, String branchName) {
        try {
            webClient.get()
                    .uri("/api/v4/projects/{id}/repository/branches/{branch}", projectId, branchName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }
            log.error("Error checking branch existence: {}", e.getMessage());
            throw new RuntimeException("Failed to check branch existence", e);
        }
    }

    /**
     * Создание релизной ветки
     */
    public String createReleaseBranch(Long projectId, String sourceBranch, String releaseBranchName) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("branch", releaseBranchName);
            requestBody.put("ref", sourceBranch);

            String response = webClient.post()
                    .uri("/api/v4/projects/{id}/repository/branches", projectId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Release branch created: {} from {}", releaseBranchName, sourceBranch);
            return response;
        } catch (WebClientResponseException e) {
            log.error("Failed to create release branch: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create release branch: " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Получение веток проекта (возвращает List<Map<String, Object>>)
     */
    public List<Map<String, Object>> getBranches(Long projectId) {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects/{id}/repository/branches", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to get branches for project: {}", projectId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Создание Merge Request
     */
    public Map<String, Object> createMergeRequest(Long projectId, String sourceBranch, String targetBranch, String title, String description) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("source_branch", sourceBranch);
            requestBody.put("target_branch", targetBranch);
            requestBody.put("title", title);
            requestBody.put("description", description);

            String response = webClient.post()
                    .uri("/api/v4/projects/{id}/merge_requests", projectId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Merge request created: {} -> {}", sourceBranch, targetBranch);
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (WebClientResponseException e) {
            log.error("Failed to create merge request: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create merge request: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to parse merge request response", e);
            throw new RuntimeException("Failed to create merge request: " + e.getMessage(), e);
        }
    }

    /**
     * Создание тега
     */
    public String createTag(Long projectId, String tagName, String ref, String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("tag_name", tagName);
            requestBody.put("ref", ref);
            requestBody.put("message", message);

            String response = webClient.post()
                    .uri("/api/v4/projects/{id}/repository/tags", projectId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Tag created: {} at {}", tagName, ref);
            return response;
        } catch (WebClientResponseException e) {
            log.error("Failed to create tag: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create tag: " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Получение коммитов между двумя ветками/тегами
     */
    public List<Map<String, Object>> getCommitsDiff(Long projectId, String from, String to) {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects/{id}/repository/compare?from={from}&to={to}", projectId, from, to)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> compareResult = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            if (compareResult != null && compareResult.containsKey("commits")) {
                return (List<Map<String, Object>>) compareResult.get("commits");
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get commits diff between {} and {}", from, to, e);
            throw new RuntimeException("Failed to get commits diff: " + e.getMessage());
        }
    }

    /**
     * Получение коммитов между двумя ветками/тегами (альтернативное название)
     */
    public List<Map<String, Object>> getCommitsBetween(Long projectId, String from, String to) {
        return getCommitsDiff(projectId, from, to);
    }

    /**
     * Получение тегов проекта
     */
    public List<Map<String, Object>> getTags(Long projectId) {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects/{id}/repository/tags", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to get tags for project: {}", projectId, e);
            throw new RuntimeException("Failed to get tags: " + e.getMessage());
        }
    }

    /**
     * Получение коммитов ветки
     */
    public List<Map<String, Object>> getBranchCommits(Long projectId, String branchName) {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects/{id}/repository/commits?ref_name={branch}&per_page=100", projectId, branchName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to get commits for branch: {}", branchName, e);
            throw new RuntimeException("Failed to get branch commits: " + e.getMessage());
        }
    }

    /**
     * Получение информации о Merge Request
     */
    public Map<String, Object> getMergeRequest(Long projectId, Long mergeRequestIid) {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects/{id}/merge_requests/{iid}", projectId, mergeRequestIid)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to get merge request: {}", mergeRequestIid, e);
            throw new RuntimeException("Failed to get merge request: " + e.getMessage());
        }
    }

    /**
     * Принятие Merge Request
     */
    public Map<String, Object> acceptMergeRequest(Long projectId, Long mergeRequestIid) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "should_remove_source_branch", true,
                    "merge_when_pipeline_succeeds", false
            );

            String response = webClient.put()
                    .uri("/api/v4/projects/{id}/merge_requests/{iid}/merge", projectId, mergeRequestIid)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to accept merge request: {}", mergeRequestIid, e);
            throw new RuntimeException("Failed to accept merge request: " + e.getMessage());
        }
    }

    // ========== Методы для получения списков проектов ==========

    /**
     * Получение списка всех проектов GitLab
     */
    public List<Map<String, Object>> getAllProjects() {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects?membership=true&per_page=100")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<Map<String, Object>> projects = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});

            // Возвращаем только нужные поля
            return projects.stream()
                    .map(project -> Map.of(
                            "id", project.get("id"),
                            "name", project.get("name"),
                            "path", project.get("path"),
                            "nameWithNamespace", project.get("name_with_namespace"),
                            "defaultBranch", project.get("default_branch"),
                            "webUrl", project.get("web_url")
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get GitLab projects", e);
            throw new RuntimeException("Failed to get GitLab projects: " + e.getMessage());
        }
    }

    /**
     * Поиск проектов GitLab по названию
     */
    public List<Map<String, Object>> searchProjects(String search) {
        try {
            String encodedSearch = URLEncoder.encode(search, StandardCharsets.UTF_8);
            String response = webClient.get()
                    .uri("/api/v4/projects?search={search}&membership=true&per_page=50", encodedSearch)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<Map<String, Object>> projects = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});

            return projects.stream()
                    .map(project -> Map.of(
                            "id", project.get("id"),
                            "name", project.get("name"),
                            "nameWithNamespace", project.get("name_with_namespace"),
                            "defaultBranch", project.get("default_branch")
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to search GitLab projects", e);
            throw new RuntimeException("Failed to search GitLab projects: " + e.getMessage());
        }
    }

    /**
     * Проверка существования проекта
     */
    public boolean projectExists(String projectId) {
        try {
            webClient.get()
                    .uri("/api/v4/projects/{id}", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }
            log.error("Error checking project existence: {}", e.getMessage());
            throw new RuntimeException("Failed to check project existence", e);
        }
    }

    // ========== Устаревшие методы (для обратной совместимости) ==========
    // Рекомендуется использовать методы выше, которые возвращают типизированные объекты

    /**
     * @deprecated Используйте getAllProjects() вместо этого метода
     */
    @Deprecated
    public String getProjects() {
        try {
            return webClient.get()
                    .uri("/api/v4/projects?membership=true")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get projects: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * @deprecated Используйте projectExists() или создайте getProject(Long projectId)
     */
    @Deprecated
    public String getProject(String projectId) {
        try {
            return webClient.get()
                    .uri("/api/v4/projects/{id}", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get project: {}", e.getMessage());
            return null;
        }
    }

    /**
     * @deprecated Используйте getBranches(Long projectId) вместо этого метода
     */
    @Deprecated
    public String getBranches(String projectId) {
        try {
            return webClient.get()
                    .uri("/api/v4/projects/{id}/repository/branches", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get branches: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * @deprecated Используйте getTags(Long projectId) вместо этого метода
     */
    @Deprecated
    public String getTags(String projectId) {
        try {
            return webClient.get()
                    .uri("/api/v4/projects/{id}/repository/tags", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get tags: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * Получение релизов проекта
     */
    public String getReleases(String projectId) {
        try {
            return webClient.get()
                    .uri("/api/v4/projects/{id}/releases", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get releases: {}", e.getMessage());
            return "[]";
        }
    }

    // ========== Дополнительные полезные методы ==========

    /**
     * Получение информации о проекте
     */
    public Map<String, Object> getProjectInfo(Long projectId) {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects/{id}", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to get project info: {}", projectId, e);
            throw new RuntimeException("Failed to get project info: " + e.getMessage());
        }
    }

    /**
     * Получение списка веток (только названия)
     */
    public List<String> getBranchNames(Long projectId) {
        try {
            List<Map<String, Object>> branches = getBranches(projectId);
            return branches.stream()
                    .map(branch -> (String) branch.get("name"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get branch names for project: {}", projectId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Получение списка тегов (только названия)
     */
    public List<String> getTagNames(Long projectId) {
        try {
            List<Map<String, Object>> tags = getTags(projectId);
            return tags.stream()
                    .map(tag -> (String) tag.get("name"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get tag names for project: {}", projectId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Получение последнего тега проекта
     */
    public String getLatestTag(Long projectId) {
        try {
            List<Map<String, Object>> tags = getTags(projectId);
            if (!tags.isEmpty()) {
                return (String) tags.get(0).get("name"); // Теги отсортированы по дате создания
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get latest tag for project: {}", projectId, e);
            return null;
        }
    }

    /**
     * Проверка статуса Merge Request
     */
    public String getMergeRequestStatus(Long projectId, Long mergeRequestIid) {
        try {
            Map<String, Object> mr = getMergeRequest(projectId, mergeRequestIid);
            return (String) mr.get("state");
        } catch (Exception e) {
            log.error("Failed to get merge request status: {}", mergeRequestIid, e);
            return "unknown";
        }
    }

    /**
     * Проверка возможности автоматического слияния MR
     */
    public boolean canMergeAutomatically(Long projectId, Long mergeRequestIid) {
        try {
            Map<String, Object> mr = getMergeRequest(projectId, mergeRequestIid);
            String mergeStatus = (String) mr.get("merge_status");
            String state = (String) mr.get("state");

            return "opened".equals(state) && "can_be_merged".equals(mergeStatus);
        } catch (Exception e) {
            log.error("Failed to check merge status: {}", mergeRequestIid, e);
            return false;
        }
    }

    /**
     * Получение списка Merge Request проекта
     */
    public List<Map<String, Object>> getMergeRequests(Long projectId, String state) {
        try {
            String response = webClient.get()
                    .uri("/api/v4/projects/{id}/merge_requests?state={state}", projectId, state)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to get merge requests for project: {}", projectId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Получение открытых Merge Request
     */
    public List<Map<String, Object>> getOpenMergeRequests(Long projectId) {
        return getMergeRequests(projectId, "opened");
    }

    /**
     * Получение закрытых Merge Request
     */
    public List<Map<String, Object>> getClosedMergeRequests(Long projectId) {
        return getMergeRequests(projectId, "closed");
    }

    /**
     * Получение объединенных Merge Request
     */
    public List<Map<String, Object>> getMergedMergeRequests(Long projectId) {
        return getMergeRequests(projectId, "merged");
    }
}

