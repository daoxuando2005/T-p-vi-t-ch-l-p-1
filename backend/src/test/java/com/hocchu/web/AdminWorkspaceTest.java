package com.hocchu.web;

import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import com.hocchu.web.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminWorkspaceTest {
    @Mock UserRepository users;@Mock RoleRepository roles;@Mock AlphabetLessonRepository alphabet;
    @Mock JdbcTemplate jdbc;@Mock PasswordEncoder passwords;
    @InjectMocks AdminService service;
    User user(long id,String role){return User.builder().id(id).username("fixture"+id).enabled(true).roles(new HashSet<>(Set.of(new Role(1L,role)))).build();}
    @Test void roleAndPermissionInputsCannotIntroduceArbitraryAuthority() {
        assertEquals("ROLE_TEACHER",AdminService.roleName("teacher"));
        assertThrows(IllegalArgumentException.class,()->AdminService.roleName("SUPERADMIN"));
        assertThrows(IllegalArgumentException.class,()->AdminService.permissionsFor("ROLE_STUDENT",List.of("ROLE_ADMIN")));
        assertThrows(IllegalArgumentException.class,()->AdminService.permissionsFor("ROLE_TEACHER",List.of("ROLE_ADMIN")));
        assertThrows(IllegalArgumentException.class,()->AdminService.permissionsFor("ROLE_TEACHER",Arrays.asList((String)null)));
        assertEquals(List.of("PERM_PARENT_TRACKING"),AdminService.permissionsFor("ROLE_PARENT",List.of()));
    }
    @Test void selfLockAndLastActiveAdministratorAreProtected() {
        when(users.findForAdminUpdate(7L)).thenReturn(Optional.of(user(7,"ROLE_ADMIN")));
        assertThrows(IllegalArgumentException.class,()->service.toggleUserStatus(7L,7L));
        when(jdbc.queryForObject(anyString(),eq(Long.class))).thenReturn(1L);
        assertThrows(IllegalArgumentException.class,()->service.toggleUserStatus(8L,7L));verify(users,never()).save(any());
    }
    @Test void lockingAndPasswordResetRevokeExistingSessionVersion() {
        User target=user(7,"ROLE_STUDENT");when(users.findForAdminUpdate(7L)).thenReturn(Optional.of(target));
        service.toggleUserStatus(8L,7L);assertFalse(target.getEnabled());assertEquals(1,target.getSessionVersion());
        service.toggleUserStatus(8L,7L);assertTrue(target.getEnabled());assertEquals(2,target.getSessionVersion());
        when(passwords.encode("fixture-password")).thenReturn("encoded");service.resetPassword(7L,"fixture-password");
        assertEquals(3,target.getSessionVersion());assertEquals("encoded",target.getPassword());
    }
    @Test void accountWithHistoryCannotBeDeleted() {
        when(users.findForAdminUpdate(7L)).thenReturn(Optional.of(user(7,"ROLE_STUDENT")));
        when(jdbc.queryForObject(anyString(),eq(Boolean.class),any(Object[].class))).thenReturn(true);
        assertThrows(IllegalArgumentException.class,()->service.deleteUser(8L,7L));verify(users,never()).delete(any());
    }
    @Test void fixedAlphabetCannotLoseARequiredLetter() {
        assertThrows(IllegalArgumentException.class,()->service.deleteAlphabetLesson(1L));verify(alphabet,never()).deleteById(any());
        assertThrows(IllegalArgumentException.class,()->service.saveAlphabetLesson(AlphabetLesson.builder().letter("Z").build()));
    }
    @Test void curriculumMustFitTheExactAndroidBlankAndTileContract() {
        var row=new AdminOperationsService.Practice("ba","Hình ba",List.of("Bé","yêu","ba."));
        AdminOperationsService.validateCurriculum("A",new AdminOperationsService.Curriculum(List.of(row,row,row)));
        assertThrows(IllegalArgumentException.class,()->AdminOperationsService.validateCurriculum("Ă",new AdminOperationsService.Curriculum(List.of(row,row,row))));
        assertThrows(IllegalArgumentException.class,()->AdminOperationsService.validateCurriculum("A",new AdminOperationsService.Curriculum(List.of(row))));
        assertThrows(IllegalArgumentException.class,()->AdminService.validatePassword("á".repeat(37)));
    }
    @Test void editingMetadataKeepsItsAlphabetIdentityAndDoesNotEraseOtherFields() {
        var stored=AlphabetLesson.builder().id(1L).letter("A").exampleWord("ba").build();
        when(alphabet.findById(1L)).thenReturn(Optional.of(stored));when(alphabet.save(any())).thenAnswer(call->call.getArgument(0));
        var update=AlphabetLesson.builder().id(1L).letter("A").exampleWord("hoa").audioUrl("/api/public/media/audio/a.mp3").isHidden(true).build();
        var result=service.saveAlphabetLesson(update);assertEquals("A",result.getLetter());assertEquals("hoa",result.getExampleWord());assertTrue(result.getIsHidden());
        assertEquals(update.getAudioUrl(),result.getAudioUrl());
        update.setLetter("B");assertThrows(IllegalArgumentException.class,()->service.saveAlphabetLesson(update));assertEquals("A",stored.getLetter());
    }
}
