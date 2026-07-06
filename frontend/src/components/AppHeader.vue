<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { useNotificationStore } from '../stores/notification'

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
    <span class="logo" @click="go('/')">校园二手交易</span>

    <nav class="nav">
      <button @click="go('/')">首页</button>
      <button @click="go('/products')">市场</button>
      <button v-if="auth.isLoggedIn" @click="go('/publish')">发布</button>
      <button v-if="auth.isLoggedIn" @click="go('/orders')">订单</button>
      <button v-if="auth.isLoggedIn" class="msg-nav" @click="go('/messages')">
        消息
        <i v-if="notify.unreadCount" class="badge">{{ notify.unreadCount }}</i>
      </button>
      <button v-if="auth.isAdmin" @click="go('/admin')">后台</button>
    </nav>

    <div class="right">
      <button class="cart" @click="go('/cart')">
        购物车
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
  display: flex; align-items: center; gap: 10px;
  padding: 0 20px; height: 52px; background: #fff;
  border-bottom: 1px solid #e8e8e8; position: sticky; top: 0; z-index: 100;
  min-width: 0;
}
.logo { font-weight: 700; font-size: 17px; color: #1677ff; cursor: pointer; flex-shrink: 0; margin-right: 6px; }
.nav { display: flex; gap: 2px; flex: 1; min-width: 0; overflow: hidden; }
.nav button { flex-shrink: 0; background: none; border: none; padding: 5px 10px; border-radius: 5px; cursor: pointer; color: #333; font-size: 13px; white-space: nowrap; }
.nav button:hover { background: #f0f0f0; }
.nav button.msg-nav { position: relative; }
.right { display: flex; gap: 6px; align-items: center; flex-shrink: 0; }
.right button { padding: 4px 12px; border: 1px solid #d9d9d9; border-radius: 5px; background: #fff; cursor: pointer; font-size: 12px; white-space: nowrap; flex-shrink: 0; }
.right button.ghost { color: #666; border-color: transparent; }
.right button.user { color: #1677ff; border-color: #1677ff; }
.right button.cart { position: relative; padding-right: 14px; }
.badge {
  position: absolute; top: -6px; right: -6px;
  min-width: 17px; height: 17px; line-height: 17px;
  background: #ff4d4f; color: #fff; border-radius: 9px;
  font-size: 10px; text-align: center; padding: 0 4px; font-style: normal;
}
</style>
