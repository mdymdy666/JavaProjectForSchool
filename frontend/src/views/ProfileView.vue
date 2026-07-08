<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createAddress,
  deleteAddress,
  getMyAddresses,
  getMyProducts,
  getMyProfile,
  setDefaultAddress,
  submitVerification,
  updateAddress,
  updateProfile
} from '../api/user'
import { uploadImage } from '../api/upload'
import type { AddressView, ProductCard, UserProfile } from '../types/domain'

type TabKey = 'profile' | 'addresses' | 'trust' | 'products'

const tabs: { key: TabKey; label: string }[] = [
  { key: 'profile', label: '资料编辑' },
  { key: 'addresses', label: '收货地址' },
  { key: 'trust', label: '信誉实名' },
  { key: 'products', label: '我的商品' }
]

const activeTab = ref<TabKey>('profile')
const profile = ref<UserProfile | null>(null)
const myProducts = ref<ProductCard[]>([])
const addresses = ref<AddressView[]>([])
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const success = ref('')

const edit = reactive({ nickname: '', phone: '', email: '', avatarUrl: '' })
const addressForm = reactive({ receiverName: '', receiverPhone: '', detailAddress: '', defaultAddress: false })
const editingAddressId = ref<number | null>(null)
const verificationForm = reactive({ realName: '', idCardNo: '' })
const avatarImageKey = ref(0)

const avatarText = computed(() => (profile.value?.nickname || profile.value?.username || '用').slice(0, 1))
const avatarPreviewUrl = computed(() => edit.avatarUrl || profile.value?.avatarUrl || '')
const trustText = computed(() => profile.value?.realNameStatus === 'VERIFIED' ? '已实名认证' : '未实名认证')
const maskedPhone = computed(() => {
  const phone = profile.value?.phone || ''
  return phone.length >= 7 ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : phone || '未填写'
})
const maskedIdCard = computed(() => {
  const id = profile.value?.idCardNo || ''
  return id.length < 8 ? id : `${id.slice(0, 4)}********${id.slice(-4)}`
})
const todoItems = computed(() => [
  { title: addresses.value.length ? '默认收货地址已配置' : '补充收货地址', detail: addresses.value.length ? addresses.value[0].detailAddress : '用于订单交易和物流演示', action: '去处理' },
  { title: profile.value?.realNameStatus === 'VERIFIED' ? '实名认证已完成' : '完成实名认证', detail: '实名状态会影响信誉分展示', action: '去查看' },
  { title: `已发布 ${myProducts.value.length} 件商品`, detail: '商品状态可在个人中心跟踪', action: '去管理' }
])

async function fetch() {
  loading.value = true
  error.value = ''
  try {
    const [pRes, prodRes, addrRes] = await Promise.all([getMyProfile(), getMyProducts(), getMyAddresses()])
    profile.value = pRes.data || null
    if (profile.value) {
      edit.nickname = profile.value.nickname || ''
      edit.phone = profile.value.phone || ''
      edit.email = profile.value.email || ''
      edit.avatarUrl = profile.value.avatarUrl || ''
      verificationForm.realName = profile.value.realName || ''
      verificationForm.idCardNo = profile.value.idCardNo || ''
    }
    const data = prodRes.data as unknown as { records?: ProductCard[] } | ProductCard[]
    myProducts.value = Array.isArray(data) ? data : (data?.records || [])
    addresses.value = addrRes.data || []
  } catch {
    error.value = '加载个人中心失败'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await updateProfile({
      nickname: edit.nickname,
      phone: edit.phone || undefined,
      email: edit.email || undefined,
      avatarUrl: edit.avatarUrl || undefined
    })
    if (res.code === 200) {
      profile.value = res.data || (profile.value ? {
        ...profile.value,
        nickname: edit.nickname,
        phone: edit.phone || null,
        email: edit.email || null,
        avatarUrl: edit.avatarUrl || null
      } : null)
      success.value = '资料已保存'
    } else {
      error.value = res.message || '保存失败'
    }
  } catch {
    error.value = '保存失败'
  } finally {
    saving.value = false
  }
}

async function onAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await uploadImage(file)
    if (res.code === 200 && res.data?.url) {
      edit.avatarUrl = res.data.url
      avatarImageKey.value += 1
      await saveProfile()
      success.value = '头像已更新'
    } else {
      error.value = res.message || '头像上传失败'
    }
  } catch {
    error.value = '头像上传失败'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function resetAddressForm() {
  editingAddressId.value = null
  addressForm.receiverName = ''
  addressForm.receiverPhone = ''
  addressForm.detailAddress = ''
  addressForm.defaultAddress = false
}

function startEditAddress(address: AddressView) {
  editingAddressId.value = address.id
  addressForm.receiverName = address.receiverName
  addressForm.receiverPhone = address.receiverPhone
  addressForm.detailAddress = address.detailAddress
  addressForm.defaultAddress = address.defaultAddress
}

async function saveAddress() {
  error.value = ''
  success.value = ''
  const payload = { ...addressForm }
  try {
    const res = editingAddressId.value ? await updateAddress(editingAddressId.value, payload) : await createAddress(payload)
    if (res.code === 200) {
      addresses.value = res.data || []
      resetAddressForm()
      success.value = '地址已保存'
    } else {
      error.value = res.message || '地址保存失败'
    }
  } catch {
    error.value = '地址保存失败'
  }
}

async function removeAddress(id: number) {
  try {
    const res = await deleteAddress(id)
    addresses.value = res.data || []
    success.value = '地址已删除'
  } catch {
    error.value = '删除地址失败'
  }
}

async function markDefault(id: number) {
  try {
    const res = await setDefaultAddress(id)
    addresses.value = res.data || []
    success.value = '默认地址已更新'
  } catch {
    error.value = '默认地址更新失败'
  }
}

async function verifyIdentity() {
  error.value = ''
  success.value = ''
  try {
    const res = await submitVerification({ realName: verificationForm.realName, idCardNo: verificationForm.idCardNo })
    if (res.code === 200) {
      profile.value = res.data
      success.value = '实名认证已通过'
    } else {
      error.value = res.message || '认证提交失败'
    }
  } catch {
    error.value = '认证提交失败'
  }
}

onMounted(fetch)
</script>

<template>
  <div class="profile-page" data-test="profile-workbench">
    <div v-if="loading" class="loading">加载中...</div>
    <template v-else-if="profile">
      <aside class="workbench-nav">
        <strong>工作台</strong>
        <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
          {{ tab.label }}
        </button>
      </aside>

      <main class="workbench-main">
        <header class="identity-strip">
          <div class="avatar">
            <img v-if="avatarPreviewUrl" :key="`${avatarPreviewUrl}-${avatarImageKey}`" :src="avatarPreviewUrl" :alt="profile.nickname" />
            <span v-else>{{ avatarText }}</span>
          </div>
          <div class="identity-copy">
            <div class="name-row">
              <h1>{{ profile.nickname }}</h1>
              <span :class="['verify-badge', { verified: profile.realNameStatus === 'VERIFIED' }]">{{ trustText }}</span>
            </div>
            <p>{{ profile.username }} · {{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</p>
            <small>手机 {{ maskedPhone }} · 邮箱 {{ profile.email || '未填写' }}</small>
          </div>
          <div class="score-card">
            <span>信誉分</span>
            <strong>{{ profile.creditScore }}</strong>
            <small>优秀</small>
          </div>
        </header>

        <section class="overview-grid">
          <div class="todo-card">
            <div class="panel-head"><h2>待处理事项</h2><span>{{ todoItems.length }}</span></div>
            <article v-for="item in todoItems" :key="item.title" class="todo-row">
              <div><strong>{{ item.title }}</strong><p>{{ item.detail }}</p></div>
              <button type="button">{{ item.action }}</button>
            </article>
          </div>
          <div class="stats-card">
            <div class="panel-head"><h2>交易统计</h2><span>近 30 天</span></div>
            <div class="stat-grid">
              <div><span>上架商品</span><strong>{{ myProducts.length }}</strong></div>
              <div><span>收货地址</span><strong>{{ addresses.length }}</strong></div>
              <div><span>信誉评分</span><strong>{{ profile.creditScore }}</strong></div>
              <div><span>实名状态</span><strong>{{ profile.realNameStatus === 'VERIFIED' ? '已认证' : '未认证' }}</strong></div>
            </div>
          </div>
        </section>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>

        <section v-if="activeTab === 'profile'" class="panel" data-test="profile-panel">
          <div class="panel-head">
            <h2>个人资料</h2>
            <label class="avatar-upload">
              {{ uploading ? '上传中...' : '上传头像' }}
              <input type="file" accept="image/*" @change="onAvatarChange" />
            </label>
          </div>
          <form class="form-grid" @submit.prevent="saveProfile">
            <label>昵称<input v-model="edit.nickname" required /></label>
            <label>手机号<input v-model="edit.phone" placeholder="用于买卖沟通和找回密码" /></label>
            <label>邮箱<input v-model="edit.email" placeholder="用于验证和通知" /></label>
            <label>头像地址<input v-model="edit.avatarUrl" placeholder="上传后自动填充，也可粘贴图片地址" /></label>
            <button type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存资料' }}</button>
          </form>
        </section>

        <section v-if="activeTab === 'addresses'" class="panel" data-test="address-panel">
          <div class="panel-head"><h2>收货地址</h2><span>{{ addresses.length }} 条</span></div>
          <form class="address-form" @submit.prevent="saveAddress">
            <input v-model="addressForm.receiverName" placeholder="收货人" required />
            <input v-model="addressForm.receiverPhone" placeholder="手机号" required />
            <input v-model="addressForm.detailAddress" placeholder="详细地址 / 校内自提点" required />
            <label class="check-line"><input v-model="addressForm.defaultAddress" type="checkbox" />设为默认</label>
            <button type="submit">{{ editingAddressId ? '更新地址' : '新增地址' }}</button>
            <button v-if="editingAddressId" type="button" class="ghost" @click="resetAddressForm">取消编辑</button>
          </form>
          <div v-if="addresses.length" class="address-list">
            <article v-for="address in addresses" :key="address.id" class="address-row">
              <div><strong>{{ address.receiverName }}</strong><span>{{ address.receiverPhone }}</span><p>{{ address.detailAddress }}</p></div>
              <div class="row-actions">
                <em v-if="address.defaultAddress">默认</em>
                <button v-else @click="markDefault(address.id)">设默认</button>
                <button @click="startEditAddress(address)">编辑</button>
                <button class="danger" @click="removeAddress(address.id)">删除</button>
              </div>
            </article>
          </div>
          <p v-else class="empty">暂无收货地址</p>
        </section>

        <section v-if="activeTab === 'trust'" class="panel" data-test="trust-panel">
          <div class="trust-layout">
            <div class="score-box"><span>信誉评分</span><strong>{{ profile.creditScore }}</strong><p>{{ trustText }}</p></div>
            <div class="verify-box">
              <h2>实名认证</h2>
              <p v-if="profile.realNameStatus === 'VERIFIED'">{{ profile.realName }} · {{ maskedIdCard }}</p>
              <form @submit.prevent="verifyIdentity">
                <input v-model="verificationForm.realName" placeholder="真实姓名" required />
                <input v-model="verificationForm.idCardNo" placeholder="身份证号" required />
                <button type="submit">{{ profile.realNameStatus === 'VERIFIED' ? '更新认证' : '提交认证' }}</button>
              </form>
            </div>
          </div>
        </section>

        <section v-if="activeTab === 'products'" class="panel" data-test="products-panel">
          <div class="panel-head"><h2>我的商品</h2><span>{{ myProducts.length }} 件</span></div>
          <div v-if="myProducts.length" class="product-list">
            <article v-for="p in myProducts" :key="p.id" class="product-row"><span>{{ p.title }}</span><small>{{ p.status }}</small></article>
          </div>
          <p v-else class="empty">暂无发布的商品</p>
        </section>
      </main>
    </template>
  </div>
</template>

<style scoped>
.profile-page {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 22px;
  max-width: 1320px;
  margin: 0 auto;
}
.loading, .empty { color: #64748b; text-align: center; padding: 28px; }
.workbench-nav, .identity-strip, .todo-card, .stats-card, .panel {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}
.workbench-nav {
  display: grid;
  gap: 8px;
  align-content: start;
  padding: 14px;
  position: sticky;
  top: 90px;
}
.workbench-nav strong { padding: 8px 10px; font-size: 18px; }
.workbench-nav button {
  height: 42px;
  border-radius: 8px;
  background: transparent;
  color: #334155;
  text-align: left;
  font-weight: 800;
}
.workbench-nav button.active {
  background: #e8f7f5;
  color: var(--brand-teal);
}
.workbench-main { display: grid; gap: 16px; min-width: 0; }
.identity-strip {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 160px;
  gap: 20px;
  align-items: center;
  padding: 22px;
}
.avatar {
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand-blue), var(--brand-teal));
  color: #fff;
  font-size: 34px;
  font-weight: 900;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.name-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.name-row h1 { margin: 0; font-size: 28px; }
.identity-copy p { margin: 6px 0; color: #64748b; }
.identity-copy small { color: #8191a5; }
.verify-badge {
  border: 1px solid #fed7aa;
  border-radius: 999px;
  padding: 5px 10px;
  color: #c2410c;
  background: #fff7ed;
  font-size: 12px;
  font-weight: 900;
}
.verify-badge.verified {
  color: #15803d;
  background: #f0fdf4;
  border-color: #86efac;
}
.score-card {
  display: grid;
  justify-items: center;
  gap: 4px;
  padding-left: 20px;
  border-left: 1px solid var(--line);
}
.score-card span, .score-card small { color: #64748b; }
.score-card strong { color: var(--brand-teal); font-size: 40px; line-height: 1; }
.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 16px;
}
.todo-card, .stats-card, .panel { padding: 18px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 16px; }
.panel-head h2 { margin: 0; font-size: 20px; }
.panel-head span { color: #64748b; }
.todo-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 90px;
  gap: 12px;
  align-items: center;
  padding: 13px 0;
  border-top: 1px solid #edf2f7;
}
.todo-row strong { color: #17212b; }
.todo-row p { margin: 4px 0 0; color: #64748b; }
.todo-row button, .form-grid button, .address-form button, .verify-box button, .avatar-upload, .row-actions button {
  height: 38px;
  border: 1px solid var(--brand-teal);
  border-radius: 8px;
  background: #fff;
  color: var(--brand-teal);
  cursor: pointer;
  font-weight: 900;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border: 1px solid #edf2f7;
  border-radius: 10px;
  overflow: hidden;
}
.stat-grid div { display: grid; gap: 8px; padding: 18px; border-right: 1px solid #edf2f7; border-bottom: 1px solid #edf2f7; }
.stat-grid div:nth-child(2n) { border-right: 0; }
.stat-grid div:nth-last-child(-n + 2) { border-bottom: 0; }
.stat-grid span { color: #64748b; }
.stat-grid strong { font-size: 24px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.form-grid label { color: #334155; font-size: 14px; font-weight: 800; }
.form-grid input, .address-form input, .verify-box input {
  width: 100%;
  height: 40px;
  margin-top: 7px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  box-sizing: border-box;
}
.form-grid input:focus, .address-form input:focus, .verify-box input:focus {
  outline: none;
  border-color: var(--brand-teal);
  box-shadow: 0 0 0 3px rgba(15, 159, 143, 0.12);
}
.form-grid button { align-self: end; background: var(--brand-teal); color: #fff; }
.avatar-upload {
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  position: relative;
}
.avatar-upload input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.address-form { display: grid; grid-template-columns: 140px 160px 1fr auto auto auto; gap: 10px; align-items: center; margin-bottom: 18px; }
.address-form input { margin-top: 0; }
.check-line { display: flex; align-items: center; gap: 6px; color: #475569; white-space: nowrap; }
.check-line input { width: 16px; height: 16px; margin: 0; }
.address-form button[type="submit"], .verify-box button { background: var(--brand-teal); color: #fff; }
.address-form .ghost { background: #fff; color: #334155; border-color: #cbd5e1; }
.address-list, .product-list { display: grid; gap: 10px; }
.address-row, .product-row { display: flex; justify-content: space-between; gap: 16px; border: 1px solid #e5e7eb; border-radius: 10px; padding: 14px; }
.address-row strong { margin-right: 12px; color: #0f172a; }
.address-row span, .address-row p, .product-row small { color: #64748b; }
.address-row p { margin: 7px 0 0; }
.row-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; justify-content: flex-end; }
.row-actions em { font-style: normal; color: #16a34a; background: #f0fdf4; border: 1px solid #86efac; border-radius: 999px; padding: 6px 10px; }
.row-actions button { padding: 0 12px; }
.row-actions button.danger { border-color: #fecaca; color: #dc2626; }
.trust-layout { display: grid; grid-template-columns: 260px 1fr; gap: 20px; }
.score-box { border: 1px solid #ccfbf1; border-radius: 12px; background: #f0fdfa; padding: 22px; }
.score-box span { color: var(--brand-teal); font-weight: 900; }
.score-box strong { display: block; margin: 8px 0; color: var(--brand-teal); font-size: 56px; line-height: 1; }
.score-box p, .verify-box p { margin: 0; color: #475569; }
.verify-box h2 { margin: 0 0 10px; }
.verify-box form { display: grid; grid-template-columns: 1fr 1fr auto; gap: 10px; margin-top: 18px; }
.verify-box input { margin-top: 0; }
.error, .success { border-radius: 8px; padding: 10px 12px; margin: 0; font-size: 14px; }
.error { color: #b91c1c; background: #fef2f2; border: 1px solid #fecaca; }
.success { color: #166534; background: #f0fdf4; border: 1px solid #bbf7d0; }
@media (max-width: 980px) {
  .profile-page, .overview-grid, .identity-strip, .trust-layout, .verify-box form { grid-template-columns: 1fr; }
  .workbench-nav { position: static; display: flex; overflow-x: auto; }
  .score-card { justify-items: start; border-left: 0; padding-left: 0; }
  .address-form, .form-grid { grid-template-columns: 1fr; }
  .address-row, .product-row { flex-direction: column; }
  .row-actions { justify-content: flex-start; }
}
</style>
