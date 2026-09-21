package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final StudyTaskService taskService;
    private final StudyPlanService planService;
    private final StudyRecordService recordService;

    public DashboardController(StudyTaskService taskService, StudyPlanService planService, StudyRecordService recordService) {
        this.taskService = taskService;
        this.planService = planService;
        this.recordService = recordService;
    }

    @GetMapping
    public Result<Map<String, Object>> dashboard(HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        List<StudyTask> allTasks = taskService.list(new LambdaQueryWrapper<StudyTask>().eq(StudyTask::getUserId, userId));
        long completed = allTasks.stream().filter(task -> "已完成".equals(task.getStatus())).count();
        long overdue = allTasks.stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(LocalDate.now()))
                .filter(task -> !"已完成".equals(task.getStatus()))
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("planCount", planService.count(new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, userId)));
        data.put("taskCount", allTasks.size());
        data.put("completedTaskCount", completed);
        data.put("overdueTaskCount", overdue);
        data.put("completionRate", allTasks.isEmpty() ? 0 : Math.round(completed * 10000.0 / allTasks.size()) / 100.0);
        data.put("recentTasks", taskService.list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getUserId, userId)
                .orderByDesc(StudyTask::getCreateTime)
                .last("LIMIT 6")));
        data.put("recentRecords", recordService.list(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .orderByDesc(StudyRecord::getStudyDate)
                .last("LIMIT 6")));
        return Result.ok(data);
    }
}
