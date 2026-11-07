package com.hocchu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssignmentRequest {

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Loại bài tập (contentType) không được để trống")
    private String contentType; // 'QUIZ' hoặc 'WRITING'

    private String contentData; // Lưu cấu trúc câu hỏi JSON hoặc chữ cái mẫu

    private LocalDateTime dueDate;

    private Long classId;
}
