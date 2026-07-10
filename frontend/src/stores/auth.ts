import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi, type LoginRequest } from '../api/auth'
import type { ApiResponse } from '../types/domain'
import { useCartStore } from './cart'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('campus-token'))
  const userId = ref<number | null>(Number(localStorage.getItem('campus-user-id')) || null)
  const nickname = ref<string | null>(localStorage.getItem('campus-user'))
  const role = ref<string | null>(localStorage.getItem('campus-role'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  async function login(request: LoginRequest): Promise<ApiResponse<{ userId: number; nickname: string; role: string; accessToken: string }>> {
    const res = await loginApi(request)
    if (res.code === 200 && res.data) {
      saveSession(res.data.accessToken, res.data.userId, res.data.nickname, res.data.role)
    }
    return res
  }

  function saveSession(accessToken: string, id: number, name: string, userRole: string) {
    token.value = accessToken
    userId.value = id
    nickname.value = name
    role.value = userRole
    localStorage.setItem('campus-token', accessToken)
    localStorage.setItem('campus-user-id', String(id))
    localStorage.setItem('campus-user', name)
    localStorage.setItem('campus-role', userRole)
    useCartStore().switchOwner()
  }

  async function logout() {
    try { await logoutApi() } catch { /* ignore */ }
    token.value = null
    userId.value = null
    nickname.value = null
    role.value = null
    localStorage.removeItem('campus-token')
    localStorage.removeItem('campus-user-id')
    localStorage.removeItem('campus-user')
    localStorage.removeItem('campus-role')
    useCartStore().switchOwner()
  }

  return { token, userId, nickname, role, isLoggedIn, isAdmin, login, saveSession, logout }
})
