package com.example.myapp.dto.youtrack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTrackLinkDTO {
    private String id;
    private LinkTypeDTO linkType;
    private YouTrackIssueDTO source;
    private YouTrackIssueDTO target;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LinkTypeDTO {
        private String name;
        private String sourceToTarget;
        private String targetToSource;
    }
}
