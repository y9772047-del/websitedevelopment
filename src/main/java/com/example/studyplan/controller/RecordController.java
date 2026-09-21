package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {
    private final StudyRecordService recordService;
    private final StudyTaskService taskService;

    public RecordController(StudyRecordService recordService, StudyTaskService taskService) {
        this.recordService = recordService;
        this.taskService = taskService;
    }

    @PostMapping
    @Transactional
    public Result<StudyRecord> add(@RequestBody StudyRecord record, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        String error = validateRecordTask(record, userId);
        if (error != null) {
            return Result.fail(error);
        }
        record.setUserId(userId);
        if (record.getStudyDate() == null) {
            record.setStudyDate(LocalDate.now());
        }
        recordService.saveAndAccumulateInvestment(record);
        return Result.ok(record);
    }

    @GetMapping
    public Result<List<StudyRecord>> list(HttpSession session) {
        List<StudyRecord> records = recordService.list(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, SessionUtil.currentUserId(session))
                .orderByDesc(StudyRecord::getStudyDate)
                .orderByDesc(StudyRecord::getCreateTime));
        return Result.ok(records);
    }

    @GetMapping("/task/{taskId}")
    public Result<List<StudyRecord>> taskTimeline(@PathVariable Long taskId, HttpSession session) {
        List<StudyRecord> records = recordService.list(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, SessionUtil.currentUserId(session))
                .eq(StudyRecord::getTaskId, taskId)
                .orderByDesc(StudyRecord::getStudyDate));
        return Result.ok(records);
    }

    @GetMapping("/recent")
    public Result<List<StudyRecord>> recent(HttpSession session) {
        List<StudyRecord> records = recordService.list(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, SessionUtil.currentUserId(session))
                .orderByDesc(StudyRecord::getStudyDate)
                .last("LIMIT 8"));
        return Result.ok(records);
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody StudyRecord record, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        String error = validateRecordTask(record, userId);
        if (error != null) {
            return Result.fail(error);
        }
        record.setId(id);
        record.setUserId(userId);
        boolean updated = recordService.updateAndRefreshInvestment(record, userId);
        return updated ? Result.ok(null) : Result.fail("修改失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        boolean removed = recordService.removeAndDeductInvestment(id, SessionUtil.currentUserId(session));
        return removed ? Result.ok(null) : Result.fail("删除失败");
    }

    private String validateRecordTask(StudyRecord record, Long userId) {
        if (record == null) {
            return "学习记录不能为空";
        }
        if (record.getTaskId() == null) {
            return "学习记录必须关联学习任务";
        }
        boolean taskExists = taskService.exists(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, record.getTaskId())
                .eq(StudyTask::getUserId, userId));
        return taskExists ? null : "学习记录关联的学习任务不存在";
    }
}
