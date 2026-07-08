import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import PublishProductView from './PublishProductView.vue'

vi.mock('../api/auth', () => ({
  loginApi: vi.fn(),
  logoutApi: vi.fn().mockResolvedValue({ code: 200, message: 'success', data: null })
}))

vi.mock('../api/product', () => ({
  publishProduct: vi.fn().mockResolvedValue({ code: 200, data: { id: 1 } })
}))

vi.mock('../api/upload', () => ({
  uploadImage: vi.fn().mockResolvedValue({ code: 200, data: { url: '/uploads/demo.jpg' } })
}))

describe('PublishProductView', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('presents a polished publish workspace without synthetic placeholder copy', async () => {
    const pinia = createPinia()
    useAuthStore(pinia).saveSession('token', 1, '计科小李', 'USER')
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/publish', component: PublishProductView },
        { path: '/login', component: { template: '<div />' } },
        { path: '/products', component: { template: '<div />' } }
      ]
    })
    await router.push('/publish')
    await router.isReady()

    const wrapper = mount(PublishProductView, { global: { plugins: [pinia, router] } })

    expect(wrapper.get('[data-test="publish-workspace"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="image-uploader"]').text()).toContain('上传商品图片')
    expect(wrapper.get('[data-test="action-bar"]').text()).toContain('发布商品')
    expect(wrapper.text()).not.toContain('让闲置被看见')
    expect(wrapper.text()).not.toContain('等待图片')
    expect(wrapper.text()).not.toContain('待审核')
    expect(wrapper.text()).not.toContain('商品标题预览')
    expect(wrapper.text()).not.toContain('¥0.00')
    expect(wrapper.text()).not.toContain('买家视角')
  })
})
