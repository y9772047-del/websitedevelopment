package com.example.studyplan.controller;

import com.example.studyplan.entity.User;
import com.example.studyplan.security.JwtService;
import com.example.studyplan.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerJwtTest {
    private final UserService users = mock(UserService.class);
    private final JwtService tokens = new JwtService("test-secret-with-at-least-thirty-two-bytes", 1800);
    private final AuthController controller = new AuthController(users, tokens);

    @Test
    void loginReturnsUserAndSetsHttpOnlyJwtCookie() {
        User form = new User();
        form.setUsername("learner");
        form.setPassword("secret");
        User user = new User();
        user.setId(4L);
        user.setUsername("learner");
        when(users.login("learner", "secret")).thenReturn(user);
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.login(form, new MockHttpServletRequest(), response);

        assertEquals(user, result.getData());
        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("studyplan_auth="));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/api"));
        assertFalse(cookie.contains("secret"));
    }

    @Test
    void logoutExpiresJwtCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(new MockHttpServletRequest(), response);

        assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
    }
}
