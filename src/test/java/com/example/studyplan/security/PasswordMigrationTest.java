package com.example.studyplan.security;

import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordMigrationTest {
    private final UserMapper users = mock(UserMapper.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void replacesLegacyPlaintextWithBcryptWithoutChangingOtherFields() {
        User old = new User();
        old.setId(5L);
        old.setPassword("legacy-pass");
        when(users.selectList(isNull())).thenReturn(List.of(old));
        AtomicReference<User> update = new AtomicReference<>();
        when(users.updateById(any(User.class))).thenAnswer(call -> {
            update.set(call.getArgument(0));
            return 1;
        });

        new PasswordMigration(users, encoder).migrate();

        assertEquals(5L, update.get().getId());
        assertTrue(encoder.matches("legacy-pass", update.get().getPassword()));
    }

    @Test
    void leavesExistingBcryptHashesAlone() {
        User alreadyHashed = new User();
        alreadyHashed.setId(6L);
        alreadyHashed.setPassword(encoder.encode("secret"));
        when(users.selectList(isNull())).thenReturn(List.of(alreadyHashed));

        new PasswordMigration(users, encoder).migrate();

        verify(users, never()).updateById(any(User.class));
    }
}
