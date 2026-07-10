<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteProduct } from '../api/product'
import { useAuthStore } from '../stores/auth'
import UiIcon from './UiIcon.vue'
import type { ProductCard } from '../types/domain'
import { formatMoney } from '../utils/money'

const props = defineProps<{ product: ProductCard }>()

const emit = defineEmits<{ select: [id: number] }>()
const router = useRouter()
const auth = useAuthStore()
const favorited = ref(false)
const favoriteBusy = ref(false)
const favoriteError = ref('')

function selectProduct() {
  emit('select', props.product.id)
}

async function toggleFavorite() {
  if (favoriteBusy.value) return
  if (!auth.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }

  favoriteBusy.value = true
  favoriteError.value = ''
  try {
    const res = await favoriteProduct(props.product.id)
    favorited.value = res.data?.favorite ?? !favorited.value
  } catch (error) {
    const status = (error as { response?: { status?: number } }).response?.status
    if (status === 401) {
      await router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      return
    }
    favoriteError.value = '收藏失败，请稍后再试'
    window.setTimeout(() => { favoriteError.value = '' }, 1800)
  } finally {
    favoriteBusy.value = false
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '在售',
    REJECTED: '已驳回',
    OFF_SHELF: '已下架',
    SOLD: '已售出',
    DELETED: '已删除'
  }
  return map[status] || status
}

function statusTag(status: string) {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED' || status === 'OFF_SHELF') return 'danger'
  return 'info'
}

function shortLocation(id: number) {
  const locations = ['东区宿舍楼', '教学楼A区', '西区宿舍楼', '图书馆门口']
  return locations[id % locations.length]
}
</script>

<template>
  <article class="product-card" data-test="product-card" @click="selectProduct">
    <div class="card-image">
      <img v-if="product.coverUrl" :src="product.coverUrl" :alt="product.title" />
      <div v-else class="img-placeholder">
        <span>{{ product.categoryName.slice(0, 1) }}</span>
        <small>暂无图片</small>
      </div>
      <span :class="['status-tag', statusTag(product.status)]">{{ statusLabel(product.status) }}</span>
      <span class="location-chip">{{ shortLocation(product.id) }}</span>
    </div>

    <div class="card-body">
      <h3>{{ product.title }}</h3>
      <div class="card-meta">
        <span>{{ product.categoryName }}</span>
        <span>{{ product.itemCondition }}</span>
      </div>

      <div class="card-footer">
        <strong class="price">&yen;{{ formatMoney(product.price) }}</strong>
        <button
          :class="['favorite', { active: favorited, loading: favoriteBusy }]"
          type="button"
          :aria-label="favorited ? '取消收藏' : '收藏'"
          :aria-pressed="favorited"
          :disabled="favoriteBusy"
          :title="favoriteError || (favorited ? '取消收藏' : '收藏')"
          @click.stop="toggleFavorite"
        >
          <UiIcon name="heart" :filled="favorited" />
        </button>
      </div>

      <div class="seller-line">
        <span class="seller-avatar">{{ product.sellerNickname.slice(0, 1) }}</span>
        <span class="seller-name">{{ product.sellerNickname }}</span>
        <span class="credit">信用良好</span>
        <small>{{ product.viewCount }} 浏览量</small>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  overflow: hidden;
  border: 1px solid #e1e8f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: #bfd8ff;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.11);
}

.card-image {
  position: relative;
  aspect-ratio: 4 / 3;
  margin: 10px 10px 0;
  overflow: hidden;
  border-radius: 7px;
  background: #eef3f8;
}

.card-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-placeholder {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  height: 100%;
  color: #94a3b8;
  background:
    linear-gradient(135deg, rgba(22, 119, 255, 0.08), rgba(15, 159, 143, 0.08)),
    #f2f6fb;
}

.img-placeholder span {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 16px;
  color: var(--brand-blue);
  background: #fff;
  font-size: 24px;
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.img-placeholder small {
  color: #94a3b8;
  font-weight: 800;
}

.status-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.16);
}

.status-tag.success { background: #22c55e; }
.status-tag.warning { background: #f59e0b; }
.status-tag.danger { background: #ef4444; }
.status-tag.info { background: var(--brand-blue); }

.location-chip {
  position: absolute;
  bottom: 10px;
  left: 10px;
  max-width: calc(100% - 20px);
  overflow: hidden;
  padding: 5px 9px;
  border-radius: 999px;
  color: #fff;
  background: rgba(15, 23, 42, 0.68);
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  backdrop-filter: blur(8px);
}

.card-body {
  display: grid;
  gap: 9px;
  padding: 14px;
}

.card-body h3 {
  display: -webkit-box;
  min-height: 43px;
  margin: 0;
  overflow: hidden;
  color: #17212b;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.36;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-meta {
  display: flex;
  gap: 8px;
  min-width: 0;
  color: #64748b;
  font-size: 13px;
}

.card-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.price {
  color: #f97316;
  font-size: 22px;
  line-height: 1;
}

.favorite {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid #dbe4ee;
  border-radius: 50%;
  color: #94a3b8;
  background: #fff;
  transition: color 0.16s ease, border-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
}

.favorite:hover {
  color: #ef4444;
  border-color: #fecaca;
  background: #fff5f5;
}

.favorite.active {
  color: #ef4444;
  border-color: #fecaca;
  background: #fff1f2;
}

.favorite.loading {
  cursor: progress;
  opacity: 0.72;
}

.favorite:disabled {
  cursor: progress;
}

.seller-line {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: #475569;
  font-size: 12px;
}

.seller-avatar {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  border-radius: 50%;
  color: #fff;
  background: #17212b;
  font-size: 12px;
  font-weight: 900;
}

.seller-name {
  overflow: hidden;
  max-width: 66px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credit {
  padding: 2px 7px;
  border-radius: 999px;
  color: #16835f;
  background: #dcfce7;
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.seller-line small {
  margin-left: auto;
  color: #94a3b8;
  white-space: nowrap;
}
</style>
