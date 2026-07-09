import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import CoverView from './CoverView.vue'

describe('CoverView', () => {
  it('shows the entrance slogan and opens the real home page', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: CoverView },
        { path: '/home', component: { template: '<div>home</div>' } }
      ]
    })
    await router.push('/')
    await router.isReady()

    const wrapper = mount(CoverView, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('同校面对面 交易更安全')
    expect(wrapper.text()).toContain('进入平台')

    await wrapper.get('.enter-button').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/home')

    wrapper.unmount()
  })
})
