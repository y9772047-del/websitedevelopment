package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_task")
public class StudyTask {
    private Long id;
    private Long userId;
    private Long planId;
    private String taskName;
    private String description;
    private String category;
    private String priority;
    private LocalDate deadline;
    private String status;
    private String repeatType;
    private LocalDate repeatEndDate;
    private String repeatGroupId;
    private Boolean reminderEnabled;
    private Integer reminderDaysBefore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}