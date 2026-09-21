package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("important_node")
public class ImportantNode {
    private Long id;
    private Long userId;
    private String title;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String importance;
    private String description;
    private Long planId;
    private Long taskId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
