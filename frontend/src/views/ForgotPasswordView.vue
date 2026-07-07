<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { passwordResetCaptchaApi, resetPasswordApi } from '../api/auth'

const router = useRouter()
const form = reactive({
  account: '',
  captcha: '',
  newPassword: ''
})
const loading = ref(false)
const captchaSent = ref(false)
const error = ref('')
const success = ref('')

async function getCaptcha() {
  if (!form.account) return
  error.value = ''
  success.value = ''
  try {
    await passwordResetCaptchaApi(form.account)
    captchaSent.value = true
    success.value = '验证码已发送，演示码为 123456'
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg || '获取验证码失败'
  }
}

async function submit() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const res = await resetPasswordApi({
      account: form.account,
      captcha: form.captcha,
      newPassword: form.newPassword
    })
    if (res.code === 200) {
      success.value = '密码已重置，请使用新密码登录'
      setTimeout(() => router.push('/login'), 700)
    } else {
      error.value = res.message || '重置失败'
    }
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg || '重置失败，请检查账号和验证码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-card">
      <div class="auth-head">
        <h2>找回密码</h2>
        <p>通过邮箱、手机号或用户名验证后重置密码</p>
      </div>
      <form @submit.prevent="submit">
        <label>
          账号
          <input v-model="form.account" type="text" placeholder="用户名 / 手机号 / 邮箱" required />
        </label>
        <label>
          验证码
          <div class="captcha-row">
            <input v-model="form.captcha" type="text" placeholder="输入验证码" required />
            <button type="button" @click="getCaptcha" :disabled="!form.account">
              {{ captchaSent ? '重新获取' : '获取验证码' }}
            </button>
          </div>
        </label>
        <label>
          新密码
          <input v-model="form.newPassword" type="password" placeholder="至少 8 位密码" required minlength="8" />
        </label>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <p v-if="success" class="success-msg">{{ success }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '提交中...' : '重置密码' }}</button>
      </form>
      <p class="switch-link">
        想起来了？<RouterLink to="/login">返回登录</RouterLink>
      </p>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding: 64px 16px;
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
.error-msg, .success-msg { font-size: 13px; margin: 0 0 12px; }
.error-msg { color: #ef4444; }
.success-msg { color: #16a34a; }
.switch-link { margin-top: 16px; text-align: center; color: #64748b; font-size: 14px; }
.switch-link a { color: #1677ff; text-decoration: none; font-weight: 700; }
</style>
