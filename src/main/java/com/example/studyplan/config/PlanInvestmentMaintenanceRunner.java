package com.example.studyplan.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PlanInvestmentMaintenanceRunner implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public PlanInvestmentMaintenanceRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureInvestmentColumns();
        refreshPlanInvestmentTotals();
    }

    void ensureInvestmentColumns() {
        if (!columnExists("total_study_minutes")) {
            jdbcTemplate.execute("ALTER TABLE study_plan ADD COLUMN total_study_minutes INT NOT NULL DEFAULT 0 COMMENT '历史累计学习分钟数' AFTER status");
        }
        if (!columnExists("total_study_record_count")) {
            jdbcTemplate.execute("ALTER TABLE study_plan ADD COLUMN total_study_record_count INT NOT NULL DEFAULT 0 COMMENT '历史累计学习记录数' AFTER total_study_minutes");
        }
    }

    void refreshPlanInvestmentTotals() {
        jdbcTemplate.update("""
                UPDATE study_plan plan
                LEFT JOIN (
                    SELECT
                        task.plan_id,
                        COALESCE(SUM(record.duration), 0) AS total_minutes,
                        COUNT(record.id) AS record_count
                    FROM study_task task
                    LEFT JOIN study_record record ON record.task_id = task.id
                    WHERE task.plan_id IS NOT NULL
                    GROUP BY task.plan_id
                ) stat ON stat.plan_id = plan.id
                SET plan.total_study_minutes = COALESCE(stat.total_minutes, 0),
                    plan.total_study_record_count = COALESCE(stat.record_count, 0)
                """);
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'study_plan'
                  AND COLUMN_NAME = ?
                """, Integer.class, columnName);
        return count != null && count > 0;
    }
}
