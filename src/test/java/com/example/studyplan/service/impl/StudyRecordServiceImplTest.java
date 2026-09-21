package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.mapper.StudyPlanMapper;
import com.example.studyplan.mapper.StudyRecordMapper;
import com.example.studyplan.service.StudyTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyRecordServiceImplTest {

    @Test
    void saveAndAccumulateInvestmentAddsMinutesAndRecordCountToTaskPlan() {
        StudyTaskService taskService = mock(StudyTaskService.class);
        StudyPlanMapper planMapper = mock(StudyPlanMapper.class);
        StudyRecordMapper recordMapper = mock(StudyRecordMapper.class);
        StudyRecordServiceImpl service = new StudyRecordServiceImpl(taskService, planMapper);
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);

        StudyTask task = new StudyTask();
        task.setId(10L);
        task.setPlanId(20L);
        when(taskService.getById(10L)).thenReturn(task);
        when(recordMapper.insert(record())).thenReturn(1);
        when(planMapper.incrementInvestment(20L, 45, 1)).thenReturn(1);

        boolean saved = service.saveAndAccumulateInvestment(record());

        assertThat(saved).isTrue();
        verify(planMapper).incrementInvestment(20L, 45, 1);
    }

    @Test
    void updateAndRefreshInvestmentClearsNullableReflectionFields() {
        StudyTaskService taskService = mock(StudyTaskService.class);
        StudyPlanMapper planMapper = mock(StudyPlanMapper.class);
        StudyRecordMapper recordMapper = mock(StudyRecordMapper.class);
        StudyRecordServiceImpl service = new StudyRecordServiceImpl(taskService, planMapper);
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);

        StudyRecord oldRecord = record();
        oldRecord.setId(30L);
        oldRecord.setUserId(7L);
        oldRecord.setProblems("old problem");
        oldRecord.setImprovementPlan("old plan");
        when(recordMapper.selectOne(any())).thenReturn(oldRecord);
        when(recordMapper.selectOne(any(), anyBoolean())).thenReturn(oldRecord);
        when(recordMapper.selectList(any())).thenReturn(List.of(oldRecord));
        when(recordMapper.update((StudyRecord) isNull(), any(Wrapper.class))).thenReturn(1);

        StudyRecord updateRecord = record();
        updateRecord.setId(30L);
        updateRecord.setUserId(7L);
        updateRecord.setProblems(null);
        updateRecord.setImprovementPlan(null);

        boolean updated = service.updateAndRefreshInvestment(updateRecord, 7L);

        assertThat(updated).isTrue();
        ArgumentCaptor<UpdateWrapper> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(recordMapper).update(isNull(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSet())
                .contains("problems", "improvement_plan");
    }

    private StudyRecord record() {
        StudyRecord record = new StudyRecord();
        record.setTaskId(10L);
        record.setDuration(45);
        return record;
    }
}

