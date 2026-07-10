import fs from "node:fs/promises";
import path from "node:path";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const ROOT = "D:/Downloads/JavaProjectForSchool-main";
const ASSET = `${ROOT}/output/ppt-assets`;
const FINAL = `${ROOT}/output/campus-trade-defense-final.pptx`;
const QA = `${ROOT}/output/ppt-build-final/qa`;

const W = 1280;
const H = 720;
const M = 56;
const ink = "#0f172a";
const muted = "#64748b";
const blue = "#1e6fff";
const blue2 = "#0f4ec4";
const pale = "#f4f8ff";
const panel = "#f7f9fc";
const line = "#d8e3f1";
const green = "#16a34a";
const orange = "#f97316";
const red = "#ef4444";

const images = {
  cover: `${ROOT}/frontend/src/assets/campus-cover-bg.jpg`,
  usecase: `${ROOT}/output/pdf/campus-market-usecase.png`,
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
  usecaseTop: `${ROOT}/output/ppt-build-final/usecase-top.png`,
  usecaseMiddle: `${ROOT}/output/ppt-build-final/usecase-middle.png`,
  usecaseBottom: `${ROOT}/output/ppt-build-final/usecase-bottom.png`,
};

async function bytes(file) {
  const data = await fs.readFile(file);
  return data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength);
}

function contentType(file) {
  const ext = path.extname(file).toLowerCase();
  if (ext === ".jpg" || ext === ".jpeg") return "image/jpeg";
  if (ext === ".webp") return "image/webp";
  return "image/png";
}

async function writeBlob(file, blob) {
  await fs.writeFile(file, new Uint8Array(await blob.arrayBuffer()));
}

function addText(slide, value, x, y, w, h, opt = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = value;
  shape.text.style = {
    fontSize: opt.size ?? 20,
    bold: opt.bold ?? false,
    color: opt.color ?? ink,
    alignment: opt.align ?? "left",
  };
  return shape;
}

function addBox(slide, x, y, w, h, opt = {}) {
  return slide.shapes.add({
    geometry: opt.geometry ?? "roundRect",
    position: { left: x, top: y, width: w, height: h },
    fill: opt.fill ?? panel,
    line: { style: "solid", fill: opt.stroke ?? "none", width: opt.stroke ? 1 : 0 },
    borderRadius: opt.radius ?? "rounded-xl",
    shadow: opt.shadow,
  });
}

function addRule(slide, x, y, w, color = line) {
  slide.shapes.add({
    geometry: "rect",
    position: { left: x, top: y, width: w, height: 1.4 },
    fill: color,
    line: { style: "solid", fill: "none", width: 0 },
  });
}

function addPill(slide, text, x, y, w, color = blue) {
  addBox(slide, x, y, w, 34, { fill: `${color}14`, stroke: `${color}44`, radius: "rounded-full" });
  addText(slide, text, x + 12, y + 7, w - 24, 20, { size: 14, bold: true, color });
}

async function addImage(slide, file, x, y, w, h, opt = {}) {
  addBox(slide, x, y, w, h, { fill: "#ffffff", stroke: opt.stroke ?? line, radius: opt.radius ?? "rounded-xl" });
  slide.images.add({
    blob: await bytes(file),
    contentType: contentType(file),
    alt: opt.alt ?? "项目界面截图",
    fit: opt.fit ?? "contain",
    position: { left: x + (opt.pad ?? 8), top: y + (opt.pad ?? 8), width: w - 2 * (opt.pad ?? 8), height: h - 2 * (opt.pad ?? 8) },
    geometry: "roundRect",
    borderRadius: opt.innerRadius ?? "rounded-lg",
  });
}

async function addRawImage(slide, file, x, y, w, h, opt = {}) {
  slide.images.add({
    blob: await bytes(file),
    contentType: contentType(file),
    alt: opt.alt ?? "图片",
    fit: opt.fit ?? "cover",
    position: { left: x, top: y, width: w, height: h },
    geometry: opt.geometry ?? "rect",
    borderRadius: opt.radius,
  });
}

function header(slide, section, title, no) {
  slide.background.fill = "#ffffff";
  addText(slide, section, M, 30, 340, 24, { size: 15, bold: true, color: blue });
  addText(slide, title, M, 62, 950, 58, { size: 38, bold: true, color: ink });
  addRule(slide, M, 136, W - M * 2);
  addText(slide, String(no).padStart(2, "0"), W - 98, 650, 42, 22, { size: 14, color: muted, align: "right" });
}

function note(slide, text) {
  slide.speakerNotes.textFrame.setText(text);
  slide.speakerNotes.setVisible(true);
}

function bullets(slide, items, x, y, w, opt = {}) {
  const gap = opt.gap ?? 46;
  items.forEach((item, i) => {
    const yy = y + i * gap;
    const color = opt.colors?.[i] ?? blue;
    slide.shapes.add({
      geometry: "ellipse",
      position: { left: x, top: yy + 10, width: 9, height: 9 },
      fill: color,
      line: { style: "solid", fill: "none", width: 0 },
    });
    addText(slide, item, x + 24, yy, w - 24, gap - 4, { size: opt.size ?? 20, color: opt.color ?? ink });
  });
}

function stat(slide, value, label, x, y, w, color = blue) {
  addBox(slide, x, y, w, 94, { fill: "#ffffff", stroke: line });
  addText(slide, value, x + 20, y + 15, w - 40, 34, { size: 30, bold: true, color });
  addText(slide, label, x + 20, y + 55, w - 40, 24, { size: 16, color: muted });
}

function flow(slide, steps, x, y, w, color = blue) {
  const gap = 14;
  const stepW = (w - gap * (steps.length - 1)) / steps.length;
  steps.forEach((step, i) => {
    addBox(slide, x + i * (stepW + gap), y, stepW, 64, { fill: i === 0 ? `${color}18` : "#ffffff", stroke: i === 0 ? `${color}66` : line });
    addText(slide, step, x + i * (stepW + gap) + 8, y + 20, stepW - 16, 24, { size: 16, bold: true, align: "center", color: i === 0 ? color : ink });
    if (i < steps.length - 1) {
      addText(slide, "→", x + i * (stepW + gap) + stepW - 2, y + 16, 18, 24, { size: 22, bold: true, color: muted, align: "center" });
    }
  });
}

const deck = Presentation.create({ slideSize: { width: W, height: H } });
let page = 1;

// 1
{
  const s = deck.slides.add();
  await addRawImage(s, images.cover, 0, 0, W, H, { fit: "cover" });
  s.shapes.add({
    geometry: "rect",
    position: { left: 0, top: 0, width: 760, height: H },
    fill: "#0b1728",
    line: { style: "solid", fill: "none", width: 0 },
  });
  s.shapes.add({
    geometry: "rect",
    position: { left: 760, top: 0, width: 170, height: H },
    fill: "#1e3a5f",
    line: { style: "solid", fill: "none", width: 0 },
  });
  addText(s, "软件与理论综合设计与实践", M, 54, 420, 28, { size: 18, bold: true, color: "#dbeafe" });
  addText(s, "校园二手物品交易平台\n的设计与实现", M, 155, 720, 136, { size: 55, bold: true, color: "#ffffff" });
  addText(s, "同校面对面  交易更安全", M, 318, 560, 34, { size: 28, bold: true, color: "#bfdbfe" });
  addBox(s, M, 520, 420, 82, { fill: "#ffffff", stroke: "#dbeafe", radius: "rounded-xl" });
  addText(s, "答辩汇报", M + 24, 538, 160, 30, { size: 24, bold: true, color: ink });
  addText(s, "Vue3 + Spring Boot + MySQL + Redis", M + 24, 570, 330, 24, { size: 16, color: muted });
  addText(s, "01", W - 100, 650, 40, 22, { size: 14, color: "#e2e8f0", align: "right" });
  note(s, "各位老师好，我们小组本次答辩的项目是校园二手物品交易平台的设计与实现。这个系统面向高校师生，目标是把校内闲置物品交易从分散的信息流，整理成可搜索、可沟通、可审核、可追踪的完整平台。");
  page++;
}

// 2
{
  const s = deck.slides.add();
  header(s, "汇报结构", "这次答辩围绕系统价值、关键设计和团队协作展开", page++);
  const items = [
    ["系统简介", "项目背景、用户角色、总体用例和功能模块"],
    ["重要设计和实现", "系统架构、设计模式、推荐算法和交易闭环"],
    ["成员分工", "小组职责划分、实现结果和后续优化方向"],
  ];
  items.forEach((it, i) => {
    const x = M + i * 390;
    addBox(s, x, 210, 340, 270, { fill: i === 1 ? pale : "#ffffff", stroke: i === 1 ? "#93c5fd" : line });
    addText(s, `0${i + 1}`, x + 24, 234, 70, 38, { size: 34, bold: true, color: i === 1 ? blue : muted });
    addText(s, it[0], x + 24, 304, 230, 36, { size: 30, bold: true });
    addText(s, it[1], x + 24, 360, 276, 78, { size: 20, color: muted });
  });
  addText(s, "核心结论：系统不仅完成了页面展示，还形成了从发布、审核、推荐、交易到治理的闭环。", M, 560, 1040, 40, { size: 25, bold: true, color: blue });
  note(s, "这次汇报分为三个部分。第一部分介绍系统为什么做、服务谁、具备哪些功能。第二部分讲关键设计和实现，包括状态模式、策略模式、外观模式、推荐算法和完整业务流程。第三部分说明成员分工和项目总结。");
}

// 3
{
  const s = deck.slides.add();
  header(s, "第一章  系统简介", "校园二手交易的核心问题是信息分散和流程不可信", page++);
  await addImage(s, images.market, 610, 180, 570, 270, { alt: "商品市场搜索界面截图" });
  addPill(s, "项目定位", M, 180, 116);
  addText(s, "面向高校师生的 C2C 二手交易平台", M, 230, 470, 42, { size: 32, bold: true });
  bullets(s, [
    "把微信群、朋友圈中的闲置信息集中到统一市场",
    "通过分类、搜索、排序和推荐提高商品发现效率",
    "通过审核、举报、消息留痕和后台管理提高交易安全性",
  ], M, 305, 500, { gap: 58, size: 21 });
  addText(s, "技术路线：Vue3 单页应用 + Spring Boot REST API + MySQL 数据持久化 + Redis 缓存与热度统计", M, 560, 970, 32, { size: 22, bold: true, color: blue2 });
  note(s, "项目背景来自校园二手交易的真实痛点。传统交易方式信息分散，用户很难检索，也难以判断交易是否安全。我们的做法是建设一个统一的 Web 平台，让商品发布、市场浏览、沟通、下单和后台治理都在系统内完成。");
}

// 4
{
  const s = deck.slides.add();
  header(s, "第一章  系统简介", "总体用例覆盖买家、卖家和管理员三类角色", page++);
  await addImage(s, images.usecaseTop, M, 175, 230, 430, { alt: "系统总体用例图上半部分", fit: "contain" });
  await addImage(s, images.usecaseMiddle, M + 260, 175, 230, 430, { alt: "系统总体用例图中部", fit: "contain" });
  await addImage(s, images.usecaseBottom, M + 520, 175, 230, 430, { alt: "系统总体用例图下半部分", fit: "contain" });
  addBox(s, 860, 188, 300, 120, { fill: pale, stroke: "#bfdbfe" });
  addText(s, "普通用户", 886, 210, 180, 28, { size: 25, bold: true, color: blue });
  addText(s, "注册登录、浏览搜索、收藏、沟通、下单、举报", 886, 252, 230, 44, { size: 18, color: ink });
  addBox(s, 860, 336, 300, 120, { fill: "#fff7ed", stroke: "#fed7aa" });
  addText(s, "卖家", 886, 358, 180, 28, { size: 25, bold: true, color: orange });
  addText(s, "发布商品、维护商品、处理订单、填写物流", 886, 400, 230, 44, { size: 18, color: ink });
  addBox(s, 860, 484, 300, 120, { fill: "#f0fdf4", stroke: "#bbf7d0" });
  addText(s, "管理员", 886, 506, 180, 28, { size: 25, bold: true, color: green });
  addText(s, "商品审核、用户管理、举报处理、公告管理", 886, 548, 230, 44, { size: 18, color: ink });
  note(s, "这一页展示的是系统总体用例。系统有普通用户、卖家和管理员三类角色。普通用户既可以作为买家，也可以作为卖家。管理员负责审核和治理，保证商品信息和交易流程更规范。");
}

// 5
{
  const s = deck.slides.add();
  header(s, "第一章  系统简介", "六大功能模块共同支撑校园交易闭环", page++);
  await addImage(s, images.cats, M, 180, 520, 120, { alt: "商品分类入口截图" });
  await addImage(s, images.hot, 835, 176, 310, 388, { alt: "热门榜和公告截图" });
  const mods = [
    ["用户认证", "注册、登录、资料、地址"],
    ["商品管理", "发布、搜索、详情、收藏"],
    ["订单交易", "购物车、支付、发货、收货"],
    ["消息通知", "站内留言、系统通知"],
    ["后台管理", "审核、公告、用户、数据"],
    ["扩展治理", "推荐、敏感词、举报、信誉"],
  ];
  mods.forEach((m, i) => {
    const x = M + (i % 2) * 350;
    const y = 342 + Math.floor(i / 2) * 76;
    addBox(s, x, y, 310, 52, { fill: i === 5 ? "#fff7ed" : "#ffffff", stroke: i === 5 ? "#fed7aa" : line });
    addText(s, m[0], x + 18, y + 13, 92, 22, { size: 19, bold: true, color: i === 5 ? orange : blue });
    addText(s, m[1], x + 122, y + 15, 160, 20, { size: 16, color: ink });
  });
  note(s, "系统功能可以概括为六大模块：用户认证、商品管理、订单交易、消息通知、后台管理和扩展治理。它不是单一的展示页面，而是覆盖了用户交易前、交易中和交易后的完整过程。");
}

// 6
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "前后端分离让展示、业务和数据职责清晰", page++);
  const layers = [
    ["前端展示层", "Vue3 / Router / Pinia / Axios", "#eff6ff", blue],
    ["接口控制层", "Controller 接收 REST 请求", "#f8fafc", ink],
    ["业务逻辑层", "Service 处理订单、审核、推荐、通知", "#fff7ed", orange],
    ["数据持久层", "Mapper + MySQL 14 张业务表", "#f0fdf4", green],
    ["缓存增强层", "Redis 验证码、限流、热度排行、缓存降级", "#fef2f2", red],
  ];
  layers.forEach((l, i) => {
    addBox(s, M, 178 + i * 76, 560, 56, { fill: l[2], stroke: line });
    addText(s, l[0], M + 22, 193 + i * 76, 150, 24, { size: 21, bold: true, color: l[3] });
    addText(s, l[1], M + 190, 195 + i * 76, 330, 22, { size: 18, color: ink });
  });
  await addImage(s, images.dash, 700, 184, 430, 290, { alt: "后台数据概览截图" });
  stat(s, "14", "页面视图", 700, 508, 125, blue);
  stat(s, "14", "业务表", 850, 508, 125, green);
  stat(s, "Redis", "缓存与热度", 1000, 508, 130, orange);
  note(s, "系统架构采用前后端分离。前端负责页面展示和状态管理，后端按 Controller、Service、Mapper 分层处理请求，数据库负责核心数据保存，Redis 用于验证码、限流、缓存和热度排行。这样的结构让系统更容易维护和扩展。");
}

// 7
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "状态模式约束商品和订单只能按合法路径流转", page++);
  addText(s, "商品状态", M, 184, 140, 30, { size: 24, bold: true, color: blue });
  flow(s, ["待审核", "在售", "已下架", "已售出"], M, 230, 590, blue);
  addText(s, "订单状态", M, 344, 140, 30, { size: 24, bold: true, color: orange });
  flow(s, ["待付款", "已付款", "已发货", "已完成"], M, 390, 590, orange);
  addBox(s, M, 510, 590, 88, { fill: "#fff7ed", stroke: "#fed7aa" });
  addText(s, "设计价值", M + 24, 528, 120, 26, { size: 23, bold: true, color: orange });
  addText(s, "减少非法操作，例如未付款不能发货、已完成订单不能再次取消。", M + 145, 531, 400, 28, { size: 19, color: ink });
  await addImage(s, images.orders, 724, 188, 390, 400, { alt: "订单管理界面截图" });
  note(s, "状态模式主要体现在商品和订单生命周期上。商品从待审核到在售，再到下架或售出；订单从待付款到已付款、已发货、已完成。系统只允许符合当前状态的操作执行，从而避免业务状态混乱。");
}

// 8
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "策略模式让商品展示可以按场景选择不同排序逻辑", page++);
  await addImage(s, images.rec, M, 185, 500, 220, { alt: "推荐商品卡片截图" });
  await addImage(s, images.guess, M, 430, 500, 150, { alt: "猜你喜欢推荐截图" });
  const strategies = [
    ["最新策略", "发布时间越近，排序越靠前"],
    ["热度策略", "浏览量和收藏行为提高曝光"],
    ["价格策略", "支持升序或降序比较"],
    ["行为推荐", "结合浏览、搜索、收藏、购买形成画像"],
  ];
  strategies.forEach((st, i) => {
    const y = 190 + i * 86;
    addBox(s, 660, y, 470, 62, { fill: i === 3 ? pale : "#ffffff", stroke: i === 3 ? "#93c5fd" : line });
    addText(s, st[0], 684, y + 17, 110, 24, { size: 20, bold: true, color: i === 3 ? blue : ink });
    addText(s, st[1], 810, y + 19, 280, 22, { size: 17, color: muted });
  });
  note(s, "策略模式主要用在商品排序和推荐中。市场页可以选择最新、热度、价格等不同策略；首页推荐则使用行为推荐策略。这样系统可以根据不同场景切换算法，而不是把所有排序逻辑写死。");
}

// 9
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "外观模式把复杂交易流程封装成简单接口", page++);
  addText(s, "前端只关心一个动作", M, 180, 360, 32, { size: 27, bold: true });
  addBox(s, M, 235, 320, 86, { fill: pale, stroke: "#93c5fd" });
  addText(s, "点击“立即购买”", M + 36, 262, 230, 28, { size: 25, bold: true, color: blue, align: "center" });
  addText(s, "后端封装完整业务", M, 365, 360, 32, { size: 27, bold: true });
  flow(s, ["身份校验", "商品校验", "创建订单", "更新状态", "发送通知"], M, 420, 610, green);
  addBox(s, M, 540, 610, 58, { fill: "#f0fdf4", stroke: "#bbf7d0" });
  addText(s, "Service 层提供统一入口，隐藏内部多表更新、日志记录和通知推送细节。", M + 24, 556, 550, 24, { size: 19, color: ink });
  await addImage(s, images.publish, 720, 180, 380, 180, { alt: "发布商品界面截图" });
  await addImage(s, images.msg, 720, 400, 380, 170, { alt: "消息中心界面截图" });
  note(s, "外观模式体现在业务服务封装中。比如下单，前端只需要调用一个购买接口，但后端内部要校验身份、校验商品、创建订单、更新状态并发送通知。Service 层就相当于外观，降低了调用复杂度。");
}

// 10
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "推荐算法根据用户行为画像和商品热度综合评分", page++);
  const weights = [
    ["浏览", "8", "#dbeafe", blue],
    ["搜索", "14", "#e0f2fe", blue2],
    ["收藏", "30", "#fff7ed", orange],
    ["购买", "45", "#f0fdf4", green],
  ];
  weights.forEach((w, i) => {
    const x = M + i * 142;
    addBox(s, x, 185, 118, 96, { fill: w[2], stroke: line });
    addText(s, w[1], x + 20, 204, 78, 34, { size: 32, bold: true, color: w[3], align: "center" });
    addText(s, w[0], x + 20, 244, 78, 22, { size: 18, bold: true, color: ink, align: "center" });
  });
  addText(s, "行为采集 → 时间衰减 → 兴趣画像 → 候选商品 → 综合评分 → 推荐展示", M, 330, 660, 34, { size: 25, bold: true, color: blue });
  bullets(s, [
    "画像维度：分类偏好、关键词偏好、成色偏好、价格偏好",
    "商品因素：浏览量、收藏/交易热度、发布时间新鲜度",
    "冷启动：行为较少时优先推荐热门商品和最新商品",
  ], M, 400, 620, { gap: 54, size: 20 });
  await addImage(s, images.guess, 760, 200, 360, 330, { alt: "首页猜你喜欢推荐截图" });
  note(s, "推荐算法使用用户行为画像加多因子评分。浏览、搜索、收藏和购买对应不同权重，购买和收藏代表更强兴趣。同时加入时间衰减，近期行为影响更大。系统根据分类、关键词、成色和价格偏好匹配候选商品，再结合热度和新鲜度排序。");
}

// 11
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "系统实现了从发布到治理的完整交易闭环", page++);
  const steps = ["发布商品", "敏感词检测", "管理员审核", "市场展示", "沟通下单", "支付发货", "收货完成", "举报处理"];
  flow(s, steps, M, 176, W - M * 2, blue);
  await addImage(s, images.publish, M, 292, 250, 160, { alt: "发布商品截图" });
  await addImage(s, images.market, 337, 292, 250, 160, { alt: "商品市场截图" });
  await addImage(s, images.orders, 618, 292, 250, 160, { alt: "订单中心截图" });
  await addImage(s, images.words, 899, 292, 250, 160, { alt: "敏感词管理截图" });
  addBox(s, M, 520, 1090, 76, { fill: pale, stroke: "#bfdbfe" });
  addText(s, "答辩重点", M + 28, 542, 120, 26, { size: 23, bold: true, color: blue });
  addText(s, "这套流程证明系统不是静态页面，而是具备发布、审核、交易、通知和治理能力的业务系统。", M + 160, 544, 760, 25, { size: 20, color: ink });
  note(s, "这一页是完整功能实现的总结。用户发布商品后，系统先进行敏感词检测，再进入管理员审核。审核通过后商品进入市场展示，买家可以搜索、沟通、下单、支付，卖家发货，买家确认收货。如果出现违规或纠纷，还可以举报并由后台处理。");
}

// 12
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "后台治理保证平台内容可控、交易可追踪", page++);
  await addImage(s, images.dash, M, 180, 360, 185, { alt: "后台数据概览截图" });
  await addImage(s, images.users, 460, 180, 320, 185, { alt: "用户管理截图" });
  await addImage(s, images.ann, 815, 180, 320, 185, { alt: "公告管理截图" });
  await addImage(s, images.words, M, 405, 360, 145, { alt: "敏感词管理截图" });
  await addImage(s, images.safety, 460, 405, 320, 145, { alt: "安全告示页面截图" });
  addBox(s, 815, 405, 320, 145, { fill: "#ffffff", stroke: line });
  bullets(s, [
    "商品审核：待审核商品通过后才公开展示",
    "用户管理：异常账号可禁用或恢复",
    "举报处理：交易纠纷和违规商品进入后台闭环",
  ], 838, 428, 260, { gap: 36, size: 17, colors: [green, orange, red] });
  note(s, "后台管理是平台治理的核心。管理员可以看到数据概览，进行用户管理、公告管理、敏感词维护和安全告示发布。同时，商品审核和举报处理让平台具备内容把关和纠纷处理能力。");
}

// 13
{
  const s = deck.slides.add();
  header(s, "第二章  重要设计和实现", "测试结果说明核心流程可以稳定运行", page++);
  stat(s, "24", "后端 MockMvc 集成测试", M, 188, 255, blue);
  stat(s, "16", "前端 Vitest 测试文件", M + 285, 188, 255, green);
  stat(s, "10", "黑盒关键业务流程", M + 570, 188, 255, orange);
  stat(s, "Build", "前端生产构建通过", M + 855, 188, 255, blue2);
  addText(s, "重点验证场景", M, 345, 240, 34, { size: 28, bold: true });
  const tests = [
    ["搜索分页", "100 条商品数据下，分页与筛选响应正常"],
    ["并发下单", "通过乐观锁避免同一商品被重复购买"],
    ["Redis 降级", "缓存不可用时，核心业务仍能回退到数据库"],
  ];
  tests.forEach((t, i) => {
    const x = M + i * 370;
    addBox(s, x, 410, 320, 120, { fill: i === 1 ? "#fff7ed" : "#ffffff", stroke: i === 1 ? "#fed7aa" : line });
    addText(s, t[0], x + 22, 432, 190, 28, { size: 24, bold: true, color: i === 1 ? orange : blue });
    addText(s, t[1], x + 22, 474, 260, 42, { size: 18, color: muted });
  });
  addText(s, "结论：功能完整、业务流程正确、安全控制和并发处理达到课程验收要求。", M, 590, 940, 30, { size: 23, bold: true, color: blue });
  note(s, "在测试方面，后端有 24 个 MockMvc 集成测试，前端有 16 个 Vitest 测试文件，还设计了 10 组黑盒业务流程。重点验证了搜索分页、并发下单和 Redis 降级。测试结果说明系统核心流程能够稳定运行。");
}

// 14
{
  const s = deck.slides.add();
  header(s, "第三章  成员分工", "四名成员围绕架构、业务、前端和文档协同完成项目", page++);
  const rows = [
    ["刘荣沁", "项目规划、后端架构、订单模块、数据库设计、部署配置"],
    ["明冬阳", "用户认证、JWT、Redis、商品后端、后端测试"],
    ["朱大前", "前端架构、首页/市场/详情页、后台管理、消息通知、推荐与举报扩展"],
    ["邓先毅", "个人中心、订单中心、消息中心、购物车、支付页、UI/UX、前端测试、文档"],
  ];
  rows.forEach((r, i) => {
    const y = 180 + i * 76;
    addBox(s, M, y, 1080, 58, { fill: i % 2 === 0 ? "#ffffff" : panel, stroke: line });
    addText(s, r[0], M + 22, y + 15, 105, 24, { size: 21, bold: true, color: blue });
    addText(s, r[1], M + 150, y + 16, 840, 24, { size: 18, color: ink });
  });
  addBox(s, M, 525, 1080, 78, { fill: pale, stroke: "#bfdbfe" });
  addText(s, "总结", M + 26, 546, 70, 28, { size: 24, bold: true, color: blue });
  addText(s, "系统完成了校园二手交易从需求分析、功能实现、后台治理到测试验证的完整实践，后续可继续优化真实支付、实时聊天和移动端适配。", M + 110, 548, 890, 28, { size: 19, color: ink });
  note(s, "最后是成员分工。四名成员分别承担项目规划和后端架构、用户认证和 Redis、前端架构和扩展功能、业务页面和 UI 测试等工作。总体来看，本项目完成了校园二手交易平台从需求分析、功能实现、后台治理到测试验证的完整实践。后续可以继续优化真实支付、实时聊天和移动端适配。我的汇报到这里，谢谢各位老师。");
}

await fs.mkdir(QA, { recursive: true });
for (const [i, slide] of deck.slides.items.entries()) {
  const stem = `slide-${String(i + 1).padStart(2, "0")}`;
  await writeBlob(`${QA}/${stem}.png`, await deck.export({ slide, format: "png", scale: 1 }));
  const layout = await slide.export({ format: "layout" });
  await fs.writeFile(`${QA}/${stem}.layout.json`, await layout.text(), "utf8");
}
await writeBlob(`${QA}/montage.webp`, await deck.export({ format: "webp", montage: true, scale: 1 }));
const pptx = await PresentationFile.exportPptx(deck);
await pptx.save(FINAL);
console.log(FINAL);
