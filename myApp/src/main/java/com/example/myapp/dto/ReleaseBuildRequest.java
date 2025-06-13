package com.example.myapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseBuildRequest {
    private Long projectId;
    private String releaseTaskId; // YouTrack ID релизной задачи
    private String sourceBranch; // Ветка, из которой создается релиз
    private boolean skipPipeline; // Пропустить pipeline
    private String createdBy; // Кто создает релиз
}