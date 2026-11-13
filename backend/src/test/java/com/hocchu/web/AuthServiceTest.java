package com.hocchu.web;
import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import com.hocchu.web.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;
    SignUpRequest request(String role) {
        var value=new SignUpRequest();value.setUsername(" testuser ");value.setEmail(" test@example.com ");
        value.setFullName(" Học sinh ");value.setPassword(" secret ");value.setRole(role);return value;
    }
    @Test void publicRegistrationCannotCreateAnAdministrator() {
        assertFalse(authService.registerUser(request("ADMIN")).getSuccess());verifyNoInteractions(userRepository,roleRepository,passwordEncoder);
    }
    @Test void studentCanSignInImmediatelyWithoutOtpAndPasswordIsNeverTrimmed() {
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(Role.builder().name("ROLE_STUDENT").build()));
        when(passwordEncoder.encode(" secret ")).thenReturn("encoded");
        assertTrue(authService.registerUser(request("STUDENT")).getSuccess());
        var saved=ArgumentCaptor.forClass(User.class);verify(userRepository).save(saved.capture());var user=saved.getValue();
        assertTrue(user.getEnabled());assertNull(user.getOtpCode());assertNull(user.getOtpExpiry());assertEquals("encoded",user.getPassword());
        assertEquals("testuser",user.getUsername());assertEquals("test@example.com",user.getEmail());assertEquals("Học sinh",user.getFullName());
        assertEquals(Set.of("ROLE_STUDENT"),user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void newTeacherHasWorkingDefaultPermissionsAndParentHasTrackingPermission() {
        when(roleRepository.findByName(anyString())).thenAnswer(call->Optional.of(Role.builder().name(call.getArgument(0)).build()));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        authService.registerUser(request("TEACHER"));authService.registerUser(request("PARENT"));
        var saved=ArgumentCaptor.forClass(User.class);verify(userRepository,times(2)).save(saved.capture());
        assertEquals(Set.of("ROLE_TEACHER","PERM_ASSIGN_HOMEWORK","PERM_MANAGE_CLASSES","PERM_VIEW_ALPHABET","PERM_EDIT_ALPHABET"),
            saved.getAllValues().get(0).getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of("ROLE_PARENT","PERM_PARENT_TRACKING"),saved.getAllValues().get(1).getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void existingAccountIsNeverActivatedOrOverwritten() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        assertFalse(authService.registerUser(request("STUDENT")).getSuccess());verify(userRepository,never()).save(any());verifyNoInteractions(passwordEncoder);
    }
    @Test void bcryptByteLimitPreventsSilentPasswordTruncation() {
        var value=request("STUDENT");value.setPassword("ế".repeat(25));
        assertThrows(IllegalArgumentException.class,()->authService.registerUser(value));verifyNoInteractions(userRepository,passwordEncoder);
    }
}
