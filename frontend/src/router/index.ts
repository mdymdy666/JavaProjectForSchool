import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('../views/ForgotPasswordView.vue')
  },
  {
    path: '/',
    name: 'cover',
    component: () => import('../views/CoverView.vue'),
    meta: { cover: true }
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('../views/HomeView.vue')
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('../views/ProductListView.vue')
  },
  {
    path: '/products/:id',
    name: 'product-detail',
    component: () => import('../views/ProductDetailView.vue')
  },
  {
    path: '/safety',
    name: 'safety',
    component: () => import('../views/SafetyNoticeView.vue')
  },
  {
    path: '/announcements',
    name: 'announcements',
    component: () => import('../views/AnnouncementListView.vue')
  },
  {
    path: '/publish',
    name: 'publish',
    component: () => import('../views/PublishProductView.vue'),
    meta: { auth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { auth: true }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/OrderCenterView.vue'),
    meta: { auth: true }
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('../views/MessageCenterView.vue'),
    meta: { auth: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminDashboardView.vue'),
    meta: { admin: true }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('../views/CartView.vue')
  },
  {
    path: '/pay/:orderId',
    name: 'pay',
    component: () => import('../views/PaymentView.vue'),
    meta: { auth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('campus-token')
  const role = localStorage.getItem('campus-role')
  if (to.meta.admin && (!token || role !== 'ADMIN')) {
    return token ? { name: 'home' } : { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.auth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
