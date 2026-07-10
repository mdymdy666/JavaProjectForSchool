import { apiDelete, apiGet, apiPut, apiPost } from './http'
import type { AddressView, ApiResponse, MyProduct, ProductCard, UserProfile, UserReport } from '../types/domain'

export interface UpdateProfileRequest {
  nickname?: string
  phone?: string
  email?: string
  avatarUrl?: string
}

export interface AddressRequest {
  receiverName: string
  receiverPhone: string
  detailAddress: string
  defaultAddress?: boolean
}

export interface VerificationRequest {
  realName: string
  idCardNo: string
}

export async function getMyProfile(): Promise<ApiResponse<UserProfile>> {
  return apiGet<UserProfile>('/users/me')
}

export async function updateProfile(data: UpdateProfileRequest): Promise<ApiResponse<UserProfile>> {
  return apiPut<UserProfile>('/users/me', data)
}

export async function getMyProducts(): Promise<ApiResponse<MyProduct[]>> {
  return apiGet<MyProduct[]>('/users/me/products')
}

export async function getMyFavorites(): Promise<ApiResponse<ProductCard[]>> {
  return apiGet<ProductCard[]>('/users/me/favorites')
}

export async function getMyReports(): Promise<ApiResponse<UserReport[]>> {
  return apiGet<UserReport[]>('/reports/my')
}

export async function getMyAddresses(): Promise<ApiResponse<AddressView[]>> {
  return apiGet<AddressView[]>('/users/me/addresses')
}

export async function createAddress(data: AddressRequest): Promise<ApiResponse<AddressView[]>> {
  return apiPost<AddressView[]>('/users/me/addresses', data)
}

export async function updateAddress(id: number, data: AddressRequest): Promise<ApiResponse<AddressView[]>> {
  return apiPut<AddressView[]>(`/users/me/addresses/${id}`, data)
}

export async function deleteAddress(id: number): Promise<ApiResponse<AddressView[]>> {
  return apiDelete<AddressView[]>(`/users/me/addresses/${id}`)
}

export async function setDefaultAddress(id: number): Promise<ApiResponse<AddressView[]>> {
  return apiPost<AddressView[]>(`/users/me/addresses/${id}/default`)
}

export async function submitVerification(data: VerificationRequest): Promise<ApiResponse<UserProfile>> {
  return apiPost<UserProfile>('/users/me/verification', data)
}
