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
public class PracticeResponse {
    private Long id;
    private String letter;
    private String canvasImageUrl;
    private Float accuracyScore;
    private LocalDateTime practicedAt;
}
