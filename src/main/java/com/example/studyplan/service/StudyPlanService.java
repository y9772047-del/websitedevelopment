package com.example.studyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyplan.entity.StudyPlan;

public interface StudyPlanService extends IService<StudyPlan> {
    boolean removePlanWithTasksAndRecords(Long id, Long userId);
}