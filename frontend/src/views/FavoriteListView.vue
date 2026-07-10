<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteProduct } from '../api/product'
import { getMyFavorites } from '../api/user'
import StatusTag from '../components/StatusTag.vue'
import type { ProductCard } from '../types/domain'
import { formatMoney } from '../utils/money'

const router = useRouter()
const favorites = ref<ProductCard[]>([])
const loading = ref(false)
const error = ref('')
const actingId = ref<number | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await getMyFavorites()
    favorites.value = res.data || []
  } catch (e: any) {
    error.value = e?.response?.data?.message || '收藏列表加载失败'
  } finally {
    loading.value = false
  }
}

async function removeFavorite(id: number) {
  actingId.value = id
  error.value = ''
  try {
    await favoriteProduct(id)
    favorites.value = favorites.value.filter(item => item.id !== id)
  } catch (e: any) {
    error.value = e?.response?.data?.message || '取消收藏失败'
  } finally {
    actingId.value = null
  }
}

function formatDate(value: string) {
  return value ? value.slice(0, 10) : ''
}

onMounted(load)
</script>

<template>
  <main class="favorite-page">
    <header class="page-head">
      <div>
        <p>我的收藏</p>
        <h1>关注的校园闲置</h1>
        <span>已下架或已售出的商品会保留状态，方便你判断是否继续关注。</span>
      </div>
      <button type="button" @click="router.push('/products')">继续逛市场</button>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="favorite-panel">
      <div v-if="loading" class="state">加载中...</div>
      <div v-else-if="favorites.length" class="favorite-list">
        <article v-for="item in favorites" :key="item.id" class="favorite-row">
          <button class="cover" type="button" @click="router.push(`/products/${item.id}`)">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
            <span v-else>暂无图片</span>
          </button>
          <div class="favorite-main">
            <div class="title-line">
              <button type="button" @click="router.push(`/products/${item.id}`)">{{ item.title }}</button>
              <StatusTag :status="item.status" />
            </div>
            <p>{{ item.categoryName }} · {{ item.itemCondition }} · {{ item.sellerNickname }}</p>
            <div class="meta-line">
              <strong>&yen;{{ formatMoney(item.price) }}</strong>
              <span>{{ item.viewCount }} 浏览</span>
              <span>发布于 {{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
          <div class="actions">
            <button type="button" @click="router.push(`/products/${item.id}`)">查看详情</button>
            <button type="button" class="danger" :disabled="actingId === item.id" @click="removeFavorite(item.id)">
              {{ actingId === item.id ? '处理中...' : '取消收藏' }}
            </button>
          </div>
        </article>
      </div>
      <div v-else class="state">
        <strong>还没有收藏商品</strong>
        <span>在商品卡片或详情页点击收藏后，会出现在这里。</span>
        <button type="button" @click="router.push('/products')">去收藏商品</button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.favorite-page {
  display: grid;
  gap: 16px;
  max-width: 1080px;
  margin: 0 auto;
}
.page-head,
.favorite-panel {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px;
}
.page-head p {
  margin: 0 0 6px;
  color: var(--brand-blue);
  font-weight: 900;
}
.page-head h1 {
  margin: 0;
  color: #17212b;
  font-size: 28px;
}
.page-head span {
  display: block;
  margin-top: 8px;
  color: #64748b;
}
.page-head button,
.actions button,
.state button {
  height: 38px;
  border: 1px solid var(--brand-blue);
  border-radius: 8px;
  color: var(--brand-blue);
  background: #fff;
  cursor: pointer;
  font-weight: 900;
}
.page-head button {
  padding: 0 16px;
  color: #fff;
  background: var(--brand-blue);
  white-space: nowrap;
}
.favorite-panel {
  overflow: hidden;
}
.favorite-list {
  display: grid;
}
.favorite-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eef2f7;
}
.favorite-row:last-child {
  border-bottom: 0;
}
.cover {
  display: grid;
  place-items: center;
  width: 120px;
  height: 90px;
  overflow: hidden;
  border: 0;
  border-radius: 8px;
  color: #94a3b8;
  background: #f1f5f9;
  cursor: pointer;
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.favorite-main {
  display: grid;
  gap: 8px;
  min-width: 0;
}
.title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title-line button {
  overflow: hidden;
  padding: 0;
  border: 0;
  color: #17212b;
  background: transparent;
  cursor: pointer;
  font-size: 17px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.favorite-main p,
.meta-line span {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}
.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.meta-line strong {
  color: #f97316;
  font-size: 20px;
}
.actions {
  display: flex;
  gap: 8px;
}
.actions button {
  padding: 0 12px;
}
.actions .danger {
  border-color: #fecaca;
  color: #dc2626;
}
.actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 260px;
  padding: 24px;
  color: #64748b;
  text-align: center;
}
.state strong {
  color: #17212b;
  font-size: 18px;
}
.state button {
  margin-top: 8px;
  padding: 0 16px;
}
.error {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #b91c1c;
  background: #fef2f2;
}
@media (max-width: 760px) {
  .page-head,
  .favorite-row {
    grid-template-columns: 1fr;
  }
  .page-head,
  .actions,
  .title-line {
    align-items: flex-start;
    flex-direction: column;
  }
  .favorite-row {
    display: grid;
  }
  .cover {
    width: 100%;
    height: auto;
    aspect-ratio: 4 / 3;
  }
}
</style>
