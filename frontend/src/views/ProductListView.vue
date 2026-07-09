<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchProducts } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
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
const activeFeed = ref<'recommended' | 'newest'>(sort.value === 'newest' ? 'newest' : 'recommended')
const size = 12

const categories: Array<{ label: string; categoryId: number | null; tone: string; icon: string }> = [
  { label: '全部分类', categoryId: null, tone: 'blue', icon: '全' },
  { label: '图书教材', categoryId: 2, tone: 'green', icon: '书' },
  { label: '数码配件', categoryId: 1, tone: 'cyan', icon: '数' },
  { label: '生活用品', categoryId: 3, tone: 'violet', icon: '生' },
  { label: '运动户外', categoryId: 4, tone: 'orange', icon: '动' },
  { label: '服饰鞋包', categoryId: 5, tone: 'amber', icon: '包' },
  { label: '美妆护肤', categoryId: 6, tone: 'rose', icon: '美' }
]
const hotKeywords = ['自行车', '考研资料', '台灯', '键盘', '教材', '耳机']

const visibleProducts = computed(() => products.value)
const selectedCategory = computed(() => categories.find(item => item.categoryId === activeCategoryId.value) || categories[0])
const resultLabel = computed(() => {
  const category = selectedCategory.value.categoryId === null ? '全校' : selectedCategory.value.label
  return `${category} · ${total.value} 件商品`
})
const activeChips = computed(() => {
  const chips: string[] = []
  if (keyword.value.trim()) chips.push(`关键词：${keyword.value.trim()}`)
  if (activeCategoryId.value !== null) chips.push(`分类：${selectedCategory.value.label}`)
  chips.push(sortLabel(sort.value))
  return chips
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
  activeCategoryId.value = null
  page.value = 1
  search()
}

function selectFeed(feed: 'recommended' | 'newest') {
  activeFeed.value = feed
  sort.value = feed === 'newest' ? 'newest' : 'hot'
  page.value = 1
}

function selectSort(nextSort: string) {
  sort.value = nextSort
  activeFeed.value = nextSort === 'newest' ? 'newest' : 'recommended'
  page.value = 1
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
  activeFeed.value = 'newest'
  page.value = 1
  fetch()
}

function sortLabel(value: string) {
  const labels: Record<string, string> = {
    newest: '排序：最新',
    hot: '排序：热度',
    priceAsc: '排序：价格从低到高',
    priceDesc: '排序：价格从高到低'
  }
  return labels[value] || '排序：默认'
}

onMounted(fetch)
watch([sort, page, activeCategoryId], fetch)
</script>

<template>
  <div class="product-list-page" data-test="market-shell">
    <section class="market-console" aria-label="商品市场检索">
      <div class="console-copy">
        <span>商品市场</span>
        <h1>快速找到想要的校园闲置</h1>
        <p>按关键词、分类、热度和价格筛选完整商品库。</p>
      </div>

      <div class="search-rail">
        <div class="market-search">
          <span aria-hidden="true">⌕</span>
          <input v-model="keyword" type="text" placeholder="搜索商品、分类或关键词" @keyup.enter="search" />
          <button type="button" @click="search">搜索</button>
        </div>
        <button class="publish-link" type="button" @click="router.push('/publish')">发布闲置</button>
      </div>

      <div class="quick-keywords" aria-label="热门搜索">
        <span>热门搜索</span>
        <button v-for="word in hotKeywords" :key="word" type="button" @click="useKeyword(word)">
          {{ word }}
        </button>
      </div>

      <div class="console-metrics" aria-label="市场概览">
        <div class="metric-card">
          <span>当前结果</span>
          <strong>{{ total }}</strong>
          <small>件在看商品</small>
        </div>
        <div class="metric-card">
          <span>当前分类</span>
          <strong>{{ selectedCategory.label }}</strong>
          <small>可随时切换</small>
        </div>
        <div class="metric-card">
          <span>浏览方式</span>
          <strong>{{ sort === 'newest' ? '最新' : sort === 'hot' ? '热度' : '价格' }}</strong>
          <small>支持排序</small>
        </div>
      </div>
    </section>

    <section class="catalog-layout" aria-label="商品目录">
      <aside class="filter-sidebar">
        <div class="filter-card">
          <div class="filter-title">
            <h2>分类筛选</h2>
            <button type="button" @click="resetFilters">重置</button>
          </div>
          <button
            v-for="category in categories"
            :key="category.label"
            type="button"
            :class="['category-filter', category.tone, { active: activeCategoryId === category.categoryId }]"
            :aria-pressed="activeCategoryId === category.categoryId"
            @click="selectCategory(category.categoryId)"
          >
            <span aria-hidden="true">{{ category.icon }}</span>
            {{ category.label }}
          </button>
        </div>

        <div class="filter-card compact-card">
          <h2>交易提醒</h2>
          <p>优先当面交易，贵重物品现场验收，保留聊天记录和交易凭证。</p>
          <button type="button" @click="router.push('/safety')">安全告示</button>
        </div>
      </aside>

      <main class="product-stream">
        <div class="result-toolbar">
          <div class="stream-head">
            <div class="section-tabs">
              <button :class="{ active: activeFeed === 'recommended' }" type="button" @click="selectFeed('recommended')">
                推荐结果
              </button>
              <button :class="{ active: activeFeed === 'newest' }" type="button" @click="selectFeed('newest')">
                最新发布
              </button>
            </div>
            <p>{{ resultLabel }}</p>
          </div>

          <div class="filter-line">
            <div class="active-chips" aria-label="当前筛选">
              <span v-for="chip in activeChips" :key="chip">{{ chip }}</span>
            </div>
            <div class="sort-bar">
              <span>排序</span>
              <button :class="{ active: sort === 'newest' }" type="button" @click="selectSort('newest')">最新</button>
              <button :class="{ active: sort === 'hot' }" type="button" @click="selectSort('hot')">热度</button>
              <button :class="{ active: sort === 'priceAsc' }" type="button" @click="selectSort('priceAsc')">价格↑</button>
              <button :class="{ active: sort === 'priceDesc' }" type="button" @click="selectSort('priceDesc')">价格↓</button>
              <button class="reset-btn" type="button" @click="resetFilters">重置</button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="visibleProducts.length" class="product-grid">
          <ProductCard v-for="p in visibleProducts" :key="p.id" :product="p" @select="goDetail" />
        </div>
        <div v-else class="empty-hint">
          <strong>没有找到匹配商品</strong>
          <span>换个关键词、分类或排序方式再试试。</span>
          <button type="button" @click="router.push('/publish')">发布闲置</button>
        </div>

        <div v-if="total > size" class="pagination">
          <button :disabled="page <= 1" type="button" @click="page--">上一页</button>
          <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
          <button :disabled="page >= Math.ceil(total / size)" type="button" @click="page++">下一页</button>
        </div>
      </main>
    </section>
  </div>
</template>

<style scoped>
.product-list-page {
  max-width: 1360px;
  margin: 0 auto;
}

.market-console,
.filter-card,
.product-stream {
  border: 1px solid rgba(219, 228, 238, 0.95);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}

.market-console {
  position: relative;
  display: grid;
  grid-template-columns: minmax(240px, 0.9fr) minmax(360px, 1.5fr);
  gap: 16px 22px;
  align-items: center;
  margin-bottom: 16px;
  overflow: hidden;
  padding: 22px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.94)),
    radial-gradient(circle at 94% 14%, rgba(22, 119, 255, 0.16), transparent 32%);
}

.market-console::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  background: linear-gradient(180deg, var(--brand-blue), #22c55e);
  content: "";
}

.market-console::after {
  position: absolute;
  right: -72px;
  bottom: -86px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(22, 119, 255, 0.08);
  content: "";
  pointer-events: none;
}

.console-copy span {
  display: inline-flex;
  width: fit-content;
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eaf3ff;
  font-size: 13px;
  font-weight: 900;
}

.console-copy h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
  line-height: 1.2;
}

.console-copy p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.search-rail {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.market-search {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 92px;
  align-items: center;
  min-height: 48px;
  border: 1px solid #9ec8ff;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 24px rgba(22, 119, 255, 0.12);
}

.market-search span {
  display: grid;
  place-items: center;
  color: #1e3a8a;
  font-size: 22px;
}

.market-search input {
  width: 100%;
  border: 0;
  color: #17212b;
  background: transparent;
  font-size: 15px;
}

.market-search input:focus {
  outline: none;
}

.market-search button,
.publish-link {
  height: 38px;
  border-radius: 7px;
  background: var(--brand-blue);
  font-weight: 900;
}

.market-search button {
  margin-right: 5px;
}

.publish-link {
  padding: 0 15px;
}

.quick-keywords {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding-top: 2px;
}

.console-metrics {
  position: relative;
  z-index: 1;
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  display: grid;
  gap: 5px;
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid rgba(207, 225, 255, 0.86);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.07);
}

.metric-card span,
.metric-card small {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.metric-card strong {
  overflow: hidden;
  color: #102033;
  font-size: 20px;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-keywords span {
  color: #64748b;
  font-size: 13px;
  font-weight: 900;
}

.quick-keywords button {
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid #e2eaf4;
  border-radius: 999px;
  color: #334155;
  background: #fff;
  font-size: 13px;
  font-weight: 800;
}

.quick-keywords button:hover {
  color: #fff;
  border-color: var(--brand-blue);
  background: var(--brand-blue);
}

.catalog-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.filter-sidebar {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 14px;
}

.filter-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
}

.filter-card::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 4px;
  background: linear-gradient(90deg, #1677ff, #38bdf8, #22c55e);
  content: "";
}

.filter-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.filter-card h2 {
  margin: 0;
  color: #17212b;
  font-size: 17px;
  line-height: 1.2;
}

.filter-title button,
.compact-card button {
  padding: 0;
  color: var(--brand-blue);
  background: transparent;
  font-size: 13px;
  font-weight: 900;
}

.category-filter {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 44px;
  margin-top: 8px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #334155;
  background: transparent;
  font-weight: 900;
  text-align: left;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.category-filter span {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
}

.category-filter:hover,
.category-filter.active {
  border-color: #cfe1ff;
  color: #0f4ba8;
  background: #f7fbff;
  transform: translateX(2px);
}

.category-filter.blue span { background: #3b82f6; }
.category-filter.green span { background: #22c55e; }
.category-filter.cyan span { background: #06b6d4; }
.category-filter.violet span { background: #8b5cf6; }
.category-filter.orange span { background: #f97316; }
.category-filter.amber span { background: #f59e0b; }
.category-filter.rose span { background: #ec4899; }

.compact-card {
  color: #64748b;
  line-height: 1.6;
}

.compact-card p {
  margin: 10px 0 12px;
  font-size: 13px;
}

.product-stream {
  min-width: 0;
  padding: 0;
  overflow: hidden;
}

.result-toolbar {
  padding: 16px;
  border-bottom: 1px solid #e5edf6;
  background:
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.stream-head,
.filter-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stream-head {
  margin-bottom: 12px;
}

.stream-head p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 900;
}

.section-tabs {
  display: flex;
  gap: 22px;
  align-items: center;
}

.section-tabs button {
  position: relative;
  padding: 0 0 8px;
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
  color: #111827;
}

.section-tabs button.active::after {
  background: var(--brand-blue);
}

.filter-line {
  min-height: 48px;
  margin-bottom: 0;
  padding: 10px 12px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
}

.active-chips,
.sort-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.active-chips span {
  min-height: 28px;
  padding: 5px 10px;
  border-radius: 999px;
  color: #475569;
  background: #fff;
  font-size: 12px;
  font-weight: 800;
}

.sort-bar {
  color: #253044;
  font-size: 14px;
}

.sort-bar > span {
  font-weight: 900;
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
  background: #eef2f7;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  padding: 18px;
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

@media (max-width: 980px) {
  .market-console,
  .catalog-layout {
    grid-template-columns: 1fr;
  }

  .filter-sidebar {
    position: static;
  }

  .filter-card:first-child {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 8px;
  }

  .filter-title {
    grid-column: 1 / -1;
  }

  .category-filter {
    margin-top: 0;
  }
}

@media (max-width: 760px) {
  .market-console {
    padding: 16px;
  }

  .search-rail,
  .market-search,
  .product-grid {
    grid-template-columns: 1fr;
  }

  .market-search {
    gap: 8px;
    padding: 10px;
  }

  .market-search span {
    display: none;
  }

  .market-search button {
    width: 100%;
    margin: 0;
  }

  .filter-card:first-child {
    grid-template-columns: 1fr;
  }
}
</style>
