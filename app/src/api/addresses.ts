import request from '@/utils/request'

export interface AddressResponse {
  id: string
  label?: string | null
  recipientName: string
  phone: string
  countryCode: string
  province?: string | null
  city?: string | null
  district?: string | null
  detailAddress: string
  postalCode?: string | null
  isDefault: boolean
  createdAt?: string
  updatedAt?: string
}

export interface AddressPayload {
  label?: string
  recipientName: string
  phone: string
  countryCode: string
  province?: string
  city?: string
  district?: string
  detailAddress: string
  postalCode?: string
  isDefault?: boolean
}

export function listAddresses(): Promise<AddressResponse[]> {
  return request.get<AddressResponse[]>('/addresses')
}

export function createAddress(payload: AddressPayload): Promise<AddressResponse> {
  return request.post<AddressResponse>('/addresses', payload)
}

export function updateAddress(
  id: string,
  payload: Partial<AddressPayload>,
): Promise<AddressResponse> {
  return request.patch<AddressResponse>(`/addresses/${id}`, payload)
}

export function removeAddress(id: string): Promise<void> {
  return request.delete<void>(`/addresses/${id}`)
}
