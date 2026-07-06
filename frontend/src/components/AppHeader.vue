<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

function go(path: string) {
  router.push(path)
}
</script>

<template>
  <header class="app-header">
    <div class="header-left" @click="go('/')">
      <span class="logo-text">校园二手交易</span>
    </div>

    <nav class="header-nav">
      <button @click="go('/')">首页</button>
      <button @click="go('/products')">商品市场</button>
      <button v-if="auth.isLoggedIn" @click="go('/publish')">发布</button>
      <button v-if="auth.isLoggedIn" @click="go('/orders')">订单</button>
      <button v-if="auth.isLoggedIn" @click="go('/messages')">消息</button>
      <button v-if="auth.isAdmin" @click="go('/admin')">后台</button>
    </nav>

    <div class="header-right">
      <button class="cart-btn" @click="go('/cart')">
        购物车
        <span v-if="cart.totalCount" class="cart-badge">{{ cart.totalCount }}</span>
      </button>
      <template v-if="auth.isLoggedIn">
        <button @click="go('/profile')">{{ auth.nickname }}</button>
        <button class="secondary" @click="auth.logout(); go('/')">退出</button>
      </template>
      <template v-else>
        <button @click="go('/login')">登录</button>
        <button class="secondary" @click="go('/register')">注册</button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  cursor: pointer;
  font-weight: 700;
  font-size: 18px;
  color: #1677ff;
  margin-right: 24px;
}
.header-nav {
  display: flex;
  gap: 4px;
  flex: 1;
}
.header-nav button {
  background: none;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  color: #333;
  font-size: 14px;
}
.header-nav button:hover {
  background: #f0f0f0;
}
.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}
.header-right button {
  padding: 5px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}
.header-right button.secondary {
  color: #666;
  border-color: transparent;
}
.cart-btn {
  position: relative;
}
.cart-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  background: #ff4d4f;
  color: #fff;
  border-radius: 9px;
  font-size: 11px;
  text-align: center;
  padding: 0 4px;
}
</style>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  cursor: pointer;
  font-weight: 700;
  font-size: 18px;
  color: #1677ff;
  margin-right: 24px;
}
.header-nav {
  display: flex;
  gap: 4px;
  flex: 1;
}
.header-nav button {
  background: none;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  color: #333;
  font-size: 14px;
}
.header-nav button:hover {
  background: #f0f0f0;
}
.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}
.header-right button {
  padding: 5px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}
.header-right button.secondary {
  color: #666;
  border-color: transparent;
}
</style>
