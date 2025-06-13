package com.example.myapp.controller;

import com.example.myapp.dto.ReleaseBuildRequest;
import com.example.myapp.dto.ReleaseBuildResult;
import com.example.myapp.service.ReleaseBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/release-builder")
@RequiredArgsConstructor
public class ReleaseBuilderController {

    private final ReleaseBuilderService releaseBuilderService;

    /**
     * Запуск процесса сборки релиза
     */
    @PostMapping("/build")
    public ResponseEntity<ReleaseBuildResult> buildRelease(@Valid @RequestBody ReleaseBuildRequest request) {
        log.info("Starting release build for project: {}, task: {}",
                request.getProjectId(), request.getReleaseTaskId());

        ReleaseBuildResult result = releaseBuilderService.startReleaseBuild(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Получение статуса процесса сборки
     */
    @GetMapping("/status/{buildId}")
    public ResponseEntity<ReleaseBuildResult> getBuildStatus(@PathVariable String buildId) {
        log.info("Getting build status for: {}", buildId);

        ReleaseBuildResult result = releaseBuilderService.getBuildStatus(buildId);
        return ResponseEntity.ok(result);
    }

    /**
     * Предварительная валидация параметров сборки
     */
    @PostMapping("/validate")
    public ResponseEntity<ReleaseBuildResult> validateBuildRequest(@Valid @RequestBody ReleaseBuildRequest request) {
        log.info("Validating build request for project: {}, task: {}",
                request.getProjectId(), request.getReleaseTaskId());

        try {
            // Создаем результат только с валидацией, без фактической сборки
            ReleaseBuildResult result = releaseBuilderService.validateOnly(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Validation failed", e);
            return ResponseEntity.badRequest().body(
                    ReleaseBuildResult.builder()
                            .status(ReleaseBuildResult.BuildStatus.FAILED)
                            .errors(java.util.List.of(e.getMessage()))
                            .build()
            );
        }
    }
}