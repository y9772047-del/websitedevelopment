package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.mapper.UserMapper;
import com.example.studyplan.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final RbacMapper rbacMapper;

    public UserServiceImpl(PasswordEncoder passwordEncoder, RbacMapper rbacMapper) {
        this.passwordEncoder = passwordEncoder;
        this.rbacMapper = rbacMapper;
    }

    @Override
    @Transactional
    public User register(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()
                || user.getUsername().length() > 50 || user.getUsername().chars().anyMatch(Character::isISOControl)) {
            throw new RuntimeException("用户名格式错误");
        }
        if (user.getPassword() == null || user.getPassword().getBytes(StandardCharsets.UTF_8).length < 8
                || user.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuntimeException("密码长度需为 8 至 72 字节");
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        user.setRole("student");
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);
        if (rbacMapper.assignRole(user.getId(), "student") != 1) {
            throw new IllegalStateException("student 角色未初始化，请先执行数据库升级脚本");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user != null) {
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                return null;
            }
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new RuntimeException("账号已被禁用，请联系管理员");
            }
            List<String> roles = rbacMapper.rolesForUser(user.getId());
            if (roles == null || roles.isEmpty()) {
                throw new IllegalStateException("账号未分配角色");
            }
            user.setRole(roles.contains("admin") ? "admin" : "student");
            user.setPassword(null);
        }
        return user;
    }
}
