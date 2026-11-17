import request from '@/utils/request'
import type { PageResult } from './types'

/** 与后端 NotificationResponse 字段保持一致 */
export interface NotificationResponse {
  id: string
  /** transfer_received / payment_received / budget_alert / income / system */
  type: string
  title: string
  body: string
  amount: string | null
  currencyCode: string | null
  relatedTransactionId: string | null
  isRead: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationQuery {
  isRead?: boolean
  page?: number
  pageSize?: number
}

export function listNotifications(params: NotificationQuery = {}): Promise<PageResult<NotificationResponse>> {
  const query: Record<string, unknown> = {
    page: params.page ?? 1,
    pageSize: params.pageSize ?? 50,
  }
  if (params.isRead !== undefined) query.isRead = params.isRead
  return request.get<PageResult<NotificationResponse>>('/notifications', query)
}

export function getUnreadCount(): Promise<{ count: number }> {
  return request.get<{ count: number }>('/notifications/unread-count')
}

export function markAsRead(id: string): Promise<NotificationResponse> {
  return request.patch<NotificationResponse>(`/notifications/${id}/read`)
}

export function markAllAsRead(): Promise<{ updatedCount: number }> {
  return request.post<{ updatedCount: number }>('/notifications/read-all')
}
