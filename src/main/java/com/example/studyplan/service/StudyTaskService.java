package com.example.studyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyplan.entity.StudyTask;

import java.util.List;

public interface StudyTaskService extends IService<StudyTask> {
    String DEFAULT_CATEGORY = "鍏朵粬浠诲姟";

    List<String> listTaskCategories();

    boolean isValidCategory(String category);

    int normalizeLegacyCategories();

    boolean removeTaskWithRecords(Long id, Long userId);
}