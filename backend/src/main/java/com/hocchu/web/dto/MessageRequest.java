package com.hocchu.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageRequest {

    private Long receiverId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
}
