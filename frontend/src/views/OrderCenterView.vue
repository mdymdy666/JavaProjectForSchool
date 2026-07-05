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

async function fetch() {
  try {
    const [bRes, sRes] = await Promise.all([
      getBuyerOrders(), getSellerOrders()
    ])
    buyerOrders.value = bRes.data?.records || []
    sellerOrders.value = sRes.data?.records || []
  } catch { /* ignore */ }
}

async function buy() {
  const productId = Number(route.query.productId)
  if (!productId) return
  error.value = ''
  try {
    const res = await createOrder({ productId })
    if (res.code === 0) {
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

function statusLabel(s: string) {
  const m: Record<string, string> = { PENDING_PAYMENT: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELED: '已取消' }
  return m[s] || s
}

async function act(id: number, fn: (id: number) => Promise<unknown>) {
  try { await fn(id); await fetch() } catch { /* ignore */ }
}

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
            <span class="amount">&yen;{{ o.amount?.toFixed(2) }}</span>
          </div>
          <div class="order-actions">
            <button v-if="o.status === 'PENDING_PAYMENT'" @click="act(o.id, payOrder)">支付</button>
            <button v-if="o.status === 'SHIPPED'" @click="act(o.id, confirmOrder)">确认收货</button>
            <button v-if="o.status === 'PENDING_PAYMENT'" class="ghost" @click="act(o.id, cancelOrder)">取消</button>
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
            <span class="amount">&yen;{{ o.amount?.toFixed(2) }}</span>
          </div>
          <div class="order-actions">
            <button v-if="o.status === 'PAID'" @click="act(o.id, shipOrder)">发货</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无卖家订单</p>
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
.empty { color: #999; text-align: center; padding: 32px; }
</style>
