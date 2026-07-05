# Campus Trade Mixed Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前内存演示版升级为 Vue3 + Spring Boot + MySQL + Redis 的完整校园二手交易实训系统，打通注册登录、商品发布审核、搜索详情、下单支付、发货收货、消息通知和后台统计主线。

**Architecture:** 后端按 auth、user、product、order、message、admin 模块组织，MySQL 保存核心事实数据，Redis 提供验证码、缓存、浏览量、热度、限流和 token 黑名单。前端使用 Vue Router、Pinia、Axios 和独立视图组件，通过统一 API 层访问后端；保留 `/api/demo/**` 作为开发兜底，直到真实业务链路通过测试。

**Tech Stack:** Java 24、Spring Boot 3.5.15、Spring Security、JWT、MyBatis-Plus、MySQL 8、Redis、Vue3、TypeScript、Vite、Pinia、Vue Router、Axios、Vitest、MockMvc。

---

## File Structure

后端新增或调整：

```text
backend/src/main/java/com/campustrade/
  common/
    ApiResponse.java
    BusinessException.java
    ErrorCode.java
    GlobalExceptionHandler.java
    PageResult.java
  security/
    JwtService.java
    JwtAuthenticationFilter.java
    SecurityUser.java
  auth/
    AuthController.java
    AuthService.java
    AuthMapper.java
    UserAccount.java
    AuthDtos.java
  user/
    UserController.java
    UserService.java
    UserMapper.java
    UserAddressMapper.java
    UserProfile.java
    UserAddress.java
    UserDtos.java
  product/
    ProductController.java
    ProductService.java
    ProductMapper.java
    ProductImageMapper.java
    FavoriteMapper.java
    Product.java
    ProductImage.java
    Favorite.java
    ProductDtos.java
  order/
    OrderController.java
    OrderService.java
    OrderMapper.java
    OrderLogMapper.java
    TradeOrder.java
    OrderLog.java
    OrderStatus.java
    OrderDtos.java
  message/
    MessageController.java
    MessageService.java
    MessageMapper.java
    NotificationMapper.java
    SiteMessage.java
    Notification.java
    MessageDtos.java
  admin/
    AdminController.java
    AdminService.java
    AnnouncementMapper.java
    AuditLogMapper.java
    Announcement.java
    AuditLogEntry.java
    AdminDtos.java
```

前端新增或调整：

```text
frontend/src/
  App.vue
  api/http.ts
  api/auth.ts
  api/user.ts
  api/product.ts
  api/order.ts
  api/message.ts
  api/admin.ts
  router/index.ts
  stores/auth.ts
  types/domain.ts
  layouts/AppLayout.vue
  components/AppHeader.vue
  components/ProductCard.vue
  components/StatusTag.vue
  components/EmptyState.vue
  views/LoginView.vue
  views/RegisterView.vue
  views/HomeView.vue
  views/ProductListView.vue
  views/ProductDetailView.vue
  views/PublishProductView.vue
  views/ProfileView.vue
  views/OrderCenterView.vue
  views/MessageCenterView.vue
  views/AdminDashboardView.vue
```

数据库与配置：

```text
sql/schema.sql
sql/seed.sql
backend/src/main/resources/application.yml
backend/src/test/resources/application-test.yml
backend/src/test/resources/schema.sql
```

## Task 1: 建立可测试的持久化基础

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/resources/application-test.yml`
- Create: `backend/src/test/resources/schema.sql`
- Modify: `sql/schema.sql`
- Modify: `sql/seed.sql`

- [ ] **Step 1: 为测试增加 H2 依赖**

在 `backend/pom.xml` 测试依赖区加入：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 写持久化环境启动测试**

创建 `backend/src/test/java/com/campustrade/PersistenceContextTest.java`：

```java
package com.campustrade;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PersistenceContextTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void createsTestDatasource() {
        assertThat(dataSource).isNotNull();
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=PersistenceContextTest test
```

Expected: FAIL，因为 test profile 和 H2 schema 尚未配置。

- [ ] **Step 4: 配置测试数据库**

创建 `backend/src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:campus_trade;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always
  data:
    redis:
      repositories:
        enabled: false

campus-trade:
  redis-enabled: false
  upload-dir: target/test-uploads
```

创建 `backend/src/test/resources/schema.sql`，包含第一轮测试需要的 `users`、`user_addresses`、`categories`、`products`、`product_images`、`favorites`、`orders`、`order_logs`、`messages`、`notifications`、`announcements`、`audit_logs` 表。字段与生产 `sql/schema.sql` 一致，时间字段使用 H2 支持的 `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`。

- [ ] **Step 5: 完善生产数据库字段**

在 `sql/schema.sql` 中为 `products` 和 `orders` 增加：

```sql
version INT NOT NULL DEFAULT 0,
deleted TINYINT NOT NULL DEFAULT 0
```

新增 `order_logs` 和 `notifications`：

```sql
CREATE TABLE IF NOT EXISTS order_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30) NOT NULL,
  operator_id BIGINT NOT NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_order_logs_order_created (order_id, created_at)
);

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(500) NOT NULL,
  read_status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notifications_user_status (user_id, read_status)
);
```

- [ ] **Step 6: 运行测试确认通过**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=PersistenceContextTest test
```

Expected: PASS。

- [ ] **Step 7: Commit**

```powershell
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/test/resources sql
git commit -m "build: add persistence test profile and schema"
```

## Task 2: 统一错误、分页和接口响应

**Files:**
- Modify: `backend/src/main/java/com/campustrade/common/ApiResponse.java`
- Create: `backend/src/main/java/com/campustrade/common/ErrorCode.java`
- Create: `backend/src/main/java/com/campustrade/common/BusinessException.java`
- Create: `backend/src/main/java/com/campustrade/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/campustrade/common/PageResult.java`
- Test: `backend/src/test/java/com/campustrade/common/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: 写异常响应失败测试**

```java
package com.campustrade.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStructuredNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=GlobalExceptionHandlerTest test
```

Expected: FAIL，因为统一异常处理尚不存在。

- [ ] **Step 3: 实现错误模型**

`ErrorCode.java`：

```java
package com.campustrade.common;

public enum ErrorCode {
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "请先登录"),
    FORBIDDEN(40300, "无权执行该操作"),
    PRODUCT_NOT_FOUND(40401, "商品不存在"),
    ORDER_NOT_FOUND(40402, "订单不存在"),
    INVALID_STATE(40901, "当前状态不允许该操作");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }
    public String message() { return message; }
}
```

`BusinessException.java`：

```java
package com.campustrade.common;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
```

`PageResult.java`：

```java
package com.campustrade.common;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long page, long size) {
}
```

- [ ] **Step 4: 实现统一异常处理**

`GlobalExceptionHandler.java`：

```java
package com.campustrade.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        int status = exception.errorCode().code() / 100;
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(exception.errorCode().code(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("请求参数错误");
        return ResponseEntity.badRequest().body(ApiResponse.failure(40000, message));
    }
}
```

同时给 `ApiResponse` 增加：

```java
public static <T> ApiResponse<T> failure(int code, String message) {
    return new ApiResponse<>(code, message, null);
}
```

- [ ] **Step 5: 运行测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=GlobalExceptionHandlerTest test
```

Expected: PASS。

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/campustrade/common backend/src/test/java/com/campustrade/common
git commit -m "feat: add structured errors and pagination"
```

## Task 3: 实现 JWT 注册登录和用户资料

**Files:**
- Create: `backend/src/main/java/com/campustrade/auth/*`
- Create: `backend/src/main/java/com/campustrade/security/*`
- Create: `backend/src/main/java/com/campustrade/user/*`
- Modify: `backend/src/main/java/com/campustrade/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/campustrade/auth/AuthControllerTest.java`
- Test: `backend/src/test/java/com/campustrade/user/UserControllerTest.java`

- [ ] **Step 1: 写注册登录失败测试**

测试必须覆盖：

```java
mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"username":"student01","password":"Pass1234","nickname":"测试学生"}
            """))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.username").value("student01"));

mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"account":"student01","password":"Pass1234"}
            """))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
    .andExpect(jsonPath("$.data.role").value("USER"));
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=AuthControllerTest test
```

Expected: FAIL，接口不存在。

- [ ] **Step 3: 实现用户实体、Mapper 和 DTO**

`UserAccount` 至少包含：

```java
@TableName("users")
public class UserAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String phone;
    private String email;
    private String avatarUrl;
    private String role;
    private String status;
}
```

`AuthDtos` 使用嵌套 record：

```java
public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank String username,
            @Size(min = 8, max = 64) String password,
            @NotBlank String nickname) {}

    public record LoginRequest(@NotBlank String account, @NotBlank String password) {}
    public record LoginResponse(Long userId, String nickname, String role, String accessToken) {}
    public record UserSummary(Long id, String username, String nickname, String role) {}
}
```

- [ ] **Step 4: 实现 JWT 与认证过滤器**

`JwtService` 使用 `jjwt`，签发 claim：

```java
return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("role", role)
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plus(Duration.ofHours(8))))
        .signWith(secretKey)
        .compact();
```

`JwtAuthenticationFilter` 从 `Authorization: Bearer <token>` 读取 token，解析 userId 和 role，创建 `UsernamePasswordAuthenticationToken` 放入 `SecurityContextHolder`。

- [ ] **Step 5: 实现注册登录 Service**

注册流程：

```java
if (authMapper.existsByUsername(request.username())) {
    throw new BusinessException(ErrorCode.BAD_REQUEST);
}
UserAccount user = new UserAccount();
user.setUsername(request.username());
user.setPasswordHash(passwordEncoder.encode(request.password()));
user.setNickname(request.nickname());
user.setRole("USER");
user.setStatus("NORMAL");
authMapper.insert(user);
```

登录流程必须检查账号存在、状态正常、密码匹配，再签发 JWT。

- [ ] **Step 6: 配置 Security**

允许匿名：

```java
.requestMatchers(
    "/api/health",
    "/api/auth/register",
    "/api/auth/login",
    "/api/products",
    "/api/products/*",
    "/doc.html",
    "/v3/api-docs/**",
    "/swagger-ui/**"
).permitAll()
```

管理员接口：

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

其余接口要求认证，并关闭 session：

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

- [ ] **Step 7: 实现个人中心和地址**

`GET /api/users/me` 从认证上下文获取 userId。地址接口提供：

```text
GET    /api/users/me/addresses
POST   /api/users/me/addresses
PUT    /api/users/me/addresses/{id}
DELETE /api/users/me/addresses/{id}
```

所有 SQL 都必须带当前用户 id 条件。

- [ ] **Step 8: 运行认证和用户测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=AuthControllerTest,UserControllerTest test
```

Expected: PASS。

- [ ] **Step 9: Commit**

```powershell
git add backend/src/main/java/com/campustrade/auth backend/src/main/java/com/campustrade/security backend/src/main/java/com/campustrade/user backend/src/main/java/com/campustrade/config/SecurityConfig.java backend/src/test
git commit -m "feat: add jwt authentication and user profile"
```

## Task 4: 实现商品 CRUD、搜索、收藏和审核

**Files:**
- Replace: `backend/src/main/java/com/campustrade/product/ProductController.java`
- Replace: `backend/src/main/java/com/campustrade/product/ProductService.java`
- Create: `backend/src/main/java/com/campustrade/product/Product.java`
- Create: `backend/src/main/java/com/campustrade/product/ProductImage.java`
- Create: `backend/src/main/java/com/campustrade/product/Favorite.java`
- Create: `backend/src/main/java/com/campustrade/product/ProductMapper.java`
- Create: `backend/src/main/java/com/campustrade/product/ProductImageMapper.java`
- Create: `backend/src/main/java/com/campustrade/product/FavoriteMapper.java`
- Create: `backend/src/main/java/com/campustrade/product/ProductDtos.java`
- Test: `backend/src/test/java/com/campustrade/product/ProductFlowTest.java`

- [x] **Step 1: 写商品完整流程测试**

测试覆盖：

```text
卖家发布商品 -> 状态 PENDING
管理员审核通过 -> 状态 APPROVED
游客搜索商品 -> 能找到
用户收藏商品 -> favorite=true
卖家下架商品 -> 状态 OFF_SHELF
```

核心断言：

```java
.andExpect(jsonPath("$.data.status").value("PENDING"));
.andExpect(jsonPath("$.data.records[0].title").value("蓝牙降噪耳机"));
```

- [x] **Step 2: 运行测试确认失败**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=ProductFlowTest test
```

Expected: FAIL。

- [x] **Step 3: 实现商品实体和查询**

商品状态常量：

```java
public enum ProductStatus {
    PENDING, APPROVED, REJECTED, OFF_SHELF, SOLD, DELETED
}
```

列表请求 DTO：

```java
public record ProductQuery(
        String keyword,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort,
        long page,
        long size) {}
```

允许排序值仅限：

```text
priceAsc
priceDesc
newest
hot
```

未知排序值默认 `newest`，不得拼接任意前端 SQL 字段。

- [x] **Step 4: 实现发布、编辑、上下架、收藏**

发布商品时 sellerId 来自认证上下文，初始状态固定为 `PENDING`。审核通过前不进入公开列表。

收藏使用用户与商品联合唯一约束：

```sql
UNIQUE KEY uk_favorites_user_product (user_id, product_id)
```

重复收藏返回已有结果，不产生重复数据。

- [x] **Step 5: 实现管理员审核**

接口：

```text
GET  /api/admin/products/pending
POST /api/admin/products/{id}/audit
```

请求：

```json
{"approved":true,"reason":"信息完整"}
```

审核后写 `audit_logs`，并生成卖家通知。

- [x] **Step 6: 运行商品测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=ProductFlowTest test
```

Expected: PASS。

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/campustrade/product backend/src/main/java/com/campustrade/admin backend/src/test/java/com/campustrade/product sql
git commit -m "feat: add persistent product lifecycle"
```

## Task 5: 实现订单状态机和交易事务

**Files:**
- Create: `backend/src/main/java/com/campustrade/order/*`
- Test: `backend/src/test/java/com/campustrade/order/OrderFlowTest.java`
- Test: `backend/src/test/java/com/campustrade/order/OrderConcurrencyTest.java`

- [x] **Step 1: 写订单主线失败测试**

测试主线：

```text
APPROVED 商品 -> 创建订单 PENDING_PAYMENT
支付 -> PAID
卖家发货 -> SHIPPED
买家收货 -> COMPLETED
```

测试非法流转：

```java
mockMvc.perform(post("/api/orders/{id}/confirm", orderId)
        .header("Authorization", buyerToken))
    .andExpect(status().isConflict())
    .andExpect(jsonPath("$.code").value(40901));
```

- [x] **Step 2: 运行测试确认失败**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=OrderFlowTest test
```

Expected: FAIL。

- [x] **Step 3: 实现订单状态机**

`OrderStatus.java`：

```java
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELED;
            case PAID -> target == SHIPPED || target == CANCELED;
            case SHIPPED -> target == COMPLETED;
            case COMPLETED, CANCELED -> false;
        };
    }
}
```

- [x] **Step 4: 实现创建订单事务**

Service 公共方法标注：

```java
@Transactional
public OrderView createOrder(long buyerId, CreateOrderRequest request)
```

事务内执行：

```text
1. 查询商品并确认 APPROVED。
2. 确认 buyerId != sellerId。
3. 后端读取商品价格。
4. 插入订单 PENDING_PAYMENT。
5. 将商品更新为 SOLD，更新条件包含 id + APPROVED + version。
6. 插入 order_logs。
```

商品条件更新行数为 0 时抛出 `INVALID_STATE`，防止重复下单。

- [x] **Step 5: 实现支付、发货、收货、取消**

每个操作必须校验操作者身份：

```text
支付/收货/取消：买家
发货：卖家
异常处理：管理员
```

取消成功时把商品从 `SOLD` 恢复为 `APPROVED`。

- [x] **Step 6: 运行订单测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=OrderFlowTest,OrderConcurrencyTest test
```

Expected: PASS，且两个用户同时下单只有一个成功。

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/campustrade/order backend/src/test/java/com/campustrade/order sql
git commit -m "feat: add transactional order state machine"
```

## Task 6: 实现消息、通知、公告和后台统计

**Files:**
- Create: `backend/src/main/java/com/campustrade/message/*`
- Complete: `backend/src/main/java/com/campustrade/admin/*`
- Test: `backend/src/test/java/com/campustrade/message/MessageControllerTest.java`
- Test: `backend/src/test/java/com/campustrade/admin/AdminControllerTest.java`

- [x] **Step 1: 写消息与后台失败测试**

覆盖：

```text
用户给卖家留言
接收方看到 UNREAD
接收方标为 READ
普通用户访问 /api/admin/** 返回 403
管理员查看统计和发布公告成功
```

- [x] **Step 2: 实现留言和通知**

留言接口：

```text
GET  /api/messages
POST /api/messages
POST /api/messages/{id}/read
```

发送留言时 senderId 来自认证上下文，receiverId 必须是商品卖家或已有订单参与者。

订单和审核状态变化调用：

```java
notificationService.create(
    userId,
    "ORDER_STATUS",
    "订单状态更新",
    "订单 " + orderNo + " 已变更为 " + status
);
```

- [x] **Step 3: 实现公告和统计**

统计返回：

```java
public record DashboardView(
        long userCount,
        long productCount,
        long orderCount,
        BigDecimal turnover,
        List<CategoryStat> categories) {}
```

公告接口：

```text
GET    /api/announcements
POST   /api/admin/announcements
DELETE /api/admin/announcements/{id}
```

- [x] **Step 4: 运行测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=MessageControllerTest,AdminControllerTest test
```

Expected: PASS。

- [ ] **Step 5: Commit**

```powershell
git add backend/src/main/java/com/campustrade/message backend/src/main/java/com/campustrade/admin backend/src/test
git commit -m "feat: add messaging announcements and admin dashboard"
```

## Task 7: 落地 Redis 验证码、缓存、浏览量和限流

**Files:**
- Create: `backend/src/main/java/com/campustrade/cache/CacheNames.java`
- Create: `backend/src/main/java/com/campustrade/cache/RedisSupport.java`
- Modify: `backend/src/main/java/com/campustrade/auth/AuthService.java`
- Modify: `backend/src/main/java/com/campustrade/product/ProductService.java`
- Test: `backend/src/test/java/com/campustrade/cache/RedisFallbackTest.java`

- [ ] **Step 1: 写 Redis 关闭时降级测试**

在 test profile 关闭 Redis，调用商品详情仍返回 200：

```java
mockMvc.perform(get("/api/products/{id}", productId))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.id").value(productId));
```

- [ ] **Step 2: 定义缓存 key**

```java
public final class CacheNames {
    public static final String PRODUCT_DETAIL = "product:detail:";
    public static final String PRODUCT_VIEW = "product:view:";
    public static final String PRODUCT_HOT = "product:hot";
    public static final String REGISTER_CAPTCHA = "captcha:register:";
    public static final String RESET_CAPTCHA = "captcha:reset:";
    public static final String LOGIN_RATE = "rate:login:";
    public static final String JWT_BLACKLIST = "jwt:blacklist:";

    private CacheNames() {}
}
```

- [ ] **Step 3: 实现可降级 RedisSupport**

当 `campus-trade.redis-enabled=false` 时返回空缓存并跳过写入；开启时使用 `StringRedisTemplate`。

商品详情：

```text
先查 Redis -> 未命中查 MySQL -> 写入 Redis 10 分钟
商品编辑/审核/上下架提交后删除缓存
```

浏览量：

```text
访问详情时 INCR product:view:{id}
后台统计读取 Redis 值与 MySQL 基础值合并
```

- [ ] **Step 4: 实现验证码和登录限流**

验证码开发模式生成固定六位数 `123456`，写 Redis 5 分钟。登录失败计数超过 5 次后锁定 10 分钟。

- [ ] **Step 5: 运行测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=RedisFallbackTest test
```

Expected: PASS。

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/campustrade/cache backend/src/main/java/com/campustrade/auth backend/src/main/java/com/campustrade/product backend/src/test
git commit -m "feat: add redis cache counters and rate limits"
```

## Task 8: 重构前端基础结构

**Files:**
- Modify: `frontend/src/main.ts`
- Replace: `frontend/src/App.vue`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/types/domain.ts`
- Create: `frontend/src/layouts/AppLayout.vue`
- Test: `frontend/src/router/router.spec.ts`

- [ ] **Step 1: 写路由失败测试**

```ts
import { describe, expect, it } from 'vitest'
import { routes } from './index'

describe('routes', () => {
  it('contains required answer-defense pages', () => {
    const names = routes.map((route) => route.name)
    expect(names).toContain('login')
    expect(names).toContain('home')
    expect(names).toContain('product-detail')
    expect(names).toContain('orders')
    expect(names).toContain('admin')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
pnpm test -- src/router/router.spec.ts
```

Expected: FAIL，路由不存在。

- [ ] **Step 3: 实现路由和布局**

路由至少包含：

```ts
export const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/products', name: 'products', component: () => import('../views/ProductListView.vue') },
  { path: '/products/:id', name: 'product-detail', component: () => import('../views/ProductDetailView.vue') },
  { path: '/publish', name: 'publish', component: () => import('../views/PublishProductView.vue'), meta: { auth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
  { path: '/orders', name: 'orders', component: () => import('../views/OrderCenterView.vue'), meta: { auth: true } },
  { path: '/messages', name: 'messages', component: () => import('../views/MessageCenterView.vue'), meta: { auth: true } },
  { path: '/admin', name: 'admin', component: () => import('../views/AdminDashboardView.vue'), meta: { admin: true } }
]
```

`App.vue` 仅保留：

```vue
<template>
  <RouterView />
</template>
```

- [ ] **Step 4: 实现 Axios 与 auth store**

`http.ts`：

```ts
import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('campus-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

auth store 保存 token、nickname、role，并提供 `login`、`logout`、`isAdmin`。

- [ ] **Step 5: 运行测试和构建**

Run:

```powershell
pnpm test -- src/router/router.spec.ts
pnpm build
```

Expected: PASS。

- [ ] **Step 6: Commit**

```powershell
git add frontend/src
git commit -m "refactor: add frontend routing api and auth store"
```

## Task 9: 实现认证、首页和商品页面

**Files:**
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/RegisterView.vue`
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/views/ProductListView.vue`
- Create: `frontend/src/views/ProductDetailView.vue`
- Create: `frontend/src/views/PublishProductView.vue`
- Create: `frontend/src/components/ProductCard.vue`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/api/product.ts`
- Test: `frontend/src/views/ProductFlow.spec.ts`

- [ ] **Step 1: 写商品页面失败测试**

```ts
it('renders filters and product actions', () => {
  const wrapper = mount(ProductListView)
  expect(wrapper.text()).toContain('关键词')
  expect(wrapper.text()).toContain('价格排序')
  expect(wrapper.text()).toContain('热度排序')
})
```

- [ ] **Step 2: 实现登录和注册**

登录页提交：

```ts
const result = await loginApi({
  account: form.account,
  password: form.password
})
authStore.saveSession(result)
router.push('/')
```

注册页包含账号、昵称、密码、验证码；验证码按钮调用 `/auth/captcha/register`。

- [ ] **Step 3: 实现首页和列表**

首页展示：

```text
公告栏
热门商品
最新商品
分类入口
```

列表查询参数与后端保持一致：

```ts
{
  keyword,
  categoryId,
  minPrice,
  maxPrice,
  sort,
  page,
  size
}
```

- [ ] **Step 4: 实现详情、收藏、购买和留言入口**

详情页必须展示图片轮播、价格、卖家、浏览量、收藏按钮、立即购买、留言按钮。

- [ ] **Step 5: 实现商品发布**

发布表单包含标题、分类、价格、成色、描述和最多 6 张图片。第一轮图片上传使用本地 multipart 接口。

- [ ] **Step 6: 运行测试和构建**

Run:

```powershell
pnpm test -- src/views/ProductFlow.spec.ts
pnpm build
```

Expected: PASS。

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/views frontend/src/components frontend/src/api
git commit -m "feat: add authentication and product pages"
```

## Task 10: 实现个人中心、订单、消息和后台页面

**Files:**
- Create: `frontend/src/views/ProfileView.vue`
- Create: `frontend/src/views/OrderCenterView.vue`
- Create: `frontend/src/views/MessageCenterView.vue`
- Create: `frontend/src/views/AdminDashboardView.vue`
- Create: `frontend/src/api/user.ts`
- Create: `frontend/src/api/order.ts`
- Create: `frontend/src/api/message.ts`
- Create: `frontend/src/api/admin.ts`
- Create: `frontend/src/components/StatusTag.vue`
- Create: `frontend/src/components/DataStatCard.vue`
- Test: `frontend/src/views/BusinessPages.spec.ts`

- [ ] **Step 1: 写业务页面失败测试**

覆盖页面文字：

```ts
expect(profile.text()).toContain('收货地址')
expect(orders.text()).toContain('待支付')
expect(messages.text()).toContain('未读消息')
expect(admin.text()).toContain('商品审核')
```

- [ ] **Step 2: 实现个人中心**

包括：

```text
资料编辑
头像上传
地址 CRUD
我的商品
我的收藏
```

- [ ] **Step 3: 实现订单中心**

按买家订单/卖家订单分栏。按钮根据状态显示：

```text
PENDING_PAYMENT: 支付、取消
PAID: 卖家发货、买家取消申请
SHIPPED: 买家确认收货
COMPLETED/CANCELED: 查看详情
```

- [ ] **Step 4: 实现消息中心**

分为“交易留言”和“系统通知”，支持单条已读和全部已读。

- [ ] **Step 5: 实现后台**

后台使用 tabs：

```text
数据概览
用户管理
商品审核
订单管理
公告管理
```

统计图表使用已有 ECharts，至少展示分类占比和近 7 日成交趋势。

- [ ] **Step 6: 运行测试和构建**

Run:

```powershell
pnpm test -- src/views/BusinessPages.spec.ts
pnpm build
```

Expected: PASS。

- [ ] **Step 7: Commit**

```powershell
git add frontend/src
git commit -m "feat: add profile orders messages and admin pages"
```

## Task 11: 扩展敏感词、举报和轻量推荐

**Files:**
- Create: `backend/src/main/java/com/campustrade/extension/ContentReviewService.java`
- Create: `backend/src/main/java/com/campustrade/extension/RecommendationService.java`
- Create: `backend/src/main/java/com/campustrade/extension/ReportController.java`
- Create: `backend/src/main/java/com/campustrade/extension/ReportService.java`
- Test: `backend/src/test/java/com/campustrade/extension/ExtensionFlowTest.java`

- [ ] **Step 1: 写扩展功能测试**

覆盖：

```text
包含敏感词的商品发布返回 400
用户可以举报商品
管理员可以处理举报
推荐接口优先返回同分类热门商品
```

- [ ] **Step 2: 实现敏感词过滤**

第一轮从 `sensitive_words` 表读取启用词，服务启动后缓存到内存集合。匹配商品标题、描述和留言内容。

- [ ] **Step 3: 实现举报**

接口：

```text
POST /api/reports
GET  /api/admin/reports
POST /api/admin/reports/{id}/resolve
```

- [ ] **Step 4: 实现推荐**

优先级：

```text
用户最近收藏分类
用户最近浏览分类
热门商品
最新商品
```

不足数量时逐级补齐，排除已下架和已售出商品。

- [ ] **Step 5: 运行测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml -Dtest=ExtensionFlowTest test
```

Expected: PASS。

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/campustrade/extension backend/src/test sql
git commit -m "feat: add content review reports and recommendations"
```

## Task 12: 文档、全量验证和答辩演示

**Files:**
- Modify: `README.md`
- Modify: `codex.md`
- Create: `docs/需求分析.md`
- Create: `docs/系统设计.md`
- Create: `docs/数据库设计.md`
- Create: `docs/接口清单.md`
- Create: `docs/测试报告.md`
- Create: `docs/答辩演示路线.md`

- [ ] **Step 1: 更新 README**

README 必须包含：

```text
环境要求
MySQL 初始化命令
Redis 启动方式
后端启动命令
前端启动命令
演示账号
接口文档地址
测试命令
```

- [ ] **Step 2: 编写答辩演示路线**

固定为：

```text
注册登录 -> 发布商品 -> 管理员审核 -> 搜索详情 -> 收藏下单
-> 模拟支付 -> 卖家发货 -> 买家收货 -> 消息通知 -> 后台统计
```

- [ ] **Step 3: 执行后端全量测试**

Run:

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml test
```

Expected: `BUILD SUCCESS`，0 failures。

- [ ] **Step 4: 执行前端全量测试与构建**

Run:

```powershell
cd frontend
pnpm test
pnpm build
```

Expected: 所有 Vitest 测试通过，Vite 构建成功。

- [ ] **Step 5: 启动并人工验证**

后端：

```powershell
& 'D:\java\idea\IntelliJ IDEA Community Edition 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd' -f backend\pom.xml spring-boot:run
```

前端：

```powershell
cd frontend
pnpm dev --host 127.0.0.1 --port 5173
```

检查：

```text
http://127.0.0.1:5173
http://127.0.0.1:8080/api/health
http://127.0.0.1:8080/doc.html
```

- [ ] **Step 6: 更新协作记录并提交**

```powershell
git add README.md docs codex.md
git commit -m "docs: add delivery and answer defense documentation"
```
