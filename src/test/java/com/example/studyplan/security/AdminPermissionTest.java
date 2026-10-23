package com.example.studyplan.security;

import com.example.studyplan.config.AdminInterceptor;
import com.example.studyplan.config.LoginInterceptor;
import com.example.studyplan.config.WebConfig;
import com.example.studyplan.controller.AdminController;
import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.mapper.UserMapper;
import com.example.studyplan.service.FeedbackService;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyTaskService;
import com.example.studyplan.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AdminController.class)
@ContextConfiguration(classes = {AdminController.class, SecurityConfig.class, JwtService.class,
        JwtAuthenticationFilter.class, WebConfig.class, LoginInterceptor.class, AdminInterceptor.class})
class AdminPermissionTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService tokens;
    @MockBean UserService userService;
    @MockBean StudyTaskService taskService;
    @MockBean StudyPlanService planService;
    @MockBean FeedbackService feedbackService;
    @MockBean UserMapper users;
    @MockBean RbacMapper rbac;

    @BeforeEach
    void currentAdmin() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        when(users.selectById(1L)).thenReturn(user);
        when(rbac.rolesForUser(1L)).thenReturn(List.of("admin"));
        when(rbac.permissionsForUser(1L)).thenReturn(List.of("admin:access"));
    }

    @Test
    void adminNeedsSpecificUserManagementPermissionForStatusChange() throws Exception {
        Cookie csrf = mvc.perform(get("/api/admin/users/999").cookie(auth()))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(put("/api/admin/users/2/status").cookie(auth(), csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json").content("{\"status\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDisableUserWithAdminRoleInRbacTable() throws Exception {
        when(rbac.permissionsForUser(1L)).thenReturn(List.of("admin:access", "users:manage"));
        User target = new User();
        target.setId(2L);
        target.setRole("student");
        when(userService.getById(2L)).thenReturn(target);
        when(rbac.rolesForUser(2L)).thenReturn(List.of("admin"));
        Cookie csrf = mvc.perform(get("/api/admin/users/999").cookie(auth()))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(put("/api/admin/users/2/status").cookie(auth(), csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType("application/json").content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
        verify(userService, never()).updateById(any());
    }

    private Cookie auth() {
        return new Cookie(JwtAuthenticationFilter.AUTH_COOKIE, tokens.issue(1L));
    }
}
