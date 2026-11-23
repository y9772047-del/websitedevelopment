package com.example.studyplan.controller;

import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import lombok.Data;
import java.util.List;

@Data
public class QuickCaptureRequest {
    private StudyPlan plan;
    private List<QuickCaptureTask> tasks;

    @Data
    public static class QuickCaptureTask {
        private StudyTask task;
        private StudyRecord record;
    }
}
