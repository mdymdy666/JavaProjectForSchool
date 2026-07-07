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
      <img v-if="product.coverUrl" :src="product.coverUrl" :alt="product.title" />
      <div v-else class="img-placeholder">暂无图片</div>
      <span :class="['status-tag', statusTag(product.status)]">{{ statusLabel(product.status) }}</span>
    </div>
    <div class="card-body">
      <h3>{{ product.title }}</h3>
      <div class="card-meta">
        <span>{{ product.categoryName }}</span>
        <span>{{ product.itemCondition }}</span>
      </div>
      <div class="seller-line">
        <span class="seller-avatar">{{ product.sellerNickname.slice(0, 1) }}</span>
        <span>{{ product.sellerNickname }}</span>
        <small>{{ product.viewCount }} 浏览</small>
      </div>
      <div class="card-footer">
        <strong class="price">&yen;{{ product.price.toFixed(2) }}</strong>
        <span>同校自提</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  border: 1px solid #e1e8f0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  background: #fff;
}
.product-card:hover {
  transform: translateY(-2px);
  border-color: #bfd8ff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.1);
}
.card-image {
  position: relative;
  height: 184px;
  background: #eef3f8;
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
  color: #94a3b8;
  font-size: 14px;
}
.status-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.16);
}
.status-tag.success { background: #22c55e; }
.status-tag.warning { background: #f59e0b; }
.status-tag.danger { background: #ef4444; }
.status-tag.info { background: var(--brand-blue); }
.card-body {
  display: grid;
  gap: 8px;
  padding: 13px 14px 14px;
}
.card-body h3 {
  min-height: 22px;
  margin: 0;
  overflow: hidden;
  color: #17212b;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta {
  display: flex;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
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
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e8f3ff;
  color: var(--brand-blue);
  font-size: 12px;
  font-weight: 900;
}
.seller-line small {
  margin-left: auto;
  color: #94a3b8;
  white-space: nowrap;
}
.card-footer {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 8px;
}
.price {
  color: #ef4444;
  font-size: 21px;
  line-height: 1;
}
.card-footer span {
  color: #64748b;
  font-size: 12px;
}
</style>
