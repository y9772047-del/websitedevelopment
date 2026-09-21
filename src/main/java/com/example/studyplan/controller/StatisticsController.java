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
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final StudyPlanService planService;
    private final StudyTaskService taskService;
    private final StudyRecordService recordService;

    public StatisticsController(StudyPlanService planService, StudyTaskService taskService, StudyRecordService recordService) {
        this.planService = planService;
        this.taskService = taskService;
        this.recordService = recordService;
    }

    @GetMapping
    public Result<Map<String, Object>> statistics(HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        List<StudyTask> tasks = taskService.list(new LambdaQueryWrapper<StudyTask>().eq(StudyTask::getUserId, userId));
        long total = tasks.size();
        long completed = tasks.stream().filter(task -> "已完成".equals(task.getStatus())).count();
        long overdue = tasks.stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isBefore(LocalDate.now()))
                .filter(task -> !"已完成".equals(task.getStatus()))
                .count();
        int totalDuration = recordService.list(new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getUserId, userId))
                .stream()
                .mapToInt(record -> record.getDuration() == null ? 0 : record.getDuration())
                .sum();

        Map<String, Object> data = new HashMap<>();
        data.put("planCount", planService.count(new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, userId)));
        data.put("taskCount", total);
        data.put("completedTaskCount", completed);
        data.put("overdueTaskCount", overdue);
        data.put("completionRate", total == 0 ? 0 : Math.round(completed * 10000.0 / total) / 100.0);
        data.put("totalDuration", totalDuration);
        return Result.ok(data);
    }
}
