package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTaskCategory;
import com.example.studyplan.entity.UserTaskCategory;
import com.example.studyplan.dto.TaskCategorySummary;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.mapper.StudyPlanMapper;
import com.example.studyplan.mapper.StudyRecordMapper;
import com.example.studyplan.mapper.StudyTaskCategoryMapper;
import com.example.studyplan.mapper.StudyTaskMapper;
import com.example.studyplan.mapper.UserTaskCategoryMapper;
import com.example.studyplan.service.StudyTaskService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudyTaskServiceImpl extends ServiceImpl<StudyTaskMapper, StudyTask> implements StudyTaskService {
    private final StudyTaskCategoryMapper taskCategoryMapper;
    private final StudyRecordMapper recordMapper;
    private final StudyPlanMapper planMapper;
    private final UserTaskCategoryMapper userCategoryMapper;

    public StudyTaskServiceImpl(StudyTaskCategoryMapper taskCategoryMapper,
                                StudyRecordMapper recordMapper,
                                StudyPlanMapper planMapper) {
        this(taskCategoryMapper, recordMapper, planMapper, null);
    }

    @Autowired
    public StudyTaskServiceImpl(StudyTaskCategoryMapper taskCategoryMapper,
                                StudyRecordMapper recordMapper,
                                StudyPlanMapper planMapper,
                                UserTaskCategoryMapper userCategoryMapper) {
        this.taskCategoryMapper = taskCategoryMapper;
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
        this.userCategoryMapper = userCategoryMapper;
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
    public List<String> listAvailableCategories(Long userId) {
        List<String> categories = new java.util.ArrayList<>();
        if (userId != null && userCategoryMapper != null) {
            categories.addAll(userCategoryMapper.selectList(new LambdaQueryWrapper<UserTaskCategory>()
                            .eq(UserTaskCategory::getUserId, userId)
                            .orderByAsc(UserTaskCategory::getSortOrder))
                    .stream().map(UserTaskCategory::getCategoryName).toList());
        }
        return categories.stream().distinct().toList();
    }

    @Override
    public List<TaskCategorySummary> listMyCategories(Long userId) {
        return userCategoryMapper.listWithTaskCounts(userId);
    }

    private boolean invalidCategoryName(String name) {
        return name == null || name.isBlank() || name.length() > 50;
    }

    private boolean categoryNameExists(String name, Long userId, Long excludedId) {
        return userCategoryMapper.selectCount(new LambdaQueryWrapper<UserTaskCategory>()
                .eq(UserTaskCategory::getUserId, userId).eq(UserTaskCategory::getCategoryName, name)
                .ne(excludedId != null, UserTaskCategory::getId, excludedId)) > 0;
    }

    @Override
    public String createCategory(UserTaskCategory category, Long userId) {
        if (category == null || invalidCategoryName(category.getCategoryName())) return "自定义分类名称无效";
        String name = category.getCategoryName().trim();
        if (categoryNameExists(name, userId, null)) return "分类名称已存在";
        category.setId(null); category.setUserId(userId); category.setCategoryName(name);
        if (category.getDescription() != null && category.getDescription().length() > 255)
            return "分类说明不能超过 255 个字符";
        if (category.getSortOrder() == null) category.setSortOrder(100);
        userCategoryMapper.insert(category);
        return null;
    }

    @Override
    public boolean isValidCategory(String category, Long userId) {
        if (category == null || userId == null || userCategoryMapper == null) return false;
        // Called inside the task write transaction, using the same row lock as rename/delete.
        UserTaskCategory owned = userCategoryMapper.selectOne(new LambdaQueryWrapper<UserTaskCategory>()
                .eq(UserTaskCategory::getUserId, userId).eq(UserTaskCategory::getCategoryName, category).last("FOR UPDATE"));
        return owned != null && category.equals(owned.getCategoryName());
    }

    private UserTaskCategory ownedCategory(Long id, Long userId) {
        if (id == null || userId == null || id <= 0) return null;
        return userCategoryMapper.selectOne(new LambdaQueryWrapper<UserTaskCategory>()
                .eq(UserTaskCategory::getId, id).eq(UserTaskCategory::getUserId, userId).last("FOR UPDATE"));
    }

    @Override
    @Transactional
    public String renameCategory(Long id, String categoryName, Long userId) {
        String name = categoryName == null ? "" : categoryName.trim();
        if (invalidCategoryName(name)) return "分类名称须为 1 到 50 个字符";
        UserTaskCategory category = ownedCategory(id, userId);
        if (category == null) return "分类不存在或无权操作";
        if (name.equals(category.getCategoryName())) return null;
        if (categoryNameExists(name, userId, id)) return "分类名称已存在";
        int updated = userCategoryMapper.update(null, new UpdateWrapper<UserTaskCategory>()
                .set("category_name", name).eq("id", id).eq("user_id", userId));
        if (updated != 1) throw new IllegalStateException("修改分类失败");
        baseMapper.update(null, new UpdateWrapper<StudyTask>().set("category", name)
                .eq("user_id", userId).eq("category", category.getCategoryName()));
        return null;
    }

    @Override
    @Transactional
    public String deleteCategory(Long id, Long replacementId, Long userId) {
        UserTaskCategory category = ownedCategory(id, userId);
        if (category == null) return "分类不存在或无权操作";
        if (id.equals(replacementId)) return "转入分类不能是当前分类";
        long taskCount = baseMapper.selectCount(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getUserId, userId).eq(StudyTask::getCategory, category.getCategoryName()));
        if (taskCount > 0 && replacementId == null) return "该分类已有任务，请先选择转入分类";
        UserTaskCategory replacement = replacementId == null ? null : ownedCategory(replacementId, userId);
        if (replacementId != null && replacement == null) return "转入分类不存在或无权使用";
        if (taskCount > 0) {
            int moved = baseMapper.update(null, new UpdateWrapper<StudyTask>()
                    .set("category", replacement.getCategoryName())
                    .eq("user_id", userId).eq("category", category.getCategoryName()));
            if (moved != taskCount) throw new IllegalStateException("任务分类已变化，请刷新后重试");
        }
        if (userCategoryMapper.delete(new LambdaQueryWrapper<UserTaskCategory>()
                .eq(UserTaskCategory::getId, id).eq(UserTaskCategory::getUserId, userId)) != 1)
            throw new IllegalStateException("删除分类失败");
        return null;
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
                        .apply("TRIM(category) = ''")));
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
