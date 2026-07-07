<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publishProduct } from '../api/product'
import { uploadImage } from '../api/upload'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

if (!auth.isLoggedIn) { router.push('/login') }

const categoryOptions = [
  { id: 1, name: '数码配件' },
  { id: 2, name: '图书教材' },
  { id: 3, name: '生活用品' },
  { id: 4, name: '运动户外' },
  { id: 5, name: '服饰鞋包' }
]

const conditionOptions = ['全新', '九成新', '八成新', '七成新', '微瑕']

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

const validImageUrls = computed(() => form.imageUrls.map(url => url.trim()).filter(Boolean))

function addImageUrl() {
  if (form.imageUrls.length < 6) form.imageUrls.push('')
}
function removeImageUrl(index: number) {
  if (form.imageUrls.length > 1) form.imageUrls.splice(index, 1)
  else form.imageUrls[index] = ''
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
    input.value = ''
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
    <header class="publish-hero">
      <div>
        <p class="eyebrow">校园闲置发布</p>
        <h2>发布商品</h2>
      </div>
    </header>

    <main class="publish-workspace" data-test="publish-workspace">
      <form class="publish-form" @submit.prevent="submit">
        <section class="form-section">
          <div class="section-head">
            <strong>基础信息</strong>
          </div>

          <label class="field field-wide">
            <span class="field-label">商品名称</span>
            <input v-model="form.title" type="text" placeholder="例如：Cherry 机械键盘 MX3.0S" required maxlength="100" />
          </label>

          <div class="field-grid">
            <label class="field">
              <span class="field-label">分类</span>
              <select v-model.number="form.categoryId">
                <option v-for="category in categoryOptions" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </label>
            <label class="field">
              <span class="field-label">成色</span>
              <select v-model="form.itemCondition">
                <option v-for="condition in conditionOptions" :key="condition">{{ condition }}</option>
              </select>
            </label>
          </div>

          <label class="field price-field">
            <span class="field-label">价格</span>
            <span class="price-input">
              <span>¥</span>
              <input v-model.number="form.price" type="number" min="0.01" step="0.01" required />
            </span>
          </label>
        </section>

        <section class="form-section">
          <div class="section-head">
            <strong>商品描述</strong>
            <span>{{ form.description.length }}/1000</span>
          </div>
          <label class="field field-wide">
            <span class="field-label">描述详情</span>
            <textarea
              v-model="form.description"
              rows="7"
              placeholder="说明商品状况、入手时间、配件是否齐全、期望交易地点等"
              maxlength="1000"
              required
            />
          </label>
        </section>

        <section class="form-section image-section" data-test="image-uploader">
          <div class="section-head">
            <strong>上传商品图片</strong>
            <span>{{ validImageUrls.length }}/6</span>
          </div>

          <label class="upload-drop">
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              multiple
              @change="handleFileUpload"
            />
            <span class="upload-mark">＋</span>
            <span>{{ uploading ? '上传中...' : '选择图片' }}</span>
            <small>支持 JPG、PNG、GIF、WebP，最多 6 张</small>
          </label>

          <div class="image-url-list">
            <div v-for="(_, index) in form.imageUrls" :key="index" class="image-url-row">
              <input
                v-model="form.imageUrls[index]"
                type="text"
                :placeholder="`图片链接 ${index + 1}`"
              />
              <button type="button" class="remove-btn" @click="removeImageUrl(index)" :disabled="form.imageUrls.length <= 1 && !form.imageUrls[index]">
                移除
              </button>
            </div>
          </div>

          <button type="button" class="add-btn" @click="addImageUrl" :disabled="form.imageUrls.length >= 6">
            添加图片链接
          </button>
        </section>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <div class="action-bar" data-test="action-bar">
          <button type="button" class="cancel-btn" @click="router.push('/products')">取消</button>
          <button type="submit" class="submit-btn" :disabled="loading || uploading">
            {{ loading ? '发布中...' : '发布商品' }}
          </button>
        </div>
      </form>
    </main>
  </div>
</template>

<style scoped>
.publish-page { max-width: 1040px; margin: 0 auto; }
.publish-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 18px;
}
.publish-hero h2 { margin: 4px 0 6px; color: #17212b; font-size: 28px; letter-spacing: 0; }
.eyebrow { color: #1677ff !important; font-weight: 700; font-size: 13px; }
.publish-workspace {
  max-width: 760px;
}
.publish-form {
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}
.publish-form { overflow: hidden; }
.form-section {
  padding: 20px;
  border-bottom: 1px solid #edf0f3;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.section-head strong { color: #17212b; font-size: 16px; }
.section-head span { color: #8792a0; font-size: 12px; }
.field { display: block; margin-bottom: 14px; }
.field:last-child { margin-bottom: 0; }
.field-label {
  display: block;
  margin-bottom: 6px;
  color: #253044;
  font-size: 13px;
  font-weight: 600;
}
.field input, .field select, .field textarea, .image-url-row input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cfd8e3;
  border-radius: 6px;
  background: #fff;
  color: #253044;
  font-size: 14px;
  outline: none;
}
.field input, .field select, .image-url-row input { height: 40px; padding: 0 12px; }
.field textarea { padding: 10px 12px; line-height: 1.6; resize: vertical; }
.field input:focus, .field select:focus, .field textarea:focus, .image-url-row input:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.price-input {
  display: flex;
  align-items: center;
  height: 40px;
  border: 1px solid #cfd8e3;
  border-radius: 6px;
  background: #fff;
}
.price-input span {
  display: grid;
  place-items: center;
  width: 40px;
  height: 100%;
  color: #ff4d4f;
  font-weight: 700;
  border-right: 1px solid #edf0f3;
}
.price-input input {
  height: 100%;
  border: 0;
  border-radius: 0 6px 6px 0;
  box-shadow: none;
}
.image-section { padding-bottom: 18px; }
.upload-drop {
  display: grid;
  place-items: center;
  gap: 6px;
  min-height: 132px;
  margin-bottom: 14px;
  border: 1px dashed #9ec8ff;
  border-radius: 8px;
  color: #1677ff;
  background: #f5f9ff;
  cursor: pointer;
}
.upload-drop input { display: none; }
.upload-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #fff;
  background: #1677ff;
  font-size: 24px;
  line-height: 1;
}
.upload-drop small { color: #71808f; font-size: 12px; }
.image-url-list { display: grid; gap: 8px; }
.image-url-row { display: flex; gap: 8px; }
.image-url-row input { flex: 1; }
.remove-btn, .add-btn, .cancel-btn, .submit-btn {
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.remove-btn {
  width: 64px;
  border: 1px solid #d9d9d9;
  color: #52606d;
  background: #fff;
}
.remove-btn:disabled { opacity: 0.45; cursor: default; }
.add-btn {
  margin-top: 10px;
  padding: 7px 12px;
  border: 1px dashed #1677ff;
  color: #1677ff;
  background: #fff;
}
.add-btn:disabled { opacity: 0.45; cursor: default; }
.error-msg {
  margin: 16px 20px 0;
  padding: 9px 12px;
  border-left: 3px solid #ff4d4f;
  color: #b42318;
  background: #fff1f0;
  font-size: 13px;
}
.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  background: #fafcff;
}
.cancel-btn {
  padding: 9px 18px;
  border: 1px solid #cfd8e3;
  color: #52606d;
  background: #fff;
}
.submit-btn {
  min-width: 132px;
  padding: 9px 20px;
  border: 0;
  color: #fff;
  background: #1677ff;
  font-weight: 600;
}
.submit-btn:disabled { opacity: 0.6; cursor: default; }
@media (max-width: 900px) {
  .publish-workspace { max-width: none; }
}
@media (max-width: 640px) {
  .publish-hero { display: block; }
  .field-grid { grid-template-columns: 1fr; }
  .action-bar { display: grid; grid-template-columns: 1fr; }
}
</style>
