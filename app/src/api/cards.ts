import request from '@/utils/request'
import type { CardResponse, CreateCardPayload } from './types'

export function listCards(walletId?: string, status?: string): Promise<CardResponse[]> {
  const params: Record<string, unknown> = {}
  if (walletId) params.walletId = walletId
  if (status) params.status = status
  return request.get<CardResponse[]>('/cards', params)
}

export function createCard(payload: CreateCardPayload): Promise<CardResponse> {
  return request.post<CardResponse>('/cards', payload)
}

export function removeCard(id: string): Promise<void> {
  return request.delete<void>(`/cards/${id}`)
}
