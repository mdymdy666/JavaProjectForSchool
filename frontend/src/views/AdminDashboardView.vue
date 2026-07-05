<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDashboard, getPendingProducts, auditProduct, createAnnouncement } from '../api/admin'
import type { DashboardView, ProductCard } from '../types/domain'
import StatusTag from '../components/StatusTag.vue'

const router = useRouter()
const auth = useAuthStore()

if (!auth.isAdmin) { router.push('/') }

const activeTab = ref<'dashboard' | 'audit' | 'announce'>('dashboard')
const dashboard = ref<DashboardView | null>(null)
const pending = ref<ProductCard[]>([])
const announceForm = reactive({ title: '', content: '' })
const announceMsg = ref('')

async function fetchDashboard() {
  try { const r = await getDashboard(); dashboard.value = r.data || null } catch { /* */ }
}
async function fetchPending() {
  try { const r = await getPendingProducts(); pending.value = r.data?.records || [] } catch { /* */ }
}
async function audit(id: number, approved: boolean) {
  try { await auditProduct(id, approved, approved ? '审核通过' : '信息不完整'); await fetchPending(); await fetchDashboard() } catch { /* */ }
}
async function postAnnounce() {
  if (!announceForm.title.trim()) return
  try {
    await createAnnouncement(announceForm.title, announceForm.content)
    announceMsg.value = '公告已发布'
    announceForm.title = ''
    announceForm.content = ''
  } catch { announceMsg.value = '发布失败' }
}

onMounted(() => { fetchDashboard(); fetchPending() })
</script>

<template>
  <div class="admin-page">
    <h2>后台管理</h2>

    <div class="tabs">
      <button :class="{ active: activeTab === 'dashboard' }" @click="activeTab = 'dashboard'">数据概览</button>
      <button :class="{ active: activeTab === 'audit' }" @click="activeTab = 'audit'">商品审核</button>
      <button :class="{ active: activeTab === 'announce' }" @click="activeTab = 'announce'">公告管理</button>
    </div>

    <!-- Dashboard -->
    <div v-if="activeTab === 'dashboard' && dashboard" class="dashboard">
      <div class="stat-grid">
        <div class="stat-card"><strong>{{ dashboard.userCount }}</strong><span>注册用户</span></div>
        <div class="stat-card"><strong>{{ dashboard.productCount }}</strong><span>商品总数</span></div>
        <div class="stat-card"><strong>{{ dashboard.orderCount }}</strong><span>订单总数</span></div>
        <div class="stat-card"><strong>&yen;{{ (dashboard.turnover || 0).toFixed(2) }}</strong><span>成交总额</span></div>
      </div>
      <h3>分类统计</h3>
      <div v-if="dashboard.categories?.length" class="cat-list">
        <div v-for="c in dashboard.categories" :key="c.name" class="cat-row">
          <span>{{ c.name }}</span>
          <div class="bar"><div :style="{ width: Math.max((c.count / (dashboard.productCount || 1)) * 100, 4) + '%' }" /></div>
          <strong>{{ c.count }}</strong>
        </div>
      </div>
    </div>

    <!-- Audit -->
    <div v-if="activeTab === 'audit'">
      <div v-if="pending.length" class="pending-list">
        <div v-for="p in pending" :key="p.id" class="pending-card">
          <div>
            <strong>{{ p.title }}</strong>
            <p>{{ p.sellerNickname }} · &yen;{{ p.price?.toFixed(2) }} · {{ p.itemCondition }}</p>
          </div>
          <StatusTag :status="p.status" />
          <div class="audit-actions">
            <button @click="audit(p.id, true)">通过</button>
            <button class="danger" @click="audit(p.id, false)">驳回</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无待审核商品</p>
    </div>

    <!-- Announcement -->
    <div v-if="activeTab === 'announce'" class="announce-section">
      <label>公告标题 <input v-model="announceForm.title" placeholder="公告标题" /></label>
      <label>公告内容 <textarea v-model="announceForm.content" rows="3" placeholder="公告内容" /></label>
      <button @click="postAnnounce">发布公告</button>
      <p v-if="announceMsg" class="msg">{{ announceMsg }}</p>
    </div>
  </div>
</template>

<style scoped>
.admin-page { max-width: 860px; margin: 0 auto; }
h2 { margin: 0 0 16px; }
.tabs { display: flex; gap: 8px; margin-bottom: 20px; }
.tabs button {
  padding: 6px 20px; border: 1px solid #d9d9d9; border-radius: 6px;
  background: #fff; cursor: pointer; font-size: 14px;
}
.tabs button.active { background: #1677ff; color: #fff; border-color: #1677ff; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px;
  padding: 20px; text-align: center;
}
.stat-card strong { display: block; font-size: 28px; color: #1677ff; margin-bottom: 4px; }
.stat-card span { color: #888; font-size: 13px; }
h3 { font-size: 17px; margin: 0 0 12px; }
.cat-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; font-size: 14px; }
.cat-row span { width: 80px; }
.bar { flex: 1; height: 18px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.bar div { height: 100%; background: #1677ff; border-radius: 4px; min-width: 4px; }
.pending-list { display: flex; flex-direction: column; gap: 10px; }
.pending-card {
  display: flex; align-items: center; gap: 16px;
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 14px 16px;
}
.pending-card p { margin: 2px 0 0; font-size: 13px; color: #888; }
.audit-actions { display: flex; gap: 6px; margin-left: auto; }
.audit-actions button {
  padding: 4px 12px; border: none; border-radius: 4px; cursor: pointer;
  background: #1677ff; color: #fff; font-size: 13px;
}
.audit-actions button.danger { background: #ff4d4f; }
.announce-section label { display: block; margin-bottom: 12px; font-size: 14px; }
.announce-section input, .announce-section textarea {
  display: block; width: 100%; margin-top: 4px; padding: 8px;
  border: 1px solid #d9d9d9; border-radius: 6px; box-sizing: border-box;
}
.announce-section button {
  padding: 8px 20px; background: #1677ff; color: #fff; border: none; border-radius: 6px; cursor: pointer;
}
.msg { margin-top: 8px; font-size: 13px; color: #52c41a; }
.empty { color: #999; text-align: center; padding: 32px; }
</style>
