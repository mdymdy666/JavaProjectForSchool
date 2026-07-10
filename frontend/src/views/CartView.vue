<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { formatMoney } from '../utils/money'

const router = useRouter()
const cart = useCartStore()

function goPay(productId: number) {
  router.push(`/pay/product/${productId}`)
}

function goBatchPay() {
  router.push('/pay/batch')
}
</script>

<template>
  <div class="cart-page">
    <h2>购物车</h2>

    <div v-if="cart.items.length === 0" class="empty">
      <p>购物车是空的</p>
      <RouterLink to="/products">去逛逛</RouterLink>
    </div>

    <template v-else>
      <div class="cart-list">
        <div v-for="item in cart.items" :key="item.productId" class="cart-item">
          <div class="item-img">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
            <div v-else class="no-img">无图</div>
          </div>
          <div class="item-info">
            <strong class="item-title">{{ item.title }}</strong>
            <p class="item-seller">{{ item.sellerNickname }}</p>
            <p class="item-price">&yen;{{ formatMoney(item.price) }}</p>
          </div>
          <div class="item-qty">
            <button :disabled="item.quantity <= 1" @click="cart.updateQuantity(item.productId, item.quantity - 1)">−</button>
            <span>{{ item.quantity }}</span>
            <button @click="cart.updateQuantity(item.productId, item.quantity + 1)">+</button>
          </div>
          <div class="item-subtotal">&yen;{{ formatMoney(item.price * item.quantity) }}</div>
          <button class="btn-del" @click="cart.remove(item.productId)">删除</button>
          <button class="btn-pay" @click="goPay(item.productId)">结算</button>
        </div>
      </div>

      <div class="cart-footer">
        <span>共 {{ cart.totalCount }} 件，合计 <strong>&yen;{{ formatMoney(cart.totalAmount) }}</strong></span>
        <button class="btn-batch" @click="goBatchPay">全部结算</button>
        <button class="btn-clear" @click="cart.clear()">清空</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.cart-page { max-width: 800px; margin: 0 auto; }
.cart-page h2 { margin: 0 0 20px; }
.empty { text-align: center; padding: 64px 0; color: #999; }
.empty a { color: #1677ff; font-size: 16px; }
.cart-list { display: flex; flex-direction: column; gap: 14px; }
.cart-item {
  display: flex; align-items: center; gap: 14px; padding: 14px 16px;
  background: #fff; border: 1px solid #e8e8e8; border-radius: 10px;
}
.item-img { width: 80px; height: 80px; border-radius: 6px; overflow: hidden; flex-shrink: 0; background: #f5f5f5; }
.item-img img { width: 100%; height: 100%; object-fit: cover; }
.no-img { display: flex; align-items: center; justify-content: center; height: 100%; color: #bbb; font-size: 12px; }
.item-info { flex: 1; min-width: 0; }
.item-title { font-size: 15px; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-seller { margin: 3px 0; font-size: 12px; color: #999; }
.item-price { margin: 0; color: #ff4d4f; font-weight: 600; font-size: 15px; }
.item-qty { display: flex; align-items: center; gap: 6px; }
.item-qty button { width: 28px; height: 28px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer; font-size: 16px; display: flex; align-items: center; justify-content: center; }
.item-qty button:disabled { opacity: 0.3; cursor: default; }
.item-qty span { min-width: 24px; text-align: center; font-weight: 600; }
.item-subtotal { font-weight: 700; font-size: 15px; color: #ff4d4f; min-width: 80px; text-align: right; }
.btn-del { padding: 4px 10px; border: none; background: none; color: #999; cursor: pointer; font-size: 13px; }
.btn-del:hover { color: #ff4d4f; }
.btn-pay { padding: 6px 14px; background: #ff4d4f; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; white-space: nowrap; }
.cart-footer { display: flex; align-items: center; justify-content: flex-end; gap: 14px; margin-top: 20px; padding: 16px; background: #fff; border: 1px solid #e8e8e8; border-radius: 10px; }
.cart-footer span { font-size: 15px; }
.cart-footer strong { color: #ff4d4f; font-size: 20px; }
.btn-batch { padding: 10px 28px; background: #ff4d4f; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: 600; }
.btn-clear { padding: 6px 14px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; font-size: 13px; color: #999; }
</style>
