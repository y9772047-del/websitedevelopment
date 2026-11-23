package com.example.studyplan.dto;

import lombok.Data;

@Data
public class TaskCategorySummary {
    private Long id;
    private String categoryName;
    private Long taskCount;
}
