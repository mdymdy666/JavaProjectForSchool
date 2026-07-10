<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { useNotificationStore } from '../stores/notification'
import UiIcon from './UiIcon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const notify = useNotificationStore()

const navItems = computed(() => [
  { label: '首页', path: '/home', show: true },
  { label: '市场', path: '/products', show: true },
  { label: '发布', path: '/publish', show: auth.isLoggedIn },
  { label: '收藏', path: '/favorites', show: auth.isLoggedIn },
  { label: '订单', path: '/orders', show: auth.isLoggedIn },
  { label: '消息', path: '/messages', show: true, badge: auth.isLoggedIn ? notify.unreadCount : 0 },
  { label: '后台', path: '/admin', show: auth.isAdmin }
])

onMounted(() => {
  if (auth.isLoggedIn) notify.refresh()
})

function go(path: string) {
  if (path === '/messages' && !auth.isLoggedIn) {
    router.push('/login')
    return
  }
  router.push(path)
}

function isActive(path: string) {
  if (path === '/home') return route.path === '/home'
  return route.path.startsWith(path)
}
</script>

<template>
  <header class="app-header">
    <button class="logo" type="button" @click="go('/home')">
      <span class="logo-mark">校</span>
      <span class="logo-copy">
        <span class="logo-text">校园二手交易</span>
        <small>同校交易 · 安全放心</small>
      </span>
    </button>

    <nav class="nav" aria-label="主导航">
      <button
        v-for="item in navItems.filter(item => item.show)"
        :key="item.path"
        :class="{ active: isActive(item.path) }"
        @click="go(item.path)"
      >
        {{ item.label }}
        <i v-if="item.badge" class="badge">{{ item.badge }}</i>
      </button>
    </nav>

    <div class="right">
      <button class="publish-shortcut" type="button" @click="auth.isLoggedIn ? go('/publish') : go('/login')">
        <span>+</span>
        发布闲置
      </button>
      <button class="cart" aria-label="购物车" type="button" @click="go('/cart')">
        <UiIcon name="cart" />
        <span>购物车</span>
        <i v-if="cart.totalCount" class="badge">{{ cart.totalCount }}</i>
      </button>
      <template v-if="auth.isLoggedIn">
        <button class="user" type="button" @click="go('/profile')">
          <span class="avatar">{{ auth.nickname.slice(0, 1) }}</span>
          {{ auth.nickname }}
        </button>
        <button class="ghost logout" type="button" @click="auth.logout(); go('/home')">退出</button>
      </template>
      <template v-else>
        <button type="button" @click="go('/login')">登录</button>
        <button class="ghost" type="button" @click="go('/register')">注册</button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 12px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 28px;
  width: min(1440px, calc(100% - 48px));
  height: 64px;
  margin: 12px auto 0;
  padding: 0 22px;
  border: 1px solid rgba(219, 228, 238, 0.9);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 210px;
  padding: 0;
  border: 0;
  color: var(--ink);
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #1677ff, #3aa0ff);
  font-size: 18px;
  font-weight: 900;
  box-shadow: 0 10px 20px rgba(22, 119, 255, 0.22);
}

.logo-copy {
  display: grid;
  gap: 2px;
}

.logo-text {
  color: var(--brand-blue);
  font-size: 20px;
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
  align-self: stretch;
  gap: 8px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.nav button {
  position: relative;
  min-width: 64px;
  padding: 0 14px;
  border: 0;
  border-radius: 0;
  color: #334155;
  background: transparent;
  cursor: pointer;
  font-size: 15px;
  font-weight: 800;
  white-space: nowrap;
}

.nav button::after {
  position: absolute;
  right: 12px;
  bottom: 0;
  left: 12px;
  height: 3px;
  border-radius: 999px 999px 0 0;
  background: transparent;
  content: "";
}

.nav button:hover {
  color: var(--brand-blue);
}

.nav button.active {
  color: var(--brand-blue);
}

.nav button.active::after {
  background: var(--brand-blue);
}

.right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-shrink: 0;
}

.right button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 38px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: #233044;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.right button:hover {
  border-color: var(--brand-blue);
  color: var(--brand-blue);
}

.right button.publish-shortcut {
  border-color: var(--brand-blue);
  color: #fff;
  background: var(--brand-blue);
  box-shadow: 0 10px 22px rgba(22, 119, 255, 0.2);
}

.publish-shortcut span {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  color: var(--brand-blue);
  background: #fff;
  font-weight: 900;
}

.right button.ghost {
  color: #64748b;
  border-color: transparent;
  background: transparent;
}

.right button.logout {
  color: #dc2626;
  border-color: #fecaca;
  background: #fff5f5;
}

.right button.logout:hover {
  color: #b91c1c;
  border-color: #fca5a5;
  background: #fee2e2;
}

.right button.user {
  padding-left: 8px;
  color: #17212b;
  border-color: transparent;
  background: #f8fbff;
}

.avatar {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: #fff;
  background: #17212b;
  font-size: 12px;
  font-weight: 900;
}

.right button.cart {
  border-width: 2px;
  border-color: #bfdbfe;
  color: #1e3a8a;
  background: #f8fbff;
}

.badge {
  position: absolute;
  top: -8px;
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

@media (max-width: 980px) {
  .app-header {
    top: 0;
    width: 100%;
    height: auto;
    margin: 0;
    padding: 12px 14px;
    border-radius: 0;
    flex-wrap: wrap;
  }

  .logo {
    min-width: 170px;
  }

  .nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }

  .right {
    margin-left: auto;
  }

  .publish-shortcut {
    display: none !important;
  }
}

@media (max-width: 640px) {
  .right .cart span,
  .right .ghost {
    display: none;
  }
}
</style>
