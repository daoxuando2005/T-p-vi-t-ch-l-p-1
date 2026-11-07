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
public class ParentLinkResponse {
    private Long id;
    private Long parentId;
    private String parentName;
    private Long studentId;
    private String studentName;
    private String studentUsername;
    private String linkCode;
    private String status; // 'PENDING' hoặc 'APPROVED'
    private LocalDateTime linkedAt;
}
