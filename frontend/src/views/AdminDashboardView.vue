<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  getDashboard,
  getPendingProducts,
  auditProduct,
  createAnnouncement,
  getReports,
  handleReport,
  getSensitiveWords,
  addSensitiveWord,
  setSensitiveWordStatus,
  type ReportView,
  type SensitiveWordView
} from '../api/admin'
import { apiGet, apiPut } from '../api/http'
import type { DashboardView, ProductDetail } from '../types/domain'
import StatusTag from '../components/StatusTag.vue'
import * as echarts from 'echarts'
import { formatMoney } from '../utils/money'

const router = useRouter()
const auth = useAuthStore()
if (!auth.isAdmin) { router.push('/') }

// ---- State ----
const activeTab = ref<'dashboard' | 'audit' | 'users' | 'orders' | 'reports' | 'content' | 'announce'>('dashboard')
const dashboard = ref<DashboardView | null>(null)

// Audit
const pending = ref<ProductDetail[]>([])
// Users
const users = ref<any[]>([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')
// Orders
const orders = ref<any[]>([])
const orderTotal = ref(0)
const orderPage = ref(1)
const orderStatus = ref('')
// Reports
const reports = ref<ReportView[]>([])
const reportStatus = ref('PENDING')
const reportMsg = ref('')
// Content review
const sensitiveWords = ref<SensitiveWordView[]>([])
const newSensitiveWord = ref('')
const contentMsg = ref('')
// Announce
const announceForm = ref({ title: '', content: '' })
const announceMsg = ref('')
// Charts
const pieChart = ref<echarts.ECharts | null>(null)
const lineChart = ref<echarts.ECharts | null>(null)

// ---- Fetch helpers ----
async function fetchDashboard() {
  try {
    const r = await getDashboard()
    dashboard.value = r.data || null
    await nextTick()
    renderCharts()
  } catch { /* */ }
}
async function fetchPending() {
  try { const r = await getPendingProducts(); pending.value = r.data?.records || [] } catch { /* */ }
}
async function fetchUsers() {
  try {
    const r = await apiGet<any>(`/admin/users?keyword=${encodeURIComponent(userKeyword.value)}&page=${userPage.value}&size=10`)
    users.value = r.data?.records || []
    userTotal.value = r.data?.total || 0
  } catch { /* */ }
}
async function fetchOrders() {
  try {
    const statusQ = orderStatus.value ? `&status=${orderStatus.value}` : ''
    const r = await apiGet<any>(`/admin/orders?page=${orderPage.value}&size=10${statusQ}`)
    orders.value = r.data?.records || []
    orderTotal.value = r.data?.total || 0
  } catch { /* */ }
}
async function fetchReports() {
  try {
    const r = await getReports(reportStatus.value || undefined)
    reports.value = r.data || []
  } catch { /* */ }
}
async function fetchSensitiveWords() {
  try {
    const r = await getSensitiveWords()
    sensitiveWords.value = r.data || []
  } catch { /* */ }
}

// ---- Actions ----
async function audit(id: number, approved: boolean) {
  const reason = approved
    ? '信息完整，准予上架'
    : window.prompt('请输入驳回原因，卖家会在商品详情和个人中心看到', '图片、描述或价格信息不完整')?.trim()
  if (!approved && !reason) return
  try { await auditProduct(id, approved, reason || '通过'); fetchPending(); fetchDashboard() } catch { /* */ }
}
async function toggleUserStatus(id: number) {
  try { await apiPut(`/admin/users/${id}/status`); fetchUsers() } catch { /* */ }
}
async function postAnnounce() {
  if (!announceForm.value.title.trim()) return
  try {
    await createAnnouncement(announceForm.value.title, announceForm.value.content)
    announceMsg.value = '公告已发布'; announceForm.value = { title: '', content: '' }
  } catch { announceMsg.value = '发布失败' }
}
async function processReport(id: number, status: string, offShelfProduct = false) {
  const fallback = status === 'REJECTED' ? '举报依据不足，已驳回' : offShelfProduct ? '举报属实，商品已下架' : '举报已处理'
  const handlingResult = window.prompt('填写处理说明', fallback) || fallback
  try {
    await handleReport(id, { status, handlingResult, offShelfProduct })
    reportMsg.value = '举报已处理'
    fetchReports()
  } catch (e: any) {
    reportMsg.value = e?.response?.data?.message || '处理失败'
  }
}
async function createSensitiveWord() {
  if (!newSensitiveWord.value.trim()) return
  try {
    await addSensitiveWord(newSensitiveWord.value)
    newSensitiveWord.value = ''
    contentMsg.value = '敏感词已保存'
    fetchSensitiveWords()
  } catch (e: any) {
    contentMsg.value = e?.response?.data?.message || '保存失败'
  }
}
async function toggleSensitiveWord(word: SensitiveWordView) {
  try {
    await setSensitiveWordStatus(word.id, !word.enabled)
    fetchSensitiveWords()
  } catch { contentMsg.value = '状态更新失败' }
}

// ---- ECharts ----
function renderCharts() {
  if (!dashboard.value) return
  renderPieChart()
  renderLineChart()
}
async function renderPieChart() {
  await nextTick()
  const el = document.getElementById('pie-chart')
  if (!el || !dashboard.value?.categories?.length) return
  if (pieChart.value) pieChart.value.dispose()
  const chart = echarts.init(el)
  const data = dashboard.value.categories.map((c: any) => ({ name: c.categoryName, value: c.productCount }))
  const total = data.reduce((sum: number, item: { value: number }) => sum + Number(item.value || 0), 0)
  chart.setOption({
    color: ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899'],
    tooltip: { trigger: 'item', formatter: '{b}<br/>商品数：{c}<br/>占比：{d}%' },
    legend: {
      type: 'scroll',
      bottom: 0,
      left: 'center',
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#475569', fontSize: 12 }
    },
    graphic: [{
      type: 'text',
      left: 'center',
      top: '42%',
      style: {
        text: `${total}\n商品`,
        textAlign: 'center',
        fill: '#17212b',
        fontSize: 18,
        fontWeight: 800,
        lineHeight: 24
      }
    }],
    series: [{
      name: '分类占比',
      type: 'pie',
      radius: ['48%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      minAngle: 8,
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
      data,
      label: {
        show: true,
        color: '#334155',
        fontSize: 12,
        formatter: (params: { name: string; percent: number }) => `${params.name}\n${Number(params.percent).toFixed(0)}%`
      },
      labelLine: { length: 14, length2: 10, smooth: true }
    }]
  })
  pieChart.value = chart
}
async function renderLineChart() {
  await nextTick()
  const el = document.getElementById('line-chart')
  if (!el) return
  try {
    const r = await apiGet<any>('/admin/trend')
    const data = r.data || []
    if (lineChart.value) lineChart.value.dispose()
    const chart = echarts.init(el)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['订单数', '成交额(元)'], bottom: 0 },
      xAxis: { type: 'category', data: data.map((d: any) => d.date.slice(5)) },
      yAxis: [
        { type: 'value', name: '订单数', minInterval: 1, splitNumber: 4, axisLabel: { formatter: (value: number) => `${Math.round(value)}` } },
        { type: 'value', name: '元', min: 0, axisLabel: { formatter: (value: number) => `${Math.round(value)}` } }
      ],
      series: [
        { name: '订单数', type: 'line', data: data.map((d: any) => d.orderCount), smooth: true },
        { name: '成交额(元)', type: 'line', yAxisIndex: 1, data: data.map((d: any) => d.amount), smooth: true }
      ]
    })
    lineChart.value = chart
  } catch { /* */ }
}

// ---- Tab switches ----
function switchTab(tab: string) {
  activeTab.value = tab as any
  if (tab === 'dashboard') { fetchDashboard() }
  else if (tab === 'audit') { fetchPending() }
  else if (tab === 'users') { fetchUsers() }
  else if (tab === 'orders') { fetchOrders() }
  else if (tab === 'reports') { fetchReports() }
  else if (tab === 'content') { fetchSensitiveWords() }
  else if (tab === 'announce') { }
}

function formatDate(value: string) {
  return value ? value.slice(0, 16).replace('T', ' ') : ''
}

onMounted(() => { fetchDashboard(); fetchPending() })
</script>

<template>
  <div class="admin-page">
    <h2>后台管理</h2>

    <div class="tabs">
      <button :class="{ active: activeTab === 'dashboard' }" @click="switchTab('dashboard')">数据概览</button>
      <button :class="{ active: activeTab === 'audit' }" @click="switchTab('audit')">商品审核</button>
      <button :class="{ active: activeTab === 'users' }" @click="switchTab('users')">用户管理</button>
      <button :class="{ active: activeTab === 'orders' }" @click="switchTab('orders')">订单管理</button>
      <button :class="{ active: activeTab === 'reports' }" @click="switchTab('reports')">举报处理</button>
      <button :class="{ active: activeTab === 'content' }" @click="switchTab('content')">内容审核</button>
      <button :class="{ active: activeTab === 'announce' }" @click="switchTab('announce')">公告管理</button>
    </div>

    <!-- ====== 数据概览 ====== -->
    <div v-if="activeTab === 'dashboard' && dashboard">
      <div class="stat-grid">
        <div class="stat-card"><strong>{{ dashboard.userCount }}</strong><span>注册用户</span></div>
        <div class="stat-card"><strong>{{ dashboard.productCount }}</strong><span>商品总数</span></div>
        <div class="stat-card"><strong>{{ dashboard.orderCount }}</strong><span>订单总数</span></div>
        <div class="stat-card"><strong>&yen;{{ formatMoney(dashboard.turnover) }}</strong><span>成交总额</span></div>
      </div>

      <div class="charts-row">
        <div class="chart-box">
          <h3>分类占比</h3>
          <div id="pie-chart" class="chart-canvas" />
        </div>
        <div class="chart-box">
          <h3>近 7 日趋势</h3>
          <div id="line-chart" class="chart-canvas" />
        </div>
      </div>
    </div>

    <!-- ====== 商品审核 ====== -->
    <div v-if="activeTab === 'audit'">
      <div v-if="pending.length" class="list-cards">
        <div v-for="p in pending" :key="p.id" class="row-card audit-card">
          <div class="audit-cover">
            <img v-if="p.images?.length" :src="p.images[0]" :alt="p.title" />
            <span v-else>暂无图片</span>
          </div>
          <div class="row-main">
            <strong>{{ p.title }}</strong>
            <p>{{ p.sellerNickname }} · {{ p.categoryName }} · &yen;{{ formatMoney(p.price) }} · {{ p.itemCondition }}</p>
            <p class="audit-desc">{{ p.description }}</p>
            <p class="audit-meta">发布时间：{{ formatDate(p.createdAt) }} · 图片 {{ p.images?.length || 0 }} 张</p>
          </div>
          <StatusTag :status="p.status" />
          <div class="actions">
            <button @click="audit(p.id, true)">通过</button>
            <button class="danger" @click="audit(p.id, false)">驳回</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无待审核商品</p>
    </div>

    <!-- ====== 用户管理 ====== -->
    <div v-if="activeTab === 'users'">
      <div class="search-bar">
        <input v-model="userKeyword" placeholder="搜索用户名/昵称..." @keyup.enter="userPage=1;fetchUsers()" />
        <button @click="userPage=1;fetchUsers()">搜索</button>
      </div>
      <table v-if="users.length" class="data-table">
        <thead>
          <tr><th>ID</th><th>用户名</th><th>昵称</th><th>角色</th><th>状态</th><th>注册时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.nickname }}</td>
            <td>{{ u.role === 'ADMIN' ? '管理员' : '用户' }}</td>
            <td>{{ u.status === 'NORMAL' ? '正常' : '已禁用' }}</td>
            <td>{{ u.createdAt?.slice(0, 10) }}</td>
            <td>
              <button
                v-if="u.role !== 'ADMIN'"
                :class="u.status === 'NORMAL' ? 'danger' : 'success'"
                @click="toggleUserStatus(u.id)"
              >{{ u.status === 'NORMAL' ? '禁用' : '取消禁用' }}</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="userTotal > 10" class="pager">
        <button :disabled="userPage <= 1" @click="userPage--; fetchUsers()">上一页</button>
        <span>{{ userPage }} / {{ Math.ceil(userTotal / 10) }}</span>
        <button :disabled="userPage >= Math.ceil(userTotal / 10)" @click="userPage++; fetchUsers()">下一页</button>
      </div>
      <p v-if="!users.length" class="empty">暂无用户</p>
    </div>

    <!-- ====== 订单管理 ====== -->
    <div v-if="activeTab === 'orders'">
      <div class="search-bar">
        <select v-model="orderStatus" @change="orderPage=1;fetchOrders()">
          <option value="">全部状态</option>
          <option value="PENDING_PAYMENT">待支付</option>
          <option value="PAID">已支付</option>
          <option value="SHIPPED">已发货</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELED">已取消</option>
        </select>
      </div>
      <table v-if="orders.length" class="data-table">
        <thead>
          <tr><th>订单号</th><th>商品</th><th>买家</th><th>卖家</th><th>金额</th><th>状态</th><th>物流</th><th>时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.id">
            <td>{{ o.orderNo?.slice(-8) }}</td>
            <td>{{ o.productTitle }}</td>
            <td>{{ o.buyerNickname }}</td>
            <td>{{ o.sellerNickname }}</td>
            <td>&yen;{{ formatMoney(o.totalAmount) }}</td>
            <td><StatusTag :status="o.status" /></td>
            <td>{{ o.logisticsInfo || '-' }}</td>
            <td>{{ o.createdAt?.slice(0, 10) }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="orderTotal > 10" class="pager">
        <button :disabled="orderPage <= 1" @click="orderPage--; fetchOrders()">上一页</button>
        <span>{{ orderPage }} / {{ Math.ceil(orderTotal / 10) }}</span>
        <button :disabled="orderPage >= Math.ceil(orderTotal / 10)" @click="orderPage++; fetchOrders()">下一页</button>
      </div>
      <p v-if="!orders.length" class="empty">暂无订单</p>
    </div>

    <!-- ====== 举报处理 ====== -->
    <div v-if="activeTab === 'reports'">
      <div class="search-bar">
        <select v-model="reportStatus" @change="fetchReports()">
          <option value="">全部状态</option>
          <option value="PENDING">待处理</option>
          <option value="RESOLVED">已处理</option>
          <option value="PRODUCT_REMOVED">商品已下架</option>
          <option value="REJECTED">已驳回</option>
        </select>
      </div>
      <div v-if="reports.length" class="list-cards">
        <div v-for="r in reports" :key="r.id" class="row-card report-row">
          <div class="row-main">
            <strong>{{ r.productTitle }}</strong>
            <p>举报人：{{ r.reporterNickname }} · 卖家：{{ r.sellerNickname }} · {{ r.createdAt?.slice(0, 16).replace('T', ' ') }}</p>
            <p class="report-reason">{{ r.reason }}</p>
            <p v-if="r.handlingResult" class="report-result">处理结果：{{ r.handlingResult }}</p>
          </div>
          <StatusTag :status="r.reportStatus" />
          <div v-if="r.reportStatus === 'PENDING'" class="actions">
            <button @click="processReport(r.id, 'RESOLVED')">已处理</button>
            <button class="danger" @click="processReport(r.id, 'PRODUCT_REMOVED', true)">下架商品</button>
            <button class="ghost" @click="processReport(r.id, 'REJECTED')">驳回</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无举报记录</p>
      <p v-if="reportMsg" :class="reportMsg.includes('失败') ? 'err' : 'ok'">{{ reportMsg }}</p>
    </div>

    <!-- ====== 内容审核 ====== -->
    <div v-if="activeTab === 'content'">
      <div class="search-bar">
        <input v-model="newSensitiveWord" placeholder="新增敏感词..." @keyup.enter="createSensitiveWord" />
        <button @click="createSensitiveWord">新增</button>
      </div>
      <table v-if="sensitiveWords.length" class="data-table">
        <thead>
          <tr><th>ID</th><th>敏感词</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="word in sensitiveWords" :key="word.id">
            <td>{{ word.id }}</td>
            <td>{{ word.word }}</td>
            <td>{{ word.enabled ? '启用' : '停用' }}</td>
            <td>
              <button :class="word.enabled ? 'danger' : ''" @click="toggleSensitiveWord(word)">
                {{ word.enabled ? '停用' : '启用' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">暂无敏感词</p>
      <p v-if="contentMsg" :class="contentMsg.includes('失败') ? 'err' : 'ok'">{{ contentMsg }}</p>
    </div>

    <!-- ====== 公告管理 ====== -->
    <div v-if="activeTab === 'announce'">
      <label>标题 <input v-model="announceForm.title" placeholder="公告标题" class="full-width" /></label>
      <label>内容 <textarea v-model="announceForm.content" rows="3" placeholder="公告内容" class="full-width" /></label>
      <button @click="postAnnounce">发布公告</button>
      <p v-if="announceMsg" :class="announceMsg.includes('失败') ? 'err' : 'ok'">{{ announceMsg }}</p>
    </div>
  </div>
</template>

<style scoped>
.admin-page { max-width: 960px; margin: 0 auto; }
h2 { margin: 0 0 16px; }
.tabs { display: flex; gap: 6px; margin-bottom: 20px; flex-wrap: wrap; }
.tabs button {
  padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 6px;
  background: #fff; color: #253044; cursor: pointer; font-size: 13px; font-weight: 700;
  min-width: 108px;
}
.tabs button:hover { color: #1677ff; border-color: #1677ff; background: #eef6ff; }
.tabs button.active { background: #1677ff; color: #fff; border-color: #1677ff; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px;
  padding: 20px; text-align: center;
}
.stat-card strong { display: block; font-size: 28px; color: #1677ff; margin-bottom: 4px; }
.stat-card span { color: #888; font-size: 13px; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.chart-box { background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 16px; }
.chart-box h3 { margin: 0 0 12px; font-size: 16px; }
.chart-canvas { width: 100%; height: 340px; }
.list-cards { display: flex; flex-direction: column; gap: 10px; }
.row-card {
  display: flex; align-items: center; gap: 16px;
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 12px 16px;
}
.audit-card { align-items: flex-start; }
.audit-cover { width: 112px; height: 84px; flex: 0 0 112px; overflow: hidden; border-radius: 8px; background: #f1f5f9; color: #94a3b8; display: grid; place-items: center; font-size: 12px; }
.audit-cover img { width: 100%; height: 100%; object-fit: cover; }
.row-main { flex: 1; min-width: 0; }
.row-card p { margin: 2px 0 0; font-size: 13px; color: #888; }
.audit-desc { color: #334155 !important; line-height: 1.55; display: -webkit-box; overflow: hidden; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.audit-meta { color: #64748b !important; }
.report-row { align-items: flex-start; }
.report-reason { color: #334155 !important; line-height: 1.55; }
.report-result { color: #16a34a !important; }
.actions { display: flex; gap: 6px; margin-left: auto; }
.actions button {
  padding: 4px 12px; border: none; border-radius: 4px; cursor: pointer;
  background: #1677ff; color: #fff; font-size: 13px;
}
.actions button.danger { background: #ff4d4f; }
.actions button.ghost { color: #64748b; background: #eef2f7; }
.search-bar { display: flex; gap: 8px; margin-bottom: 14px; }
.search-bar input, .search-bar select {
  padding: 6px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px;
}
.search-bar input { flex: 1; }
.search-bar button {
  padding: 6px 16px; background: #1677ff; color: #fff; border: none; border-radius: 6px; cursor: pointer;
}
.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; font-size: 13px; }
.data-table th { background: #fafafa; padding: 10px 12px; text-align: left; font-weight: 600; border-bottom: 1px solid #e8e8e8; }
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f0f0; }
.data-table td button { min-width: 76px; padding: 3px 10px; border: 1px solid #d9d9d9; border-radius: 4px; color: #253044; background: #fff; cursor: pointer; font-size: 12px; }
.data-table td button.danger { color: #ff4d4f; border-color: #ff4d4f; }
.data-table td button.success { color: #1677ff; border-color: #1677ff; }
.pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 16px; }
.pager button { padding: 5px 14px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; }
.pager button:disabled { opacity: 0.4; }
.full-width { display: block; width: 100%; margin-top: 4px; padding: 8px; border: 1px solid #d9d9d9; border-radius: 6px; box-sizing: border-box; }
label { display: block; margin-bottom: 12px; font-size: 14px; }
.ok { color: #52c41a; font-size: 13px; margin-top: 8px; }
.err { color: #ff4d4f; font-size: 13px; margin-top: 8px; }
.empty { color: #999; text-align: center; padding: 32px; }
</style>
