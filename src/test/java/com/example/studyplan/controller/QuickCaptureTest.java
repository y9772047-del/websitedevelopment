package com.example.studyplan.controller;

import com.example.studyplan.entity.*;
import com.example.studyplan.service.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class QuickCaptureTest {
    @Test
    void persistenceFailureRollsBackTheWholeTransaction() {
        StudyTaskService tasks = mock(StudyTaskService.class);
        StudyPlanService plans = mock(StudyPlanService.class);
        StudyRecordService records = mock(StudyRecordService.class);
        when(tasks.isValidCategory(any(), any())).thenReturn(true);
        java.util.concurrent.atomic.AtomicInteger storedPlans = new java.util.concurrent.atomic.AtomicInteger();
        when(plans.save(any())).thenAnswer(call -> {
            StudyPlan plan = call.getArgument(0); plan.setId(10L); storedPlans.incrementAndGet(); return true;
        });
        when(tasks.save(any(StudyTask.class))).thenReturn(false);
        var manager = new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition def) {}
            protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {}
            protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) { storedPlans.set(0); }
        };
        var factory = new org.springframework.aop.framework.ProxyFactory(new TaskController(tasks, plans, records));
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        TaskController controller = (TaskController) factory.getProxy();
        StudyPlan plan = new StudyPlan(); plan.setPlanName("复习");
        StudyTask task = new StudyTask(); task.setTaskName("章节");
        QuickCaptureRequest.QuickCaptureTask row = new QuickCaptureRequest.QuickCaptureTask(); row.setTask(task);
        QuickCaptureRequest request = new QuickCaptureRequest(); request.setPlan(plan); request.setTasks(List.of(row));
        HttpSession session = mock(HttpSession.class); User user = new User(); user.setId(9L);
        when(session.getAttribute(com.example.studyplan.common.SessionUtil.LOGIN_USER)).thenReturn(user);
        assertThatThrownBy(() -> controller.quickCapture(request, session)).isInstanceOf(IllegalStateException.class);
        assertThat(storedPlans.get()).isZero();
    }

    @Test
    void captureAssignsNewIdsOwnershipAndRecordLinks() {
        StudyTaskService tasks = mock(StudyTaskService.class);
        StudyPlanService plans = mock(StudyPlanService.class);
        StudyRecordService records = mock(StudyRecordService.class);
        when(tasks.isValidCategory(any(), any())).thenReturn(true);
        when(plans.save(any())).thenAnswer(call -> { ((StudyPlan) call.getArgument(0)).setId(10L); return true; });
        when(tasks.save(any(StudyTask.class))).thenAnswer(call -> { ((StudyTask) call.getArgument(0)).setId(20L); return true; });
        when(records.saveAndAccumulateInvestment(any())).thenReturn(true);
        StudyPlan plan = new StudyPlan(); plan.setPlanName("复习"); plan.setUserId(999L); plan.setTotalStudyMinutes(999);
        StudyTask task = new StudyTask(); task.setTaskName("章节"); task.setUserId(999L); task.setPlanId(999L);
        StudyRecord record = new StudyRecord(); record.setId(999L); record.setUserId(999L); record.setContent("已复习");
        QuickCaptureRequest.QuickCaptureTask row = new QuickCaptureRequest.QuickCaptureTask(); row.setTask(task); row.setRecord(record);
        QuickCaptureRequest request = new QuickCaptureRequest(); request.setPlan(plan); request.setTasks(List.of(row));
        HttpSession session = mock(HttpSession.class); User user = new User(); user.setId(9L);
        when(session.getAttribute(com.example.studyplan.common.SessionUtil.LOGIN_USER)).thenReturn(user);
        assertThat(new TaskController(tasks, plans, records).quickCapture(request, session).isSuccess()).isTrue();
        assertThat(plan.getUserId()).isEqualTo(9L); assertThat(plan.getTotalStudyMinutes()).isZero();
        assertThat(task.getPlanId()).isEqualTo(10L); assertThat(task.getUserId()).isEqualTo(9L);
        assertThat(record.getTaskId()).isEqualTo(20L); assertThat(record.getUserId()).isEqualTo(9L);
        assertThat(record.getId()).isNull();
    }

    @Test
    void invalidLaterRowDoesNotSaveAnyPartOfTheCapture() {
        StudyTaskService tasks = mock(StudyTaskService.class);
        StudyPlanService plans = mock(StudyPlanService.class);
        StudyRecordService records = mock(StudyRecordService.class);
        when(plans.save(any())).thenReturn(true);
        when(plans.exists(any())).thenReturn(true);
        when(tasks.isValidCategory(any(), any())).thenReturn(true);
        StudyPlan plan = new StudyPlan(); plan.setPlanName("复习");
        StudyTask task = new StudyTask(); task.setTaskName("章节一");
        QuickCaptureRequest.QuickCaptureTask first = new QuickCaptureRequest.QuickCaptureTask(); first.setTask(task);
        QuickCaptureRequest.QuickCaptureTask second = new QuickCaptureRequest.QuickCaptureTask();
        QuickCaptureRequest request = new QuickCaptureRequest(); request.setPlan(plan); request.setTasks(List.of(first, second));
        HttpSession session = mock(HttpSession.class);
        User user = new User(); user.setId(9L);
        when(session.getAttribute(com.example.studyplan.common.SessionUtil.LOGIN_USER)).thenReturn(user);
        var result = new TaskController(tasks, plans, records).quickCapture(request, session);
        assertThat(result.isSuccess()).isFalse();
        verify(plans, never()).save(any());
        verify(tasks, never()).save(any(StudyTask.class));
    }
}
