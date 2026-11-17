<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { register as registerRequest, logout as logoutRequest } from '@/api/auth'
import AuthPageShell from './components/AuthPageShell.vue'
import { authCopy } from './copy'

const router = useRouter()

type FieldState = 'idle' | 'valid' | 'invalid'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const ICON_OK = '/assets/ic-correct-g-1d.png'
const ICON_BAD = '/assets/ic-false-e-1e.png'

const displayName = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)
const touched = ref(false)

const nameState = ref<FieldState>('idle')
const emailState = ref<FieldState>('idle')
const passwordState = ref<FieldState>('idle')
const confirmState = ref<FieldState>('idle')

function emailValid(value: string): boolean {
  return EMAIL_RE.test(value.trim())
}

function passwordValid(value: string): boolean {
  return value.length >= 8 && value.length <= 64
}

function stateOf(valid: boolean, filled: boolean): FieldState {
  if (!filled) return 'idle'
  if (valid) return 'valid'
  return touched.value ? 'invalid' : 'idle'
}

function syncStates(): void {
  nameState.value = stateOf(
    displayName.value.trim().length > 0 && displayName.value.trim().length <= 64,
    displayName.value.length > 0,
  )
  emailState.value = stateOf(emailValid(email.value), email.value.length > 0)
  passwordState.value = stateOf(passwordValid(password.value), password.value.length > 0)
  confirmState.value = stateOf(
    passwordValid(confirmPassword.value) && confirmPassword.value === password.value,
    confirmPassword.value.length > 0,
  )
}

// 密码变化时同步校验确认密码
watch(password, syncStates)

function goBack() {
  const previous = (window.history.state as { back?: string } | null)?.back
  if (previous) router.back()
  else router.replace('/auth')
}

async function onSubmit() {
  const normalizedName = displayName.value.trim()
  const normalizedEmail = email.value.trim()

  touched.value = true
  syncStates()

  if (!normalizedName) {
    ElMessage.warning(authCopy.errors.displayNameRequired)
    return
  }
  if (normalizedName.length > 64) {
    ElMessage.warning(authCopy.errors.displayNameTooLong)
    return
  }
  if (!normalizedEmail) {
    ElMessage.warning(authCopy.errors.emailRequired)
    return
  }
  if (!emailValid(normalizedEmail)) {
    ElMessage.warning(authCopy.errors.emailInvalid)
    return
  }
  if (!passwordValid(password.value)) {
    ElMessage.warning(authCopy.errors.passwordLength)
    return
  }
  if (password.value !== confirmPassword.value) {
    ElMessage.warning(authCopy.errors.passwordMismatch)
    return
  }

  submitting.value = true
  try {
    const tokens = await registerRequest({
      displayName: normalizedName,
      email: normalizedEmail,
      password: password.value,
    })
    // 后端注册接口会顺带签发令牌；这里不保留登录态，并把这次会话注销掉
    if (tokens?.refreshToken) {
      logoutRequest(tokens.refreshToken).catch(() => {
        /* 注销失败不影响注册结果 */
      })
    }
    ElMessage.success(authCopy.register.success)
    await router.replace({ path: '/login', query: { email: normalizedEmail } })
  } catch {
    // 请求层已经展示错误信息
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthPageShell @back="goBack">
    <h1 class="auth-title">{{ authCopy.register.title }}</h1>

    <form class="auth-form" novalidate @submit.prevent="onSubmit">
      <label class="auth-field" :class="{ 'auth-field--error': nameState === 'invalid' }">
        <span class="auth-field__label">{{ authCopy.register.displayName }}</span>
        <span class="auth-field__control">
          <input
            v-model="displayName"
            class="auth-field__input"
            type="text"
            autocomplete="name"
            maxlength="64"
            :placeholder="authCopy.register.displayNamePlaceholder"
            @input="syncStates"
            @blur="touched = true; syncStates()"
          />
          <img
            v-if="nameState !== 'idle'"
            class="auth-field__icon"
            :src="nameState === 'valid' ? ICON_OK : ICON_BAD"
            alt=""
          />
        </span>
      </label>

      <label class="auth-field" :class="{ 'auth-field--error': emailState === 'invalid' }">
        <span class="auth-field__label">{{ authCopy.register.email }}</span>
        <span class="auth-field__control">
          <input
            v-model="email"
            class="auth-field__input"
            type="email"
            inputmode="email"
            autocomplete="email"
            :placeholder="authCopy.register.emailPlaceholder"
            @input="syncStates"
            @blur="touched = true; syncStates()"
          />
          <img
            v-if="emailState !== 'idle'"
            class="auth-field__icon"
            :src="emailState === 'valid' ? ICON_OK : ICON_BAD"
            alt=""
          />
        </span>
      </label>

      <label class="auth-field" :class="{ 'auth-field--error': passwordState === 'invalid' }">
        <span class="auth-field__label">{{ authCopy.register.password }}</span>
        <span class="auth-field__control">
          <input
            v-model="password"
            class="auth-field__input"
            type="password"
            autocomplete="new-password"
            maxlength="64"
            :placeholder="authCopy.register.passwordPlaceholder"
            @input="syncStates"
            @blur="touched = true; syncStates()"
          />
          <img
            v-if="passwordState !== 'idle'"
            class="auth-field__icon"
            :src="passwordState === 'valid' ? ICON_OK : ICON_BAD"
            alt=""
          />
        </span>
      </label>

      <label class="auth-field" :class="{ 'auth-field--error': confirmState === 'invalid' }">
        <span class="auth-field__label">{{ authCopy.register.confirmPassword }}</span>
        <span class="auth-field__control">
          <input
            v-model="confirmPassword"
            class="auth-field__input"
            type="password"
            autocomplete="new-password"
            maxlength="64"
            :placeholder="authCopy.register.confirmPasswordPlaceholder"
            @input="syncStates"
            @blur="touched = true; syncStates()"
          />
          <img
            v-if="confirmState !== 'idle'"
            class="auth-field__icon"
            :src="confirmState === 'valid' ? ICON_OK : ICON_BAD"
            alt=""
          />
        </span>
      </label>

      <button class="auth-submit" type="submit" :disabled="submitting">
        {{ authCopy.register.submit }}
      </button>
    </form>

    <p class="auth-switch">
      {{ authCopy.register.hasAccount }}
      <button type="button" @click="router.push('/login')">{{ authCopy.register.login }}</button>
    </p>
  </AuthPageShell>
</template>

<style scoped lang="scss">
@use './forms';

.auth-form {
  margin-top: 60px;
}

.auth-submit {
  margin-top: 40px;
}
</style>
