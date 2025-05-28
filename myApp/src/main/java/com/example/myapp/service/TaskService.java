package com.example.myapp.service;

import com.example.myapp.dto.TaskDTO;
import com.example.myapp.model.Release;
import com.example.myapp.model.Task;
import com.example.myapp.repository.ReleaseRepository;
import com.example.myapp.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {
    private final TaskRepository taskRepository;
    private final ReleaseRepository releaseRepository;

    public TaskService(TaskRepository taskRepository, ReleaseRepository releaseRepository) {
        this.taskRepository = taskRepository;
        this.releaseRepository = releaseRepository;
    }

    public TaskDTO createTask(TaskDTO taskDTO) {
        Release release = releaseRepository.findById(taskDTO.getReleaseId())
                .orElseThrow(() -> new EntityNotFoundException("Release not found"));

        Task task = new Task();
        task.setYoutrackId(taskDTO.getYoutrackId());
        task.setTitle(taskDTO.getTitle());
        task.setStatus(taskDTO.getStatus());
        task.setRelease(release);
        task.setAuthor(taskDTO.getAuthor());
        task.setDeveloper(taskDTO.getDeveloper());
        task.setTags(taskDTO.getTags());

        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    public List<TaskDTO> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(this::convertToDTO)
                .toList();
    }


    public List<TaskDTO> getTasksByRelease(Long releaseId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new EntityNotFoundException("Release not found"));
        return taskRepository.findByRelease(release).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setYoutrackId(task.getYoutrackId());
        dto.setTitle(task.getTitle());
        dto.setStatus(task.getStatus());
        dto.setReleaseId(task.getRelease().getId());
        dto.setAuthor(task.getAuthor());
        dto.setDeveloper(task.getDeveloper());
        dto.setTags(task.getTags());
        return dto;
    }
}
