package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.mapper.StudyPlanMapper;
import com.example.studyplan.mapper.StudyRecordMapper;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyRecordServiceImpl extends ServiceImpl<StudyRecordMapper, StudyRecord> implements StudyRecordService {
    private final StudyTaskService taskService;
    private final StudyPlanMapper planMapper;

    public StudyRecordServiceImpl(StudyTaskService taskService, StudyPlanMapper planMapper) {
        this.taskService = taskService;
        this.planMapper = planMapper;
    }

    @Override
    @Transactional
    public boolean saveAndAccumulateInvestment(StudyRecord record) {
        boolean saved = save(record);
        if (saved) {
            applyInvestmentDelta(record, 1);
        }
        return saved;
    }

    @Override
    @Transactional
    public boolean updateAndRefreshInvestment(StudyRecord record, Long userId) {
        StudyRecord oldRecord = getUserRecord(record.getId(), userId);
        if (oldRecord == null) {
            return false;
        }
        boolean updated = update(new UpdateWrapper<StudyRecord>()
                .set("task_id", record.getTaskId())
                .set("study_date", record.getStudyDate())
                .set("duration", record.getDuration())
                .set("content", record.getContent())
                .set("completion_status", record.getCompletionStatus())
                .set("problems", record.getProblems())
                .set("improvement_plan", record.getImprovementPlan())
                .eq("id", record.getId())
                .eq("user_id", userId));
        if (updated) {
            applyInvestmentDelta(oldRecord, -1);
            applyInvestmentDelta(record, 1);
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean removeAndDeductInvestment(Long id, Long userId) {
        StudyRecord oldRecord = getUserRecord(id, userId);
        if (oldRecord == null) {
            return false;
        }
        boolean removed = remove(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getId, id)
                .eq(StudyRecord::getUserId, userId));
        if (removed) {
            applyInvestmentDelta(oldRecord, -1);
        }
        return removed;
    }

    private StudyRecord getUserRecord(Long id, Long userId) {
        if (id == null || userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getId, id)
                .eq(StudyRecord::getUserId, userId));
    }

    private void applyInvestmentDelta(StudyRecord record, int recordCountDelta) {
        if (record == null || record.getTaskId() == null) {
            return;
        }
        StudyTask task = taskService.getById(record.getTaskId());
        if (task == null || task.getPlanId() == null) {
            return;
        }
        int minutes = Math.max(record.getDuration() == null ? 0 : record.getDuration(), 0);
        planMapper.incrementInvestment(task.getPlanId(), minutes * recordCountDelta, recordCountDelta);
    }
}
