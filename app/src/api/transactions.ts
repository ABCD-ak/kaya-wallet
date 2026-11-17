import request from '@/utils/request'
import type {
  CreateTransactionPayload,
  PageResult,
  TransactionListParams,
  TransactionResponse,
  TransactionSummaryResponse,
} from './types'

export function listTransactions(params: TransactionListParams): Promise<PageResult<TransactionResponse>> {
  return request.get<PageResult<TransactionResponse>>('/transactions', params as unknown as Record<string, unknown>)
}

export function getTransaction(id: string): Promise<TransactionResponse> {
  return request.get<TransactionResponse>(`/transactions/${id}`)
}

export function getTransactionSummary(month?: string, walletId?: string): Promise<TransactionSummaryResponse> {
  const params: Record<string, unknown> = {}
  if (month) params.month = month
  if (walletId) params.walletId = walletId
  return request.get<TransactionSummaryResponse>('/transactions/summary', params)
}

function genIdempotencyKey(): string {
  return `tx_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

export function createTransaction(payload: CreateTransactionPayload): Promise<TransactionResponse> {
  return request.post<TransactionResponse>('/transactions', payload, {
    headers: { 'Idempotency-Key': genIdempotencyKey() },
  })
}
