package com.example.studyplan.security;

import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.mapper.UserMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private final JwtService tokens = new JwtService("test-secret-with-at-least-thirty-two-bytes", 1800);
    private final UserMapper users = mock(UserMapper.class);
    private final RbacMapper rbac = mock(RbacMapper.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users, rbac);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenLoadsCurrentRolesAndPermissionsFromDatabase() throws Exception {
        User user = new User();
        user.setId(9L);
        user.setRole("student");
        user.setStatus(1);
        user.setPassword("stored-hash");
        when(users.selectById(9L)).thenReturn(user);
        when(rbac.rolesForUser(9L)).thenReturn(List.of("admin"));
        when(rbac.permissionsForUser(9L)).thenReturn(List.of("admin:access"));
        MockHttpServletRequest request = requestFor(9L);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(9L, ((User) authentication.getPrincipal()).getId());
        assertEquals("admin", ((User) authentication.getPrincipal()).getRole());
        assertNull(((User) authentication.getPrincipal()).getPassword());
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin:access")));
    }

    @Test
    void disabledUserCannotUsePreviouslyIssuedToken() throws Exception {
        User user = new User();
        user.setId(9L);
        user.setStatus(0);
        when(users.selectById(9L)).thenReturn(user);

        filter.doFilter(requestFor(9L), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest requestFor(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/plans");
        request.setCookies(new Cookie(JwtAuthenticationFilter.AUTH_COOKIE, tokens.issue(userId)));
        return request;
    }
}
