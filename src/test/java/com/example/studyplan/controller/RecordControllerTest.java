package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.User;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordControllerTest {

    @Test
    void addSavesRecordWithImprovementPlan() {
        StudyRecordService recordService = mock(StudyRecordService.class);
        StudyTaskService taskService = mock(StudyTaskService.class);
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(7L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);
        when(taskService.exists(any(Wrapper.class))).thenReturn(true);

        StudyRecord record = new StudyRecord();
        record.setTaskId(12L);
        record.setContent("完成导数练习");
        record.setProblems("隐函数求导不会");
        record.setImprovementPlan("先复习隐函数求导步骤，再做 5 道基础题。");

        RecordController controller = new RecordController(recordService, taskService);
        Result<StudyRecord> result = controller.add(record, session);

        assertThat(result.isSuccess()).isTrue();
        verify(recordService).saveAndAccumulateInvestment(record);
        verify(taskService, never()).update(any(), any(Wrapper.class));
        assertThat(record.getImprovementPlan()).isEqualTo("先复习隐函数求导步骤，再做 5 道基础题。");
        assertThat(record.getUserId()).isEqualTo(7L);
    }

    @Test
    void addSavesRecordWhenImprovementPlanIsBlank() {
        StudyRecordService recordService = mock(StudyRecordService.class);
        StudyTaskService taskService = mock(StudyTaskService.class);
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(7L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);
        when(taskService.exists(any(Wrapper.class))).thenReturn(true);

        StudyRecord record = new StudyRecord();
        record.setTaskId(12L);
        record.setContent("完成导数练习");
        record.setImprovementPlan(" ");

        RecordController controller = new RecordController(recordService, taskService);
        controller.add(record, session);

        verify(recordService).saveAndAccumulateInvestment(record);
        verify(taskService, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void addRejectsRecordWhenTaskDoesNotBelongToCurrentUser() {
        StudyRecordService recordService = mock(StudyRecordService.class);
        StudyTaskService taskService = mock(StudyTaskService.class);
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(7L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);
        when(taskService.exists(any(Wrapper.class))).thenReturn(false);

        StudyRecord record = new StudyRecord();
        record.setTaskId(12L);
        record.setContent("完成导数练习");
        record.setProblems("隐函数求导不会");
        record.setImprovementPlan("先复习隐函数求导步骤，再做 5 道基础题。");

        RecordController controller = new RecordController(recordService, taskService);
        Result<StudyRecord> result = controller.add(record, session);

        assertThat(result.isSuccess()).isFalse();
        verify(recordService, never()).saveAndAccumulateInvestment(any(StudyRecord.class));
        verify(taskService, never()).update(any(), any(Wrapper.class));
    }
}
