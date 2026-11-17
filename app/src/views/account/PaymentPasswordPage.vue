<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import { getPaymentPasswordStatus, resetPaymentPassword, setPaymentPassword } from '@/api/users'

const router = useRouter()

const enabled = ref(false)
const loadingStatus = ref(true)

// step: old -> new -> confirm
const step = ref<'old' | 'new' | 'confirm'>('new')
const oldValue = ref('')
const newValue = ref('')
const confirmValue = ref('')
const submitting = ref(false)
const shake = ref(false)

// 忘记支付密码：改用登录密码验证后重设
const resetMode = ref(false)
const showResetDialog = ref(false)
const loginPassword = ref('')

const currentInput = computed(() => {
  if (step.value === 'old') return oldValue.value
  if (step.value === 'new') return newValue.value
  return confirmValue.value
})

const stepTitle = computed(() => {
  if (resetMode.value) {
    if (step.value === 'confirm') return '请再次输入支付密码'
    return '请设置新的支付密码'
  }
  if (step.value === 'old') return '请输入当前支付密码'
  if (step.value === 'new') return enabled.value ? '请输入新的支付密码' : '请设置 6 位数字支付密码'
  return '请再次输入支付密码'
})

onMounted(async () => {
  try {
    enabled.value = (await getPaymentPasswordStatus()).enabled
    if (enabled.value) step.value = 'old'
  } catch {
    // 请求层已处理
  } finally {
    loadingStatus.value = false
  }
})

function goBack() {
  router.back()
}

function pressKey(key: string) {
  if (submitting.value) return
  const target =
    step.value === 'old' ? oldValue : step.value === 'new' ? newValue : confirmValue
  if (target.value.length >= 6) return
  target.value += key
  if (target.value.length === 6) {
    // 输满 6 位自动进入下一步 / 提交
    if (step.value === 'old') {
      step.value = 'new'
    } else if (step.value === 'new') {
      step.value = 'confirm'
    } else {
      void submit()
    }
  }
}

function pressDelete() {
  if (submitting.value) return
  const target =
    step.value === 'old' ? oldValue : step.value === 'new' ? newValue : confirmValue
  target.value = target.value.slice(0, -1)
}

function resetToOld() {
  step.value = 'old'
  oldValue.value = ''
  newValue.value = ''
  confirmValue.value = ''
  shake.value = false
}

function openResetDialog() {
  loginPassword.value = ''
  showResetDialog.value = true
}

function cancelReset() {
  showResetDialog.value = false
  loginPassword.value = ''
}

// 验证登录密码后进入「设置新密码」两步输入
function confirmResetIdentity() {
  if (!loginPassword.value.trim()) {
    ElMessage.error('请输入登录密码')
    return
  }
  resetMode.value = true
  step.value = 'new'
  newValue.value = ''
  confirmValue.value = ''
  showResetDialog.value = false
}

async function submit() {
  if (submitting.value) return
  if (newValue.value !== confirmValue.value) {
    ElMessage.error('两次输入的支付密码不一致，请重新设置')
    step.value = 'new'
    newValue.value = ''
    confirmValue.value = ''
    return
  }
  if (!resetMode.value && enabled.value && newValue.value === oldValue.value) {
    ElMessage.error('新支付密码不能与当前支付密码相同')
    step.value = 'new'
    newValue.value = ''
    confirmValue.value = ''
    return
  }

  submitting.value = true
  try {
    if (resetMode.value) {
      await resetPaymentPassword({
        loginPassword: loginPassword.value,
        newPassword: newValue.value,
      })
      ElMessage.success('支付密码已重置')
    } else {
      await setPaymentPassword({
        currentPassword: enabled.value ? oldValue.value : undefined,
        newPassword: newValue.value,
      })
      ElMessage.success(enabled.value ? '支付密码已更新' : '支付密码设置成功')
    }
    router.back()
  } catch {
    // 请求层已提示错误。重置失败（如登录密码错误）时重新弹出登录密码验证
    if (resetMode.value) {
      step.value = 'new'
      newValue.value = ''
      confirmValue.value = ''
      loginPassword.value = ''
      showResetDialog.value = true
    } else {
      resetToOld()
    }
  } finally {
    submitting.value = false
  }
}

const keypadKeys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del']
</script>

<template>
  <div class="payment-password-page">
    <PageLayout title="支付密码" @back="goBack">
      <div class="pay-pwd">
        <div class="shield-wrap">
          <div class="shield-circle">
            <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3.5" y="10.5" width="17" height="10" rx="2.2" />
              <path d="M7.5 10.5V7.8a4.5 4.5 0 0 1 9 0v2.7" />
              <circle cx="12" cy="15.5" r="1.4" fill="#ffffff" stroke="none" />
            </svg>
          </div>
        </div>

        <h2 class="step-title">{{ stepTitle }}</h2>
        <p class="step-sub">用于转账等敏感操作的身份验证，请勿与登录密码相同</p>

        <div class="dots" :class="{ 'dots--shake': shake }">
          <span
            v-for="i in 6"
            :key="i"
            class="dot"
            :class="{ 'dot--filled': currentInput.length >= i }"
          />
        </div>

        <button
          v-if="enabled && !resetMode && step === 'old'"
          class="forgot-link"
          type="button"
          @click="openResetDialog"
        >
          忘记当前支付密码？
        </button>

        <!-- 内置数字键盘，不依赖系统键盘 -->
        <div class="keypad">
          <button
            v-for="(k, idx) in keypadKeys"
            :key="idx"
            type="button"
            class="key"
            :class="{ 'key--blank': k === '' }"
            :disabled="k === ''"
            @click="k === 'del' ? pressDelete() : pressKey(k)"
          >
            <svg v-if="k === 'del'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 5H8.6a2 2 0 0 0-1.5.7L2.4 11a1.6 1.6 0 0 0 0 2l4.7 5.3a2 2 0 0 0 1.5.7H21a1.5 1.5 0 0 0 1.5-1.5v-12A1.5 1.5 0 0 0 21 5z" />
              <path d="M17 9.5l-5 5M12 9.5l5 5" />
            </svg>
            <span v-else>{{ k }}</span>
          </button>
        </div>
      </div>
    </PageLayout>

    <!-- 忘记支付密码：验证登录密码 -->
    <div v-if="showResetDialog" class="reset-mask" @click.self="cancelReset">
      <div class="reset-dialog">
        <p class="reset-title">验证登录密码</p>
        <p class="reset-sub">验证通过后可重新设置支付密码</p>
        <input
          v-model="loginPassword"
          class="reset-input"
          type="password"
          placeholder="请输入登录密码"
          autocomplete="current-password"
        />
        <div class="reset-actions">
          <button class="reset-btn reset-btn--ghost" type="button" @click="cancelReset">
            取消
          </button>
          <button class="reset-btn" type="button" @click="confirmResetIdentity">
            下一步
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-password-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-width: 390px;
  margin: 0 auto;
  overflow: hidden;
}
.payment-password-page :deep(.page-layout) {
  flex: 1;
  height: auto;
  min-height: 0;
}

.pay-pwd {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0 24px;
}

.shield-wrap {
  padding: 8px 0 16px;
}
.shield-circle {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: #17b7bd;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(23, 183, 189, 0.25);
}

.step-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #353333;
}

.step-sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: #696565;
  text-align: center;
  max-width: 280px;
}

.dots {
  display: flex;
  gap: 14px;
  margin: 24px 0 8px;
}

.forgot-link {
  margin-top: 2px;
  border: 0;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: #576b95;
  cursor: pointer;
  padding: 6px;
}

/* 忘记支付密码：登录密码验证弹窗 */
.reset-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 34px;
  background: rgba(0, 0, 0, 0.45);
}
.reset-dialog {
  width: 100%;
  max-width: 300px;
  padding: 20px 18px 16px;
  border-radius: 16px;
  background: #ffffff;
  animation: reset-pop 0.18s ease-out;
}
@keyframes reset-pop {
  from { transform: scale(0.94); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.reset-title {
  margin: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #353333;
}
.reset-sub {
  margin: 6px 0 14px;
  text-align: center;
  font-size: 12px;
  color: #afafaf;
}
.reset-input {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #f6f6f6;
  border-radius: 8px;
  background: #f7f7f7;
  font-family: inherit;
  font-size: 14px;
  color: #353333;
  outline: none;
  box-sizing: border-box;
}
.reset-input:focus {
  border-color: #07c160;
  background: #ffffff;
}
.reset-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
.reset-btn {
  flex: 1;
  height: 40px;
  border: 0;
  border-radius: 8px;
  background: #07c160;
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  cursor: pointer;
}
.reset-btn:active {
  background: #06ad56;
}
.reset-btn--ghost {
  background: #f2f2f2;
  color: #353333;
}
.reset-btn--ghost:active {
  background: #e5e5e5;
}
.dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1.5px solid rgba(0, 0, 0, 0.18);
  background: #fff;
  transition: background 0.15s, border-color 0.15s, transform 0.15s;
}
.dot--filled {
  background: #17b7bd;
  border-color: #17b7bd;
  transform: scale(1.08);
}
.dots--shake {
  animation: shake 0.5s;
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  50% { transform: translateX(8px); }
  75% { transform: translateX(-5px); }
}

/* 内置键盘 */
.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  width: 100%;
  max-width: 315px;
  margin-top: 18px;
}
.key {
  height: 56px;
  border: 0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(24, 39, 43, 0.08);
  font-family: inherit;
  font-size: 20px;
  font-weight: 600;
  color: #353333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.1s;
  user-select: none;
}
.key:active {
  background: rgba(23, 183, 189, 0.12);
}
.key:disabled {
  background: transparent;
  box-shadow: none;
  cursor: default;
}
</style>
