package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.Role;
import com.hocchu.web.entity.User;
import com.hocchu.web.repository.RoleRepository;
import com.hocchu.web.repository.UserRepository;
import com.hocchu.web.security.JwtTokenProvider;
import com.hocchu.web.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Transactional
    public ApiResponse registerUser(SignUpRequest signUpRequest) {
        if(signUpRequest.getRole()==null||!Set.of("STUDENT","PARENT","TEACHER").contains(signUpRequest.getRole().toUpperCase(Locale.ROOT)))
            return new ApiResponse(false,"Vai trò đăng ký chưa hợp lệ.");
        AdminService.validatePassword(signUpRequest.getPassword());
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return new ApiResponse(false, "Tên đăng nhập đã tồn tại!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ApiResponse(false, "Email đã được sử dụng!");
        }

        // Demo registration is immediately usable; existing locked accounts stay locked.
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setEmail(signUpRequest.getEmail());
        user.setFullName(signUpRequest.getFullName());
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setEnabled(true);

        // Cấu hình vai trò
        Set<Role> roles = new HashSet<>();
        String requestedRole = signUpRequest.getRole().toUpperCase(Locale.ROOT);
        String roleName = "ROLE_" + requestedRole;
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy vai trò " + roleName));
        roles.add(role);
        for(String permission:AdminService.permissionsFor(roleName,null))
            roles.add(roleRepository.findByName(permission)
                .orElseThrow(()->new IllegalStateException("Thiếu cấu hình quyền hệ thống.")));
        user.setRoles(roles);

        userRepository.save(user);

        return new ApiResponse(true, "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
    }

    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtAuthenticationResponse(
                jwt,
                "Bearer",
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getFullName(),
                roles
        );
    }
}
