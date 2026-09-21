package com.example.studyplan.staticresources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PlanInvestmentPersistenceStaticTest {

    @Test
    void studyPlanStoresCachedAllRangeInvestmentFields() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/db/schema.sql"));
        String upgrade = Files.readString(Path.of("src/main/resources/db/plan_investment_upgrade.sql"));
        String entity = Files.readString(Path.of("src/main/java/com/example/studyplan/entity/StudyPlan.java"));
        String mapper = Files.readString(Path.of("src/main/java/com/example/studyplan/mapper/StudyPlanMapper.java"));
        String service = Files.readString(Path.of("src/main/java/com/example/studyplan/service/StudyRecordService.java"));
        String controller = Files.readString(Path.of("src/main/java/com/example/studyplan/controller/RecordController.java"));
        String runner = Files.readString(Path.of("src/main/java/com/example/studyplan/config/PlanInvestmentMaintenanceRunner.java"));

        assertThat(schema).contains("total_study_minutes", "total_study_record_count");
        assertThat(upgrade).contains("ALTER TABLE study_plan ADD COLUMN total_study_minutes");
        assertThat(upgrade).contains("ALTER TABLE study_plan ADD COLUMN total_study_record_count");
        assertThat(upgrade).contains("study_record", "study_task", "SUM", "COUNT");
        assertThat(entity).contains("private Integer totalStudyMinutes;", "private Integer totalStudyRecordCount;");
        assertThat(mapper).contains("incrementInvestment");
        assertThat(service).contains("saveAndAccumulateInvestment", "updateAndRefreshInvestment", "removeAndDeductInvestment");
        assertThat(controller).contains("saveAndAccumulateInvestment", "updateAndRefreshInvestment", "removeAndDeductInvestment");
        assertThat(runner).contains("ensureInvestmentColumns", "refreshPlanInvestmentTotals");
        assertThat(runner).contains("ALTER TABLE study_plan ADD COLUMN total_study_minutes");
        assertThat(runner).contains("ALTER TABLE study_plan ADD COLUMN total_study_record_count");
        assertThat(runner).contains("UPDATE study_plan plan");
    }
}
