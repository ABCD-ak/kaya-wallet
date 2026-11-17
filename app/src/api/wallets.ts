import request from '@/utils/request'
import type { WalletResponse, WalletSummaryResponse } from './types'

export function listWallets(): Promise<WalletResponse[]> {
  return request.get<WalletResponse[]>('/wallets')
}

export function getWalletSummary(walletId: string, month?: string): Promise<WalletSummaryResponse> {
  const params = month ? { month } : undefined
  return request.get<WalletSummaryResponse>(`/wallets/${walletId}/summary`, params)
}
