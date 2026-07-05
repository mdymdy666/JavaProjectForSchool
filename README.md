# 校园二手物品交易平台的设计与实现

本项目面向《软件与理论综合设计与实践》课程实训，目标是实现一个基于 Vue3 + Spring Boot + MySQL + Redis 的校园 C2C 二手物品交易 Web 应用。当前版本已经具备答辩展示可用的前后端工程、演示业务接口、前端交互工作台和数据库建表脚本。

## 当前状态

| 模块 | 状态 | 说明 |
|---|---|---|
| 前端 | 已创建 | Vue3 + Vite + TypeScript，包含商品、订单、消息、后台管理和统计展示 |
| 后端 | 已创建 | Spring Boot 3，提供健康检查、商品演示接口和 `/api/demo/**` 演示业务接口 |
| 数据库 | 已创建脚本 | `sql/schema.sql`、`sql/seed.sql` 覆盖用户、商品、订单、消息、公告、审计日志等表 |
| Redis | 已预留 | 当前演示接口使用内存数据，后续可接入 Redis 做缓存、验证码或会话 |
| 测试 | 已配置 | 后端 MockMvc 测试，前端 Vitest 测试 |

## 演示功能

- 用户登录：提供管理员和普通用户演示登录接口。
- 商品市场：展示审核通过的二手商品，可直接创建订单。
- 商品发布：前端可快速发布演示商品，进入待审核状态。
- 后台审核：管理员可通过或驳回待审核商品。
- 订单交易：支持创建订单，并按待支付、已支付、已发货、已完成推进状态。
- 消息沟通：支持买卖双方留言、未读和已读状态展示。
- 数据统计：展示注册用户、在售商品、订单数量、成交金额和分类占比。

## 运行方式

后端：

```powershell
cd D:\实训\JavaProjectForSchool
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml spring-boot:run
```

前端：

```powershell
cd D:\实训\JavaProjectForSchool\frontend
pnpm install
pnpm dev
```

默认访问：

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8080/api/health
- 后端演示数据：http://localhost:8080/api/demo/dashboard

## 常用验证命令

```powershell
cd D:\实训\JavaProjectForSchool
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml test

cd D:\实训\JavaProjectForSchool\frontend
pnpm test
pnpm build
```

## 目录结构

```text
backend/   Spring Boot 后端工程
frontend/  Vue3 前端工程
sql/       MySQL 建表和种子数据脚本
docs/      项目文档和环境说明
codex.md   协作记录
```

## 文档入口

- [项目说明](docs/项目说明.md)
- [后端工业级业务事务与高并发全景设计](docs/后端工业级业务事务与高并发全景设计.md)
- [开发环境](docs/开发环境.md)
- [环境安装与版本对齐](docs/环境安装与版本对齐.md)
- [依赖版本清单](docs/依赖版本清单.md)
- [接口文档规范](docs/接口文档规范.md)
- [数据库设计规范](docs/数据库设计规范.md)
- [测试规范](docs/测试规范.md)
- [协作记录规范](docs/协作记录规范.md)
