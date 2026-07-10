<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({
  account: '',
  password: ''
})
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const res = await auth.login({
      account: form.account,
      password: form.password
    })
    if (res.code === 200) {
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      router.push(redirect)
    } else {
      error.value = res.message || '登录失败'
    }
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg || '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-card">
      <div class="auth-head">
        <h2>登录</h2>
        <p>进入校园二手交易平台</p>
      </div>
      <form @submit.prevent="submit">
        <label>
          账号
          <input v-model="form.account" type="text" placeholder="用户名 / 手机号 / 邮箱" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" placeholder="输入密码" required minlength="8" />
        </label>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
      </form>
      <div class="auth-links">
        <RouterLink to="/forgot-password">忘记密码</RouterLink>
        <span>还没有账号？<RouterLink to="/register">立即注册</RouterLink></span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 72px 16px;
}
.auth-card {
  width: min(420px, 100%);
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}
.auth-head h2 {
  margin: 0;
  font-size: 26px;
}
.auth-head p {
  margin: 8px 0 24px;
  color: #64748b;
}
label {
  display: block;
  margin-bottom: 16px;
  font-size: 14px;
  color: #334155;
  font-weight: 700;
}
label input {
  display: block;
  width: 100%;
  height: 42px;
  margin-top: 7px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
label input:focus {
  outline: none;
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.12);
}
button[type="submit"] {
  width: 100%;
  height: 42px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
button[type="submit"]:disabled { opacity: 0.65; cursor: not-allowed; }
.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin: 0 0 12px;
}
.auth-links {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: #64748b;
}
.auth-links a { color: #1677ff; text-decoration: none; font-weight: 700; }
@media (max-width: 520px) {
  .auth-links { flex-direction: column; align-items: center; }
}
</style>
