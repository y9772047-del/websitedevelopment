package com.example.studyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyplan.entity.User;

public interface UserService extends IService<User> {
    User register(User user);
    User login(String username, String password);
}
