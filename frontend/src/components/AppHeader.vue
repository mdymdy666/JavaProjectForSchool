<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { useNotificationStore } from '../stores/notification'
import UiIcon from './UiIcon.vue'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const notify = useNotificationStore()

onMounted(() => { if (auth.isLoggedIn) notify.refresh() })

function go(path: string) {
  router.push(path)
}
</script>

<template>
  <header class="app-header">
    <button class="logo" type="button" @click="go('/')">
      <span class="logo-text">校园二手交易</span>
      <small>同校交易 · 安全放心</small>
    </button>

    <nav class="nav" aria-label="主导航">
      <button @click="go('/')">首页</button>
      <button @click="go('/products')">市场</button>
      <button v-if="auth.isLoggedIn" @click="go('/publish')">发布</button>
      <button v-if="auth.isLoggedIn" @click="go('/orders')">订单</button>
      <button class="msg-nav" @click="auth.isLoggedIn ? go('/messages') : go('/login')">
        消息
        <i v-if="auth.isLoggedIn && notify.unreadCount" class="badge">{{ notify.unreadCount }}</i>
      </button>
      <button v-if="auth.isAdmin" @click="go('/admin')">后台</button>
    </nav>

    <div class="right">
      <button class="cart" aria-label="购物车" @click="go('/cart')">
        <UiIcon name="cart" />
        <span>购物车</span>
        <i v-if="cart.totalCount" class="badge">{{ cart.totalCount }}</i>
      </button>
      <template v-if="auth.isLoggedIn">
        <button class="user" @click="go('/profile')">{{ auth.nickname }}</button>
        <button class="ghost" @click="auth.logout(); go('/')">退出</button>
      </template>
      <template v-else>
        <button @click="go('/login')">登录</button>
        <button class="ghost" @click="go('/register')">注册</button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: 22px;
  min-width: 0;
  height: 68px;
  padding: 0 38px;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid rgba(219, 228, 238, 0.9);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(14px);
}
.logo {
  display: grid;
  gap: 2px;
  min-width: 186px;
  padding: 0;
  border: 0;
  color: var(--ink);
  background: transparent;
  cursor: pointer;
  text-align: left;
}
.logo-text {
  color: var(--brand-blue);
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1;
}
.logo small {
  color: #8a99aa;
  font-size: 11px;
  font-weight: 700;
}
.nav {
  display: flex;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.nav button {
  position: relative;
  flex-shrink: 0;
  min-width: 54px;
  height: 38px;
  padding: 0 13px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #233044;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}
.nav button:hover {
  color: var(--brand-blue);
  background: var(--soft-blue);
}
.nav button.msg-nav { position: relative; }
.right {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}
.right button {
  height: 36px;
  padding: 0 13px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: #233044;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
}
.right button:hover {
  border-color: var(--brand-blue);
  color: var(--brand-blue);
}
.right button.ghost {
  color: #64748b;
  border-color: transparent;
  background: transparent;
}
.right button.user {
  color: var(--brand-blue);
  border-color: #b9d7ff;
  background: #f7fbff;
}
.right button.cart {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding-right: 14px;
}
.badge {
  position: absolute;
  top: -7px;
  right: -7px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border: 2px solid #fff;
  border-radius: 999px;
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  font-style: normal;
  line-height: 14px;
  text-align: center;
}
@media (max-width: 820px) {
  .app-header {
    gap: 10px;
    height: auto;
    padding: 12px 14px;
    flex-wrap: wrap;
  }
  .logo { min-width: 150px; }
  .nav { order: 3; width: 100%; overflow-x: auto; }
  .right { margin-left: auto; }
}
</style>
