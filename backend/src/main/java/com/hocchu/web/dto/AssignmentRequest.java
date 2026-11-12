package com.hocchu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssignmentRequest {

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    @jakarta.validation.constraints.Size(max=160,message="Tiêu đề tối đa 160 ký tự")
    private String title;

    @jakarta.validation.constraints.Size(max=2000,message="Hướng dẫn tối đa 2000 ký tự")
    private String description;

    @NotBlank(message = "Loại bài tập (contentType) không được để trống")
    private String contentType; // WRITING, QUIZ, SENTENCE

    @NotBlank(message="Nội dung bài tập không được để trống")
    @jakarta.validation.constraints.Size(max=200000,message="Nội dung bài tập quá dài")
    private String contentData;

    private LocalDateTime dueDate;

    @jakarta.validation.constraints.Positive(message="Lớp học chưa hợp lệ")
    private Long classId;
}
