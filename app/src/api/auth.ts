import request from '@/utils/request'
import type { AuthTokens, LoginPayload, RegisterPayload } from './types'

export function login(payload: LoginPayload): Promise<AuthTokens> {
  return request.post<AuthTokens>('/auth/login', payload)
}

/** 发送邮箱登录验证码（真实邮件） */
export function sendEmailCode(email: string): Promise<void> {
  return request.post<void>('/auth/email-code', { email })
}

/** 邮箱验证码登录 */
export function loginWithEmailCode(email: string, code: string): Promise<AuthTokens> {
  return request.post<AuthTokens>('/auth/login/email', { email, code })
}

export function register(payload: RegisterPayload): Promise<AuthTokens> {
  return request.post<AuthTokens>('/auth/register', payload)
}

export function logout(refreshToken?: string): Promise<void> {
  return request.post<void>('/auth/logout', refreshToken ? { refreshToken } : {})
}

/** 发送重置密码邮件（后端未接邮件服务时，链接写入服务端日志） */
export function forgotPassword(email: string): Promise<void> {
  return request.post<void>('/auth/forgot-password', { email })
}

/** 用邮件里的 token 设置新密码 */
export function resetPassword(token: string, newPassword: string): Promise<void> {
  return request.post<void>('/auth/reset-password', { token, newPassword })
}
