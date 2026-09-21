package com.example.studyplan.controller;

import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.AiImprovementRequest;
import com.example.studyplan.entity.AiImprovementResponse;
import com.example.studyplan.entity.AiWeeklyReportResponse;
import com.example.studyplan.entity.User;
import com.example.studyplan.service.AiService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiControllerTest {

    @Test
    void generateImprovementAcceptsStudyContentWithoutProblems() {
        AiService aiService = mock(AiService.class);
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(7L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);

        AiImprovementResponse response = new AiImprovementResponse();
        response.setImprovementPlan("复盘阅读定位方法");
        response.setSuggestedActions(List.of("整理定位句"));
        response.setGenerationSource("local");
        response.setGenerationSourceLabel("本地规则生成");
        when(aiService.generateImprovement(any(AiImprovementRequest.class), eq(7L))).thenReturn(response);

        AiImprovementRequest request = new AiImprovementRequest();
        request.setTaskName("六级阅读");
        request.setStudyContent("2025.12 第一套第一篇重做都会");

        Result<AiImprovementResponse> result = new AiController(aiService).generateImprovement(request, session);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getGenerationSourceLabel()).isEqualTo("本地规则生成");
        verify(aiService).generateImprovement(request, 7L);
    }

    @Test
    void generateWeeklyReportReturnsDeepSeekReport() {
        AiService aiService = mock(AiService.class);
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(7L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);

        AiWeeklyReportResponse response = new AiWeeklyReportResponse();
        response.setWeeklySummary("本周完成了英语和高数复习");
        response.setMainProblems("错题复盘不够及时");
        response.setNextWeekSuggestions("优先安排高数错题复盘");
        response.setSuggestedActions(List.of("整理错题", "安排晨读"));
        response.setGenerationSource("deepseek");
        when(aiService.generateWeeklyReport(7L)).thenReturn(response);

        Result<AiWeeklyReportResponse> result = new AiController(aiService).generateWeeklyReport(session);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getGenerationSource()).isEqualTo("deepseek");
        assertThat(result.getData().getSuggestedActions()).contains("整理错题");
        verify(aiService).generateWeeklyReport(7L);
    }

    @Test
    void generateWeeklyReportFailsWhenDeepSeekUnavailable() {
        AiService aiService = mock(AiService.class);
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(7L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);
        when(aiService.generateWeeklyReport(7L)).thenThrow(new RuntimeException("DeepSeek API Key 未配置"));

        Result<AiWeeklyReportResponse> result = new AiController(aiService).generateWeeklyReport(session);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("DeepSeek API Key 未配置");
    }

}
