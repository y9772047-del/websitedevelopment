USE study_plan_system;

-- 管理员端和用户反馈功能增量升级脚本，不会清空已有业务数据。
ALTER TABLE user
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色：student/admin';

ALTER TABLE user
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常/0禁用';

CREATE TABLE IF NOT EXISTS feedback (
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

UPDATE user SET role = 'student' WHERE role IS NULL OR role = '';
UPDATE user SET status = 1 WHERE status IS NULL;

INSERT INTO user (username, password, real_name, role, status)
SELECT 'admin', 'admin123', '系统管理员', 'admin', 1
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'admin');

UPDATE user SET role = 'admin', status = 1 WHERE username = 'admin';
UPDATE user SET role = 'student' WHERE username <> 'admin' AND (role IS NULL OR role = '');
