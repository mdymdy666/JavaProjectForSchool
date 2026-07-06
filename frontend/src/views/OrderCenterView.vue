<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createOrder, payOrder, shipOrder, confirmOrder, cancelOrder, getBuyerOrders, getSellerOrders } from '../api/order'
import StatusTag from '../components/StatusTag.vue'
import type { OrderView } from '../types/domain'

const route = useRoute()
const router = useRouter()
const activeTab = ref<'buyer' | 'seller'>('buyer')
const buyerOrders = ref<OrderView[]>([])
const sellerOrders = ref<OrderView[]>([])
const error = ref('')
const trackOrder = ref<OrderView | null>(null)
const trackSteps = ref<{ time: string; text: string }[]>([])

async function fetch() {
  try {
    const [bRes, sRes] = await Promise.all([
      getBuyerOrders(), getSellerOrders()
    ])
    buyerOrders.value = bRes.data || []
    sellerOrders.value = sRes.data || []
  } catch { /* ignore */ }
}

async function buy() {
  const productId = Number(route.query.productId)
  if (!productId) return
  error.value = ''
  try {
    const res = await createOrder({ productId })
    if (res.code === 200) {
      await fetch()
      router.replace({ path: '/orders', query: {} })
    } else {
      error.value = res.message || '下单失败'
    }
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg || '下单失败'
  }
}

async function act(id: number, fn: (id: number) => Promise<unknown>) {
  try { await fn(id); await fetch() } catch { /* ignore */ }
}

async function ship(id: number) {
  const logisticsInfo = window.prompt('请输入快递单号、校内自提点或配送备注')?.trim()
  if (!logisticsInfo) return
  try { await shipOrder(id, logisticsInfo); await fetch() } catch { /* ignore */ }
}

function goPay(orderId: number) {
  router.push(`/pay/${orderId}`)
}

function showTrack(o: OrderView) {
  trackOrder.value = o
  const created = new Date(o.createdAt)
  const steps = [
    { time: fmt(created), text: '订单已创建' },
    { time: fmt(new Date(+created + 600000)), text: '买家已支付' },
    { time: fmt(new Date(+created + 1800000)), text: o.logisticsInfo ? `卖家已发货 — ${o.logisticsInfo}` : '卖家已发货' },
    { time: fmt(new Date(+created + 7200000)), text: '快递已揽件' },
    { time: fmt(new Date(+created + 14400000)), text: '运输中' }
  ]
  if (o.status === 'COMPLETED') {
    steps.push(
      { time: fmt(new Date(+created + 43200000)), text: '派送中' },
      { time: fmt(new Date(+created + 86400000)), text: '已签收' }
    )
  }
  trackSteps.value = steps
}
function closeTrack() { trackOrder.value = null }
function fmt(d: Date) { return d.toLocaleString('zh-CN', { hour12: false }) }

onMounted(async () => {
  await fetch()
  if (route.query.action === 'buy') await buy()
})
</script>

<template>
  <div class="order-page">
    <h2>订单中心</h2>
    <p v-if="error" class="err">{{ error }}</p>

    <div class="tabs">
      <button :class="{ active: activeTab === 'buyer' }" @click="activeTab = 'buyer'">我买的</button>
      <button :class="{ active: activeTab === 'seller' }" @click="activeTab = 'seller'">我卖的</button>
    </div>

    <div v-if="activeTab === 'buyer'">
      <div v-if="buyerOrders.length" class="order-list">
        <div v-for="o in buyerOrders" :key="o.id" class="order-card">
          <div class="order-head">
            <strong>{{ o.productTitle }}</strong>
            <StatusTag :status="o.status" />
          </div>
          <div class="order-info">
            <span>订单号: {{ o.orderNo }}</span>
            <span>卖家: {{ o.sellerNickname }}</span>
            <span class="amount">&yen;{{ o.totalAmount?.toFixed(2) }}</span>
          </div>
          <div class="order-actions">
            <button v-if="o.status === 'PENDING_PAYMENT'" @click="goPay(o.id)">支付</button>
            <button v-if="o.status === 'SHIPPED'" @click="act(o.id, confirmOrder)">确认收货</button>
            <button v-if="o.status === 'PENDING_PAYMENT'" class="ghost" @click="act(o.id, cancelOrder)">取消</button>
            <button v-if="o.status === 'SHIPPED' || o.status === 'COMPLETED'" class="track-btn" @click="showTrack(o)">查看物流</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无买家订单</p>
    </div>

    <div v-if="activeTab === 'seller'">
      <div v-if="sellerOrders.length" class="order-list">
        <div v-for="o in sellerOrders" :key="o.id" class="order-card">
          <div class="order-head">
            <strong>{{ o.productTitle }}</strong>
            <StatusTag :status="o.status" />
          </div>
          <div class="order-info">
            <span>订单号: {{ o.orderNo }}</span>
            <span>买家: {{ o.buyerNickname }}</span>
            <span class="amount">&yen;{{ o.totalAmount?.toFixed(2) }}</span>
          </div>
          <div class="order-actions">
            <button v-if="o.status === 'PAID'" @click="ship(o.id)">发货</button>
            <button v-if="o.status === 'SHIPPED' || o.status === 'COMPLETED'" class="track-btn" @click="showTrack(o)">查看物流</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无卖家订单</p>
    </div>

    <!-- 物流轨迹弹窗 -->
    <div v-if="trackOrder" class="track-modal" @click.self="closeTrack">
      <div class="track-card">
        <h3>物流信息</h3>
        <p class="track-no">{{ trackOrder.logisticsInfo || '暂无物流单号' }}</p>
        <div class="track-timeline">
          <div v-for="(s, i) in trackSteps" :key="i" :class="['track-step', { done: i < trackSteps.length - (trackOrder.status === 'COMPLETED' ? 0 : 2) }]">
            <div class="track-dot" />
            <div class="track-body">
              <p class="track-text">{{ s.text }}</p>
              <small class="track-time">{{ s.time }}</small>
            </div>
          </div>
        </div>
        <button class="close-btn" @click="closeTrack">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-page { max-width: 760px; margin: 0 auto; }
.order-page h2 { margin: 0 0 16px; }
.err { color: #ff4d4f; font-size: 14px; margin-bottom: 12px; }
.tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.tabs button {
  padding: 6px 20px; border: 1px solid #d9d9d9; border-radius: 6px;
  background: #fff; cursor: pointer; font-size: 14px;
}
.tabs button.active { background: #1677ff; color: #fff; border-color: #1677ff; }
.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 16px;
}
.order-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-info { display: flex; gap: 20px; font-size: 13px; color: #666; margin-bottom: 10px; }
.amount { color: #ff4d4f; font-weight: 600; }
.order-actions { display: flex; gap: 8px; }
.order-actions button {
  padding: 5px 16px; border: none; border-radius: 4px; cursor: pointer;
  background: #1677ff; color: #fff; font-size: 13px;
}
.order-actions button.ghost { background: #fff; color: #666; border: 1px solid #d9d9d9; }
.order-actions button.track-btn { background: #fff; color: #1677ff; border: 1px solid #1677ff; }
.empty { color: #999; text-align: center; padding: 32px; }

/* 物流弹窗 */
.track-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 200; }
.track-card { background: #fff; border-radius: 12px; padding: 28px; width: 420px; max-width: 90vw; max-height: 80vh; overflow-y: auto; }
.track-card h3 { margin: 0 0 6px; font-size: 18px; }
.track-no { color: #888; font-size: 13px; margin: 0 0 20px; }
.track-timeline { position: relative; padding-left: 24px; }
.track-step { position: relative; padding-bottom: 20px; }
.track-step:last-child { padding-bottom: 0; }
.track-dot { position: absolute; left: -20px; top: 4px; width: 10px; height: 10px; border-radius: 50%; background: #d9d9d9; }
.track-step.done .track-dot { background: #1677ff; }
.track-step::before { content: ''; position: absolute; left: -16px; top: 18px; width: 2px; height: calc(100% - 14px); background: #e8e8e8; }
.track-step:last-child::before { display: none; }
.track-step.done::before { background: #1677ff; }
.track-text { margin: 0; font-size: 14px; color: #999; }
.track-step.done .track-text { color: #333; }
.track-time { font-size: 12px; color: #bbb; }
.close-btn { margin-top: 20px; padding: 6px 20px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; font-size: 14px; }
</style>
