package com.hocchu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    @NotBlank(message = "Email hoặc Username không được để trống")
    private String emailOrUsername;

    @NotBlank(message = "Mã OTP không được để trống")
    private String otpCode;
}
