import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import * as usersApi from '@/api/users'
import type {
  AuthTokens,
  LoginPayload,
  RegisterPayload,
  UpdateProfilePayload,
  User,
} from '@/api/types'
import {
  clearAuthStorage,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken,
} from '@/utils/auth'

const USER_KEY = 'kaya_user'

function readStoredUser(): User | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

function persistUser(user: User | null): void {
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(USER_KEY)
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(getToken())
  const refreshToken = ref(getRefreshToken())
  const user = ref<User | null>(readStoredUser())

  const isAuthenticated = computed(() => !!accessToken.value)

  function setUser(next: User | null): void {
    user.value = next
    persistUser(next)
  }

  function applyTokens(tokens: AuthTokens): void {
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    setToken(tokens.accessToken)
    setRefreshToken(tokens.refreshToken)
    setUser(tokens.user)
  }

  function reset(): void {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    clearAuthStorage()
  }

  async function login(payload: LoginPayload): Promise<User> {
    const tokens = await authApi.login(payload)
    applyTokens(tokens)
    return tokens.user
  }

  async function loginWithEmailCode(email: string, code: string): Promise<User> {
    const tokens = await authApi.loginWithEmailCode(email, code)
    applyTokens(tokens)
    return tokens.user
  }

  async function register(payload: RegisterPayload): Promise<User> {
    const tokens = await authApi.register(payload)
    applyTokens(tokens)
    return tokens.user
  }

  async function fetchMe(): Promise<User> {
    const me = await usersApi.getMe()
    setUser(me)
    return me
  }

  async function updateProfile(payload: UpdateProfilePayload): Promise<User> {
    const me = await usersApi.updateMe(payload)
    setUser(me)
    return me
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout(refreshToken.value || undefined)
    } finally {
      reset()
    }
  }

  return {
    accessToken,
    refreshToken,
    user,
    isAuthenticated,
    applyTokens,
    setUser,
    reset,
    login,
    loginWithEmailCode,
    register,
    fetchMe,
    updateProfile,
    logout,
  }
})
