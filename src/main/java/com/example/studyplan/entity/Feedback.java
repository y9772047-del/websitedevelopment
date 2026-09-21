package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    private Long id;
    private Long userId;
    private String content;
    private String status;
    private String reply;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
