-- Upgrade existing databases for repeated study tasks.

SET @task_repeat_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_task'
      AND COLUMN_NAME = 'repeat_type'
);
SET @task_repeat_type_sql := IF(
    @task_repeat_type_exists = 0,
    'ALTER TABLE study_task ADD COLUMN repeat_type VARCHAR(20) NOT NULL DEFAULT ''NONE'' COMMENT ''重复规则：NONE/DAILY/WEEKLY/WORKDAY'' AFTER status',
    'SELECT ''study_task.repeat_type already exists'''
);
PREPARE task_repeat_type_stmt FROM @task_repeat_type_sql;
EXECUTE task_repeat_type_stmt;
DEALLOCATE PREPARE task_repeat_type_stmt;

SET @task_repeat_end_date_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_task'
      AND COLUMN_NAME = 'repeat_end_date'
);
SET @task_repeat_end_date_sql := IF(
    @task_repeat_end_date_exists = 0,
    'ALTER TABLE study_task ADD COLUMN repeat_end_date DATE COMMENT ''重复结束日期'' AFTER repeat_type',
    'SELECT ''study_task.repeat_end_date already exists'''
);
PREPARE task_repeat_end_date_stmt FROM @task_repeat_end_date_sql;
EXECUTE task_repeat_end_date_stmt;
DEALLOCATE PREPARE task_repeat_end_date_stmt;

SET @task_repeat_group_id_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_task'
      AND COLUMN_NAME = 'repeat_group_id'
);
SET @task_repeat_group_id_sql := IF(
    @task_repeat_group_id_exists = 0,
    'ALTER TABLE study_task ADD COLUMN repeat_group_id VARCHAR(64) COMMENT ''重复任务组ID'' AFTER repeat_end_date',
    'SELECT ''study_task.repeat_group_id already exists'''
);
PREPARE task_repeat_group_id_stmt FROM @task_repeat_group_id_sql;
EXECUTE task_repeat_group_id_stmt;
DEALLOCATE PREPARE task_repeat_group_id_stmt;

SET @task_repeat_group_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_task'
      AND INDEX_NAME = 'idx_task_repeat_group'
);
SET @task_repeat_group_index_sql := IF(
    @task_repeat_group_index_exists = 0,
    'ALTER TABLE study_task ADD INDEX idx_task_repeat_group (repeat_group_id)',
    'SELECT ''idx_task_repeat_group already exists'''
);
PREPARE task_repeat_group_index_stmt FROM @task_repeat_group_index_sql;
EXECUTE task_repeat_group_index_stmt;
DEALLOCATE PREPARE task_repeat_group_index_stmt;
SET @task_reminder_enabled_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_task'
      AND COLUMN_NAME = 'reminder_enabled'
);
SET @task_reminder_enabled_sql := IF(
    @task_reminder_enabled_exists = 0,
    'ALTER TABLE study_task ADD COLUMN reminder_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''是否启用提醒'' AFTER repeat_group_id',
    'SELECT ''study_task.reminder_enabled already exists'''
);
PREPARE task_reminder_enabled_stmt FROM @task_reminder_enabled_sql;
EXECUTE task_reminder_enabled_stmt;
DEALLOCATE PREPARE task_reminder_enabled_stmt;

SET @task_reminder_days_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_task'
      AND COLUMN_NAME = 'reminder_days_before'
);
SET @task_reminder_days_sql := IF(
    @task_reminder_days_exists = 0,
    'ALTER TABLE study_task ADD COLUMN reminder_days_before INT NOT NULL DEFAULT 3 COMMENT ''提前提醒天数'' AFTER reminder_enabled',
    'SELECT ''study_task.reminder_days_before already exists'''
);
PREPARE task_reminder_days_stmt FROM @task_reminder_days_sql;
EXECUTE task_reminder_days_stmt;
DEALLOCATE PREPARE task_reminder_days_stmt;