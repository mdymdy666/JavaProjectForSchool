import { describe, expect, it } from 'vitest'
import { routes } from './index'

describe('routes', () => {
  it('contains required answer-defense pages', () => {
    const names = routes.map((route) => route.name)
    expect(names).toContain('login')
    expect(names).toContain('home')
    expect(names).toContain('product-detail')
    expect(names).toContain('orders')
    expect(names).toContain('admin')
  })

  it('has auth guard on protected pages', () => {
    const publish = routes.find((r) => r.name === 'publish')
    expect(publish?.meta?.auth).toBe(true)

    const profile = routes.find((r) => r.name === 'profile')
    expect(profile?.meta?.auth).toBe(true)

    const orders = routes.find((r) => r.name === 'orders')
    expect(orders?.meta?.auth).toBe(true)

    const messages = routes.find((r) => r.name === 'messages')
    expect(messages?.meta?.auth).toBe(true)
  })

  it('has admin guard on admin page', () => {
    const admin = routes.find((r) => r.name === 'admin')
    expect(admin?.meta?.admin).toBe(true)
  })
})
