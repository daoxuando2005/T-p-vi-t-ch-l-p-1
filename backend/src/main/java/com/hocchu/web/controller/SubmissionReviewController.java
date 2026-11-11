package com.hocchu.web.controller;

import com.hocchu.web.security.*;
import com.hocchu.web.service.SubmissionReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SubmissionReviewController {
    @Autowired private SubmissionReviewService reviews;
    @GetMapping("/student/assignments/{id}/submission") @PreAuthorize("hasRole('STUDENT')")
    public SubmissionReviewService.Detail student(@CurrentUser UserPrincipal user,@PathVariable Long id) {return reviews.student(user.getId(),id);}
    @GetMapping("/parent/students/{studentId}/submissions/{id}") @PreAuthorize("hasRole('PARENT')")
    public SubmissionReviewService.Detail parent(@CurrentUser UserPrincipal user,@PathVariable Long studentId,@PathVariable Long id) {return reviews.parent(user.getId(),studentId,id);}
}
