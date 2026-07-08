<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchProducts } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
import heroImage from '../assets/campus-market-hero.png'
import type { ProductCard as ProductCardType } from '../types/domain'

const route = useRoute()
const router = useRouter()

const products = ref<ProductCardType[]>([])
const total = ref(0)
const page = ref(Number(route.query.page) > 0 ? Number(route.query.page) : 1)
const loading = ref(false)

const keyword = ref((route.query.keyword as string) || '')
const sort = ref((route.query.sort as string) || 'newest')
const routeCategoryId = Number(route.query.categoryId)
const activeCategoryId = ref<number | null>(Number.isFinite(routeCategoryId) ? routeCategoryId : null)
const activeFeed = ref<'recommended' | 'newest'>('recommended')
const size = 12

const categories: Array<{ label: string; categoryId: number | null; tone: string; icon: string }> = [
  { label: '推荐', categoryId: null, tone: 'blue', icon: '荐' },
  { label: '图书教材', categoryId: 2, tone: 'green', icon: '书' },
  { label: '数码配件', categoryId: 1, tone: 'cyan', icon: '数' },
  { label: '生活用品', categoryId: 3, tone: 'violet', icon: '生' },
  { label: '运动户外', categoryId: 4, tone: 'orange', icon: '动' },
  { label: '服饰鞋包', categoryId: 5, tone: 'amber', icon: '包' },
  { label: '美妆护肤', categoryId: 6, tone: 'rose', icon: '美' }
]
const hotKeywords = ['自行车', '考研资料', '台灯', '键盘', '教材', '耳机']

const visibleProducts = computed(() => {
  if (activeFeed.value === 'newest') {
    return [...products.value].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
  }
  return products.value
})

const rankProducts = computed(() => [...products.value].sort((a, b) => b.viewCount - a.viewCount).slice(0, 5))
const selectedCategory = computed(() => categories.find(item => item.categoryId === activeCategoryId.value) || categories[0])
const resultLabel = computed(() => {
  const category = selectedCategory.value.categoryId === null ? '全校' : selectedCategory.value.label
  return `${category} · ${total.value} 件商品`
})

async function fetch() {
  loading.value = true
  syncRouteState()
  try {
    const res = await searchProducts({
      keyword: keyword.value || undefined,
      categoryId: activeCategoryId.value ?? undefined,
      sort: sort.value,
      page: page.value,
      size
    })
    products.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/products/${id}`)
}

function syncRouteState() {
  const query: Record<string, string> = {}
  if (keyword.value.trim()) query.keyword = keyword.value.trim()
  if (sort.value) query.sort = sort.value
  if (activeCategoryId.value !== null) query.categoryId = String(activeCategoryId.value)
  if (page.value > 1) query.page = String(page.value)
  void router.replace({ path: '/products', query }).catch(() => {})
}

function search() {
  page.value = 1
  fetch()
}

function useKeyword(word: string) {
  keyword.value = word
  search()
}

function selectCategory(categoryId: number | null) {
  keyword.value = ''
  activeCategoryId.value = categoryId
  page.value = 1
}

function resetFilters() {
  keyword.value = ''
  activeCategoryId.value = null
  sort.value = 'newest'
  page.value = 1
  fetch()
}

onMounted(fetch)
watch([sort, page, activeCategoryId], fetch)
</script>

<template>
  <div class="product-list-page" data-test="market-shell">
    <section class="market-hero-grid" aria-label="校园二手交易市场">
      <div class="hero-panel" :style="{ '--hero-image': `url(${heroImage})` }">
        <p class="eyebrow">校园市场</p>
        <h1>校园二手交易</h1>
        <p>让闲置流转起来，让校园生活更美好</p>

        <div class="hero-search">
          <span class="search-symbol" aria-hidden="true">⌕</span>
          <input v-model="keyword" type="text" placeholder="搜索商品、分类或关键词" @keyup.enter="search" />
          <button type="button" @click="search">搜索</button>
        </div>

        <div class="quick-keywords" aria-label="热门搜索">
          <span>热门搜索:</span>
          <button v-for="word in hotKeywords" :key="word" type="button" @click="useKeyword(word)">
            {{ word }}
          </button>
        </div>
      </div>

      <aside class="safe-panel">
        <div class="panel-icon success" aria-hidden="true">✓</div>
        <div>
          <h2>交易安全提示</h2>
          <ul>
            <li>建议当面交易，确保人身财产安全</li>
            <li>不要脱离平台交易，谨防诈骗</li>
            <li>仔细核对商品信息，确认无误再交易</li>
            <li>保留聊天记录和交易凭证</li>
          </ul>
          <button type="button" @click="router.push('/safety')">了解更多安全知识</button>
        </div>
      </aside>
    </section>

    <section class="category-dock" aria-label="商品分类">
      <button
        v-for="category in categories"
        :key="category.label"
        type="button"
        :class="['category-tile', category.tone, { active: activeCategoryId === category.categoryId }]"
        :aria-pressed="activeCategoryId === category.categoryId"
        @click="selectCategory(category.categoryId)"
      >
        <span class="category-icon" aria-hidden="true">{{ category.icon }}</span>
        <span>{{ category.label }}</span>
      </button>
    </section>

    <div class="market-content">
      <main class="product-stream">
        <div class="stream-head">
          <div class="section-tabs">
            <button :class="{ active: activeFeed === 'recommended' }" type="button" @click="activeFeed = 'recommended'">
              推荐好物
            </button>
            <button :class="{ active: activeFeed === 'newest' }" type="button" @click="activeFeed = 'newest'">
              最新发布
            </button>
          </div>
          <button class="publish-link" type="button" @click="router.push('/publish')">发布闲置</button>
        </div>

        <div class="filter-line">
          <p>{{ resultLabel }}</p>
          <div class="sort-bar">
            <span>排序</span>
            <button :class="{ active: sort === 'newest' }" type="button" @click="sort = 'newest'">最新</button>
            <button :class="{ active: sort === 'hot' }" type="button" @click="sort = 'hot'">热度</button>
            <button :class="{ active: sort === 'priceAsc' }" type="button" @click="sort = 'priceAsc'">价格↑</button>
            <button :class="{ active: sort === 'priceDesc' }" type="button" @click="sort = 'priceDesc'">价格↓</button>
            <button class="reset-btn" type="button" @click="resetFilters">重置</button>
          </div>
        </div>

        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="visibleProducts.length" class="product-grid">
          <ProductCard v-for="p in visibleProducts" :key="p.id" :product="p" @select="goDetail" />
        </div>
        <div v-else class="empty-hint">
          <strong>没有找到匹配商品</strong>
          <span>换个关键词，或者发布你手里的闲置。</span>
          <button type="button" @click="router.push('/publish')">发布闲置</button>
        </div>

        <div v-if="total > size" class="pagination">
          <button :disabled="page <= 1" type="button" @click="page--">上一页</button>
          <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
          <button :disabled="page >= Math.ceil(total / size)" type="button" @click="page++">下一页</button>
        </div>
      </main>

      <aside class="market-aside">
        <section class="side-panel rank-panel">
          <div class="side-title">
            <h2>本周热门榜</h2>
            <button type="button" @click="sort = 'hot'">更多</button>
          </div>
          <ol v-if="rankProducts.length">
            <li v-for="(item, index) in rankProducts" :key="item.id">
              <span>{{ index + 1 }}</span>
              <span class="rank-thumb">
                <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
                <b v-else>{{ item.categoryName.slice(0, 1) }}</b>
              </span>
              <button type="button" @click="goDetail(item.id)">{{ item.title }}</button>
              <strong>&yen;{{ item.price.toFixed(0) }}</strong>
            </li>
          </ol>
          <p v-else>暂无商品</p>
        </section>

        <section class="side-panel notice-panel">
          <div class="notice-mark" aria-hidden="true">告</div>
          <div>
            <div class="side-title">
              <h2>平台公告</h2>
              <button type="button" @click="router.push('/messages')">更多</button>
            </div>
            <p>关于加强校园二手交易管理的通知</p>
            <small>请优先选择校内公共区域交易，贵重物品当面验收。</small>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.product-list-page {
  max-width: 1360px;
  margin: 0 auto;
}

.market-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: stretch;
  margin-bottom: 18px;
}

.hero-panel,
.safe-panel,
.category-dock,
.side-panel {
  border: 1px solid rgba(219, 228, 238, 0.95);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.07);
}

.hero-panel {
  position: relative;
  min-height: 258px;
  overflow: hidden;
  padding: 34px 38px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.86) 42%, rgba(255, 255, 255, 0.24) 100%),
    var(--hero-image) center right / cover no-repeat;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--brand-blue);
  font-size: 15px;
  font-weight: 900;
}

.hero-panel h1 {
  margin: 0;
  color: #111827;
  font-size: 42px;
  line-height: 1.12;
}

.hero-panel p:not(.eyebrow) {
  margin: 12px 0 28px;
  color: #475569;
  font-size: 17px;
}

.hero-search {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 118px;
  align-items: center;
  width: min(620px, 100%);
  min-height: 62px;
  border: 1px solid #7fb4ff;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 30px rgba(22, 119, 255, 0.12);
}

.search-symbol {
  display: grid;
  place-items: center;
  color: #334155;
  font-size: 26px;
  line-height: 1;
}

.hero-search input {
  width: 100%;
  border: 0;
  color: #17212b;
  background: transparent;
  font-size: 17px;
}

.hero-search input:focus {
  outline: none;
}

.hero-search button {
  height: 48px;
  margin-right: 8px;
  border-radius: 10px;
  background: var(--brand-blue);
  font-size: 16px;
  font-weight: 900;
}

.quick-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  align-items: center;
  margin-top: 16px;
  color: #64748b;
  font-size: 14px;
}

.quick-keywords button {
  min-height: 30px;
  padding: 0 14px;
  border: 1px solid #e8eef6;
  border-radius: 999px;
  color: #475569;
  background: rgba(255, 255, 255, 0.9);
  font-weight: 700;
}

.quick-keywords button:hover {
  color: var(--brand-blue);
  border-color: #b9d7ff;
}

.safe-panel {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 14px;
  padding: 24px;
}

.panel-icon,
.notice-mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  color: #fff;
  font-weight: 900;
}

.panel-icon.success {
  background: #22c55e;
}

.safe-panel h2,
.side-panel h2 {
  margin: 0;
  color: #17212b;
  font-size: 20px;
}

.safe-panel ul {
  display: grid;
  gap: 11px;
  margin: 16px 0 20px;
  padding-left: 18px;
  color: #475569;
  line-height: 1.6;
}

.safe-panel button {
  width: 100%;
  height: 42px;
  border: 1px solid #d9f2e2;
  border-radius: 8px;
  color: #15803d;
  background: #f0fdf4;
  font-weight: 900;
}

.category-dock {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 22px;
  padding: 20px;
}

.category-tile {
  display: grid;
  justify-items: center;
  gap: 9px;
  min-height: 96px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #334155;
  background: transparent;
  font-weight: 800;
}

.category-tile:hover,
.category-tile.active {
  border-color: #d7e7ff;
  background: #f7fbff;
  transform: translateY(-1px);
}

.category-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  color: #fff;
  font-size: 18px;
  font-weight: 900;
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.13);
}

.category-tile.blue .category-icon { background: #3b82f6; }
.category-tile.green .category-icon { background: #22c55e; }
.category-tile.cyan .category-icon { background: #06b6d4; }
.category-tile.violet .category-icon { background: #8b5cf6; }
.category-tile.orange .category-icon { background: #f97316; }
.category-tile.amber .category-icon { background: #f59e0b; }
.category-tile.rose .category-icon { background: #ec4899; }

.market-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

.product-stream {
  min-width: 0;
}

.stream-head,
.filter-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.section-tabs {
  display: flex;
  gap: 28px;
  align-items: center;
}

.section-tabs button {
  position: relative;
  padding: 0 0 10px;
  color: #64748b;
  background: transparent;
  font-size: 22px;
  font-weight: 900;
}

.section-tabs button::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 4px;
  border-radius: 999px;
  background: transparent;
}

.section-tabs button.active {
  color: #111827;
}

.section-tabs button.active::after {
  background: var(--brand-blue);
}

.publish-link {
  height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  background: var(--brand-blue);
  font-weight: 900;
}

.filter-line {
  min-height: 50px;
  padding: 12px 14px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
}

.filter-line p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 800;
}

.sort-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: #253044;
  font-size: 14px;
}

.sort-bar button {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #c9d3e1;
  border-radius: 8px;
  color: #253044;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
}

.sort-bar button:hover {
  color: var(--brand-blue);
  border-color: var(--brand-blue);
  background: var(--soft-blue);
}

.sort-bar button.active {
  border-color: var(--brand-blue);
  color: #fff;
  background: var(--brand-blue);
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.2);
}

.sort-bar .reset-btn {
  color: #64748b;
  border-color: transparent;
  background: #f1f5f9;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.market-aside {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 16px;
}

.side-panel {
  padding: 20px;
}

.side-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}

.side-title button {
  padding: 0;
  color: #64748b;
  background: transparent;
  font-size: 13px;
  font-weight: 900;
}

.rank-panel ol {
  display: grid;
  gap: 13px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rank-panel li {
  display: grid;
  grid-template-columns: 26px 46px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.rank-thumb {
  display: grid;
  place-items: center;
  width: 46px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
  overflow: hidden;
}

.rank-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rank-thumb b {
  color: var(--brand-blue);
  font-size: 15px;
}

.rank-panel li > span {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  color: #fff;
  background: #f59e0b;
  font-size: 13px;
  font-weight: 900;
}

.rank-panel li:nth-child(n+4) > span {
  color: #64748b;
  background: #eef2f7;
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
  white-space: nowrap;
}

.rank-panel p,
.notice-panel p {
  margin: 0;
  color: #64748b;
}

.notice-panel {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 14px;
  border-color: #bfdbfe;
  background: linear-gradient(135deg, #f8fbff, #fff);
}

.notice-mark {
  color: var(--brand-blue);
  background: #dbeafe;
}

.notice-panel small {
  display: block;
  margin-top: 8px;
  color: #8191a5;
  line-height: 1.6;
}

.loading,
.empty-hint {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 56px 18px;
  color: #94a3b8;
  text-align: center;
}

.empty-hint strong {
  color: #17212b;
  font-size: 18px;
}

.empty-hint button {
  margin-top: 8px;
  background: var(--brand-blue);
  font-weight: 900;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.pagination button {
  padding: 7px 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: #334155;
  background: #fff;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.45;
  cursor: default;
}

@media (max-width: 1180px) {
  .market-hero-grid,
  .market-content {
    grid-template-columns: 1fr;
  }

  .market-aside {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .category-dock {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero-panel {
    padding: 24px 18px;
  }

  .hero-panel h1 {
    font-size: 34px;
  }

  .hero-search {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .hero-search button {
    grid-column: 1 / -1;
    width: auto;
    margin: 0 8px 8px;
  }

  .category-dock,
  .product-grid,
  .market-aside {
    grid-template-columns: 1fr;
  }

  .category-tile {
    grid-template-columns: 42px minmax(0, 1fr);
    justify-items: start;
    min-height: 62px;
    padding: 0 10px;
  }

  .category-icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
  }
}
</style>
