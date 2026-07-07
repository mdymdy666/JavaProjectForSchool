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
  <div class="product-list-page">
    <div class="filters">
      <div class="search-bar">
        <input
          v-model="keyword" type="text" placeholder="关键词搜索..."
          @keyup.enter="search"
        />
        <button @click="search">搜索</button>
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
      <ProductCard
        v-for="p in products" :key="p.id"
        :product="p" @select="goDetail"
      />
    </div>
    <p v-else class="empty-hint">暂无商品</p>

    <div v-if="total > size" class="pagination">
      <button :disabled="page <= 1" @click="page--">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
      <button :disabled="page >= Math.ceil(total / size)" @click="page++">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.product-list-page { max-width: 960px; margin: 0 auto; }
.filters { margin-bottom: 16px; }
.search-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.search-bar input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
}
.search-bar button {
  padding: 8px 20px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.sort-bar { display: flex; gap: 8px; align-items: center; font-size: 14px; color: #253044; }
.sort-bar button {
  background: #fff;
  color: #253044;
  border: 1px solid #c9d3e1;
  padding: 5px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.sort-bar button:hover {
  color: #1677ff;
  border-color: #1677ff;
  background: #eef6ff;
}
.sort-bar button.active { background: #1677ff; color: #fff; border-color: #1677ff; box-shadow: 0 2px 6px rgba(22, 119, 255, 0.24); }
.result-info { color: #999; font-size: 13px; margin-bottom: 12px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 768px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
.loading { text-align: center; padding: 48px; color: #999; }
.empty-hint { color: #999; text-align: center; padding: 48px 0; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; }
.pagination button { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; }
.pagination button:disabled { opacity: 0.4; cursor: default; }
</style>
