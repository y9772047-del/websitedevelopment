package com.example.studyplan.controller;

import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.User;
import com.example.studyplan.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
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
    public Result<User> login(@RequestBody User loginForm, HttpSession session) {
        try {
            User user = userService.login(loginForm.getUsername(), loginForm.getPassword());
            if (user == null) {
                return Result.fail("用户名或密码错误");
            }
            session.setAttribute(SessionUtil.LOGIN_USER, user);
            return Result.ok("登录成功", user);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.ok("退出成功", null);
    }

    @GetMapping("/current")
    public Result<User> current(HttpSession session) {
        User user = (User) session.getAttribute(SessionUtil.LOGIN_USER);
        return Result.ok(user);
    }
}
