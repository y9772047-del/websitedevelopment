-- Existing databases: run once before deploying Experiment 4. Do not run schema.sql.
-- Passwords are converted to BCrypt by the application startup migration after this SQL runs.
USE study_plan_system;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
) COMMENT='角色表';

CREATE TABLE IF NOT EXISTS permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL
) COMMENT='权限表';

CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
) COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission(id)
) COMMENT='角色权限关联';

ALTER TABLE role COMMENT='角色表';
ALTER TABLE permission COMMENT='权限表';
ALTER TABLE user_role COMMENT='用户角色关联';
ALTER TABLE role_permission COMMENT='角色权限关联';

INSERT INTO role (code, name) VALUES ('student', '学生'), ('admin', '管理员')
ON DUPLICATE KEY UPDATE name = VALUES(name);
INSERT INTO permission (code, name) VALUES
('study:access', '使用学习功能'),
('admin:access', '进入管理后台'),
('users:manage', '管理用户'),
('tasks:manage', '管理任务'),
('feedback:manage', '处理反馈')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r JOIN permission p
WHERE (r.code = 'student' AND p.code = 'study:access')
   OR (r.code = 'admin');
INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM user u JOIN role r
ON r.code = CASE WHEN u.role = 'admin' THEN 'admin' ELSE 'student' END;
