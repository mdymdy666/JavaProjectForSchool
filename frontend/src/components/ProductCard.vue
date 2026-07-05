<script setup lang="ts">
import type { ProductCard } from '../types/domain'

defineProps<{ product: ProductCard }>()

defineEmits<{ select: [id: number] }>()

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
</script>

<template>
  <article class="product-card" data-test="product-card" @click="$emit('select', product.id)">
    <div class="card-image">
      <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.title" />
      <div v-else class="img-placeholder">暂无图片</div>
      <span :class="['status-tag', statusTag(product.status)]">{{ statusLabel(product.status) }}</span>
    </div>
    <div class="card-body">
      <h3>{{ product.title }}</h3>
      <div class="card-meta">
        <span>{{ product.categoryName }}</span>
        <span>{{ product.itemCondition }}</span>
      </div>
      <div class="card-footer">
        <strong class="price">&yen;{{ product.price.toFixed(2) }}</strong>
        <small>{{ product.viewCount }} 浏览 · {{ product.sellerNickname }}</small>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
  background: #fff;
}
.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.card-image {
  position: relative;
  height: 180px;
  background: #f5f5f5;
}
.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 14px;
}
.status-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}
.status-tag.success { background: #52c41a; }
.status-tag.warning { background: #faad14; }
.status-tag.danger { background: #ff4d4f; }
.status-tag.info { background: #1677ff; }
.card-body {
  padding: 12px;
}
.card-body h3 {
  font-size: 15px;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #888;
  margin-bottom: 8px;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.price {
  color: #ff4d4f;
  font-size: 18px;
}
.card-footer small {
  color: #999;
  font-size: 12px;
}
</style>
