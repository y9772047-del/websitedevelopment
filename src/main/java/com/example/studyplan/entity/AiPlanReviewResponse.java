package com.example.studyplan.entity;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiPlanReviewResponse {
    private String summary;
    private String problems;
    private List<String> actions = new ArrayList<>();
    private int recordCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String generationSourceLabel;
}
