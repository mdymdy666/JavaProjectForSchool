<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet } from '../api/http'
import type { Announcement } from '../types/domain'

const router = useRouter()
const announcements = ref<Announcement[]>([])
const loading = ref(false)

function formatTime(value: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await apiGet<Announcement[]>('/announcements')
    announcements.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="announcement-page">
    <section class="announcement-hero">
      <button type="button" @click="router.back()">返回</button>
      <p>校园通知中心</p>
      <h1>平台公告</h1>
      <span>集中查看平台规则、活动安排和交易提醒。</span>
    </section>

    <section class="announcement-list">
      <div class="list-head">
        <h2>全部公告</h2>
        <small>{{ announcements.length }} 条</small>
      </div>

      <div v-if="loading" class="state-panel">加载中...</div>
      <div v-else-if="announcements.length" class="notice-stack">
        <article v-for="item in announcements" :key="item.id" class="notice-card">
          <div class="notice-meta">
            <span>公告</span>
            <time>{{ formatTime(item.createdAt) }}</time>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.content }}</p>
        </article>
      </div>
      <div v-else class="state-panel">暂无公告</div>
    </section>
  </main>
</template>

<style scoped>
.announcement-page {
  display: grid;
  gap: 18px;
  max-width: 1040px;
  margin: 0 auto;
}

.announcement-hero,
.announcement-list {
  border: 1px solid rgba(219, 228, 238, 0.92);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.07);
}

.announcement-hero {
  position: relative;
  overflow: hidden;
  padding: 34px 38px;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(255, 255, 255, 0.96) 58%),
    linear-gradient(90deg, rgba(22, 119, 255, 0.12), rgba(34, 197, 94, 0.1));
}

.announcement-hero button {
  height: 34px;
  padding: 0 13px;
  border: 1px solid #cfe1ff;
  border-radius: 8px;
  color: var(--brand-blue);
  background: #fff;
  font-weight: 900;
}

.announcement-hero p {
  margin: 22px 0 8px;
  color: var(--brand-blue);
  font-size: 14px;
  font-weight: 900;
}

.announcement-hero h1 {
  margin: 0;
  color: #132033;
  font-size: 40px;
  line-height: 1.14;
}

.announcement-hero span {
  display: block;
  margin-top: 12px;
  color: #526174;
  font-size: 16px;
}

.announcement-list {
  padding: 24px;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.list-head h2 {
  margin: 0;
  color: #132033;
  font-size: 24px;
}

.list-head small {
  color: #64748b;
  font-weight: 800;
}

.notice-stack {
  display: grid;
  gap: 14px;
}

.notice-card {
  display: grid;
  gap: 10px;
  padding: 20px;
  border: 1px solid #e2eaf4;
  border-radius: 8px;
  background: #fff;
}

.notice-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.notice-meta span {
  padding: 4px 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 12px;
  font-weight: 900;
}

.notice-meta time {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 800;
}

.notice-card h3 {
  margin: 0;
  color: #17212b;
  font-size: 20px;
}

.notice-card p {
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.state-panel {
  display: grid;
  place-items: center;
  min-height: 180px;
  color: #94a3b8;
}

@media (max-width: 640px) {
  .announcement-hero,
  .announcement-list {
    padding: 20px;
  }

  .announcement-hero h1 {
    font-size: 32px;
  }
}
</style>
