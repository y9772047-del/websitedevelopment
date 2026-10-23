package com.example.studyplan.security;

import com.example.studyplan.config.AdminInterceptor;
import com.example.studyplan.config.LoginInterceptor;
import com.example.studyplan.config.WebConfig;
import com.example.studyplan.controller.AuthController;
import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.mapper.UserMapper;
import com.example.studyplan.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNull;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {AuthController.class, SecurityConfig.class, JwtService.class,
        JwtAuthenticationFilter.class, WebConfig.class, LoginInterceptor.class, AdminInterceptor.class})
class AuthSecurityFlowTest {
    @Autowired MockMvc mvc;
    @MockBean UserService userService;
    @MockBean UserMapper users;
    @MockBean RbacMapper rbac;

    @Test
    void browserGetsCsrfThenLogsInAndRestoresCurrentUserFromJwt() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("learner");
        user.setRole("student");
        user.setStatus(1);
        when(userService.login("learner", "password")).thenReturn(user);
        when(users.selectById(7L)).thenReturn(user);
        when(rbac.rolesForUser(7L)).thenReturn(List.of("student"));
        when(rbac.permissionsForUser(7L)).thenReturn(List.of("study:access"));

        Cookie csrf = mvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        Cookie auth = mvc.perform(post("/api/auth/login").cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json")
                        .content("{\"username\":\"learner\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andReturn().getResponse().getCookie(JwtAuthenticationFilter.AUTH_COOKIE);

        var current = mvc.perform(get("/api/auth/current").cookie(auth, csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.role").value("student"))
                .andReturn();
        assertNull(current.getResponse().getCookie("XSRF-TOKEN"),
                "authenticated GET must not delete the CSRF cookie");
        mvc.perform(post("/api/auth/logout").cookie(auth, csrf))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(auth, csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk());
    }
}
