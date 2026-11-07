package com.hocchu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionRequest {

    @NotBlank(message = "Dữ liệu nộp bài không được để trống")
    private String submissionData; // Chuỗi câu trả lời trắc nghiệm JSON hoặc base64 ảnh nét vẽ
}
