package com.example.myapp.repository;

import com.example.myapp.model.Release;
import com.example.myapp.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByRelease(Release release);
    Optional<Task> findByYoutrackId(String youtrackId);
}
