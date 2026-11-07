package com.hocchu.web.controller;

import com.hocchu.web.dto.*;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import com.hocchu.web.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER') or hasAuthority('PERM_ASSIGN_HOMEWORK') or hasAuthority('PERM_MANAGE_CLASSES')")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @PostMapping("/class")
    public ResponseEntity<ClassResponse> createClass(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam String className,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(teacherService.createClass(currentUser.getId(), className, description));
    }

    @GetMapping("/class")
    public ResponseEntity<List<ClassResponse>> getMyClasses(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(teacherService.getMyClasses(currentUser.getId()));
    }

    @GetMapping("/class/{classId}/students")
    public ResponseEntity<List<UserProfileResponse>> getClassStudents(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId) {
        return ResponseEntity.ok(teacherService.getClassStudents(currentUser.getId(), classId));
    }

    @PostMapping("/class/{classId}/students")
    public ResponseEntity<ApiResponse> addStudentToClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId,
            @RequestParam String studentUsername) {
        ApiResponse response = teacherService.addStudentToClass(currentUser.getId(), classId, studentUsername);
        if (!response.getSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/class/{classId}/students/{studentId}")
    public ResponseEntity<ApiResponse> removeStudentFromClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(teacherService.removeStudentFromClass(currentUser.getId(), classId, studentId));
    }

    @PostMapping("/assignments")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(teacherService.createAssignment(currentUser.getId(), request));
    }

    @GetMapping("/class/{classId}/assignments")
    public ResponseEntity<List<AssignmentResponse>> getClassAssignments(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId) {
        return ResponseEntity.ok(teacherService.getClassAssignments(currentUser.getId(), classId));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<List<SubmissionResponse>> getAssignmentSubmissions(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(teacherService.getAssignmentSubmissions(currentUser.getId(), assignmentId));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<ApiResponse> gradeSubmission(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(teacherService.gradeSubmission(currentUser.getId(), submissionId, request));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId,
            @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(teacherService.updateAssignment(currentUser.getId(), assignmentId, request));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<ApiResponse> deleteAssignment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(teacherService.deleteAssignment(currentUser.getId(), assignmentId));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getTeacherStats(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(teacherService.getTeacherStats(currentUser.getId()));
    }

    @PutMapping("/class/{classId}")
    public ResponseEntity<ClassResponse> updateClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId,
            @RequestParam String className,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(teacherService.updateClass(currentUser.getId(), classId, className, description));
    }

    @DeleteMapping("/class/{classId}")
    public ResponseEntity<ApiResponse> deleteClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId) {
        return ResponseEntity.ok(teacherService.deleteClass(currentUser.getId(), classId));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse> uploadQuizImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "File ảnh rỗng!"));
            }
            java.nio.file.Path targetDir = java.nio.file.Paths.get("d:/study/doanchuyennganh/code/code/frontend/public/example_images");
            if (!java.nio.file.Files.exists(targetDir)) {
                java.nio.file.Files.createDirectories(targetDir);
            }
            java.nio.file.Path targetFile = targetDir.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(new ApiResponse(true, "/example_images/" + filename));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể lưu tệp ảnh: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-audio")
    public ResponseEntity<ApiResponse> uploadQuizAudio(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("filename") String filename) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "File âm thanh rỗng!"));
            }
            java.nio.file.Path targetDir = java.nio.file.Paths.get("d:/study/doanchuyennganh/code/code/frontend/public/audio");
            if (!java.nio.file.Files.exists(targetDir)) {
                java.nio.file.Files.createDirectories(targetDir);
            }
            java.nio.file.Path targetFile = targetDir.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(new ApiResponse(true, "/audio/" + filename));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Không thể lưu tệp âm thanh: " + e.getMessage()));
        }
    }
}

