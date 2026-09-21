package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.entity.User;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    @Test
    void categoriesComeFromTaskService() {
        StudyTaskService taskService = mock(StudyTaskService.class);
        StudyPlanService planService = mock(StudyPlanService.class);
        when(taskService.listTaskCategories()).thenReturn(List.of("课程进度", "其他任务"));

        TaskController controller = new TaskController(taskService, planService);
        Result<List<String>> result = controller.categories();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("课程进度", "其他任务");
    }

    @Test
    void addRejectsTaskWhenCategoryIsNotFixedOption() {
        StudyTaskService taskService = mock(StudyTaskService.class);
        StudyPlanService planService = mock(StudyPlanService.class);
        HttpSession session = mockStudentSession();
        when(planService.exists(any(Wrapper.class))).thenReturn(true);
        when(taskService.isValidCategory("随便写的分类")).thenReturn(false);

        StudyTask task = new StudyTask();
        task.setPlanId(3L);
        task.setTaskName("准备期末复习");
        task.setCategory("随便写的分类");
        task.setPriority("Q1");

        TaskController controller = new TaskController(taskService, planService);
        Result<StudyTask> result = controller.add(task, session);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("任务分类必须是固定选项之一");
        verify(taskService, never()).save(any(StudyTask.class));
    }

    @Test
    void addDefaultsBlankCategoryToOtherTaskWhenCategoryExists() {
        StudyTaskService taskService = mock(StudyTaskService.class);
        StudyPlanService planService = mock(StudyPlanService.class);
        HttpSession session = mockStudentSession();
        when(planService.exists(any(Wrapper.class))).thenReturn(true);
        when(taskService.isValidCategory(StudyTaskService.DEFAULT_CATEGORY)).thenReturn(true);

        StudyTask task = new StudyTask();
        task.setPlanId(3L);
        task.setTaskName("整理临时学习事项");
        task.setPriority("Q2");

        TaskController controller = new TaskController(taskService, planService);
        Result<StudyTask> result = controller.add(task, session);

        assertThat(result.isSuccess()).isTrue();
        assertThat(task.getCategory()).isEqualTo(StudyTaskService.DEFAULT_CATEGORY);
        verify(taskService).save(task);
    }


    @Test
    void addCreatesWeekdayRepeatTasksUntilRepeatEndDate() {
        StudyTaskService taskService = mock(StudyTaskService.class);
        StudyPlanService planService = mock(StudyPlanService.class);
        HttpSession session = mockStudentSession();
        when(planService.exists(any(Wrapper.class))).thenReturn(true);
        when(taskService.isValidCategory(StudyTaskService.DEFAULT_CATEGORY)).thenReturn(true);

        StudyTask task = new StudyTask();
        task.setPlanId(3L);
        task.setTaskName("英语晨读");
        task.setCategory(StudyTaskService.DEFAULT_CATEGORY);
        task.setPriority("Q2");
        task.setDeadline(LocalDate.of(2026, 7, 6));
        task.setRepeatType("WORKDAY");
        task.setRepeatEndDate(LocalDate.of(2026, 7, 10));
        task.setReminderEnabled(true);
        task.setReminderDaysBefore(2);

        TaskController controller = new TaskController(taskService, planService);
        Result<StudyTask> result = controller.add(task, session);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<List<StudyTask>> captor = forClass(List.class);
        verify(taskService).saveBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(StudyTask::getDeadline)
                .containsExactly(
                        LocalDate.of(2026, 7, 6),
                        LocalDate.of(2026, 7, 7),
                        LocalDate.of(2026, 7, 8),
                        LocalDate.of(2026, 7, 9),
                        LocalDate.of(2026, 7, 10));
        assertThat(captor.getValue()).allSatisfy(row -> {
            assertThat(row.getRepeatType()).isEqualTo("WORKDAY");
            assertThat(row.getRepeatGroupId()).isNotBlank();
            assertThat(row.getUserId()).isEqualTo(9L);
            assertThat(row.getReminderEnabled()).isTrue();
            assertThat(row.getReminderDaysBefore()).isEqualTo(2);
        });
    }

    private HttpSession mockStudentSession() {
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(9L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);
        return session;
    }
}
