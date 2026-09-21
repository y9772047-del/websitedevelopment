package com.example.studyplan.common;

import com.example.studyplan.entity.User;
import jakarta.servlet.http.HttpSession;

public class SessionUtil {
    public static final String LOGIN_USER = "loginUser";

    private SessionUtil() {
    }

    public static Long currentUserId(HttpSession session) {
        User user = (User) session.getAttribute(LOGIN_USER);
        return user == null ? null : user.getId();
    }

    public static User currentUser(HttpSession session) {
        return (User) session.getAttribute(LOGIN_USER);
    }

    public static boolean isAdmin(HttpSession session) {
        User user = currentUser(session);
        return user != null && "admin".equals(user.getRole());
    }
}
