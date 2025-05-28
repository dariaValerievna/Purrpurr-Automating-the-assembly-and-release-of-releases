package com.example.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskDTO {
    private Long id;
    @NotBlank
    private String youtrackId;
    @NotBlank
    private String title;
    @NotBlank
    private String status;
    @NotNull
    private Long releaseId;
    private String author;
    private String developer;
    private String tags;
}
