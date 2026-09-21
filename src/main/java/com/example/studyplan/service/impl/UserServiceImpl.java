package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.UserMapper;
import com.example.studyplan.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public User register(User user) {
        Long count = lambdaQuery().eq(User::getUsername, user.getUsername()).count();
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        user.setRole("student");
        user.setStatus(1);
        save(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getPassword, password));
        if (user != null) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new RuntimeException("账号已被禁用，请联系管理员");
            }
            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole("student");
            }
            user.setPassword(null);
        }
        return user;
    }
}
