<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import StatusBar from '@/components/StatusBar.vue'
import { sendEmailCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { authCopy } from './copy'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

type FieldState = 'idle' | 'valid' | 'invalid'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const ICON_OK = '/assets/ic-correct-g-1d.png'
const ICON_BAD = '/assets/ic-false-e-1e.png'

const email = ref(typeof route.query.email === 'string' ? route.query.email : '')
const password = ref('')
const submitting = ref(false)

type LoginMode = 'password' | 'code'
const mode = ref<LoginMode>('password')
const code = ref('')
const sending = ref(false)
const codeSubmitting = ref(false)
const countdown = ref(0)
let countdownTimer: number | undefined

const canSendCode = computed(
  () => countdown.value === 0 && !sending.value && emailValid(email.value),
)

const sendLabel = computed(() => {
  if (countdown.value > 0) return `${countdown.value}s 后重发`
  if (sending.value) return '发送中…'
  return '获取验证码'
})

const emailTouched = ref(false)
const passwordTouched = ref(false)
const emailState = ref<FieldState>('idle')
const passwordState = ref<FieldState>('idle')

function emailValid(value: string): boolean {
  return EMAIL_RE.test(value.trim())
}

function passwordValid(value: string): boolean {
  return value.length >= 8 && value.length <= 64
}

function syncEmailState(): void {
  if (!email.value) {
    emailState.value = 'idle'
    return
  }
  if (emailValid(email.value)) emailState.value = 'valid'
  else emailState.value = emailTouched.value ? 'invalid' : 'idle'
}

function syncPasswordState(): void {
  if (!password.value) {
    passwordState.value = 'idle'
    return
  }
  if (passwordValid(password.value)) passwordState.value = 'valid'
  else passwordState.value = passwordTouched.value ? 'invalid' : 'idle'
}

function onEmailBlur(): void {
  emailTouched.value = true
  syncEmailState()
}

function onPasswordBlur(): void {
  passwordTouched.value = true
  syncPasswordState()
}

// 从注册页跳过来时带上了邮箱，这里直接展示为已填写
if (email.value) {
  emailTouched.value = true
  syncEmailState()
}

function goBack() {
  const previous = (window.history.state as { back?: string } | null)?.back
  if (previous) router.back()
  else router.replace('/auth')
}

async function onSubmit() {
  const normalizedEmail = email.value.trim()

  emailTouched.value = true
  passwordTouched.value = true
  syncEmailState()
  syncPasswordState()

  if (!normalizedEmail) {
    ElMessage.warning(authCopy.errors.emailRequired)
    return
  }
  if (!emailValid(normalizedEmail)) {
    ElMessage.warning(authCopy.errors.emailInvalid)
    return
  }
  if (!password.value) {
    ElMessage.warning(authCopy.errors.passwordRequired)
    return
  }
  if (!passwordValid(password.value)) {
    ElMessage.warning(authCopy.errors.passwordLength)
    return
  }

  submitting.value = true
  try {
    await auth.login({ email: normalizedEmail, password: password.value })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/wallet'
    await router.replace(redirect)
  } catch {
    // 登录失败：按设计稿把密码框标为错误态
    passwordState.value = 'invalid'
  } finally {
    submitting.value = false
  }
}

function switchMode(next: LoginMode) {
  mode.value = next
  passwordState.value = 'idle'
}

function onCodeInput(event: Event) {
  const input = event.target as HTMLInputElement
  const digits = input.value.replace(/\D/g, '').slice(0, 6)
  code.value = digits
  input.value = digits
}

function startCountdown() {
  countdown.value = 60
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

onUnmounted(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})

async function onSendCode() {
  const normalizedEmail = email.value.trim()

  emailTouched.value = true
  syncEmailState()

  if (!normalizedEmail) {
    ElMessage.warning(authCopy.errors.emailRequired)
    return
  }
  if (!emailValid(normalizedEmail)) {
    ElMessage.warning(authCopy.errors.emailInvalid)
    return
  }
  if (!canSendCode.value) {
    return
  }

  sending.value = true
  try {
    await sendEmailCode(normalizedEmail)
    ElMessage.success('验证码已发送，请查看邮箱')
    startCountdown()
  } catch (error) {
    // 太频繁时也进入倒计时，让按钮状态和实际可发送时间一致
    const code = (error as { response?: { data?: { code?: number } } }).response?.data?.code
    if (code === 40104) startCountdown()
  } finally {
    sending.value = false
  }
}

async function onCodeSubmit() {
  const normalizedEmail = email.value.trim()

  emailTouched.value = true
  syncEmailState()

  if (!normalizedEmail) {
    ElMessage.warning(authCopy.errors.emailRequired)
    return
  }
  if (!emailValid(normalizedEmail)) {
    ElMessage.warning(authCopy.errors.emailInvalid)
    return
  }
  if (!/^\d{6}$/.test(code.value)) {
    ElMessage.warning('请输入 6 位验证码')
    return
  }

  codeSubmitting.value = true
  try {
    await auth.loginWithEmailCode(normalizedEmail, code.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/wallet'
    await router.replace(redirect)
  } catch {
    // 请求层已统一提示错误
  } finally {
    codeSubmitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <StatusBar />

    <!-- 品牌丝带：与欢迎页同一根曲线，收细贴顶 -->
    <svg class="ribbon" viewBox="0 0 390 320" fill="none" aria-hidden="true">
      <defs>
        <linearGradient
          id="kaya-ribbon-login"
          x1="0"
          y1="40"
          x2="390"
          y2="150"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stop-color="#17b7bd" />
          <stop offset="1" stop-color="#9deb93" />
        </linearGradient>
      </defs>
      <path
        class="ribbon__band ribbon__band--a"
        d="M-70 120 C 80 10, 250 0, 450 106"
        stroke="#17b7bd"
        stroke-opacity=".13"
        stroke-width="84"
        stroke-linecap="round"
      />
      <path
        class="ribbon__band ribbon__band--b"
        d="M-70 168 C 90 60, 255 48, 450 150"
        stroke="#7ee39b"
        stroke-opacity=".15"
        stroke-width="40"
        stroke-linecap="round"
      />
      <path
        class="ribbon__band ribbon__band--c"
        d="M-70 142 C 92 34, 252 22, 450 126"
        stroke="url(#kaya-ribbon-login)"
        stroke-width="2.2"
        stroke-linecap="round"
        stroke-opacity=".85"
      />
    </svg>

    <main class="login-main">
      <div class="login-top">
        <button class="login-back" type="button" aria-label="返回" @click="goBack">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M15 18l-6-6 6-6"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
        <span class="login-brand">KAYA</span>
      </div>

      <h1 class="login-title">{{ authCopy.login.title }}</h1>
      <p class="login-subtitle">{{ authCopy.login.subtitle }}</p>

      <div class="login-mode" role="tablist" aria-label="登录方式">
        <button
          type="button"
          role="tab"
          :aria-selected="mode === 'password'"
          :class="{ 'is-active': mode === 'password' }"
          @click="switchMode('password')"
        >
          密码登录
        </button>
        <button
          type="button"
          role="tab"
          :aria-selected="mode === 'code'"
          :class="{ 'is-active': mode === 'code' }"
          @click="switchMode('code')"
        >
          验证码登录
        </button>
      </div>

      <form v-if="mode === 'password'" class="login-form" novalidate @submit.prevent="onSubmit">
        <label class="field" :class="{ 'field--error': emailState === 'invalid' }">
          <span class="field__label">{{ authCopy.login.email }}</span>
          <span class="field__box">
            <input
              v-model="email"
              class="field__input"
              type="email"
              inputmode="email"
              autocomplete="email"
              :placeholder="authCopy.login.emailPlaceholder"
              @input="syncEmailState"
              @blur="onEmailBlur"
            />
            <img
              v-if="emailState !== 'idle'"
              class="field__icon"
              :src="emailState === 'valid' ? ICON_OK : ICON_BAD"
              alt=""
            />
          </span>
        </label>

        <label class="field" :class="{ 'field--error': passwordState === 'invalid' }">
          <span class="field__label">{{ authCopy.login.password }}</span>
          <span class="field__box">
            <input
              v-model="password"
              class="field__input"
              type="password"
              autocomplete="current-password"
              :placeholder="authCopy.login.passwordPlaceholder"
              @input="syncPasswordState"
              @blur="onPasswordBlur"
            />
            <img
              v-if="passwordState !== 'idle'"
              class="field__icon"
              :src="passwordState === 'valid' ? ICON_OK : ICON_BAD"
              alt=""
            />
          </span>
        </label>

        <button class="login-forgot" type="button" @click="router.push('/forgot-password')">
          {{ authCopy.login.forgot }}
        </button>

        <button
          class="submit"
          :class="{ 'is-loading': submitting }"
          type="submit"
          :disabled="submitting"
        >
          <span class="submit__label">{{ authCopy.login.submit }}</span>
          <span v-if="submitting" class="submit__spinner" aria-hidden="true" />
        </button>
      </form>

      <form v-else class="login-form" novalidate @submit.prevent="onCodeSubmit">
        <label class="field" :class="{ 'field--error': emailState === 'invalid' }">
          <span class="field__label">{{ authCopy.login.email }}</span>
          <span class="field__box">
            <input
              v-model="email"
              class="field__input"
              type="email"
              inputmode="email"
              autocomplete="email"
              :placeholder="authCopy.login.emailPlaceholder"
              @input="syncEmailState"
              @blur="onEmailBlur"
            />
            <img
              v-if="emailState !== 'idle'"
              class="field__icon"
              :src="emailState === 'valid' ? ICON_OK : ICON_BAD"
              alt=""
            />
          </span>
        </label>

        <label class="field">
          <span class="field__label">验证码</span>
          <span class="field__box">
            <input
              class="field__input field__input--code"
              :value="code"
              type="text"
              inputmode="numeric"
              maxlength="6"
              autocomplete="one-time-code"
              placeholder="6 位数字"
              @input="onCodeInput"
            />
            <button
              class="code-send"
              type="button"
              :disabled="!canSendCode"
              @click.prevent="onSendCode"
            >
              {{ sendLabel }}
            </button>
          </span>
        </label>

        <p class="code-hint">验证码 5 分钟内有效，请留意收件箱和垃圾邮件</p>

        <button
          class="submit"
          :class="{ 'is-loading': codeSubmitting }"
          type="submit"
          :disabled="codeSubmitting"
        >
          <span class="submit__label">{{ authCopy.login.submit }}</span>
          <span v-if="codeSubmitting" class="submit__spinner" aria-hidden="true" />
        </button>
      </form>

      <p class="login-switch">
        {{ authCopy.login.noAccount }}
        <button type="button" @click="router.push('/register')">{{ authCopy.login.register }}</button>
      </p>

      <p class="login-safe">
        <svg width="11" height="13" viewBox="0 0 11 13" fill="none" aria-hidden="true">
          <rect x=".8" y="5.4" width="9.4" height="7" rx="2" stroke="#c2cac8" stroke-width="1.1" />
          <path
            d="M3.2 5.2V3.9a2.3 2.3 0 0 1 4.6 0v1.3"
            stroke="#c2cac8"
            stroke-width="1.1"
          />
        </svg>
        数据经加密传输
      </p>
    </main>

    <div class="login-home"><span /></div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--app-bg, #f7f6f8);
}

.login-page :deep(.status-bar) {
  position: relative;
  z-index: 4;
  background: transparent;
}

.ribbon {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
  width: 100%;
  height: 320px;
  pointer-events: none;
}

.ribbon__band {
  transform-box: fill-box;
  transform-origin: center;
  will-change: transform;
}

.login-main {
  position: relative;
  z-index: 2;
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 24px 28px 24px;
  overflow-y: auto;
  scrollbar-width: none;
}

.login-main::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.login-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  margin-left: -10px;
  padding: 0;
  border: 0;
  background: none;
  color: var(--app-ink, #1d2b29);
  cursor: pointer;
}

.login-brand {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.34em;
  color: #b3bcba;
}

.login-title {
  margin: 42px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 36px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: 0.1em;
  color: var(--app-ink, #1d2b29);
}

.login-subtitle {
  margin: 12px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  line-height: 22px;
  letter-spacing: 0.04em;
  color: #8b9694;
}

.login-form {
  display: flex;
  flex-direction: column;
  margin-top: 26px;
}

.login-mode {
  display: flex;
  gap: 4px;
  margin-top: 22px;
  padding: 4px;
  border-radius: 14px;
  background: #eef2f1;
}

.login-mode button {
  flex: 1;
  height: 38px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #8b9694;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.login-mode button.is-active {
  background: #fff;
  color: #1d2b29;
  box-shadow: 0 4px 10px rgba(16, 40, 38, 0.08);
}

.field {
  display: flex;
  flex-direction: column;
}

.field + .field {
  margin-top: 18px;
}

.field__label {
  margin-bottom: 9px;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  line-height: 18px;
  letter-spacing: 0.08em;
  color: #8b9694;
  transition: color 0.18s ease;
}

.field__box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px;
  border: 1px solid #ecefee;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 6px 14px rgba(16, 40, 38, 0.035);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.field:focus-within .field__box {
  border-color: #17b7bd;
  box-shadow: 0 0 0 3px rgba(23, 183, 189, 0.14), 0 10px 20px rgba(23, 183, 189, 0.1);
}

.field:focus-within .field__label {
  color: #17b7bd;
}

.field--error .field__box,
.field--error:focus-within .field__box {
  border-color: #ef6874;
  box-shadow: 0 0 0 3px rgba(239, 104, 116, 0.12);
}

.field--error .field__label {
  color: #ef6874;
}

.field__input {
  flex: 1;
  min-width: 0;
  width: 100%;
  padding: 0;
  border: 0;
  outline: 0;
  background: transparent;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15.5px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--app-ink, #1d2b29);
}

.field__input::placeholder {
  font-weight: 400;
  color: #b3bcba;
}

.field__input:-webkit-autofill {
  -webkit-box-shadow: 0 0 0 40px #fff inset;
  -webkit-text-fill-color: #1d2b29;
}

.field__icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  margin-left: 8px;
  user-select: none;
}

.field__input--code {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.32em;
}

.field__input--code::placeholder {
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0.02em;
}

.code-send {
  flex: none;
  margin-left: 10px;
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  color: #17b7bd;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.18s ease;
}

.code-send:disabled {
  color: #b3bcba;
  cursor: default;
}

.code-hint {
  margin: 10px 2px 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 11px;
  line-height: 18px;
  color: #a7b0ae;
}

.login-forgot {
  display: block;
  margin: 14px 0 0 auto;
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 12.5px;
  line-height: 20px;
  letter-spacing: 0.02em;
  color: #7d8987;
  cursor: pointer;
  transition: color 0.18s ease;
}

.login-forgot:hover {
  color: #17b7bd;
}

.submit {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 54px;
  margin-top: 32px;
  overflow: hidden;
  border: 0;
  border-radius: 16px;
  background-image: linear-gradient(120deg, #12aeb4 0%, #17b7bd 44%, #7ee39b 100%);
  background-size: 180% 180%;
  box-shadow: 0 16px 30px rgba(23, 183, 189, 0.28);
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: #fff;
  cursor: pointer;
  transition: transform 0.16s ease, opacity 0.2s ease;
}

.submit:active {
  transform: scale(0.99);
}

.submit:disabled {
  cursor: default;
}

.submit::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(
    105deg,
    transparent 38%,
    rgba(255, 255, 255, 0.5) 50%,
    transparent 62%
  );
  background-size: 260% 100%;
  background-position: 132% 0;
}

.submit__label {
  position: relative;
  z-index: 1;
}

.submit.is-loading .submit__label {
  opacity: 0;
}

.submit__spinner {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 22px;
  height: 22px;
  margin: auto;
  border: 2.5px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.login-switch {
  margin: 24px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  line-height: 21px;
  color: #8b9694;
  text-align: center;
}

.login-switch button {
  margin-left: 4px;
  padding: 0;
  border: 0;
  background: none;
  font: inherit;
  font-weight: 600;
  color: #17b7bd;
  cursor: pointer;
}

.login-safe {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: auto;
  padding-top: 32px;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: #b3bcba;
}

.login-home {
  display: flex;
  flex-shrink: 0;
  justify-content: center;
  padding-bottom: calc(9px + env(safe-area-inset-bottom));
}

.login-home span {
  width: 134px;
  height: 5px;
  border-radius: 100px;
  background: #d3cbcb;
}

/* 动效：与欢迎页同一套语言 */
@keyframes ribbon-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(0deg);
  }
  50% {
    transform: translate3d(-12px, 10px, 0) rotate(-1deg);
  }
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes flow {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes glow {
  0%,
  100% {
    box-shadow: 0 16px 30px rgba(23, 183, 189, 0.24);
  }
  50% {
    box-shadow: 0 20px 38px rgba(23, 183, 189, 0.38);
  }
}

@keyframes sheen {
  0% {
    background-position: 132% 0;
  }
  58%,
  100% {
    background-position: -132% 0;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: no-preference) {
  .ribbon__band--a {
    animation: ribbon-drift 13s ease-in-out infinite;
  }
  .ribbon__band--b {
    animation: ribbon-drift 17s ease-in-out infinite reverse;
  }
  .ribbon__band--c {
    animation: ribbon-drift 21s ease-in-out infinite;
  }
  .login-title {
    animation: rise-in 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) both;
  }
  .login-subtitle {
    animation: rise-in 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) 0.08s both;
  }
  .login-form {
    animation: rise-in 0.55s cubic-bezier(0.2, 0.7, 0.2, 1) 0.16s both;
  }
  .login-switch {
    animation: rise-in 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) 0.24s both;
  }
  .submit {
    animation: flow 7s ease-in-out infinite, glow 3.6s ease-in-out infinite;
  }
  .submit::after {
    animation: sheen 3.8s ease-in-out infinite;
  }
  .submit.is-loading {
    animation: glow 1.2s ease-in-out infinite;
  }
  .submit.is-loading::after {
    animation: none;
    opacity: 0;
  }
}

@media (min-width: 600px) {
  .login-page {
    max-width: 390px;
    margin: 0 auto;
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.2);
  }
}
</style>
