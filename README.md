# 校园二手物品交易平台的设计与实现

本项目面向《软件与理论综合设计与实践》课程实训，是一个基于 Vue3 + Spring Boot + MySQL + Redis 的校园 C2C 二手物品交易 Web 应用，采用前后端分离架构。

## 当前状态

| 模块 | 状态 | 说明 |
|---|---|---|
| 后端 | ✅ 已完成 | Spring Boot 3.5 + MyBatis-Plus + JWT + Spring Security + Redis（带降级），14 张业务表 |
| 前端 | ✅ 已完成 | Vue3 + Vite + Pinia + Vue Router + Axios，10 个独立视图 |
| 数据库 | ✅ 已创建 | `sql/schema.sql`（14张表）、`sql/seed.sql`（分类种子数据+管理员账号） |
| Redis | ✅ 已接入 | 验证码、商品详情缓存、浏览量计数、热度排行、登录限流、JWT 黑名单 |
| 测试 | ✅ 已配置 | 后端 24 个 MockMvc 测试，前端 5 个 Vitest 测试 |

## 业务功能

- **用户认证**：注册（验证码）、登录（JWT）、登出（黑名单）、登录限流
- **商品市场**：发布、搜索筛选排序、详情、收藏、审核（通过/驳回）、上下架
- **订单交易**：下单、模拟支付、发货（物流信息）、确认收货、取消，乐观锁防并发
- **消息通知**：买卖双方留言、系统通知、已读标记、敏感词过滤
- **后台管理**：数据概览、商品审核、公告管理
- **扩展功能**：敏感词过滤、用户举报、商品推荐

## 环境要求

- Java 24+、Maven 3.9+
- Node.js 22+、pnpm 9+
- MySQL 8.0+、Redis（可选，Redis 不可用时自动降级）

## 快速启动

### 1. 初始化数据库

```sql
-- 执行建表脚本
SOURCE sql/schema.sql;
-- 执行种子数据（分类 + 管理员账号 admin / admin123）
SOURCE sql/seed.sql;
```

### 2. 配置数据库连接

编辑 `backend/src/main/resources/application.yml`，修改 MySQL 用户名和密码。

### 3. 启动后端

```powershell
$env:JAVA_HOME = "D:\java"
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml spring-boot:run
```

### 4. 启动前端

```powershell
cd frontend
pnpm install
pnpm dev
```

### 5. 访问

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8080/api/health
- 接口文档（Knife4j）：http://localhost:8080/doc.html

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | admin123 |
| 普通用户 | 自行注册 | - |

## 答辩演示主线

注册登录 → 发布商品 → 管理员审核 → 搜索浏览 → 收藏下单 → 模拟支付 → 卖家发货 → 买家收货 → 消息通知 → 后台统计

## 测试

```powershell
# 后端测试（24 个）
$env:JAVA_HOME = "D:\java"
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml test

# 前端测试 + 构建（5 个测试）
cd frontend
pnpm test
pnpm build
```

## 目录结构

```text
backend/    Spring Boot 后端工程（56 个 Java 源文件，13 个测试类）
frontend/   Vue3 前端工程（10 个视图，4 个组件，5 个 API 模块）
sql/        MySQL 建表和种子数据脚本
docs/       项目文档和环境说明
codex.md    协作记录
```

## 文档入口

- [项目说明](docs/项目说明.md)
- [后端工业级业务事务与高并发全景设计](docs/后端工业级业务事务与高并发全景设计.md)
- [环境安装与版本对齐](docs/环境安装与版本对齐.md)
- [接口文档规范](docs/接口文档规范.md)
- [数据库设计规范](docs/数据库设计规范.md)
