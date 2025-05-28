package com.example.myapp.repository;

import com.example.myapp.model.Project;
import com.example.myapp.model.Release;
import com.example.myapp.model.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {
    List<Release> findByProject(Project project);
    List<Release> findByStatus(ReleaseStatus status);
}
