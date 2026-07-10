import { beforeEach, describe, expect, it } from 'vitest'
import router, { routes } from './index'

describe('routes', () => {
  beforeEach(async () => {
    localStorage.clear()
    await router.push('/')
  })
  it('contains required answer-defense pages', () => {
    const names = routes.map((route) => route.name)
    expect(names).toContain('login')
    expect(names).toContain('cover')
    expect(names).toContain('home')
    expect(names).toContain('product-detail')
    expect(names).toContain('safety')
    expect(names).toContain('orders')
    expect(names).toContain('favorites')
    expect(names).toContain('product-edit')
    expect(names).toContain('pay-product')
    expect(names).toContain('pay-order')
    expect(names).toContain('admin')
  })

  it('uses the cover as the default entry and keeps home behind /home', () => {
    const cover = routes.find((r) => r.name === 'cover')
    const home = routes.find((r) => r.name === 'home')

    expect(cover?.path).toBe('/')
    expect(cover?.meta?.cover).toBe(true)
    expect(home?.path).toBe('/home')
  })

  it('has auth guard on protected pages', () => {
    const publish = routes.find((r) => r.name === 'publish')
    expect(publish?.meta?.auth).toBe(true)

    const profile = routes.find((r) => r.name === 'profile')
    expect(profile?.meta?.auth).toBe(true)

    const orders = routes.find((r) => r.name === 'orders')
    expect(orders?.meta?.auth).toBe(true)

    const favorites = routes.find((r) => r.name === 'favorites')
    expect(favorites?.meta?.auth).toBe(true)

    const productEdit = routes.find((r) => r.name === 'product-edit')
    expect(productEdit?.meta?.auth).toBe(true)

    const messages = routes.find((r) => r.name === 'messages')
    expect(messages?.meta?.auth).toBe(true)
  })

  it('keeps product checkout separate from existing order payment', () => {
    expect(routes.find((r) => r.name === 'pay-product')?.path).toBe('/pay/product/:productId')
    expect(routes.find((r) => r.name === 'pay-order')?.path).toBe('/pay/order/:orderId')
    expect(routes.find((r) => r.name === 'pay-batch')?.path).toBe('/pay/batch')
  })

  it('has admin guard on admin page', () => {
    const admin = routes.find((r) => r.name === 'admin')
    expect(admin?.meta?.admin).toBe(true)
  })

  it('redirects anonymous users away from protected pages', async () => {
    await router.push('/profile')
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('redirects non-admin users away from the admin page', async () => {
    localStorage.setItem('campus-token', 'token')
    localStorage.setItem('campus-role', 'USER')
    await router.push('/admin')
    expect(router.currentRoute.value.name).toBe('home')
  })
})
