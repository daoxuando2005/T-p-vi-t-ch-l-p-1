package com.hocchu.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private List<String> roles;
    private Boolean enabled;
    private String childInfo; // Ví dụ: "Phụ huynh bé Nguyễn Văn A (Lớp 1A)"
    private String className; // Thêm trường lớp học

    public UserProfileResponse(Long id, String username, String email, String fullName, String phoneNumber, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.roles = roles;
    }
}
