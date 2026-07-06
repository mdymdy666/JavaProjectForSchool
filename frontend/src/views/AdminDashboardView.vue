<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDashboard, getPendingProducts, auditProduct, createAnnouncement } from '../api/admin'
import { apiGet, apiPut } from '../api/http'
import type { DashboardView, ProductCard } from '../types/domain'
import StatusTag from '../components/StatusTag.vue'
import * as echarts from 'echarts'

const router = useRouter()
const auth = useAuthStore()
if (!auth.isAdmin) { router.push('/') }

// ---- State ----
const activeTab = ref<'dashboard' | 'audit' | 'users' | 'orders' | 'announce'>('dashboard')
const dashboard = ref<DashboardView | null>(null)

// Audit
const pending = ref<ProductCard[]>([])
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

// ---- Actions ----
async function audit(id: number, approved: boolean) {
  try { await auditProduct(id, approved, approved ? '通过' : '驳回'); fetchPending(); fetchDashboard() } catch { /* */ }
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
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['45%', '70%'], center: ['50%', '45%'],
      data: dashboard.value.categories.map((c: any) => ({ name: c.categoryName, value: c.productCount })),
      label: { show: true, formatter: '{b}\n{d}%' }
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
        { type: 'value', name: '订单数' },
        { type: 'value', name: '元' }
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
  else if (tab === 'announce') { }
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
      <button :class="{ active: activeTab === 'announce' }" @click="switchTab('announce')">公告管理</button>
    </div>

    <!-- ====== 数据概览 ====== -->
    <div v-if="activeTab === 'dashboard' && dashboard">
      <div class="stat-grid">
        <div class="stat-card"><strong>{{ dashboard.userCount }}</strong><span>注册用户</span></div>
        <div class="stat-card"><strong>{{ dashboard.productCount }}</strong><span>商品总数</span></div>
        <div class="stat-card"><strong>{{ dashboard.orderCount }}</strong><span>订单总数</span></div>
        <div class="stat-card"><strong>&yen;{{ (dashboard.turnover || 0).toFixed(2) }}</strong><span>成交总额</span></div>
      </div>

      <div class="charts-row">
        <div class="chart-box">
          <h3>分类占比</h3>
          <div id="pie-chart" style="width:100%;height:300px" />
        </div>
        <div class="chart-box">
          <h3>近 7 日趋势</h3>
          <div id="line-chart" style="width:100%;height:300px" />
        </div>
      </div>
    </div>

    <!-- ====== 商品审核 ====== -->
    <div v-if="activeTab === 'audit'">
      <div v-if="pending.length" class="list-cards">
        <div v-for="p in pending" :key="p.id" class="row-card">
          <div>
            <strong>{{ p.title }}</strong>
            <p>{{ p.sellerNickname }} · &yen;{{ p.price?.toFixed(2) }} · {{ p.itemCondition }}</p>
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
                :class="u.status === 'NORMAL' ? 'danger' : ''"
                @click="toggleUserStatus(u.id)"
              >{{ u.status === 'NORMAL' ? '禁用' : '启用' }}</button>
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
            <td>&yen;{{ o.totalAmount?.toFixed(2) }}</td>
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
  background: #fff; cursor: pointer; font-size: 13px;
}
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
.list-cards { display: flex; flex-direction: column; gap: 10px; }
.row-card {
  display: flex; align-items: center; gap: 16px;
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 12px 16px;
}
.row-card p { margin: 2px 0 0; font-size: 13px; color: #888; }
.actions { display: flex; gap: 6px; margin-left: auto; }
.actions button {
  padding: 4px 12px; border: none; border-radius: 4px; cursor: pointer;
  background: #1677ff; color: #fff; font-size: 13px;
}
.actions button.danger { background: #ff4d4f; }
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
.data-table td button { padding: 3px 10px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; font-size: 12px; }
.data-table td button.danger { color: #ff4d4f; border-color: #ff4d4f; }
.pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 16px; }
.pager button { padding: 5px 14px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; }
.pager button:disabled { opacity: 0.4; }
.full-width { display: block; width: 100%; margin-top: 4px; padding: 8px; border: 1px solid #d9d9d9; border-radius: 6px; box-sizing: border-box; }
label { display: block; margin-bottom: 12px; font-size: 14px; }
.ok { color: #52c41a; font-size: 13px; margin-top: 8px; }
.err { color: #ff4d4f; font-size: 13px; margin-top: 8px; }
.empty { color: #999; text-align: center; padding: 32px; }
</style>
