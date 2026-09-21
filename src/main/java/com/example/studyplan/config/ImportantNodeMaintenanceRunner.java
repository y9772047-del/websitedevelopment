package com.example.studyplan.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ImportantNodeMaintenanceRunner implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public ImportantNodeMaintenanceRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureImportantNodeTable();
        ensureKeywordRuleTable();
    }

    void ensureImportantNodeTable() {
        if (!tableExists("important_node")) {
            createImportantNodeTable();
        }
    }

    void ensureKeywordRuleTable() {
        if (!tableExists("important_node_keyword_rule")) {
            createKeywordRuleTable();
        }
    }

    private void createImportantNodeTable() {
        jdbcTemplate.execute("""
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
                ) COMMENT='重要节点表'
                """);
    }

    private void createKeywordRuleTable() {
        jdbcTemplate.execute("""
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
                ) COMMENT='重要节点关键词规则表'
                """);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }
}