package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_plan")
public class StudyPlan {
    private Long id;
    private Long userId;
    private String planName;
    private String goal;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer totalStudyMinutes;
    private Integer totalStudyRecordCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
