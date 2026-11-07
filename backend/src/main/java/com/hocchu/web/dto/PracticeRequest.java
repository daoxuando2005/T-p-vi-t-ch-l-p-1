package com.hocchu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PracticeRequest {

    @NotBlank(message = "Chữ cái luyện viết không được để trống")
    private String letter;

    @NotBlank(message = "Dữ liệu hình ảnh canvas không được để trống")
    private String canvasImageUrl; // Base64

    private Float accuracyScore; // Điểm số tính toán thực tế từ frontend
}
