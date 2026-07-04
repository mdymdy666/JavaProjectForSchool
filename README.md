# 校园二手物品交易平台的设计与实现

本仓库用于《徐士伟-软件与理论综合设计与实践项目指导书【徐士伟-题目2-2026】》对应项目的初始化、文档沉淀与后续开发协作。

当前阶段只完成项目初始化、文档规范和开发环境检查，尚未创建 Vue3 前端项目或 Spring Boot 后端项目，也尚未实现任何业务代码。

## 项目定位

项目要求建设一个基于 Vue3 + Spring Boot + MySQL + Redis 的校园 C2C 二手物品交易 Web 应用，采用前后端分离架构。后续计划围绕用户管理、商品管理、订单交易、消息沟通、后台管理、数据统计、公告管理等模块开展需求分析、系统设计、编码、联调与测试。

## 当前状态

| 项目 | 状态 | 说明 |
|---|---|---|
| Git 仓库 | 已初始化 | 已创建 `.git` |
| 忽略规则 | 已创建 | 已创建 `.gitignore` |
| 文本属性 | 已创建 | 已创建 `.gitattributes` |
| 文档规范 | 已创建 | 文档统一放在 `docs/` |
| Agent 协作记录 | 已创建 | 根目录 `codex.md` |
| 依赖检测脚本 | 已创建 | `scripts/check-dependencies.bat` |
| 后端项目 | 未配置 | 尚未创建 `pom.xml` |
| 前端项目 | 未配置 | 尚未创建 `package.json` |
| 数据库脚本 | 未配置 | 后续数据库设计完成后再创建 |

## 文档入口

- [项目说明](docs/项目说明.md)
- [文档规范](docs/文档规范.md)
- [开发环境](docs/开发环境.md)
- [依赖版本清单](docs/依赖版本清单.md)
- [环境安装与版本对齐](docs/环境安装与版本对齐.md)
- [目录结构规范](docs/目录结构规范.md)
- [接口文档规范](docs/接口文档规范.md)
- [数据库设计规范](docs/数据库设计规范.md)
- [测试规范](docs/测试规范.md)
- [协作记录规范](docs/协作记录规范.md)

## 后续建议

1. 先完成需求分析、角色用例、业务流程、ER 图和接口草案。
2. 再创建后端 Spring Boot 项目和前端 Vue3 项目。
3. 创建项目配置时，使用 `.env.example`、`application-example.yml` 等样例文件记录配置项，不提交真实账号、密码、密钥。
4. 每次文档、代码或环境配置变更后，按 `codex.md` 记录本次变动。

## 依赖验证

此前曾使用独立 smoke test 验证本机工具、Spring Boot 配置型依赖、Vue3/Vite 配置型依赖、MySQL、Redis 和 Apifox 可运行性；当前已按要求删除 `test/` 目录。正式前端和后端工程仍未创建，后续如需复验依赖，应重新在临时测试目录中执行，不要把 smoke test 写成业务工程。

当前仓库提供 Windows 依赖检测脚本，用于组员在不同软件版本、不同安装路径下检查本机环境是否可用于本项目：

```powershell
scripts\check-dependencies.bat
```

可选参数：

```powershell
scripts\check-dependencies.bat --no-pause
scripts\check-dependencies.bat --verbose
```

脚本会优先检查 PATH，再检查常见环境变量和安装目录，并生成 Markdown 报告：

```text
reports/dependency-check-report.md
```

脚本只做检测和报告生成，不会安装、卸载或重装软件，不会修改系统 PATH，不会修改 Java、MySQL、Redis 配置，也不会写入真实账号、密码或密钥。
