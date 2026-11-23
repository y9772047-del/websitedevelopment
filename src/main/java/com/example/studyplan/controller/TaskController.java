package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.entity.UserTaskCategory;
import com.example.studyplan.dto.TaskCategorySummary;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

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
    private final StudyRecordService recordService;

    public TaskController(StudyTaskService taskService, StudyPlanService planService) {
        this(taskService, planService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TaskController(StudyTaskService taskService, StudyPlanService planService,
                          StudyRecordService recordService) {
        this.taskService = taskService;
        this.planService = planService;
        this.recordService = recordService;
    }

    @PostMapping
    @Transactional
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
    @Transactional
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

    @GetMapping("/categories/available")
    public Result<List<String>> availableCategories(HttpSession session) {
        return Result.ok(taskService.listAvailableCategories(SessionUtil.currentUserId(session)));
    }

    @GetMapping("/categories/templates")
    public Result<List<java.util.Map<String, Object>>> categoryTemplates() {
        return Result.ok(List.of(
                java.util.Map.of("code", "college", "name", "大学生推荐", "categories", taskService.listTaskCategories()),
                java.util.Map.of("code", "worker", "name", "打工人推荐", "categories",
                        List.of("工作项目", "日常事务", "会议沟通", "职业成长", "健康生活", "其他任务"))));
    }

    @GetMapping("/categories/mine")
    public Result<List<TaskCategorySummary>> myCategories(HttpSession session) {
        return Result.ok(taskService.listMyCategories(SessionUtil.currentUserId(session)));
    }

    @PutMapping("/categories/{id}")
    public Result<Void> renameCategory(@PathVariable Long id, @RequestBody UserTaskCategory category, HttpSession session) {
        try {
            String error = taskService.renameCategory(id, category == null ? null : category.getCategoryName(),
                    SessionUtil.currentUserId(session));
            return error == null ? Result.ok("分类及其任务已更新", null) : Result.fail(error);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            return Result.fail("分类名称已存在");
        }
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id, @RequestParam(required = false) Long replacementId, HttpSession session) {
        String error = taskService.deleteCategory(id, replacementId, SessionUtil.currentUserId(session));
        return error == null ? Result.ok("分类已删除，任务和学习记录已保留", null) : Result.fail(error);
    }

    @PostMapping("/categories")
    public Result<UserTaskCategory> addCategory(@RequestBody UserTaskCategory category, HttpSession session) {
        try {
            String error = taskService.createCategory(category, SessionUtil.currentUserId(session));
            return error == null ? Result.ok("自定义分类已保存", category) : Result.fail(error);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            return Result.fail("分类名称已存在");
        }
    }

    @PostMapping("/quick-capture")
    @Transactional
    public Result<StudyPlan> quickCapture(@RequestBody QuickCaptureRequest request, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        if (request == null || request.getPlan() == null || request.getTasks() == null || request.getTasks().isEmpty()) {
            return Result.fail("快速收集至少需要一个计划和一个任务");
        }
        StudyPlan plan = request.getPlan();
        if (plan.getPlanName() == null || plan.getPlanName().isBlank()) return Result.fail("计划名称不能为空");
        if (plan.getPlanName().length() > 100 || (plan.getGoal() != null && plan.getGoal().length() > 255))
            return Result.fail("计划名称或目标过长");
        if (plan.getStartDate() != null && plan.getEndDate() != null && plan.getEndDate().isBefore(plan.getStartDate()))
            return Result.fail("结束日期不能早于开始日期");
        if (plan.getStatus() == null) plan.setStatus("进行中");
        if (!List.of("未开始", "进行中", "已完成", "已取消").contains(plan.getStatus())) return Result.fail("计划状态无效");
        // Validate the entire batch before the first write. New tasks all belong to this new plan.
        for (QuickCaptureRequest.QuickCaptureTask row : request.getTasks()) {
            if (row == null || row.getTask() == null) return Result.fail("任务数据不能为空");
            StudyTask task = row.getTask();
            String error = prepareTask(task, userId, true);
            if (error != null) return Result.fail(error);
            if (task.getTaskName().length() > 100) return Result.fail("任务名称不能超过 100 个字符");
            if (task.getStatus() == null) task.setStatus("未完成");
            if (!List.of("未完成", "进行中", "已完成", "延期").contains(task.getStatus())) return Result.fail("任务状态无效");
            if (isRepeatTask(task)) return Result.fail("快速收集暂不支持重复任务，请创建后设置");
            StudyRecord record = row.getRecord();
            if (record != null) {
                if (record.getContent() == null || record.getContent().isBlank()) return Result.fail("学习记录内容不能为空");
                if (record.getDuration() == null) record.setDuration(0);
                if (record.getDuration() < 0) return Result.fail("学习时长不能为负数");
                if (record.getCompletionStatus() == null) record.setCompletionStatus("部分完成");
                if (!List.of("已完成", "部分完成", "未完成").contains(record.getCompletionStatus())) return Result.fail("记录完成情况无效");
                if (record.getStudyDate() == null) record.setStudyDate(LocalDate.now());
            }
        }
        plan.setId(null); plan.setUserId(userId);
        plan.setTotalStudyMinutes(0); plan.setTotalStudyRecordCount(0);
        plan.setCreateTime(null); plan.setUpdateTime(null);
        if (!planService.save(plan)) throw new IllegalStateException("保存计划失败");
        for (QuickCaptureRequest.QuickCaptureTask row : request.getTasks()) {
            StudyTask task = row.getTask();
            task.setId(null); task.setPlanId(plan.getId()); task.setCreateTime(null); task.setUpdateTime(null);
            if (!taskService.save(task)) throw new IllegalStateException("保存任务失败");
            StudyRecord record = row.getRecord();
            if (record != null) {
                record.setId(null); record.setUserId(userId); record.setTaskId(task.getId());
                record.setCreateTime(null); record.setUpdateTime(null);
                if (!recordService.saveAndAccumulateInvestment(record)) throw new IllegalStateException("保存记录失败");
            }
        }
        return Result.ok("快速收集成功", plan);
    }

    @GetMapping("/{id}")
    public Result<StudyTask> detail(@PathVariable Long id, HttpSession session) {
        StudyTask task = taskService.getOne(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, id)
                .eq(StudyTask::getUserId, SessionUtil.currentUserId(session)));
        return task == null ? Result.fail("任务不存在") : Result.ok(task);
    }

    @PutMapping("/{id}")
    @Transactional
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
        return prepareTask(task, userId, false);
    }

    private String prepareTask(StudyTask task, Long userId, boolean newPlan) {
        if (task == null) {
            return "任务数据不能为空";
        }
        if (task.getTaskName() == null || task.getTaskName().isBlank()) {
            return "任务名称不能为空";
        }
        if (!newPlan && task.getPlanId() == null) {
            return "学习任务必须关联学习计划";
        }
        boolean planExists = newPlan || planService.exists(new LambdaQueryWrapper<StudyPlan>()
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
        if (!taskService.isValidCategory(task.getCategory(), userId)) {
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
