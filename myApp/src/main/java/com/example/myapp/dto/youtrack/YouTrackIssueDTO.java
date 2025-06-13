package com.example.myapp.dto.youtrack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTrackIssueDTO {

    private String id;
    private String summary;
    private String description;

    @JsonProperty("customFields")
    private List<CustomFieldDTO> customFields;

    private UserDTO reporter;
    private UserDTO assignee;
    private List<TagDTO> tags;

    // Вспомогательные методы для получения статуса
    public String getStatus() {
        return getCustomFieldValue("State");
    }

    public String getPriority() {
        return getCustomFieldValue("Priority");
    }

    public String getType() {
        return getCustomFieldValue("Type");
    }

    /**
     * Получение списка названий тегов как строк
     */
    public List<String> getTagNames() {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(TagDTO::getName)
                .collect(Collectors.toList());
    }

    /**
     * Получение описания из customFields если оно там хранится
     */
    public String getDescription() {
        if (description != null) {
            return description;
        }
        // Если description хранится в customFields
        return getCustomFieldValue("description");
    }

    private String getCustomFieldValue(String fieldName) {
        if (customFields == null) return null;

        return customFields.stream()
                .filter(field -> fieldName.equals(field.getName()))
                .map(CustomFieldDTO::getValue)
                .filter(value -> value != null)
                .map(Object::toString)
                .findFirst()
                .orElse(null);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomFieldDTO {
        private String name;
        private Object value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserDTO {
        private String name;
        private String login;
        private String email;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagDTO {
        private String name;
        private String color;
    }
}
