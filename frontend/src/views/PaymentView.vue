<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { createOrder, payOrder } from '../api/order'
import { getProductDetail } from '../api/product'
import type { ProductDetail } from '../types/domain'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()

const product = ref<ProductDetail | null>(null)
const paying = ref(false)
const paid = ref(false)
const orderId = ref<number | null>(null)
const error = ref('')
const method = ref<'alipay' | 'wechat' | 'card'>('alipay')

const isBatch = computed(() => route.params.orderId === 'batch')
const isSingle = computed(() => route.params.orderId && route.params.orderId !== 'batch')

const title = computed(() => isBatch.value ? '购物车结算' : product.value?.title || '订单支付')
const amount = computed(() => isBatch.value ? cart.totalAmount : (product.value?.price || 0))

async function fetchProduct() {
  if (!isSingle.value) return
  try {
    const id = Number(route.params.orderId)
    const res = await getProductDetail(id)
    product.value = res.data || null
  } catch { /* */ }
}

function goOrders() { router.push('/orders') }
function goHome() { router.push('/') }

async function doPay() {
  paying.value = true
  error.value = ''
  try {
    if (isSingle.value) {
      // 直接购买: 创建订单 → 支付
      const oRes = await createOrder({ productId: Number(route.params.orderId) })
      if (oRes.code !== 200 || !oRes.data) { error.value = oRes.message || '下单失败'; return }
      orderId.value = oRes.data.id
      const pRes = await payOrder(oRes.data.id)
      if (pRes.code !== 200) { error.value = pRes.message || '支付失败'; return }
    } else {
      // 购物车结算: 逐个下单 + 支付
      if (cart.items.length === 0) { error.value = '购物车为空'; return }
      for (const item of cart.items) {
        const oRes = await createOrder({ productId: item.productId })
        if (oRes.code !== 200) { error.value = `"${item.title}" 下单失败`; return }
        const pRes = await payOrder(oRes.data!.id)
        if (pRes.code !== 200) { error.value = `"${item.title}" 支付失败`; return }
        orderId.value = oRes.data!.id
      }
      cart.clear()
    }
    paid.value = true
  } catch { error.value = '支付异常，请重试' }
  finally { paying.value = false }
}

fetchProduct()
</script>

<template>
  <div class="pay-page">
    <!-- 支付成功 -->
    <div v-if="paid" class="pay-success">
      <div class="check-icon">✓</div>
      <h2>支付成功</h2>
      <p>订单已提交，等待卖家发货</p>
      <div class="success-actions">
        <button @click="goOrders">查看订单</button>
        <button class="ghost" @click="goHome">返回首页</button>
      </div>
    </div>

    <!-- 支付表单 -->
    <template v-else>
      <h2>确认支付</h2>
      <div class="pay-card">
        <div class="order-summary">
          <span class="label">订单内容</span>
          <strong>{{ isBatch ? `${cart.totalCount} 件商品` : title }}</strong>
        </div>
        <div class="order-summary">
          <span class="label">应付金额</span>
          <strong class="money">&yen;{{ amount.toFixed(2) }}</strong>
        </div>

        <div class="method-section">
          <span class="label">支付方式（模拟）</span>
          <div class="methods">
            <label :class="['method', { on: method === 'alipay' }]">
              <input type="radio" v-model="method" value="alipay" /> 支付宝
            </label>
            <label :class="['method', { on: method === 'wechat' }]">
              <input type="radio" v-model="method" value="wechat" /> 微信支付
            </label>
            <label :class="['method', { on: method === 'card' }]">
              <input type="radio" v-model="method" value="card" /> 校园卡
            </label>
          </div>
        </div>

        <p v-if="error" class="err">{{ error }}</p>

        <button class="btn-pay" :disabled="paying" @click="doPay">
          {{ paying ? '支付处理中...' : `确认支付 ¥${amount.toFixed(2)}` }}
        </button>
        <button class="btn-cancel" @click="$router.back()">取消</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.pay-page { max-width: 480px; margin: 0 auto; padding-top: 40px; }
.pay-page h2 { margin: 0 0 20px; font-size: 22px; }
.pay-card { background: #fff; border: 1px solid #e8e8e8; border-radius: 12px; padding: 28px; }
.order-summary { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.order-summary .label { color: #888; font-size: 14px; }
.order-summary strong { font-size: 16px; }
.money { color: #ff4d4f; font-size: 24px !important; }
.method-section { margin: 20px 0; }
.method-section .label { display: block; margin-bottom: 10px; color: #888; font-size: 14px; }
.methods { display: flex; gap: 10px; }
.method { flex: 1; padding: 12px; border: 2px solid #e8e8e8; border-radius: 8px; text-align: center; cursor: pointer; font-size: 14px; }
.method.on { border-color: #1677ff; background: #e6f7ff; }
.method input { display: none; }
.btn-pay { display: block; width: 100%; padding: 14px; background: #ff4d4f; color: #fff; border: none; border-radius: 10px; font-size: 17px; font-weight: 600; cursor: pointer; margin-top: 20px; }
.btn-pay:disabled { opacity: 0.5; cursor: default; }
.btn-cancel { display: block; width: 100%; padding: 10px; margin-top: 10px; border: 1px solid #d9d9d9; border-radius: 8px; background: #fff; cursor: pointer; font-size: 14px; color: #666; }
.err { color: #ff4d4f; font-size: 13px; margin: 12px 0 0; }

.pay-success { text-align: center; padding: 64px 24px; }
.check-icon { width: 72px; height: 72px; border-radius: 50%; background: #52c41a; color: #fff; font-size: 36px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; }
.pay-success h2 { font-size: 24px; margin-bottom: 8px; }
.pay-success p { color: #888; margin: 0 0 28px; }
.success-actions { display: flex; gap: 12px; justify-content: center; }
.success-actions button { padding: 10px 28px; background: #1677ff; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 15px; }
.success-actions button.ghost { background: #fff; color: #666; border: 1px solid #d9d9d9; }
.pay-card strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 260px; }
</style>
