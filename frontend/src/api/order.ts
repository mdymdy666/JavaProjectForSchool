import { apiGet, apiPost } from './http'
import type { ApiResponse, OrderView } from '../types/domain'

export interface CreateOrderRequest {
  productId: number
  remark?: string
}

export async function getBuyerOrders(): Promise<ApiResponse<OrderView[]>> {
  return apiGet<OrderView[]>('/orders', { role: 'buyer' })
}

export async function getSellerOrders(): Promise<ApiResponse<OrderView[]>> {
  return apiGet<OrderView[]>('/orders', { role: 'seller' })
}

export async function createOrder(data: CreateOrderRequest): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>('/orders', data)
}

export async function payOrder(id: number): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/pay`)
}

export async function shipOrder(id: number, logisticsInfo: string): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/ship`, { logisticsInfo })
}

export async function confirmOrder(id: number): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/confirm`)
}

export async function cancelOrder(id: number): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/cancel`)
}
