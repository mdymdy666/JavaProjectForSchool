<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, favoriteProduct, offShelfProduct, relistProduct, deleteProduct } from '../api/product'
import { useAuthStore } from '../stores/auth'
import type { ProductDetail } from '../types/domain'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const product = ref<ProductDetail | null>(null)
const loading = ref(true)
const currentImage = ref(0)
const acting = ref(false)
const error = ref('')

const isOwner = computed(() =>
  auth.isLoggedIn && product.value != null && auth.userId === product.value.sellerId
)
const canBuy = computed(() =>
  product.value != null && product.value.status === 'APPROVED' && !isOwner.value
)

async function fetch() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getProductDetail(id)
    product.value = res.data || null
  } finally { loading.value = false }
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) { router.push({ name: 'login', query: { redirect: route.fullPath } }); return }
  if (!product.value) return
  acting.value = true
  try {
    const res = await favoriteProduct(product.value.id)
    if (res.code === 200 && res.data) {
      product.value = { ...product.value, favorite: res.data.favorite }
    }
  } catch { error.value = '收藏操作失败' }
  finally { acting.value = false }
}

async function handleOffShelf() {
  if (!product.value || !confirm('确定要下架该商品吗？')) return
  acting.value = true
  try {
    const res = await offShelfProduct(product.value.id)
    if (res.code === 200 && res.data) product.value = res.data
  } catch { error.value = '操作失败' }
  finally { acting.value = false }
}

async function handleRelist() {
  if (!product.value || !confirm('确定要重新上架该商品吗？')) return
  acting.value = true
  try {
    const res = await relistProduct(product.value.id)
    if (res.code === 200 && res.data) product.value = res.data
  } catch { error.value = '操作失败' }
  finally { acting.value = false }
}

async function handleDelete() {
  if (!product.value || !confirm('确定要删除该商品吗？此操作不可恢复。')) return
  acting.value = true
  try {
    const res = await deleteProduct(product.value.id)
    if (res.code === 200) router.push('/')
  } catch { error.value = '操作失败' }
  finally { acting.value = false }
}

function buy() {
  if (!auth.isLoggedIn) { router.push('/login'); return }
  router.push(`/orders?action=buy&productId=${product.value?.id}`)
}

function contactSeller() {
  if (!auth.isLoggedIn) { router.push('/login'); return }
  router.push('/messages')
}

function statusLabel(s: string) {
  const m: Record<string, string> = { PENDING: '待审核', APPROVED: '在售', REJECTED: '已驳回', OFF_SHELF: '已下架', SOLD: '已售出', DELETED: '已删除' }
  return m[s] || s
}

function statusClass(s: string) {
  if (s === 'APPROVED') return 's-ok'
  if (s === 'SOLD' || s === 'REJECTED' || s === 'DELETED') return 's-bad'
  return 's-warn'
}

function formatDate(d: string) {
  return d ? d.slice(0, 10) + ' ' + d.slice(11, 16) : ''
}

function sellerInitial(name: string) {
  return name ? name.charAt(0) : '?'
}

onMounted(fetch)
</script>

<template>
  <div class="detail-page">
    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="product">
      <!-- 面包屑 -->
      <div class="breadcrumb">
        <RouterLink to="/">首页</RouterLink> &gt;
        <RouterLink to="/products">商品市场</RouterLink> &gt;
        <span>{{ product.title }}</span>
      </div>

      <div class="detail-main">
        <!-- 左：图片区 -->
        <div class="gallery-col">
          <div v-if="product.images.length" class="gallery">
            <div class="thumb-col">
              <div
                v-for="(img, i) in product.images" :key="i"
                :class="['thumb', { active: i === currentImage }]"
                @click="currentImage = i"
              >
                <img :src="img" :alt="`图${i + 1}`" />
              </div>
            </div>
            <div class="main-img-wrap">
              <img :src="product.images[currentImage]" :alt="product.title" class="main-img" />
              <div v-if="product.images.length > 1" class="img-counter">{{ currentImage + 1 }} / {{ product.images.length }}</div>
            </div>
          </div>
          <div v-else class="no-img">暂无图片</div>
        </div>

        <!-- 右：信息区 -->
        <div class="info-col">
          <span :class="['status-badge', statusClass(product.status)]">{{ statusLabel(product.status) }}</span>
          <h1>{{ product.title }}</h1>
          <p class="price"><span>&yen;</span>{{ product.price.toFixed(2) }}</p>

          <div class="meta-grid">
            <div class="meta-item"><label>分类</label><span>{{ product.categoryName }}</span></div>
            <div class="meta-item"><label>成色</label><span>{{ product.itemCondition }}</span></div>
            <div class="meta-item"><label>浏览</label><span>{{ product.viewCount }} 次</span></div>
            <div class="meta-item"><label>发布</label><span>{{ formatDate(product.createdAt) }}</span></div>
          </div>

          <!-- 卖家操作 -->
          <div v-if="isOwner" class="owner-bar">
            <button v-if="product.status === 'APPROVED'" class="btn-warn" :disabled="acting" @click="handleOffShelf">下架</button>
            <button v-if="product.status === 'OFF_SHELF'" class="btn-ok" :disabled="acting" @click="handleRelist">重新上架</button>
            <button v-if="product.status !== 'SOLD' && product.status !== 'DELETED'" class="btn-del" :disabled="acting" @click="handleDelete">删除</button>
          </div>

          <!-- 购买操作 -->
          <div v-if="!isOwner" class="buy-bar">
            <button class="btn-fav" :class="{ on: product.favorite }" :disabled="acting" @click="toggleFavorite">
              {{ acting ? '...' : (product.favorite ? '★ 已收藏' : '☆ 收藏') }}
            </button>
            <button v-if="canBuy" class="btn-buy" @click="buy">立即购买</button>
          </div>
          <p v-if="error" class="err">{{ error }}</p>
        </div>
      </div>

      <!-- 描述 -->
      <div class="desc-section">
        <h3>商品描述</h3>
        <p>{{ product.description }}</p>
      </div>

      <!-- 卖家卡片 -->
      <div class="seller-card">
        <div class="seller-avatar">{{ sellerInitial(product.sellerNickname) }}</div>
        <div class="seller-info">
          <strong>{{ product.sellerNickname }}</strong>
          <span v-if="product.sellerId === 1" class="role-tag admin">管理员</span>
          <p>在售 {{ product.sellerProductCount || 0 }} 件商品</p>
        </div>
        <button v-if="!isOwner" class="btn-contact" @click="contactSeller">联系卖家</button>
      </div>
    </template>

    <p v-else class="empty">商品不存在</p>
  </div>
</template>

<style scoped>
.detail-page { max-width: 960px; margin: 0 auto; }
.loading, .empty { text-align: center; padding: 64px 0; color: #999; font-size: 15px; }

/* 面包屑 */
.breadcrumb { font-size: 13px; color: #999; margin-bottom: 20px; }
.breadcrumb a { color: #666; text-decoration: none; }
.breadcrumb a:hover { color: #1677ff; }
.breadcrumb span { color: #333; }

/* 主区域 */
.detail-main { display: grid; grid-template-columns: 480px 1fr; gap: 32px; margin-bottom: 32px; }
@media (max-width: 860px) { .detail-main { grid-template-columns: 1fr; } }

/* 图片区 */
.gallery-col { min-width: 0; }
.gallery { display: flex; gap: 12px; }
.thumb-col { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }
.thumb { width: 64px; height: 64px; border-radius: 6px; overflow: hidden; cursor: pointer; border: 2px solid transparent; opacity: 0.6; transition: all 0.15s; }
.thumb:hover { opacity: 0.9; }
.thumb.active { border-color: #1677ff; opacity: 1; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.main-img-wrap { flex: 1; position: relative; border-radius: 10px; overflow: hidden; background: #f5f5f5; min-height: 360px; }
.main-img { width: 100%; height: 400px; object-fit: cover; display: block; }
.img-counter { position: absolute; bottom: 10px; right: 10px; background: rgba(0,0,0,0.55); color: #fff; padding: 3px 10px; border-radius: 12px; font-size: 12px; }
.no-img { width: 100%; height: 400px; background: #f5f5f5; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #999; font-size: 15px; }

/* 信息区 */
.info-col { display: flex; flex-direction: column; gap: 14px; }
.status-badge { display: inline-block; align-self: flex-start; padding: 4px 14px; border-radius: 4px; font-size: 13px; font-weight: 600; }
.status-badge.s-ok { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.status-badge.s-bad { background: #fff2f0; color: #cf1322; border: 1px solid #ffccc7; }
.status-badge.s-warn { background: #fffbe6; color: #d48806; border: 1px solid #ffe58f; }

.info-col h1 { font-size: 22px; font-weight: 700; margin: 0; line-height: 1.4; }
.price { margin: 0; color: #ff4d4f; font-size: 32px; font-weight: 700; }
.price span { font-size: 18px; margin-right: 2px; }

.meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; background: #fafafa; border-radius: 8px; padding: 14px 16px; }
.meta-item label { display: block; font-size: 12px; color: #999; margin-bottom: 2px; }
.meta-item span { font-size: 14px; color: #333; }

/* 卖家操作栏 */
.owner-bar { display: flex; gap: 8px; flex-wrap: wrap; }
.owner-bar button { padding: 8px 18px; border-radius: 6px; border: none; cursor: pointer; font-size: 14px; color: #fff; }
.owner-bar button:disabled { opacity: 0.5; cursor: default; }
.btn-warn { background: #faad14; }
.btn-ok { background: #52c41a; }
.btn-del { background: #ff4d4f; }

/* 购买栏 */
.buy-bar { display: flex; gap: 10px; align-items: center; }
.btn-fav { padding: 8px 20px; border: 1px solid #d9d9d9; border-radius: 8px; background: #fff; cursor: pointer; font-size: 15px; }
.btn-fav.on { border-color: #faad14; color: #faad14; }
.btn-buy { padding: 10px 40px; background: #ff4d4f; color: #fff; border: none; border-radius: 8px; font-size: 17px; font-weight: 600; cursor: pointer; }
.btn-buy:hover { background: #e04343; }
.err { color: #ff4d4f; font-size: 13px; margin: 0; }

/* 描述 */
.desc-section { margin-bottom: 28px; }
.desc-section h3 { font-size: 17px; margin: 0 0 10px; }
.desc-section p { color: #555; line-height: 1.8; font-size: 14px; margin: 0; }

/* 卖家卡片 */
.seller-card { display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid #e8e8e8; border-radius: 10px; padding: 18px 20px; }
.seller-avatar { width: 48px; height: 48px; border-radius: 50%; background: #1677ff; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; flex-shrink: 0; }
.seller-info { flex: 1; }
.seller-info strong { font-size: 16px; }
.seller-info p { margin: 4px 0 0; font-size: 13px; color: #888; }
.role-tag { display: inline-block; margin-left: 6px; padding: 1px 8px; border-radius: 3px; font-size: 11px; color: #fff; vertical-align: middle; }
.role-tag.admin { background: #1677ff; }
.btn-contact { padding: 8px 20px; background: #fff; border: 1px solid #1677ff; border-radius: 8px; color: #1677ff; cursor: pointer; font-size: 14px; white-space: nowrap; }
.btn-contact:hover { background: #e6f7ff; }
</style>
