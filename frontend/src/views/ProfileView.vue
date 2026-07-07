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
const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  detailAddress: '',
  defaultAddress: false
})
const editingAddressId = ref<number | null>(null)
const verificationForm = reactive({ realName: '', idCardNo: '' })

const avatarText = computed(() => (profile.value?.nickname || profile.value?.username || '用').slice(0, 1))
const trustText = computed(() => profile.value?.realNameStatus === 'VERIFIED' ? '已实名认证' : '未实名认证')
const maskedIdCard = computed(() => {
  const id = profile.value?.idCardNo || ''
  if (id.length < 8) return id
  return `${id.slice(0, 4)}********${id.slice(-4)}`
})

async function fetch() {
  loading.value = true
  error.value = ''
  try {
    const [pRes, prodRes, addrRes] = await Promise.all([
      getMyProfile(),
      getMyProducts(),
      getMyAddresses()
    ])
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
      profile.value = res.data
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
  const payload = {
    receiverName: addressForm.receiverName,
    receiverPhone: addressForm.receiverPhone,
    detailAddress: addressForm.detailAddress,
    defaultAddress: addressForm.defaultAddress
  }
  try {
    const res = editingAddressId.value
      ? await updateAddress(editingAddressId.value, payload)
      : await createAddress(payload)
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
  error.value = ''
  success.value = ''
  try {
    const res = await deleteAddress(id)
    addresses.value = res.data || []
    success.value = '地址已删除'
  } catch {
    error.value = '删除地址失败'
  }
}

async function markDefault(id: number) {
  error.value = ''
  success.value = ''
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
    const res = await submitVerification({
      realName: verificationForm.realName,
      idCardNo: verificationForm.idCardNo
    })
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
  <div class="profile-page">
    <div v-if="loading" class="loading">加载中...</div>
    <template v-else-if="profile">
      <header class="profile-header">
        <div class="avatar">
          <img v-if="profile.avatarUrl" :src="profile.avatarUrl" :alt="profile.nickname" />
          <span v-else>{{ avatarText }}</span>
        </div>
        <div class="profile-title">
          <h2>{{ profile.nickname }}</h2>
          <p>{{ profile.username }} · {{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</p>
        </div>
        <div class="trust-pill" :class="{ verified: profile.realNameStatus === 'VERIFIED' }">
          {{ trustText }} · {{ profile.creditScore }} 分
        </div>
      </header>

      <nav class="tabs" aria-label="个人中心模块">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </nav>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>

      <section v-if="activeTab === 'profile'" class="panel" data-test="profile-panel">
        <div class="panel-head">
          <h3>个人资料</h3>
          <label class="avatar-upload">
            {{ uploading ? '上传中...' : '上传头像' }}
            <input type="file" accept="image/*" @change="onAvatarChange" />
          </label>
        </div>
        <form class="form-grid" @submit.prevent="saveProfile">
          <label>
            昵称
            <input v-model="edit.nickname" required />
          </label>
          <label>
            手机号
            <input v-model="edit.phone" placeholder="用于买卖沟通和找回密码" />
          </label>
          <label>
            邮箱
            <input v-model="edit.email" placeholder="用于验证和通知" />
          </label>
          <label>
            头像地址
            <input v-model="edit.avatarUrl" placeholder="上传后自动填充，也可粘贴图片地址" />
          </label>
          <button type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存资料' }}</button>
        </form>
      </section>

      <section v-if="activeTab === 'addresses'" class="panel" data-test="address-panel">
        <div class="panel-head">
          <h3>收货地址</h3>
          <span>{{ addresses.length }} 条</span>
        </div>
        <form class="address-form" @submit.prevent="saveAddress">
          <input v-model="addressForm.receiverName" placeholder="收货人" required />
          <input v-model="addressForm.receiverPhone" placeholder="手机号" required />
          <input v-model="addressForm.detailAddress" placeholder="详细地址 / 校内自提点" required />
          <label class="check-line">
            <input v-model="addressForm.defaultAddress" type="checkbox" />
            设为默认
          </label>
          <button type="submit">{{ editingAddressId ? '更新地址' : '新增地址' }}</button>
          <button v-if="editingAddressId" type="button" class="ghost" @click="resetAddressForm">取消编辑</button>
        </form>
        <div v-if="addresses.length" class="address-list">
          <article v-for="address in addresses" :key="address.id" class="address-row">
            <div>
              <strong>{{ address.receiverName }}</strong>
              <span>{{ address.receiverPhone }}</span>
              <p>{{ address.detailAddress }}</p>
            </div>
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
          <div class="score-box">
            <span>信誉评分</span>
            <strong>{{ profile.creditScore }}</strong>
            <p>{{ trustText }}</p>
          </div>
          <div class="verify-box">
            <h3>实名认证</h3>
            <p v-if="profile.realNameStatus === 'VERIFIED'">
              {{ profile.realName }} · {{ maskedIdCard }}
            </p>
            <form @submit.prevent="verifyIdentity">
              <input v-model="verificationForm.realName" placeholder="真实姓名" required />
              <input v-model="verificationForm.idCardNo" placeholder="身份证号" required />
              <button type="submit">{{ profile.realNameStatus === 'VERIFIED' ? '更新认证' : '提交认证' }}</button>
            </form>
          </div>
        </div>
      </section>

      <section v-if="activeTab === 'products'" class="panel" data-test="products-panel">
        <div class="panel-head">
          <h3>我的商品</h3>
          <span>{{ myProducts.length }} 件</span>
        </div>
        <div v-if="myProducts.length" class="product-list">
          <article v-for="p in myProducts" :key="p.id" class="product-row">
            <span>{{ p.title }}</span>
            <small>{{ p.status }}</small>
          </article>
        </div>
        <p v-else class="empty">暂无发布的商品</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1040px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.loading, .empty {
  color: #64748b;
  text-align: center;
  padding: 28px;
}
.profile-header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 18px;
  align-items: center;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 22px;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}
.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  display: grid;
  place-items: center;
  overflow: hidden;
  font-size: 30px;
  font-weight: 800;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.profile-title h2 { margin: 0 0 6px; font-size: 26px; }
.profile-title p { margin: 0; color: #64748b; }
.trust-pill {
  border: 1px solid #facc15;
  color: #a16207;
  background: #fefce8;
  border-radius: 999px;
  padding: 8px 14px;
  font-weight: 700;
}
.trust-pill.verified {
  border-color: #86efac;
  color: #15803d;
  background: #f0fdf4;
}
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0;
}
.tabs button {
  height: 38px;
  padding: 0 18px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  font-weight: 700;
}
.tabs button.active {
  background: #1677ff;
  border-color: #1677ff;
  color: #fff;
}
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 22px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
}
.panel-head h3 { margin: 0; font-size: 20px; }
.panel-head span { color: #64748b; }
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.form-grid label {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}
.form-grid input, .address-form input, .verify-box input {
  width: 100%;
  height: 40px;
  margin-top: 7px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  box-sizing: border-box;
  font-size: 14px;
}
.form-grid input:focus, .address-form input:focus, .verify-box input:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}
.form-grid button, .address-form button, .verify-box button, .avatar-upload, .row-actions button {
  height: 40px;
  border: 1px solid #1677ff;
  border-radius: 8px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
  font-weight: 700;
}
.form-grid button {
  align-self: end;
}
.avatar-upload {
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  position: relative;
}
.avatar-upload input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.address-form {
  display: grid;
  grid-template-columns: 140px 160px 1fr auto auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 18px;
}
.address-form input { margin-top: 0; }
.check-line {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  white-space: nowrap;
}
.check-line input {
  width: 16px;
  height: 16px;
  margin: 0;
}
.address-form .ghost {
  background: #fff;
  color: #334155;
  border-color: #cbd5e1;
}
.address-list, .product-list {
  display: grid;
  gap: 10px;
}
.address-row, .product-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
}
.address-row strong { margin-right: 12px; color: #0f172a; }
.address-row span, .address-row p, .product-row small { color: #64748b; }
.address-row p { margin: 7px 0 0; }
.row-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.row-actions em {
  font-style: normal;
  color: #16a34a;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 999px;
  padding: 6px 10px;
}
.row-actions button {
  background: #fff;
  color: #1677ff;
  padding: 0 12px;
}
.row-actions button.danger {
  border-color: #fecaca;
  color: #dc2626;
}
.trust-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
}
.score-box {
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #eff6ff;
  padding: 22px;
}
.score-box span { color: #1d4ed8; font-weight: 700; }
.score-box strong {
  display: block;
  margin: 8px 0;
  font-size: 54px;
  color: #1677ff;
  line-height: 1;
}
.score-box p, .verify-box p { margin: 0; color: #475569; }
.verify-box h3 { margin: 0 0 10px; }
.verify-box form {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 10px;
  margin-top: 18px;
}
.verify-box input { margin-top: 0; }
.error, .success {
  border-radius: 8px;
  padding: 10px 12px;
  margin: 0 0 14px;
  font-size: 14px;
}
.error { color: #b91c1c; background: #fef2f2; border: 1px solid #fecaca; }
.success { color: #166534; background: #f0fdf4; border: 1px solid #bbf7d0; }
@media (max-width: 760px) {
  .profile-header {
    grid-template-columns: auto 1fr;
  }
  .trust-pill {
    grid-column: 1 / -1;
    width: fit-content;
  }
  .form-grid, .trust-layout, .verify-box form {
    grid-template-columns: 1fr;
  }
  .address-form {
    grid-template-columns: 1fr;
  }
  .address-row, .product-row {
    flex-direction: column;
  }
  .row-actions {
    justify-content: flex-start;
  }
}
</style>
