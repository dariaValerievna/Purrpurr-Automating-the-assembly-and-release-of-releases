package com.example.myapp.controller;

import com.example.myapp.dto.ProjectDTO;
import com.example.myapp.dto.youtrack.YouTrackIssueDTO;
import com.example.myapp.service.GitLabService;
import com.example.myapp.service.ProjectService;
import com.example.myapp.service.YouTrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final ProjectService projectService;
    private final GitLabService gitLabService;
    private final YouTrackService youTrackService;

    /**
     * Получение списка проектов
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDTO>> getProjects() {
        log.info("Getting projects list");
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Получение веток проекта
     */
    @GetMapping("/projects/{projectId}/branches")
    public ResponseEntity<List<Map<String, Object>>> getProjectBranches(@PathVariable Long projectId) {
        log.info("Getting branches for project: {}", projectId);
        try {
            ProjectDTO project = projectService.getProjectById(projectId);
            Long gitlabProjectId = Long.valueOf(project.getGitlabProjectId());

            // Используем метод, который возвращает List<Map<String, Object>>
            List<Map<String, Object>> branches = gitLabService.getBranches(gitlabProjectId);
            return ResponseEntity.ok(branches);
        } catch (Exception e) {
            log.error("Failed to get branches for project: {}", projectId, e);
            return ResponseEntity.badRequest().build();
        }
    }


    /**
     * Поиск релизных задач в YouTrack
     */
    @GetMapping("/projects/{projectId}/release-tasks")
    public ResponseEntity<List<Map<String, Object>>> getReleaseTasksForProject(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "") String query) {

        log.info("Getting release tasks for project: {}, query: {}", projectId, query);

        try {
            ProjectDTO project = projectService.getProjectById(projectId);
            String searchQuery = buildReleaseTaskQuery(project.getYoutrackProjectId(), query);

            // Конвертируем YouTrackIssueDTO в Map
            List<Map<String, Object>> tasks = youTrackService.searchIssues(searchQuery).stream()
                    .map(this::convertIssueToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            log.error("Failed to get release tasks for project: {}", projectId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Поиск задач в YouTrack
     */
    @GetMapping("/youtrack/search")
    public ResponseEntity<List<Map<String, Object>>> searchYouTrackTasks(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        log.info("Searching YouTrack tasks with query: {}", query);

        try {
            // Конвертируем YouTrackIssueDTO в Map
            List<Map<String, Object>> tasks = youTrackService.searchIssues(query + " #take-" + limit).stream()
                    .map(this::convertIssueToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(tasks);

        } catch (Exception e) {
            log.error("Failed to search YouTrack tasks", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получение информации о задаче YouTrack
     */
    @GetMapping("/youtrack/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getYouTrackTask(@PathVariable String taskId) {
        log.info("Getting YouTrack task: {}", taskId);

        try {
            // Конвертируем YouTrackIssueDTO в Map
            Map<String, Object> task = convertIssueToMap(youTrackService.getIssue(taskId));
            return ResponseEntity.ok(task);

        } catch (Exception e) {
            log.error("Failed to get YouTrack task: {}", taskId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получение связанных задач
     */
    @GetMapping("/youtrack/tasks/{taskId}/linked")
    public ResponseEntity<List<Map<String, Object>>> getLinkedTasks(@PathVariable String taskId) {
        log.info("Getting linked tasks for: {}", taskId);

        try {
            // Конвертируем YouTrackIssueDTO в Map
            List<Map<String, Object>> linkedTasks = youTrackService.getLinkedIssues(taskId).stream()
                    .map(this::convertIssueToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(linkedTasks);

        } catch (Exception e) {
            log.error("Failed to get linked tasks for: {}", taskId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Конвертация YouTrackIssueDTO в Map
     */
    private Map<String, Object> convertIssueToMap(YouTrackIssueDTO issue) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", issue.getId());
        map.put("summary", issue.getSummary());
        map.put("description", issue.getDescription());
        map.put("status", issue.getStatus());
        map.put("priority", issue.getPriority());
        map.put("type", issue.getType());
        map.put("reporter", issue.getReporter());
        map.put("assignee", issue.getAssignee());
        map.put("tags", issue.getTagNames());
        return map;
    }

    /**
     * Формирование поискового запроса для релизных задач
     */
    private String buildReleaseTaskQuery(String youtrackProjectId, String userQuery) {
        StringBuilder query = new StringBuilder();

        if (youtrackProjectId != null && !youtrackProjectId.isEmpty()) {
            query.append("project: ").append(youtrackProjectId).append(" ");
        }

        query.append("(Type: Release OR summary: release OR summary: релиз) ");

        if (userQuery != null && !userQuery.trim().isEmpty()) {
            query.append("AND (").append(userQuery.trim()).append(") ");
        }

        query.append("sort by: created desc");

        return query.toString();
    }
}