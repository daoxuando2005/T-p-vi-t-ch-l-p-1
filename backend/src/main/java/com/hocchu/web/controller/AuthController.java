package com.hocchu.web.controller;

import com.hocchu.web.dto.*;
import com.hocchu.web.security.CurrentUser;
import com.hocchu.web.security.UserPrincipal;
import com.hocchu.web.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        ApiResponse response = authService.registerUser(signUpRequest);
        if (!response.getSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JwtAuthenticationResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        List<String> roles = currentUser.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());

        JwtAuthenticationResponse userMe = new JwtAuthenticationResponse(
                null, // Token không cần gửi lại ở endpoint me
                "Bearer",
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getFullName(),
                roles
        );
        return ResponseEntity.ok(userMe);
    }
}
