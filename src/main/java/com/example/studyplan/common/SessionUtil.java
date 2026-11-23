package com.example.studyplan.common;

import com.example.studyplan.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SessionUtil {
    public static final String LOGIN_USER = "loginUser";

    private SessionUtil() {
    }

    public static Long currentUserId(HttpSession session) {
        User user = currentUser(session);
        return user == null ? null : user.getId();
    }

    public static User currentUser(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return session == null ? null : (User) session.getAttribute(LOGIN_USER);
    }

    public static boolean isAdmin(HttpSession session) {
        User user = currentUser(session);
        return user != null && "admin".equals(user.getRole());
    }
}
