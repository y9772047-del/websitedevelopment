CREATE TABLE IF NOT EXISTS important_node_keyword_rule (
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