package com.hocchu.web.controller;

import com.hocchu.web.dto.ApiResponse;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import com.hocchu.web.service.LearningTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LearningTrackingController {
    @Autowired private LearningTrackingService tracking;
    @PostMapping("/student/learning/sync") @PreAuthorize("hasRole('STUDENT')")
    public Map<String,Object> sync(@CurrentUser UserPrincipal user,@RequestBody LearningTrackingService.Sync body) {return tracking.sync(user.getId(),body);}
    @GetMapping("/parent/students/{id}/dashboard") @PreAuthorize("hasRole('PARENT')")
    public Map<String,Object> dashboard(@CurrentUser UserPrincipal user,@PathVariable Long id,@RequestParam(required=false) String date) {
        return tracking.dashboard(user.getId(),id,date==null?LocalDate.now(LearningTrackingService.SCHOOL_ZONE):LocalDate.parse(date));
    }
    @ExceptionHandler({IllegalArgumentException.class,java.time.format.DateTimeParseException.class})
    public ResponseEntity<ApiResponse> invalid(RuntimeException e) {return ResponseEntity.badRequest().body(new ApiResponse(false,"Thông tin hoặc ngày giờ chưa hợp lệ. Hãy kiểm tra và thử lại."));}
}
