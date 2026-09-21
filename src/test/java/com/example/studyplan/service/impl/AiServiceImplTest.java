package com.example.studyplan.service.impl;

import com.example.studyplan.entity.AiImprovementRequest;
import com.example.studyplan.entity.AiImprovementResponse;
import com.example.studyplan.service.StudyPlanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiServiceImplTest {

    @Test
    void fallbackUsesStudyContentWhenStudentReportsNoProblem() {
        AiServiceImpl service = new AiServiceImpl(mock(StudyPlanService.class), new ObjectMapper());
        AiImprovementRequest request = new AiImprovementRequest();
        request.setTaskName("六级阅读");
        request.setStudyContent("2025.12 第一套第一篇重做都会");
        request.setCompletion("已完成");
        request.setProblems("无");
        request.setProblemSummary("无");

        AiImprovementResponse response = service.generateImprovement(request, 7L);

        assertThat(response.getGenerationSource()).isEqualTo("local");
        assertThat(response.getGenerationSourceLabel()).contains("本地规则生成");
        assertThat(response.getGenerationSourceDetail()).contains("DeepSeek");
        assertThat(response.getImprovementPlan())
                .contains("2025.12 第一套第一篇")
                .contains("重做")
                .contains("阅读")
                .doesNotContain("暴露的问题")
                .doesNotContain("问题重点：无")
                .doesNotContain("基础例题");
        assertThat(response.getSuggestedActions()).anySatisfy(action ->
                assertThat(action).contains("2025.12 第一套第一篇"));
    }
}
