package com.hocchu.web.controller;

import com.hocchu.web.dto.*;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import com.hocchu.web.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parent")
@PreAuthorize("hasRole('PARENT') or hasAuthority('PERM_ASSIGN_HOMEWORK') or hasAuthority('PERM_PARENT_TRACKING')")
public class ParentController {

    @Autowired
    private ParentService parentService;

    @PostMapping("/link")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse> linkStudent(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam String linkCode) {
        ApiResponse response = parentService.linkStudent(currentUser.getId(), linkCode);
        if (!response.getSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students")
    public ResponseEntity<List<ParentLinkResponse>> getMyStudents(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(parentService.getMyStudents(currentUser.getId()));
    }

    @GetMapping("/students/{studentId}/classmates")
    public ResponseEntity<List<com.hocchu.web.dto.ClassmateScoreResponse>> getClassmatesScores(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(parentService.getClassmatesScores(currentUser.getId(), studentId));
    }

    @GetMapping("/students/{studentId}/progress")
    public ResponseEntity<StudentProgressResponse> getStudentProgress(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(parentService.getStudentProgress(currentUser.getId(), studentId));
    }

    @GetMapping("/students/{studentId}/assignments")
    public ResponseEntity<List<AssignmentResponse>> getStudentAssignments(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(parentService.getStudentAssignments(currentUser.getId(), studentId));
    }

    @PostMapping("/students/{studentId}/assignments/{assignmentId}/remind")
    public ResponseEntity<ApiResponse> remindStudentAssignment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(parentService.remindStudentAssignment(currentUser.getId(), studentId, assignmentId));
    }

    // Giao bÄ‚Â i tĂ¡ÂºÂ­p cho con (Cho phÄ‚Â©p PhĂ¡Â»Â¥ huynh Ă„â€˜Ă†Â°Ă¡Â»Â£c Admin cĂ¡ÂºÂ¥p quyĂ¡Â»Ân PERM_ASSIGN_HOMEWORK thĂ¡Â»Â±c hiĂ¡Â»â€¡n)
    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN') or hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<ApiResponse> assignHomeworkToChild(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam Long studentId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String dueDateStr, @RequestParam(required = false) String letters) {
        return ResponseEntity.ok(parentService.assignHomeworkToChild(currentUser.getId(), studentId, title, description, dueDateStr, letters));
    }

    @PutMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN') or hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<ApiResponse> updateAssignment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId,
            @RequestParam String title,
            @RequestParam(required = false) String dueDateStr,
            @RequestParam(required = false) String letters) {
        return ResponseEntity.ok(parentService.updateAssignment(currentUser.getId(), assignmentId, title, dueDateStr, letters));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN') or hasAuthority('PERM_ASSIGN_HOMEWORK')")
    public ResponseEntity<ApiResponse> deleteAssignment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(parentService.deleteAssignment(currentUser.getId(), assignmentId));
    }
}
