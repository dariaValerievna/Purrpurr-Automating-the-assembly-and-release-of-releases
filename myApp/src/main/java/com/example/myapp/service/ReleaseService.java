package com.example.myapp.service;

import com.example.myapp.dto.ReleaseDTO;
import com.example.myapp.model.Project;
import com.example.myapp.model.Release;
import com.example.myapp.repository.ProjectRepository;
import com.example.myapp.repository.ReleaseRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReleaseService {
    private final ReleaseRepository releaseRepository;
    private final ProjectRepository projectRepository;

    public ReleaseService(ReleaseRepository releaseRepository, ProjectRepository projectRepository) {
        this.releaseRepository = releaseRepository;
        this.projectRepository = projectRepository;
    }

    public ReleaseDTO createRelease(ReleaseDTO releaseDTO) {
        Project project = projectRepository.findById(releaseDTO.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        Release release = new Release();
        release.setProject(project);
        release.setReleaseBranch(releaseDTO.getReleaseBranch());
        release.setSourceBranch(releaseDTO.getSourceBranch());
        release.setYoutrackReleaseTaskId(releaseDTO.getYoutrackReleaseTaskId());
        release.setCreatedAt(LocalDateTime.now());
        release.setStatus(releaseDTO.getStatus());

        Release savedRelease = releaseRepository.save(release);
        return convertToDTO(savedRelease);
    }

    public List<ReleaseDTO> getReleasesByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        return releaseRepository.findByProject(project).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ReleaseDTO convertToDTO(Release release) {
        ReleaseDTO dto = new ReleaseDTO();
        dto.setId(release.getId());
        dto.setProjectId(release.getProject().getId());
        dto.setReleaseBranch(release.getReleaseBranch());
        dto.setSourceBranch(release.getSourceBranch());
        dto.setYoutrackReleaseTaskId(release.getYoutrackReleaseTaskId());
        dto.setCreatedAt(release.getCreatedAt());
        dto.setCompletedAt(release.getCompletedAt());
        dto.setStatus(release.getStatus());
        dto.setVersion(release.getVersion());
        dto.setBranchName(release.getBranchName());
        return dto;
    }

    public List<ReleaseDTO> getAllReleases() {
        List<Release> releases = releaseRepository.findAll();
        return releases.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /*public void createReleaseBranch(Long releaseId, String sourceBranch) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new EntityNotFoundException("Release not found"));

        Project project = release.getProject();
        String releaseBranchName = "release/" + release.getVersion();

        Long gitlabProjectId = Long.valueOf(project.getGitlabProjectId());

        if (gitLabService.branchExists(gitlabProjectId, releaseBranchName)) {
            throw new IllegalStateException("Release branch already exists");
        }

        gitLabService.createReleaseBranch(gitlabProjectId, sourceBranch, releaseBranchName);

        String mrTitle = "Release " + release.getVersion();
        gitLabService.createMergeRequest(gitlabProjectId, releaseBranchName, "main", mrTitle);

        release.setBranchName(releaseBranchName);
        releaseRepository.save(release);
    }
    */
}
