package com.example.studyplan.config;

import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        User user = SessionUtil.currentUser(request.getSession(false));
        if (user != null && "admin".equals(user.getRole())) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"没有管理员权限\",\"data\":null}");
        return false;
    }
}
