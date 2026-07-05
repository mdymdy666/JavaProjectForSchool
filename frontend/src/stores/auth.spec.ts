import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../api/auth', () => ({
  loginApi: vi.fn().mockResolvedValue({
    code: 200,
    message: 'success',
    data: { userId: 9, nickname: '测试用户', role: 'USER', accessToken: 'jwt-token' }
  }),
  logoutApi: vi.fn().mockResolvedValue({ code: 200, message: 'success', data: null })
}))

import { useAuthStore } from './auth'

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('stores a session for the backend success code', async () => {
    const store = useAuthStore()
    await store.login({ account: 'student', password: 'Pass1234' })

    expect(store.isLoggedIn).toBe(true)
    expect(localStorage.getItem('campus-token')).toBe('jwt-token')
  })
})
