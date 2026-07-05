import { apiGet, apiPut, apiPost } from './http'
import type { ApiResponse, UserProfile } from '../types/domain'

export interface UpdateProfileRequest {
  nickname?: string
  phone?: string
  email?: string
}

export async function getMyProfile(): Promise<ApiResponse<UserProfile>> {
  return apiGet<UserProfile>('/users/me')
}

export async function updateProfile(data: UpdateProfileRequest): Promise<ApiResponse<UserProfile>> {
  return apiPut<UserProfile>('/users/me', data)
}

export async function getMyProducts(): Promise<ApiResponse<unknown>> {
  return apiGet<unknown>('/users/me/products')
}
