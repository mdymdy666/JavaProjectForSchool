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
    path: '/',
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
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
