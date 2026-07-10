import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./http', () => ({
  apiGet: vi.fn().mockResolvedValue({ code: 200, message: 'success', data: [] }),
  apiPost: vi.fn().mockResolvedValue({ code: 200, message: 'success', data: null }),
  apiPut: vi.fn(),
  apiDelete: vi.fn()
}))

import { apiGet, apiPost } from './http'
import { getBuyerOrders, getOrderDetail, getSellerOrders, shipOrder } from './order'
import { relistProduct } from './product'
import { createAnnouncement } from './admin'
import { getMyFavorites, getMyReports } from './user'

describe('frontend and backend API contract', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uses the role query for buyer and seller order lists', async () => {
    await getBuyerOrders()
    expect(apiGet).toHaveBeenCalledWith('/orders', { role: 'buyer' })

    await getSellerOrders()
    expect(apiGet).toHaveBeenCalledWith('/orders', { role: 'seller' })
  })

  it('sends logistics information when shipping', async () => {
    await shipOrder(8, '东门自提柜 A12')
    expect(apiPost).toHaveBeenCalledWith('/orders/8/ship', {
      logisticsInfo: '东门自提柜 A12'
    })
  })

  it('uses dedicated endpoints for existing order payment and user lists', async () => {
    await getOrderDetail(9)
    expect(apiGet).toHaveBeenCalledWith('/orders/9')

    await getMyFavorites()
    expect(apiGet).toHaveBeenCalledWith('/users/me/favorites')

    await getMyReports()
    expect(apiGet).toHaveBeenCalledWith('/reports/my')
  })

  it('uses the backend relist route and publishes announcements', async () => {
    await relistProduct(3)
    expect(apiPost).toHaveBeenCalledWith('/products/3/on-shelf')

    await createAnnouncement('交易提醒', '请在公共场所交易')
    expect(apiPost).toHaveBeenCalledWith('/admin/announcements', {
      title: '交易提醒',
      content: '请在公共场所交易',
      published: true
    })
  })
})
