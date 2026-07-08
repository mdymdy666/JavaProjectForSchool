import { apiGet, apiPost, apiPut, apiDelete } from './http'
import type { ApiResponse, PageResult, ProductCard, ProductDetail, RecommendedProduct } from '../types/domain'

export interface ProductQuery {
  keyword?: string
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  sort?: string
  page?: number
  size?: number
}

export interface PublishProductRequest {
  title: string
  categoryId: number
  price: number
  itemCondition: string
  description: string
  imageUrls: string[]
}

export async function searchProducts(query: ProductQuery): Promise<ApiResponse<PageResult<ProductCard>>> {
  return apiGet<PageResult<ProductCard>>('/products', query as Record<string, unknown>)
}

export async function getRecommendedProducts(limit = 4): Promise<ApiResponse<RecommendedProduct[]>> {
  return apiGet<RecommendedProduct[]>('/products/recommend', { limit })
}

export async function getProductDetail(id: number): Promise<ApiResponse<ProductDetail>> {
  return apiGet<ProductDetail>(`/products/${id}`)
}

export async function publishProduct(data: PublishProductRequest): Promise<ApiResponse<ProductDetail>> {
  return apiPost<ProductDetail>('/products', data)
}

export async function editProduct(id: number, data: PublishProductRequest): Promise<ApiResponse<ProductDetail>> {
  return apiPut<ProductDetail>(`/products/${id}`, data)
}

export async function favoriteProduct(id: number): Promise<ApiResponse<{ productId: number; favorite: boolean }>> {
  return apiPost<{ productId: number; favorite: boolean }>(`/products/${id}/favorite`)
}

export async function reportProduct(productId: number, reason: string): Promise<ApiResponse<null>> {
  return apiPost<null>('/reports', { productId, reason })
}

export async function offShelfProduct(id: number): Promise<ApiResponse<ProductDetail>> {
  return apiPost<ProductDetail>(`/products/${id}/off-shelf`)
}

export async function relistProduct(id: number): Promise<ApiResponse<ProductDetail>> {
  return apiPost<ProductDetail>(`/products/${id}/on-shelf`)
}

export async function deleteProduct(id: number): Promise<ApiResponse<null>> {
  return apiDelete<null>(`/products/${id}`)
}
