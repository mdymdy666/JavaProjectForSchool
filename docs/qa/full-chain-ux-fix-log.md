# 校园二手交易平台全链路 UX 修复记录

审计时间：2026-07-10 15:55  
执行 Agent：Codex  
初始 Git 状态记录：开始前存在 `M codex.md` 与 `?? start-all.ps1`，均为既有变动；本轮未删除、覆盖或格式化这些文件。

## 问题编号：UX-001

- 发现时间：2026-07-10 14:10
- 发现方式：UI 测试
- 用户角色：游客 / 普通用户 / 管理员
- 页面路径：`/products`、`/home`、`/favorites`、`/orders`、`/pay/order/:orderId`、`/admin`
- 业务链路：浏览商品、收藏、下单、支付、后台审核与统计
- 问题现象：价格为 `0.01` 的商品在列表卡片和部分页面显示为 `¥0`，订单金额显示口径不一致。
- 预期行为：金额统一保留两位小数，`0.01` 必须显示为 `¥0.01`。
- 实际行为：部分页面使用整数格式化或直接拼接，导致小数金额丢失。
- 严重级别：P1
- 涉及文件：`frontend/src/utils/money.ts`、`frontend/src/components/ProductCard.vue`、`frontend/src/views/HomeView.vue`、`frontend/src/views/CartView.vue`、`frontend/src/views/OrderCenterView.vue`、`frontend/src/views/PaymentView.vue`、`frontend/src/views/AdminDashboardView.vue`
- 修复方案：新增统一 `formatMoney` 工具，替换卡片、首页、收藏、购物车、订单、支付和后台页面的金额展示逻辑。
- 修复状态：已修复
- UI 验证方式：发布并审核 `0.01` 元商品后，在市场页验证商品卡显示 `¥0.01`；截图：`output/qa/full-chain-ux/ui-market-cent-price.png`
- 自动化测试：`ProductCard.spec.ts` 增加 `0.01` 展示断言；`pnpm test` 44 项通过。
- 后续风险：后续新增金额页面时需继续使用统一工具，避免重新出现整数化格式。

## 问题编号：UX-002

- 发现时间：2026-07-10 14:18
- 发现方式：UI 测试
- 用户角色：买家
- 页面路径：`/orders`、`/pay/:id`
- 业务链路：订单中心 → 支付页 → 模拟支付
- 问题现象：订单中心点击支付后进入 `/pay/4`，页面按商品 ID 读取数据，显示了错误商品和错误金额。
- 预期行为：已存在订单支付页必须按订单 ID 读取订单详情，不能误按商品 ID 创建或展示订单。
- 实际行为：商品直接购买和订单支付共用 `/pay/:id`，发生订单 ID / 商品 ID 语义冲突。
- 严重级别：P0
- 涉及文件：`backend/src/main/java/com/campustrade/order/OrderController.java`、`backend/src/main/java/com/campustrade/order/OrderService.java`、`frontend/src/router/index.ts`、`frontend/src/api/order.ts`、`frontend/src/views/PaymentView.vue`、`frontend/src/views/OrderCenterView.vue`、`frontend/src/views/ProductDetailView.vue`、`frontend/src/views/CartView.vue`
- 修复方案：新增 `GET /api/orders/{id}`；拆分支付路由为 `/pay/order/:orderId`、`/pay/product/:productId`、`/pay/batch`，保留旧 `/pay/:orderId` 重定向；支付页按路由类型分别读取订单或商品。
- 修复状态：已修复
- UI 验证方式：买家创建待支付订单后进入 `/pay/order/6`，页面显示原订单商品 `Nike 双肩包 黑色` 与 `¥120.00`，点击确认支付出现支付成功；截图：`output/qa/full-chain-ux/ui-payment-order-detail.png`
- 自动化测试：`OrderFlowTest` 覆盖订单详情；`PaymentView.spec.ts` 覆盖已有订单支付不再创建新订单。
- 后续风险：批量支付仍是前端顺序模拟创建订单，后续可补批量事务接口。

## 问题编号：UX-003

- 发现时间：2026-07-10 14:24
- 发现方式：代码审计 / UI 测试
- 用户角色：管理员
- 页面路径：`/admin`
- 业务链路：后台管理 → 商品审核
- 问题现象：待审核列表只显示标题、卖家、价格和成色，管理员无法看到描述、分类、图片、发布时间等判断依据。
- 预期行为：审核页必须展示标题、分类、价格、成色、描述、图片、卖家信息和发布时间。
- 实际行为：后端待审核接口返回卡片摘要，前端审核卡信息不足。
- 严重级别：P1
- 涉及文件：`backend/src/main/java/com/campustrade/admin/AdminProductController.java`、`backend/src/main/java/com/campustrade/product/ProductService.java`、`frontend/src/api/admin.ts`、`frontend/src/views/AdminDashboardView.vue`
- 修复方案：管理员待审核接口返回 `ProductDetail`；后台审核卡展示封面图、描述、分类、价格、成色、卖家、发布时间和图片数量。
- 修复状态：已修复
- UI 验证方式：管理员审核页打开 0.01 元待审核商品，完整信息可见并可审核通过；截图：`output/qa/full-chain-ux/ui-admin-audit-detail.png`
- 自动化测试：`ProductFlowTest` 增加管理员待审核详情字段断言；`AdminDashboardView.spec.ts` 保持通过。
- 后续风险：管理员批量审核仍未实现，适合作为 P3 增强。

## 问题编号：UX-004

- 发现时间：2026-07-10 14:29
- 发现方式：UI 测试
- 用户角色：卖家
- 页面路径：`/profile`
- 业务链路：个人中心 → 我的商品 → 查看/编辑商品
- 问题现象：我的商品列表只显示标题和英文状态，缺少描述、图片、分类、成色、价格、浏览量、收藏数、审核原因和操作入口。
- 预期行为：卖家能完整管理自己发布的商品，并清楚看到审核状态和可执行操作。
- 实际行为：个人中心无法支撑真实商品管理。
- 严重级别：P1
- 涉及文件：`backend/src/main/java/com/campustrade/user/UserController.java`、`frontend/src/types/domain.ts`、`frontend/src/api/user.ts`、`frontend/src/views/ProfileView.vue`、`frontend/src/views/PublishProductView.vue`
- 修复方案：扩展 `/api/users/me/products` 返回描述、封面、分类、价格、浏览量、收藏数和审核原因；个人中心改为富信息商品行，增加查看详情和编辑入口；发布页支持编辑模式。
- 修复状态：已修复
- UI 验证方式：卖家发布待审核 0.01 元商品后，个人中心我的商品展示标题、描述、图片、分类、成色、价格、状态、浏览量、收藏数和操作入口；截图：`output/qa/full-chain-ux/ui-profile-products.png`
- 自动化测试：`ProfileView.spec.ts` 增加我的商品金额和举报记录断言；`PublishProductView.spec.ts` 补编辑依赖 mock。
- 后续风险：商品编辑后重新审核的文案可继续细化。

## 问题编号：UX-005

- 发现时间：2026-07-10 14:35
- 发现方式：代码审计 / UI 测试
- 用户角色：普通用户 / 买家
- 页面路径：`/products/:id`、`/favorites`
- 业务链路：商品详情 → 收藏 → 我的收藏 → 取消收藏
- 问题现象：商品详情有收藏动作，但缺少明确收藏页面和导航入口，用户收藏后无处管理。
- 预期行为：登录用户可从导航或个人入口进入收藏列表，查看已收藏商品并取消收藏。
- 实际行为：收藏链路断在动作反馈之后。
- 严重级别：P1
- 涉及文件：`backend/src/main/java/com/campustrade/user/UserController.java`、`frontend/src/router/index.ts`、`frontend/src/components/AppHeader.vue`、`frontend/src/views/FavoriteListView.vue`、`frontend/src/api/user.ts`
- 修复方案：新增 `/api/users/me/favorites`；新增 `/favorites` 页面和导航入口，展示封面、状态、价格、分类、成色、卖家、浏览量，并支持取消收藏和空状态。
- 修复状态：已修复
- UI 验证方式：买家收藏 0.01 元商品后进入收藏页，商品显示 `¥0.01`，点击取消收藏后从列表移除；截图：`output/qa/full-chain-ux/ui-favorites-page.png`
- 自动化测试：`FavoriteListView.spec.ts`、`AppHeader.spec.ts`、`router.spec.ts`、`api/contract.spec.ts` 覆盖收藏入口、路由和接口。
- 后续风险：已售出/下架商品保留展示状态，后续可补筛选。

## 问题编号：UX-006

- 发现时间：2026-07-10 14:42
- 发现方式：代码审计
- 用户角色：卖家 / 管理员
- 页面路径：`/products/:id`、`/products/:id/edit`、`/admin`
- 业务链路：待审核商品 → 审核驳回 → 卖家查看原因 → 修改商品
- 问题现象：待审核/驳回商品虽可通过权限查看，但缺少审核原因展示和明确编辑入口，卖家无法形成闭环。
- 预期行为：卖家可查看自己待审核或驳回商品详情；驳回商品能看到原因并进入编辑。
- 实际行为：状态可见但后续动作和原因不清晰。
- 严重级别：P1
- 涉及文件：`backend/src/main/java/com/campustrade/product/ProductDtos.java`、`backend/src/main/java/com/campustrade/product/ProductMapper.java`、`backend/src/main/java/com/campustrade/product/ProductService.java`、`frontend/src/router/index.ts`、`frontend/src/views/ProductDetailView.vue`、`frontend/src/views/PublishProductView.vue`
- 修复方案：商品详情增加最近驳回原因；商品详情 owner bar 增加编辑入口；发布页根据 `/products/:id/edit` 加载原商品并提交编辑。
- 修复状态：已修复
- UI 验证方式：卖家发布商品后进入待审核详情，价格、描述、图片和编辑入口可见；截图：`output/qa/full-chain-ux/ui-pending-detail.png`
- 自动化测试：`ProductFlowTest` 覆盖待审核详情和审核信息；`router.spec.ts` 覆盖编辑路由权限。
- 后续风险：驳回后自动重新提交审核策略仍依赖现有编辑接口状态规则，后续可增加更明确的“重新提交审核”按钮。

## 问题编号：UX-007

- 发现时间：2026-07-10 14:50
- 发现方式：代码审计 / UI 测试
- 用户角色：普通用户 / 管理员
- 页面路径：`/products/:id`、`/profile`、`/admin`
- 业务链路：商品详情 → 提交举报 → 用户查看举报状态 → 管理员处理
- 问题现象：用户可以提交举报，但无法查看自己的举报状态；管理员处理后用户侧缺少可追踪入口。
- 预期行为：用户能查看自己提交过的举报、状态和处理结果；管理员能看到举报商品、举报人、卖家、原因和处理动作。
- 实际行为：举报提交后只停留在一次性反馈。
- 严重级别：P1
- 涉及文件：`backend/src/main/java/com/campustrade/extension/ReportController.java`、`backend/src/main/java/com/campustrade/extension/ReportService.java`、`frontend/src/api/user.ts`、`frontend/src/types/domain.ts`、`frontend/src/views/ProfileView.vue`、`frontend/src/views/AdminDashboardView.vue`
- 修复方案：新增 `GET /api/reports/my`；个人中心新增“我的举报”标签；后台举报处理保留原因、处理结果和状态。
- 修复状态：已修复
- UI 验证方式：买家提交举报后在个人中心“我的举报”看到待处理记录；管理员处理后后台显示处理反馈；截图：`output/qa/full-chain-ux/ui-reports-status.png`、`output/qa/full-chain-ux/ui-admin-report-process.png`
- 自动化测试：`ExtensionFlowTest` 覆盖我的举报接口；`ProfileView.spec.ts` 覆盖举报记录渲染。
- 后续风险：用户侧处理后刷新通知可进一步增强。

## 问题编号：UX-008

- 发现时间：2026-07-10 14:57
- 发现方式：代码审计 / UI 测试
- 用户角色：游客 / 普通用户 / 管理员
- 页面路径：`/login`、受保护路由
- 业务链路：未登录访问发布/收藏/下单/举报 → 登录 → 回到原路径
- 问题现象：登录成功后固定跳转首页，用户从受保护动作进入登录后不能回到原业务上下文。
- 预期行为：登录页应尊重 `redirect` 参数，登录后回到用户原路径。
- 实际行为：登录后丢失原业务意图。
- 严重级别：P2
- 涉及文件：`frontend/src/views/LoginView.vue`
- 修复方案：登录成功后优先读取 `route.query.redirect`，无 redirect 时才回首页。
- 修复状态：已修复
- UI 验证方式：使用 `/login?redirect=/products` 登录后回到商品市场，导航显示用户态和收藏入口。
- 自动化测试：路由鉴权测试保持通过。
- 后续风险：若 redirect 指向不存在路由，仍由 Vue Router 默认处理。

## 问题编号：UX-009

- 发现时间：2026-07-10 15:05
- 发现方式：代码审计 / UI 测试
- 用户角色：买家 / 管理员
- 页面路径：`/products/:id`、`/cart`、`/pay/product/:productId`
- 业务链路：商品详情/购物车 → 直接购买/单品支付
- 问题现象：购物车单品支付和商品详情直接购买沿用旧 `/pay/:id`，与订单支付语义混淆。
- 预期行为：商品支付入口应明确是商品 ID，订单支付入口应明确是订单 ID。
- 实际行为：入口可用但隐藏了后续错误风险。
- 严重级别：P1
- 涉及文件：`frontend/src/views/ProductDetailView.vue`、`frontend/src/views/CartView.vue`、`frontend/src/views/PaymentView.vue`、`frontend/src/router/index.ts`
- 修复方案：商品入口统一跳转 `/pay/product/:productId`；支付页按商品路径创建订单后支付。
- 修复状态：已修复
- UI 验证方式：买家从详情页可看到立即购买，订单中心支付已按 `/pay/order/:orderId` 验证，购物车入口路由已由单元测试覆盖。
- 自动化测试：`router.spec.ts`、`api/contract.spec.ts`、`PaymentView.spec.ts` 覆盖新路由和接口。
- 后续风险：购物车批量结算仍可补后端事务接口与 UI 选择态回归截图。

## 问题编号：UX-010

- 发现时间：2026-07-10 16:16
- 发现方式：用户指定 / 代码审计 / 自动化测试
- 用户角色：买家 / 收藏用户
- 页面路径：`/favorites`、`/products/:id`
- 业务链路：收藏商品 → 商品被卖出或下架 → 用户取消收藏
- 问题现象：已售出或非在售商品仍保留在收藏列表里，但点击取消收藏会被后端“当前商品不可收藏”状态校验拦截。
- 预期行为：非在售商品不能新增收藏，但用户必须能取消自己已有的收藏。
- 实际行为：收藏 toggle 接口先校验商品必须 `APPROVED`，导致已有收藏无法取消。
- 严重级别：P1
- 涉及文件：`backend/src/main/java/com/campustrade/product/ProductService.java`、`backend/src/test/java/com/campustrade/product/ProductFlowTest.java`
- 修复方案：调整收藏接口顺序，先检查是否已有收藏；已有收藏时允许删除并返回 `favorite=false`，仅在新增收藏时要求商品为 `APPROVED`。
- 修复状态：已修复
- UI 验证方式：收藏列表保留取消收藏按钮；后端回归覆盖商品状态改为 `SOLD` 后仍可取消已有收藏。
- 自动化测试：`ProductFlowTest` 覆盖已售出商品可取消已有收藏、不能重新新增收藏。
- 后续风险：若后续新增多收藏类型，需要沿用“取消优先于状态校验”的规则。

## 问题编号：UX-011

- 发现时间：2026-07-10 16:16
- 发现方式：用户指定 / 代码审计 / 自动化测试
- 用户角色：普通用户 / 多账号用户
- 页面路径：`/cart`、全局导航购物车徽标
- 业务链路：账号 A 加入购物车 → 切换账号 B → 查看购物车
- 问题现象：购物车使用全局 localStorage key `campus-cart`，账号切换后账号 B 会看到账号 A 的购物车。
- 预期行为：购物车应按账号隔离，账号切换后加载当前账号自己的购物车；登出后切换到游客购物车。
- 实际行为：所有账号共享同一份本地购物车。
- 严重级别：P1
- 涉及文件：`frontend/src/stores/cart.ts`、`frontend/src/stores/auth.ts`、`frontend/src/stores/cart.spec.ts`、`frontend/src/views/ProductDetailView.spec.ts`
- 修复方案：购物车改为按 `campus-cart:{userId}` 存储，游客使用 `campus-cart:guest`；登录、切换账号、登出时调用 `cart.switchOwner()` 重新加载对应分桶，并清理旧全局 key。
- 修复状态：已修复
- UI 验证方式：前端 store 测试模拟账号 101/202 切换，验证两边购物车互不串号。
- 自动化测试：新增 `cart.spec.ts`；更新商品详情购物车测试断言新分桶 key；`pnpm test` 45 项通过。
- 后续风险：如果未来购物车改为服务端持久化，需要迁移当前本地分桶数据。
