package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.Feedback;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.RbacMapper;
import com.example.studyplan.service.FeedbackService;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyTaskService;
import com.example.studyplan.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('admin:access')")
public class AdminController {
    private final UserService userService;
    private final StudyTaskService taskService;
    private final StudyPlanService planService;
    private final FeedbackService feedbackService;
    private final RbacMapper rbacMapper;

    public AdminController(UserService userService,
                           StudyTaskService taskService,
                           StudyPlanService planService,
                           FeedbackService feedbackService,
                           RbacMapper rbacMapper) {
        this.userService = userService;
        this.taskService = taskService;
        this.planService = planService;
        this.feedbackService = feedbackService;
        this.rbacMapper = rbacMapper;
    }

    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> dashboardStats() {
        List<StudyTask> tasks = taskService.list();
        long finished = tasks.stream().filter(task -> "已完成".equals(task.getStatus())).count();
        long overdue = tasks.stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(LocalDate.now()))
                .filter(task -> !"已完成".equals(task.getStatus()))
                .count();
        long todayNew = tasks.stream()
                .filter(task -> task.getCreateTime() != null)
                .filter(task -> task.getCreateTime().toLocalDate().equals(LocalDate.now()))
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userService.count());
        data.put("taskCount", tasks.size());
        data.put("finishedTaskCount", finished);
        data.put("unfinishedTaskCount", tasks.size() - finished);
        data.put("overdueTaskCount", overdue);
        data.put("todayNewTaskCount", todayNew);
        data.put("feedbackCount", feedbackService.count());
        data.put("pendingFeedbackCount", feedbackService.count(new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, "未处理")));
        data.put("recentUsers", safeUsers(userService.list(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime)
                .last("LIMIT 5"))));
        data.put("recentFeedbacks", feedbackService.list(new LambdaQueryWrapper<Feedback>()
                .orderByDesc(Feedback::getCreateTime)
                .last("LIMIT 5")));
        return Result.ok(data);
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users(@RequestParam(value = "username", required = false) String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(User::getUsername, username);
        }
        wrapper.orderByDesc(User::getCreateTime);
        List<Map<String, Object>> rows = userService.list(wrapper).stream()
                .map(this::userRow)
                .collect(Collectors.toList());
        return Result.ok(rows);
    }

    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> userDetail(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        List<StudyTask> tasks = taskService.list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getUserId, id)
                .orderByDesc(StudyTask::getCreateTime));
        long completed = tasks.stream().filter(task -> "已完成".equals(task.getStatus())).count();
        long overdue = tasks.stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(LocalDate.now()))
                .filter(task -> !"已完成".equals(task.getStatus()))
                .count();
        Map<String, Object> data = userRow(user);
        data.put("completedTaskCount", completed);
        data.put("unfinishedTaskCount", tasks.size() - completed);
        data.put("overdueTaskCount", overdue);
        data.put("recentTasks", tasks.stream().limit(8).collect(Collectors.toList()));
        return Result.ok(data);
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('admin:access') and hasAuthority('users:manage')")
    public Result<Void> updateUserStatus(@PathVariable("id") Long id, @RequestBody User update, HttpSession session) {
        User target = userService.getById(id);
        User current = SessionUtil.currentUser(session);
        if (target == null) {
            return Result.fail("用户不存在");
        }
        if (rbacMapper.rolesForUser(id).contains("admin")) {
            return Result.fail("不能禁用管理员账号");
        }
        if (current != null && current.getId().equals(id)) {
            return Result.fail("不能禁用自己");
        }
        Integer status = update.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("用户状态只能是 0 或 1");
        }
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.ok(status == 1 ? "启用成功" : "禁用成功", null);
    }

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks(@RequestParam(value = "username", required = false) String username,
                                                   @RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "priority", required = false) String priority,
                                                   @RequestParam(value = "status", required = false) String status,
                                                   @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                   @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LambdaQueryWrapper<StudyTask> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(StudyTask::getTaskName, keyword);
        }
        if (priority != null && !priority.isBlank()) {
            wrapper.eq(StudyTask::getPriority, priority);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(StudyTask::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(StudyTask::getDeadline, startDate);
        }
        if (endDate != null) {
            wrapper.le(StudyTask::getDeadline, endDate);
        }
        wrapper.orderByDesc(StudyTask::getCreateTime);
        List<Map<String, Object>> rows = taskService.list(wrapper).stream()
                .map(this::taskRow)
                .filter(row -> username == null || username.isBlank() || String.valueOf(row.get("username")).contains(username))
                .collect(Collectors.toList());
        return Result.ok(rows);
    }

    @GetMapping("/tasks/{id}")
    public Result<Map<String, Object>> taskDetail(@PathVariable("id") Long id) {
        StudyTask task = taskService.getById(id);
        return task == null ? Result.fail("任务不存在") : Result.ok(taskRow(task));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('admin:access') and hasAuthority('tasks:manage')")
    public Result<Void> deleteTask(@PathVariable("id") Long id) {
        StudyTask task = taskService.getById(id);
        if (task == null) {
            return Result.fail("\u4efb\u52a1\u4e0d\u5b58\u5728");
        }
        boolean removed = taskService.removeTaskWithRecords(id, task.getUserId());
        return removed ? Result.ok("\u5220\u9664\u4efb\u52a1\u6210\u529f", null) : Result.fail("\u5220\u9664\u4efb\u52a1\u5931\u8d25");
    }

    @GetMapping("/feedbacks")
    public Result<List<Map<String, Object>>> feedbacks(@RequestParam(value = "username", required = false) String username,
                                                       @RequestParam(value = "status", required = false) String status,
                                                       @RequestParam(value = "keyword", required = false) String keyword) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Feedback::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Feedback::getContent, keyword);
        }
        wrapper.orderByDesc(Feedback::getCreateTime);
        List<Map<String, Object>> rows = feedbackService.list(wrapper).stream()
                .map(this::feedbackRow)
                .filter(row -> username == null || username.isBlank() || String.valueOf(row.get("username")).contains(username))
                .collect(Collectors.toList());
        return Result.ok(rows);
    }

    @GetMapping("/feedbacks/{id}")
    public Result<Map<String, Object>> feedbackDetail(@PathVariable("id") Long id) {
        Feedback feedback = feedbackService.getById(id);
        return feedback == null ? Result.fail("反馈不存在") : Result.ok(feedbackRow(feedback));
    }

    @PutMapping("/feedbacks/{id}")
    @PreAuthorize("hasAuthority('admin:access') and hasAuthority('feedback:manage')")
    public Result<Void> updateFeedback(@PathVariable("id") Long id, @RequestBody Feedback update) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.fail("反馈不存在");
        }
        if (!List.of("未处理", "已处理", "已忽略").contains(update.getStatus())) {
            return Result.fail("反馈状态不正确");
        }
        Feedback saved = new Feedback();
        saved.setId(id);
        saved.setStatus(update.getStatus());
        saved.setReply(update.getReply());
        feedbackService.updateById(saved);
        return Result.ok("反馈处理成功", null);
    }

    private Map<String, Object> userRow(User user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("realName", user.getRealName());
        row.put("role", user.getRole());
        row.put("status", user.getStatus());
        row.put("createTime", user.getCreateTime());
        row.put("taskCount", taskService.count(new LambdaQueryWrapper<StudyTask>().eq(StudyTask::getUserId, user.getId())));
        return row;
    }

    private List<User> safeUsers(List<User> users) {
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    private Map<String, Object> taskRow(StudyTask task) {
        User user = userService.getById(task.getUserId());
        StudyPlan plan = task.getPlanId() == null ? null : planService.getById(task.getPlanId());
        Map<String, Object> row = new HashMap<>();
        row.put("id", task.getId());
        row.put("taskName", task.getTaskName());
        row.put("description", task.getDescription());
        row.put("userId", task.getUserId());
        row.put("username", user == null ? "未知用户" : user.getUsername());
        row.put("planId", task.getPlanId());
        row.put("planName", plan == null ? "未关联" : plan.getPlanName());
        row.put("category", task.getCategory());
        row.put("priority", task.getPriority());
        row.put("deadline", task.getDeadline());
        row.put("status", task.getStatus());
        row.put("createTime", task.getCreateTime());
        row.put("updateTime", task.getUpdateTime());
        return row;
    }

    private Map<String, Object> feedbackRow(Feedback feedback) {
        User user = userService.getById(feedback.getUserId());
        Map<String, Object> row = new HashMap<>();
        row.put("id", feedback.getId());
        row.put("userId", feedback.getUserId());
        row.put("username", user == null ? "未知用户" : user.getUsername());
        row.put("content", feedback.getContent());
        row.put("status", feedback.getStatus());
        row.put("reply", feedback.getReply());
        row.put("createTime", feedback.getCreateTime());
        row.put("updateTime", feedback.getUpdateTime());
        return row;
    }
}
