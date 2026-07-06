<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publishProduct } from '../api/product'
import { uploadImage } from '../api/upload'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

if (!auth.isLoggedIn) { router.push('/login') }

const form = reactive({
  title: '',
  categoryId: 1,
  price: 0,
  itemCondition: '九成新',
  description: '',
  imageUrls: [''] as string[]
})
const loading = ref(false)
const uploading = ref(false)
const error = ref('')

function addImageUrl() {
  if (form.imageUrls.length < 6) form.imageUrls.push('')
}
function removeImageUrl(index: number) {
  if (form.imageUrls.length > 1) form.imageUrls.splice(index, 1)
}

async function handleFileUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  uploading.value = true
  error.value = ''
  try {
    for (const file of files) {
      if (form.imageUrls.filter(u => u.trim()).length >= 6) break
      const res = await uploadImage(file)
      if (res.code === 200 && res.data) {
        // Fill first empty slot or append
        const emptyIdx = form.imageUrls.findIndex(u => !u.trim())
        if (emptyIdx >= 0) {
          form.imageUrls[emptyIdx] = res.data.url
        } else if (form.imageUrls.length < 6) {
          form.imageUrls.push(res.data.url)
        }
      }
    }
  } catch {
    error.value = '图片上传失败'
  } finally {
    uploading.value = false
    input.value = '' // reset so same file can be re-selected
  }
}

async function submit() {
  if (!form.title.trim()) { error.value = '请输入商品名称'; return }
  if (form.price <= 0) { error.value = '请输入有效价格'; return }
  if (!form.description.trim()) { error.value = '请输入商品描述'; return }
  const validUrls = form.imageUrls.filter(u => u.trim())
  if (validUrls.length === 0) { error.value = '请至少上传一张图片或填写图片URL'; return }
  loading.value = true
  error.value = ''
  try {
    const res = await publishProduct({
      title: form.title.trim(),
      categoryId: form.categoryId,
      price: form.price,
      itemCondition: form.itemCondition,
      description: form.description.trim(),
      imageUrls: validUrls
    })
    if (res.code === 200) {
      router.push(`/products/${res.data?.id}`)
    } else {
      error.value = res.message || '发布失败'
    }
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg || '发布失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="publish-page">
    <h2>发布商品</h2>
    <form class="publish-form" @submit.prevent="submit">
      <label>
        商品名称
        <input v-model="form.title" type="text" placeholder="商品名称" required maxlength="100" />
      </label>
      <label>
        分类
        <select v-model="form.categoryId">
          <option :value="1">数码配件</option>
          <option :value="2">图书教材</option>
          <option :value="3">生活用品</option>
          <option :value="4">运动户外</option>
          <option :value="5">服饰鞋包</option>
        </select>
      </label>
      <label>
        价格 (元)
        <input v-model.number="form.price" type="number" min="0.01" step="0.01" required />
      </label>
      <label>
        成色
        <select v-model="form.itemCondition">
          <option>全新</option>
          <option>九成新</option>
          <option>八成新</option>
          <option>七成新</option>
          <option>微瑕</option>
        </select>
      </label>
      <label>
        描述
        <textarea v-model="form.description" rows="5" placeholder="详细描述商品状况、使用情况..." maxlength="1000" required />
      </label>

      <div class="image-section">
        <label>商品图片（至少1张，最多6张）</label>
        <div class="upload-row">
          <input
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            @change="handleFileUpload"
            class="file-input"
            ref="fileInput"
          />
          <span v-if="uploading" class="uploading-hint">上传中...</span>
          <span v-else class="upload-hint">支持 JPG/PNG/GIF/WebP，单文件 ≤ 5MB</span>
        </div>
        <div v-for="(_, index) in form.imageUrls" :key="index" class="image-url-row">
          <input
            v-model="form.imageUrls[index]"
            type="text"
            :placeholder="`图片链接 ${index + 1}（或点击上方按钮上传）`"
          />
          <button type="button" class="remove-btn" @click="removeImageUrl(index)" :disabled="form.imageUrls.length <= 1">×</button>
        </div>
        <button type="button" class="add-btn" @click="addImageUrl" :disabled="form.imageUrls.length >= 6">
          + 添加图片URL
        </button>
      </div>

      <p v-if="error" class="error-msg">{{ error }}</p>
      <button type="submit" :disabled="loading || uploading">{{ loading ? '发布中...' : '发布商品' }}</button>
    </form>
  </div>
</template>

<style scoped>
.publish-page { max-width: 560px; margin: 0 auto; }
.publish-page h2 { margin: 0 0 24px; font-size: 22px; }
.publish-form label {
  display: block; margin-bottom: 16px; font-size: 14px; color: #333;
}
.publish-form input, .publish-form select, .publish-form textarea {
  display: block; width: 100%; margin-top: 4px; padding: 8px 12px;
  border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box;
}
.publish-form textarea { resize: vertical; }
.image-section { margin-bottom: 16px; }
.image-section > label { margin-bottom: 8px; font-weight: 500; }
.upload-row { margin-bottom: 8px; display: flex; align-items: center; gap: 12px; }
.file-input { flex: 0; font-size: 13px; }
.upload-hint, .uploading-hint { font-size: 12px; color: #999; }
.uploading-hint { color: #1677ff; }
.image-url-row { display: flex; gap: 6px; margin-bottom: 6px; }
.image-url-row input { flex: 1; }
.remove-btn {
  width: 32px; padding: 0; border: 1px solid #d9d9d9; border-radius: 4px;
  background: #fff; color: #999; font-size: 18px; cursor: pointer;
}
.remove-btn:disabled { opacity: 0.3; cursor: default; }
.add-btn {
  padding: 4px 12px; border: 1px dashed #1677ff; border-radius: 4px;
  background: #fff; color: #1677ff; cursor: pointer; font-size: 13px;
}
.add-btn:disabled { opacity: 0.3; cursor: default; }
.publish-form button[type="submit"] {
  width: 100%; padding: 10px; background: #1677ff; color: #fff;
  border: none; border-radius: 6px; font-size: 15px; cursor: pointer;
}
.publish-form button[type="submit"]:disabled { opacity: 0.6; }
.error-msg { color: #ff4d4f; font-size: 13px; margin: 0 0 12px; }
</style>
