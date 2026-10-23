package com.example.studyplan.config;

import com.example.studyplan.controller.PlanController;
import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.mapper.UserMapper;
import com.example.studyplan.security.JwtAuthenticationFilter;
import com.example.studyplan.security.JwtService;
import com.example.studyplan.security.SecurityConfig;
import com.example.studyplan.service.StudyPlanService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@org.springframework.test.context.ContextConfiguration(classes = {PlanController.class, WebConfig.class,
        LoginInterceptor.class, AdminInterceptor.class, SecurityConfig.class,
        JwtService.class, JwtAuthenticationFilter.class})
class ExperimentCorsTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService tokens;
    @MockBean StudyPlanService plans;
    @MockBean UserMapper users;
    @MockBean RbacMapper rbac;

    @BeforeEach void setUpUser() {
        User user = new User();
        user.setId(7L);
        user.setStatus(1);
        org.mockito.Mockito.when(users.selectById(7L)).thenReturn(user);
        org.mockito.Mockito.when(rbac.rolesForUser(7L)).thenReturn(java.util.List.of("student"));
        org.mockito.Mockito.when(rbac.permissionsForUser(7L)).thenReturn(java.util.List.of("study:access"));
    }

    @Test void preflightDoesNotRequireLogin() throws Exception {
        mvc.perform(options("/api/plans/1")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test void actualRequestStillRequiresLogin() throws Exception {
        mvc.perform(get("/api/plans").header("Origin", "http://localhost:5173"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test void unknownOriginIsRejected() throws Exception {
        mvc.perform(options("/api/plans").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void authenticatedListUsesExistingEnvelope() throws Exception {
        org.mockito.Mockito.when(plans.list(org.mockito.ArgumentMatchers.any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of());
        mvc.perform(get("/api/plans").cookie(loggedIn()).header("Origin", "http://localhost:5173"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test void createUsesSessionOwnerInsteadOfSuppliedOwner() throws Exception {
        Cookie csrf = csrfCookie();
        mvc.perform(post("/api/plans").cookie(loggedIn(), csrf).header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                .content("{\"id\":999,\"userId\":999,\"planName\":\"Test plan\",\"status\":\"进行中\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(7));
        org.mockito.Mockito.verify(plans).save(org.mockito.ArgumentMatchers.argThat(plan ->
            plan.getId() == null && plan.getUserId().equals(7L)));
    }

    @Test void blankNameRetainsBusinessFailureEnvelope() throws Exception {
        Cookie csrf = csrfCookie();
        mvc.perform(post("/api/plans").cookie(loggedIn(), csrf).header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                .content("{\"planName\":\" \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        org.mockito.Mockito.verifyNoInteractions(plans);
    }

    @Test void malformedDateIsHttpBadRequest() throws Exception {
        Cookie csrf = csrfCookie();
        mvc.perform(post("/api/plans").cookie(loggedIn(), csrf).header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json")
                .content("{\"planName\":\"Test\",\"startDate\":\"not-a-date\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void missingOrOtherUsersPlanIsNotReturned() throws Exception {
        mvc.perform(get("/api/plans/999").cookie(loggedIn()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
    }

    @Test void writesRequireCsrfTokenEvenWithValidJwt() throws Exception {
        mvc.perform(post("/api/plans").cookie(loggedIn()).contentType("application/json")
                .content("{\"planName\":\"Test plan\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void studentTokenCannotReachAdminApi() throws Exception {
        mvc.perform(get("/api/admin/users").cookie(loggedIn()))
            .andExpect(status().isForbidden());
    }

    private Cookie loggedIn() {
        return new Cookie(JwtAuthenticationFilter.AUTH_COOKIE, tokens.issue(7L));
    }

    private Cookie csrfCookie() throws Exception {
        Cookie cookie = mvc.perform(get("/api/plans").cookie(loggedIn()))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        org.mockito.Mockito.clearInvocations(plans);
        return cookie;
    }
}
