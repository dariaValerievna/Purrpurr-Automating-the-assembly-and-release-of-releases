package com.example.myapp.controller;

import com.example.myapp.dto.TaskDTO;
import com.example.myapp.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO taskDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(taskDTO));
    }

    @GetMapping("/release/{releaseId}")
    public ResponseEntity<List<TaskDTO>> getTasksByRelease(@PathVariable Long releaseId) {
        return ResponseEntity.ok(taskService.getTasksByRelease(releaseId));
    }

    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

}
