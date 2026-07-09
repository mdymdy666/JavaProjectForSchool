import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

const artifactToolPath = "C:/Users/邓先毅/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs";
const { Presentation, PresentationFile } = await import(pathToFileURL(artifactToolPath).href);

const ROOT = "D:/Downloads/JavaProjectForSchool-main";
const ASSET = `${ROOT}/output/ppt-assets`;
const OUT = `${ROOT}/output/campus-trade-defense-simple-15.pptx`;
const QA = `${ROOT}/output/ppt-build-15/qa`;

const W = 1280;
const H = 720;
const M = 54;
const ink = "#0F172A";
const muted = "#64748B";
const blue = "#1E6FFF";
const paleBlue = "#EEF5FF";
const panel = "#F5F7FB";
const line = "#D8E2F0";
const orange = "#FF7A1A";

const img = {
  guess: `${ASSET}/img01-guess-like.png`,
  rec: `${ASSET}/img02-recommend-cards.png`,
  cats: `${ASSET}/img03-categories.png`,
  hot: `${ASSET}/img04-hot-announcement.png`,
  market: `${ASSET}/img05-market-search.png`,
  publish: `${ASSET}/img06-publish-form.png`,
  msg: `${ASSET}/img07-message-center.png`,
  dash: `${ASSET}/img08-admin-dashboard.png`,
  users: `${ASSET}/img09-user-management.png`,
  orders: `${ASSET}/img10-order-management.png`,
  words: `${ASSET}/img11-sensitive-words.png`,
  ann: `${ASSET}/img12-announcements.png`,
  safety: `${ASSET}/img13-safety-notice.png`,
};

async function writeBlob(file, blob) {
  await fs.writeFile(file, new Uint8Array(await blob.arrayBuffer()));
}

async function imageBytes(file) {
  const bytes = await fs.readFile(file);
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
}

function text(slide, value, x, y, w, h, opt = {}) {
  const s = slide.shapes.add({
    geometry: "textbox",
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  s.text = value;
  s.text.style = {
    fontSize: opt.size ?? 20,
    bold: opt.bold ?? false,
    color: opt.color ?? ink,
    alignment: opt.align ?? "left",
  };
  return s;
}

function box(slide, x, y, w, h, fill = panel, stroke = "none") {
  return slide.shapes.add({
    geometry: "roundRect",
    position: { left: x, top: y, width: w, height: h },
    fill,
    line: { style: "solid", fill: stroke, width: stroke === "none" ? 0 : 1 },
    borderRadius: "rounded-xl",
  });
}

async function image(slide, file, x, y, w, h, opt = {}) {
  const frame = box(slide, x, y, w, h, "#FFFFFF", opt.stroke ?? line);
  slide.images.add({
    blob: await imageBytes(file),
    contentType: "image/png",
    alt: opt.alt ?? "项目页面截图",
    fit: opt.fit ?? "contain",
    position: { left: x + 8, top: y + 8, width: w - 16, height: h - 16 },
    geometry: "roundRect",
    borderRadius: "rounded-lg",
  });
  return frame;
}

function header(slide, section, title, pageNo) {
  slide.background.fill = "#FFFFFF";
  text(slide, section, M, 34, 260, 24, { size: 14, bold: true, color: blue });
  text(slide, title, M, 66, 950, 58, { size: 36, bold: true });
  slide.shapes.add({
    geometry: "rect",
    position: { left: M, top: 138, width: W - M * 2, height: 1 },
    fill: line,
    line: { style: "solid", fill: "none", width: 0 },
  });
  text(slide, String(pageNo).padStart(2, "0"), W - 92, 650, 38, 24, { size: 14, color: muted, align: "right" });
}

function bulletList(slide, items, x, y, w, opt = {}) {
  const gap = opt.gap ?? 44;
  const size = opt.size ?? 20;
  items.forEach((item, i) => {
    const yy = y + i * gap;
    slide.shapes.add({
      geometry: "ellipse",
      position: { left: x, top: yy + 8, width: 8, height: 8 },
      fill: opt.dot ?? blue,
      line: { style: "solid", fill: "none", width: 0 },
    });
    text(slide, item, x + 22, yy, w - 22, gap - 2, { size, color: opt.color ?? ink });
  });
}

function stat(slide, label, value, x, y, w, color = blue) {
  box(slide, x, y, w, 96, "#FFFFFF", line);
  text(slide, value, x + 20, y + 16, w - 40, 35, { size: 30, bold: true, color });
  text(slide, label, x + 20, y + 56, w - 40, 24, { size: 16, color: muted });
}

function sectionSlide(p, title, subtitle, pageNo) {
  const slide = p.slides.add();
  slide.background.fill = "#FFFFFF";
  box(slide, 880, 0, 280, H, paleBlue, "none");
  text(slide, title, M, 210, 740, 84, { size: 54, bold: true });
  text(slide, subtitle, M, 315, 720, 44, { size: 22, color: muted });
  text(slide, String(pageNo).padStart(2, "0"), W - 92, 650, 38, 24, { size: 14, color: muted, align: "right" });
}

const p = Presentation.create({ slideSize: { width: W, height: H } });
let page = 1;

// 1 Cover
{
  const s = p.slides.add();
  s.background.fill = "#FFFFFF";
  text(s, "软件与理论综合设计与实践", M, 44, 360, 28, { size: 16, bold: true, color: blue });
  text(s, "校园二手物品交易平台的设计与实现", M, 168, 630, 138, { size: 52, bold: true });
  text(s, "Vue3 + Spring Boot + MySQL + Redis", M, 340, 520, 34, { size: 24, color: muted });
  text(s, "项目答辩", M, 555, 200, 32, { size: 22, bold: true });
  text(s, "指导教师：徐士伟 / 小组成员 / 日期", M, 596, 420, 26, { size: 18, color: muted });
  await image(s, img.rec, 730, 140, 470, 310, { fit: "contain", alt: "推荐商品卡片页面截图" });
  text(s, "01", W - 92, 650, 38, 24, { size: 14, color: muted, align: "right" });
  page++;
}

// 2 Requirements
{
  const s = p.slides.add();
  header(s, "第一章 项目简介", "项目严格对齐指导书给出的技术栈和功能范围", page++);
  bulletList(s, [
    "项目题目：校园二手物品交易平台的设计与实现。",
    "技术要求：Vue3 + Spring Boot + MySQL + Redis，采用前后端分离架构。",
    "安全要求：JWT + Spring Security 实现用户认证与授权。",
    "功能要求：覆盖用户、商品、订单、消息、后台管理，并扩展推荐、敏感词和举报处理。",
  ], M, 190, 690, { gap: 60, size: 21 });
  box(s, 820, 195, 330, 210, paleBlue, line);
  text(s, "指导书关键词", 850, 222, 260, 32, { size: 24, bold: true, color: blue });
  text(s, "应用系统\n项目难度 B\n建议 3-4 人\n校园 C2C 交易平台", 850, 274, 260, 112, { size: 22 });
  await image(s, img.cats, 785, 468, 410, 86, { fit: "contain", alt: "平台商品分类截图" });
}

// 3 Business loop
{
  const s = p.slides.add();
  header(s, "第一章 项目简介", "平台把校园闲置交易整理成一个可追踪闭环", page++);
  await image(s, img.market, 610, 178, 570, 250, { fit: "contain", alt: "商品市场搜索页面截图" });
  const steps = ["注册登录", "发布商品", "管理员审核", "搜索浏览", "下单支付", "发货收货", "消息留痕", "后台管理"];
  steps.forEach((step, i) => {
    const x = M + (i % 4) * 132;
    const y = i < 4 ? 210 : 340;
    box(s, x, y, 108, 70, i === 2 ? "#FFF1E8" : panel, i === 2 ? orange : "none");
    text(s, step, x + 12, y + 23, 84, 24, { size: 17, bold: true, align: "center", color: i === 2 ? orange : ink });
  });
  text(s, "解决的问题：信息分散、搜索困难、流程不规范、交易缺少留痕。", M, 520, 840, 34, { size: 24, bold: true });
  text(s, "市场页体现了搜索、分类、结果统计和发布入口，是业务闭环的主要入口。", M, 568, 960, 30, { size: 19, color: muted });
}

// 4 Module overview
{
  const s = p.slides.add();
  header(s, "第一章 项目简介", "功能模块覆盖用户端、交易端和管理端", page++);
  await image(s, img.cats, M, 185, 560, 120, { fit: "contain", alt: "商品分类入口截图" });
  await image(s, img.hot, 820, 178, 330, 380, { fit: "contain", alt: "热门榜和公告截图" });
  const mods = [
    ["用户", "注册登录、个人中心、地址管理"],
    ["商品", "发布、分类、搜索、收藏、上下架"],
    ["订单", "下单、支付、发货、收货、取消"],
    ["消息", "交易沟通、系统通知、已读未读"],
    ["后台", "数据概览、审核、用户、订单、公告"],
  ];
  mods.forEach((m, i) => {
    const x = M + (i % 2) * 355;
    const y = 350 + Math.floor(i / 2) * 78;
    text(s, m[0], x, y, 70, 28, { size: 22, bold: true, color: blue });
    text(s, m[1], x + 84, y + 2, 260, 26, { size: 18, color: ink });
  });
}

// 5 Recommendation evidence
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "推荐算法在首页直接形成商品曝光入口", page++);
  await image(s, img.guess, M, 185, 560, 190, { fit: "contain", alt: "猜你喜欢推荐区截图" });
  await image(s, img.rec, M, 410, 560, 210, { fit: "contain", alt: "推荐好物商品卡片截图" });
  text(s, "设计目标", 690, 188, 180, 32, { size: 25, bold: true });
  bulletList(s, [
    "新用户优先看到热门、新鲜商品。",
    "老用户根据搜索、浏览、收藏、购买形成个性化推荐。",
    "推荐结果带有价格、分类、浏览量和收藏入口，便于继续交易。",
  ], 690, 245, 470, { gap: 58, size: 21 });
  text(s, "页面上的“猜你喜欢”和“推荐好物”是推荐算法的前端落点。", 690, 520, 450, 54, { size: 24, bold: true, color: blue });
}

// 6 Recommendation strategy
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "推荐策略同时处理冷启动和个性化场景", page++);
  await image(s, img.hot, 770, 180, 340, 395, { fit: "contain", alt: "热门商品榜单截图" });
  box(s, M, 205, 300, 175, "#FFFFFF", line);
  text(s, "冷启动推荐", M + 24, 230, 250, 30, { size: 24, bold: true, color: blue });
  text(s, "面向未登录或无行为用户，依据浏览热度和发布时间排序。", M + 24, 282, 240, 70, { size: 20 });
  box(s, M + 345, 205, 330, 175, "#FFFFFF", line);
  text(s, "个性化推荐", M + 369, 230, 270, 30, { size: 24, bold: true, color: blue });
  text(s, "面向有行为用户，根据搜索、浏览、收藏、购买构建用户画像。", M + 369, 282, 260, 70, { size: 20 });
  box(s, M, 430, 620, 100, paleBlue, line);
  text(s, "行为权重：浏览 8 / 搜索 14 / 收藏 30 / 购买 45", M + 24, 462, 560, 32, { size: 25, bold: true });
  text(s, "热门榜对应冷启动与热度推荐，猜你喜欢对应个性化推荐。", M, 590, 800, 30, { size: 20, color: muted });
}

// 7 Scoring
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "推荐分数由热度、新鲜度和用户偏好共同决定", page++);
  await image(s, img.rec, 700, 184, 470, 270, { fit: "contain", alt: "商品推荐卡片细节截图" });
  const factors = [
    ["热度", "浏览量越高，基础分越高"],
    ["新鲜度", "发布时间越近，衰减越少"],
    ["偏好", "分类、关键词、成色与价格匹配加分"],
    ["降权", "已浏览或已购买商品降低推荐分"],
  ];
  factors.forEach((f, i) => {
    const x = M + (i % 2) * 295;
    const y = 200 + Math.floor(i / 2) * 125;
    box(s, x, y, 255, 88, i === 0 ? "#FFF1E8" : panel, i === 0 ? orange : "none");
    text(s, f[0], x + 22, y + 16, 72, 28, { size: 22, bold: true, color: i === 0 ? orange : blue });
    text(s, f[1], x + 96, y + 18, 128, 42, { size: 17 });
  });
  box(s, M, 500, 1080, 58, paleBlue, line);
  text(s, "推荐分数 = 热度分 + 新鲜度分 + 分类偏好分 + 关键词匹配分 + 价格匹配分 - 已接触商品降权", M + 24, 518, 1020, 26, { size: 22, bold: true, color: blue });
}

// 8 State pattern
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "状态模式让订单流转规则集中且可控", page++);
  await image(s, img.orders, 670, 195, 500, 240, { fit: "contain", alt: "后台订单管理状态截图" });
  bulletList(s, [
    "订单状态包括待支付、已支付、已发货、已完成、已取消。",
    "OrderStatus 枚举集中定义状态，canTransitionTo() 控制合法流转。",
    "状态列中的“已支付、已发货、已完成”对应真实订单流转结果。",
  ], M, 195, 540, { gap: 65, size: 21 });
  box(s, M, 500, 1000, 62, "#FFFFFF", line);
  text(s, "待支付 → 已支付 / 已取消；已支付 → 已发货 / 已取消；已发货 → 已完成。", M + 24, 520, 940, 26, { size: 22, bold: true });
}

// 9 Template method
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "模板方法模式复用订单操作的公共流程", page++);
  await image(s, img.orders, 800, 190, 350, 170, { fit: "contain", alt: "订单管理截图" });
  text(s, "支付、发货、确认收货、取消订单的目标状态不同，但处理骨架一致。", M, 185, 680, 34, { size: 23, bold: true });
  const steps = ["查询订单", "校验操作者", "校验状态", "更新状态", "写日志", "发通知"];
  steps.forEach((step, i) => {
    const x = M + i * 185;
    box(s, x, 305, 145, 76, i === 3 ? "#FFF1E8" : panel, i === 3 ? orange : "none");
    text(s, step, x + 14, 330, 116, 24, { size: 18, bold: true, align: "center" });
  });
  box(s, M, 465, 720, 108, paleBlue, line);
  text(s, "具体实现", M + 24, 486, 150, 28, { size: 22, bold: true, color: blue });
  text(s, "pay()、ship()、confirm()、cancel() 最终调用统一的 transition() / transitionEntity()。", M + 24, 526, 660, 26, { size: 20 });
}

// 10 Facade
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "门面模式把复杂业务封装在 Service 层", page++);
  await image(s, img.publish, 690, 175, 470, 370, { fit: "contain", alt: "商品发布表单截图" });
  box(s, M, 205, 240, 110, "#FFFFFF", line);
  text(s, "前端入口", M + 24, 228, 170, 28, { size: 23, bold: true, color: blue });
  text(s, "用户提交发布表单", M + 24, 272, 170, 24, { size: 20 });
  box(s, M + 300, 205, 285, 250, "#FFF1E8", orange);
  text(s, "Service 内部处理", M + 324, 228, 230, 28, { size: 23, bold: true, color: orange });
  text(s, "参数校验\n分类校验\n敏感词检测\n图片保存\n状态初始化\n通知与后续审核", M + 324, 276, 230, 150, { size: 20 });
  text(s, "页面保持简单，后端 Service 统一编排业务细节，Controller 不直接处理复杂流程。", M, 555, 920, 32, { size: 21, color: muted });
}

// 11 Strategy pattern
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "策略模式思想让不同用户走不同推荐路径", page++);
  await image(s, img.guess, M, 190, 520, 180, { fit: "contain", alt: "猜你喜欢截图" });
  await image(s, img.hot, 795, 185, 330, 360, { fit: "contain", alt: "热门榜截图" });
  box(s, M, 420, 520, 95, paleBlue, line);
  text(s, "有行为用户：构建画像，计算分类、关键词、价格等匹配分。", M + 20, 452, 475, 26, { size: 20, bold: true, color: blue });
  box(s, 590, 420, 175, 95, panel, "none");
  text(s, "选择策略", 615, 454, 120, 28, { size: 22, bold: true, align: "center" });
  box(s, 795, 570, 330, 52, "#FFFFFF", line);
  text(s, "无行为用户：优先按热度和新鲜度推荐。", 815, 586, 290, 24, { size: 19, bold: true });
}

// 12 Market and publish
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "商品市场和发布页共同支撑买卖双方操作", page++);
  await image(s, img.market, M, 185, 575, 250, { fit: "contain", alt: "商品市场搜索页截图" });
  await image(s, img.publish, 690, 185, 470, 310, { fit: "contain", alt: "商品发布表单截图" });
  text(s, "买家侧：搜索、分类、热门关键词、排序、查看结果。", M, 485, 560, 28, { size: 21, bold: true });
  text(s, "卖家侧：填写名称、分类、成色、价格和描述，提交后进入审核流程。", 690, 525, 470, 54, { size: 21, bold: true });
}

// 13 Messages, announcements, safety
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "消息、公告和安全须知共同保障交易沟通", page++);
  await image(s, img.msg, M, 188, 500, 250, { fit: "contain", alt: "消息中心截图" });
  await image(s, img.ann, 620, 184, 520, 175, { fit: "contain", alt: "平台公告截图" });
  await image(s, img.safety, 620, 395, 520, 200, { fit: "cover", alt: "交易安全须知截图" });
  text(s, "消息中心记录商品咨询、交易进度和系统通知。", M, 475, 500, 30, { size: 21, bold: true });
  text(s, "公告与安全须知承担平台规则、活动安排、交易提醒和纠纷处理说明。", M, 520, 520, 50, { size: 19, color: muted });
}

// 14 Admin
{
  const s = p.slides.add();
  header(s, "第二章 重要设计与实现", "后台管理把平台运营和内容治理集中起来", page++);
  await image(s, img.dash, M, 175, 610, 300, { fit: "contain", alt: "后台数据概览截图" });
  await image(s, img.users, 720, 174, 420, 116, { fit: "contain", alt: "用户管理截图" });
  await image(s, img.orders, 720, 315, 420, 112, { fit: "contain", alt: "订单管理截图" });
  await image(s, img.words, 720, 452, 420, 112, { fit: "contain", alt: "敏感词管理截图" });
  text(s, "数据概览显示注册用户、商品总数、订单总数、成交总额、分类占比和趋势。", M, 505, 600, 28, { size: 20, bold: true });
  text(s, "用户、订单、敏感词管理体现平台治理能力。", M, 548, 530, 26, { size: 19, color: muted });
}

// 15 Summary and roles
{
  const s = p.slides.add();
  header(s, "第三章 成员分工", "团队分工围绕架构、交易、前端和质量保障展开", page++);
  const roles = [
    ["成员 A", "后端核心与系统架构\nJWT 认证\n权限控制\n统一异常处理"],
    ["成员 B", "商品与订单模块\n商品审核\n订单状态机\n乐观锁防超卖"],
    ["成员 C", "前端页面与交互\n商品市场\n发布页\n消息与公告"],
    ["成员 D", "推荐算法、数据库与测试\nMySQL / Redis\n后台管理\n答辩材料"],
  ];
  roles.forEach((r, i) => {
    const x = M + i * 292;
    box(s, x, 185, 250, 225, i === 3 ? "#FFF1E8" : panel, i === 3 ? orange : "none");
    text(s, r[0], x + 22, 210, 200, 28, { size: 22, bold: true, color: i === 3 ? orange : blue });
    text(s, r[1], x + 22, 258, 205, 122, { size: 18 });
  });
  await image(s, img.market, M, 462, 245, 120, { fit: "cover", alt: "商品市场缩略图" });
  await image(s, img.guess, M + 292, 462, 245, 120, { fit: "cover", alt: "推荐区缩略图" });
  await image(s, img.msg, M + 584, 462, 245, 120, { fit: "cover", alt: "消息中心缩略图" });
  await image(s, img.dash, M + 876, 462, 245, 120, { fit: "cover", alt: "后台概览缩略图" });
  text(s, "项目完成了从商品发布、推荐曝光、订单交易到后台治理的核心闭环。", M, 620, 860, 28, { size: 21, bold: true });
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
