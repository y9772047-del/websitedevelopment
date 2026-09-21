package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("study_record")
public class StudyRecord {
    private Long id;
    private Long userId;
    private Long taskId;
    private LocalDate studyDate;
    private Integer duration;
    private String content;
    private String completionStatus;
    private String problems;
    private String improvementPlan;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
