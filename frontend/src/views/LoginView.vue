<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { registerCaptchaApi } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()

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
    const res = await auth.login({
      account: form.username,
      password: form.password
    })
    if (res.code === 200) {
      router.push('/')
    } else {
      error.value = res.message || '登录失败'
    }
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg || '登录失败，请检查账号密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>登录</h2>
      <form @submit.prevent="submit">
        <label>
          账号
          <input v-model="form.username" type="text" placeholder="用户名 / 手机号 / 邮箱" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" placeholder="输入密码" required minlength="8" />
        </label>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
      </form>
      <p class="switch-link">
        还没有账号？<RouterLink to="/register">立即注册</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 80px;
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
.captcha-row input {
  flex: 1;
}
.captcha-row button {
  white-space: nowrap;
  padding: 8px 12px;
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
button[type="submit"]:disabled {
  opacity: 0.6;
}
.error-msg {
  color: #ff4d4f;
  font-size: 13px;
  margin: 0 0 12px;
}
.switch-link {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #666;
}
.switch-link a {
  color: #1677ff;
}
</style>
