package com.example.studyplan.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiImprovementResponse {
    private String improvementPlan;
    private List<String> suggestedActions = new ArrayList<>();
    private String generationSource;
    private String generationSourceLabel;
    private String generationSourceDetail;
}
