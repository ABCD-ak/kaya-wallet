<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import { resetPassword } from '@/api/auth'
import AuthPageShell from './components/AuthPageShell.vue'
import { authCopy } from './copy'

const route = useRoute()
const router = useRouter()

type FieldState = 'idle' | 'valid' | 'invalid'

const ICON_OK = '/assets/ic-correct-g-1d.png'
const ICON_BAD = '/assets/ic-false-e-1e.png'

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))

const password = ref('')
const confirmPassword = ref('')
const touched = ref(false)
const submitting = ref(false)
const done = ref(false)
const failed = ref(false)

const passwordState = ref<FieldState>('idle')
const confirmState = ref<FieldState>('idle')

function passwordValid(value: string): boolean {
  return value.length >= 8 && value.length <= 64
}

function syncStates(): void {
  passwordState.value = !password.value
    ? 'idle'
    : passwordValid(password.value)
      ? 'valid'
      : touched.value
        ? 'invalid'
        : 'idle'

  if (!confirmPassword.value) {
    confirmState.value = 'idle'
  } else if (passwordValid(confirmPassword.value) && confirmPassword.value === password.value) {
    confirmState.value = 'valid'
  } else {
    confirmState.value = touched.value ? 'invalid' : 'idle'
  }
}

watch(password, syncStates)

function goBack() {
  router.replace('/login')
}

async function onSubmit() {
  touched.value = true
  syncStates()

  if (!token.value) {
    failed.value = true
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
    await resetPassword(token.value, password.value)
    done.value = true
  } catch {
    failed.value = true
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthPageShell @back="goBack">
    <h1 class="auth-title">{{ authCopy.reset.title }}</h1>

    <!-- 链接无效 / 已过期 -->
    <div v-if="!token || failed" class="auth-done">
      <p class="auth-done__title">{{ authCopy.reset.invalidTitle }}</p>
      <p class="auth-done__desc">{{ authCopy.reset.invalidDesc }}</p>
      <button class="auth-submit" type="button" @click="router.replace('/forgot-password')">
        {{ authCopy.reset.retry }}
      </button>
      <button class="auth-link" type="button" @click="router.replace('/login')">
        {{ authCopy.reset.goLogin }}
      </button>
    </div>

    <!-- 设置成功 -->
    <div v-else-if="done" class="auth-done">
      <span class="auth-done__icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m5 12 4.5 4.5L19 7" />
        </svg>
      </span>
      <p class="auth-done__title">{{ authCopy.reset.doneTitle }}</p>
      <p class="auth-done__desc">{{ authCopy.reset.doneDesc }}</p>
      <button class="auth-submit" type="button" @click="router.replace('/login')">
        {{ authCopy.reset.goLogin }}
      </button>
    </div>

    <!-- 设置新密码 -->
    <form v-else class="auth-form" novalidate @submit.prevent="onSubmit">
      <label class="auth-field" :class="{ 'auth-field--error': passwordState === 'invalid' }">
        <span class="auth-field__label">{{ authCopy.reset.password }}</span>
        <span class="auth-field__control">
          <input
            v-model="password"
            class="auth-field__input"
            type="password"
            autocomplete="new-password"
            maxlength="64"
            :placeholder="authCopy.reset.passwordPlaceholder"
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
        <span class="auth-field__label">{{ authCopy.reset.confirmPassword }}</span>
        <span class="auth-field__control">
          <input
            v-model="confirmPassword"
            class="auth-field__input"
            type="password"
            autocomplete="new-password"
            maxlength="64"
            :placeholder="authCopy.reset.confirmPasswordPlaceholder"
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
        {{ authCopy.reset.submit }}
      </button>
    </form>
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
