// ============== 通用 ==============

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

// ============== User / Auth ==============

export interface User {
  id: string
  email: string
  phone?: string
  displayName: string
  username: string
  avatarUrl?: string
  gender?: string
  birthday?: string
  emailVerified: boolean
  phoneVerified: boolean
  status: string
  createdAt: string
  updatedAt: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  displayName: string
  email: string
  password: string
  username?: string
}

export interface UpdateProfilePayload {
  displayName?: string
  username?: string
  gender?: string
  birthday?: string
}

// ============== Wallet ==============

export interface WalletResponse {
  id: string
  currencyCode: string
  balance: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface WalletSummaryResponse {
  walletId: string
  currencyCode: string
  balance: string
  month: string
  income: string
  spent: string
}

// ============== Category ==============

export interface Category {
  id: string
  code: string
  type: 'income' | 'expense'
  name: string
  icon?: string
  color?: string
  sortOrder?: number
  isActive?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface CategoryRef {
  id: string
  code: string
  type: string
  name: string
  icon?: string
  color?: string
  sortOrder?: number
}

// ============== Transaction ==============

export type TransactionType = 'income' | 'expense' | 'transfer_out' | 'transfer_in' | 'refund'
export type TransactionDirection = 'credit' | 'debit'
export type TransactionStatus = 'pending' | 'success' | 'failed' | 'cancelled'

export interface CounterpartyResponse {
  userId?: string
  name: string
  accountNo?: string
  bankName?: string
}

export interface TransactionResponse {
  id: string
  referenceNo: string
  walletId: string
  type: TransactionType
  direction: TransactionDirection
  status: TransactionStatus
  amount: string
  feeAmount: string
  currencyCode: string
  category?: CategoryRef
  counterparty?: CounterpartyResponse
  recipientId?: string
  relatedTransactionId?: string
  note?: string
  occurredAt: string
  createdAt: string
}

export interface CreateTransactionPayload {
  walletId: string
  type: 'income' | 'expense'
  amount: string
  categoryId: string
  note?: string
  occurredAt?: string
}

export interface TransactionListParams {
  walletId?: string
  type?: string
  direction?: string
  categoryId?: string
  from?: string
  to?: string
  page?: number
  pageSize?: number
}

export interface CategorySpendResponse {
  categoryId: string
  categoryName: string
  categoryColor?: string
  amount: string
  percentage: number
}

export interface TransactionSummaryResponse {
  month: string
  walletId: string
  currencyCode: string
  totalIncome: string
  totalSpent: string
  net: string
  byCategory: CategorySpendResponse[]
}

// ============== Transfer ==============

export interface TransferQuotePayload {
  walletId: string
  cardId?: string
  recipientId?: string
  accountNo?: string
  amount: string
}

export interface TransferQuoteResponse {
  walletId: string
  currencyCode: string
  amount: string
  feeRate: string
  feeAmount: string
  totalAmount: string
  availableBalance: string
  sufficientBalance: boolean
}

export interface TransferRecipientInput {
  displayName?: string
  accountNo?: string
  bankName?: string
}

export interface CreateTransferPayload {
  walletId: string
  cardId?: string
  recipientId?: string
  recipient?: TransferRecipientInput
  amount: string
  note?: string
  paymentPassword?: string
}

export interface ReceiptResponse {
  referenceNo: string
  amount: string
  feeAmount: string
  totalAmount: string
  currencyCode: string
  senderName?: string
  recipientName?: string
  status: string
  occurredAt: string
}

export interface TransferResponse {
  id: string
  referenceNo: string
  walletId: string
  status: string
  amount: string
  feeAmount: string
  totalAmount: string
  currencyCode: string
  recipient: CounterpartyResponse
  note?: string
  occurredAt: string
  relatedTransactionId?: string
  receipt?: ReceiptResponse
}

// ============== Card ==============

export type CardType = 'debit' | 'credit'
export type CardBrand =
  | 'visa'
  | 'mastercard'
  | 'unionpay'
  | 'icbc'
  | 'abc'
  | 'boc'
  | 'ccb'
  | 'bocom'
  | 'psbc'
export type CardStatus = 'active' | 'expired' | 'frozen' | 'removed'

export interface CardResponse {
  id: string
  walletId?: string
  cardType: CardType
  brand: CardBrand
  last4: string
  holderName: string
  expiryMonth: number
  expiryYear: number
  currencyCode: string
  balance: string
  creditLimit?: string
  isPrimary: boolean
  status: CardStatus
  createdAt: string
  updatedAt: string
}

export interface CreateCardPayload {
  cardType: CardType
  brand: CardBrand
  last4: string
  holderName: string
  expiryMonth: number
  expiryYear: number
  currencyCode: string
  paymentToken?: string
  isPrimary?: boolean
}

// ============== Recipient ==============

export interface RecipientResponse {
  id: string
  displayName: string
  phone?: string
  email?: string
  accountNo?: string
  bankName?: string
  avatarUrl?: string
  isFavorite: boolean
  lastUsedAt?: string
  createdAt: string
}
