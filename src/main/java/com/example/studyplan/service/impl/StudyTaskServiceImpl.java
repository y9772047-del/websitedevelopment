package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTaskCategory;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.mapper.StudyPlanMapper;
import com.example.studyplan.mapper.StudyRecordMapper;
import com.example.studyplan.mapper.StudyTaskCategoryMapper;
import com.example.studyplan.mapper.StudyTaskMapper;
import com.example.studyplan.service.StudyTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudyTaskServiceImpl extends ServiceImpl<StudyTaskMapper, StudyTask> implements StudyTaskService {
    private final StudyTaskCategoryMapper taskCategoryMapper;
    private final StudyRecordMapper recordMapper;
    private final StudyPlanMapper planMapper;

    public StudyTaskServiceImpl(StudyTaskCategoryMapper taskCategoryMapper,
                                StudyRecordMapper recordMapper,
                                StudyPlanMapper planMapper) {
        this.taskCategoryMapper = taskCategoryMapper;
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
    }

    @Override
    public List<String> listTaskCategories() {
        return taskCategoryMapper.selectList(new LambdaQueryWrapper<StudyTaskCategory>()
                        .orderByAsc(StudyTaskCategory::getSortOrder))
                .stream()
                .map(StudyTaskCategory::getCategoryName)
                .toList();
    }

    @Override
    public boolean isValidCategory(String category) {
        return category != null && listTaskCategories().contains(category);
    }

    @Override
    public int normalizeLegacyCategories() {
        List<String> categories = listTaskCategories();
        if (categories.isEmpty()) {
            return 0;
        }
        return baseMapper.update(null, new UpdateWrapper<StudyTask>()
                .set("category", DEFAULT_CATEGORY)
                .and(wrapper -> wrapper
                        .isNull("category")
                        .or()
                        .eq("category", "")
                        .or()
                        .apply("TRIM(category) = ''")
                        .or()
                        .notIn("category", categories)));
    }

    @Override
    @Transactional
    public boolean removeTaskWithRecords(Long id, Long userId) {
        if (id == null || userId == null) {
            return false;
        }
        StudyTask task = baseMapper.selectOne(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, id)
                .eq(StudyTask::getUserId, userId));
        if (task == null) {
            return false;
        }

        List<StudyRecord> records = recordMapper.selectList(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getTaskId, id)
                .eq(StudyRecord::getUserId, userId));
        deductPlanInvestment(task, records);
        removeRecordsByTask(id, userId, records.size());

        return baseMapper.delete(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, id)
                .eq(StudyTask::getUserId, userId)) > 0;
    }

    private void deductPlanInvestment(StudyTask task, List<StudyRecord> records) {
        if (task.getPlanId() == null || records.isEmpty()) {
            return;
        }
        int minutes = records.stream()
                .map(StudyRecord::getDuration)
                .map(duration -> Math.max(duration == null ? 0 : duration, 0))
                .reduce(0, Integer::sum);
        planMapper.incrementInvestment(task.getPlanId(), -minutes, -records.size());
    }

    private void removeRecordsByTask(Long taskId, Long userId, int expectedRecordCount) {
        if (expectedRecordCount == 0) {
            return;
        }
        int removedRecords = recordMapper.delete(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getTaskId, taskId)
                .eq(StudyRecord::getUserId, userId));
        if (removedRecords != expectedRecordCount) {
            throw new IllegalStateException("Failed to remove all task records");
        }
    }
}