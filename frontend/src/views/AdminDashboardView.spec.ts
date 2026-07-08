import { describe, expect, it } from 'vitest'
import adminSource from './AdminDashboardView.vue?raw'

describe('AdminDashboardView', () => {
  it('keeps inactive admin tabs readable', () => {
    expect(adminSource).toMatch(/\.tabs button \{[^}]*color:\s*#253044;/s)
    expect(adminSource).toMatch(/\.tabs button\.active \{[^}]*color:\s*#fff;/s)
  })

  it('renders cleaner dashboard charts', () => {
    expect(adminSource).toContain('class="chart-canvas"')
    expect(adminSource).toMatch(/minInterval:\s*1/)
    expect(adminSource).toContain('Number(params.percent).toFixed(0)')
  })
})
