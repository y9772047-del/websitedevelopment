# 大学生学习计划管理系统：课程提交说明

## 提交材料

- 项目源代码压缩包：后端 Java、Vue 前端源码、前端构建资源、数据库 SQL、Maven Wrapper 和相关文档。
- GitHub 仓库：[y9772047-del/websitedevelopment](https://github.com/y9772047-del/websitedevelopment)。
- 本次提交分支：[codex/submission-20261024](https://github.com/y9772047-del/websitedevelopment/tree/codex/submission-20261024)。

## 环境要求

| 项目 | 要求 |
| --- | --- |
| Java | JDK 17 或 21；本次在 JDK 21 下验证，编译目标为 Java 17 |
| Maven | 可直接使用项目自带的 Maven Wrapper，自动获取 Maven 3.9.11 |
| 数据库 | MySQL 8.x，支持 utf8mb4 |
| 前端构建 | Node.js 22 LTS，或支持 Vite 7 的 Node.js 版本；项目 CI 使用 Node.js 22 |
| 依赖管理 | pnpm；依赖版本由 `frontend/pnpm-lock.yaml` 锁定 |

压缩包已经包含 `src/main/resources/static/` 中的前端构建资源。只运行系统时，无须安装 Node.js 或重新构建前端。首次下载 Maven 和 Java 依赖需要网络。

## 数据库配置

1. 启动本机 MySQL。
2. 用 MySQL 客户端以 UTF-8 编码导入 `src/main/resources/db/schema.sql`。脚本会创建 `study_plan_system` 数据库、表、基础配置和演示账号。
3. 该脚本包含 `DROP TABLE`，仅用于全新演示数据库，已有数据时不要重新执行，也不要批量执行整个 `db/` 目录。
4. 默认连接：`localhost:3306`，数据库 `study_plan_system`，用户名 `root`。如需调整主机、端口或用户名，修改 `src/main/resources/application.yml` 中的 `spring.datasource`。
5. 数据库密码通过环境变量 `DB_PASSWORD` 提供，真实密码不写入提交包。

已有数据库升级登录权限模块时，先备份，再参考 `docs/experiment-4/README.md` 执行 `experiment4_auth_upgrade.sql`。其他增量脚本按对应旧数据库的实际版本使用。

## Windows 启动步骤

使用 PowerShell 进入解压后的项目根目录，确保 `JAVA_HOME` 指向 JDK 17 或 21：

```powershell
$env:DB_PASSWORD = '填写本机 MySQL 密码'
# 随机生成仅供本机演示使用的 JWT 签名密钥：
$env:JWT_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
.\mvnw.cmd spring-boot:run
```

PowerShell 5.1 可把生成密钥的一行替换为：

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRng.Dispose()
```

浏览器访问 <http://localhost:8080>。默认端口为 8080。正式部署应设置固定的随机 `JWT_SECRET` 并启用 HTTPS。

也可先构建，再启动：

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target/study-plan-system-1.0.0.jar
```

## 演示账号

仅适用于全新导入初始化脚本后的本机演示环境：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 学生 | student | 123456 |
| 管理员 | admin | admin123 |

登录后按账号角色显示学生端或管理端。已有数据库的账号以实际数据为准。

## AI 功能与前端开发

计划、任务、记录、日历和管理功能不要求提供 AI 密钥。使用 AI 改进建议、AI 周报或目标复盘时，启动前另外设置：

```powershell
$env:DEEPSEEK_API_KEY = '填写自己的 DeepSeek API Key'
```

前端源码在 `frontend/`。修改前端后，在该目录执行：

```powershell
pnpm install --frozen-lockfile
pnpm test
pnpm build
```

`pnpm build` 会更新后端静态资源。开发模式运行 `pnpm dev` 后访问 <http://localhost:5173>，同时保持后端运行。页面使用 Bootstrap CDN，完整加载样式需要网络。

## 验证范围

本次交付的实际构建及测试结果记录在压缩包的 `验证结果.txt` 中。数据库初始化 SQL 随源码提供，不自动连接或修改使用者的数据库。

## 目录

- `src/main/java/`：后端源码。
- `src/main/resources/db/`：建表、基础数据与升级脚本。
- `src/main/resources/application.yml`：运行配置和环境变量占位符。
- `src/main/resources/static/`：可由 Spring Boot 直接提供的前端资源。
- `src/test/`：后端测试。
- `frontend/`：Vue 前端源码、测试、构建配置及锁文件。
- `.mvn/wrapper/`、`mvnw`、`mvnw.cmd`、`pom.xml`：Maven 构建文件。
- `.github/`：GitHub 持续集成与协作配置。
- `docs/`：接口、实验和相关项目文档。

提交包排除 `.git`、`target`、`node_modules`、依赖缓存、IDE 配置、日志、本机私密配置和无关报告附件。
