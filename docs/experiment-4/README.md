# 实验四：登录、权限与 AI 安全

## 实现范围

- Spring Security 对 `/api/**` 进行认证，管理接口与 AI 接口使用 `@PreAuthorize` 校验数据库中的权限。原有按 `userId` 限定数据归属的查询继续生效。
- `user.password` 存储 BCrypt 哈希。新注册立即哈希；已有明文密码在升级 SQL 执行后的首次启动时自动转为哈希。启动迁移可重复运行，已哈希密码不会再次哈希。
- 登录签发 30 分钟有效的 HS256 JWT，放在 `HttpOnly`、`SameSite=Lax`、`Path=/api` Cookie 中。服务端每次请求都重新读取账号状态、角色和权限；禁用账号立即失效。前端只记住用户名，不把 JWT 写入 `localStorage` 或 `sessionStorage`。
- 所有修改请求都需要 `X-XSRF-TOKEN` 请求头；前端从 `XSRF-TOKEN` Cookie 取得 CSRF 令牌。登录前的 `/api/auth/current` 请求会发放该令牌。
- 前端与 API 应使用同一主机名（端口可不同），确保前端能读取 `XSRF-TOKEN` Cookie。若将两者部署在不同主机名下，现有前端无法取得 CSRF 令牌，修改请求会返回 403；请通过同一主机名的反向代理部署。正式环境使用 HTTPS。
- AI 改进建议、周报、目标复盘把用户可控内容限制长度后编码为 JSON 数据，并在系统指令中声明其不可信；模型错误不把原始响应或输入回传给浏览器。提示注入防护只能降低风险，不能保证模型绝不受诱导。

## 数据库升级

1. 备份现有 `study_plan_system` 数据库。
2. 对**已有数据库**用 UTF-8 客户端执行 [`experiment4_auth_upgrade.sql`](../../src/main/resources/db/experiment4_auth_upgrade.sql)，例如 `mysql --default-character-set=utf8mb4 -u root -D study_plan_system < src/main/resources/db/experiment4_auth_upgrade.sql`。该脚本只增加角色权限表并关联原有用户，不删除业务数据。
3. 配置 `DB_PASSWORD` 和至少 32 字节的随机 `JWT_SECRET`，启动应用。首次启动会将旧密码转换成 BCrypt 哈希。若只想运行迁移、不启动 Web 服务，可在 PowerShell 执行 `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.main.web-application-type=none'`。
4. 可用 `SELECT id, username FROM user WHERE password NOT LIKE '$2%';` 检查是否仍有未迁移密码。不要查询或导出完整密码列。

全新数据库只使用 [`schema.sql`](../../src/main/resources/db/schema.sql)，它包含删除重建表的语句，不能用于已有数据的升级。

## 运行边界

- 正式环境使用 HTTPS，并确保反向代理把原始协议传给 Spring Boot，以便认证 Cookie 带 `Secure` 标记。
- 未配置 `JWT_SECRET` 时应用生成临时随机密钥供本地开发；重启后所有 JWT 失效。不要在仓库中写入真实密钥。
- 退出登录会清除浏览器 Cookie；已泄露的 JWT 在到期前仍可能被使用，因此应保护 Cookie 并保持较短有效期。禁用用户会在下一次请求时被拒绝。
- `user.role` 字段暂留给现有用户展示和迁移；实际授权读取 `user_role`、`role_permission` 表。角色变化应同时保持展示字段一致。

## 验证

在 `frontend/` 执行 `pnpm test`、`pnpm build`；在项目根目录执行 `./mvnw.cmd test`、`./mvnw.cmd clean package -DskipTests`。重点检查匿名访问返回 401、学生访问管理接口返回 403、缺少 CSRF 的修改请求返回 403、管理员具备细分权限后可以修改，以及禁用账号的旧 JWT 被拒绝。
