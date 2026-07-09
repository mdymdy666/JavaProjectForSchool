import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from './App.vue'
import { routes } from './router'

async function mountApp(initialPath = '/') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(initialPath)
  await router.isReady()
  return mount(App, { global: { plugins: [pinia, router] } })
}

describe('App', () => {
  it('renders the cover page by default without the app header', async () => {
    const wrapper = await mountApp()

    expect(wrapper.text()).toContain('同校面对面 交易更安全')
    expect(wrapper.text()).toContain('进入平台')
    expect(wrapper.find('.app-header').exists()).toBe(false)
  })

  it('keeps the original home page available after the cover', async () => {
    const wrapper = await mountApp()
    const router = wrapper.vm.$router

    await router.push('/home')
    await flushPromises()
    expect(wrapper.text()).toContain('首页')
    expect(wrapper.find('.app-header').exists()).toBe(true)
  })
})
