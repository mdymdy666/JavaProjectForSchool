import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

const artifactToolPath = "C:/Users/邓先毅/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs";
const { Presentation, PresentationFile } = await import(pathToFileURL(artifactToolPath).href);

const OUT = "D:/Downloads/JavaProjectForSchool-main/output/campus-trade-defense.pptx";
const QA = "D:/Downloads/JavaProjectForSchool-main/output/ppt-build/qa";
const HERO = "D:/Downloads/JavaProjectForSchool-main/frontend/src/assets/campus-market-hero.png";

const W = 1280;
const H = 720;
const M = 54;
const ink = "#111111";
const muted = "#555555";
const panel = "#EFEFEF";
const rule = "#B8BCC4";
const accent = "#FF6B35";
const dark = "#202020";

async function writeBlob(file, blob) {
  await fs.writeFile(file, new Uint8Array(await blob.arrayBuffer()));
}

async function imageBytes(file) {
  const bytes = await fs.readFile(file);
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
}

function addText(slide, text, x, y, w, h, opts = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    fontFace: "Microsoft YaHei",
    fontSize: opts.size ?? 22,
    bold: opts.bold ?? false,
    color: opts.color ?? ink,
    alignment: opts.align ?? "left",
  };
  return shape;
}

function addBox(slide, x, y, w, h, fill = panel, lineFill = "none") {
  return slide.shapes.add({
    geometry: "rect",
    position: { left: x, top: y, width: w, height: h },
    fill,
    line: { style: "solid", fill: lineFill, width: lineFill === "none" ? 0 : 1 },
  });
}

function addHeader(slide, section, title, pageNo) {
  slide.background.fill = "#FFFFFF";
  addText(slide, section, M, 34, 280, 24, { size: 14, bold: true, color: muted });
  addText(slide, title, M, 70, 930, 64, { size: 38, bold: true });
  addBox(slide, M, 148, W - 2 * M, 1, rule);
  addText(slide, String(pageNo).padStart(2, "0"), W - 95, 650, 40, 24, { size: 14, color: muted, align: "right" });
}

function addBullets(slide, items, x, y, w, opts = {}) {
  const gap = opts.gap ?? 47;
  const size = opts.size ?? 22;
  items.forEach((item, i) => {
    const top = y + i * gap;
    addBox(slide, x, top + 8, 9, 9, opts.dot ?? ink);
    addText(slide, item, x + 24, top, w - 24, gap - 2, { size, color: opts.color ?? ink });
  });
}

function addLabeledPanel(slide, title, body, x, y, w, h, opts = {}) {
  addBox(slide, x, y, w, h, opts.fill ?? panel, opts.line ?? "none");
  addText(slide, title, x + 22, y + 18, w - 44, 32, { size: opts.titleSize ?? 22, bold: true });
  addText(slide, body, x + 22, y + 58, w - 44, h - 70, { size: opts.bodySize ?? 18, color: opts.bodyColor ?? dark });
}

function addSectionSlide(p, label, title, subtitle, pageNo) {
  const slide = p.slides.add();
  slide.background.fill = "#FFFFFF";
  addText(slide, label, M, 42, 330, 28, { size: 15, bold: true, color: muted });
  addText(slide, title, M, 190, 820, 150, { size: 58, bold: true });
  addText(slide, subtitle, M, 382, 820, 80, { size: 24, color: muted });
  addBox(slide, 930, 0, 300, H, panel);
  addText(slide, String(pageNo).padStart(2, "0"), W - 95, 650, 40, 24, { size: 14, color: muted, align: "right" });
}

const p = Presentation.create({ slideSize: { width: W, height: H } });
let page = 1;

{
  const slide = p.slides.add();
  slide.background.fill = "#FFFFFF";
  addText(slide, "软件与理论综合设计与实践", M, 48, 360, 28, { size: 16, bold: true, color: muted });
  addText(slide, "校园二手物品交易平台的设计与实现", M, 165, 760, 150, { size: 56, bold: true });
  addText(slide, "基于 Vue3 + Spring Boot + MySQL + Redis 的前后端分离 Web 应用", M, 340, 680, 72, { size: 24, color: muted });
  addText(slide, "项目答辩", M, 555, 240, 36, { size: 22, bold: true });
  addText(slide, "小组成员 / 指导教师 / 日期", M, 596, 360, 28, { size: 18, color: muted });
  try {
    slide.images.add({
      blob: await imageBytes(HERO),
      contentType: "image/png",
      alt: "校园二手交易平台首页视觉图",
      fit: "cover",
      position: { left: 840, top: 80, width: 360, height: 520 },
    });
  } catch {
    addBox(slide, 840, 80, 360, 520, panel);
  }
  addText(slide, "01", W - 95, 650, 40, 24, { size: 14, color: muted, align: "right" });
  page++;
}

{
  const slide = p.slides.add();
  addHeader(slide, "目录", "答辩围绕项目价值、实现重点和团队交付展开", page++);
  const items = [
    ["01", "项目简介", "背景、目标、架构、功能与主流程"],
    ["02", "重要设计与实现", "数据库、权限、审核、订单、并发、设计模式、推荐算法、测试"],
    ["03", "总结与成员分工", "项目成果、不足改进与团队职责"],
  ];
  items.forEach((it, i) => {
    const y = 205 + i * 120;
    addText(slide, it[0], M, y, 80, 50, { size: 42, bold: true });
    addText(slide, it[1], M + 120, y + 2, 350, 38, { size: 28, bold: true });
    addText(slide, it[2], M + 120, y + 48, 720, 34, { size: 20, color: muted });
    addBox(slide, M, y + 96, W - 2 * M, 1, rule);
  });
}

addSectionSlide(p, "第一部分", "项目简介", "先说明为什么做、做什么，以及系统怎样跑起来。", page++);

{
  const slide = p.slides.add();
  addHeader(slide, "项目简介", "校园闲置交易需要一个更规范的线上闭环", page++);
  addBullets(slide, [
    "校园中存在大量闲置教材、电子产品、生活用品等可流通物品。",
    "微信群、朋友圈等传统方式信息分散，搜索和比较成本较高。",
    "线下私聊交易缺少统一流程，容易出现审核缺失、状态不清、沟通留痕不足等问题。",
    "本项目面向校园 C2C 场景，构建可发布、可搜索、可下单、可管理的二手交易平台。",
  ], M, 205, 760, { gap: 70, size: 23 });
  addLabeledPanel(slide, "核心判断", "平台价值不只是展示商品，而是把发布、审核、交易、通知、后台管理连成一条可追踪的业务链。", 900, 205, 260, 290, { fill: "#F4F4F4", bodySize: 20 });
}

{
  const slide = p.slides.add();
  addHeader(slide, "项目简介", "项目目标覆盖用户、商品、交易和管理四条主线", page++);
  const cols = [
    ["用户侧", "注册登录\n个人中心\n安全认证"],
    ["商品侧", "发布商品\n搜索筛选\n收藏浏览"],
    ["交易侧", "创建订单\n模拟支付\n发货收货"],
    ["管理侧", "商品审核\n公告管理\n数据统计"],
  ];
  cols.forEach((c, i) => {
    addLabeledPanel(slide, c[0], c[1], M + i * 292, 220, 250, 250, { fill: i === 1 ? "#EAEAEA" : panel, titleSize: 26, bodySize: 24 });
  });
  addText(slide, "目标是形成完整业务闭环，同时保证结构清晰、安全可控、便于后续扩展。", M, 540, 1000, 40, { size: 25, bold: true });
}

{
  const slide = p.slides.add();
  addHeader(slide, "项目简介", "系统采用前后端分离架构，核心数据以 MySQL 为事实源", page++);
  addLabeledPanel(slide, "前端", "Vue3\nVite\nTypeScript\nPinia\nVue Router\nAxios\nElement Plus", M, 200, 260, 330, { bodySize: 21 });
  addLabeledPanel(slide, "后端", "Spring Boot\nSpring Security\nJWT\nMyBatis-Plus\nRESTful API\n统一异常处理", M + 310, 200, 260, 330, { bodySize: 21 });
  addLabeledPanel(slide, "数据与缓存", "MySQL 存储核心业务数据\nRedis 支持验证码、登录限流、JWT 黑名单、商品热度统计", M + 620, 200, 360, 330, { bodySize: 21 });
  addText(slide, "前端处理页面与交互，后端处理业务规则和权限，数据库保证交易状态可靠，Redis 承担高频辅助能力。", M, 570, 1060, 50, { size: 22, color: muted });
}

{
  const slide = p.slides.add();
  addHeader(slide, "项目简介", "功能模块围绕二手交易闭环组织", page++);
  const modules = [
    ["用户模块", "注册、登录、找回密码、个人中心"],
    ["商品模块", "发布、图片上传、搜索筛选、详情、收藏、上下架"],
    ["订单模块", "下单、模拟支付、发货、确认收货、取消订单"],
    ["消息模块", "买卖留言、系统通知、已读未读"],
    ["后台模块", "商品审核、用户管理、订单管理、公告管理、数据统计"],
  ];
  modules.forEach((m, i) => {
    const y = 188 + i * 78;
    addText(slide, m[0], M, y, 170, 32, { size: 24, bold: true });
    addText(slide, m[1], 260, y + 3, 830, 30, { size: 21, color: dark });
    addBox(slide, M, y + 52, 1060, 1, rule);
  });
}

{
  const slide = p.slides.add();
  addHeader(slide, "项目简介", "主流程从发布审核走到交易完成", page++);
  const steps = ["注册登录", "发布商品", "管理员审核", "商品上架", "搜索浏览", "收藏/下单", "支付发货", "确认收货"];
  steps.forEach((s, i) => {
    const x = M + (i % 4) * 292;
    const y = i < 4 ? 205 : 395;
    addBox(slide, x, y, 230, 82, i === 2 ? "#FFE5D8" : panel, i === 2 ? accent : "none");
    addText(slide, `0${i + 1}`, x + 18, y + 18, 48, 30, { size: 20, bold: true, color: i === 2 ? accent : muted });
    addText(slide, s, x + 70, y + 20, 135, 32, { size: 22, bold: true });
  });
  addText(slide, "审核是商品从“用户提交”进入“公开交易”的关键闸口，订单状态机则负责约束后续交易。", M, 585, 980, 40, { size: 22, color: muted });
}

addSectionSlide(p, "第二部分", "重要设计与实现", "这一部分说明系统怎样保证可用、可靠、可维护。", page++);

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "数据库围绕用户、商品、交易、消息和后台管理建模", page++);
  const left = ["users 用户", "products 商品", "orders 订单", "messages 留言", "notifications 通知", "announcements 公告"];
  const right = ["categories 分类", "product_images 图片", "favorites 收藏", "order_logs 订单日志", "reports 举报", "sensitive_words 敏感词"];
  addLabeledPanel(slide, "核心业务表", left.join("\n"), M, 205, 360, 330, { bodySize: 20 });
  addLabeledPanel(slide, "辅助与治理表", right.join("\n"), M + 410, 205, 360, 330, { bodySize: 20 });
  addLabeledPanel(slide, "设计特点", "status 管理状态\nversion 支持乐观锁\ndeleted 支持软删除\n唯一约束避免重复收藏\n索引支撑列表与订单查询", M + 820, 205, 300, 330, { bodySize: 20, fill: "#FFEFE8", line: accent });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "认证与权限设计把用户身份贯穿前后端", page++);
  addBullets(slide, [
    "登录成功后，后端生成 JWT，前端在后续请求中携带 token。",
    "后端解析 token，识别用户 ID 与角色，接口以认证上下文为准。",
    "普通用户可浏览、发布和购买商品；管理员可进入后台审核和管理。",
    "前端路由守卫限制未登录用户访问发布、订单、消息等页面。",
    "Redis 支持验证码、登录限流和 JWT 黑名单，提升安全性。",
  ], M, 190, 800, { gap: 63, size: 22 });
  addLabeledPanel(slide, "权限边界", "用户身份不信任前端传入的 ownerId、sellerId 或 buyerId，而是以后端解析出的认证身份为准。", 900, 235, 260, 220, { bodySize: 20 });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "商品发布先审核再展示，降低违规内容风险", page++);
  const cols = [
    ["提交", "用户填写标题、描述、价格、分类和图片，后端校验参数与分类。"],
    ["检测", "商品标题和描述经过敏感词检测，不合规内容直接阻断。"],
    ["审核", "商品初始状态为 PENDING，管理员通过后变为 APPROVED。"],
    ["通知", "审核结果写入通知，发布者可看到通过或拒绝原因。"],
  ];
  cols.forEach((c, i) => addLabeledPanel(slide, c[0], c[1], M + i * 292, 220, 250, 250, { bodySize: 20, fill: i === 2 ? "#FFEFE8" : panel, line: i === 2 ? accent : "none" }));
  addText(slide, "审核流程让商品从“个人发布内容”变成“平台可交易资源”。", M, 540, 900, 42, { size: 25, bold: true });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "订单状态机保证交易流程不能被随意跳转", page++);
  const states = [["待支付", "PENDING_PAYMENT"], ["已支付", "PAID"], ["已发货", "SHIPPED"], ["已完成", "COMPLETED"], ["已取消", "CANCELED"]];
  states.forEach((s, i) => {
    const x = M + i * 225;
    const y = i === 4 ? 440 : 250;
    addBox(slide, x, y, 180, 86, i === 4 ? "#F5F5F5" : panel, "none");
    addText(slide, s[0], x + 18, y + 16, 150, 26, { size: 22, bold: true });
    addText(slide, s[1], x + 18, y + 48, 150, 22, { size: 13, color: muted });
  });
  addText(slide, "允许流转：待支付 → 已支付 / 已取消；已支付 → 已发货 / 已取消；已发货 → 已完成。", M, 570, 1050, 34, { size: 22, color: muted });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "项目已实现基础并发保障，重点防止交易状态错乱", page++);
  const items = [
    ["下单防超卖", "商品使用 version 乐观锁，只允许状态仍为 APPROVED 且版本匹配的请求购买成功。"],
    ["订单状态并发控制", "订单更新同时校验当前状态和 version，避免多个请求覆盖同一状态。"],
    ["登录限流", "Redis 记录同一 IP 登录失败次数，超过阈值后限制继续尝试。"],
    ["热度统计", "商品详情访问写入浏览量，并用 Redis 记录热门商品分数。"],
  ];
  items.forEach((it, i) => {
    const x = M + (i % 2) * 585;
    const y = i < 2 ? 210 : 420;
    addLabeledPanel(slide, it[0], it[1], x, y, 525, 142, { bodySize: 19, fill: i === 0 ? "#FFEFE8" : panel, line: i === 0 ? accent : "none" });
  });
  addText(slide, "答辩口径：这是基础高并发保障，不是完整分布式高并发架构。", M, 610, 900, 30, { size: 19, color: muted });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "状态模式把订单流转规则集中到一个位置", page++);
  addLabeledPanel(slide, "功能", "把订单不同阶段的规则集中管理，避免每个接口都重复写状态判断。", M, 210, 340, 170, { bodySize: 21 });
  addLabeledPanel(slide, "具体实现", "OrderStatus 枚举表示状态，并通过 canTransitionTo() 定义允许的状态变化。支付、发货、确认收货、取消订单时都先校验流转是否合法。", M + 400, 210, 420, 240, { bodySize: 20, fill: "#FFEFE8", line: accent });
  addLabeledPanel(slide, "项目价值", "订单流程更清晰；非法状态跳转更容易被阻止；后续新增状态时维护范围更集中。", M + 880, 210, 260, 240, { bodySize: 20 });
  addText(slide, "不是硬套框架能力，而是项目代码里围绕订单业务抽象出的状态机。", M, 565, 950, 38, { size: 23, bold: true });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "模板方法模式减少订单操作的重复代码", page++);
  addText(slide, "支付、发货、确认收货、取消订单的目标不同，但处理骨架相似。", M, 185, 940, 34, { size: 24, bold: true });
  const steps = ["查询订单", "校验操作者", "校验状态", "更新状态", "写订单日志", "发送通知"];
  steps.forEach((s, i) => {
    const x = M + i * 185;
    addBox(slide, x, 295, 150, 82, i === 3 ? "#FFEFE8" : panel, i === 3 ? accent : "none");
    addText(slide, s, x + 16, 320, 118, 30, { size: 20, bold: true, align: "center" });
  });
  addLabeledPanel(slide, "具体实现", "pay()、ship()、confirm() 等方法最终调用统一的 transition() / transitionEntity() 方法，只传入操作者、目标状态和必要参数。", M, 465, 900, 105, { bodySize: 20 });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "Service 作为门面封装复杂业务流程", page++);
  addLabeledPanel(slide, "Controller 看到的接口", "orderService.createOrder(userId, request)", M, 210, 420, 150, { bodySize: 24, fill: "#F6F6F6" });
  addLabeledPanel(slide, "Service 内部协调的步骤", "商品查询\n禁止购买自己的商品\n状态校验\n订单生成\n乐观锁标记已售\n订单日志\n行为记录\n通知卖家", M + 500, 180, 420, 360, { bodySize: 21, fill: "#FFEFE8", line: accent });
  addText(slide, "门面模式让控制层保持简单，把业务编排集中在 Service 层，便于测试和维护。", M, 585, 980, 38, { size: 23, bold: true });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "推荐模块根据用户行为选择不同评分策略", page++);
  addLabeledPanel(slide, "冷启动推荐", "面向未登录用户或没有行为记录的用户，主要依据商品热度和发布时间。", M, 215, 340, 230, { bodySize: 22 });
  addLabeledPanel(slide, "个性化推荐", "面向有行为记录的用户，根据浏览、收藏、购买、搜索关键词、分类偏好和价格偏好构建兴趣画像。", M + 420, 215, 390, 230, { bodySize: 21, fill: "#FFEFE8", line: accent });
  addLabeledPanel(slide, "策略模式思想", "不同用户情况采用不同评分策略，避免新用户没有历史数据时无法推荐。", M + 880, 215, 260, 230, { bodySize: 20 });
  addText(slide, "推荐分数 = 热度分 + 新鲜度分 + 分类偏好分 + 关键词匹配分 + 价格匹配分 - 已接触商品降权", M, 550, 1050, 36, { size: 23, bold: true });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "商品推荐算法用轻量画像提升商品曝光效率", page++);
  const factors = [
    ["行为权重", "浏览 8\n搜索 14\n收藏 30\n购买 45"],
    ["时间衰减", "越新的行为影响越大，历史行为逐步降低权重。"],
    ["候选商品", "只取审核通过、未删除且不是自己发布的商品。"],
    ["降权处理", "已经浏览、收藏或购买过的商品降低推荐分。"],
  ];
  factors.forEach((f, i) => {
    const x = M + (i % 2) * 585;
    const y = i < 2 ? 205 : 405;
    addLabeledPanel(slide, f[0], f[1], x, y, 525, 145, { bodySize: 21, fill: i === 0 ? "#FFEFE8" : panel, line: i === 0 ? accent : "none" });
  });
  addText(slide, "实现特点：算法解释性强，便于答辩说明，也方便后续替换成协同过滤或机器学习模型。", M, 600, 1010, 32, { size: 20, color: muted });
}

{
  const slide = p.slides.add();
  addHeader(slide, "重要设计与实现", "测试覆盖后端流程、前端页面和关键异常场景", page++);
  addLabeledPanel(slide, "后端测试", "用户认证\n商品流程\n订单流程\n并发下单\n消息模块\n后台管理\nRedis 降级", M, 205, 340, 330, { bodySize: 22 });
  addLabeledPanel(slide, "前端测试", "页面渲染\n路由权限\n商品卡片\n订单中心\n消息中心\nAPI 合约", M + 420, 205, 340, 330, { bodySize: 22 });
  addLabeledPanel(slide, "质量保障", "统一响应格式\n统一异常处理\n参数校验\n权限控制\n数据库约束\n乐观锁并发控制", M + 840, 205, 300, 330, { bodySize: 21, fill: "#FFEFE8", line: accent });
}

addSectionSlide(p, "第三部分", "总结与成员分工", "最后回到项目成果、后续改进和团队贡献。", page++);

{
  const slide = p.slides.add();
  addHeader(slide, "总结与成员分工", "项目完成了从发布到交易再到管理的核心闭环", page++);
  addBullets(slide, [
    "前后端分离，页面、接口、数据库和缓存职责清晰。",
    "商品审核、敏感词检测和后台管理提升平台治理能力。",
    "订单状态机和乐观锁保证交易流程可靠，降低并发冲突风险。",
    "推荐算法利用用户行为与商品热度，提高商品曝光和用户体验。",
    "测试覆盖主要业务流，为课程验收和后续扩展提供基础。",
  ], M, 190, 850, { gap: 63, size: 22 });
}

{
  const slide = p.slides.add();
  addHeader(slide, "总结与成员分工", "后续可以从真实交易、实时沟通和系统性能继续增强", page++);
  const items = [
    ["真实支付与物流", "接入支付沙箱或第三方物流接口，让交易闭环更接近真实平台。"],
    ["实时聊天", "将站内留言升级为 WebSocket 实时会话，提升买卖沟通效率。"],
    ["推荐优化", "在规则评分基础上加入协同过滤、标签画像或更细粒度行为分析。"],
    ["高并发增强", "引入消息队列削峰、缓存预热、压测报告和集群部署方案。"],
  ];
  items.forEach((it, i) => addLabeledPanel(slide, it[0], it[1], M + (i % 2) * 585, i < 2 ? 210 : 420, 525, 142, { bodySize: 20 }));
}

{
  const slide = p.slides.add();
  addHeader(slide, "总结与成员分工", "成员分工覆盖架构、业务、前端、数据与测试", page++);
  const roles = [
    ["成员 A", "后端核心与系统架构\nSpring Boot 搭建\nJWT 与安全配置\n统一响应和异常处理"],
    ["成员 B", "商品与订单模块\n发布、搜索、收藏、审核\n订单状态机\n乐观锁防超卖"],
    ["成员 C", "前端页面与交互\nVue3 工程\n首页、详情、订单、消息\n路由守卫和交互优化"],
    ["成员 D", "推荐算法、数据库与测试\n表结构与种子数据\n推荐算法设计\n测试用例和答辩文档"],
  ];
  roles.forEach((r, i) => {
    addLabeledPanel(slide, r[0], r[1], M + i * 292, 205, 250, 360, { bodySize: 19, fill: i === 3 ? "#FFEFE8" : panel, line: i === 3 ? accent : "none" });
  });
}

{
  const slide = p.slides.add();
  slide.background.fill = "#FFFFFF";
  addText(slide, "谢谢观看", M, 190, 760, 90, { size: 72, bold: true });
  addText(slide, "欢迎老师批评指正", M, 310, 520, 42, { size: 28, color: muted });
  addBox(slide, M, 445, 720, 1, rule);
  addText(slide, "校园二手物品交易平台的设计与实现", M, 480, 560, 34, { size: 22, bold: true });
  addText(slide, "Vue3 + Spring Boot + MySQL + Redis", M, 520, 460, 30, { size: 20, color: muted });
  addBox(slide, 910, 0, 260, H, panel);
  addText(slide, "25", W - 95, 650, 40, 24, { size: 14, color: muted, align: "right" });
}

await fs.mkdir(path.dirname(OUT), { recursive: true });
await fs.mkdir(QA, { recursive: true });

for (const [index, slide] of p.slides.items.entries()) {
  const stem = `slide-${String(index + 1).padStart(2, "0")}`;
  await writeBlob(path.join(QA, `${stem}.png`), await p.export({ slide, format: "png", scale: 1 }));
  await fs.writeFile(path.join(QA, `${stem}.layout.json`), await (await slide.export({ format: "layout" })).text());
}

await writeBlob(path.join(QA, "montage.webp"), await p.export({ format: "webp", montage: true, scale: 1 }));

const pptx = await PresentationFile.exportPptx(p);
await pptx.save(OUT);
console.log(OUT);
