package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
}
