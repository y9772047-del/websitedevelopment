CREATE DATABASE IF NOT EXISTS study_plan_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE study_plan_system;

DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS important_node_keyword_rule;
DROP TABLE IF EXISTS important_node;
DROP TABLE IF EXISTS study_record;
DROP TABLE IF EXISTS study_task;
DROP TABLE IF EXISTS study_task_category;
DROP TABLE IF EXISTS study_plan;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码，课程项目第一版使用明文，正式项目应加密',
    real_name VARCHAR(50) COMMENT '姓名',
    role VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色：student/admin',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常/0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间'
) COMMENT='用户表';

CREATE TABLE study_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    plan_name VARCHAR(100) NOT NULL COMMENT '计划名称',
    goal VARCHAR(255) COMMENT '学习目标',
    description TEXT COMMENT '计划描述',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    status VARCHAR(20) DEFAULT '进行中' COMMENT '状态：未开始/进行中/已完成/已取消',
    total_study_minutes INT NOT NULL DEFAULT 0 COMMENT '历史累计学习分钟数',
    total_study_record_count INT NOT NULL DEFAULT 0 COMMENT '历史累计学习记录数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_user (user_id)
) COMMENT='学习计划表';

CREATE TABLE study_task_category (
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
('其他任务', 8, '暂时无法归类但仍需要记录的学习任务');

CREATE TABLE study_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    plan_id BIGINT COMMENT '所属计划ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    description TEXT COMMENT '任务描述',
    category VARCHAR(50) NOT NULL DEFAULT '其他任务' COMMENT '分类',
    priority VARCHAR(20) DEFAULT 'Q2' COMMENT '优先级：Q0紧急/Q1重要成长/Q2普通维护/Q3低优先级',
    deadline DATE COMMENT '截止日期',
    status VARCHAR(20) DEFAULT '未完成' COMMENT '状态：未完成/进行中/已完成/延期',
    repeat_type VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '重复规则：NONE/DAILY/WEEKLY/WORKDAY',
    repeat_end_date DATE COMMENT '重复结束日期',
    repeat_group_id VARCHAR(64) COMMENT '重复任务组ID',
    reminder_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用提醒',
    reminder_days_before INT NOT NULL DEFAULT 3 COMMENT '提前提醒天数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_study_task_priority CHECK (priority IN ('Q0','Q1','Q2','Q3')),
    CONSTRAINT fk_task_category FOREIGN KEY (category) REFERENCES study_task_category(category_name),
    INDEX idx_task_user (user_id),
    INDEX idx_task_plan (plan_id),
    INDEX idx_task_deadline (deadline),
    INDEX idx_task_status (status),
    INDEX idx_task_repeat_group (repeat_group_id)
) COMMENT='学习任务表';

CREATE TABLE study_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    task_id BIGINT NOT NULL COMMENT '所属任务ID',
    study_date DATE NOT NULL COMMENT '学习日期',
    duration INT DEFAULT 0 COMMENT '学习时长，单位分钟',
    content TEXT NOT NULL COMMENT '学习内容',
    completion_status VARCHAR(30) DEFAULT '部分完成' COMMENT '完成情况',
    problems TEXT COMMENT '遇到的问题与学习反思',
    improvement_plan TEXT COMMENT '改进建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_record_user (user_id),
    INDEX idx_record_task (task_id),
    INDEX idx_record_date (study_date)
) COMMENT='学习记录表';

CREATE TABLE important_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '重要节点ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    title VARCHAR(120) NOT NULL COMMENT '节点标题',
    type VARCHAR(20) NOT NULL COMMENT '类型：考试/截止/比赛/汇报/其他',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    start_time TIME COMMENT '开始时间',
    end_time TIME COMMENT '结束时间',
    importance VARCHAR(20) NOT NULL DEFAULT '重要' COMMENT '重要程度：普通/重要/紧急',
    description TEXT COMMENT '描述',
    plan_id BIGINT COMMENT '关联学习计划ID，可为空',
    task_id BIGINT COMMENT '关联学习任务ID，可为空',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_important_event_type CHECK (type IN ('考试','截止','比赛','汇报','其他')),
    CONSTRAINT chk_important_node_importance CHECK (importance IN ('普通','重要','紧急')),
    INDEX idx_important_node_user_range (user_id, start_date, end_date),
    INDEX idx_important_event_type (type),
    INDEX idx_important_node_plan (plan_id),
    INDEX idx_important_node_task (task_id)
) COMMENT='重要节点表';

CREATE TABLE feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    user_id BIGINT NOT NULL COMMENT '提交用户ID',
    content TEXT NOT NULL COMMENT '反馈内容',
    status VARCHAR(20) NOT NULL DEFAULT '未处理' COMMENT '状态：未处理/已处理/已忽略',
    reply TEXT COMMENT '管理员处理备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feedback_user (user_id),
    INDEX idx_feedback_status (status),
    INDEX idx_feedback_create_time (create_time)
) COMMENT='用户反馈表';

INSERT INTO user (username, password, real_name, role, status) VALUES
('student', '123456', '测试学生', 'student', 1),
('admin', 'admin123', '系统管理员', 'admin', 1);
CREATE TABLE important_node_keyword_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关键词规则ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    keyword VARCHAR(60) NOT NULL COMMENT '匹配关键词',
    node_type VARCHAR(20) COMMENT '适用节点类型，空表示全部类型',
    effect VARCHAR(20) NOT NULL DEFAULT 'SCORE' COMMENT '影响方式：SCORE加分/MIN_IMPORTANT至少重要/URGENT直接紧急',
    score_delta INT NOT NULL DEFAULT 10 COMMENT '加分值，仅SCORE使用',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用/0停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_keyword_rule_node_type CHECK (node_type IS NULL OR node_type IN ('考试','截止','比赛','汇报','其他')),
    CONSTRAINT chk_keyword_rule_effect CHECK (effect IN ('SCORE','MIN_IMPORTANT','URGENT')),
    CONSTRAINT chk_keyword_rule_score CHECK (score_delta BETWEEN 0 AND 50),
    INDEX idx_keyword_rule_user_enabled (user_id, enabled),
    INDEX idx_keyword_rule_user_type (user_id, node_type)
) COMMENT='重要节点关键词规则表';
