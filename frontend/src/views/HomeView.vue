<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommendedProducts, searchProducts } from '../api/product'
import HomeIcon from '../components/HomeIcon.vue'
import ProductCard from '../components/ProductCard.vue'
import type { Announcement, ProductCard as ProductCardType, RecommendedProduct } from '../types/domain'
import { apiGet } from '../api/http'
import heroImage from '../assets/campus-market-hero.png'
import { formatMoney } from '../utils/money'

const router = useRouter()

const hotProducts = ref<ProductCardType[]>([])
const newProducts = ref<ProductCardType[]>([])
const recommendedProducts = ref<RecommendedProduct[]>([])
const announcements = ref<Announcement[]>([])
const keyword = ref('')
const activeTab = ref<'hot' | 'newest'>('hot')
const activeSlide = ref(0)
const dragging = ref(false)
const dragDeltaX = ref(0)
const dragStartX = ref(0)
const ignoreSlideClick = ref(false)
const pressedSlideId = ref<number | null>(null)
const brokenCarouselImages = ref<Set<number>>(new Set())
const readAnnouncementIds = ref<Set<number>>(new Set())
let slideTimer: number | undefined
const READ_ANNOUNCEMENT_KEY = 'campus-read-announcements'

type CategoryIconName = 'device' | 'study' | 'book' | 'home' | 'sport' | 'beauty' | 'bag' | 'grid'

const categories: Array<{ name: string; tone: string; icon: CategoryIconName }> = [
  { name: '数码电子', tone: 'blue', icon: 'device' },
  { name: '学习资料', tone: 'green', icon: 'study' },
  { name: '图书教材', tone: 'orange', icon: 'book' },
  { name: '生活用品', tone: 'violet', icon: 'home' },
  { name: '运动户外', tone: 'cyan', icon: 'sport' },
  { name: '美妆护肤', tone: 'rose', icon: 'beauty' },
  { name: '服饰箱包', tone: 'amber', icon: 'bag' },
  { name: '全部分类', tone: 'gray', icon: 'grid' }
]

const hotWords = ['自行车', '考研资料', '台灯', '键盘', '教材', '耳机']

const visibleProducts = computed(() => (activeTab.value === 'hot' ? hotProducts.value : newProducts.value).slice(0, 2))
const carouselProducts = computed(() => {
  const map = new Map<number, ProductCardType>()
  ;[...hotProducts.value, ...newProducts.value].forEach(product => {
    if (product.coverUrl && !map.has(product.id)) {
      map.set(product.id, product)
    }
  })
  return Array.from(map.values()).slice(0, 5)
})
const heroStats = computed(() => [
  { value: `${hotProducts.value.length + newProducts.value.length || 8}+`, label: '精选好物' },
  { value: `${recommendedProducts.value.length || 4}`, label: '智能推荐' },
  { value: '校内', label: '当面验货' }
])
const carouselTrackStyle = computed(() => ({
  transform: `translateX(calc(${-activeSlide.value * 100}% + ${dragDeltaX.value}px))`,
  transition: dragging.value ? 'none' : undefined
}))

function nextSlide() {
  if (!carouselProducts.value.length) return
  activeSlide.value = (activeSlide.value + 1) % carouselProducts.value.length
}

function prevSlide() {
  if (!carouselProducts.value.length) return
  activeSlide.value = (activeSlide.value + carouselProducts.value.length - 1) % carouselProducts.value.length
}

function setSlide(index: number) {
  activeSlide.value = index
  restartSlideTimer()
}

function manualNextSlide() {
  nextSlide()
  restartSlideTimer()
}

function manualPrevSlide() {
  prevSlide()
  restartSlideTimer()
}

function startDrag(event: PointerEvent) {
  if (!carouselProducts.value.length) return
  const target = event.target as HTMLElement | null
  const slide = target?.closest<HTMLButtonElement>('.slide-main')
  pressedSlideId.value = Number(slide?.dataset.productId) || null
  dragging.value = true
  dragStartX.value = event.clientX
  dragDeltaX.value = 0
  ignoreSlideClick.value = false
  window.clearInterval(slideTimer)
  ;(event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId)
}

function moveDrag(event: PointerEvent) {
  if (!dragging.value) return
  dragDeltaX.value = event.clientX - dragStartX.value
  if (Math.abs(dragDeltaX.value) > 8) ignoreSlideClick.value = true
}

function endDrag(event: PointerEvent) {
  if (!dragging.value) return
  const delta = dragDeltaX.value
  const pressedId = pressedSlideId.value
  pressedSlideId.value = null
  dragging.value = false
  dragDeltaX.value = 0
  ;(event.currentTarget as HTMLElement).releasePointerCapture?.(event.pointerId)
  if (delta < -72) {
    nextSlide()
  } else if (delta > 72) {
    prevSlide()
  } else if (Math.abs(delta) <= 8 && event.type === 'pointerup') {
    const target = event.target as HTMLElement | null
    const slide = target?.closest<HTMLButtonElement>('.slide-main')
    const id = pressedId || Number(slide?.dataset.productId)
    if (id) {
      ignoreSlideClick.value = true
      restartSlideTimer()
      goProduct(id)
      return
    }
  }
  restartSlideTimer()
}

function openSlide(id: number) {
  if (ignoreSlideClick.value) {
    ignoreSlideClick.value = false
    return
  }
  goProduct(id)
}

function markBrokenSlideImage(id: number) {
  const next = new Set(brokenCarouselImages.value)
  next.add(id)
  brokenCarouselImages.value = next
}

function categoryIconFor(categoryName: string): CategoryIconName {
  if (categoryName.includes('数码')) return 'device'
  if (categoryName.includes('图书')) return 'book'
  if (categoryName.includes('学习')) return 'study'
  if (categoryName.includes('生活')) return 'home'
  if (categoryName.includes('运动')) return 'sport'
  if (categoryName.includes('美妆')) return 'beauty'
  if (categoryName.includes('服饰') || categoryName.includes('箱包')) return 'bag'
  return 'grid'
}

function restartSlideTimer() {
  window.clearInterval(slideTimer)
  slideTimer = window.setInterval(nextSlide, 4200)
}

function loadReadAnnouncementIds() {
  try {
    const raw = localStorage.getItem(READ_ANNOUNCEMENT_KEY)
    const ids = raw ? JSON.parse(raw) : []
    readAnnouncementIds.value = new Set(Array.isArray(ids) ? ids.map(Number) : [])
  } catch {
    readAnnouncementIds.value = new Set()
  }
}

function markAnnouncementRead(id: number) {
  const next = new Set(readAnnouncementIds.value)
  next.add(id)
  readAnnouncementIds.value = next
  localStorage.setItem(READ_ANNOUNCEMENT_KEY, JSON.stringify([...next]))
}

onMounted(async () => {
  loadReadAnnouncementIds()
  try {
    const [hot, latest, annRes] = await Promise.all([
      searchProducts({ sort: 'hot', size: 4 }),
      searchProducts({ sort: 'newest', size: 4 }),
      apiGet<Announcement[]>('/announcements')
    ])
    hotProducts.value = hot.data?.records || []
    newProducts.value = latest.data?.records || []
    announcements.value = annRes.data || []
  } catch {
    /* Demo page keeps quiet when backend is offline. */
  }

  try {
    const recRes = await getRecommendedProducts(4)
    recommendedProducts.value = recRes.data || []
  } catch {
    recommendedProducts.value = []
  }
  restartSlideTimer()
})

onBeforeUnmount(() => {
  window.clearInterval(slideTimer)
})

watch(carouselProducts, products => {
  if (activeSlide.value >= products.length) activeSlide.value = 0
})

function goProduct(id: number) {
  router.push(`/products/${id}`)
}

function goList(sort: string) {
  router.push({ path: '/products', query: { sort } })
}

function search() {
  router.push({ path: '/products', query: keyword.value ? { keyword: keyword.value } : {} })
}

function searchWord(word: string) {
  router.push({ path: '/products', query: { keyword: word } })
}

function goAnnouncements() {
  router.push('/announcements')
}

function openAnnouncement(item: Announcement) {
  markAnnouncementRead(item.id)
  goAnnouncements()
}
</script>

<template>
  <div class="home-page">
    <div class="home-grid">
      <main class="home-main">
        <section class="market-hero">
          <div class="hero-copy">
            <p class="eyebrow">同校交易 · 当面验货 · 闲置流动</p>
            <h1>校园二手交易</h1>
            <p class="summary">让闲置流转起来，让校园生活更美好。</p>

            <div class="hero-stats" aria-label="平台亮点">
              <span v-for="item in heroStats" :key="item.label" class="stat-pill">
                <strong>{{ item.value }}</strong>
                <small>{{ item.label }}</small>
              </span>
            </div>

            <div class="search-box">
              <input v-model="keyword" placeholder="搜索商品、分类或关键词" @keyup.enter="search" />
              <button type="button" @click="search">搜索</button>
            </div>

            <div class="hot-words">
              <span class="hot-label" aria-hidden="true">热</span>
              <span class="hot-title">热门搜索</span>
              <button v-for="word in hotWords" :key="word" type="button" @click="searchWord(word)">
                {{ word }}
              </button>
            </div>
          </div>
          <img class="hero-image" :src="heroImage" alt="" />
          <span class="motion-ring" aria-hidden="true"></span>
        </section>

        <section class="product-carousel" aria-label="商品图片轮播">
          <div class="carousel-head">
            <div>
              <span class="section-kicker">精选上新</span>
              <h2>正在校园里流转的好物</h2>
            </div>
            <button type="button" @click="goList('hot')">进入市场</button>
          </div>

          <div v-if="carouselProducts.length" class="carousel-stage" :class="{ dragging }">
            <button class="slide-nav prev" type="button" aria-label="上一张商品图" @click.stop="manualPrevSlide">‹</button>
            <div
              class="carousel-viewport"
              @pointerdown="startDrag"
              @pointermove="moveDrag"
              @pointerup="endDrag"
              @pointercancel="endDrag"
              @pointerleave="endDrag"
            >
              <div class="carousel-track" :style="carouselTrackStyle">
                <button
                  v-for="product in carouselProducts"
                  :key="product.id"
                  class="slide-main"
                  type="button"
                  :data-product-id="product.id"
                  @click="openSlide(product.id)"
                >
                  <img
                    v-if="product.coverUrl && !brokenCarouselImages.has(product.id)"
                    :src="product.coverUrl"
                    :alt="product.title"
                    draggable="false"
                    @error="markBrokenSlideImage(product.id)"
                  />
                  <span v-else class="slide-fallback">
                    <HomeIcon :name="categoryIconFor(product.categoryName)" />
                  </span>
                  <span class="slide-shade"></span>
                  <span class="slide-badge">{{ product.categoryName }}</span>
                  <span class="slide-copy">
                    <strong>{{ product.title }}</strong>
                    <small>{{ product.itemCondition }} · {{ product.viewCount }} 浏览量</small>
                  </span>
                  <span class="slide-price">&yen;{{ formatMoney(product.price) }}</span>
                </button>
              </div>
            </div>
            <button class="slide-nav next" type="button" aria-label="下一张商品图" @click.stop="manualNextSlide">›</button>
          </div>

          <div v-else class="carousel-empty">
            <strong>暂无可轮播商品</strong>
            <span>商品上传图片后会自动出现在这里。</span>
          </div>

          <div v-if="carouselProducts.length > 1" class="slide-dots" aria-label="轮播切换">
            <button
              v-for="(item, index) in carouselProducts"
              :key="item.id"
              type="button"
              :class="{ active: activeSlide === index }"
              :aria-label="`查看第 ${index + 1} 张商品图`"
              :aria-pressed="activeSlide === index"
              @click="setSlide(index)"
            ></button>
          </div>
        </section>

        <section class="category-strip" aria-label="商品分类">
          <button
            v-for="category in categories"
            :key="category.name"
            type="button"
            :class="`tone-${category.tone}`"
            @click="router.push('/products')"
          >
            <span class="category-icon">
              <HomeIcon :name="category.icon" />
            </span>
            {{ category.name }}
          </button>
        </section>

        <section v-if="recommendedProducts.length" class="recommendation-section" aria-label="猜你喜欢">
          <div class="recommend-head">
            <div>
              <h2 class="recommend-title">猜你喜欢</h2>
            </div>
            <button type="button" @click="goList('hot')">查看更多</button>
          </div>
          <div class="recommend-grid">
            <button
              v-for="item in recommendedProducts"
              :key="item.id"
              class="recommend-card"
              type="button"
              @click="goProduct(item.id)"
            >
              <span class="recommend-cover">
                <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" />
                <HomeIcon v-else :name="categoryIconFor(item.categoryName)" />
              </span>
              <span class="recommend-body">
                <span class="recommend-category">{{ item.categoryName }}</span>
                <strong>{{ item.title }}</strong>
                <small>{{ item.reason || '根据热度和新鲜度推荐' }}</small>
              </span>
              <span class="recommend-price">&yen;{{ formatMoney(item.price) }}</span>
            </button>
          </div>
        </section>

        <section class="product-section">
          <div class="section-tabs">
            <button :class="{ active: activeTab === 'hot' }" type="button" @click="activeTab = 'hot'">推荐好物</button>
            <button :class="{ active: activeTab === 'newest' }" type="button" @click="activeTab = 'newest'">最新发布</button>
            <button class="more-link" type="button" @click="goList(activeTab)">查看更多</button>
          </div>

          <div v-if="visibleProducts.length" class="product-grid">
            <ProductCard v-for="p in visibleProducts" :key="p.id" :product="p" @select="goProduct" />
          </div>
          <div v-else class="empty-panel">
            <strong>暂时还没有商品</strong>
            <span>发布第一件闲置，让首页热闹起来。</span>
            <button type="button" @click="router.push('/publish')">发布闲置</button>
          </div>
        </section>
      </main>

      <aside class="side-panels">
        <section class="safe-panel">
          <div class="panel-title">
            <span class="panel-icon shield">✓</span>
            <h2>交易安全提示</h2>
          </div>
          <ul>
            <li>建议当面交易，确认人身财产安全</li>
            <li>不要脱离平台交易，谨防诈骗</li>
            <li>仔细核对商品信息，确认无误再交易</li>
            <li>保留聊天记录和交易凭证</li>
          </ul>
          <button type="button" @click="router.push('/safety')">了解更多安全知识</button>
        </section>

        <section class="rank-panel">
          <div class="panel-title compact">
            <span class="panel-icon fire">热</span>
            <h2>本周热门榜</h2>
            <button type="button" @click="goList('hot')">更多</button>
          </div>
          <ol v-if="hotProducts.length">
            <li v-for="(item, index) in hotProducts.slice(0, 5)" :key="item.id">
              <span v-if="index < 3" :class="['rank-medal', `place-${index + 1}`]">
                <HomeIcon name="medal" />
              </span>
              <span v-else>{{ index + 1 }}</span>
              <button type="button" @click="goProduct(item.id)">{{ item.title }}</button>
              <strong>¥{{ formatMoney(item.price) }}</strong>
            </li>
          </ol>
          <p v-else class="side-empty">暂无热门商品</p>
        </section>

        <section class="notice-panel">
          <div class="panel-title compact">
            <span class="panel-icon notice">告</span>
            <h2>平台公告</h2>
            <button type="button" @click="goAnnouncements">更多</button>
          </div>
          <div v-if="announcements.length" class="notice-list">
            <button
              v-for="a in announcements.slice(0, 2)"
              :key="a.id"
              type="button"
              :class="{ read: readAnnouncementIds.has(a.id) }"
              @click="openAnnouncement(a)"
            >
              <span>{{ a.title }}</span>
              <i v-if="!readAnnouncementIds.has(a.id)"></i>
            </button>
          </div>
          <p v-else class="side-empty">暂无公告</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  position: relative;
  isolation: isolate;
  max-width: 1440px;
  margin: 0 auto;
}

.home-page::before {
  position: absolute;
  inset: -42px -28px auto;
  z-index: -1;
  height: 360px;
  background:
    linear-gradient(135deg, rgba(22, 119, 255, 0.12), rgba(34, 197, 94, 0.07) 42%, rgba(249, 115, 22, 0.06) 100%),
    linear-gradient(180deg, rgba(255, 255, 255, 0), rgba(248, 251, 255, 0.92));
  content: "";
  pointer-events: none;
}

.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 22px;
  align-items: start;
}

.home-grid > * {
  animation: soft-rise 0.58s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.home-grid > *:nth-child(2) {
  animation-delay: 0.08s;
}

.home-main,
.side-panels {
  display: grid;
  gap: 18px;
}

.market-hero,
.product-carousel,
.category-strip,
.recommendation-section,
.product-section,
.safe-panel,
.rank-panel,
.notice-panel {
  border: 1px solid rgba(219, 228, 238, 0.92);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 46px rgba(15, 23, 42, 0.08);
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease;
}

.market-hero:hover,
.product-carousel:hover,
.category-strip:hover,
.recommendation-section:hover,
.product-section:hover,
.safe-panel:hover,
.rank-panel:hover,
.notice-panel:hover {
  border-color: rgba(147, 197, 253, 0.72);
  box-shadow: 0 24px 54px rgba(15, 23, 42, 0.1);
  transform: translateY(-2px);
}

.market-hero {
  position: relative;
  min-height: 274px;
  overflow: hidden;
  box-shadow: 0 26px 64px rgba(22, 119, 255, 0.16);
}

.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.market-hero::after {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.88) 48%, rgba(255, 255, 255, 0.18) 100%),
    linear-gradient(180deg, rgba(255, 255, 255, 0) 58%, rgba(255, 255, 255, 0.32) 100%);
  content: "";
}

.hero-copy {
  position: relative;
  z-index: 3;
  max-width: 660px;
  padding: 34px 36px;
}

.motion-ring {
  position: absolute;
  right: 8%;
  bottom: 34px;
  z-index: 2;
  width: 132px;
  height: 132px;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 50%;
  box-shadow: inset 0 0 0 18px rgba(255, 255, 255, 0.13), 0 18px 42px rgba(15, 23, 42, 0.12);
  animation: ring-drift 6.8s ease-in-out infinite;
  pointer-events: none;
}

.motion-ring::after {
  position: absolute;
  inset: 18px;
  border: 1px dashed rgba(22, 119, 255, 0.44);
  border-radius: 50%;
  content: "";
  animation: ring-spin 14s linear infinite;
}

.eyebrow {
  margin: 0 0 10px;
  color: var(--brand-blue);
  font-size: 14px;
  font-weight: 900;
}

h1 {
  margin: 0;
  color: #132033;
  font-size: 44px;
  line-height: 1.14;
}

.summary {
  margin: 12px 0 24px;
  color: #536275;
  font-size: 17px;
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 0 18px;
}

.stat-pill {
  display: inline-grid;
  grid-template-columns: auto auto;
  gap: 7px;
  align-items: baseline;
  min-height: 36px;
  padding: 8px 12px;
  border: 1px solid rgba(191, 219, 254, 0.92);
  border-radius: 999px;
  color: #1e3a8a;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 24px rgba(22, 119, 255, 0.1);
}

.stat-pill strong {
  color: #0f172a;
  font-size: 17px;
  font-weight: 900;
}

.stat-pill small {
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.search-box {
  display: flex;
  max-width: 610px;
  height: 58px;
  overflow: hidden;
  border: 1px solid #86b8ff;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(22, 119, 255, 0.12);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.search-box:focus-within,
.search-box:hover {
  border-color: #1677ff;
  box-shadow: 0 16px 36px rgba(22, 119, 255, 0.18);
  transform: translateY(-1px);
}

.search-box input {
  flex: 1;
  border: 0;
  padding: 0 20px;
  color: #17212b;
  font-size: 16px;
}

.search-box input:focus {
  outline: none;
}

.search-box button {
  width: 120px;
  border-radius: 10px;
  margin: 7px;
  background: var(--brand-blue);
  font-size: 16px;
  font-weight: 900;
}

.hot-words {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 15px;
  width: fit-content;
  max-width: 100%;
  padding: 8px 10px;
  border: 1px solid rgba(191, 219, 254, 0.82);
  border-radius: 999px;
  color: #425466;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.08);
  font-size: 13px;
}

.hot-label {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #fb923c, #ef4444);
  font-weight: 900;
}

.hot-title {
  color: #1e3a8a;
  font-weight: 900;
}

.hot-words button {
  height: 28px;
  padding: 0 13px;
  border-radius: 999px;
  color: #334155;
  background: #fff;
  box-shadow: 0 5px 14px rgba(15, 23, 42, 0.08);
}

.hot-words button:hover {
  color: #fff;
  background: var(--brand-blue);
}

.product-carousel {
  padding: 18px;
}

.carousel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  width: fit-content;
  margin-bottom: 7px;
  padding: 5px 10px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eaf3ff;
  font-size: 13px;
  font-weight: 900;
}

.section-kicker::before {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-blue);
  content: "";
}

.carousel-head h2 {
  margin: 0;
  color: #132033;
  font-size: 24px;
  line-height: 1.2;
}

.carousel-head > button {
  height: 38px;
  padding: 0 15px;
  border: 1px solid #cfe1ff;
  border-radius: 8px;
  color: var(--brand-blue);
  background: #f7fbff;
  font-weight: 900;
}

.carousel-stage {
  position: relative;
  display: block;
  min-height: 316px;
  overflow: hidden;
  border-radius: 8px;
  background: #eef4fb;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.62);
  touch-action: pan-y;
  user-select: none;
}

.carousel-viewport {
  min-height: 316px;
  overflow: hidden;
  border-radius: 8px;
  cursor: grab;
  touch-action: pan-y;
}

.carousel-stage.dragging .carousel-viewport {
  cursor: grabbing;
}

.carousel-track {
  display: flex;
  min-height: 316px;
  will-change: transform;
  transition: transform 0.38s ease;
}

.slide-main {
  position: relative;
  display: block;
  flex: 0 0 100%;
  width: 100%;
  min-height: 316px;
  overflow: hidden;
  padding: 0;
  border-radius: 8px;
  background: #eef4fb;
  text-align: left;
  transform: translateZ(0);
}

.slide-main::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 8%, rgba(255, 255, 255, 0.18) 24%, transparent 40%);
  content: "";
  opacity: 0;
  transform: translateX(-40%);
  transition: opacity 0.2s ease, transform 0.42s ease;
}

.slide-main:hover::after {
  opacity: 1;
  transform: translateX(42%);
}

.slide-main img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
  -webkit-user-drag: none;
}

.slide-main:hover img {
  transform: scale(1.035);
}

.slide-fallback {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.72);
  background:
    radial-gradient(circle at 70% 22%, rgba(255, 255, 255, 0.28), transparent 34%),
    linear-gradient(135deg, #1e3a8a, #0f766e 56%, #f97316);
}

.slide-fallback .home-icon {
  width: 120px;
  height: 120px;
  stroke-width: 1.3;
}

.slide-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.74) 0%, rgba(15, 23, 42, 0.32) 44%, rgba(15, 23, 42, 0.06) 100%),
    linear-gradient(180deg, rgba(15, 23, 42, 0) 54%, rgba(15, 23, 42, 0.58) 100%);
}

.slide-badge,
.slide-copy,
.slide-price {
  position: absolute;
  z-index: 1;
}

.slide-badge {
  top: 22px;
  left: 22px;
  min-height: 32px;
  padding: 7px 13px;
  border-radius: 999px;
  color: #dbeafe;
  background: rgba(22, 119, 255, 0.92);
  font-size: 13px;
  font-weight: 900;
}

.slide-copy {
  bottom: 28px;
  left: 28px;
  display: grid;
  gap: 7px;
  max-width: min(560px, calc(100% - 180px));
  color: #fff;
}

.slide-copy strong {
  overflow: hidden;
  font-size: 31px;
  line-height: 1.18;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slide-copy small {
  color: rgba(255, 255, 255, 0.82);
  font-size: 15px;
  font-weight: 800;
}

.slide-price {
  right: 28px;
  bottom: 28px;
  min-width: 108px;
  padding: 12px 15px;
  border-radius: 8px;
  color: #ffedd5;
  background: rgba(249, 115, 22, 0.92);
  font-size: 28px;
  font-weight: 900;
  text-align: center;
}

.slide-nav {
  position: absolute;
  top: 50%;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.56);
  border-radius: 50%;
  color: #fff;
  background: rgba(15, 23, 42, 0.42);
  font-size: 30px;
  line-height: 1;
  transform: translateY(-50%);
  transition: background 0.2s ease, transform 0.2s ease;
}

.slide-nav:hover {
  background: rgba(22, 119, 255, 0.9);
  transform: translateY(-50%) scale(1.06);
}

.slide-nav.prev {
  left: 16px;
}

.slide-nav.next {
  right: 16px;
}

.slide-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 13px;
}

.slide-dots button {
  width: 24px;
  height: 8px;
  padding: 0;
  border-radius: 999px;
  background: #d5e0ee;
  transition: width 0.2s ease, background 0.2s ease;
}

.slide-dots button.active {
  width: 42px;
  background: var(--brand-blue);
}

.carousel-empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  min-height: 220px;
  border: 1px dashed #c8d6e8;
  border-radius: 8px;
  color: #64748b;
  background: #f8fbff;
  text-align: center;
}

.carousel-empty strong {
  color: #17212b;
  font-size: 18px;
}

.category-strip {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 8px;
  padding: 18px 20px;
}

.category-strip button {
  display: grid;
  gap: 10px;
  justify-items: center;
  min-height: 92px;
  padding: 10px 6px;
  border-radius: 10px;
  color: #334155;
  background: transparent;
  font-size: 14px;
  font-weight: 800;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.category-strip button:hover {
  color: var(--brand-blue);
  background: var(--soft-blue);
  transform: translateY(-2px);
}

.category-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #fff;
  font-size: 18px;
  font-weight: 900;
  box-shadow: 0 12px 18px rgba(15, 23, 42, 0.12);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.category-strip button:hover .category-icon {
  box-shadow: 0 16px 26px rgba(15, 23, 42, 0.18);
  transform: translateY(-2px) rotate(-3deg);
}

.category-icon .home-icon {
  width: 24px;
  height: 24px;
}

.tone-blue .category-icon { background: #1677ff; }
.tone-green .category-icon { background: #22c55e; }
.tone-orange .category-icon { background: #fb923c; }
.tone-violet .category-icon { background: #8b5cf6; }
.tone-cyan .category-icon { background: #38bdf8; }
.tone-rose .category-icon { background: #fb7185; }
.tone-amber .category-icon { background: #f59e0b; }
.tone-gray .category-icon { background: #94a3b8; }

.recommendation-section {
  padding: 18px;
}

.recommend-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.recommend-head h2 {
  margin: 0;
  color: #132033;
  font-size: 24px;
  line-height: 1.2;
}

.recommend-title {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding-left: 12px;
  border-left: 4px solid var(--brand-blue);
  background: linear-gradient(90deg, rgba(22, 119, 255, 0.12), transparent 74%);
}

.recommend-head > button {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #cfe1ff;
  border-radius: 8px;
  color: var(--brand-blue);
  background: #f7fbff;
  font-weight: 900;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.recommend-card {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 104px;
  padding: 10px;
  border: 1px solid #e2eaf4;
  border-radius: 8px;
  color: #17212b;
  background: #fff;
  text-align: left;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.recommend-card:hover {
  border-color: #bfd8ff;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.1);
  transform: translateY(-1px);
}

.recommend-cover {
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  overflow: hidden;
  border-radius: 7px;
  color: #fff;
  background: linear-gradient(135deg, #1677ff, #0f9f8f);
}

.recommend-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recommend-cover .home-icon {
  width: 34px;
  height: 34px;
}

.recommend-body {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.recommend-category {
  width: fit-content;
  max-width: 100%;
  padding: 3px 8px;
  border-radius: 999px;
  color: #1677ff;
  background: #edf6ff;
  font-size: 12px;
  font-weight: 900;
}

.recommend-body strong,
.recommend-body small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-body strong {
  color: #17212b;
  font-size: 16px;
}

.recommend-body small {
  color: #64748b;
  font-size: 13px;
}

.recommend-price {
  color: #f97316;
  font-size: 20px;
  font-weight: 900;
}

.product-section {
  padding: 18px;
}

.section-tabs {
  display: flex;
  align-items: center;
  gap: 26px;
  margin-bottom: 16px;
}

.section-tabs button {
  position: relative;
  height: 38px;
  padding: 0;
  color: #64748b;
  background: transparent;
  font-size: 20px;
  font-weight: 900;
}

.section-tabs button::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  border-radius: 999px;
  background: transparent;
  content: "";
}

.section-tabs button.active {
  color: #132033;
}

.section-tabs button.active::after {
  background: var(--brand-blue);
}

.section-tabs .more-link {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 14px;
  border: 1px solid #cfe1ff;
  border-radius: 8px;
  color: var(--brand-blue);
  background: #f7fbff;
  font-size: 14px;
}

.section-tabs .more-link::after {
  display: none;
}

.product-section :deep(.product-card) {
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.product-section :deep(.product-card:hover) {
  border-color: rgba(147, 197, 253, 0.84);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.11);
  transform: translateY(-3px);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.empty-panel {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 42px 18px;
  color: #64748b;
  text-align: center;
}

.empty-panel strong {
  color: #17212b;
  font-size: 18px;
}

.empty-panel button {
  margin-top: 8px;
  background: var(--brand-blue);
  font-weight: 900;
}

.side-panels {
  position: sticky;
  top: 92px;
}

.safe-panel,
.rank-panel,
.notice-panel {
  padding: 20px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-title.compact {
  margin-bottom: 14px;
}

.panel-title h2 {
  margin: 0;
  color: #17212b;
  font-size: 20px;
}

.panel-title.compact button {
  margin-left: auto;
  padding: 0;
  color: #94a3b8;
  background: transparent;
  font-size: 13px;
}

.panel-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
}

.shield { background: #22c55e; }
.fire { background: #fb923c; }
.notice { background: #60a5fa; }

.safe-panel ul,
.rank-panel ol {
  display: grid;
  gap: 13px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.safe-panel li {
  position: relative;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 9px;
  align-items: start;
  padding: 10px 12px;
  border: 1px solid #fed7aa;
  border-left: 4px solid #f97316;
  border-radius: 8px;
  color: #7c2d12;
  background: #fff7ed;
  line-height: 1.55;
}

.safe-panel li::before {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  background: #f97316;
  content: "!";
  font-size: 13px;
  font-weight: 900;
}

.safe-panel > button {
  width: 100%;
  height: 42px;
  margin-top: 18px;
  color: #16835f;
  background: #effaf5;
  font-weight: 900;
}

.rank-panel li {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  gap: 10px;
  align-items: center;
}

.rank-panel li > span {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  color: #fff;
  background: #f59e0b;
  font-size: 12px;
  font-weight: 900;
}

.rank-panel li:nth-child(n+4) > span {
  color: #64748b;
  background: #eef2f7;
}

.rank-panel li > .rank-medal {
  border-radius: 50%;
  color: #fff;
}

.rank-medal .home-icon {
  width: 18px;
  height: 18px;
  stroke-width: 2.2;
}

.rank-medal.place-1 {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  box-shadow: 0 7px 14px rgba(245, 158, 11, 0.28);
}

.rank-medal.place-2 {
  background: linear-gradient(135deg, #dbe4ef, #94a3b8);
  box-shadow: 0 7px 14px rgba(100, 116, 139, 0.22);
}

.rank-medal.place-3 {
  background: linear-gradient(135deg, #f59e0b, #b45309);
  box-shadow: 0 7px 14px rgba(180, 83, 9, 0.22);
}

.rank-panel li button {
  overflow: hidden;
  padding: 0;
  color: #334155;
  background: transparent;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-panel li strong {
  color: #f97316;
  font-size: 14px;
}

.notice-panel {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.notice-list {
  display: grid;
  gap: 10px;
}

.notice-list button {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 8px;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
  color: #334155;
  background: transparent;
  text-align: left;
}

.notice-list button span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-list button.read {
  color: #94a3b8;
}

.notice-list i {
  justify-self: end;
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  border-radius: 50%;
  background: #ef4444;
}

.side-empty {
  margin: 0;
  color: #94a3b8;
}

@keyframes soft-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes ring-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }

  50% {
    transform: translate3d(-10px, -8px, 0) scale(1.04);
  }
}

@keyframes ring-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1180px) {
  .home-grid {
    grid-template-columns: 1fr;
  }

  .side-panels {
    position: static;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .category-strip {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-copy {
    padding: 26px 22px;
  }

  h1 {
    font-size: 34px;
  }

  .search-box {
    display: grid;
    height: auto;
  }

  .hero-stats {
    gap: 8px;
  }

  .motion-ring {
    right: -18px;
    bottom: 24px;
    width: 92px;
    height: 92px;
  }

  .search-box input {
    min-height: 48px;
  }

  .search-box button {
    width: auto;
    height: 44px;
  }

  .carousel-head {
    align-items: flex-start;
  }

  .carousel-head h2 {
    font-size: 21px;
  }

  .carousel-stage,
  .carousel-viewport,
  .carousel-track,
  .slide-main {
    min-height: 248px;
  }

  .slide-copy {
    right: 20px;
    bottom: 76px;
    left: 20px;
    max-width: none;
  }

  .slide-copy strong {
    font-size: 24px;
  }

  .slide-price {
    right: auto;
    bottom: 20px;
    left: 20px;
    min-width: 92px;
    font-size: 22px;
  }

  .category-strip,
  .recommend-grid,
  .product-grid,
  .side-panels {
    grid-template-columns: 1fr;
  }

  .recommend-card {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .recommend-cover {
    width: 72px;
    height: 72px;
  }

  .recommend-price {
    grid-column: 2;
    justify-self: start;
    font-size: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-grid > *,
  .motion-ring,
  .motion-ring::after {
    animation: none;
  }

  .market-hero,
  .product-carousel,
  .category-strip,
  .recommendation-section,
  .product-section,
  .safe-panel,
  .rank-panel,
  .notice-panel,
  .search-box,
  .slide-main img,
  .slide-main::after,
  .slide-nav,
  .category-strip button,
  .category-icon,
  .recommend-card,
  .product-section :deep(.product-card) {
    transition: none;
  }
}
</style>
