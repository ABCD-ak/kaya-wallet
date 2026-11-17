import request from '@/utils/request'

export type VerificationType = 'identity' | 'address' | 'phone' | 'email'

export interface VerificationResponse {
  id: string
  type: VerificationType
  status: 'pending' | 'verified' | 'rejected' | string
  documentType?: string | null
  documentNoLast4?: string | null
  verifiedAt?: string | null
  rejectedReason?: string | null
  createdAt?: string
}

export interface CreateVerificationPayload {
  type: VerificationType
  documentType?: string
  documentNo?: string
}

export function listVerifications(): Promise<VerificationResponse[]> {
  return request.get<VerificationResponse[]>('/users/me/verifications')
}

export function submitVerification(
  payload: CreateVerificationPayload,
): Promise<VerificationResponse> {
  return request.post<VerificationResponse>('/users/me/verifications', payload)
}
