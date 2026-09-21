USE study_plan_system;

-- Optional cleanup script for old demo data.
-- This file no longer inserts preset plans, tasks, or records,
-- so AI suggestions will not be influenced by unrelated sample content.
SET @test_user_id := (
    SELECT id FROM user WHERE username = 'student' LIMIT 1
);

DELETE FROM study_record
WHERE user_id = @test_user_id
  AND task_id IN (
      SELECT id FROM study_task
      WHERE user_id = @test_user_id
        AND task_name LIKE '约束测试-%'
  );

DELETE FROM study_task
WHERE user_id = @test_user_id
  AND task_name LIKE '约束测试-%';

DELETE FROM study_plan
WHERE user_id = @test_user_id
  AND plan_name LIKE '约束测试-%';
