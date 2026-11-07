package com.hocchu.web.controller;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.AlphabetLesson;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import com.hocchu.web.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // Lấy toàn bộ người dùng
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // Khóa/Mở khóa người dùng
    @PutMapping("/users/{userId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> toggleUserStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.toggleUserStatus(userId));
    }

    // Đổi vai trò / phân quyền người dùng
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        return ResponseEntity.ok(adminService.changeUserRole(userId, role));
    }

    // Phân quyền chi tiết (thêm/khóa quyền cụ thể cho từng tài khoản)
    @PutMapping("/users/{userId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateUserPermissions(
            @PathVariable Long userId,
            @RequestBody List<String> permissions) {
        return ResponseEntity.ok(adminService.updateUserPermissions(userId, permissions));
    }

    // Xóa người dùng
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.deleteUser(userId));
    }

    @PutMapping("/users/{userId}/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateUserInfo(
            @PathVariable Long userId,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(adminService.updateUserInfo(userId, payload));
    }

    // Quản lý học liệu chữ cái
    @PostMapping("/alphabet")
    @PreAuthorize("hasAnyRole('ADMIN') or (hasAuthority('PERM_EDIT_ALPHABET') and #lesson.id != null) or (hasAuthority('PERM_ADD_ALPHABET') and #lesson.id == null)")
    public ResponseEntity<AlphabetLesson> saveAlphabetLesson(@Valid @RequestBody AlphabetLesson lesson) {
        return ResponseEntity.ok(adminService.saveAlphabetLesson(lesson));
    }

    @PostMapping("/alphabet/upload-image")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('PERM_EDIT_ALPHABET')")
    public ResponseEntity<ApiResponse> uploadAlphabetImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        return ResponseEntity.ok(adminService.uploadAlphabetImage(file, filename));
    }

    @PostMapping("/alphabet/upload-audio")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('PERM_EDIT_ALPHABET')")
    public ResponseEntity<ApiResponse> uploadAlphabetAudio(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        return ResponseEntity.ok(adminService.uploadAlphabetAudio(file, filename));
    }

    @PostMapping("/alphabet/upload-example-image")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('PERM_EDIT_ALPHABET')")
    public ResponseEntity<ApiResponse> uploadAlphabetExampleImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        return ResponseEntity.ok(adminService.uploadAlphabetExampleImage(file, filename));
    }

    @DeleteMapping("/alphabet/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or hasAuthority('PERM_ADD_ALPHABET')")
    public ResponseEntity<Void> deleteAlphabetLesson(@PathVariable Long id) {
        adminService.deleteAlphabetLesson(id);
        return ResponseEntity.noContent().build();
    }

    // Tạo tài khoản
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.ok(adminService.createUser(request));
    }

    // Thống kê hệ thống
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }
}
