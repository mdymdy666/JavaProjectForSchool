import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ForgotPasswordView from './ForgotPasswordView.vue'
import { passwordResetCaptchaApi, resetPasswordApi } from '../api/auth'

vi.mock('../api/auth', () => ({
  passwordResetCaptchaApi: vi.fn().mockResolvedValue({ code: 200, data: { code: '123456', expiresInSeconds: 300 } }),
  resetPasswordApi: vi.fn().mockResolvedValue({ code: 200, data: null })
}))

describe('ForgotPasswordView', () => {
  it('requests captcha and submits password reset', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/forgot-password', component: ForgotPasswordView },
        { path: '/login', component: { template: '<div />' } }
      ]
    })
    await router.push('/forgot-password')
    await router.isReady()

    const wrapper = mount(ForgotPasswordView, { global: { plugins: [router] } })
    await wrapper.find('input[placeholder="用户名 / 手机号 / 邮箱"]').setValue('student01')
    await wrapper.find('button[type="button"]').trigger('click')
    await flushPromises()

    expect(passwordResetCaptchaApi).toHaveBeenCalledWith('student01')
    expect(wrapper.text()).toContain('123456')

    await wrapper.find('input[placeholder="输入验证码"]').setValue('123456')
    await wrapper.find('input[placeholder="至少 8 位密码"]').setValue('NewPass123')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(resetPasswordApi).toHaveBeenCalledWith({
      account: 'student01',
      captcha: '123456',
      newPassword: 'NewPass123'
    })
    expect(wrapper.text()).toContain('密码已重置')
  })
})
