-- 现有数据库升级脚本：保留原分类、计划、任务和记录。不要用 schema.sql 升级。
USE study_plan_system;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_task_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 100,
    description VARCHAR(255),
    UNIQUE KEY uk_user_category (user_id, category_name)
) COMMENT='个人DIY任务分类；推荐目录不参与统计';

-- 为已有账号保留原来可选的全部分类。推荐目录不再运行时混入个人列表。
INSERT INTO user_task_category (user_id, category_name, sort_order, description)
SELECT u.id, c.category_name, c.sort_order, c.description
FROM user u CROSS JOIN study_task_category c
WHERE NOT EXISTS (SELECT 1 FROM user_task_category p
                  WHERE p.user_id = u.id AND p.category_name = c.category_name);

-- 表刚创建时没有历史重复数据；若已有该表，保留全部内容。
-- 将已有非模板分类收录到对应用户，避免历史任务失去可选分类。
INSERT INTO user_task_category (user_id, category_name, sort_order)
SELECT DISTINCT t.user_id, t.category, 100
FROM study_task t
WHERE t.category IS NOT NULL AND TRIM(t.category) <> ''
  AND NOT EXISTS (SELECT 1 FROM study_task_category c WHERE c.category_name = t.category)
  AND NOT EXISTS (SELECT 1 FROM user_task_category u WHERE u.user_id = t.user_id AND u.category_name = t.category);

-- 仅移除全局分类外键，改由服务按当前用户校验。允许脚本重复执行。
SET @category_fk_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'study_task'
              AND CONSTRAINT_NAME = 'fk_task_category' AND CONSTRAINT_TYPE = 'FOREIGN KEY'),
    'ALTER TABLE study_task DROP FOREIGN KEY fk_task_category',
    'SELECT 1');
PREPARE category_fk_stmt FROM @category_fk_sql;
EXECUTE category_fk_stmt;
DEALLOCATE PREPARE category_fk_stmt;
