export type ProductStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'OFF_SHELF' | 'SOLD' | 'DELETED'

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELED'

export type MessageType = 'MESSAGE' | 'NOTIFICATION'

export interface ProductCard {
  id: number
  title: string
  categoryName: string
  sellerNickname: string
  price: number
  status: ProductStatus
  viewCount: number
  coverUrl: string | null
  itemCondition: string
  createdAt: string
}

export interface RecommendedProduct {
  id: number
  title: string
  categoryName: string
  price: number
  imageUrl: string | null
  viewCount: number
  score: number
  reason: string
}

export interface ProductDetail {
  id: number
  sellerId: number
  sellerNickname: string
  sellerAvatarUrl: string | null
  sellerProductCount: number
  categoryId: number
  categoryName: string
  title: string
  description: string
  price: number
  itemCondition: string
  status: ProductStatus
  auditReason: string | null
  viewCount: number
  images: string[]
  favorite: boolean
  createdAt: string
}

export interface OrderView {
  id: number
  orderNo: string
  productId: number
  productTitle: string
  buyerId: number
  buyerNickname: string
  sellerId: number
  sellerNickname: string
  totalAmount: number
  status: OrderStatus
  logisticsInfo: string | null
  version: number
  createdAt: string
}

export interface SiteMessage {
  id: number
  senderId: number
  senderNickname: string
  receiverId: number
  receiverNickname: string
  productId: number | null
  productTitle: string | null
  content: string
  readStatus: 'UNREAD' | 'READ'
  createdAt: string
}

export interface Notification {
  id: number
  type: string
  title: string
  content: string
  readStatus: 'UNREAD' | 'READ'
  createdAt: string
}

export interface Announcement {
  id: number
  title: string
  content: string
  createdAt: string
}

export interface DashboardView {
  userCount: number
  productCount: number
  orderCount: number
  turnover: number
  categories: CategoryStat[]
}

export interface CategoryStat {
  categoryName: string
  productCount: number
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  phone: string | null
  email: string | null
  avatarUrl: string | null
  role: string
  creditScore: number
  realName: string | null
  idCardNo: string | null
  realNameStatus: 'UNVERIFIED' | 'PENDING' | 'VERIFIED' | 'REJECTED'
}

export interface MyProduct {
  id: number
  title: string
  description: string
  price: number
  itemCondition: string
  status: ProductStatus
  viewCount: number
  categoryName: string
  sellerNickname: string
  coverUrl: string | null
  createdAt: string
  favoriteCount: number
  auditReason: string | null
}

export interface UserReport {
  id: number
  reporterId: number
  reporterNickname: string
  productId: number
  productTitle: string
  sellerNickname: string
  reason: string
  reportStatus: string
  handlingResult: string | null
  createdAt: string
  processedAt: string | null
}

export interface AddressView {
  id: number
  receiverName: string
  receiverPhone: string
  detailAddress: string
  defaultAddress: boolean
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
