<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchProducts } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
import type { Announcement, ProductCard as ProductCardType } from '../types/domain'
import { apiGet } from '../api/http'

const router = useRouter()

const hotProducts = ref<ProductCardType[]>([])
const newProducts = ref<ProductCardType[]>([])
const announcements = ref<Announcement[]>([])
const keyword = ref('')

const categories = ['图书教材', '数码电子', '生活用品', '运动户外', '家具家电', '美妆个护', '全部分类']
const hotWords = ['考研资料', '机械键盘', '台灯', '羽毛球拍', '书桌', '显卡']

onMounted(async () => {
  try {
    const [hot, latest, annRes] = await Promise.all([
      searchProducts({ sort: 'hot', size: 4 }),
      searchProducts({ sort: 'newest', size: 4 }),
      apiGet<Announcement[]>('/announcements')
    ])
    hotProducts.value = hot.data?.records || []
    newProducts.value = latest.data?.records || []
    announcements.value = annRes.data || []
  } catch { /* demo page keeps quiet when offline */ }
})

function goProduct(id: number) { router.push(`/products/${id}`) }
function goList(sort: string) { router.push({ path: '/products', query: { sort } }) }
function search() {
  router.push({ path: '/products', query: keyword.value ? { keyword: keyword.value } : {} })
}
</script>

<template>
  <div class="home-page">
    <section class="market-hero">
      <div class="hero-copy">
        <p class="eyebrow">同校交易 · 当面验货 · 闲置流动</p>
        <h1>校园二手，找得到也卖得快</h1>
        <p class="summary">发布闲置物品，浏览同校好物，消息沟通到订单流转都能在平台内完成。</p>
        <div class="search-box">
          <input v-model="keyword" placeholder="搜索商品、分类或关键词（如：教材、键盘、台灯）" @keyup.enter="search" />
          <button @click="search">搜索</button>
        </div>
        <div class="hot-words">
          <span>热门搜索：</span>
          <button v-for="word in hotWords" :key="word" @click="router.push({ path: '/products', query: { keyword: word } })">
            {{ word }}
          </button>
        </div>
      </div>
      <div class="publish-panel">
        <strong>一键发布闲置</strong>
        <p>图片、分类、价格、描述一次填完，发布后进入审核流程。</p>
        <RouterLink to="/publish">发布商品</RouterLink>
      </div>
    </section>

    <section class="category-strip" aria-label="商品分类">
      <button v-for="category in categories" :key="category" @click="router.push('/products')">
        <span>{{ category.slice(0, 1) }}</span>
        {{ category }}
      </button>
    </section>

    <section v-if="announcements.length" class="announcements">
      <div v-for="a in announcements" :key="a.id" class="announce-item">
        <strong>公告</strong>
        <span>{{ a.title }}</span>
      </div>
    </section>

    <div class="home-layout">
      <main>
        <section class="product-section">
          <div class="section-head">
            <div>
              <h2>推荐好物</h2>
              <p>浏览量高、交易意向强的同校闲置</p>
            </div>
            <button @click="goList('hot')">查看更多</button>
          </div>
          <div v-if="hotProducts.length" class="product-grid">
            <ProductCard v-for="p in hotProducts" :key="p.id" :product="p" @select="goProduct" />
          </div>
          <p v-else class="empty-hint">暂无热门商品</p>
        </section>

        <section class="product-section">
          <div class="section-head">
            <div>
              <h2>最新发布</h2>
              <p>刚刚上架的商品更容易捡漏</p>
            </div>
            <button @click="goList('newest')">查看更多</button>
          </div>
          <div v-if="newProducts.length" class="product-grid">
            <ProductCard v-for="p in newProducts" :key="p.id" :product="p" @select="goProduct" />
          </div>
          <p v-else class="empty-hint">暂无商品</p>
        </section>
      </main>

      <aside class="side-panels">
        <section class="safe-panel">
          <h3>交易安全提示</h3>
          <ul>
            <li>建议同校自提，当面验货交易</li>
            <li>不要脱离平台沟通，谨防诈骗</li>
            <li>遇到问题可在消息中心联系客服</li>
          </ul>
        </section>
        <section class="rank-panel">
          <h3>热门榜</h3>
          <ol>
            <li v-for="(item, index) in hotProducts.slice(0, 5)" :key="item.id">
              <span>{{ index + 1 }}</span>
              <button @click="goProduct(item.id)">{{ item.title }}</button>
            </li>
          </ol>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1320px;
  margin: 0 auto;
}
.market-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 24px;
  align-items: stretch;
  margin-bottom: 18px;
}
.hero-copy, .publish-panel, .category-strip, .announcements, .product-section, .safe-panel, .rank-panel {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}
.hero-copy {
  padding: 30px;
}
.eyebrow {
  margin: 0 0 8px;
  color: var(--brand-blue);
  font-weight: 900;
}
h1 {
  max-width: 760px;
  margin: 0;
  font-size: 38px;
  line-height: 1.18;
}
.summary {
  margin: 10px 0 22px;
  color: var(--muted);
  font-size: 16px;
}
.search-box {
  display: flex;
  max-width: 860px;
  height: 54px;
  overflow: hidden;
  border: 1px solid #9ec5ff;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(22, 119, 255, 0.08);
}
.search-box input {
  flex: 1;
  border: 0;
  padding: 0 18px;
  font-size: 15px;
}
.search-box input:focus { outline: none; }
.search-box button {
  width: 118px;
  border-radius: 0;
  background: var(--brand-blue);
  font-weight: 900;
}
.hot-words {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
  color: #64748b;
  font-size: 13px;
}
.hot-words button {
  padding: 0;
  color: #41546a;
  background: transparent;
  font-size: 13px;
}
.publish-panel {
  display: grid;
  gap: 10px;
  align-content: center;
  padding: 24px;
}
.publish-panel strong {
  color: #17212b;
  font-size: 20px;
}
.publish-panel p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}
.publish-panel a {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  height: 42px;
  border-radius: 8px;
  background: var(--brand-blue);
  color: #fff;
  text-decoration: none;
  font-weight: 900;
}
.category-strip {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  padding: 16px;
  margin-bottom: 16px;
}
.category-strip button {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 12px 8px;
  border-radius: 10px;
  background: transparent;
  color: #334155;
  font-weight: 800;
}
.category-strip button:hover { background: var(--soft-blue); color: var(--brand-blue); }
.category-strip span {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #eef6ff;
  color: var(--brand-blue);
}
.announcements {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px;
  background: #fffaf0;
  border-color: #fed7aa;
}
.announce-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 9px 12px;
  color: #334155;
}
.announce-item strong { color: #d97706; }
.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  gap: 18px;
}
.product-section {
  padding: 18px;
  margin-bottom: 18px;
}
.section-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}
.section-head h2 { margin: 0; font-size: 22px; }
.section-head p { margin: 5px 0 0; color: var(--muted); }
.section-head button {
  background: transparent;
  color: var(--brand-blue);
  font-weight: 900;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.side-panels {
  display: grid;
  gap: 16px;
  align-content: start;
}
.safe-panel, .rank-panel { padding: 18px; }
.safe-panel h3, .rank-panel h3 { margin: 0 0 12px; font-size: 18px; }
.safe-panel ul, .rank-panel ol {
  display: grid;
  gap: 11px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.safe-panel li {
  color: #475569;
  line-height: 1.55;
}
.rank-panel li {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}
.rank-panel span {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: #ffedd5;
  color: #f97316;
  font-weight: 900;
}
.rank-panel button {
  overflow: hidden;
  padding: 0;
  color: #334155;
  background: transparent;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty-hint {
  color: #94a3b8;
  text-align: center;
  padding: 30px 0;
}
@media (max-width: 1080px) {
  .market-hero, .home-layout { grid-template-columns: 1fr; }
  .category-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .hero-copy { padding: 22px; }
  h1 { font-size: 30px; }
  .search-box { height: auto; display: grid; }
  .search-box button { width: 100%; height: 44px; }
  .category-strip, .product-grid { grid-template-columns: 1fr; }
}
</style>
