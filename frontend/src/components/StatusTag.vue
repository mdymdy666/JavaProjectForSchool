<script setup lang="ts">
defineProps<{ status: string }>()

const labels: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '在售',
  REJECTED: '已驳回',
  OFF_SHELF: '已下架',
  SOLD: '已售出',
  DELETED: '已删除',
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELED: '已取消',
  UNREAD: '未读',
  READ: '已读'
}

function tagClass(status: string) {
  if (['APPROVED', 'COMPLETED', 'READ'].includes(status)) return 'success'
  if (['PENDING', 'PENDING_PAYMENT', 'PAID', 'SHIPPED', 'UNREAD'].includes(status)) return 'warning'
  if (['REJECTED', 'CANCELED', 'DELETED'].includes(status)) return 'danger'
  return 'info'
}
</script>

<template>
  <span :class="['status-tag', tagClass(status)]">{{ labels[status] || status }}</span>
</template>

<style scoped>
.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.status-tag.success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.status-tag.warning { background: #fffbe6; color: #d48806; border: 1px solid #ffe58f; }
.status-tag.danger { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.status-tag.info { background: #e6f7ff; color: #1677ff; border: 1px solid #91d5ff; }
</style>
