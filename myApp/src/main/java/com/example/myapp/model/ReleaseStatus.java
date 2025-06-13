package com.example.myapp.model;

public enum ReleaseStatus {
    CREATED,                    // Релиз создан в БД
    BRANCH_CREATED,            // Ветка создана в GitLab
    MERGE_REQUEST_CREATED,     // MR создан в GitLab
    MERGE_REQUEST_MERGED,      // MR смержен
    GITLAB_RELEASE_CREATED,    // Релиз создан в GitLab
    COMPLETED,                 // Релиз полностью завершен
    FAILED,                    // Ошибка в процессе
    IN_PROGRESS,
    CANCELLED                  // Релиз отменен
}
