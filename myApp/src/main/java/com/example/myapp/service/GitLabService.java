/*
package com.example.myapp.service;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitLabService {

    private static final Logger logger = LoggerFactory.getLogger(GitLabService.class);

    private final GitLabApi gitLabApi;

    public GitLabService(@Value("${gitlab.url}") String gitLabUrl,
                         @Value("${gitlab.token}") String gitLabToken) {
        this.gitLabApi = new GitLabApi(gitLabUrl, gitLabToken);
    }

    public void createReleaseBranch(Long projectId, String sourceBranch, String releaseBranch) {
        try {
            gitLabApi.getRepositoryApi().createBranch(projectId, releaseBranch, sourceBranch);
            logger.info("Created release branch '{}' from '{}' in project {}", releaseBranch, sourceBranch, projectId);
        } catch (GitLabApiException e) {
            logger.error("Failed to create release branch '{}' from '{}' in project {}: {}",
                    releaseBranch, sourceBranch, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to create release branch", e);
        }
    }

    public boolean branchExists(Long projectId, String branchName) {
        try {
            boolean exists = gitLabApi.getRepositoryApi().getBranch(projectId, branchName) != null;
            logger.debug("Checked existence of branch '{}' in project {}: {}", branchName, projectId, exists);
            return exists;
        } catch (GitLabApiException e) {
            logger.warn("Error checking if branch '{}' exists in project {}: {}",
                    branchName, projectId, e.getMessage());
            return false;
        }
    }

    public void createMergeRequest(Long projectId, String sourceBranch, String targetBranch, String title) {
        try {
            gitLabApi.getMergeRequestApi().createMergeRequest(
                    projectId,
                    sourceBranch,
                    targetBranch,
                    title,
                    "Автоматически созданный MR для релиза",
                    null
            );
            logger.info("Created merge request from '{}' to '{}' in project {} with title '{}'",
                    sourceBranch, targetBranch, projectId, title);
        } catch (GitLabApiException e) {
            logger.error("Failed to create merge request from '{}' to '{}' in project {}: {}",
                    sourceBranch, targetBranch, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to create merge request", e);
        }
    }
}
*/