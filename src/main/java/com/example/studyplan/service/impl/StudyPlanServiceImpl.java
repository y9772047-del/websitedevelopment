package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.mapper.StudyPlanMapper;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudyPlanServiceImpl extends ServiceImpl<StudyPlanMapper, StudyPlan> implements StudyPlanService {
    private final StudyTaskService taskService;
    private final StudyRecordService recordService;

    public StudyPlanServiceImpl(StudyTaskService taskService, StudyRecordService recordService) {
        this.taskService = taskService;
        this.recordService = recordService;
    }

    @Override
    @Transactional
    public boolean removePlanWithTasksAndRecords(Long id, Long userId) {
        if (id == null || userId == null) {
            return false;
        }
        StudyPlan plan = baseMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, id)
                .eq(StudyPlan::getUserId, userId));
        if (plan == null) {
            return false;
        }

        List<StudyTask> tasks = taskService.list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getPlanId, id)
                .eq(StudyTask::getUserId, userId));
        List<Long> taskIds = tasks.stream()
                .map(StudyTask::getId)
                .toList();
        if (!taskIds.isEmpty()) {
            recordService.remove(new LambdaQueryWrapper<StudyRecord>()
                    .in(StudyRecord::getTaskId, taskIds)
                    .eq(StudyRecord::getUserId, userId));
            taskService.remove(new LambdaQueryWrapper<StudyTask>()
                    .eq(StudyTask::getPlanId, id)
                    .eq(StudyTask::getUserId, userId));
        }

        return baseMapper.delete(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, id)
                .eq(StudyPlan::getUserId, userId)) > 0;
    }
}