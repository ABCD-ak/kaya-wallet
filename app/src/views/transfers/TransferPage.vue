<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import BottomNav from '@/components/BottomNav.vue'
import { listWallets } from '@/api/wallets'
import { quoteTransfer, createTransfer } from '@/api/transfers'
import { listRecipients } from '@/api/recipients'
import { listCards } from '@/api/cards'
import { getPaymentPasswordStatus, setPaymentPassword } from '@/api/users'
import type {
  WalletResponse,
  TransferQuoteResponse,
  RecipientResponse,
  CardResponse,
} from '@/api/types'

const router = useRouter()

// ----- 状态 -----
const wallets = ref<WalletResponse[]>([])
const recipients = ref<RecipientResponse[]>([])
const cards = ref<CardResponse[]>([])

const walletId = ref<string>('')
const cardId = ref<string>('')
const amountInput = ref<string>('')
const submitting = ref(false)
const quoting = ref(false)

// 收款方模式: contacts | manual
const recipientMode = ref<'contacts' | 'manual'>('contacts')
const selectedRecipientId = ref<string>('')
const manualRecipient = ref({
  recipientName: '',
  accountNo: '',
  bankName: '',
})

const quote = ref<TransferQuoteResponse | null>(null)
const quoteError = ref<string>('')

// ----- 确认付款弹窗 + 支付密码 -----
type PayDialogMode = 'confirm' | 'setpwd' | 'inputpwd'
const showPayDialog = ref(false)
const payDialogMode = ref<PayDialogMode>('confirm')
const payPwdStatus = ref<'unknown' | 'set' | 'none'>('unknown')
const setPwdFirst = ref('')
const setPwdSecond = ref('')
const inputPwd = ref('')
const paySubmitting = ref(false)
const payShake = ref(false)
const showAllMethods = ref(false)

// 付款方式：钱包零钱（不选卡）+ 所有银行卡
const paymentMethods = computed(() => {
  const list: { id: string; label: string; desc: string; isWallet: boolean }[] = []
  if (fromWallet.value) {
    list.push({
      id: '',
      label: '钱包零钱',
      desc: `可用余额 ${fromWallet.value.balance} ${currencyLabel(fromWallet.value.currencyCode)}`,
      isWallet: true,
    })
  }
  for (const c of cards.value) {
    list.push({
      id: c.id,
      label: `${cardBrandLabel(c)} **** ${c.last4}`,
      desc: `可用余额 ${c.balance} ${currencyLabel(c.currencyCode)}`,
      isWallet: false,
    })
  }
  return list
})

const selectedMethod = computed(() =>
  paymentMethods.value.find((m) => m.id === cardId.value) ?? paymentMethods.value[0] ?? null,
)

const recipientLabel = computed(() => {
  if (recipientMode.value === 'contacts') {
    return selectedRecipient.value?.displayName ?? ''
  }
  return manualRecipient.value.recipientName.trim()
})

const activeSetPwd = computed(() =>
  setPwdFirst.value.length < 6 ? setPwdFirst.value : setPwdSecond.value,
)

const dialogKeys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del']

// ----- 派生 -----
const fromWallet = computed(() => wallets.value.find((w) => w.id === walletId.value) ?? null)
const selectedCard = computed(() => cards.value.find((c) => c.id === cardId.value) ?? null)
const selectedRecipient = computed(() =>
  recipients.value.find((r) => r.id === selectedRecipientId.value) ?? null,
)

// 付款卡可用余额（未选卡时回退钱包余额）
const availableBalance = computed(() => selectedCard.value?.balance ?? fromWallet.value?.balance)

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

function currencyLabel(code?: string): string {
  if (!code) return ''
  const labels: Record<string, string> = {
    CNY: '人民币',
    USD: '美元',
    EUR: '欧元',
    JPY: '日元',
    GBP: '英镑',
    HKD: '港币',
  }
  return labels[code] ?? '其他币种'
}

function currencySymbol(code?: string): string {
  if (!code) return '¥'
  const symbols: Record<string, string> = {
    CNY: '¥',
    USD: '$',
    EUR: '€',
    JPY: '¥',
    GBP: '£',
    HKD: 'HK$',
  }
  return symbols[code] ?? '¥'
}

const amountReady = computed(() => {
  const n = Number(amountInput.value)
  return !Number.isNaN(n) && n > 0
})

const recipientReady = computed(() => {
  if (recipientMode.value === 'contacts') {
    return !!selectedRecipientId.value
  }
  const m = manualRecipient.value
  return !!m.recipientName.trim() && !!m.accountNo.trim()
})

const canConfirm = computed(() => {
  // 余额不足也允许先进入付款环节，进入后再提示更换付款方式
  return !!quote.value && !quoting.value && !submitting.value
})

// ----- 生命周期 -----
onMounted(async () => {
  try {
    const [ws, rs, cs] = await Promise.all([
      listWallets(),
      listRecipients(),
      listCards(undefined, 'active'),
    ])
    wallets.value = ws
    recipients.value = rs
    cards.value = cs

    const firstActive = ws.find((w) => w.status === 'active') ?? ws[0]
    if (firstActive) walletId.value = firstActive.id

    // 默认选主卡，否则第一张
    const defaultCard = cs.find((c) => c.isPrimary) ?? cs[0]
    if (defaultCard) cardId.value = defaultCard.id

    // 联系人列表为空自动切到手动
    if (rs.length === 0) {
      recipientMode.value = 'manual'
    }
  } catch {
    // 请求层已处理错误提示
  }
})

// ----- 操作 -----
function goBack() {
  router.back()
}

function formatAmount() {
  const raw = amountInput.value.trim()
  if (!raw) return
  const num = Number(raw)
  if (!Number.isNaN(num)) {
    amountInput.value = num.toFixed(2)
  }
}

function resetQuote() {
  quote.value = null
  quoteError.value = ''
}

// 输入过程中允许 "1"、"1.5" 这类写法，提交前统一转成两位小数
function normalizedAmount(): string | null {
  const raw = amountInput.value.trim()
  if (!/^\d+(\.\d{1,2})?$/.test(raw)) return null
  const num = Number(raw)
  if (!num || num <= 0) return null
  return num.toFixed(2)
}

async function doQuote() {
  const amount = normalizedAmount()
  if (!walletId.value || !recipientReady.value || !amount) {
    resetQuote()
    return
  }

  quoting.value = true
  quoteError.value = ''
  try {
    const payload: { walletId: string; cardId?: string; amount: string; recipientId?: string; accountNo?: string } = {
      walletId: walletId.value,
      amount,
    }
    if (cardId.value) payload.cardId = cardId.value
    if (recipientMode.value === 'contacts' && selectedRecipient.value) {
      payload.recipientId = selectedRecipient.value.id
      if (selectedRecipient.value.accountNo) payload.accountNo = selectedRecipient.value.accountNo
    } else {
      if (manualRecipient.value.accountNo) payload.accountNo = manualRecipient.value.accountNo.trim()
    }

    quote.value = await quoteTransfer(payload)
  } catch {
    quote.value = null
    quoteError.value = '获取报价失败，请稍后重试'
  } finally {
    quoting.value = false
  }
}

// 关键字段变化时自动刷新报价
watch([walletId, cardId, amountInput, recipientMode, selectedRecipientId, manualRecipient], () => {
  resetQuote()
  void doQuote()
}, { deep: true })

function openConfirm() {
  if (!canConfirm.value || !quote.value) return
  payDialogMode.value = 'confirm'
  showAllMethods.value = false
  showPayDialog.value = true
}

function closePayDialog() {
  if (paySubmitting.value) return
  showPayDialog.value = false
  payDialogMode.value = 'confirm'
  showAllMethods.value = false
  setPwdFirst.value = ''
  setPwdSecond.value = ''
  inputPwd.value = ''
  payShake.value = false
}

// 弹窗内点击「确认付款」：检查支付密码状态，决定进入输入还是设置流程
async function onDialogPay() {
  if (paySubmitting.value) return
  // 余额不足：在付款环节提示更换银行卡，并展开付款方式列表
  if (quote.value && !quote.value.sufficientBalance) {
    ElMessage.error('余额不足，请更换银行卡')
    showAllMethods.value = true
    shakePayDialog()
    return
  }
  paySubmitting.value = true
  try {
    if (payPwdStatus.value === 'unknown') {
      payPwdStatus.value = (await getPaymentPasswordStatus()).enabled ? 'set' : 'none'
    }
    if (payPwdStatus.value === 'set') {
      inputPwd.value = ''
      payDialogMode.value = 'inputpwd'
    } else {
      setPwdFirst.value = ''
      setPwdSecond.value = ''
      payDialogMode.value = 'setpwd'
    }
  } catch {
    // 请求层已处理错误提示
  } finally {
    paySubmitting.value = false
  }
}

function buildTransferPayload(paymentPassword?: string) {
  const amount = normalizedAmount()
  if (!amount) return null
  const payload: {
    walletId: string
    cardId?: string
    amount: string
    recipientId?: string
    recipient?: { displayName?: string; accountNo?: string; bankName?: string }
    paymentPassword?: string
  } = {
    walletId: walletId.value,
    amount,
  }
  if (cardId.value) payload.cardId = cardId.value
  if (recipientMode.value === 'contacts' && selectedRecipient.value) {
    payload.recipientId = selectedRecipient.value.id
  } else {
    payload.recipient = {
      displayName: manualRecipient.value.recipientName.trim(),
      accountNo: manualRecipient.value.accountNo.trim(),
      bankName: manualRecipient.value.bankName.trim() || undefined,
    }
  }
  if (paymentPassword) payload.paymentPassword = paymentPassword
  return payload
}

function shakePayDialog() {
  payShake.value = true
  window.setTimeout(() => {
    payShake.value = false
  }, 600)
}

// 弹窗键盘：设置支付密码（输两遍）
function setPwdPress(key: string) {
  if (paySubmitting.value) return
  if (setPwdFirst.value.length < 6) {
    setPwdFirst.value += key
    return
  }
  if (setPwdSecond.value.length < 6) {
    setPwdSecond.value += key
    if (setPwdSecond.value.length === 6) void submitSetPwd()
  }
}

function setPwdDelete() {
  if (paySubmitting.value) return
  if (setPwdSecond.value.length > 0) {
    setPwdSecond.value = setPwdSecond.value.slice(0, -1)
  } else {
    setPwdFirst.value = setPwdFirst.value.slice(0, -1)
  }
}

async function submitSetPwd() {
  if (paySubmitting.value) return
  if (setPwdFirst.value !== setPwdSecond.value) {
    ElMessage.error('两次输入的支付密码不一致，请重新设置')
    setPwdFirst.value = ''
    setPwdSecond.value = ''
    shakePayDialog()
    return
  }
  paySubmitting.value = true
  try {
    await setPaymentPassword({ newPassword: setPwdFirst.value })
    payPwdStatus.value = 'set'
    ElMessage.success('支付密码设置成功')
    inputPwd.value = ''
    payDialogMode.value = 'inputpwd'
  } catch {
    // 请求层已显示后端错误
    setPwdFirst.value = ''
    setPwdSecond.value = ''
    shakePayDialog()
  } finally {
    paySubmitting.value = false
  }
}

// 弹窗键盘：输入支付密码并提交转账
function inputPwdPress(key: string) {
  if (paySubmitting.value || inputPwd.value.length >= 6) return
  inputPwd.value += key
  if (inputPwd.value.length === 6) void submitTransfer()
}

function inputPwdDelete() {
  if (paySubmitting.value) return
  inputPwd.value = inputPwd.value.slice(0, -1)
}

async function submitTransfer() {
  const payload = buildTransferPayload(inputPwd.value)
  if (!payload) return
  paySubmitting.value = true
  try {
    const res = await createTransfer(payload)
    ElMessage.success('转账成功')
    showPayDialog.value = false
    await router.push({
      path: '/transfers/success',
      query: {
        transferId: res.id,
        referenceNo: res.referenceNo,
        amount: res.amount,
        recipient: res.recipient?.name ?? '',
      },
    })
  } catch {
    // 支付密码错误或余额变化：抖动提示并清空重输
    shakePayDialog()
    inputPwd.value = ''
  } finally {
    paySubmitting.value = false
  }
}

// 辅助函数
function getInitial(name: string): string {
  const trimmed = name.trim()
  if (!trimmed) return '?'
  return trimmed.slice(0, 1).toUpperCase()
}
</script>

<template>
  <div class="transfer-page">
    <PageLayout title="转账" @back="goBack">
    <div class="transfer">
      <!-- Step 1: 付款方式（钱包零钱与银行卡） -->
      <section class="card">
        <h3 class="card__title">付款方式</h3>
        <select v-model="cardId" class="field-select" :disabled="submitting">
          <option v-if="fromWallet" value="">
            钱包零钱 · {{ fromWallet.balance }} {{ currencyLabel(fromWallet.currencyCode) }}
          </option>
          <option v-for="c in cards" :key="c.id" :value="c.id">
            {{ cardBrandLabel(c) }} **** {{ c.last4 }} · {{ c.balance }} {{ currencyLabel(c.currencyCode) }}
          </option>
        </select>
        <p v-if="availableBalance" class="card__hint">
          可用余额: {{ availableBalance }}
          {{ currencyLabel(selectedCard?.currencyCode) }}
        </p>
      </section>

      <!-- Step 2: To recipient -->
      <section class="card">
        <h3 class="card__title">收款方</h3>

        <div v-if="recipients.length > 0" class="mode-toggle">
          <button
            type="button"
            class="mode-toggle__btn"
            :class="{ 'mode-toggle__btn--active': recipientMode === 'contacts' }"
            @click="recipientMode = 'contacts'"
          >
            联系人
          </button>
          <button
            type="button"
            class="mode-toggle__btn"
            :class="{ 'mode-toggle__btn--active': recipientMode === 'manual' }"
            @click="recipientMode = 'manual'"
          >
            手动填写
          </button>
        </div>

        <!-- 联系人模式 -->
        <div v-if="recipientMode === 'contacts'" class="chip-list">
          <button
            v-for="r in recipients"
            :key="r.id"
            type="button"
            class="chip"
            :class="{ 'chip--active': selectedRecipientId === r.id }"
            @click="selectedRecipientId = r.id"
          >
            <span class="chip__avatar">{{ getInitial(r.displayName) }}</span>
            <span class="chip__text">
              <span class="chip__name">{{ r.displayName }}</span>
              <span v-if="r.accountNo" class="chip__acc">
                **** {{ r.accountNo.slice(-4) }}
              </span>
            </span>
          </button>
        </div>

        <!-- 手动填写模式 -->
        <div v-else class="manual-form">
          <label class="field">
            <span class="field__label">收款人姓名</span>
            <input v-model="manualRecipient.recipientName" type="text" :disabled="submitting" placeholder="请输入姓名" />
          </label>
          <label class="field">
            <span class="field__label">账号</span>
            <input v-model="manualRecipient.accountNo" type="text" inputmode="numeric" enterkeyhint="next" :disabled="submitting" placeholder="请输入账号" />
          </label>
          <label class="field">
            <span class="field__label">开户行 (可选)</span>
            <input v-model="manualRecipient.bankName" type="text" :disabled="submitting" placeholder="如: 招商银行" />
          </label>
        </div>
      </section>

      <!-- Step 3: Amount -->
      <section class="card">
        <h3 class="card__title">转账金额</h3>
        <div class="amount-wrap">
          <span class="amount-wrap__symbol">{{ currencyLabel(fromWallet?.currencyCode) }}</span>
          <input
            v-model="amountInput"
            type="text"
            inputmode="decimal"
            placeholder="0.00"
            :disabled="submitting"
            @blur="formatAmount"
          />
        </div>
        <p v-if="fromWallet" class="card__hint">
          可用余额: {{ availableBalance }} {{ currencyLabel(fromWallet.currencyCode) }}
          <template v-if="selectedCard">（卡 **** {{ selectedCard.last4 }}）</template>
        </p>
      </section>

      <!-- Quote -->
      <section v-if="amountReady && recipientReady && walletId" class="card">
        <h3 class="card__title">费用预览</h3>

        <div v-if="quoting" class="quote-loading">正在获取报价…</div>

        <div v-else-if="quoteError" class="quote-error">
          ⚠️ {{ quoteError }}
        </div>

        <template v-else-if="quote">
          <div class="quote-row">
            <span class="quote-row__label">转账金额</span>
            <span class="quote-row__value">
              {{ quote.amount }} {{ currencyLabel(quote.currencyCode) }}
            </span>
          </div>
          <div class="quote-row">
            <span class="quote-row__label">手续费 ({{ Number(quote.feeRate) * 100 }}%)</span>
            <span class="quote-row__value">
              {{ quote.feeAmount }} {{ currencyLabel(quote.currencyCode) }}
            </span>
          </div>
          <div class="quote-row quote-row--bold">
            <span class="quote-row__label">合计扣款</span>
            <span class="quote-row__value quote-row__value--total">
              {{ quote.totalAmount }} {{ currencyLabel(quote.currencyCode) }}
            </span>
          </div>
        </template>
      </section>

      <!-- Confirm -->
      <button
        class="submit-btn"
        type="button"
        :disabled="!canConfirm"
        @click="openConfirm"
      >
        {{ submitting ? '转账中…' : '确认转账' }}
      </button>
    </div>
    </PageLayout>

    <!-- 确认付款弹窗 -->
    <div v-if="showPayDialog" class="pay-dialog__mask" @click.self="closePayDialog">
      <div class="pay-dialog" :class="{ 'pay-dialog--shake': payShake }">
        <!-- 模式一：确认付款 -->
        <template v-if="payDialogMode === 'confirm'">
          <div class="pay-dialog__header pay-dialog__header--center">
            <button class="pay-dialog__close" type="button" @click="closePayDialog">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
            <span class="pay-dialog__title">向 {{ recipientLabel || '—' }} 转账</span>
            <span class="pay-dialog__header-spacer"></span>
          </div>

          <div class="pay-dialog__amount">
            <span class="pay-dialog__symbol">{{ currencySymbol(quote?.currencyCode) }}</span>
            <span>{{ quote?.amount ?? '--' }}</span>
          </div>

          <div class="pay-dialog__divider"></div>

          <div class="pay-dialog__method-header">
            <span>付款方式</span>
            <button
              v-if="paymentMethods.length > 1"
              class="pay-dialog__change"
              type="button"
              @click="showAllMethods = !showAllMethods"
            >
              {{ showAllMethods ? '收起' : '更改' }}
            </button>
          </div>

          <!-- 已选付款方式 -->
          <div v-if="selectedMethod" class="pay-option pay-option--active pay-option--single">
            <span
              class="method-icon"
              :class="selectedMethod.isWallet ? 'method-icon--wallet' : 'method-icon--card'"
            >
              {{ selectedMethod.isWallet ? '¥' : selectedMethod.label.charAt(0) }}
            </span>
            <span class="pay-option__text">
              <span class="pay-option__name">{{ selectedMethod.label }}</span>
              <span class="pay-option__desc">{{ selectedMethod.desc }}</span>
            </span>
            <svg class="pay-option__check" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#07c160" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
          </div>

          <!-- 切换付款方式 -->
          <div v-if="showAllMethods" class="pay-dialog__method-list">
            <button
              v-for="m in paymentMethods"
              :key="m.id || 'wallet'"
              type="button"
              class="pay-option"
              :class="{ 'pay-option--active': cardId === m.id }"
              @click="cardId = m.id; showAllMethods = false"
            >
              <span
                class="method-icon"
                :class="m.isWallet ? 'method-icon--wallet' : 'method-icon--card'"
              >
                {{ m.isWallet ? '¥' : m.label.charAt(0) }}
              </span>
              <span class="pay-option__text">
                <span class="pay-option__name">{{ m.label }}</span>
                <span class="pay-option__desc">{{ m.desc }}</span>
              </span>
              <svg v-if="cardId === m.id" class="pay-option__check" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#07c160" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
            </button>
          </div>

          <div v-if="quoting" class="pay-dialog__hint-info">正在更新报价…</div>
          <div v-else-if="quote && !quote.sufficientBalance" class="pay-dialog__warn">
            ⚠️ 余额不足，请更换银行卡
          </div>

          <button
            class="pay-dialog__btn"
            type="button"
            :disabled="!canConfirm || paySubmitting"
            @click="onDialogPay"
          >
            {{ paySubmitting ? '请稍候…' : '支付' }}
          </button>
        </template>

        <!-- 模式二：设置支付密码（输两遍） -->
        <template v-else-if="payDialogMode === 'setpwd'">
          <div class="pay-dialog__header">
            <span class="pay-dialog__title">设置支付密码</span>
            <button class="pay-dialog__close" type="button" @click="closePayDialog">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <div class="pay-dialog__pwd-title">
            {{ setPwdFirst.length < 6 ? '请设置 6 位数字支付密码' : '请再次输入以确认' }}
          </div>
          <div class="pay-dialog__dots">
            <span
              v-for="i in 6"
              :key="i"
              class="pdot"
              :class="{ 'pdot--filled': activeSetPwd.length >= i }"
            />
          </div>
          <div class="pay-dialog__keypad">
            <button
              v-for="(k, idx) in dialogKeys"
              :key="idx"
              type="button"
              class="pkey"
              :class="{ 'pkey--blank': k === '' }"
              :disabled="k === ''"
              @click="k === 'del' ? setPwdDelete() : setPwdPress(k)"
            >
              <svg v-if="k === 'del'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 5H8.6a2 2 0 0 0-1.5.7L2.4 11a1.6 1.6 0 0 0 0 2l4.7 5.3a2 2 0 0 0 1.5.7H21a1.5 1.5 0 0 0 1.5-1.5v-12A1.5 1.5 0 0 0 21 5z"/><path d="M17 9.5l-5 5M12 9.5l5 5"/></svg>
              <span v-else>{{ k }}</span>
            </button>
          </div>
        </template>

        <!-- 模式三：输入支付密码 -->
        <template v-else>
          <div class="pay-dialog__header">
            <span class="pay-dialog__title">请输入支付密码</span>
            <button class="pay-dialog__close" type="button" @click="closePayDialog">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </div>
          <div class="pay-dialog__pwd-amount">
            {{ currencyLabel(quote?.currencyCode) }} {{ quote?.amount }}
          </div>
          <div class="pay-dialog__pwd-sub">收款人 {{ recipientLabel || '—' }}</div>
          <div class="pay-dialog__dots" :class="{ 'pay-dialog__dots--shake': payShake }">
            <span
              v-for="i in 6"
              :key="i"
              class="pdot"
              :class="{ 'pdot--filled': inputPwd.length >= i }"
            />
          </div>
          <div class="pay-dialog__keypad">
            <button
              v-for="(k, idx) in dialogKeys"
              :key="idx"
              type="button"
              class="pkey"
              :class="{ 'pkey--blank': k === '' }"
              :disabled="k === ''"
              @click="k === 'del' ? inputPwdDelete() : inputPwdPress(k)"
            >
              <svg v-if="k === 'del'" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 5H8.6a2 2 0 0 0-1.5.7L2.4 11a1.6 1.6 0 0 0 0 2l4.7 5.3a2 2 0 0 0 1.5.7H21a1.5 1.5 0 0 0 1.5-1.5v-12A1.5 1.5 0 0 0 21 5z"/><path d="M17 9.5l-5 5M12 9.5l5 5"/></svg>
              <span v-else>{{ k }}</span>
            </button>
          </div>
        </template>
      </div>
    </div>

    <BottomNav active="transfers" />
  </div>
</template>

<style scoped>
.transfer-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-width: 390px;
  margin: 0 auto;
  overflow: hidden;
}
.transfer-page :deep(.page-layout) {
  flex: 1;
  height: auto;
  min-height: 0;
}
.transfer-page :deep(.page-layout__home) {
  display: none;
}

.transfer {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 24px;
}

.card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--app-line, #f0eeef);
  background: #fff;
  box-shadow: 0 2px 8px rgba(24, 39, 43, 0.03);
}

.card__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #353333;
}

.card__hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #696565;
}

.field-select,
.field input,
.amount-wrap input {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 12px;
  outline: none;
  background: #fff;
  font-family: inherit;
  font-size: 14px;
  color: #353333;
  box-sizing: border-box;
}

.field-select:focus,
.field input:focus,
.amount-wrap input:focus {
  border-color: #17b7bd;
}

.mode-toggle {
  display: inline-flex;
  padding: 3px;
  margin-bottom: 12px;
  border-radius: 10px;
  background: #f6f6f6;
}

.mode-toggle__btn {
  padding: 6px 16px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: #696565;
  cursor: pointer;
}

.mode-toggle__btn--active {
  background: #fff;
  color: #17b7bd;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 999px;
  background: #fff;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.chip--active {
  border-color: #17b7bd;
  box-shadow: 0 0 0 2px rgba(23, 183, 189, 0.15);
}

.chip__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #17b7bd;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.chip__text {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.chip__name {
  font-size: 13px;
  font-weight: 600;
  color: #353333;
}

.chip__acc {
  font-size: 11px;
  color: #696565;
}

.manual-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field__label {
  font-size: 12px;
  color: #696565;
}

.amount-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 12px;
  background: #fff;
}

.amount-wrap:focus-within {
  border-color: #17b7bd;
}

.amount-wrap__symbol {
  font-size: 14px;
  font-weight: 600;
  color: #17b7bd;
}

.amount-wrap input {
  flex: 1;
  border: 0;
  padding: 0;
  height: 44px;
  background: transparent;
}

.amount-wrap input:focus {
  border: 0;
}

.quote-loading {
  font-size: 13px;
  color: #696565;
}

.quote-error {
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(231, 76, 60, 0.08);
  font-size: 13px;
  color: #e74c3c;
}

.quote-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.quote-row:last-of-type {
  border-bottom: 0;
}

.quote-row__label {
  font-size: 13px;
  color: #696565;
}

.quote-row__value {
  font-size: 14px;
  color: #353333;
}

.quote-row--bold .quote-row__label,
.quote-row--bold .quote-row__value {
  font-weight: 700;
}

.quote-row__value--total {
  color: #17b7bd;
}

.quote-warn {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(231, 76, 60, 0.08);
  font-size: 13px;
  color: #e74c3c;
}

.submit-btn {
  width: 100%;
  height: 52px;
  margin-top: 8px;
  border: 0;
  border-radius: 14px;
  background: var(--app-primary, #17b7bd);
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.16);
}

.submit-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* ===== 确认付款弹窗 ===== */
.pay-dialog__mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
}
.pay-dialog {
  width: 100%;
  max-width: 390px;
  max-height: 86vh;
  max-height: 86dvh;
  overflow-y: auto;
  padding: 16px 20px calc(20px + env(safe-area-inset-bottom));
  border-radius: 14px 14px 0 0;
  background: #ffffff;
  animation: pay-dialog-up 0.22s ease-out;
}
@keyframes pay-dialog-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.pay-dialog--shake {
  animation: pay-dialog-shake 0.5s;
}
@keyframes pay-dialog-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-10px); }
  50% { transform: translateX(10px); }
  75% { transform: translateX(-6px); }
}
.pay-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.pay-dialog__header--center {
  position: relative;
  justify-content: flex-start;
}
.pay-dialog__header--center .pay-dialog__title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}
.pay-dialog__header-spacer {
  width: 28px;
  flex-shrink: 0;
}
.pay-dialog__title {
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: #353333;
}
.pay-dialog__close {
  border: 0;
  background: transparent;
  color: #afafaf;
  cursor: pointer;
  padding: 4px;
  display: inline-flex;
  z-index: 1;
}
.pay-dialog__amount {
  text-align: center;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 36px;
  font-weight: 700;
  color: #353333;
  margin: 16px 0 20px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}
.pay-dialog__amount .pay-dialog__symbol {
  font-size: 28px;
  font-weight: 700;
  color: #353333;
  margin-right: 2px;
}
.pay-dialog__symbol {
  font-size: 14px;
  font-weight: 500;
  color: #696565;
  margin-right: 4px;
}
.pay-dialog__rows {
  margin: 8px 0 4px;
}
.pay-dialog__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 0;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 13px;
  color: #696565;
}
.pay-dialog__row b {
  font-weight: 600;
  color: #353333;
  font-variant-numeric: tabular-nums;
}
.pay-dialog__row--total b {
  color: #353333;
  font-size: 15px;
}
.pay-dialog__section-label {
  margin: 10px 0 6px;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 12px;
  color: #afafaf;
}
.pay-dialog__divider {
  height: 1px;
  background: #f0f0f0;
  margin: 0 0 16px;
}
.pay-dialog__method-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 13px;
  color: #696565;
}
.pay-dialog__change {
  border: 0;
  background: transparent;
  color: #576b95;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
.pay-option--single {
  cursor: default;
  margin-bottom: 4px;
}
.pay-dialog__method-list {
  margin-top: 4px;
}
.pay-dialog__method-list .pay-option {
  margin-bottom: 6px;
}
.pay-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px;
  margin-bottom: 8px;
  border: 1px solid #f6f6f6;
  border-radius: 8px;
  background: #ffffff;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.pay-option--active {
  background: #fdf9ec;
  border-color: #fdf9ec;
}
.method-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.method-icon--wallet {
  background: #ffd666;
  color: #8a6d1a;
}
.method-icon--card {
  background: #e8f1ff;
  color: #2f6fd0;
}
.pay-option__text {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.pay-option__name {
  font-size: 14px;
  font-weight: 500;
  color: #353333;
}
.pay-option__desc {
  font-size: 12px;
  color: #afafaf;
  margin-top: 2px;
}
.pay-dialog__hint-info {
  font-size: 12px;
  color: #afafaf;
  margin: 4px 0 8px;
}
.pay-dialog__warn {
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(230, 67, 64, 0.08);
  font-size: 13px;
  color: #e64340;
  margin-bottom: 8px;
}
.pay-dialog__btn {
  display: block;
  width: 80%;
  height: 44px;
  margin: 16px auto 0;
  border: 0;
  border-radius: 6px;
  background: #07c160;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 16px;
  font-weight: 500;
  color: #ffffff;
  cursor: pointer;
  box-shadow: none;
}
.pay-dialog__btn:active {
  background: #06ad56;
}
.pay-dialog__btn:disabled {
  background: #8fd9ac;
  cursor: not-allowed;
}
.pay-dialog__pwd-title {
  text-align: center;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 13px;
  color: #afafaf;
  margin-top: 6px;
}
.pay-dialog__pwd-amount {
  text-align: center;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 26px;
  font-weight: 600;
  color: #353333;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
}
.pay-dialog__pwd-sub {
  text-align: center;
  font-size: 13px;
  color: #afafaf;
  margin: 4px 0 0;
}
.pay-dialog__dots {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin: 18px 0 6px;
}
.pay-dialog__dots--shake {
  animation: pay-dialog-shake 0.5s;
}
.pdot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1.5px solid #d5d5d5;
  background: #ffffff;
  transition: background 0.15s, border-color 0.15s;
}
.pdot--filled {
  background: #07c160;
  border-color: #07c160;
}
.pay-dialog__keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}
.pkey {
  height: 46px;
  border: 0;
  border-radius: 6px;
  background: #f5f5f5;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 20px;
  font-weight: 500;
  color: #353333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  transition: background 0.1s;
}
.pkey:active {
  background: #e5e5e5;
}
.pkey:disabled {
  background: transparent;
  cursor: default;
}
</style>
