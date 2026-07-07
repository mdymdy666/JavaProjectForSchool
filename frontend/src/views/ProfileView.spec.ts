import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ProfileView from './ProfileView.vue'

vi.mock('../api/user', () => ({
  getMyProfile: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      id: 1,
      username: 'student01',
      nickname: '计科小李',
      phone: '13800000000',
      email: 'student@campus.edu',
      avatarUrl: null,
      role: 'USER',
      creditScore: 95,
      realName: null,
      idCardNo: null,
      realNameStatus: 'UNVERIFIED'
    }
  }),
  getMyProducts: vi.fn().mockResolvedValue({
    code: 200,
    data: [{ id: 1, title: 'Cherry 键盘', status: 'APPROVED' }]
  }),
  getMyAddresses: vi.fn().mockResolvedValue({
    code: 200,
    data: [{
      id: 3,
      receiverName: '计科小李',
      receiverPhone: '13800000000',
      detailAddress: '软件学院 502',
      defaultAddress: true,
      createdAt: '2026-07-07T08:00:00'
    }]
  }),
  updateProfile: vi.fn().mockResolvedValue({ code: 200, data: null }),
  createAddress: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  updateAddress: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  deleteAddress: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  setDefaultAddress: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  submitVerification: vi.fn().mockResolvedValue({ code: 200, data: null })
}))

vi.mock('../api/upload', () => ({
  uploadImage: vi.fn().mockResolvedValue({ code: 200, data: { url: '/uploads/avatar.png' } })
}))

describe('ProfileView', () => {
  it('renders the workspace-style user center modules', async () => {
    const wrapper = mount(ProfileView)
    await flushPromises()

    expect(wrapper.get('[data-test="profile-workbench"]').text()).toContain('计科小李')
    expect(wrapper.get('[data-test="profile-panel"]').text()).toContain('上传头像')
    expect(wrapper.text()).toContain('待处理事项')

    await wrapper.get('button:nth-of-type(2)').trigger('click')
    expect(wrapper.get('[data-test="address-panel"]').text()).toContain('软件学院 502')
    expect(wrapper.get('[data-test="address-panel"]').text()).toContain('默认')

    await wrapper.get('button:nth-of-type(3)').trigger('click')
    expect(wrapper.get('[data-test="trust-panel"]').text()).toContain('信誉评分')
    expect(wrapper.get('[data-test="trust-panel"]').text()).toContain('未实名认证')

    await wrapper.get('button:nth-of-type(4)').trigger('click')
    expect(wrapper.get('[data-test="products-panel"]').text()).toContain('Cherry 键盘')
  })
})
