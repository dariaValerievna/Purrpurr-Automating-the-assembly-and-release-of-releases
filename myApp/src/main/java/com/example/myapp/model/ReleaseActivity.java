package com.example.myapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "release_activities")
public class ReleaseActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Column(nullable = false)
    private String action;              // "BRANCH_CREATED", "MR_CREATED", etc.

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String description;         // Детали действия

    @Column
    private String performedBy;         // Кто выполнил действие

    @Enumerated(EnumType.STRING)
    private ActivityStatus status;      // SUCCESS, FAILED

    @Column(columnDefinition = "TEXT")
    private String errorMessage;        // Сообщение об ошибке если есть
}
