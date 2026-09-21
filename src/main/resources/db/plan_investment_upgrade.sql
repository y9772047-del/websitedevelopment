USE study_plan_system;

SET @plan_total_minutes_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_plan'
      AND COLUMN_NAME = 'total_study_minutes'
);

SET @plan_total_minutes_sql := IF(
    @plan_total_minutes_exists = 0,
    'ALTER TABLE study_plan ADD COLUMN total_study_minutes INT NOT NULL DEFAULT 0 COMMENT ''历史累计学习分钟数'' AFTER status',
    'SELECT ''study_plan.total_study_minutes already exists'''
);
PREPARE plan_total_minutes_stmt FROM @plan_total_minutes_sql;
EXECUTE plan_total_minutes_stmt;
DEALLOCATE PREPARE plan_total_minutes_stmt;

SET @plan_total_record_count_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_plan'
      AND COLUMN_NAME = 'total_study_record_count'
);

SET @plan_total_record_count_sql := IF(
    @plan_total_record_count_exists = 0,
    'ALTER TABLE study_plan ADD COLUMN total_study_record_count INT NOT NULL DEFAULT 0 COMMENT ''历史累计学习记录数'' AFTER total_study_minutes',
    'SELECT ''study_plan.total_study_record_count already exists'''
);
PREPARE plan_total_record_count_stmt FROM @plan_total_record_count_sql;
EXECUTE plan_total_record_count_stmt;
DEALLOCATE PREPARE plan_total_record_count_stmt;

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
    plan.total_study_record_count = COALESCE(stat.record_count, 0);
