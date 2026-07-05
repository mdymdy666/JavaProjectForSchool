import { apiGet, apiPost } from './http'
import type { ApiResponse, SiteMessage, Notification } from '../types/domain'

export interface SendMessageRequest {
  receiverId: number
  productId?: number
  content: string
}

export async function getMessages(): Promise<ApiResponse<SiteMessage[]>> {
  return apiGet<SiteMessage[]>('/messages')
}

export async function sendMessage(data: SendMessageRequest): Promise<ApiResponse<SiteMessage>> {
  return apiPost<SiteMessage>('/messages', data)
}

export async function markMessageRead(id: number): Promise<ApiResponse<null>> {
  return apiPost<null>(`/messages/${id}/read`)
}

export async function getNotifications(): Promise<ApiResponse<Notification[]>> {
  return apiGet<Notification[]>('/notifications')
}

export async function markNotificationRead(id: number): Promise<ApiResponse<null>> {
  return apiPost<null>(`/notifications/${id}/read`)
}
