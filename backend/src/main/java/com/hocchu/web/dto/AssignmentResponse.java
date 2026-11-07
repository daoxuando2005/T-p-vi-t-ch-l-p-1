package com.hocchu.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private String contentType;
    private String contentData;
    private LocalDateTime dueDate;
    private Long classId;
    private String className;
    private LocalDateTime createdAt;
    private Boolean isSubmitted;
    private Float score;
    private String feedback;
    private String submissionData;
    private String assignedBy;
}