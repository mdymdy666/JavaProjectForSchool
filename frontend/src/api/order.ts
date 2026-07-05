import { apiGet, apiPost } from './http'
import type { ApiResponse, OrderView, PageResult } from '../types/domain'

export interface CreateOrderRequest {
  productId: number
  remark?: string
}

export async function getBuyerOrders(params?: Record<string, unknown>): Promise<ApiResponse<PageResult<OrderView>>> {
  return apiGet<PageResult<OrderView>>('/orders/buyer', params)
}

export async function getSellerOrders(params?: Record<string, unknown>): Promise<ApiResponse<PageResult<OrderView>>> {
  return apiGet<PageResult<OrderView>>('/orders/seller', params)
}

export async function createOrder(data: CreateOrderRequest): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>('/orders', data)
}

export async function payOrder(id: number): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/pay`)
}

export async function shipOrder(id: number): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/ship`)
}

export async function confirmOrder(id: number): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/confirm`)
}

export async function cancelOrder(id: number): Promise<ApiResponse<OrderView>> {
  return apiPost<OrderView>(`/orders/${id}/cancel`)
}
