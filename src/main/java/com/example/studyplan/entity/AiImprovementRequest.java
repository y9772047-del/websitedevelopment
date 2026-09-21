package com.example.studyplan.entity;

import lombok.Data;

@Data
public class AiImprovementRequest {
    private Long planId;
    private String planName;
    private String taskName;
    private String studyContent;
    private String completion;
    private String problems;
    private String problemSummary;
}
