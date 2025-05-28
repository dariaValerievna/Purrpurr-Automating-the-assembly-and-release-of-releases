package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String gitlabProjectId;

    @Column(nullable = false)
    private String youtrackProjectId;

    @Column
    private String description;

    @Column(nullable = false)
    private String defaultBranch;
}
