package com.example.studyplan.controller;

import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.User;
import com.example.studyplan.security.JwtAuthenticationFilter;
import com.example.studyplan.security.JwtService;
import com.example.studyplan.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        try {
            return Result.ok("注册成功", userService.register(user));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody User loginForm, HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = userService.login(loginForm.getUsername(), loginForm.getPassword());
            if (user == null) {
                return Result.fail("用户名或密码错误");
            }
            response.addHeader(HttpHeaders.SET_COOKIE, authCookie(jwtService.issue(user.getId()),
                    request.isSecure(), Duration.ofSeconds(jwtService.ttlSeconds())).toString());
            return Result.ok("登录成功", user);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie("", request.isSecure(), Duration.ZERO).toString());
        return Result.ok("退出成功", null);
    }

    @GetMapping("/current")
    public Result<User> current() {
        return Result.ok(SessionUtil.currentUser(null));
    }

    private ResponseCookie authCookie(String value, boolean secure, Duration age) {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE, value)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api")
                .maxAge(age).build();
    }
}
