package com.example.studyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.entity.UserTaskCategory;
import com.example.studyplan.dto.TaskCategorySummary;

import java.util.List;

public interface StudyTaskService extends IService<StudyTask> {
    String DEFAULT_CATEGORY = "其他任务";

    List<String> listTaskCategories();

    List<String> listAvailableCategories(Long userId);

    List<TaskCategorySummary> listMyCategories(Long userId);

    String createCategory(UserTaskCategory category, Long userId);

    String renameCategory(Long id, String categoryName, Long userId);

    String deleteCategory(Long id, Long replacementId, Long userId);

    boolean isValidCategory(String category);

    boolean isValidCategory(String category, Long userId);

    int normalizeLegacyCategories();

    boolean removeTaskWithRecords(Long id, Long userId);
}
