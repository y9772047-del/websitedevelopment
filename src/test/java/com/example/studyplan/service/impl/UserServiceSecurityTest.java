package com.example.studyplan.service.impl;

import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class UserServiceSecurityTest {
    private final UserMapper users = mock(UserMapper.class);
    private final RbacMapper rbac = mock(RbacMapper.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(encoder, rbac);
        ReflectionTestUtils.setField(service, "baseMapper", users);
    }

    @Test
    void registrationStoresOnlyBcryptHashAndAssignsStudentRole() {
        when(users.selectCount(any())).thenReturn(0L);
        AtomicReference<String> storedPassword = new AtomicReference<>();
        when(users.insert(any(User.class))).thenAnswer(call -> {
            User saved = call.getArgument(0);
            storedPassword.set(saved.getPassword());
            saved.setId(7L);
            return 1;
        });
        when(rbac.assignRole(7L, "student")).thenReturn(1);
        User newUser = new User();
        newUser.setUsername("learner");
        newUser.setPassword("safe-password");

        User result = service.register(newUser);

        assertTrue(encoder.matches("safe-password", storedPassword.get()));
        assertFalse(storedPassword.get().equals("safe-password"));
        assertNull(result.getPassword());
        verify(rbac).assignRole(7L, "student");
    }

    @Test
    void loginChecksBcryptHashInsteadOfPlaintextDatabaseMatch() {
        User stored = new User();
        stored.setId(9L);
        stored.setUsername("learner");
        stored.setPassword(encoder.encode("safe-password"));
        stored.setStatus(1);
        stored.setRole("student");
        when(users.selectOne(any())).thenReturn(stored);
        when(rbac.rolesForUser(9L)).thenReturn(List.of("student"));

        assertNull(service.login("learner", "wrong"));
        assertTrue(service.login("learner", "safe-password").getId() == 9L);
        verify(users, times(2)).selectOne(any());
    }

    @Test
    void loginReturnsRoleFromRbacTable() {
        User stored = new User();
        stored.setId(9L);
        stored.setUsername("learner");
        stored.setPassword(encoder.encode("safe-password"));
        stored.setStatus(1);
        stored.setRole("student");
        when(users.selectOne(any())).thenReturn(stored);
        when(rbac.rolesForUser(9L)).thenReturn(List.of("admin"));

        assertTrue("admin".equals(service.login("learner", "safe-password").getRole()));
    }

    @Test
    void registrationRejectsShortPasswordBeforeDatabaseWrite() {
        User newUser = new User();
        newUser.setUsername("learner");
        newUser.setPassword("short");

        assertThrows(RuntimeException.class, () -> service.register(newUser));

        verifyNoInteractions(users, rbac);
    }
}
