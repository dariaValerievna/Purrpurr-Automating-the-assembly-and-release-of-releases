package com.example.myapp.repository;

import com.example.myapp.model.ReleaseIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseIssueRepository extends JpaRepository<ReleaseIssue, Long> {

    List<ReleaseIssue> findByReleaseId(Long releaseId);

    Optional<ReleaseIssue> findByReleaseIdAndIssueId(Long releaseId, String issueId);

    List<ReleaseIssue> findByIssueId(String issueId);

    List<ReleaseIssue> findByStatus(String status);

    List<ReleaseIssue> findByAssignee(String assignee);

    @Query("SELECT ri FROM ReleaseIssue ri WHERE ri.release.id = :releaseId AND ri.status IN :statuses")
    List<ReleaseIssue> findByReleaseIdAndStatusIn(@Param("releaseId") Long releaseId, @Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(ri) FROM ReleaseIssue ri WHERE ri.release.id = :releaseId")
    long countByReleaseId(@Param("releaseId") Long releaseId);

    @Query("SELECT COUNT(ri) FROM ReleaseIssue ri WHERE ri.release.id = :releaseId AND ri.status = :status")
    long countByReleaseIdAndStatus(@Param("releaseId") Long releaseId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM ReleaseIssue ri WHERE ri.release.id = :releaseId")
    void deleteByReleaseId(@Param("releaseId") Long releaseId);

    boolean existsByReleaseIdAndIssueId(Long releaseId, String issueId);
}
