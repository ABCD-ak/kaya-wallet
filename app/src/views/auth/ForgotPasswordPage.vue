<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { forgotPassword } from '@/api/auth'
import AuthPageShell from './components/AuthPageShell.vue'
import { authCopy } from './copy'

const router = useRouter()

type FieldState = 'idle' | 'valid' | 'invalid'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const ICON_OK = '/assets/ic-correct-g-1d.png'
const ICON_BAD = '/assets/ic-false-e-1e.png'

const email = ref('')
const touched = ref(false)
const submitting = ref(false)
const sent = ref(false)

const emailState = computed<FieldState>(() => {
  if (!email.value) return 'idle'
  if (EMAIL_RE.test(email.value.trim())) return 'valid'
  return touched.value ? 'invalid' : 'idle'
})

function goBack() {
  const previous = (window.history.state as { back?: string } | null)?.back
  if (previous) router.back()
  else router.replace('/login')
}

async function onSubmit() {
  const normalizedEmail = email.value.trim()
  touched.value = true

  if (!normalizedEmail) {
    ElMessage.warning(authCopy.errors.emailRequired)
    return
  }
  if (!EMAIL_RE.test(normalizedEmail)) {
    ElMessage.warning(authCopy.errors.emailInvalid)
    return
  }

  submitting.value = true
  try {
    await forgotPassword(normalizedEmail)
    email.value = normalizedEmail
    sent.value = true
  } catch {
    // 请求层已经展示错误信息
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthPageShell @back="goBack">
    <h1 class="auth-title">{{ authCopy.forgot.title }}</h1>

    <form v-if="!sent" class="auth-form" novalidate @submit.prevent="onSubmit">
      <label class="auth-field" :class="{ 'auth-field--error': emailState === 'invalid' }">
        <span class="auth-field__label">{{ authCopy.forgot.email }}</span>
        <span class="auth-field__control">
          <input
            v-model="email"
            class="auth-field__input"
            type="email"
            inputmode="email"
            autocomplete="email"
            :placeholder="authCopy.forgot.emailPlaceholder"
            @blur="touched = true"
          />
          <img
            v-if="emailState !== 'idle'"
            class="auth-field__icon"
            :src="emailState === 'valid' ? ICON_OK : ICON_BAD"
            alt=""
          />
        </span>
      </label>

      <button class="auth-submit" type="submit" :disabled="submitting">
        {{ submitting ? authCopy.forgot.sending : authCopy.forgot.submit }}
      </button>
    </form>

    <div v-else class="auth-done">
      <span class="auth-done__icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 6h16v12H4z" />
          <path d="m4 7 8 6 8-6" />
        </svg>
      </span>
      <p class="auth-done__title">{{ authCopy.forgot.sentTitle }}</p>
      <p class="auth-done__desc">{{ authCopy.forgot.sentDesc }}</p>
      <p class="auth-done__email">{{ email }}</p>

      <button class="auth-submit" type="button" @click="router.replace('/login')">
        {{ authCopy.forgot.backToLogin }}
      </button>
      <button class="auth-link" type="button" @click="sent = false">
        {{ authCopy.forgot.resend }}
      </button>
    </div>
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

.auth-done {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 60px;
  text-align: center;
}

.auth-done__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  border-radius: 50%;
  background: #f0fffb;
  color: #17b7bd;
}

.auth-done__icon svg {
  width: 30px;
  height: 30px;
}

.auth-done__title {
  margin: 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 20px;
  font-weight: 600;
  line-height: 30px;
  color: #353333;
}

.auth-done__desc {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 23px;
  color: #696565;
}

.auth-done__email {
  margin: 4px 0 0;
  font-size: 15px;
  font-weight: 500;
  line-height: 25px;
  color: #17b7bd;
  word-break: break-all;
}

.auth-link {
  margin-top: 16px;
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 23px;
  color: #696565;
  cursor: pointer;
}
</style>
