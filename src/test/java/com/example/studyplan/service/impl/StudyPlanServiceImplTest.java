package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.mapper.StudyPlanMapper;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyPlanServiceImplTest {

    @Test
    void removePlanWithTasksAndRecordsDeletesChildrenBeforePlan() {
        StudyPlanMapper planMapper = mock(StudyPlanMapper.class);
        StudyTaskService taskService = mock(StudyTaskService.class);
        StudyRecordService recordService = mock(StudyRecordService.class);
        StudyPlanServiceImpl service = new StudyPlanServiceImpl(taskService, recordService);
        ReflectionTestUtils.setField(service, "baseMapper", planMapper);

        when(planMapper.selectOne(any())).thenReturn(plan());
        when(taskService.list(org.mockito.ArgumentMatchers.<Wrapper<StudyTask>>any())).thenReturn(List.of(task(10L), task(11L)));
        when(recordService.remove(org.mockito.ArgumentMatchers.<Wrapper<StudyRecord>>any())).thenReturn(true);
        when(taskService.remove(org.mockito.ArgumentMatchers.<Wrapper<StudyTask>>any())).thenReturn(true);
        when(planMapper.delete(any())).thenReturn(1);

        boolean removed = service.removePlanWithTasksAndRecords(1L, 2L);

        assertThat(removed).isTrue();
        verify(recordService).remove(org.mockito.ArgumentMatchers.<Wrapper<StudyRecord>>any());
        verify(taskService).remove(org.mockito.ArgumentMatchers.<Wrapper<StudyTask>>any());
        verify(planMapper).delete(any());
    }

    private StudyPlan plan() {
        StudyPlan plan = new StudyPlan();
        plan.setId(1L);
        plan.setUserId(2L);
        return plan;
    }

    private StudyTask task(Long id) {
        StudyTask task = new StudyTask();
        task.setId(id);
        task.setUserId(2L);
        task.setPlanId(1L);
        return task;
    }
}