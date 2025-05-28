package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String youtrackId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "release_id")
    private Release release;

    @Column
    private String author;

    @Column
    private String developer;

    @Column
    private String tags;
}
