package com.example.studyplan.service;

import com.example.studyplan.entity.AiImprovementRequest;
import com.example.studyplan.entity.AiImprovementResponse;
import com.example.studyplan.entity.AiWeeklyReportResponse;

public interface AiService {
    AiImprovementResponse generateImprovement(AiImprovementRequest request, Long userId);
    AiWeeklyReportResponse generateWeeklyReport(Long userId);
}
