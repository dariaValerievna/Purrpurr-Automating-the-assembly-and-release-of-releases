package com.example.myapp.repository;

import com.example.myapp.model.ReleaseActivity;
import com.example.myapp.model.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReleaseActivityRepository extends JpaRepository<ReleaseActivity, Long> {
    List<ReleaseActivity> findByReleaseOrderByTimestampDesc(Release release);
    List<ReleaseActivity> findByReleaseIdOrderByTimestampDesc(Long releaseId);
}
