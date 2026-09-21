USE study_plan_system;

SET @fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'fk_task_category'
      AND TABLE_NAME = 'study_task'
);
SET @drop_fk_sql = IF(@fk_exists > 0,
    'ALTER TABLE study_task DROP FOREIGN KEY fk_task_category',
    'SELECT 1'
);
PREPARE drop_fk_stmt FROM @drop_fk_sql;
EXECUTE drop_fk_stmt;
DEALLOCATE PREPARE drop_fk_stmt;

CREATE TABLE IF NOT EXISTS study_task_category (
    category_name VARCHAR(50) PRIMARY KEY COMMENT '任务分类名称',
    sort_order INT NOT NULL COMMENT '排序',
    description VARCHAR(255) COMMENT '分类说明'
) COMMENT='学习任务分类表';

INSERT INTO study_task_category (category_name, sort_order, description) VALUES
('课程进度', 1, '学校课程、课堂学习、章节预习复习'),
('作业实验', 2, '课程作业、实验报告、实践任务、课程设计'),
('考试备考', 3, '期末考试、四六级、考研、证书考试'),
('竞赛科研', 4, '学科竞赛、科研训练、论文阅读、项目申报'),
('专业能力', 5, '编程训练、算法练习、项目开发、专业技能提升'),
('复盘总结', 6, '错题整理、阶段复盘、学习反思、经验总结'),
('自我提升', 7, '阅读、表达、英语听说、时间管理、习惯养成'),
('其他任务', 8, '暂时无法归类但仍需要记录的学习任务')
ON DUPLICATE KEY UPDATE
    sort_order = VALUES(sort_order),
    description = VALUES(description);

UPDATE study_task
SET category = '其他任务'
WHERE category IS NULL
   OR category = ''
   OR TRIM(category) = ''
   OR category NOT IN (
       '课程进度',
       '作业实验',
       '考试备考',
       '竞赛科研',
       '专业能力',
       '复盘总结',
       '自我提升',
       '其他任务'
   );

ALTER TABLE study_task
    MODIFY category VARCHAR(50) NOT NULL DEFAULT '其他任务';

ALTER TABLE study_task
    ADD CONSTRAINT fk_task_category
    FOREIGN KEY (category) REFERENCES study_task_category(category_name);
