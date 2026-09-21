package com.example.studyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyplan.entity.StudyRecord;

public interface StudyRecordService extends IService<StudyRecord> {
    boolean saveAndAccumulateInvestment(StudyRecord record);

    boolean updateAndRefreshInvestment(StudyRecord record, Long userId);

    boolean removeAndDeductInvestment(Long id, Long userId);
}
