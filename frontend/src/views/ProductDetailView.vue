<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, favoriteProduct, offShelfProduct, relistProduct, deleteProduct, reportProduct } from '../api/product'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import type { ProductDetail } from '../types/domain'
import UiIcon from '../components/UiIcon.vue'
import ProductChatDialog from '../components/chat/ProductChatDialog.vue'
import { formatMoney } from '../utils/money'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const product = ref<ProductDetail | null>(null)
const loading = ref(true)
const currentImage = ref(0)
const acting = ref(false)
const error = ref('')
const showChat = ref(false)
const showReport = ref(false)
const reportReason = ref('')
const reportMessage = ref('')
const reportBusy = ref(false)

const isOwner = computed(() =>
  auth.isLoggedIn && product.value != null && auth.userId === product.value.sellerId
)
const canBuy = computed(() =>
  product.value != null && product.value.status === 'APPROVED' && !isOwner.value && auth.role !== 'ADMIN'
)
const isInCart = computed(() =>
  product.value != null && cart.has(product.value.id)
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
  router.push(`/pay/product/${product.value?.id}`)
}

function toggleCart() {
  if (!product.value || !canBuy.value) return
  if (cart.has(product.value.id)) {
    cart.remove(product.value.id)
    return
  }
  cart.add({
    productId: product.value.id,
    title: product.value.title,
    price: product.value.price,
    coverUrl: product.value.images[0] || null,
    sellerNickname: product.value.sellerNickname
  })
}

function contactSeller() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  showChat.value = true
}

function openReport() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  reportMessage.value = ''
  reportReason.value = ''
  showReport.value = true
}

async function submitReport() {
  if (!product.value || reportBusy.value) return
  if (!reportReason.value.trim()) {
    reportMessage.value = '请填写举报原因'
    return
  }
  reportBusy.value = true
  reportMessage.value = ''
  try {
    await reportProduct(product.value.id, reportReason.value)
    reportMessage.value = '举报已提交，管理员会尽快处理'
    window.setTimeout(() => { showReport.value = false }, 900)
  } catch (e: any) {
    reportMessage.value = e?.response?.data?.message || '提交失败，请稍后再试'
  } finally {
    reportBusy.value = false
  }
}

function openMessageCenter() {
  if (!product.value) return
  showChat.value = false
  router.push({
    path: '/messages',
    query: { counterpartId: product.value.sellerId, productId: product.value.id }
  })
}

function returnToSourcePage() {
  const previous = window.history.state?.back
  if (typeof previous === 'string' && previous && previous !== route.fullPath) {
    router.back()
    return
  }
  router.push('/products')
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
        <button type="button" class="breadcrumb-link" @click="returnToSourcePage">商品市场</button> &gt;
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
          <p class="price"><span>&yen;</span>{{ formatMoney(product.price) }}</p>
          <p v-if="product.status === 'REJECTED' && product.auditReason" class="audit-reason">
            驳回原因：{{ product.auditReason }}
          </p>

          <div class="meta-grid">
            <div class="meta-item"><label>分类</label><span>{{ product.categoryName }}</span></div>
            <div class="meta-item"><label>成色</label><span>{{ product.itemCondition }}</span></div>
            <div class="meta-item"><label>浏览量</label><span>{{ product.viewCount }} 次</span></div>
            <div class="meta-item"><label>发布时间</label><span>{{ formatDate(product.createdAt) }}</span></div>
          </div>

          <!-- 卖家操作 -->
          <div v-if="isOwner" class="owner-bar">
            <button v-if="product.status !== 'SOLD' && product.status !== 'DELETED'" class="btn-edit" :disabled="acting" @click="router.push(`/products/${product.id}/edit`)">编辑商品</button>
            <button v-if="product.status === 'APPROVED'" class="btn-warn" :disabled="acting" @click="handleOffShelf">下架</button>
            <button v-if="product.status === 'OFF_SHELF'" class="btn-ok" :disabled="acting" @click="handleRelist">重新上架</button>
            <button v-if="product.status !== 'SOLD' && product.status !== 'DELETED'" class="btn-del" :disabled="acting" @click="handleDelete">删除</button>
          </div>

          <!-- 操作按钮 -->
          <div class="action-bar">
            <button
              class="btn-fav"
              :class="{ on: product.favorite }"
              :disabled="acting || isOwner"
              :aria-label="isOwner ? '自己的商品' : (product.favorite ? '取消收藏商品' : '收藏商品')"
              :title="isOwner ? '不能收藏自己的商品' : ''"
              @click="toggleFavorite"
            >
              <UiIcon name="heart" :filled="product.favorite" />
              <span>{{ isOwner ? '自己的商品' : (acting ? '处理中...' : (product.favorite ? '已收藏' : '收藏')) }}</span>
            </button>
            <button
              v-if="canBuy"
              class="btn-cart"
              :class="{ on: isInCart }"
              :disabled="acting"
              @click="toggleCart"
            >
              {{ isInCart ? '已加入购物车' : '加入购物车' }}
            </button>
            <button v-if="canBuy" class="btn-buy" @click="buy">立即购买</button>
            <button v-if="!isOwner" class="btn-report" :disabled="acting" @click="openReport">举报</button>
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

      <ProductChatDialog
        v-if="auth.userId"
        :open="showChat"
        :product-id="product.id"
        :product-title="product.title"
        :seller-id="product.sellerId"
        :seller-nickname="product.sellerNickname"
        :current-user-id="auth.userId"
        @close="showChat = false"
        @open-center="openMessageCenter"
      />

      <div v-if="showReport" class="report-mask" @click.self="showReport = false">
        <div class="report-dialog">
          <div class="report-head">
            <h3>举报商品</h3>
            <button type="button" aria-label="关闭" @click="showReport = false">×</button>
          </div>
          <p>请说明商品信息、图片或交易沟通中存在的问题，平台会根据证据进行处理。</p>
          <textarea
            v-model="reportReason"
            rows="4"
            maxlength="500"
            placeholder="例如：商品信息不准确、图片与实物不符、价格异常、卖家沟通异常等"
          ></textarea>
          <div class="report-foot">
            <span>{{ reportReason.length }}/500</span>
            <button type="button" :disabled="reportBusy" @click="submitReport">
              {{ reportBusy ? '提交中...' : '提交举报' }}
            </button>
          </div>
          <p v-if="reportMessage" :class="reportMessage.includes('已提交') ? 'ok' : 'err'">{{ reportMessage }}</p>
        </div>
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
.breadcrumb a,
.breadcrumb-link { color: #666; text-decoration: none; }
.breadcrumb-link { padding: 0; border: 0; background: transparent; cursor: pointer; font: inherit; }
.breadcrumb a:hover,
.breadcrumb-link:hover { color: #1677ff; }
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
.meta-item label { display: block; font-size: 12px; color: #475569; font-weight: 700; margin-bottom: 2px; }
.meta-item span { font-size: 14px; color: #333; }

/* 卖家操作栏 */
.owner-bar { display: flex; gap: 8px; flex-wrap: wrap; }
.owner-bar button { padding: 8px 18px; border-radius: 6px; border: none; cursor: pointer; font-size: 14px; color: #fff; }
.owner-bar button:disabled { opacity: 0.5; cursor: default; }
.btn-edit { background: #1677ff; }
.btn-warn { background: #faad14; }
.btn-ok { background: #52c41a; }
.btn-del { background: #ff4d4f; }

/* 操作栏 */
.action-bar { display: flex; gap: 10px; align-items: center; }
.btn-fav { display: inline-flex; align-items: center; justify-content: center; gap: 6px; padding: 8px 20px; border: 1px solid #d9d9d9; border-radius: 8px; color: #333; background: #fff; cursor: pointer; font-size: 15px; }
.btn-fav:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-fav.on { border-color: #faad14; color: #faad14; background: #fffbe6; }
.btn-cart { padding: 8px 20px; border: 1px solid #ff4d4f; border-radius: 8px; background: #fff; color: #ff4d4f; cursor: pointer; font-size: 15px; }
.btn-cart:hover { background: #fff2f0; }
.btn-cart.on { border-color: #1677ff; color: #1677ff; background: #eef6ff; }
.btn-cart.on:hover { background: #e0f0ff; }
.btn-buy { padding: 10px 40px; background: #ff4d4f; color: #fff; border: none; border-radius: 8px; font-size: 17px; font-weight: 600; cursor: pointer; }
.btn-buy:hover { background: #e04343; }
.btn-report { padding: 8px 18px; border: 1px solid #d9d9d9; border-radius: 8px; color: #64748b; background: #fff; cursor: pointer; font-size: 15px; }
.btn-report:hover { color: #ff4d4f; border-color: #ffb3b3; background: #fff7f7; }
.err { color: #ff4d4f; font-size: 13px; margin: 0; }
.ok { color: #16a34a; font-size: 13px; margin: 0; }
.audit-reason { margin: 0; padding: 10px 12px; border-radius: 8px; color: #b45309; background: #fff7ed; border: 1px solid #fed7aa; line-height: 1.6; }

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

.report-mask { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 20px; background: rgba(15, 23, 42, 0.38); }
.report-dialog { width: min(480px, 100%); border-radius: 10px; background: #fff; padding: 18px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22); }
.report-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 8px; }
.report-head h3 { margin: 0; font-size: 18px; }
.report-head button { width: 32px; height: 32px; border: 0; border-radius: 50%; background: #f1f5f9; color: #475569; cursor: pointer; font-size: 20px; line-height: 1; }
.report-dialog > p { margin: 0 0 12px; color: #64748b; font-size: 14px; line-height: 1.6; }
.report-dialog textarea { box-sizing: border-box; width: 100%; resize: vertical; border: 1px solid #d9e2ef; border-radius: 8px; padding: 10px 12px; color: #17212b; font-size: 14px; line-height: 1.6; }
.report-dialog textarea:focus { outline: none; border-color: #1677ff; box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12); }
.report-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; }
.report-foot span { color: #94a3b8; font-size: 13px; }
.report-foot button { height: 38px; padding: 0 16px; border: 0; border-radius: 8px; color: #fff; background: #1677ff; cursor: pointer; font-weight: 700; }
.report-foot button:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
