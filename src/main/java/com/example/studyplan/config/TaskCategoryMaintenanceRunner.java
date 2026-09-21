package com.example.studyplan.config;

import com.example.studyplan.service.StudyTaskService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TaskCategoryMaintenanceRunner implements ApplicationRunner {
    private final StudyTaskService taskService;

    public TaskCategoryMaintenanceRunner(StudyTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void run(ApplicationArguments args) {
        taskService.normalizeLegacyCategories();
    }
}
