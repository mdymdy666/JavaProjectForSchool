<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const rules = [
  { title: '优先当面交易', text: '建议选择校园内公开区域交接，当场确认商品型号、成色、配件和数量。' },
  { title: '保留平台沟通记录', text: '重要约定尽量留在平台聊天中，包括价格、交付时间、售后说明和退款条件。' },
  { title: '谨慎提前付款', text: '不要脱离平台私下转账，贵重物品应在验收无误后再完成付款。' },
  { title: '核对身份与商品', text: '交易前确认对方账号、商品图片、描述和实物一致，发现异常及时取消交易。' }
]

const risks = [
  '要求加外部联系方式并催促转账',
  '商品价格明显低于正常范围且拒绝当面验货',
  '图片模糊、描述含糊，无法提供实物细节',
  '临时更换收款账号、交付地点或交易对象'
]

const steps = [
  '在商品详情页点击举报，说明问题并提交证据线索。',
  '管理员在后台核验举报内容、商品信息和聊天记录。',
  '情况属实的平台会下架商品、通知双方，并保留处理记录。',
  '订单或付款争议优先通过平台消息沟通，必要时联系辅导员或校内管理人员。'
]
</script>

<template>
  <main class="safety-page">
    <section class="safety-hero">
      <div>
        <p class="eyebrow">交易安全告示</p>
        <h1>校园二手交易安全须知</h1>
        <p>所有交易以真实、透明、可追溯为原则。请在付款、交付和确认收货前认真核对信息。</p>
      </div>
      <button type="button" @click="router.push('/products')">返回市场</button>
    </section>

    <section class="notice-band">
      <strong>平台提醒</strong>
      <span>不要离开平台交易，不要向陌生账号提前转账，不要忽略与商品描述不一致的细节。</span>
    </section>

    <section class="rule-grid" aria-label="核心规则">
      <article v-for="rule in rules" :key="rule.title">
        <span>✓</span>
        <h2>{{ rule.title }}</h2>
        <p>{{ rule.text }}</p>
      </article>
    </section>

    <section class="two-col">
      <div class="panel">
        <h2>常见风险信号</h2>
        <ul>
          <li v-for="risk in risks" :key="risk">{{ risk }}</li>
        </ul>
      </div>

      <div class="panel">
        <h2>举报与纠纷处理</h2>
        <ol>
          <li v-for="step in steps" :key="step">{{ step }}</li>
        </ol>
      </div>
    </section>

    <section class="evidence-panel">
      <div>
        <h2>建议保留的凭证</h2>
        <p>商品截图、聊天记录、付款记录、物流或交付照片、验货视频，都可以帮助管理员更快判断问题。</p>
      </div>
      <button type="button" @click="router.push('/messages')">查看消息记录</button>
    </section>
  </main>
</template>

<style scoped>
.safety-page {
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.safety-hero,
.notice-band,
.rule-grid article,
.panel,
.evidence-panel {
  border: 1px solid #dfe8f3;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.07);
}

.safety-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 34px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.98), rgba(255, 255, 255, 0.84)),
    linear-gradient(135deg, #e0f2fe, #f0fdf4);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--brand-blue);
  font-weight: 900;
}

.safety-hero h1 {
  margin: 0;
  color: #111827;
  font-size: 34px;
  line-height: 1.2;
}

.safety-hero p:not(.eyebrow) {
  max-width: 680px;
  margin: 12px 0 0;
  color: #526173;
  font-size: 16px;
  line-height: 1.7;
}

.safety-hero button,
.evidence-panel button {
  height: 42px;
  padding: 0 18px;
  border-radius: 8px;
  background: var(--brand-blue);
  font-weight: 900;
  white-space: nowrap;
}

.notice-band {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 16px 20px;
  color: #166534;
  background: #f0fdf4;
}

.notice-band strong {
  color: #15803d;
  font-size: 18px;
}

.rule-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.rule-grid article {
  padding: 20px;
}

.rule-grid span {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  margin-bottom: 14px;
  border-radius: 10px;
  color: #fff;
  background: #22c55e;
  font-weight: 900;
}

.rule-grid h2,
.panel h2,
.evidence-panel h2 {
  margin: 0 0 10px;
  color: #17212b;
  font-size: 20px;
}

.rule-grid p,
.evidence-panel p {
  margin: 0;
  color: #526173;
  line-height: 1.7;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.panel {
  padding: 22px;
}

.panel ul,
.panel ol {
  display: grid;
  gap: 12px;
  margin: 0;
  padding-left: 22px;
  color: #475569;
  line-height: 1.7;
}

.evidence-panel {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 24px;
}

@media (max-width: 900px) {
  .safety-hero,
  .evidence-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .rule-grid,
  .two-col {
    grid-template-columns: 1fr;
  }

  .notice-band {
    grid-template-columns: 1fr;
  }
}
</style>
