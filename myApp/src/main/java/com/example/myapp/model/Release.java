package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


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

    @Column(nullable = false)
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

    private String version;
    private String branchName;
}
