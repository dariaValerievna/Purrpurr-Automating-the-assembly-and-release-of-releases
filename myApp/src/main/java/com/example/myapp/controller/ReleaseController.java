package com.example.myapp.controller;

import com.example.myapp.dto.ReleaseDTO;
import com.example.myapp.service.ReleaseService;
import com.example.myapp.service.ReleaseDiffService;
import com.example.myapp.service.GitLabService;
import com.example.myapp.service.YouTrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;
    private final ReleaseDiffService releaseDiffService;
    private final GitLabService gitLabService;
    private final YouTrackService youTrackService;

    /**
     * Получение всех релизов
     */
    @GetMapping
    public ResponseEntity<List<ReleaseDTO>> getAllReleases() {
        log.info("Getting all releases");
        List<ReleaseDTO> releases = releaseService.getAllReleases();
        return ResponseEntity.ok(releases);
    }


    /**
     * Получение всех проектов GitLab
     */
    @GetMapping("/gitlab")
    public ResponseEntity<List<Map<String, Object>>> getAllGitLabProjects() {
        log.info("Getting all GitLab projects");
        try {
            List<Map<String, Object>> projects = gitLabService.getAllProjects();
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Failed to get GitLab projects", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Поиск проектов GitLab
     */
    @GetMapping("/gitlab/search")
    public ResponseEntity<List<Map<String, Object>>> searchGitLabProjects(
            @RequestParam String query) {
        log.info("Searching GitLab projects with query: {}", query);
        try {
            List<Map<String, Object>> projects = gitLabService.searchProjects(query);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Failed to search GitLab projects", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ========== YouTrack Projects ==========

    /**
     * Получение всех проектов YouTrack
     */
    @GetMapping("/youtrack")
    public ResponseEntity<List<Map<String, Object>>> getAllYouTrackProjects() {
        log.info("Getting all YouTrack projects");
        try {
            List<Map<String, Object>> projects = youTrackService.getAllProjects();
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Failed to get YouTrack projects", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Поиск проектов YouTrack
     */
    @GetMapping("/youtrack/search")
    public ResponseEntity<List<Map<String, Object>>> searchYouTrackProjects(
            @RequestParam String query) {
        log.info("Searching YouTrack projects with query: {}", query);
        try {
            List<Map<String, Object>> projects = youTrackService.searchProjects(query);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Failed to search YouTrack projects", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Получение релизов проекта
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ReleaseDTO>> getReleasesByProject(@PathVariable Long projectId) {
        log.info("Getting releases for project: {}", projectId);
        List<ReleaseDTO> releases = releaseService.getReleasesByProject(projectId);
        return ResponseEntity.ok(releases);
    }

    /**
     * Получение информации о релизе
     */
    @GetMapping("/{releaseId}")
    public ResponseEntity<ReleaseDTO> getReleaseById(@PathVariable Long releaseId) {
        log.info("Getting release: {}", releaseId);
        ReleaseDTO release = releaseService.getReleaseById(releaseId);
        return ResponseEntity.ok(release);
    }

    /**
     * Создание релиза
     */
    @PostMapping
    public ResponseEntity<ReleaseDTO> createRelease(@RequestBody ReleaseDTO releaseDTO) {
        log.info("Creating release for project: {}", releaseDTO.getProjectId());
        ReleaseDTO created = releaseService.createRelease(releaseDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * Обновление релиза
     */
    @PutMapping("/{releaseId}")
    public ResponseEntity<ReleaseDTO> updateRelease(@PathVariable Long releaseId,
                                                    @RequestBody ReleaseDTO releaseDTO) {
        log.info("Updating release: {}", releaseId);
        ReleaseDTO updated = releaseService.updateRelease(releaseId, releaseDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Получение diff коммитов релиза
     */
    @GetMapping("/{releaseId}/diff")
    public ResponseEntity<Map<String, Object>> getReleaseDiff(@PathVariable Long releaseId) {
        log.info("Getting diff for release: {}", releaseId);
        try {
            Map<String, Object> diff = releaseDiffService.getReleaseDiff(releaseId);
            return ResponseEntity.ok(diff);
        } catch (Exception e) {
            log.error("Failed to get diff for release: {}", releaseId, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Получение задач релиза
     */
    @GetMapping("/{releaseId}/tasks")
    public ResponseEntity<Map<String, Object>> getReleaseTasks(@PathVariable Long releaseId) {
        log.info("Getting tasks for release: {}", releaseId);
        try {
            Map<String, Object> tasks = releaseDiffService.getReleaseTasks(releaseId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Failed to get tasks for release: {}", releaseId, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Добавление задачи в релиз
     */
    @PostMapping("/{releaseId}/add-task")
    public ResponseEntity<Map<String, Object>> addTaskToRelease(
            @PathVariable Long releaseId,
            @RequestBody Map<String, String> request) {
        String taskId = request.get("taskId");
        log.info("Adding task {} to release: {}", taskId, releaseId);
        try {
            Map<String, Object> result = releaseDiffService.addTaskToRelease(releaseId, taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to add task {} to release: {}", taskId, releaseId, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Анализ расхождений в релизе
     */
    @GetMapping("/{releaseId}/analysis")
    public ResponseEntity<Map<String, Object>> analyzeRelease(@PathVariable Long releaseId) {
        log.info("Analyzing release: {}", releaseId);
        try {
            Map<String, Object> analysis = releaseDiffService.analyzeRelease(releaseId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Failed to analyze release: {}", releaseId, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    /**
     * Создание ветки релиза
     */
    @PostMapping("/{releaseId}/create-branch")
    public ResponseEntity<ReleaseDTO> createReleaseBranch(
            @PathVariable Long releaseId,
            @RequestBody Map<String, String> request) {
        String sourceBranch = request.get("sourceBranch");
        log.info("Creating branch for release: {} from source: {}", releaseId, sourceBranch);
        try {
            ReleaseDTO updated = releaseService.createReleaseBranch(releaseId, sourceBranch);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to create branch for release: {}", releaseId, e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}
