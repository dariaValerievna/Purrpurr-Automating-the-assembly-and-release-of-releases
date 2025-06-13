package com.example.myapp.service;

import com.example.myapp.dto.ReleaseDTO;
import com.example.myapp.model.Project;
import com.example.myapp.model.Release;
import com.example.myapp.model.ReleaseStatus;
import com.example.myapp.model.ReleaseActivity;
import com.example.myapp.model.ActivityStatus;
import com.example.myapp.repository.ProjectRepository;
import com.example.myapp.repository.ReleaseRepository;
import com.example.myapp.repository.ReleaseActivityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ProjectRepository projectRepository;
    private final ReleaseActivityRepository releaseActivityRepository;
    private final GitLabService gitLabService;

    public ReleaseService(ReleaseRepository releaseRepository,
                          ProjectRepository projectRepository,
                          ReleaseActivityRepository releaseActivityRepository,
                          GitLabService gitLabService) {
        this.releaseRepository = releaseRepository;
        this.projectRepository = projectRepository;
        this.releaseActivityRepository = releaseActivityRepository;
        this.gitLabService = gitLabService;
    }

    // Методы для YouTrack интеграции
    public Release save(Release release) {
        log.info("Saving release: {}", release.getName() != null ? release.getName() : "unnamed");
        return releaseRepository.save(release);
    }

    public Release findById(Long id) {
        log.info("Finding release by ID: {}", id);
        return releaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Release not found with ID: " + id));
    }

    public Optional<Release> findByIdOptional(Long id) {
        return releaseRepository.findById(id);
    }

    public List<Release> findAll() {
        return releaseRepository.findAll();
    }

    public List<Release> findByProjectId(Long projectId) {
        return releaseRepository.findByProjectId(projectId);
    }

    // DTO методы
    public ReleaseDTO createRelease(ReleaseDTO releaseDTO) {
        log.info("Creating release for project ID: {}", releaseDTO.getProjectId());
        Project project = projectRepository.findById(releaseDTO.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID: " + releaseDTO.getProjectId()));

        Release release = new Release();
        release.setProject(project);
        release.setReleaseBranch(releaseDTO.getReleaseBranch());
        release.setSourceBranch(releaseDTO.getSourceBranch());
        release.setYoutrackReleaseTaskId(releaseDTO.getYoutrackReleaseTaskId());
        release.setCreatedAt(LocalDateTime.now());
        release.setVersion(releaseDTO.getVersion());
        release.setReleaseNotes(releaseDTO.getReleaseNotes());
        release.setCreatedBy(releaseDTO.getCreatedBy());

        // Устанавливаем статус
        if (releaseDTO.getStatus() != null) {
            release.setStatus(releaseDTO.getStatus());
        } else {
            release.setStatus(ReleaseStatus.CREATED);
        }

        // Устанавливаем name и description если они не заданы
        if (releaseDTO.getName() != null) {
            release.setName(releaseDTO.getName());
        } else if (release.getName() == null || release.getName().isEmpty()) {
            release.setName("Release " + release.getVersion());
        }

        if (releaseDTO.getDescription() != null) {
            release.setDescription(releaseDTO.getDescription());
        } else if (release.getDescription() == null || release.getDescription().isEmpty()) {
            release.setDescription("Release created via Release Manager");
        }

        Release savedRelease = releaseRepository.save(release);

        // Логируем создание релиза
        logReleaseActivity(savedRelease, "RELEASE_CREATED",
                "Release created successfully", releaseDTO.getCreatedBy());

        log.info("Release created with ID: {}", savedRelease.getId());
        return convertToDTO(savedRelease);
    }

    public List<ReleaseDTO> getReleasesByProject(Long projectId) {
        log.info("Getting releases for project ID: {}", projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID: " + projectId));

        return releaseRepository.findByProjectId(projectId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<ReleaseDTO> getAllReleases() {
        log.info("Getting all releases");
        return releaseRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public ReleaseDTO getReleaseById(Long id) {
        log.info("Getting release by ID: {}", id);
        Release release = findById(id);
        return convertToDTO(release);
    }

    public ReleaseDTO updateRelease(Long id, ReleaseDTO releaseDTO) {
        log.info("Updating release ID: {}", id);
        Release release = findById(id);

        // Обновляем поля
        Optional.ofNullable(releaseDTO.getStatus()).ifPresent(release::setStatus);
        Optional.ofNullable(releaseDTO.getVersion()).ifPresent(release::setVersion);
        Optional.ofNullable(releaseDTO.getReleaseBranch()).ifPresent(release::setReleaseBranch);
        Optional.ofNullable(releaseDTO.getReleaseNotes()).ifPresent(release::setReleaseNotes);
        Optional.ofNullable(releaseDTO.getGitlabReleaseUrl()).ifPresent(release::setGitlabReleaseUrl);
        Optional.ofNullable(releaseDTO.getGitlabMergeRequestUrl()).ifPresent(release::setGitlabMergeRequestUrl);
        Optional.ofNullable(releaseDTO.getGitlabTagName()).ifPresent(release::setGitlabTagName);
        Optional.ofNullable(releaseDTO.getName()).ifPresent(release::setName);
        Optional.ofNullable(releaseDTO.getDescription()).ifPresent(release::setDescription);

        // Если статус изменился на COMPLETED, устанавливаем время завершения
        if (releaseDTO.getStatus() == ReleaseStatus.COMPLETED && release.getCompletedAt() == null) {
            release.setCompletedAt(LocalDateTime.now());
            logReleaseActivity(release, "RELEASE_COMPLETED",
                    "Release completed successfully", releaseDTO.getCreatedBy());
        }

        Release savedRelease = releaseRepository.save(release);
        log.info("Release updated: {}", savedRelease.getId());
        return convertToDTO(savedRelease);
    }

    public void deleteRelease(Long id) {
        log.info("Deleting release ID: {}", id);
        Release release = findById(id);
        logReleaseActivity(release, "RELEASE_DELETED",
                "Release deleted", "system");
        releaseRepository.delete(release);
        log.info("Release deleted: {}", id);
    }

    // Методы для работы с GitLab интеграцией
    public ReleaseDTO updateGitlabInfo(Long releaseId, String gitlabReleaseUrl,
                                       String gitlabMergeRequestUrl, String gitlabTagName) {
        log.info("Updating GitLab info for release ID: {}", releaseId);
        Release release = findById(releaseId);
        release.setGitlabReleaseUrl(gitlabReleaseUrl);
        release.setGitlabMergeRequestUrl(gitlabMergeRequestUrl);
        release.setGitlabTagName(gitlabTagName);

        Release savedRelease = releaseRepository.save(release);
        logReleaseActivity(release, "GITLAB_INFO_UPDATED",
                "GitLab integration info updated", "system");
        return convertToDTO(savedRelease);
    }

    // Получить релизы по статусу
    public List<ReleaseDTO> getReleasesByStatus(ReleaseStatus status) {
        log.info("Getting releases by status: {}", status);
        return releaseRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    // Создание ветки релиза
    public ReleaseDTO createReleaseBranch(Long releaseId, String sourceBranch) {
        log.info("Creating release branch for release ID: {}, source branch: {}", releaseId, sourceBranch);
        Release release = findById(releaseId);
        Project project = release.getProject();

        String releaseBranchName = generateReleaseBranchName(release);
        Long gitlabProjectId = Long.valueOf(project.getGitlabProjectId());

        try {
            // Проверяем, существует ли уже ветка
            if (gitLabService.branchExists(gitlabProjectId, releaseBranchName)) {
                logReleaseActivity(release, "BRANCH_EXISTS",
                        "Release branch already exists: " + releaseBranchName, "system");
                throw new IllegalStateException("Release branch already exists: " + releaseBranchName);
            }

            // Создаем ветку релиза
            gitLabService.createReleaseBranch(gitlabProjectId, sourceBranch, releaseBranchName);

            // Обновляем информацию о релизе
            release.setReleaseBranch(releaseBranchName);
            release.setSourceBranch(sourceBranch);
            release.setStatus(ReleaseStatus.BRANCH_CREATED);

            Release savedRelease = releaseRepository.save(release);

            logReleaseActivity(release, "BRANCH_CREATED",
                    "Release branch created: " + releaseBranchName + " from " + sourceBranch, "system");

            log.info("Release branch created successfully: {}", releaseBranchName);
            return convertToDTO(savedRelease);

        } catch (Exception e) {
            logReleaseActivity(release, "BRANCH_CREATION_FAILED",
                    "Failed to create branch: " + e.getMessage(), "system");
            log.error("Failed to create release branch: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create release branch: " + e.getMessage(), e);
        }
    }

    // Конвертация в DTO
    public ReleaseDTO convertToDTO(Release release) {
        ReleaseDTO dto = new ReleaseDTO();
        dto.setId(release.getId());
        dto.setProjectId(release.getProject().getId());
        dto.setProjectName(release.getProject().getName());
        dto.setReleaseBranch(release.getReleaseBranch());
        dto.setSourceBranch(release.getSourceBranch());
        dto.setYoutrackReleaseTaskId(release.getYoutrackReleaseTaskId());
        dto.setCreatedAt(release.getCreatedAt());
        dto.setCompletedAt(release.getCompletedAt());
        dto.setStatus(release.getStatus());
        dto.setVersion(release.getVersion());
        dto.setName(release.getName());
        dto.setDescription(release.getDescription());

        // GitLab поля
        dto.setGitlabReleaseUrl(release.getGitlabReleaseUrl());
        dto.setGitlabMergeRequestUrl(release.getGitlabMergeRequestUrl());
        dto.setGitlabTagName(release.getGitlabTagName());
        dto.setReleaseNotes(release.getReleaseNotes());
        dto.setCreatedBy(release.getCreatedBy());

        return dto;
    }

    // Конвертация из DTO
    private Release convertToEntity(ReleaseDTO dto) {
        Release release = new Release();
        release.setId(dto.getId());
        release.setYoutrackReleaseTaskId(dto.getYoutrackReleaseTaskId());
        release.setVersion(dto.getVersion());
        release.setSourceBranch(dto.getSourceBranch());
        release.setReleaseBranch(dto.getReleaseBranch());
        release.setName(dto.getName());
        release.setDescription(dto.getDescription());
        release.setCreatedAt(dto.getCreatedAt());
        release.setCompletedAt(dto.getCompletedAt());
        release.setCreatedBy(dto.getCreatedBy());
        release.setGitlabReleaseUrl(dto.getGitlabReleaseUrl());
        release.setGitlabMergeRequestUrl(dto.getGitlabMergeRequestUrl());
        release.setGitlabTagName(dto.getGitlabTagName());
        release.setReleaseNotes(dto.getReleaseNotes());

        // Конвертация статуса
        if (dto.getStatus() != null) {
            release.setStatus(dto.getStatus());
        } else {
            release.setStatus(ReleaseStatus.CREATED);
        }

        return release;
    }

    // Метод для логирования активности
    private void logReleaseActivity(Release release, String action, String description, String performedBy) {
        try {
            ReleaseActivity activity = new ReleaseActivity();
            activity.setRelease(release);
            activity.setAction(action);
            activity.setDescription(description);
            activity.setPerformedBy(performedBy != null ? performedBy : "system");
            activity.setTimestamp(LocalDateTime.now());
            activity.setStatus(ActivityStatus.SUCCESS);
            releaseActivityRepository.save(activity);
        } catch (Exception e) {
            log.error("Failed to log release activity: {}", e.getMessage());
        }
    }

    // Генерация имени ветки релиза
    private String generateReleaseBranchName(Release release) {
        if (release.getVersion() != null && !release.getVersion().isEmpty()) {
            return "release/" + release.getVersion();
        } else {
            // Если версия не указана, используем ID релиза и дату
            return "release/" + release.getId() + "-" +
                    release.getCreatedAt().toLocalDate().toString();
        }
    }
}

