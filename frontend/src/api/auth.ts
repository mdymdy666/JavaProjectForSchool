import { apiGet, apiPost } from './http'
import type { ApiResponse } from '../types/domain'

export interface LoginRequest {
  account: string
  password: string
}

export interface LoginResponse {
  userId: number
  nickname: string
  role: string
  accessToken: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  captcha?: string
}

export interface ResetPasswordRequest {
  account: string
  captcha: string
  newPassword: string
}

export interface UserSummary {
  id: number
  username: string
  nickname: string
  role: string
}

export interface CaptchaResponse {
  code: string
  expiresInSeconds: number
}

export async function loginApi(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  return apiPost<LoginResponse>('/auth/login', data)
}

export async function registerApi(data: RegisterRequest): Promise<ApiResponse<UserSummary>> {
  return apiPost<UserSummary>('/auth/register', data)
}

export async function registerCaptchaApi(account: string): Promise<ApiResponse<CaptchaResponse>> {
  return apiPost<CaptchaResponse>(`/auth/captcha/register?account=${encodeURIComponent(account)}`)
}

export async function passwordResetCaptchaApi(account: string): Promise<ApiResponse<CaptchaResponse>> {
  return apiPost<CaptchaResponse>(`/auth/captcha/password-reset?account=${encodeURIComponent(account)}`)
}

export async function resetPasswordApi(data: ResetPasswordRequest): Promise<ApiResponse<null>> {
  return apiPost<null>('/auth/password/reset', data)
}

export async function logoutApi(): Promise<ApiResponse<null>> {
  return apiPost<null>('/auth/logout')
}
