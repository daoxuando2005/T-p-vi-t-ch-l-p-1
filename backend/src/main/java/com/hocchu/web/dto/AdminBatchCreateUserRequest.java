package com.hocchu.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminBatchCreateUserRequest {
    @NotEmpty(message = "Danh sách tài khoản tạo mới không được trống")
    @Valid
    private List<AdminCreateUserRequest> users;
}
