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
public class FeedbackResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userFullName;
    private String title;
    private String content;
    private String status; // 'OPEN' hoặc 'RESOLVED'
    private LocalDateTime createdAt;
}
