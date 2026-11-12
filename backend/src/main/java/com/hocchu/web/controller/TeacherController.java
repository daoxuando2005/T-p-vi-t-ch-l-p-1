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
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @PostMapping("/class")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_MANAGE_CLASSES')")
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
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_MANAGE_CLASSES')")
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
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_MANAGE_CLASSES')")
    public ResponseEntity<ApiResponse> removeStudentFromClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(teacherService.removeStudentFromClass(currentUser.getId(), classId, studentId));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(teacherService.createAssignment(currentUser.getId(), request));
    }

    @GetMapping("/class/{classId}/assignments")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<List<AssignmentResponse>> getClassAssignments(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId) {
        return ResponseEntity.ok(teacherService.getClassAssignments(currentUser.getId(), classId));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<List<SubmissionResponse>> getAssignmentSubmissions(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(teacherService.getAssignmentSubmissions(currentUser.getId(), assignmentId));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<ApiResponse> gradeSubmission(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(teacherService.gradeSubmission(currentUser.getId(), submissionId, request));
    }

    @PutMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(teacherService.updateAssignment(currentUser.getId(), assignmentId, request));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
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
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_MANAGE_CLASSES')")
    public ResponseEntity<ClassResponse> updateClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId,
            @RequestParam String className,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(teacherService.updateClass(currentUser.getId(), classId, className, description));
    }

    @DeleteMapping("/class/{classId}")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_MANAGE_CLASSES')")
    public ResponseEntity<ApiResponse> deleteClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId) {
        return ResponseEntity.ok(teacherService.deleteClass(currentUser.getId(), classId));
    }

    @Autowired private com.hocchu.web.service.MediaStorageService media;

    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<ApiResponse> uploadQuizImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,@RequestParam("filename") String filename) {
        var result=media.store(file,"example_images",filename);
        return result.getSuccess()?ResponseEntity.ok(result):ResponseEntity.badRequest().body(result);
    }
    @PostMapping("/upload-audio")
    @PreAuthorize("hasRole('TEACHER') and hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<ApiResponse> uploadQuizAudio(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,@RequestParam("filename") String filename) {
        var result=media.store(file,"audio",filename);
        return result.getSuccess()?ResponseEntity.ok(result):ResponseEntity.badRequest().body(result);
    }
}
