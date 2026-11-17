<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import { listWallets } from '@/api/wallets'
import { listCards } from '@/api/cards'
import { listRecipients } from '@/api/recipients'
import { createTransfer, quoteTransfer } from '@/api/transfers'
import { getPaymentPasswordStatus, setPaymentPassword } from '@/api/users'
import type {
  WalletResponse,
  TransferQuoteResponse,
  CardResponse,
  RecipientResponse,
} from '@/api/types'

const router = useRouter()
const route = useRoute()

// ----- 状态 -----
const wallets = ref<WalletResponse[]>([])
const cards = ref<CardResponse[]>([])
const walletId = ref<string>('')
const cardId = ref<string>('')
const amount = ref<string>('')
const note = ref<string>('')
const submitting = ref(false)
const quoting = ref(false)
const quote = ref<TransferQuoteResponse | null>(null)
const quoteError = ref<string>('')

// 从 route 拿到 recipient 信息
const selectedRecipientId = ref<string>(
  (route.query.recipient as string | undefined) ?? '',
)
const recipientName = ref<string>(
  (route.query.recipientName as string | undefined) ?? '',
)
const recipient = ref<RecipientResponse | null>(null)

const recipientDisplayName = computed(
  () => recipient.value?.displayName || recipientName.value || '收款人',
)
const recipientSub = computed(() => {
  const r = recipient.value
  if (!r) return selectedRecipientId.value ? `收款人 ID ${selectedRecipientId.value}` : ''
  return r.accountNo || r.phone || r.email || ''
})
const recipientAvatar = computed(() => recipient.value?.avatarUrl ?? '')
const avatarLetter = computed(() => recipientDisplayName.value.slice(0, 1))

// ----- 派生 -----
const fromWallet = computed(() =>
  wallets.value.find((w) => w.id === walletId.value) ?? null,
)
const selectedCard = computed(() => cards.value.find((c) => c.id === cardId.value) ?? null)

// 可用余额取所选银行卡余额，未选卡时回退钱包余额
const balanceNum = computed(() => {
  const n = Number(selectedCard.value?.balance ?? fromWallet.value?.balance ?? '0')
  return Number.isFinite(n) ? n : 0
})

function cardBrandLabel(card: CardResponse): string {
  const brands: Record<string, string> = {
    visa: 'Visa',
    mastercard: 'Mastercard',
    unionpay: '银联',
    icbc: '工商银行',
    abc: '农业银行',
    boc: '中国银行',
    ccb: '建设银行',
    bocom: '交通银行',
    psbc: '邮储银行',
  }
  return brands[card.brand ?? ''] ?? '银行卡'
}

const amountNum = computed(() => {
  const n = Number(amount.value)
  return Number.isFinite(n) ? n : 0
})

const insufficientBalance = computed(() => {
  if (amountNum.value <= 0) return false
  return quote.value ? !quote.value.sufficientBalance : amountNum.value > balanceNum.value
})

const amountReady = computed(() => amountNum.value > 0)

const canSend = computed(() => {
  // 余额不足也允许先点转账，进入付款环节后再提示更换银行卡
  return !!quote.value &&
    amountReady.value &&
    selectedRecipientId.value.length > 0 &&
    !quoting.value &&
    !submitting.value
})

// ----- 付款弹层 -----
const paySheetOpen = ref(false)
const methodPickerOpen = ref(false)

// 未选择银行卡时使用零钱（钱包）支付
const isWalletPay = computed(() => !cardId.value)
const walletBalanceNum = computed(() => {
  const n = Number(fromWallet.value?.balance ?? '0')
  return Number.isFinite(n) ? n : 0
})
const currentMethodLabel = computed(() => {
  if (isWalletPay.value) return '零钱'
  const c = selectedCard.value
  return c ? `${cardBrandLabel(c)} 卡(${c.last4})` : '零钱'
})
const methodInitial = computed(() => {
  if (isWalletPay.value || !selectedCard.value) return '¥'
  return cardBrandLabel(selectedCard.value).charAt(0)
})

function openPaySheet() {
  if (!canSend.value) return
  paySheetOpen.value = true
}

function closePaySheet() {
  paySheetOpen.value = false
  methodPickerOpen.value = false
}

function openMethodPicker() {
  methodPickerOpen.value = true
}

function closeMethodPicker() {
  methodPickerOpen.value = false
}

function chooseWallet() {
  cardId.value = ''
  methodPickerOpen.value = false
}

function chooseCard(id: string) {
  cardId.value = id
  methodPickerOpen.value = false
}

function goCards() {
  void router.push('/cards')
}

// ----- 支付密码 -----
const pwdOpen = ref(false)
const pwdMode = ref<'input' | 'set'>('input')
const pwdStatus = ref<'unknown' | 'set' | 'none'>('unknown')
const pwdValue = ref('')
const pwdFirst = ref('')
const pwdSecond = ref('')
const pwdShake = ref(false)
const pwdSubmitting = ref(false)

const pwdKeys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', '⌫']

// 设置密码模式下当前正在输入的密码
const setActivePwd = computed(() =>
  pwdFirst.value.length < 6 ? pwdFirst.value : pwdSecond.value,
)

function shakePwd() {
  pwdShake.value = true
  window.setTimeout(() => {
    pwdShake.value = false
  }, 600)
}

function closePwd() {
  if (pwdSubmitting.value) return
  pwdOpen.value = false
  pwdValue.value = ''
  pwdFirst.value = ''
  pwdSecond.value = ''
}

// 点「支付」：先确认支付密码状态，已设置则输密码，未设置则先引导设置
async function startPay() {
  if (!canSend.value || submitting.value || pwdSubmitting.value) return
  // 余额不足：在付款环节提示更换银行卡，并展开付款方式选择
  if (insufficientBalance.value) {
    methodPickerOpen.value = true
    return
  }
  try {
    if (pwdStatus.value === 'unknown') {
      pwdStatus.value = (await getPaymentPasswordStatus()).enabled ? 'set' : 'none'
    }
  } catch {
    return
  }
  pwdValue.value = ''
  pwdFirst.value = ''
  pwdSecond.value = ''
  if (pwdStatus.value === 'set') {
    pwdMode.value = 'input'
  } else {
    pwdMode.value = 'set'
  }
  pwdOpen.value = true
}

function onPwdKey(k: string) {
  if (pwdSubmitting.value) return
  const isDel = k === '⌫'
  if (pwdMode.value === 'input') {
    if (isDel) {
      pwdValue.value = pwdValue.value.slice(0, -1)
      return
    }
    if (pwdValue.value.length >= 6) return
    pwdValue.value += k
    if (pwdValue.value.length === 6) void sendMoney(pwdValue.value)
    return
  }

  // 设置模式：先输第一遍，再输第二遍确认
  if (pwdFirst.value.length < 6) {
    if (isDel) pwdFirst.value = pwdFirst.value.slice(0, -1)
    else pwdFirst.value = (pwdFirst.value + k).slice(0, 6)
    return
  }
  if (isDel) {
    pwdSecond.value = pwdSecond.value.slice(0, -1)
    return
  }
  if (pwdSecond.value.length >= 6) return
  pwdSecond.value += k
  if (pwdSecond.value.length === 6) void submitSetPwd()
}

async function submitSetPwd() {
  if (pwdSubmitting.value) return
  if (pwdFirst.value !== pwdSecond.value) {
    pwdFirst.value = ''
    pwdSecond.value = ''
    shakePwd()
    return
  }
  pwdSubmitting.value = true
  try {
    await setPaymentPassword({ newPassword: pwdFirst.value })
    pwdStatus.value = 'set'
    pwdFirst.value = ''
    pwdSecond.value = ''
    pwdMode.value = 'input'
    pwdValue.value = ''
  } catch {
    pwdFirst.value = ''
    pwdSecond.value = ''
    shakePwd()
  } finally {
    pwdSubmitting.value = false
  }
}

// ----- 金额格式化 -----
function toMoney(raw: string): string {
  // 只保留数字和一个小数点
  let s = raw.replace(/[^0-9.]/g, '')
  const dotIdx = s.indexOf('.')
  if (dotIdx !== -1) {
    s = s.slice(0, dotIdx + 1) + s.slice(dotIdx + 1).replace(/\./g, '')
  }
  // 限制最多 2 位小数
  if (dotIdx !== -1) {
    const [intPart, decPart = ''] = s.split('.')
    s = intPart + '.' + decPart.slice(0, 2)
  }
  // 限制整数部分最多 9 位
  const parts = s.split('.')
  parts[0] = parts[0].slice(-9)
  s = parts.join('.')
  return s
}

function appendDigit(d: string) {
  if (d === '.') {
    if (!amount.value.includes('.')) {
      amount.value = (amount.value || '0') + '.'
    }
    return
  }
  // 已输入两位小数时，再按数字则重新开始输入（避免新数字被截断）
  const dotIdx = amount.value.indexOf('.')
  if (dotIdx !== -1 && amount.value.length - dotIdx - 1 >= 2) {
    amount.value = d
    return
  }
  amount.value = toMoney(amount.value + d)
}

function backspace() {
  if (!amount.value) return
  amount.value = amount.value.slice(0, -1)
}

function onKey(k: string) {
  if (k === '⌫') backspace()
  else appendDigit(k)
}

// 接口要求两位小数的金额（如 1 → 1.00）
function apiAmount(): string {
  if (!amount.value) return '0.00'
  const n = Number(amount.value)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

// ----- 生命周期 -----
onMounted(async () => {
  try {
    const [ws, cs, rs] = await Promise.all([
      listWallets(),
      listCards(undefined, 'active'),
      listRecipients().catch(() => [] as RecipientResponse[]),
    ])
    wallets.value = ws
    cards.value = cs
    recipient.value = rs.find((r) => r.id === selectedRecipientId.value) ?? null
    if (!recipientName.value && recipient.value) {
      recipientName.value = recipient.value.displayName
    }
    const firstActive = ws.find((w) => w.status === 'active') ?? ws[0]
    if (firstActive) walletId.value = firstActive.id
    // 默认选主卡，否则第一张
    const defaultCard = cs.find((c) => c.isPrimary) ?? cs[0]
    if (defaultCard) cardId.value = defaultCard.id
  } catch {
    // 请求层已处理
  }
})

// ----- 操作 -----
function goBack() {
  router.back()
}

function resetQuote() {
  quote.value = null
  quoteError.value = ''
}

async function refreshQuote() {
  if (!walletId.value || !selectedRecipientId.value || !amountReady.value) {
    resetQuote()
    return
  }
  quoting.value = true
  quoteError.value = ''
  try {
    quote.value = await quoteTransfer({
      walletId: walletId.value,
      cardId: cardId.value || undefined,
      recipientId: selectedRecipientId.value,
      amount: apiAmount(),
    })
  } catch {
    quote.value = null
    quoteError.value = '获取转账费用失败，请稍后重试'
  } finally {
    quoting.value = false
  }
}

watch(
  [walletId, cardId, amount, selectedRecipientId],
  () => void refreshQuote(),
)

async function sendMoney(paymentPassword?: string) {
  if (!canSend.value) return
  submitting.value = true
  try {
    const res = await createTransfer({
      walletId: walletId.value,
      cardId: cardId.value || undefined,
      recipientId: selectedRecipientId.value,
      amount: apiAmount(),
      note: note.value.trim() || undefined,
      paymentPassword,
    })
    pwdOpen.value = false
    // 跳转到 money-sent 成功页，带上转账信息
    await router.replace({
      path: '/transfers/success',
      query: {
        transferId: res.id,
        referenceNo: res.referenceNo,
        amount: res.amount,
        recipient: res.recipient?.name || recipientName.value,
        feeAmount: res.feeAmount,
        totalAmount: res.totalAmount,
        currencyCode: res.currencyCode,
      },
    })
  } catch {
    // 支付密码错误等：抖动提示并清空重输
    pwdValue.value = ''
    shakePwd()
  } finally {
    submitting.value = false
  }
}

function formatBalance(b: string | number): string {
  const n = typeof b === 'string' ? Number(b) : b
  if (!Number.isFinite(n)) return '0.00'
  return n.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}
</script>

<template>
  <PageLayout title="转账" @back="goBack">
    <div class="add-amount">
      <div class="scroll-area">
        <!-- 收款人 -->
        <div class="recipient-row">
          <div class="recipient-info">
            <p class="recipient-name">转账给 {{ recipientDisplayName }}</p>
            <p v-if="recipientSub" class="recipient-sub">{{ recipientSub }}</p>
          </div>
          <div class="recipient-avatar">
            <img v-if="recipientAvatar" :src="recipientAvatar" alt="" />
            <span v-else>{{ avatarLetter }}</span>
          </div>
        </div>

        <!-- 金额卡片 -->
        <div class="amount-card">
          <p class="amount-label">转账金额</p>
          <div class="amount-input">
            <span class="amount-symbol">¥</span>
            <span v-if="amount" class="amount-value">{{ amount }}</span>
            <span v-if="!amount" class="caret" />
          </div>
          <div class="amount-divider" />

          <div class="note-row">
            <input
              v-model="note"
              class="note-input"
              type="text"
              placeholder="添加转账说明"
              maxlength="30"
            />
          </div>
        </div>

        <!-- 费用提示 -->
        <p v-if="quote && amountReady" class="fee-hint">
          手续费 ¥{{ formatBalance(quote.feeAmount) }}，合计 ¥{{ formatBalance(quote.totalAmount) }}
        </p>
        <p v-else-if="quoteError" class="fee-hint fee-hint--error">{{ quoteError }}</p>
      </div>

      <!-- 数字键盘（常驻底部） -->
      <div class="keypad">
        <button class="key" type="button" @click="onKey('1')">1</button>
        <button class="key" type="button" @click="onKey('2')">2</button>
        <button class="key" type="button" @click="onKey('3')">3</button>
        <button class="key key--del" type="button" @click="onKey('⌫')">
          <svg width="24" height="18" viewBox="0 0 24 18" fill="none">
            <path d="M15.5 1.5 L22 9 L15.5 16.5 H3.5 C2.12 16.5 1 15.38 1 14 V4 C1 2.62 2.12 1.5 3.5 1.5 Z"
              stroke="#353333" stroke-width="1.5" stroke-linejoin="round" fill="none"/>
            <line x1="8" y1="6.5" x2="13" y2="11.5" stroke="#353333" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="13" y1="6.5" x2="8" y2="11.5" stroke="#353333" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>

        <button class="key" type="button" @click="onKey('4')">4</button>
        <button class="key" type="button" @click="onKey('5')">5</button>
        <button class="key" type="button" @click="onKey('6')">6</button>
        <button
          class="key key--transfer"
          type="button"
          :disabled="!canSend"
          @click="openPaySheet"
        >
          转账
        </button>

        <button class="key" type="button" @click="onKey('7')">7</button>
        <button class="key" type="button" @click="onKey('8')">8</button>
        <button class="key" type="button" @click="onKey('9')">9</button>

        <button class="key key--zero" type="button" @click="onKey('0')">0</button>
        <button class="key" type="button" @click="onKey('.')">.</button>
      </div>

      <!-- 确认付款弹层 -->
      <div v-if="paySheetOpen" class="pay-mask" @click.self="closePaySheet">
        <div class="pay-sheet">
          <div class="pay-sheet__head">
            <button class="pay-sheet__close" type="button" @click="closePaySheet">✕</button>
          </div>
          <p class="pay-sheet__title">向 {{ recipientDisplayName }} 转账</p>
          <p class="pay-sheet__amount">¥{{ formatBalance(amountNum) }}</p>
          <div class="pay-sheet__divider" />

          <div class="pay-sheet__method-head">
            <span>付款方式</span>
            <button class="pay-sheet__change" type="button" @click="openMethodPicker">
              更改
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M2.5 4.5 L6 8 L9.5 4.5" stroke="#afafaf" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>

          <button class="pay-method" type="button" @click="openMethodPicker">
            <span class="method-icon" :class="isWalletPay ? 'method-icon--wallet' : 'method-icon--card'">
              {{ methodInitial }}
            </span>
            <span class="pay-method__name">{{ currentMethodLabel }}</span>
            <svg class="check" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M4 10.5 L8 14.5 L16 5.5" stroke="#07c160" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>

          <p v-if="insufficientBalance" class="pay-sheet__warn">⚠️ 余额不足，请更换银行卡</p>

          <button class="pay-btn" type="button" :disabled="submitting" @click="startPay">
            {{ submitting ? '支付中…' : '支付' }}
          </button>
        </div>
      </div>

      <!-- 选择付款方式 -->
      <div v-if="methodPickerOpen" class="picker-mask" @click.self="closeMethodPicker">
        <div class="picker-sheet">
          <div class="picker-head">
            <button class="picker-collapse" type="button" @click="closeMethodPicker">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M3 6 L8 11 L13 6" stroke="#353333" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <span class="picker-head__title">选择付款方式</span>
            <span class="picker-head__spacer" />
          </div>

          <div class="picker-body">
            <button
              class="method-item"
              :class="{ 'method-item--active': isWalletPay }"
              type="button"
              @click="chooseWallet"
            >
              <span class="method-icon method-icon--wallet">¥</span>
              <span class="method-item__main">
                <span class="method-item__name">零钱</span>
                <span class="method-item__sub">剩余 ¥{{ formatBalance(walletBalanceNum) }}</span>
              </span>
              <svg v-if="isWalletPay" class="check" width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M4 10.5 L8 14.5 L16 5.5" stroke="#07c160" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>

            <p class="method-group">银行卡</p>
            <button
              v-for="c in cards"
              :key="c.id"
              class="method-item"
              :class="{ 'method-item--active': cardId === c.id }"
              type="button"
              @click="chooseCard(c.id)"
            >
              <span class="method-icon method-icon--card">{{ cardBrandLabel(c).charAt(0) }}</span>
              <span class="method-item__main">
                <span class="method-item__name">{{ cardBrandLabel(c) }} 卡({{ c.last4 }})</span>
                <span class="method-item__sub">剩余 ¥{{ formatBalance(c.balance) }}</span>
              </span>
              <svg v-if="cardId === c.id" class="check" width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M4 10.5 L8 14.5 L16 5.5" stroke="#07c160" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>

            <button class="method-item method-item--add" type="button" @click="goCards">
              <span class="method-icon method-icon--add">＋</span>
              <span class="method-item__name">添加新银行卡</span>
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M5 2.5 L9.5 7 L5 11.5" stroke="#b2b2b2" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>

          <p class="picker-footer">本服务由 Kaya 提供</p>
        </div>
      </div>

      <!-- 支付密码 -->
      <div v-if="pwdOpen" class="pwd-mask" @click.self="closePwd">
        <div class="pwd-dialog" :class="{ 'pwd-dialog--shake': pwdShake }">
          <button class="pwd-close" type="button" @click="closePwd">✕</button>

          <p class="pwd-title">
            {{ pwdMode === 'set' ? '设置支付密码' : '请输入支付密码' }}
          </p>
          <p class="pwd-amount">¥{{ formatBalance(amountNum) }}</p>
          <p v-if="pwdMode === 'set'" class="pwd-hint">
            {{ pwdFirst.length < 6 ? '首次转账需设置 6 位数字支付密码' : '请再次输入以确认' }}
          </p>

          <div class="pwd-dots">
            <span
              v-for="i in 6"
              :key="i"
              class="pwd-dot"
              :class="{
                'pwd-dot--filled':
                  (pwdMode === 'input' ? pwdValue : setActivePwd).length >= i,
              }"
            />
          </div>

          <div class="pwd-keypad">
            <button
              v-for="(k, idx) in pwdKeys"
              :key="idx"
              type="button"
              class="pwd-key"
              :class="{ 'pwd-key--blank': k === '' }"
              :disabled="k === '' || pwdSubmitting"
              @click="onPwdKey(k)"
            >
              {{ k }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </PageLayout>
</template>

<style scoped>
:deep(.page-layout) {
  background: #f7f6f8;
}
:deep(.page-layout__header) {
  height: 56px;
  padding: 0 20px;
  background: #f7f6f8;
}
:deep(.page-layout__back) {
  color: #353333;
  font-size: 22px;
}
:deep(.page-layout__main) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}
:deep(.page-layout__home) {
  background: #f7f6f8;
}

.add-amount {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 4px 20px 12px;
}

/* 收款人 */
.recipient-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 4px 18px;
}
.recipient-info {
  min-width: 0;
}
.recipient-name {
  margin: 0;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 17px;
  font-weight: 600;
  color: #353333;
  letter-spacing: -0.2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recipient-sub {
  margin: 5px 0 0;
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  color: #afafaf;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recipient-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  background: #d9d9d9;
}
.recipient-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.recipient-avatar span {
  font-family: 'Poppins', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

/* 金额卡片 */
.amount-card {
  padding: 18px 16px 0;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}
.amount-label {
  margin: 0 0 12px;
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  color: #696565;
}
.amount-input {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 52px;
}
.amount-symbol {
  font-family: 'Poppins', sans-serif;
  font-size: 30px;
  font-weight: 700;
  color: #353333;
}
.amount-value {
  font-family: 'Poppins', sans-serif;
  font-size: 34px;
  font-weight: 600;
  color: #353333;
  line-height: 1.2;
  letter-spacing: -0.5px;
  font-variant-numeric: tabular-nums;
  word-break: break-all;
}
.caret {
  width: 2px;
  height: 30px;
  margin-left: 2px;
  background: #07c160;
  animation: caret-blink 1s step-end infinite;
}
@keyframes caret-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.amount-divider {
  height: 1px;
  background: #f6f6f6;
}

/* 转账说明 */
.note-row {
  padding: 12px 0;
}
.note-input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  color: #353333;
}
.note-input::placeholder {
  color: #576b95;
}

/* 确认付款弹层 */
.pay-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
}
.pay-sheet {
  width: 100%;
  max-width: 390px;
  padding: 10px 20px calc(24px + env(safe-area-inset-bottom));
  border-radius: 14px 14px 0 0;
  background: #ffffff;
  animation: sheet-up 0.2s ease-out;
}
@keyframes sheet-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.pay-sheet__head {
  display: flex;
  justify-content: flex-start;
}
.pay-sheet__close {
  width: 32px;
  height: 32px;
  border: 0;
  background: none;
  font-size: 20px;
  color: #353333;
  cursor: pointer;
}
.pay-sheet__title {
  margin: 4px 0 0;
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  font-weight: 500;
  color: #353333;
}
.pay-sheet__amount {
  margin: 8px 0 0;
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 34px;
  font-weight: 600;
  color: #353333;
  font-variant-numeric: tabular-nums;
}
.pay-sheet__divider {
  height: 1px;
  margin: 20px 0 0;
  background: #f6f6f6;
}
.pay-sheet__method-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0 10px;
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  color: #696565;
}
.pay-sheet__change {
  display: flex;
  align-items: center;
  gap: 4px;
  border: 0;
  background: none;
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  color: #afafaf;
  cursor: pointer;
}
.pay-method {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 10px;
  border: 0;
  border-radius: 6px;
  background: #fdf9ec;
  text-align: left;
  cursor: pointer;
}
.pay-method__name {
  flex: 1;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  color: #353333;
}
.check {
  flex-shrink: 0;
}
.pay-btn {
  display: block;
  width: 190px;
  height: 44px;
  margin: 28px auto 4px;
  border: 0;
  border-radius: 6px;
  background: #07c160;
  font-family: 'Poppins', sans-serif;
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  cursor: pointer;
}
.pay-btn:active {
  background: #06ad56;
}
.pay-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.pay-sheet__warn {
  margin: 12px 0 0;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(230, 67, 64, 0.08);
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  color: #e64340;
}

/* 选择付款方式 */
.picker-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
}
.picker-sheet {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 390px;
  height: 86vh;
  height: 86dvh;
  border-radius: 14px 14px 0 0;
  background: #ffffff;
  animation: sheet-up 0.2s ease-out;
}
.picker-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 14px 16px 10px;
}
.picker-collapse {
  position: absolute;
  left: 12px;
  top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  background: none;
  cursor: pointer;
}
.picker-head__title {
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
}
.picker-head__spacer {
  width: 32px;
}
.picker-body {
  flex: 1;
  overflow-y: auto;
}
.method-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 20px;
  border: 0;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
}
.method-item:active {
  background: #f7f7f7;
}
.method-item__main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
  min-width: 0;
}
.method-item__name {
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  color: #353333;
}
.method-item__sub {
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  color: #afafaf;
}
.method-group {
  margin: 0;
  padding: 14px 20px 4px;
  background: #f7f7f7;
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  color: #afafaf;
}
.method-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  font-weight: 600;
}
.method-icon--wallet {
  background: #ffd666;
  color: #8a6d1a;
}
.method-icon--card {
  background: #e8f1ff;
  color: #2f6fd0;
}
.method-icon--add {
  background: #f2f2f2;
  color: #afafaf;
}
.method-item--add {
  border-top: 1px solid #f2f2f2;
}
.picker-footer {
  flex-shrink: 0;
  margin: 0;
  padding: 12px 0 calc(14px + env(safe-area-inset-bottom));
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 11px;
  color: #b2b2b2;
}

/* 支付密码 */
.pwd-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  background: rgba(0, 0, 0, 0.45);
}
.pwd-dialog {
  position: relative;
  width: 100%;
  max-width: 320px;
  padding: 22px 16px 18px;
  border-radius: 14px;
  background: #ffffff;
  animation: pwd-pop 0.18s ease-out;
}
@keyframes pwd-pop {
  from { transform: scale(0.94); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.pwd-dialog--shake {
  animation: pwd-shake 0.45s;
}
@keyframes pwd-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-9px); }
  50% { transform: translateX(9px); }
  75% { transform: translateX(-5px); }
}
.pwd-close {
  position: absolute;
  top: 8px;
  left: 10px;
  width: 30px;
  height: 30px;
  border: 0;
  background: none;
  font-size: 16px;
  color: #afafaf;
  cursor: pointer;
}
.pwd-title {
  margin: 6px 0 0;
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
}
.pwd-amount {
  margin: 8px 0 0;
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 26px;
  font-weight: 600;
  color: #353333;
  font-variant-numeric: tabular-nums;
}
.pwd-hint {
  margin: 6px 0 0;
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  color: #afafaf;
}
.pwd-dots {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin: 16px 0 4px;
}
.pwd-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1.5px solid #d5d5d5;
  background: #fff;
  transition: background 0.15s, border-color 0.15s;
}
.pwd-dot--filled {
  background: #07c160;
  border-color: #07c160;
}
.pwd-keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-top: 14px;
}
.pwd-key {
  height: 46px;
  border: 0;
  border-radius: 6px;
  background: #f5f5f5;
  font-family: 'Poppins', sans-serif;
  font-size: 20px;
  color: #353333;
  cursor: pointer;
  user-select: none;
}
.pwd-key:active {
  background: #e5e5e5;
}
.pwd-key--blank {
  background: transparent;
  cursor: default;
}

/* 提示 */
.error-msg {
  margin: 10px 4px 0;
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #e64340;
}
.fee-hint {
  margin: 10px 4px 0;
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  color: #afafaf;
}
.fee-hint--error {
  color: #e64340;
}

/* 数字键盘（常驻底部，4 列） */
.keypad {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 54px;
  gap: 6px;
  padding: 6px 6px 8px;
  background: #f6f6f6;
}
.key {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 6px;
  background: #ffffff;
  font-family: 'Poppins', sans-serif;
  font-size: 22px;
  font-weight: 400;
  color: #353333;
  cursor: pointer;
  box-shadow: 0 0.5px 0 rgba(0, 0, 0, 0.18);
  transition: background 0.1s ease;
}
.key:active {
  background: #e3e3e3;
}
.key--del {
  font-size: 16px;
}
.key--zero {
  grid-column: 1 / span 2;
}
.key--transfer {
  grid-column: 4;
  grid-row: 2 / span 2;
  height: auto;
  background: #07c160;
  color: #ffffff;
  font-size: 17px;
  font-weight: 500;
  box-shadow: none;
}
.key--transfer:active {
  background: #06ad56;
}
.key--transfer:disabled {
  background: #8fd9ac;
  cursor: not-allowed;
}
</style>
