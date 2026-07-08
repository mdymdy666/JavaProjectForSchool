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
    <section class="auth-card">
      <div class="auth-head">
        <h2>注册</h2>
        <p>验证码演示码固定为 123456</p>
      </div>
      <form @submit.prevent="submit">
        <label>
          邮箱 / 手机号 / 用户名
          <input v-model="form.username" type="text" placeholder="作为登录账号使用" required />
        </label>
        <label>
          昵称
          <input v-model="form.nickname" type="text" placeholder="买卖双方看到的名称" required />
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
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 56px 16px;
}
.auth-card {
  width: min(440px, 100%);
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}
.auth-head h2 { margin: 0; font-size: 26px; }
.auth-head p { margin: 8px 0 24px; color: #64748b; }
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
.captcha-row { display: flex; gap: 8px; margin-top: 7px; }
.captcha-row input { margin-top: 0; flex: 1; }
.captcha-row button {
  width: 112px;
  border: 1px solid #1677ff;
  border-radius: 8px;
  background: #fff;
  color: #1677ff;
  cursor: pointer;
}
.captcha-row button:disabled { opacity: 0.55; cursor: not-allowed; }
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
.error-msg { color: #ef4444; font-size: 13px; margin: 0 0 12px; }
.switch-link { margin-top: 16px; text-align: center; color: #64748b; font-size: 14px; }
.switch-link a { color: #1677ff; text-decoration: none; font-weight: 700; }
</style>
