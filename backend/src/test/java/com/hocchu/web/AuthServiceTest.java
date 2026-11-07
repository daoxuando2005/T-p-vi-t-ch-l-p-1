package com.hocchu.web;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.Role;
import com.hocchu.web.entity.User;
import com.hocchu.web.repository.RoleRepository;
import com.hocchu.web.repository.UserRepository;
import com.hocchu.web.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setup() {
        // Cấu hình @Value mock cho test
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 5);
    }

    @Test
    public void testRegisterUser_Success() {
        SignUpRequest request = new SignUpRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@hocchu.com");
        request.setFullName("Test User");
        request.setRole("STUDENT");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@hocchu.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        Role studentRole = Role.builder().id(1L).name("ROLE_STUDENT").build();
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));

        ApiResponse response = authService.registerUser(request);

        assertTrue(response.getSuccess());
        assertEquals("Đăng ký thành công! Vui lòng kiểm tra mã OTP kích hoạt tài khoản.", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegisterUser_DuplicateUsername() {
        SignUpRequest request = new SignUpRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("test@hocchu.com");
        request.setFullName("Test User");
        request.setRole("STUDENT");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        ApiResponse response = authService.registerUser(request);

        assertFalse(response.getSuccess());
        assertEquals("Tên đăng nhập đã tồn tại!", response.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testVerifyOtp_Success() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmailOrUsername("testuser");
        request.setOtpCode("123456");

        User user = User.builder()
                .username("testuser")
                .email("test@hocchu.com")
                .enabled(false)
                .otpCode("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(user));

        ApiResponse response = authService.verifyOtp(request);

        assertTrue(response.getSuccess());
        assertTrue(user.getEnabled());
        assertNull(user.getOtpCode());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testVerifyOtp_WrongOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmailOrUsername("testuser");
        request.setOtpCode("wrongotp");

        User user = User.builder()
                .username("testuser")
                .email("test@hocchu.com")
                .enabled(false)
                .otpCode("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(user));

        ApiResponse response = authService.verifyOtp(request);

        assertFalse(response.getSuccess());
        assertEquals("Mã OTP không chính xác.", response.getMessage());
        assertFalse(user.getEnabled());
        verify(userRepository, never()).save(user);
    }
}
