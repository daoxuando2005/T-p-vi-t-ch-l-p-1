package com.hocchu.web.controller;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.AlphabetLesson;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import com.hocchu.web.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/alphabet")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<List<AlphabetLesson>> getAlphabetLessons() {
        return ResponseEntity.ok(studentService.getAlphabetLessons());
    }

    @PostMapping("/practice")
    public ResponseEntity<PracticeResponse> saveWritingPractice(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody PracticeRequest request) {
        return ResponseEntity.ok(studentService.saveWritingPractice(currentUser.getId(), request));
    }

    @GetMapping("/practice/history")
    public ResponseEntity<List<PracticeResponse>> getPracticeHistory(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(studentService.getPracticeHistory(currentUser.getId()));
    }

    @PostMapping("/class/join")
    public ResponseEntity<ApiResponse> joinClass(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam String inviteCode) {
        ApiResponse response = studentService.joinClass(currentUser.getId(), inviteCode);
        if (!response.getSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/class/leave/{classId}")
    public ResponseEntity<ApiResponse> leaveClass(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long classId) {
        return ResponseEntity.ok(studentService.leaveClass(currentUser.getId(), classId));
    }

    @GetMapping("/class/joined")
    public ResponseEntity<List<ClassResponse>> getJoinedClasses(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(studentService.getJoinedClasses(currentUser.getId()));
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(studentService.getAssignments(currentUser.getId()));
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public ResponseEntity<ApiResponse> submitAssignment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmissionRequest request) {
        ApiResponse response = studentService.submitAssignment(currentUser.getId(), assignmentId, request);
        if (!response.getSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }


    @GetMapping("/link-code")
    public ResponseEntity<Map<String, String>> getLinkCode(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(studentService.getParentLinkCode(currentUser.getId()));
    }
}
