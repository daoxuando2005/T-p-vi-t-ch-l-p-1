package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.Role;
import com.hocchu.web.entity.User;
import com.hocchu.web.repository.RoleRepository;
import com.hocchu.web.repository.UserRepository;
import com.hocchu.web.security.JwtTokenProvider;
import com.hocchu.web.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
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

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Transactional
    public ApiResponse registerUser(SignUpRequest signUpRequest) {
        if(signUpRequest.getRole()==null||!Set.of("STUDENT","PARENT","TEACHER").contains(signUpRequest.getRole().toUpperCase(Locale.ROOT)))
            return new ApiResponse(false,"Vai trò đăng ký chưa hợp lệ.");
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return new ApiResponse(false, "Tên đăng nhập đã tồn tại!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ApiResponse(false, "Email đã được sử dụng!");
        }

        // Tạo tài khoản mới chưa kích hoạt
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setEmail(signUpRequest.getEmail());
        user.setFullName(signUpRequest.getFullName());
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setEnabled(false); // Chưa kích hoạt, cần OTP

        // Cấu hình vai trò
        Set<Role> roles = new HashSet<>();
        String requestedRole = signUpRequest.getRole().toUpperCase();
        String roleName = "ROLE_" + requestedRole;
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy vai trò " + roleName));
        roles.add(role);
        user.setRoles(roles);

        // Sinh mã OTP 6 số ngẫu nhiên
        java.security.SecureRandom random = new java.security.SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        userRepository.save(user);

        // In mã OTP ra Console thay thế cho việc gửi SMS/Email
        log.info("==================================================");
        log.info("MÃ OTP KÍCH HOẠT TÀI KHOẢN CHO USER '{}': {}", user.getUsername(), otp);
        log.info("VUI LÒNG DÙNG MÃ NÀY ĐỂ KÍCH HOẠT TRÊN GIAO DIỆN");
        log.info("==================================================");

        return new ApiResponse(true, "Đăng ký thành công! Vui lòng kiểm tra mã OTP kích hoạt tài khoản.");
    }

    @Transactional
    public ApiResponse verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        User user = userRepository.findByUsernameOrEmail(verifyOtpRequest.getEmailOrUsername(), verifyOtpRequest.getEmailOrUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với thông tin cung cấp"));

        if (user.getEnabled()) {
            return new ApiResponse(false, "Tài khoản này đã được kích hoạt trước đó.");
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(verifyOtpRequest.getOtpCode())) {
            return new ApiResponse(false, "Mã OTP không chính xác.");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return new ApiResponse(false, "Mã OTP đã hết hạn.");
        }

        // Kích hoạt tài khoản
        user.setEnabled(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return new ApiResponse(true, "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.");
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
