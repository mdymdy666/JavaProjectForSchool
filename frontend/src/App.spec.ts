import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'
import App from './App.vue'
import { routes } from './router'

function mountApp() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createWebHashHistory(), routes })
  return mount(App, { global: { plugins: [pinia, router] } })
}

describe('App', () => {
  it('renders the campus trade app shell with header', () => {
    const wrapper = mountApp()

    expect(wrapper.text()).toContain('校园二手交易')
    expect(wrapper.text()).toContain('登录')
    expect(wrapper.text()).toContain('注册')
  })

  it('shows home page by default', () => {
    const wrapper = mountApp()

    expect(wrapper.text()).toContain('首页')
  })
})
