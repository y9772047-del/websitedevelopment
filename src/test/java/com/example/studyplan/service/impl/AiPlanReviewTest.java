package com.example.studyplan.service.impl;

import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyTask;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiPlanReviewTest {
    @Test
    void rejectsPlanOutsideCurrentUser() {
        StudyPlanService plans = mock(StudyPlanService.class);
        AiServiceImpl service = new AiServiceImpl(plans, new ObjectMapper(), mock(StudyRecordService.class), mock(StudyTaskService.class));
        assertThatThrownBy(() -> service.generatePlanReview(9L, 7L)).hasMessageContaining("计划不存在");
        verify(plans).getOne(any());
    }

    @Test
    void rejectsReviewWithTooFewRecords() {
        StudyPlanService plans = mock(StudyPlanService.class);
        StudyTaskService tasks = mock(StudyTaskService.class);
        StudyRecordService records = mock(StudyRecordService.class);
        StudyPlan plan = new StudyPlan(); plan.setId(9L); plan.setUserId(7L);
        StudyTask task = new StudyTask(); task.setId(11L); task.setPlanId(9L);
        when(plans.getOne(any())).thenReturn(plan);
        when(tasks.list(org.mockito.ArgumentMatchers.<Wrapper<StudyTask>>any())).thenReturn(List.of(task));
        when(records.list(org.mockito.ArgumentMatchers.<Wrapper<com.example.studyplan.entity.StudyRecord>>any())).thenReturn(List.of());
        AiServiceImpl service = new AiServiceImpl(plans, new ObjectMapper(), records, tasks);
        assertThatThrownBy(() -> service.generatePlanReview(9L, 7L)).hasMessageContaining("至少 2 条");
    }
}
