package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {
    private final StudyTaskService taskService;

    public CalendarController(StudyTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Result<List<StudyTask>> month(@RequestParam int year, @RequestParam int month, HttpSession session) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<StudyTask> tasks = taskService.list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getUserId, SessionUtil.currentUserId(session))
                .between(StudyTask::getDeadline, start, end)
                .orderByAsc(StudyTask::getDeadline));
        return Result.ok(tasks);
    }
}
