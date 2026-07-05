<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, favoriteProduct } from '../api/product'
import { useAuthStore } from '../stores/auth'
import type { ProductDetail } from '../types/domain'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const product = ref<ProductDetail | null>(null)
const loading = ref(true)

async function fetch() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getProductDetail(id)
    product.value = res.data || null
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) { router.push('/login'); return }
  if (!product.value) return
  try {
    const res = await favoriteProduct(product.value.id)
    if (product.value && res.data) {
      product.value = { ...product.value, favorite: res.data.favorite }
    }
  } catch { /* ignore */ }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待审核', APPROVED: '在售', REJECTED: '已驳回',
    OFF_SHELF: '已下架', SOLD: '已售出', DELETED: '已删除'
  }
  return map[status] || status
}

function buy() {
  if (!auth.isLoggedIn) { router.push('/login'); return }
  router.push(`/orders?action=buy&productId=${product.value?.id}`)
}

onMounted(fetch)
</script>

<template>
  <div class="detail-page">
    <div v-if="loading" class="loading">加载中...</div>
    <template v-else-if="product">
      <div class="detail-layout">
        <div class="detail-gallery">
          <img v-if="product.images.length" :src="product.images[0]" :alt="product.title" class="main-image" />
          <div v-else class="img-placeholder">暂无图片</div>
        </div>
        <div class="detail-info">
          <span class="status-tag">{{ statusLabel(product.status) }}</span>
          <h1>{{ product.title }}</h1>
          <p class="price">&yen;{{ product.price.toFixed(2) }}</p>
          <div class="meta-row">
            <span>分类：{{ product.categoryName }}</span>
            <span>成色：{{ product.itemCondition }}</span>
            <span>浏览：{{ product.viewCount }}</span>
          </div>
          <p class="seller">卖家：{{ product.sellerNickname }}</p>
          <p class="desc">{{ product.description }}</p>
          <div class="actions">
            <button class="btn-fav" :class="{ active: product.favorite }" @click="toggleFavorite">
              {{ product.favorite ? '★ 已收藏' : '☆ 收藏' }}
            </button>
            <button
              v-if="product.status === 'APPROVED'"
              class="btn-buy" @click="buy"
            >立即购买</button>
          </div>
        </div>
      </div>
    </template>
    <p v-else class="empty-hint">商品不存在</p>
  </div>
</template>

<style scoped>
.detail-page { max-width: 960px; margin: 0 auto; }
.loading { text-align: center; padding: 48px; color: #999; }
.empty-hint { color: #999; text-align: center; padding: 48px 0; }
.detail-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }
@media (max-width: 768px) { .detail-layout { grid-template-columns: 1fr; } }
.detail-gallery { border-radius: 8px; overflow: hidden; }
.main-image { width: 100%; height: 360px; object-fit: cover; display: block; }
.img-placeholder {
  width: 100%; height: 360px; background: #f5f5f5;
  display: flex; align-items: center; justify-content: center; color: #999;
}
.status-tag {
  display: inline-block; padding: 3px 10px; border-radius: 4px;
  background: #e6f7ff; color: #1677ff; font-size: 13px; margin-bottom: 8px;
}
.detail-info h1 { font-size: 24px; margin: 0 0 12px; }
.price { color: #ff4d4f; font-size: 28px; font-weight: 700; margin: 0 0 12px; }
.meta-row { display: flex; gap: 20px; font-size: 14px; color: #666; margin-bottom: 8px; }
.seller { font-size: 14px; color: #333; margin-bottom: 12px; }
.desc { color: #555; line-height: 1.7; margin-bottom: 24px; }
.actions { display: flex; gap: 12px; }
.btn-fav {
  padding: 10px 20px; border: 1px solid #d9d9d9; border-radius: 8px;
  background: #fff; cursor: pointer; font-size: 15px;
}
.btn-fav.active { border-color: #faad14; color: #faad14; }
.btn-buy {
  padding: 10px 32px; background: #ff4d4f; color: #fff;
  border: none; border-radius: 8px; font-size: 16px; cursor: pointer;
}
</style>
