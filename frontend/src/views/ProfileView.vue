<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getMyProfile, updateProfile } from '../api/user'
import { getMyProducts } from '../api/user'
import type { UserProfile, ProductCard } from '../types/domain'

const profile = ref<UserProfile | null>(null)
const myProducts = ref<ProductCard[]>([])
const loading = ref(true)
const edit = reactive({ nickname: '', phone: '', email: '' })
const editing = ref(false)
const error = ref('')
const saved = ref(false)

async function fetch() {
  loading.value = true
  try {
    const [pRes, prodRes] = await Promise.all([
      getMyProfile(), getMyProducts()
    ])
    profile.value = pRes.data || null
    if (profile.value) {
      edit.nickname = profile.value.nickname || ''
      edit.phone = profile.value.phone || ''
      edit.email = profile.value.email || ''
    }
    const data = prodRes.data as unknown as { records?: ProductCard[] } | ProductCard[]
    myProducts.value = Array.isArray(data) ? data : (data?.records || [])
  } finally {
    loading.value = false
  }
}

async function save() {
  error.value = ''
  saved.value = false
  try {
    const res = await updateProfile({
      nickname: edit.nickname,
      phone: edit.phone || undefined,
      email: edit.email || undefined
    })
    if (res.code === 200) {
      profile.value = res.data || profile.value
      editing.value = false
      saved.value = true
    } else {
      error.value = res.message || '保存失败'
    }
  } catch (e: unknown) {
    error.value = '保存失败'
  }
}

onMounted(fetch)
</script>

<template>
  <div class="profile-page">
    <div v-if="loading" class="loading">加载中...</div>
    <template v-else-if="profile">
      <h2>个人中心</h2>
      <div class="profile-card">
        <div class="info-row"><span>用户名</span><strong>{{ profile.username }}</strong></div>
        <div class="info-row" v-if="!editing"><span>昵称</span><strong>{{ profile.nickname }}</strong></div>
        <div class="info-row"><span>角色</span><strong>{{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</strong></div>
        <div class="info-row" v-if="!editing && profile.phone"><span>手机</span><strong>{{ profile.phone }}</strong></div>
        <div class="info-row" v-if="!editing && profile.email"><span>邮箱</span><strong>{{ profile.email }}</strong></div>
      </div>

      <div v-if="editing" class="edit-form">
        <label>昵称 <input v-model="edit.nickname" /></label>
        <label>手机 <input v-model="edit.phone" placeholder="选填" /></label>
        <label>邮箱 <input v-model="edit.email" placeholder="选填" /></label>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="saved" class="success">已保存</p>
        <div class="form-actions">
          <button @click="save">保存</button>
          <button class="ghost" @click="editing = false">取消</button>
        </div>
      </div>

      <div class="section">
        <button v-if="!editing" @click="editing = true">编辑资料</button>
      </div>

      <h3>我的商品</h3>
      <div v-if="myProducts.length" class="my-products">
        <div v-for="p in myProducts" :key="p.id" class="product-row">
          <span>{{ p.title }}</span>
          <small>{{ p.status }}</small>
        </div>
      </div>
      <p v-else class="empty">暂无发布的商品</p>
    </template>
  </div>
</template>

<style scoped>
.profile-page { max-width: 640px; margin: 0 auto; }
.profile-page h2 { margin: 0 0 16px; }
.loading, .empty { color: #999; text-align: center; padding: 32px; }
.profile-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px;
  padding: 20px; margin-bottom: 16px;
}
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.info-row:last-child { border-bottom: none; }
.info-row span { color: #888; }
.edit-form { background: #fafafa; padding: 16px; border-radius: 8px; margin-bottom: 16px; }
.edit-form label { display: block; margin-bottom: 10px; font-size: 14px; }
.edit-form input { display: block; width: 100%; margin-top: 4px; padding: 6px 10px; border: 1px solid #d9d9d9; border-radius: 4px; box-sizing: border-box; }
.form-actions { display: flex; gap: 8px; margin-top: 12px; }
.form-actions button { padding: 6px 16px; border: none; border-radius: 4px; cursor: pointer; background: #1677ff; color: #fff; }
.form-actions button.ghost { background: #fff; color: #333; border: 1px solid #d9d9d9; }
.error { color: #ff4d4f; font-size: 13px; }
.success { color: #52c41a; font-size: 13px; }
.section { margin-bottom: 24px; }
.section button { padding: 6px 16px; border: 1px solid #1677ff; border-radius: 4px; background: #fff; color: #1677ff; cursor: pointer; }
h3 { font-size: 18px; margin: 24px 0 12px; }
.my-products { background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; }
.product-row { display: flex; justify-content: space-between; padding: 10px 16px; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.product-row:last-child { border-bottom: none; }
.product-row small { color: #999; }
</style>
