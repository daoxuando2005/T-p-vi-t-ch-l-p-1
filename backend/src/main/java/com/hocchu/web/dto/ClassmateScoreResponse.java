package com.hocchu.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassmateScoreResponse {
    private Long studentId;
    private String fullName;
    private String username;
    private Float totalScore;
    private Integer totalAssignmentsCompleted;
}