import request from '@/utils/request'
import type { UpdateProfilePayload, User } from './types'

export function getMe(): Promise<User> {
  return request.get<User>('/users/me')
}

export function updateMe(payload: UpdateProfilePayload): Promise<User> {
  return request.patch<User>('/users/me', payload)
}

export function uploadAvatar(file: File): Promise<{ avatarUrl: string }> {
  const form = new FormData()
  form.append('file', file)
  return request.post<{ avatarUrl: string }>('/users/me/avatar', form)
}

export interface PaymentPasswordStatus {
  enabled: boolean
}

export function getPaymentPasswordStatus(): Promise<PaymentPasswordStatus> {
  return request.get<PaymentPasswordStatus>('/users/me/payment-password')
}

export function setPaymentPassword(payload: {
  currentPassword?: string
  newPassword: string
}): Promise<void> {
  return request.post<void>('/users/me/payment-password', payload)
}

export function resetPaymentPassword(payload: {
  loginPassword: string
  newPassword: string
}): Promise<void> {
  return request.post<void>('/users/me/payment-password/reset', payload)
}
