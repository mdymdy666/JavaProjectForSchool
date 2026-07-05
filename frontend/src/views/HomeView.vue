<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchProducts, type ProductQuery } from '../api/product'
import ProductCard from '../components/ProductCard.vue'
import type { Announcement, ProductCard as ProductCardType } from '../types/domain'
import { apiGet } from '../api/http'

const router = useRouter()

const hotProducts = ref<ProductCardType[]>([])
const newProducts = ref<ProductCardType[]>([])
const announcements = ref<Announcement[]>([])

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
  } catch { /* ignore */ }
})

function goProduct(id: number) { router.push(`/products/${id}`) }
function goList(sort: string) { router.push({ path: '/products', query: { sort } }) }
</script>

<template>
  <div class="home-page">
    <section v-if="announcements.length" class="announcements">
      <div v-for="a in announcements" :key="a.id" class="announce-item">
        <strong>公告</strong> {{ a.title }}
      </div>
    </section>

    <section class="hero-section">
      <h2>校园二手，让闲置流动起来</h2>
      <p>发布你的闲置物品，或者淘到物美价廉的好东西</p>
      <div class="hero-actions">
        <RouterLink to="/products" class="btn-primary">浏览商品</RouterLink>
        <RouterLink to="/publish" class="btn-secondary">发布商品</RouterLink>
      </div>
    </section>

    <section class="product-section">
      <div class="section-head">
        <h3>热门商品</h3>
        <button @click="goList('hot')">查看更多</button>
      </div>
      <div v-if="hotProducts.length" class="product-grid">
        <ProductCard
          v-for="p in hotProducts" :key="p.id"
          :product="p" @select="goProduct"
        />
      </div>
      <p v-else class="empty-hint">暂无热门商品</p>
    </section>

    <section class="product-section">
      <div class="section-head">
        <h3>最新发布</h3>
        <button @click="goList('newest')">查看更多</button>
      </div>
      <div v-if="newProducts.length" class="product-grid">
        <ProductCard
          v-for="p in newProducts" :key="p.id"
          :product="p" @select="goProduct"
        />
      </div>
      <p v-else class="empty-hint">暂无商品</p>
    </section>
  </div>
</template>

<style scoped>
.home-page { max-width: 960px; margin: 0 auto; }
.announcements { margin-bottom: 16px; }
.announce-item {
  padding: 8px 16px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 14px;
}
.announce-item strong { margin-right: 8px; color: #d46b08; }
.hero-section {
  text-align: center;
  padding: 48px 0 32px;
}
.hero-section h2 { font-size: 28px; margin: 0 0 8px; }
.hero-section p { color: #666; margin: 0 0 20px; }
.hero-actions { display: flex; gap: 12px; justify-content: center; }
.btn-primary, .btn-secondary {
  padding: 10px 24px; border-radius: 8px; font-size: 15px; text-decoration: none;
}
.btn-primary { background: #1677ff; color: #fff; }
.btn-secondary { background: #fff; color: #1677ff; border: 1px solid #1677ff; }
.product-section { margin-bottom: 40px; }
.section-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.section-head h3 { font-size: 20px; margin: 0; }
.section-head button { background: none; border: none; color: #1677ff; cursor: pointer; font-size: 14px; }
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
@media (max-width: 768px) {
  .product-grid { grid-template-columns: repeat(2, 1fr); }
}
.empty-hint { color: #999; text-align: center; padding: 32px 0; }
</style>
