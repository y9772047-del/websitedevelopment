package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private static final Set<String> VALID_PRIORITIES = Set.of("Q0", "Q1", "Q2", "Q3");
    private static final Set<String> VALID_REPEAT_TYPES = Set.of("NONE", "DAILY", "WEEKLY", "WORKDAY");
    private static final String REPEAT_NONE = "NONE";
    private final StudyTaskService taskService;
    private final StudyPlanService planService;

    public TaskController(StudyTaskService taskService, StudyPlanService planService) {
        this.taskService = taskService;
        this.planService = planService;
    }

    @PostMapping
    public Result<StudyTask> add(@RequestBody StudyTask task, HttpSession session) {
        String error = prepareTask(task, SessionUtil.currentUserId(session));
        if (error != null) {
            return Result.fail(error);
        }
        task.setId(null);
        if (isRepeatTask(task)) {
            List<StudyTask> repeatTasks = buildRepeatTasks(task);
            taskService.saveBatch(repeatTasks);
            return Result.ok("保存重复任务成功", repeatTasks.get(0));
        }
        taskService.save(task);
        return Result.ok("保存任务成功", task);
    }

    @PostMapping("/batch")
    public Result<List<StudyTask>> batchAdd(@RequestBody List<StudyTask> tasks, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        if (tasks == null || tasks.isEmpty()) {
            return Result.fail("批量任务不能为空");
        }
        List<StudyTask> preparedTasks = new ArrayList<>();
        for (StudyTask task : tasks) {
            String error = prepareTask(task, userId);
            if (error != null) {
                return Result.fail(error);
            }
            task.setId(null);
            preparedTasks.add(task);
        }
        taskService.saveBatch(preparedTasks);
        return Result.ok("批量保存任务成功", preparedTasks);
    }

    @GetMapping
    public Result<List<StudyTask>> list(@RequestParam(required = false) Long planId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String priority,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        HttpSession session) {
        LambdaQueryWrapper<StudyTask> wrapper = new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getUserId, SessionUtil.currentUserId(session));
        if (planId != null) {
            wrapper.eq(StudyTask::getPlanId, planId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(StudyTask::getStatus, status);
        }
        if (priority != null && !priority.isBlank()) {
            if (!VALID_PRIORITIES.contains(priority)) {
                return Result.fail("优先级只能是 Q0、Q1、Q2、Q3");
            }
            wrapper.eq(StudyTask::getPriority, priority);
        }
        if (date != null) {
            wrapper.eq(StudyTask::getDeadline, date);
        }
        wrapper.orderByAsc(StudyTask::getPriority)
                .orderByAsc(StudyTask::getDeadline)
                .orderByDesc(StudyTask::getCreateTime);
        return Result.ok(taskService.list(wrapper));
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(taskService.listTaskCategories());
    }

    @GetMapping("/{id}")
    public Result<StudyTask> detail(@PathVariable Long id, HttpSession session) {
        StudyTask task = taskService.getOne(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, id)
                .eq(StudyTask::getUserId, SessionUtil.currentUserId(session)));
        return task == null ? Result.fail("任务不存在") : Result.ok(task);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody StudyTask task, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        String error = prepareTask(task, userId);
        if (error != null) {
            return Result.fail(error);
        }
        task.setId(id);
        boolean updated = taskService.update(task, new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, id)
                .eq(StudyTask::getUserId, userId));
        return updated ? Result.ok("修改任务成功", null) : Result.fail("修改任务失败");
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StudyTask task, HttpSession session) {
        StudyTask updateTask = new StudyTask();
        updateTask.setStatus(task.getStatus());
        boolean updated = taskService.update(updateTask, new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, id)
                .eq(StudyTask::getUserId, SessionUtil.currentUserId(session)));
        return updated ? Result.ok("修改任务状态成功", null) : Result.fail("修改任务状态失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        boolean removed = taskService.removeTaskWithRecords(id, SessionUtil.currentUserId(session));
        return removed ? Result.ok("\u5220\u9664\u4efb\u52a1\u6210\u529f", null) : Result.fail("\u5220\u9664\u4efb\u52a1\u5931\u8d25");
    }

    private String prepareTask(StudyTask task, Long userId) {
        if (task == null) {
            return "任务数据不能为空";
        }
        if (task.getTaskName() == null || task.getTaskName().isBlank()) {
            return "任务名称不能为空";
        }
        if (task.getPlanId() == null) {
            return "学习任务必须关联学习计划";
        }
        boolean planExists = planService.exists(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, task.getPlanId())
                .eq(StudyPlan::getUserId, userId));
        if (!planExists) {
            return "所属学习计划不存在";
        }
        if (task.getPriority() == null || task.getPriority().isBlank()) {
            task.setPriority("Q2");
        }
        if (!VALID_PRIORITIES.contains(task.getPriority())) {
            return "优先级只能是 Q0、Q1、Q2、Q3";
        }
        if (task.getCategory() == null || task.getCategory().isBlank()) {
            task.setCategory(StudyTaskService.DEFAULT_CATEGORY);
        }
        if (!taskService.isValidCategory(task.getCategory())) {
            return "任务分类必须是固定选项之一";
        }
        if (task.getReminderEnabled() == null) {
            task.setReminderEnabled(true);
        }
        if (task.getReminderDaysBefore() == null) {
            task.setReminderDaysBefore(3);
        }
        if (task.getReminderDaysBefore() < 0 || task.getReminderDaysBefore() > 30) {
            return "提前提醒天数必须在 0 到 30 之间";
        }
        if (task.getRepeatType() == null || task.getRepeatType().isBlank()) {
            task.setRepeatType(REPEAT_NONE);
        } else {
            task.setRepeatType(task.getRepeatType().trim().toUpperCase());
        }
        if (!VALID_REPEAT_TYPES.contains(task.getRepeatType())) {
            return "重复规则只能是 NONE、DAILY、WEEKLY、WORKDAY";
        }
        if (isRepeatTask(task)) {
            if (task.getDeadline() == null) {
                return "重复任务必须填写开始截止日期";
            }
            if (task.getRepeatEndDate() == null) {
                return "重复任务必须填写重复结束日期";
            }
            if (task.getRepeatEndDate().isBefore(task.getDeadline())) {
                return "重复结束日期不能早于开始截止日期";
            }
        } else {
            task.setRepeatEndDate(null);
            task.setRepeatGroupId(null);
        }
        task.setUserId(userId);
        return null;
    }

    private boolean isRepeatTask(StudyTask task) {
        return task != null && task.getRepeatType() != null && !REPEAT_NONE.equals(task.getRepeatType());
    }

    private List<StudyTask> buildRepeatTasks(StudyTask source) {
        List<StudyTask> tasks = new ArrayList<>();
        String groupId = UUID.randomUUID().toString();
        LocalDate date = source.getDeadline();
        while (!date.isAfter(source.getRepeatEndDate())) {
            if (shouldCreateOnDate(source.getRepeatType(), source.getDeadline(), date)) {
                tasks.add(copyRepeatTask(source, date, groupId));
            }
            date = date.plusDays(1);
        }
        return tasks;
    }

    private boolean shouldCreateOnDate(String repeatType, LocalDate startDate, LocalDate date) {
        if ("DAILY".equals(repeatType)) {
            return true;
        }
        if ("WEEKLY".equals(repeatType)) {
            return startDate.getDayOfWeek() == date.getDayOfWeek();
        }
        if ("WORKDAY".equals(repeatType)) {
            DayOfWeek day = date.getDayOfWeek();
            return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
        }
        return false;
    }

    private StudyTask copyRepeatTask(StudyTask source, LocalDate deadline, String groupId) {
        StudyTask copy = new StudyTask();
        copy.setUserId(source.getUserId());
        copy.setPlanId(source.getPlanId());
        copy.setTaskName(source.getTaskName());
        copy.setDescription(source.getDescription());
        copy.setCategory(source.getCategory());
        copy.setPriority(source.getPriority());
        copy.setDeadline(deadline);
        copy.setStatus(source.getStatus());
        copy.setRepeatType(source.getRepeatType());
        copy.setRepeatEndDate(source.getRepeatEndDate());
        copy.setRepeatGroupId(groupId);
        copy.setReminderEnabled(source.getReminderEnabled());
        copy.setReminderDaysBefore(source.getReminderDaysBefore());
        return copy;
    }
}