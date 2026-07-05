# Codex 协作记录

## 项目基本信息

| 项 | 内容 |
|---|---|
| 项目名称 | 校园二手物品交易平台的设计与实现 |
| 指导书 | 《徐士伟-软件与理论综合设计与实践项目指导书【徐士伟-题目2-2026】》 |
| 项目类型 | 应用系统 |
| 计划技术栈 | Vue3 + Spring Boot + MySQL + Redis |
| 架构方向 | 前后端分离 |
| 当前阶段 | 项目初始化、文档规范、开发环境检查 |

## 当前项目状态

- Git 仓库：已初始化。
- 文档目录：已创建 `docs/`。
- 前端项目：未创建。
- 后端项目：未创建。
- 数据库脚本：未创建。
- 当前未实现业务功能。

## Agent 工作规则

1. 工作前先检查当前目录、Git 状态、已有文档规范和已有协作记录。
2. 遵循已有规范；如无规范，使用本文件和 `docs/协作记录规范.md`。
3. 不把计划功能写成已完成功能。
4. 不提交真实账号、密码、密钥、数据库连接串、`.env`、运行时数据或大型依赖。
5. Java、MySQL、Redis 等关键环境如检测异常，应先说明检测方式和建议，不擅自安装。
6. 每次重要代码、文档或环境配置变动后，都应在本文件记录。
7. 如用户要求生成、优化或改写提示词，Agent 应优先输出可直接复制使用的优化提示词，而不是默认执行提示词中的任务。
8. 项目相关提示词必须基于当前项目真实内容；第一次处理前应浏览项目结构、关键文档和 Git 状态，后续处理时应关注项目变化。
9. 给其他 Agent 的提示词应约束输出文件数量、位置、命名、内容范围、验收标准和自检要求，避免生成过多或偏离项目的文件。
10. 代码、配置、脚本等会影响项目行为的变动，默认以 Git 变动为单位记录到本文件；只有用户明确说“不需要记录改动记录”时才跳过。
11. 后续涉及后端业务、事务一致性、高并发、MySQL、Redis、MQ、缓存、锁、审计、补偿、测试和落地路线的设计或实现时，应把 `docs/后端工业级业务事务与高并发全景设计.md` 作为全局参考文档，先对照其原则再落地。

## Git 变动记录规则

1. 记录以 Git 变动为单位。
2. 每条记录应包含时间、执行 Agent、变动类型、涉及文件、完成内容、验证方式、后续状态。
3. 如果用户明确说“不需要记录”，才可以跳过记录。
4. 未提交前也应记录本次工作内容，便于后续决定是否提交。

## 记录模板

```markdown
## YYYY-MM-DD HH:mm - 标题

- 执行 Agent：
- 变动类型：
- 涉及文件：
- 完成内容：
- 验证方式：
- 后续状态：
```

## 提示词生成任务要求

当用户的目标是让 Agent 编写提示词时，应生成结构化、可执行、可复制的提示词。提示词建议包含：

1. 任务背景。
2. 目标结果。
3. 具体步骤。
4. 约束条件。
5. 输出格式。
6. 验收标准。
7. 自检要求。
8. 必要时的追问规则。

生成提示词时应保留用户原意，拆解模糊需求，补充合理的边界条件和交付要求；如果信息不足，应让执行 Agent 用最少问题向用户确认。

## 变动记录

## 2026-07-04 - 项目初始化与文档规范建立

- 执行 Agent：Codex
- 变动类型：初始化、文档、环境检查
- 涉及文件：`.gitignore`、`.gitattributes`、`README.md`、`docs/文档规范.md`、`docs/项目说明.md`、`docs/开发环境.md`、`docs/依赖版本清单.md`、`docs/目录结构规范.md`、`docs/接口文档规范.md`、`docs/数据库设计规范.md`、`docs/测试规范.md`、`docs/协作记录规范.md`、`codex.md`
- 完成内容：初始化 Git 仓库；创建基础忽略规则和文本属性规则；建立项目文档规范、开发环境记录、依赖版本清单和 Agent 协作记录机制。
- 验证方式：执行目录检查、记录文件检查、`git init`、`git status`；执行 `git --version`、`java -version`、`javac -version`、`mvn -version`、`node --version`、`npm --version`、`pnpm --version`、`redis-server --version`、`mysql --version`；通过 Windows 服务、安装目录和完整路径命令进一步确认 MySQL。
- 后续状态：初始化完成；后续需用户确认 MySQL 使用版本及是否配置 PATH，再开始需求分析和前后端项目脚手架创建。

## 2026-07-04 - 补充 Apifox 安装状态

- 执行 Agent：Codex
- 变动类型：环境检查、文档
- 涉及文件：`docs/开发环境.md`、`docs/依赖版本清单.md`、`codex.md`
- 完成内容：根据用户提供路径 `D:\soft_wares2\APIfox_` 检查 Apifox 安装状态，确认 `Apifox.exe` 存在且版本为 `2.7.44`；更新环境文档和依赖版本清单，说明命令未加入 PATH。
- 验证方式：检查 `D:\soft_wares2\APIfox_` 目录；读取 `D:\soft_wares2\APIfox_\Apifox.exe` 版本信息；执行 `Get-Command apifox,Apifox` 确认命令当前未加入 PATH。
- 后续状态：Apifox 已安装，可作为后续接口文档、Mock、联调和接口测试工具；暂未修改 PATH。

## 2026-07-04 - 创建零依赖环境安装指南

- 执行 Agent：Codex
- 变动类型：文档、环境规范
- 涉及文件：`docs/零依赖环境安装指南.md`、`README.md`、`codex.md`
- 完成内容：新增面向零依赖成员的环境安装指南，按当前项目检测结果对齐 Git、Java、Maven、Node.js、pnpm、MySQL、Redis、IDE 和 Apifox 版本；补充推荐安装路径、PATH 配置、验证命令和无需提前全局安装的项目依赖清单。
- 验证方式：基于已检测环境版本和现有文档记录进行编写；未执行新增软件安装；未创建前后端业务代码。
- 后续状态：零依赖成员可按该指南准备本地环境；如后续统一切换 Java 或 MySQL 版本，需要同步更新环境文档和依赖清单。

## 2026-07-04 - 调整零依赖安装目录表述

- 执行 Agent：Codex
- 变动类型：文档
- 涉及文件：`docs/零依赖环境安装指南.md`、`codex.md`
- 完成内容：将零依赖指南中的安装目录从当前主机的特殊路径调整为通用建议，明确 `D:\soft_wares2`、`D:\soft_wares5`、`E:\01big_project` 只作为本机检测记录，不要求其他成员照抄；新成员可优先使用官方默认目录或 `D:\dev-tools\软件名称\版本号` 这类清晰目录。
- 验证方式：检查文档内容，确认版本号仍保持对齐，安装目录不再绑定当前主机特殊路径。
- 后续状态：零依赖成员应统一版本号和验证命令，安装目录可按电脑实际情况调整。

## 2026-07-04 - 本机环境路径对齐

- 执行 Agent：Codex
- 变动类型：环境配置、文档
- 涉及文件：`docs/本机环境路径对齐记录.md`、`docs/开发环境.md`、`docs/依赖版本清单.md`、`README.md`、`codex.md`
- 完成内容：创建 `D:\dev-tools` 规范入口目录，通过 junction 指向本机现有 Git、Java、Maven、Node.js、MySQL 8.0.20、Redis、Apifox 安装位置；设置用户级 `JAVA_HOME`、`MAVEN_HOME`、`MYSQL_HOME`、`REDIS_HOME`、`NODE_HOME`；将 MySQL、Apifox、npm-global 和规范入口加入用户级 PATH；将 npm 全局目录调整为 `D:\dev-tools\npm-global` 并安装 `pnpm@11.7.0`。
- 验证方式：通过规范入口执行 `git --version`、`java -version`、`javac -version`、`mvn -version`、`node --version`、`npm --version`、`pnpm --version`、`mysql --version`、`mysqld --version`、`redis-server --version`、`redis-cli --version`，版本均符合记录；读取 Apifox 可执行文件版本为 `2.7.44.0`。
- 后续状态：用户级路径已对齐；系统级 PATH 中旧 Git、Java、Maven、Node.js、Redis 入口因缺少管理员权限未替换，后续如需命令解析优先显示为 `D:\dev-tools`，需管理员权限调整系统 PATH；VS Code 和 IntelliJ IDEA 未调整。

## 2026-07-04 - MySQL 服务名规范化检查

- 执行 Agent：Codex
- 变动类型：环境检查、文档、脚本
- 涉及文件：`scripts/admin-align-mysql-service.ps1`、`docs/MySQL服务规范化说明.md`、`docs/开发环境.md`、`docs/本机环境路径对齐记录.md`、`docs/依赖版本清单.md`、`README.md`、`codex.md`
- 完成内容：检查本机 MySQL Windows 服务，确认当前运行服务为 `mysql820_test2`；尝试修改服务显示名和描述，但当前非管理员权限被 Windows 拒绝；新增管理员脚本用于后续把显示名规范化为 `MySQL 8.0.20 - Campus Dev`，脚本不会停止、删除或重建服务。
- 验证方式：执行 `Get-CimInstance Win32_Service`、`sc.exe qc mysql820_test2`、`sc.exe config mysql820_test2 DisplayName= ...`；修改请求返回 `Access is denied`，服务仍保持 Running。
- 后续状态：当前服务内部名仍为 `mysql820_test2`；项目文档统一称为 `MySQL 8.0.20 - Campus Dev`；如需修改 Windows 服务显示名，需在管理员 PowerShell 中运行 `scripts/admin-align-mysql-service.ps1`。

## 2026-07-04 - 整理组员环境文档

- 执行 Agent：Codex
- 变动类型：文档
- 涉及文件：`docs/组员环境准备与版本对齐说明.md`、`README.md`、`codex.md`
- 完成内容：新增面向其他组员的环境准备说明，提炼必须安装软件、推荐版本、安装目录原则、环境变量、PATH、MySQL/Redis 约定、验证命令和组员环境检查表；避免把当前主机特殊路径和服务历史作为团队安装标准。
- 验证方式：检查文档内容，确认只包含团队通用步骤和版本对齐要求，未创建业务代码。
- 后续状态：可将该文档作为发给组员的主要环境准备入口。

## 2026-07-04 - 合并环境文档为单一入口

- 执行 Agent：Codex
- 变动类型：文档整理
- 涉及文件：`docs/环境安装与版本对齐.md`、`README.md`、`docs/开发环境.md`、`codex.md`、`scripts/admin-align-mysql-service.ps1`
- 完成内容：按用户要求将给组员看的环境说明合并为单一文档 `docs/环境安装与版本对齐.md`；删除重复的组员环境、零依赖、本机路径对齐和 MySQL 服务规范化说明文档；移除额外管理员脚本，README 只保留一个环境安装入口。
- 验证方式：检查 README 文档入口和文件列表，确认对外环境说明只剩一个主文档。
- 后续状态：后续给组员时只发送 `docs/环境安装与版本对齐.md`。

## 2026-07-04 - 依赖可运行性 smoke test

- 执行 Agent：Codex
- 变动类型：环境验证、测试、文档
- 涉及文件：`test/`、`README.md`、`docs/开发环境.md`、`docs/依赖版本清单.md`、`docs/测试规范.md`、`codex.md`
- 完成内容：创建独立 `test/` 目录；编写环境检查脚本；创建 Spring Boot Maven smoke 项目和 Vue3/Vite pnpm smoke 项目；验证 Git、Java、Javac、Maven、Node.js、npm、pnpm、MySQL、Redis、Apifox；验证 Spring Boot、Spring Security、MySQL JDBC Driver、Spring Data Redis、JUnit、Vue3、Vite、Axios、Vitest 依赖可解析、测试、构建或启动。
- 验证方式：执行 `test/env/check-env.ps1`、`mvn -q test`、`mvn dependency:tree`、`mvn -q -DskipTests package`、短启动 Spring Boot jar、`pnpm install`、`pnpm build`、`pnpm test`、MySQL 服务和版本检查、Redis 临时实例 PING。
- 后续状态：smoke test 通过；MySQL `SELECT VERSION()` 因缺少测试账号密码被阻塞；现有 Redis 服务需要认证，已用临时 Redis 实例验证 PING；正式前后端工程仍未创建。
## 2026-07-04 - 依赖 smoke test 复验与报告修正

- 执行 Agent：Codex
- 变动类型：测试复验、文档修正、自检
- 涉及文件：`test/reports/dependency-test-report.md`、`test/env/env-result.md`、`codex.md`
- 完成内容：修正依赖测试总报告中 Git 状态代码块的指导书文件名显示；补充最终复验记录；重新执行环境脚本、后端 Maven 测试、前端 Vite 构建和 Vitest 测试；检查 Git 忽略规则和敏感信息痕迹。
- 验证方式：执行 `powershell -ExecutionPolicy Bypass -File .\test\env\check-env.ps1`、`mvn -q test`、`pnpm build`、`pnpm test`、`git status --short --ignored --untracked-files=all`、`git check-ignore -v ...`、`rg` 敏感信息扫描。
- 后续状态：复验通过；MySQL 登录级 `SELECT VERSION()` 仍需用户提供开发测试账号；现有 Redis 服务仍需要认证，已通过临时 Redis 实例验证 PING；未创建正式前端或后端业务工程。

## 2026-07-04 - 删除 test 目录

- 执行 Agent：Codex
- 变动类型：文件删除、文档修正
- 涉及文件：`test/`、`README.md`、`docs/开发环境.md`、`docs/依赖版本清单.md`、`docs/测试规范.md`、`codex.md`
- 完成内容：按用户要求删除 `test/` 目录；移除 README 和开发环境文档中指向已删除 smoke test 报告的链接；将依赖版本清单中的 smoke test 依赖状态调整为“正式工程未配置，历史验证版本仅供后续参考”；补充测试规范说明当前仓库不再保留 `test/`。
- 验证方式：执行路径安全校验后使用 `Remove-Item -Recurse -Force` 删除 `E:\CodexWorkspace\JavaSpringProject\test`；执行 `Test-Path` 确认目录不存在；执行 `rg` 检查失效链接；执行 `git status --short --ignored` 查看仓库状态。
- 后续状态：`test/` 目录已删除；正式前端和后端工程仍未创建。

## 2026-07-04 - 补充组员非标准环境处理规则

- 执行 Agent：Codex
- 变动类型：文档规范
- 涉及文件：`docs/环境安装与版本对齐.md`、`codex.md`
- 完成内容：补充组员已安装其他版本或其他目录时的处理规则，明确“不因目录不同强制重装”“版本不同先判断兼容性”“MySQL/Redis/Java 不擅自改动”；新增给组员 Agent 的统一检查提示词，要求输出版本、路径、状态、处理建议、MySQL/Redis 连接状态和需用户确认事项。
- 验证方式：检查文档新增段落和提示词格式，确认未创建前端或后端业务工程。
- 后续状态：组员可直接使用 `docs/环境安装与版本对齐.md` 中的提示词让各自 Agent 输出统一环境报告。

## 2026-07-04 - 补充提示词生成与 Agent 协作规则

- 执行 Agent：Codex
- 变动类型：文档规范、协作规则
- 涉及文件：`docs/协作记录规范.md`、`codex.md`
- 完成内容：根据用户提供的 Agent 协作提示词，补充提示词生成类任务规则；明确此类任务应输出可直接复制使用的优化提示词，而不是默认执行任务；补充多 Agent 记录文件检查位置、项目浏览与变化监视要求、对其他 Agent 输出文件的约束要求，以及代码/配置/脚本变动默认记录到 `codex.md` 的规则。
- 验证方式：检查 Markdown 内容，确认新增规则与现有协作记录机制一致；未修改业务代码，未创建前端或后端工程。
- 后续状态：后续项目类提示词生成、文档维护、代码修改任务均应遵循更新后的协作规则。

## 2026-07-04 - 上传 GitHub 初始仓库

- 执行 Agent：Codex
- 变动类型：Git 提交、远程仓库配置
- 涉及文件：`.gitattributes`、`.gitignore`、`README.md`、`docs/`、`codex.md`、指导书 `.docx`
- 完成内容：准备将当前项目初始化文档、协作规范、环境文档和指导书上传到 GitHub 仓库 `mdymdy666/JavaProjectForSchool`；上传前检查 Git 状态、远程仓库状态和敏感信息扫描结果。
- 验证方式：执行 `git status --short`、`git remote -v`、`git ls-remote https://github.com/mdymdy666/JavaProjectForSchool.git`、`rg` 敏感信息扫描；确认未创建正式前端、后端或测试目录。
- 后续状态：首次 commit 已完成，`origin` 已设置为 `https://github.com/mdymdy666/JavaProjectForSchool.git`，`main` 分支已推送到 GitHub。

## 2026-07-04 - 新增 Windows 依赖检测脚本

- 执行 Agent：Codex
- 变动类型：脚本、文档、环境检测
- 涉及文件：`scripts/check-dependencies.bat`、`scripts/check-dependencies.ps1`、`reports/dependency-check-report.md`、`.gitignore`、`README.md`、`docs/环境安装与版本对齐.md`、`docs/测试规范.md`、`docs/依赖版本清单.md`、`docs/开发环境.md`、`docs/目录结构规范.md`、`codex.md`
- 完成内容：新增 Windows BAT 入口脚本，调用同目录 PowerShell helper 检测 Git、Java、Javac、Maven、Node.js、npm、pnpm、MySQL、Redis、VS Code、IntelliJ IDEA、Apifox，并在后续存在 `pom.xml` 或 `package.json` 时检测 Spring Boot、Spring Security、MySQL JDBC Driver、Spring Data Redis、JUnit、Vue3、Vite、Axios、Vitest 等配置型依赖；生成 Markdown 报告；补充文档中的运行方式、状态含义和安全约束。
- 验证方式：执行 `scripts\check-dependencies.bat --no-pause`；脚本成功生成 `reports/dependency-check-report.md`；当前检测结果为已对齐 10 项、可接受待确认 6 项、需调整 0 项、未检测到 0 项、项目未配置 9 项、检测失败 0 项。
- 后续状态：脚本可供组员拉取仓库后直接运行；MySQL 登录级 `SELECT VERSION()` 仍需测试账号确认；当前 Redis 服务需要认证；正式前后端工程仍未创建。

## 2026-07-04 15:16 - 创建第一阶段前后端项目骨架

- 执行 Agent：Codex
- 变动类型：项目骨架、后端、前端、数据库脚本
- 涉及文件：`backend/`、`frontend/`、`sql/`、`codex.md`
- 完成内容：创建 Spring Boot 后端基础工程，加入统一响应、Security 基础配置、健康检查接口和首页商品演示接口；创建 Vue3/Vite 前端基础工程，加入首页应用壳、核心模块卡片和答辩演示主线；创建 SQL 目录，补充核心业务表结构草案和演示分类种子数据。
- 验证方式：先编写后端 MockMvc 测试和前端 Vitest 测试；后端执行 `mvn -f backend\pom.xml test`，结果为 3 个测试通过、0 失败；前端执行 `pnpm install`、`pnpm test` 和 `pnpm build`，结果为 1 个测试通过且 Vite 生产构建成功。
- 后续状态：第一阶段代码骨架已创建但尚未提交；后续可继续实现用户认证、商品发布审核、订单流转等业务模块。

## 2026-07-04 15:33 - 完成答辩演示版业务闭环

- 执行 Agent：Codex
- 变动类型：后端接口、前端页面、测试、文档
- 涉及文件：`backend/src/main/java/com/campustrade/demo/`、`backend/src/main/java/com/campustrade/config/SecurityConfig.java`、`backend/src/test/java/com/campustrade/demo/DemoControllerTest.java`、`frontend/src/App.vue`、`frontend/src/styles.css`、`frontend/src/App.spec.ts`、`README.md`、`codex.md`
- 完成内容：新增 `/api/demo/**` 演示接口，覆盖登录、统计、商品发布、商品审核、订单创建、订单流转、消息发送和消息查询；前端升级为可点击的答辩演示工作台，覆盖商品市场、订单交易、消息沟通、后台管理和分类统计；更新 README 为当前真实运行方式。
- 验证方式：执行 `mvn -f backend\pom.xml test`，结果为 5 个测试通过、0 失败；执行 `pnpm test`，结果为 2 个测试通过；执行 `pnpm build`，Vite 生产构建成功。
- 后续状态：答辩演示版已可运行但尚未提交；演示数据目前使用内存服务，后续可继续替换为 MyBatis-Plus + MySQL 持久化实现。

## 2026-07-04 16:35 - 登记后端工业级全局设计文档

- 执行 Agent：Codex
- 变动类型：文档、协作规则
- 涉及文件：`docs/后端工业级业务事务与高并发全景设计.md`、`README.md`、`codex.md`
- 完成内容：按用户要求将 `D:\QQ\后端工业级业务事务与高并发全景设计.md` 复制到项目 `docs/` 目录，并在 README 文档入口和 Agent 工作规则中登记为后端业务、事务一致性、高并发、MySQL、Redis、MQ、缓存、锁、审计、补偿和测试落地的全局参考文档。
- 验证方式：执行源文件与项目内文件 SHA256 校验，两个文件哈希均为 `9ADEB6928A2ACC42D3CBCF8E68E67CC0435F4F02F72E7E747A85079B9272B4A3`。
- 后续状态：后续改进后端时应先对照该全局设计文档；本次仅登记文档和规则，未修改业务代码。

## 2026-07-04 16:44 - 制定混合实训升级版设计方案

- 执行 Agent：Codex
- 变动类型：设计文档、需求方案
- 涉及文件：`docs/superpowers/specs/2026-07-04-campus-trade-mixed-upgrade-design.md`、`codex.md`
- 完成内容：基于课程截图要求、当前可运行演示版项目和后端工业级全局设计文档，制定“混合实训升级版”方案；明确用户、商品、订单、消息、后台、扩展模块的后端边界、前端页面、MySQL 表方向、Redis 使用、接口范围、错误处理、安全原则、测试验收和分阶段实施路线。
- 验证方式：检查设计文档无 `TBD`、`TODO`、`待定` 等占位内容；对照当前项目状态确认方案不把未实现能力写成已完成。
- 后续状态：等待用户审阅设计方案；确认后再进入实施计划和代码改造。

## 2026-07-05 10:36 - 完成后端持久化与 JWT 认证底座

- 执行 Agent：Codex
- 变动类型：实施计划、数据库、后端认证、安全配置、测试
- 涉及文件：`docs/superpowers/plans/2026-07-05-campus-trade-mixed-upgrade.md`、`backend/`、`sql/schema.sql`、`codex.md`
- 完成内容：建立本地功能分支 `feature/campus-trade-complete`；新增 H2 测试 profile 和完整基础表结构；为商品、订单增加版本与逻辑删除字段，新增订单日志和系统通知表；实现统一业务异常、参数校验错误与分页响应模型；实现基于 MyBatis-Plus、BCrypt、JWT 和 Spring Security 的用户注册、登录与当前用户资料接口。
- 验证方式：按 TDD 顺序先观察持久化、异常处理、认证接口测试失败，再补实现；执行后端全量 `mvn test`，结果为 10 个测试通过、0 失败、0 错误。
- 后续状态：下一阶段实现商品持久化 CRUD、筛选排序、收藏、上下架和管理员审核，再接入订单事务状态机。

## 2026-07-05 10:51 - 完成商品持久化与审核业务链

- 执行 Agent：Codex
- 变动类型：商品实体、分页查询、状态流转、管理员审核、安全配置、测试
- 涉及文件：`backend/src/main/java/com/campustrade/product/`、`backend/src/main/java/com/campustrade/admin/`、`backend/src/main/java/com/campustrade/config/`、`backend/src/test/java/com/campustrade/product/`、`backend/pom.xml`、实施计划、`codex.md`
- 完成内容：将首页商品从内存数据升级为 MyBatis-Plus 持久化数据；实现商品发布、编辑、详情、关键词与价格筛选、排序、分页、收藏幂等、下架、重新上架和软删除；实现管理员待审核列表、通过/驳回、审计日志与卖家通知；将商品写接口收紧为登录访问，管理员接口要求 ADMIN 角色。
- 验证方式：先观察商品发布接口 404、编辑接口 405，再补齐实现；商品流程测试覆盖待审核不可公开、审核后可搜索、重复收藏不重复落库、越权编辑被拒绝、上下架和逻辑删除；后端全量测试结果为 12 个测试通过、0 失败、0 错误。
- 后续状态：进入订单交易状态机，实现下单、防重复购买、模拟支付、发货、收货、取消和订单日志。

## 2026-07-05 10:58 - 完成订单事务状态机

- 执行 Agent：Codex
- 变动类型：订单实体、事务服务、状态机、并发控制、接口、测试
- 涉及文件：`backend/src/main/java/com/campustrade/order/`、`backend/src/main/java/com/campustrade/product/ProductMapper.java`、`backend/src/test/java/com/campustrade/order/`、实施计划、`codex.md`
- 完成内容：实现订单创建、模拟支付、卖家发货、买家确认收货和取消；订单金额由后端商品数据确定；商品与订单均使用状态条件和版本号更新；每次流转写入订单日志，取消待付款订单后恢复商品为可售状态。
- 验证方式：先观察订单创建接口返回 404，再补实现；主线测试覆盖非法提前收货、完整成交与日志数量，取消测试覆盖商品恢复；并发测试使用两个线程同时抢购同一商品，验证仅一个事务成功。后端全量测试为 15 个通过、0 失败、0 错误。
- 后续状态：继续实现订单列表、站内消息、系统通知与后台管理统计。
