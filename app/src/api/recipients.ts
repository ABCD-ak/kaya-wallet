import request from '@/utils/request'
import type { PageResult, RecipientResponse } from './types'

export function listRecipients(): Promise<RecipientResponse[]> {
  return request.get<PageResult<RecipientResponse>>('/recipients')
    .then((page) => page.items ?? [])
}

export interface CreateRecipientPayload {
  displayName: string
  accountNo: string
  bankName?: string
}

export function createRecipient(payload: CreateRecipientPayload): Promise<RecipientResponse> {
  return request.post<RecipientResponse>('/recipients', payload)
}
