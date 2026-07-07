<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchProducts } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
import type { ProductCard as ProductCardType } from '../types/domain'

const route = useRoute()
const router = useRouter()

const products = ref<ProductCardType[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const keyword = ref((route.query.keyword as string) || '')
const sort = ref((route.query.sort as string) || 'newest')
const size = 12
const categories = ['推荐', '图书教材', '数码配件', '生活用品', '运动户外', '家具家电', '美妆个护']

async function fetch() {
  loading.value = true
  try {
    const res = await searchProducts({
      keyword: keyword.value || undefined,
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

function goDetail(id: number) { router.push(`/products/${id}`) }
function search() { page.value = 1; fetch() }

onMounted(fetch)
watch([keyword, sort, page], fetch)
</script>

<template>
  <div class="product-list-page" data-test="market-shell">
    <section class="market-top">
      <div>
        <p class="eyebrow">校园市集</p>
        <h1>找同校好物，沟通后当面交易</h1>
      </div>
      <div class="search-bar">
        <input v-model="keyword" type="text" placeholder="搜索商品、卖家、分类" @keyup.enter="search" />
        <button @click="search">搜索</button>
      </div>
    </section>

    <div class="market-layout">
      <main class="market-main">
        <div class="filters">
          <div class="category-row" aria-label="分类">
            <button v-for="category in categories" :key="category">{{ category }}</button>
          </div>
          <div class="sort-bar">
            <span>排序：</span>
            <button :class="{ active: sort === 'newest' }" @click="sort = 'newest'">最新</button>
            <button :class="{ active: sort === 'hot' }" @click="sort = 'hot'">热度</button>
            <button :class="{ active: sort === 'priceAsc' }" @click="sort = 'priceAsc'">价格↑</button>
            <button :class="{ active: sort === 'priceDesc' }" @click="sort = 'priceDesc'">价格↓</button>
          </div>
        </div>

        <p class="result-info">共 {{ total }} 件商品</p>

        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="products.length" class="product-grid">
          <ProductCard v-for="p in products" :key="p.id" :product="p" @select="goDetail" />
        </div>
        <p v-else class="empty-hint">暂无商品</p>

        <div v-if="total > size" class="pagination">
          <button :disabled="page <= 1" @click="page--">上一页</button>
          <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
          <button :disabled="page >= Math.ceil(total / size)" @click="page++">下一页</button>
        </div>
      </main>

      <aside class="market-aside">
        <section class="side-card">
          <h3>交易安全提示</h3>
          <p>建议同校自提，当面验货后再确认付款。</p>
          <ul>
            <li>优先选择实名认证卖家</li>
            <li>贵重物品约在公共区域交易</li>
            <li>聊天和订单留在平台内</li>
          </ul>
        </section>
        <section class="side-card">
          <h3>热门榜</h3>
          <ol>
            <li v-for="(item, index) in products.slice(0, 5)" :key="item.id">
              <span>{{ index + 1 }}</span>
              <button @click="goDetail(item.id)">{{ item.title }}</button>
            </li>
          </ol>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.product-list-page {
  max-width: 1320px;
  margin: 0 auto;
}
.market-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 560px);
  gap: 18px;
  align-items: end;
  margin-bottom: 16px;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}
.eyebrow {
  margin: 0 0 7px;
  color: var(--brand-blue);
  font-weight: 900;
}
.market-top h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.25;
}
.search-bar {
  display: flex;
  height: 48px;
  overflow: hidden;
  border: 1px solid #9ec5ff;
  border-radius: 10px;
  background: #fff;
}
.search-bar input {
  flex: 1;
  border: 0;
  padding: 0 15px;
  font-size: 14px;
}
.search-bar input:focus { outline: none; }
.search-bar button {
  width: 98px;
  border-radius: 0;
  background: var(--brand-blue);
  font-weight: 900;
}
.market-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}
.market-main, .market-aside .side-card {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}
.market-main {
  padding: 18px;
}
.filters {
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
}
.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.category-row button {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #d7e2ef;
  border-radius: 999px;
  background: #fff;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}
.category-row button:first-child,
.category-row button:hover {
  border-color: #b9d7ff;
  background: var(--soft-blue);
  color: var(--brand-blue);
}
.sort-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #253044;
  font-size: 14px;
}
.sort-bar button {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #c9d3e1;
  border-radius: 7px;
  background: #fff;
  color: #253044;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.sort-bar button:hover {
  color: var(--brand-blue);
  border-color: var(--brand-blue);
  background: var(--soft-blue);
}
.sort-bar button.active {
  background: var(--brand-blue);
  color: #fff;
  border-color: var(--brand-blue);
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.22);
}
.result-info {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 12px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.market-aside {
  display: grid;
  gap: 16px;
}
.side-card {
  padding: 18px;
}
.side-card h3 {
  margin: 0 0 10px;
  font-size: 18px;
}
.side-card p {
  color: #64748b;
  line-height: 1.6;
}
.side-card ul, .side-card ol {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.side-card li {
  color: #475569;
  line-height: 1.55;
}
.side-card ol li {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}
.side-card ol span {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: #fff7ed;
  color: #f97316;
  font-weight: 900;
}
.side-card ol button {
  overflow: hidden;
  padding: 0;
  color: #334155;
  background: transparent;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.loading {
  text-align: center;
  padding: 48px;
  color: #94a3b8;
}
.empty-hint {
  color: #94a3b8;
  text-align: center;
  padding: 48px 0;
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
  background: #fff;
  color: #334155;
  cursor: pointer;
}
.pagination button:disabled {
  opacity: 0.45;
  cursor: default;
}
@media (max-width: 1080px) {
  .market-top, .market-layout { grid-template-columns: 1fr; }
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .market-top { padding: 18px; }
  .search-bar { display: grid; height: auto; }
  .search-bar button { width: 100%; height: 42px; }
  .product-grid { grid-template-columns: 1fr; }
}
</style>
