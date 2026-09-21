package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.entity.StudyTaskCategory;
import com.example.studyplan.mapper.StudyPlanMapper;
import com.example.studyplan.mapper.StudyRecordMapper;
import com.example.studyplan.mapper.StudyTaskCategoryMapper;
import com.example.studyplan.mapper.StudyTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyTaskServiceImplTest {

    @Test
    void normalizeLegacyCategoriesBackfillsBlankAndInvalidTasks() {
        StudyTaskCategoryMapper categoryMapper = mock(StudyTaskCategoryMapper.class);
        StudyTaskMapper taskMapper = mock(StudyTaskMapper.class);
        StudyTaskServiceImpl service = new StudyTaskServiceImpl(categoryMapper, mock(StudyRecordMapper.class), mock(StudyPlanMapper.class));
        ReflectionTestUtils.setField(service, "baseMapper", taskMapper);

        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category("璇剧▼杩涘害", 1),
                category("鍏朵粬浠诲姟", 8)
        ));
        when(taskMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(3);

        int updatedRows = service.normalizeLegacyCategories();

        assertThat(updatedRows).isEqualTo(3);
        verify(taskMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void removeTaskWithRecordsDeletesRecordsAndDeductsPlanInvestmentBeforeTask() {
        StudyTaskCategoryMapper categoryMapper = mock(StudyTaskCategoryMapper.class);
        StudyRecordMapper recordMapper = mock(StudyRecordMapper.class);
        StudyPlanMapper planMapper = mock(StudyPlanMapper.class);
        StudyTaskMapper taskMapper = mock(StudyTaskMapper.class);
        StudyTaskServiceImpl service = new StudyTaskServiceImpl(categoryMapper, recordMapper, planMapper);
        ReflectionTestUtils.setField(service, "baseMapper", taskMapper);

        StudyTask task = new StudyTask();
        task.setId(10L);
        task.setUserId(2L);
        task.setPlanId(20L);
        StudyRecord firstRecord = record(10L, 45);
        StudyRecord secondRecord = record(10L, 15);

        when(taskMapper.selectOne(any())).thenReturn(task);
        when(recordMapper.selectList(any())).thenReturn(List.of(firstRecord, secondRecord));
        when(planMapper.incrementInvestment(20L, -60, -2)).thenReturn(1);
        when(recordMapper.delete(any())).thenReturn(2);
        when(taskMapper.delete(any())).thenReturn(1);

        boolean removed = service.removeTaskWithRecords(10L, 2L);

        assertThat(removed).isTrue();
        verify(planMapper).incrementInvestment(20L, -60, -2);
        verify(recordMapper).delete(any());
        verify(taskMapper).delete(any());
    }

    private StudyTaskCategory category(String name, int sortOrder) {
        StudyTaskCategory category = new StudyTaskCategory();
        category.setCategoryName(name);
        category.setSortOrder(sortOrder);
        return category;
    }

    private StudyRecord record(Long taskId, Integer duration) {
        StudyRecord record = new StudyRecord();
        record.setTaskId(taskId);
        record.setUserId(2L);
        record.setDuration(duration);
        return record;
    }
}