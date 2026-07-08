import { apiGet, apiPost, apiDelete, apiPut } from './http'
import type { ApiResponse, ProductCard, PageResult, DashboardView } from '../types/domain'

export async function getDashboard(): Promise<ApiResponse<DashboardView>> {
  return apiGet<DashboardView>('/admin/dashboard')
}

export async function getPendingProducts(): Promise<ApiResponse<PageResult<ProductCard>>> {
  return apiGet<PageResult<ProductCard>>('/admin/products/pending')
}

export async function auditProduct(id: number, approved: boolean, reason?: string): Promise<ApiResponse<null>> {
  return apiPost<null>(`/admin/products/${id}/audit`, { approved, reason: reason || '' })
}

export async function createAnnouncement(title: string, content: string): Promise<ApiResponse<unknown>> {
  return apiPost<unknown>('/admin/announcements', { title, content, published: true })
}

export async function deleteAnnouncement(id: number): Promise<ApiResponse<null>> {
  return apiDelete<null>(`/admin/announcements/${id}`)
}

export interface ReportView {
  id: number
  reporterId: number
  reporterNickname: string
  productId: number
  productTitle: string
  sellerNickname: string
  reason: string
  reportStatus: string
  handlingResult: string | null
  createdAt: string
  processedAt: string | null
}

export interface SensitiveWordView {
  id: number
  word: string
  enabled: boolean
}

export async function getReports(status?: string): Promise<ApiResponse<ReportView[]>> {
  return apiGet<ReportView[]>('/admin/reports', status ? { status } : undefined)
}

export async function handleReport(
  id: number,
  payload: { status: string; handlingResult: string; offShelfProduct: boolean }
): Promise<ApiResponse<null>> {
  return apiPost<null>(`/admin/reports/${id}/handle`, payload)
}

export async function getSensitiveWords(): Promise<ApiResponse<SensitiveWordView[]>> {
  return apiGet<SensitiveWordView[]>('/admin/sensitive-words')
}

export async function addSensitiveWord(word: string): Promise<ApiResponse<SensitiveWordView>> {
  return apiPost<SensitiveWordView>('/admin/sensitive-words', { word })
}

export async function setSensitiveWordStatus(id: number, enabled: boolean): Promise<ApiResponse<null>> {
  return apiPut<null>(`/admin/sensitive-words/${id}/status`, { enabled })
}
