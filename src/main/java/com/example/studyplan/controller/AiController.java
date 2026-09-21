package com.example.studyplan.controller;

import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.AiImprovementRequest;
import com.example.studyplan.entity.AiImprovementResponse;
import com.example.studyplan.entity.AiWeeklyReportResponse;
import com.example.studyplan.service.AiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate-improvement")
    public Result<AiImprovementResponse> generateImprovement(@RequestBody AiImprovementRequest request, HttpSession session) {
        if (request == null) {
            return Result.fail("请填写需要分析的学习情况");
        }
        if ((request.getStudyContent() == null || request.getStudyContent().isBlank())
                && (request.getProblemSummary() == null || request.getProblemSummary().isBlank())
                && (request.getProblems() == null || request.getProblems().isBlank())) {
            return Result.fail("请至少填写学习内容或遇到的问题");
        }
        try {
            AiImprovementResponse response = aiService.generateImprovement(request, SessionUtil.currentUserId(session));
            return Result.ok("AI 改进措施生成成功", response);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/generate-weekly-report")
    public Result<AiWeeklyReportResponse> generateWeeklyReport(HttpSession session) {
        try {
            AiWeeklyReportResponse response = aiService.generateWeeklyReport(SessionUtil.currentUserId(session));
            return Result.ok("AI 周报生成成功", response);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
