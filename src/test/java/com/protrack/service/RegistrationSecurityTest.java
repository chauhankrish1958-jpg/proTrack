package com.protrack.service;

import com.protrack.dao.ActivityDAO;
import com.protrack.dao.SettingsDAO;
import com.protrack.dao.UserDAO;
import com.protrack.exception.AppException;
import com.protrack.model.Role;
import com.protrack.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RegistrationSecurityTest {

    private UserDAO mockUserDAO;
    private ActivityDAO mockActivityDAO;
    private SettingsDAO mockSettingsDAO;
    private AuthService authService;

    @BeforeEach
    public void setup() {
        mockUserDAO = mock(UserDAO.class);
        mockActivityDAO = mock(ActivityDAO.class);
        mockSettingsDAO = mock(SettingsDAO.class);
        authService = new AuthService(mockUserDAO, mockActivityDAO, mockSettingsDAO);

        // Default: allow public registration
        when(mockSettingsDAO.getSetting(eq("allow_user_registration"), anyString())).thenReturn("true");
    }

    @Test
    @DisplayName("Verify registration rejects passwords shorter than 8 characters")
    public void testPasswordMinimumLengthEnforced() {
        AppException ex = assertThrows(AppException.class, () -> {
            authService.register("Valid Name", "valid@test.com", "1234567", Role.TEAM_MEMBER);
        });

        assertTrue(ex.getMessage().contains("at least 8 characters"));
        verify(mockUserDAO, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Verify registration preserves raw name characters (e.g. & in R&D) and validates name length")
    public void testRegistrationPreservesRawNameAndValidatesLength() throws AppException {
        when(mockUserDAO.emailExists("rnd@protrack.com", null)).thenReturn(false);
        when(mockUserDAO.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10);
            return u;
        });

        // 1. Verify characters like & and quotes are not prematurely HTML escaped before storage
        User saved = authService.register("R&D Engineer", "rnd@protrack.com", "securePass123", Role.TEAM_MEMBER);
        assertNotNull(saved);
        assertEquals("R&D Engineer", saved.getFullName(), "Raw characters must be preserved without premature double-escaping");

        // 2. Verify empty or too short full names are rejected
        assertThrows(AppException.class, () -> {
            authService.register("   ", "blank@protrack.com", "securePass123", Role.TEAM_MEMBER);
        });
        assertThrows(AppException.class, () -> {
            authService.register("A", "short@protrack.com", "securePass123", Role.TEAM_MEMBER);
        });
    }

    @Test
    @DisplayName("Verify registration allows choosing Role.TEAM_MEMBER, Role.PROJECT_MANAGER, and Role.ADMIN")
    public void testRegistrationAllowsRoleSelection() throws AppException {
        when(mockUserDAO.emailExists(anyString(), any())).thenReturn(false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(mockUserDAO.save(captor.capture())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(55);
            return u;
        });

        // 1. Team Member
        User member = authService.register("New Developer", "dev@protrack.com", "Password123#", Role.TEAM_MEMBER);
        assertNotNull(member);
        assertEquals(Role.TEAM_MEMBER, captor.getValue().getRole());

        // 2. Project Manager
        User pm = authService.register("Project Lead", "lead@protrack.com", "Password123#", Role.PROJECT_MANAGER);
        assertNotNull(pm);
        assertEquals(Role.PROJECT_MANAGER, captor.getValue().getRole());

        // 3. Administrator
        User admin = authService.register("System Admin", "sysadmin@protrack.com", "Password123#", Role.ADMIN);
        assertNotNull(admin);
        assertEquals(Role.ADMIN, captor.getValue().getRole());

        // 4. Null role defaults to TEAM_MEMBER
        User defaultRoleUser = authService.register("Default User", "default@protrack.com", "Password123#", null);
        assertNotNull(defaultRoleUser);
        assertEquals(Role.TEAM_MEMBER, captor.getValue().getRole());
    }

    @Test
    @DisplayName("Verify registration rejects duplicate email addresses")
    public void testDuplicateEmailRejected() {
        when(mockUserDAO.emailExists("existing@protrack.com", null)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> {
            authService.register("Duplicate User", "existing@protrack.com", "ValidPass123", Role.TEAM_MEMBER);
        });

        assertTrue(ex.getMessage().contains("already exists"));
        verify(mockUserDAO, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Verify registration fails when registration policy is disabled")
    public void testRegistrationDisabledInSettings() {
        when(mockSettingsDAO.getSetting(eq("allow_user_registration"), anyString())).thenReturn("false");

        AppException ex = assertThrows(AppException.class, () -> {
            authService.register("Blocked User", "blocked@protrack.com", "ValidPass123", Role.TEAM_MEMBER);
        });

        assertTrue(ex.getMessage().contains("disabled"));
        verify(mockUserDAO, never()).save(any(User.class));
    }
}
