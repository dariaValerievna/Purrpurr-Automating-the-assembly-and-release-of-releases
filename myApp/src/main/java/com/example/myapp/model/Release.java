package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "releases")
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = true)
    private String releaseBranch;

    @Column(nullable = false)
    private String sourceBranch;

    @Column(nullable = false)
    private String youtrackReleaseTaskId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private ReleaseStatus status;

    @Column(nullable = false)
    private String version;

    private String branchName;

    // 🆕 Новые поля для YouTrack интеграции
    @Column
    private String name;                    // Название релиза

    @Column(length = 1000)
    private String description;             // Описание релиза

    // GitLab интеграция
    private String gitlabReleaseUrl;
    private String gitlabMergeRequestUrl;
    private String gitlabTagName;
    private String releaseNotes;
    private String createdBy;


    // 🆕 Связь с YouTrack задачами
    @OneToMany(mappedBy = "release", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReleaseIssue> releaseIssues;

    // 🆕 Enum для статусов (для совместимости с YouTrack интеграцией)
    public enum Status {
        PLANNING,
        IN_PROGRESS,
        TESTING,
        READY_FOR_RELEASE,
        RELEASED,
        CANCELLED
    }

    // 🆕 Методы для работы со статусами (для совместимости)
    public Status getStatusAsEnum() {
        if (status == null) return null;

        switch (status) {
            case CREATED: return Status.PLANNING;
            case IN_PROGRESS: return Status.IN_PROGRESS;
            case COMPLETED: return Status.RELEASED;
            case FAILED: return Status.CANCELLED;
            default: return Status.PLANNING;
        }
    }

    public void setStatusFromEnum(Status enumStatus) {
        if (enumStatus == null) return;

        switch (enumStatus) {
            case PLANNING: this.status = ReleaseStatus.CREATED; break;
            case IN_PROGRESS: this.status = ReleaseStatus.IN_PROGRESS; break;
            case RELEASED: this.status = ReleaseStatus.COMPLETED; break;
            case CANCELLED: this.status = ReleaseStatus.FAILED; break;
            default: this.status = ReleaseStatus.CREATED; break;
        }
    }
}
