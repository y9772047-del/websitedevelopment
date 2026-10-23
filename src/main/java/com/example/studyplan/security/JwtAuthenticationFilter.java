package com.example.studyplan.security;

import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTH_COOKIE = "studyplan_auth";

    private final JwtService tokens;
    private final UserMapper users;
    private final RbacMapper rbac;

    public JwtAuthenticationFilter(JwtService tokens, UserMapper users, RbacMapper rbac) {
        this.tokens = tokens;
        this.users = users;
        this.rbac = rbac;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AUTH_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = tokens.userId(token);
                User user = users.selectById(userId);
                if (user != null && Integer.valueOf(1).equals(user.getStatus())) {
                    List<String> roles = rbac.rolesForUser(userId);
                    if (roles != null && !roles.isEmpty()) {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        for (String role : roles) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
                        }
                        List<String> permissions = rbac.permissionsForUser(userId);
                        if (permissions != null) {
                            for (String permission : permissions) {
                                authorities.add(new SimpleGrantedAuthority(permission));
                            }
                        }
                        user.setRole(roles.contains("admin") ? "admin" : "student");
                        user.setPassword(null);
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
                        SecurityContextHolder.setContext(context);
                    }
                }
            } catch (JwtException ignored) {
                // A bad or expired cookie remains unauthenticated.
            }
        }
        chain.doFilter(request, response);
    }
}
