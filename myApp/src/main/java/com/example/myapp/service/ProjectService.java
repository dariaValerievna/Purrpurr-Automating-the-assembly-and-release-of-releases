package com.example.myapp.service;

import com.example.myapp.dto.ProjectDTO;
import com.example.myapp.model.Project;
import com.example.myapp.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // ✅ Добавь этот метод
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
    }

    public ProjectDTO createProject(ProjectDTO projectDTO) {
        Project project = new Project();
        project.setName(projectDTO.getName());
        project.setGitlabProjectId(projectDTO.getGitlabProjectId());
        project.setYoutrackProjectId(projectDTO.getYoutrackProjectId());
        project.setDescription(projectDTO.getDescription());
        project.setDefaultBranch(projectDTO.getDefaultBranch());

        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject);
    }

    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
        return convertToDTO(project);
    }

    private ProjectDTO convertToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setGitlabProjectId(project.getGitlabProjectId());
        dto.setYoutrackProjectId(project.getYoutrackProjectId());
        dto.setDescription(project.getDescription());
        dto.setDefaultBranch(project.getDefaultBranch());
        return dto;
    }

    public List<ProjectDTO> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream()
                .map(this::convertToDTO)
                .toList();
    }
}
