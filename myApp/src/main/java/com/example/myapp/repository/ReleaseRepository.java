package com.example.myapp.repository;

import com.example.myapp.model.Release;
import com.example.myapp.model.ReleaseStatus;
import com.example.myapp.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {

    @Query("SELECT r FROM Release r WHERE r.project.id = :projectId")
    List<Release> findByProjectId(@Param("projectId") Long projectId);

    List<Release> findByProject(Project project);

    @Query("SELECT r FROM Release r WHERE r.youtrackReleaseTaskId = :taskId")
    Optional<Release> findByYoutrackReleaseTaskId(@Param("taskId") String taskId);

    @Query("SELECT r FROM Release r WHERE r.version = :version AND r.project.id = :projectId")
    Optional<Release> findByVersionAndProjectId(@Param("version") String version, @Param("projectId") Long projectId);

    @Query("SELECT r FROM Release r WHERE r.releaseBranch = :branchName")
    Optional<Release> findByReleaseBranch(@Param("branchName") String branchName);

    List<Release> findByStatus(ReleaseStatus status);

    @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.status = :status")
    List<Release> findByProjectIdAndStatus(@Param("projectId") Long projectId,
                                           @Param("status") ReleaseStatus status);

    @Query("SELECT r FROM Release r ORDER BY r.createdAt DESC")
    List<Release> findAllOrderByCreatedAtDesc();

    @Query("SELECT r FROM Release r WHERE r.project.id = :projectId ORDER BY r.createdAt DESC")
    List<Release> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") Long projectId);
}
