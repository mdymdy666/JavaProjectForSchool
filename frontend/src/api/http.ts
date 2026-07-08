import axios from 'axios'
import type { ApiResponse } from '../types/domain'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('campus-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('campus-token')
      localStorage.removeItem('campus-user')
    }
    return Promise.reject(error)
  }
)

export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> {
  const res = await http.get<ApiResponse<T>>(url, { params })
  return res.data
}

export async function apiPost<T>(url: string, data?: unknown): Promise<ApiResponse<T>> {
  const res = await http.post<ApiResponse<T>>(url, data)
  return res.data
}

export async function apiPut<T>(url: string, data?: unknown): Promise<ApiResponse<T>> {
  const res = await http.put<ApiResponse<T>>(url, data)
  return res.data
}

export async function apiDelete<T>(url: string): Promise<ApiResponse<T>> {
  const res = await http.delete<ApiResponse<T>>(url)
  return res.data
}
