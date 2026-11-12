package com.hocchu.web.controller;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.AlphabetLesson;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import com.hocchu.web.service.AdminService;
import com.hocchu.web.service.AdminOperationsService;
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
    @Autowired private AdminOperationsService operations;

    // Lấy toàn bộ người dùng
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // Khóa/Mở khóa người dùng
    @PutMapping("/users/{userId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> toggleUserStatus(@CurrentUser UserPrincipal actor,@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.toggleUserStatus(actor.getId(),userId));
    }

    // Đổi vai trò / phân quyền người dùng
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> changeUserRole(
            @CurrentUser UserPrincipal actor,
            @PathVariable Long userId,
            @RequestParam String role) {
        return ResponseEntity.ok(adminService.changeUserRole(actor.getId(),userId, role));
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
    public ResponseEntity<ApiResponse> deleteUser(@CurrentUser UserPrincipal actor,@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.deleteUser(actor.getId(),userId));
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
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and hasAuthority('PERM_EDIT_ALPHABET') and #lesson.id != null)")
    public ResponseEntity<AlphabetLesson> saveAlphabetLesson(@Valid @RequestBody AlphabetLesson lesson) {
        return ResponseEntity.ok(adminService.saveAlphabetLesson(lesson));
    }

    @PostMapping("/alphabet/upload-image")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and hasAuthority('PERM_EDIT_ALPHABET'))")
    public ResponseEntity<ApiResponse> uploadAlphabetImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        return ResponseEntity.ok(adminService.uploadAlphabetImage(file, filename));
    }

    @PostMapping("/alphabet/upload-audio")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and hasAuthority('PERM_EDIT_ALPHABET'))")
    public ResponseEntity<ApiResponse> uploadAlphabetAudio(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        return ResponseEntity.ok(adminService.uploadAlphabetAudio(file, filename));
    }

    @PostMapping("/alphabet/upload-example-image")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and hasAuthority('PERM_EDIT_ALPHABET'))")
    public ResponseEntity<ApiResponse> uploadAlphabetExampleImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        return ResponseEntity.ok(adminService.uploadAlphabetExampleImage(file, filename));
    }

    @DeleteMapping("/alphabet/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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

    public record PasswordRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=6,max=72) String password) {}
    @PostMapping("/users/{id}/password") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse resetPassword(@PathVariable Long id,@Valid @RequestBody PasswordRequest body){return adminService.resetPassword(id,body.password());}
    @GetMapping("/classes") @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String,Object>> classes(){return operations.classes();}
    @GetMapping("/classes/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> classDetail(@PathVariable long id){return operations.classDetail(id);}
    @GetMapping("/family-links") @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String,Object>> familyLinks(){return operations.familyLinks();}
    @DeleteMapping("/family-links/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse unlink(@PathVariable long id){return operations.unlink(id);}
    @GetMapping("/settings") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> settings(){return operations.settings();}
    @GetMapping("/learning-content") @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String,Object>> learningContent(){return operations.learningContent();}
    @PutMapping("/learning-content/{letter}") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse curriculum(@PathVariable String letter,@RequestBody AdminOperationsService.Curriculum body){return operations.saveCurriculum(letter,body);}
}
