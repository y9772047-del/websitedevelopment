USE study_plan_system;
SET NAMES utf8mb4;

-- Upgrade existing databases after merging learning reflection into study records.
SET @study_record_improvement_plan_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_record'
      AND COLUMN_NAME = 'improvement_plan'
);

SET @study_record_improvement_plan_sql := IF(
    @study_record_improvement_plan_exists = 0,
    'ALTER TABLE study_record ADD COLUMN improvement_plan TEXT COMMENT ''改进建议'' AFTER problems',
    'SELECT ''study_record.improvement_plan already exists'''
);

PREPARE study_record_improvement_plan_stmt FROM @study_record_improvement_plan_sql;
EXECUTE study_record_improvement_plan_stmt;
DEALLOCATE PREPARE study_record_improvement_plan_stmt;

ALTER TABLE study_record
    MODIFY COLUMN problems TEXT COMMENT '遇到的问题与学习反思';

DROP TABLE IF EXISTS study_reflection;
