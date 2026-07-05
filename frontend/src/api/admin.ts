import { apiGet, apiPost, apiDelete } from './http'
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
  return apiPost<unknown>('/admin/announcements', { title, content })
}

export async function deleteAnnouncement(id: number): Promise<ApiResponse<null>> {
  return apiDelete<null>(`/admin/announcements/${id}`)
}
