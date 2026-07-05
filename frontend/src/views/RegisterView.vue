<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerApi, registerCaptchaApi } from '../api/auth'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  captcha: ''
})
const captchaSent = ref(false)
const loading = ref(false)
const error = ref('')

async function getCaptcha() {
  if (!form.username) return
  error.value = ''
  try {
    await registerCaptchaApi(form.username)
    captchaSent.value = true
  } catch {
    error.value = '获取验证码失败'
  }
}

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const res = await registerApi({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      captcha: form.captcha || undefined
    })
    if (res.code === 200) {
      router.push('/login')
    } else {
      error.value = res.message || '注册失败'
    }
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册</h2>
      <form @submit.prevent="submit">
        <label>
          账号
          <input v-model="form.username" type="text" placeholder="用户名" required />
        </label>
        <label>
          昵称
          <input v-model="form.nickname" type="text" placeholder="你的昵称" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" placeholder="至少 8 位密码" required minlength="8" />
        </label>
        <label>
          验证码
          <div class="captcha-row">
            <input v-model="form.captcha" type="text" placeholder="输入验证码" />
            <button type="button" @click="getCaptcha" :disabled="!form.username">
              {{ captchaSent ? '已发送' : '获取验证码' }}
            </button>
          </div>
        </label>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '注册中...' : '注册' }}</button>
      </form>
      <p class="switch-link">
        已有账号？<RouterLink to="/login">去登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}
.auth-card {
  width: 400px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 32px;
}
.auth-card h2 {
  margin: 0 0 24px;
  font-size: 22px;
}
label {
  display: block;
  margin-bottom: 16px;
  font-size: 14px;
  color: #333;
}
label input {
  display: block;
  width: 100%;
  margin-top: 4px;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.captcha-row {
  display: flex;
  gap: 8px;
}
.captcha-row input { flex: 1; }
.captcha-row button {
  white-space: nowrap;
  padding: 8px 12px;
  font-size: 13px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}
button[type="submit"] {
  width: 100%;
  padding: 10px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
}
button[type="submit"]:disabled { opacity: 0.6; }
.error-msg { color: #ff4d4f; font-size: 13px; margin: 0 0 12px; }
.switch-link { margin-top: 16px; text-align: center; font-size: 14px; color: #666; }
.switch-link a { color: #1677ff; }
</style>
