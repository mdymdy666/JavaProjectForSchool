<script setup lang="ts">
import { computed, reactive, ref } from 'vue'

type ProductStatus = '待审核' | '在售' | '已驳回'
type OrderStatus = '待支付' | '已支付' | '已发货' | '已完成'

interface Product {
  id: number
  title: string
  category: string
  seller: string
  condition: string
  price: number
  status: ProductStatus
  views: number
  description: string
  image: string
}

interface Order {
  id: number
  no: string
  productTitle: string
  buyer: string
  seller: string
  amount: number
  status: OrderStatus
  remark: string
}

interface Message {
  id: number
  sender: string
  receiver: string
  productTitle: string
  content: string
  unread: boolean
}

const activeView = ref<'market' | 'orders' | 'messages' | 'admin'>('market')
const publishForm = reactive({
  title: '蓝牙降噪耳机',
  category: '数码配件',
  condition: '九成新',
  price: 86,
  description: '电池健康，适合自习室和通勤使用。'
})
const messageDraft = ref('可以今晚 7 点在图书馆门口当面交易吗？')

const products = ref<Product[]>([
  {
    id: 1,
    title: '九成新机械键盘',
    category: '数码配件',
    seller: '计科小李',
    condition: '九成新',
    price: 129,
    status: '在售',
    views: 238,
    description: 'Cherry 轴体，送拔键器，适合编程和游戏。',
    image: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=480&q=80'
  },
  {
    id: 2,
    title: '考研数学复习全书',
    category: '图书教材',
    seller: '经管小周',
    condition: '轻微笔记',
    price: 35,
    status: '在售',
    views: 156,
    description: '2026 版，重点章节做了标注。',
    image: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=480&q=80'
  },
  {
    id: 3,
    title: '宿舍折叠桌',
    category: '生活用品',
    seller: '软件小陈',
    condition: '八成新',
    price: 48,
    status: '待审核',
    views: 64,
    description: '可放笔记本电脑，宿舍自提。',
    image: 'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?auto=format&fit=crop&w=480&q=80'
  }
])

const orders = ref<Order[]>([
  {
    id: 301,
    no: 'CT20260704001',
    productTitle: '九成新机械键盘',
    buyer: '软件小陈',
    seller: '计科小李',
    amount: 129,
    status: '已支付',
    remark: '等待卖家发货'
  },
  {
    id: 302,
    no: 'CT20260704002',
    productTitle: '考研数学复习全书',
    buyer: '计科小李',
    seller: '经管小周',
    amount: 35,
    status: '已完成',
    remark: '已完成交易'
  }
])

const messages = ref<Message[]>([
  {
    id: 501,
    sender: '软件小陈',
    receiver: '计科小李',
    productTitle: '九成新机械键盘',
    content: '键盘今晚可以在图书馆门口交易吗？',
    unread: true
  },
  {
    id: 502,
    sender: '管理员',
    receiver: '全体用户',
    productTitle: '系统公告',
    content: '平台演示数据已初始化，可直接按流程答辩。',
    unread: false
  }
])

const approvedProducts = computed(() => products.value.filter((product) => product.status === '在售'))
const pendingProducts = computed(() => products.value.filter((product) => product.status === '待审核'))
const totalAmount = computed(() =>
  orders.value.filter((order) => order.status === '已完成').reduce((sum, order) => sum + order.amount, 0)
)
const unreadCount = computed(() => messages.value.filter((message) => message.unread).length)

const metrics = computed(() => [
  { label: '注册用户', value: '128', hint: '+12 本周' },
  { label: '在售商品', value: String(approvedProducts.value.length), hint: `${pendingProducts.value.length} 件待审核` },
  { label: '订单总数', value: String(orders.value.length), hint: '含模拟支付' },
  { label: '成交金额', value: `￥${totalAmount.value.toFixed(2)}`, hint: '可演示闭环' }
])

const categories = computed(() => {
  const names = ['数码配件', '图书教材', '生活用品']
  return names.map((name) => ({
    name,
    count: products.value.filter((product) => product.category === name).length
  }))
})

function statusClass(status: ProductStatus | OrderStatus) {
  if (status === '在售' || status === '已完成') return 'success'
  if (status === '待审核' || status === '待支付') return 'warning'
  if (status === '已驳回') return 'danger'
  return 'info'
}

function publishProduct() {
  products.value.unshift({
    id: Date.now(),
    title: publishForm.title,
    category: publishForm.category,
    seller: '演示卖家',
    condition: publishForm.condition,
    price: Number(publishForm.price),
    status: '待审核',
    views: 0,
    description: publishForm.description,
    image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=480&q=80'
  })
  activeView.value = 'admin'
}

function auditProduct(product: Product, approved: boolean) {
  product.status = approved ? '在售' : '已驳回'
}

function createOrder(product: Product) {
  orders.value.unshift({
    id: Date.now(),
    no: `CT${new Date().toISOString().slice(0, 10).replaceAll('-', '')}${orders.value.length + 1}`,
    productTitle: product.title,
    buyer: '演示买家',
    seller: product.seller,
    amount: product.price,
    status: '待支付',
    remark: '订单已创建，等待模拟支付'
  })
  activeView.value = 'orders'
}

function nextOrderStep(order: Order) {
  if (order.status === '待支付') {
    order.status = '已支付'
    order.remark = '买家已完成模拟支付'
    return
  }
  if (order.status === '已支付') {
    order.status = '已发货'
    order.remark = '卖家已填写校内交付信息'
    return
  }
  if (order.status === '已发货') {
    order.status = '已完成'
    order.remark = '买家确认收货，交易闭环完成'
  }
}

function sendMessage() {
  messages.value.unshift({
    id: Date.now(),
    sender: '演示买家',
    receiver: '演示卖家',
    productTitle: '九成新机械键盘',
    content: messageDraft.value,
    unread: true
  })
  activeView.value = 'messages'
}

function markRead(message: Message) {
  message.unread = false
}
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">Campus C2C Marketplace</p>
        <h1>校园二手交易平台</h1>
      </div>
      <div class="user-chip">
        <span>管理员</span>
        <strong>答辩演示账号</strong>
      </div>
    </header>

    <section class="hero">
      <div class="hero-copy">
        <p class="summary">
          前后端分离的校园闲置物品交易系统，覆盖发布、审核、浏览、下单、模拟支付、发货、收货、留言和后台统计。
        </p>
        <div class="actions" aria-label="主要操作">
          <button type="button" @click="activeView = 'market'">浏览商品</button>
          <button type="button" class="secondary" @click="publishProduct">发布演示商品</button>
          <button type="button" class="ghost" @click="sendMessage">发送留言</button>
        </div>
      </div>
      <div class="hero-media" aria-label="演示流程">
        <div class="flow-step">发布商品</div>
        <div class="flow-step">管理员审核</div>
        <div class="flow-step">买家下单</div>
        <div class="flow-step">模拟支付</div>
        <div class="flow-step">发货收货</div>
      </div>
    </section>

    <section class="metrics" aria-label="数据概览">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.hint }}</small>
      </article>
    </section>

    <section class="workspace">
      <nav class="side-nav" aria-label="模块导航">
        <button type="button" :class="{ active: activeView === 'market' }" @click="activeView = 'market'">
          商品市场
        </button>
        <button type="button" :class="{ active: activeView === 'orders' }" @click="activeView = 'orders'">
          订单交易
        </button>
        <button type="button" :class="{ active: activeView === 'messages' }" @click="activeView = 'messages'">
          消息沟通
          <span v-if="unreadCount">{{ unreadCount }}</span>
        </button>
        <button type="button" :class="{ active: activeView === 'admin' }" @click="activeView = 'admin'">
          后台管理
        </button>
      </nav>

      <section v-if="activeView === 'market'" class="content-panel" aria-label="商品市场">
        <div class="section-title">
          <div>
            <h2>商品市场</h2>
            <p>只展示审核通过的商品，适合演示买家浏览和下单。</p>
          </div>
          <select v-model="publishForm.category" aria-label="发布分类">
            <option>数码配件</option>
            <option>图书教材</option>
            <option>生活用品</option>
          </select>
        </div>

        <div class="product-grid">
          <article v-for="product in approvedProducts" :key="product.id" class="product-card" data-test="product-card">
            <img :src="product.image" :alt="product.title">
            <div class="product-body">
              <div class="card-head">
                <h3>{{ product.title }}</h3>
                <span :class="['tag', statusClass(product.status)]">{{ product.status }}</span>
              </div>
              <p>{{ product.description }}</p>
              <dl>
                <div><dt>分类</dt><dd>{{ product.category }}</dd></div>
                <div><dt>成色</dt><dd>{{ product.condition }}</dd></div>
                <div><dt>卖家</dt><dd>{{ product.seller }}</dd></div>
              </dl>
              <div class="card-actions">
                <strong>￥{{ product.price.toFixed(2) }}</strong>
                <button type="button" @click="createOrder(product)">立即下单</button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section v-if="activeView === 'orders'" class="content-panel" aria-label="订单交易">
        <div class="section-title">
          <div>
            <h2>订单交易</h2>
            <p>按“支付、发货、完成”推进状态，展示完整交易闭环。</p>
          </div>
        </div>

        <div class="table-list">
          <article v-for="order in orders" :key="order.id" class="row-card" data-test="order-row">
            <div>
              <strong>{{ order.productTitle }}</strong>
              <p>{{ order.no }} · 买家 {{ order.buyer }} · 卖家 {{ order.seller }}</p>
            </div>
            <span :class="['tag', statusClass(order.status)]">{{ order.status }}</span>
            <strong>￥{{ order.amount.toFixed(2) }}</strong>
            <p>{{ order.remark }}</p>
            <button type="button" :disabled="order.status === '已完成'" @click="nextOrderStep(order)">
              推进状态
            </button>
          </article>
        </div>
      </section>

      <section v-if="activeView === 'messages'" class="content-panel" aria-label="消息沟通">
        <div class="section-title">
          <div>
            <h2>消息沟通</h2>
            <p>展示买卖双方留言和系统通知，支持未读标记。</p>
          </div>
          <button type="button" @click="sendMessage">新增留言</button>
        </div>

        <div class="message-list">
          <article v-for="message in messages" :key="message.id" class="message-card">
            <div class="card-head">
              <h3>{{ message.productTitle }}</h3>
              <span :class="['tag', message.unread ? 'warning' : 'success']">
                {{ message.unread ? '未读' : '已读' }}
              </span>
            </div>
            <p>{{ message.content }}</p>
            <small>{{ message.sender }} 发给 {{ message.receiver }}</small>
            <button type="button" class="secondary" @click="markRead(message)">标为已读</button>
          </article>
        </div>
      </section>

      <section v-if="activeView === 'admin'" class="content-panel" aria-label="后台管理">
        <div class="section-title">
          <div>
            <h2>后台管理</h2>
            <p>审核商品、查看分类占比，是答辩时最容易讲清楚权限控制的部分。</p>
          </div>
        </div>

        <div class="admin-layout">
          <div class="publish-box">
            <h3>快速发布商品</h3>
            <label>
              商品名称
              <input v-model="publishForm.title" type="text">
            </label>
            <label>
              价格
              <input v-model.number="publishForm.price" type="number" min="1">
            </label>
            <label>
              描述
              <textarea v-model="publishForm.description" rows="3" />
            </label>
            <button type="button" @click="publishProduct">提交待审核</button>
          </div>

          <div class="review-list">
            <article v-for="product in products" :key="product.id" class="review-card">
              <div>
                <strong>{{ product.title }}</strong>
                <p>{{ product.category }} · {{ product.seller }} · 浏览 {{ product.views }}</p>
              </div>
              <span :class="['tag', statusClass(product.status)]">{{ product.status }}</span>
              <div class="inline-actions">
                <button type="button" class="secondary" @click="auditProduct(product, true)">通过</button>
                <button type="button" class="danger" @click="auditProduct(product, false)">驳回</button>
              </div>
            </article>
          </div>

          <div class="chart-box">
            <h3>分类统计</h3>
            <div v-for="category in categories" :key="category.name" class="bar-line">
              <span>{{ category.name }}</span>
              <div><i :style="{ width: `${Math.max(category.count * 24, 12)}%` }" /></div>
              <strong>{{ category.count }}</strong>
            </div>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>
