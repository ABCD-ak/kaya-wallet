import request from '@/utils/request'
import type {
  CreateTransferPayload,
  TransferQuotePayload,
  TransferQuoteResponse,
  TransferResponse,
} from './types'

export function quoteTransfer(payload: TransferQuotePayload): Promise<TransferQuoteResponse> {
  return request.post<TransferQuoteResponse>('/transfers/quote', payload)
}

function genIdempotencyKey(): string {
  return `tf_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

export function createTransfer(payload: CreateTransferPayload): Promise<TransferResponse> {
  return request.post<TransferResponse>('/transfers', payload, {
    headers: { 'Idempotency-Key': genIdempotencyKey() },
  })
}

export function getTransfer(id: string): Promise<TransferResponse> {
  return request.get<TransferResponse>(`/transfers/${id}`)
}
