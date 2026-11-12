package com.hocchu.web.controller;
import com.hocchu.web.dto.ApiResponse;
import com.hocchu.web.security.*;
import com.hocchu.web.service.TeacherAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/assistant")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAssistantController {
    private final TeacherAssistantService service;
    public TeacherAssistantController(TeacherAssistantService service){this.service=service;}
    @GetMapping("/status") public Map<String,Object> status(){return service.status();}
    @PostMapping("/draft") public TeacherAssistantService.Reply draft(@CurrentUser UserPrincipal user,@RequestBody TeacherAssistantService.Request request){return service.generate(user.getId(),request);}
    @ExceptionHandler(TeacherAssistantService.Failure.class)
    public ResponseEntity<ApiResponse> failure(TeacherAssistantService.Failure e){return ResponseEntity.status(e.status).body(new ApiResponse(false,e.getMessage()));}
}
