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
@PreAuthorize("hasRole('PARENT')")
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

}
