<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import { listWallets } from '@/api/wallets'
import { listCategories } from '@/api/categories'
import { createTransaction } from '@/api/transactions'
import { ElMessage } from 'element-plus'
import type { Category, WalletResponse } from '@/api/types'
import { categoryIcon } from '@/utils/categoryIcons'

const router = useRouter()
const route = useRoute()

// State
function nowLocal(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const type = ref<'income' | 'expense'>(route.query.type === 'income' ? 'income' : 'expense')

const amount = ref<string>('')
const keypadOpen = ref<boolean>(false)
const KEYS: string[] = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', '⌫']
const selectedCategoryId = ref<string>('')
const selectedWalletId = ref<string>('')
const note = ref<string>('')
const occurredAt = ref<string>(nowLocal())
const submitting = ref<boolean>(false)

const wallets = ref<WalletResponse[]>([])
const categories = ref<Category[]>([])

// Helpers
function to2Decimals(val: string): string {
  const num = parseFloat(val)
  if (isNaN(num)) return '0.00'
  return num.toFixed(2)
}

// 内置数字键盘：只保留数字和一个小数点，最多 2 位小数、整数最多 9 位
function normalizeAmountInput(raw: string): string {
  let s = raw.replace(/[^0-9.]/g, '')
  const dotIdx = s.indexOf('.')
  if (dotIdx !== -1) {
    s = s.slice(0, dotIdx + 1) + s.slice(dotIdx + 1).replace(/\./g, '')
  }
  const [intRaw, dec = ''] = s.split('.')
  const int = intRaw.slice(0, 9)
  if (s.includes('.')) return `${int || '0'}.${dec.slice(0, 2)}`
  return int
}

function openKeypad() {
  keypadOpen.value = true
}

function closeKeypad() {
  keypadOpen.value = false
}

function onKey(k: string) {
  if (k === '⌫') {
    amount.value = amount.value.slice(0, -1)
    return
  }
  amount.value = normalizeAmountInput((amount.value === '0.00' ? '' : amount.value) + k)
}

// Computed
const selectedWallet = computed<WalletResponse | undefined>(() =>
  wallets.value.find((w) => w.id === selectedWalletId.value)
)

const currencyCode = computed<string>(() => selectedWallet.value?.currencyCode ?? '')

function currencyLabel(code: string): string {
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

const amountIsValid = computed<boolean>(() => {
  const n = parseFloat(amount.value)
  return !isNaN(n) && n > 0
})

const categoryIsValid = computed<boolean>(() => selectedCategoryId.value.length > 0)

const canSubmit = computed<boolean>(
  () => !submitting.value && !!selectedWalletId.value && amountIsValid.value && categoryIsValid.value
)

// Actions
async function reloadCategories() {
  try {
    const res = await listCategories({ type: type.value })
    categories.value = res?.items ?? []
    // Reset selection if current not in list
    if (selectedCategoryId.value && !categories.value.some((c) => c.id === selectedCategoryId.value)) {
      selectedCategoryId.value = ''
    }
  } catch {
    categories.value = []
  }
}

async function loadWallets() {
  try {
    const list = await listWallets()
    wallets.value = list ?? []
    if (!selectedWalletId.value && wallets.value.length > 0) {
      selectedWalletId.value = wallets.value[0].id
    }
  } catch {
    wallets.value = []
  }
}

function selectCategory(id: string) {
  selectedCategoryId.value = id
}

function switchType(t: 'income' | 'expense') {
  if (t === type.value) return
  type.value = t
  selectedCategoryId.value = ''
}

async function handleSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const payload = {
      walletId: selectedWalletId.value,
      type: type.value,
      amount: to2Decimals(amount.value),
      categoryId: selectedCategoryId.value,
      note: note.value.trim() || undefined,
      occurredAt: occurredAt.value ? new Date(occurredAt.value).toISOString() : undefined,
    }
    await createTransaction(payload)
    ElMessage.success('保存成功')
    router.replace('/wallet')
  } catch (err) {
    // request layer already shows message; show a fallback
    if (!err) ElMessage.error('保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// Watch
watch(type, () => {
  reloadCategories()
})

// Lifecycle
onMounted(async () => {
  await Promise.all([loadWallets(), reloadCategories()])
})
</script>

<template>
  <PageLayout title="记一笔" @back="router.back()">
    <div class="page">
      <!-- Type toggle -->
      <div class="type-toggle">
        <button
          class="pill pill-expense"
          :class="{ active: type === 'expense' }"
          type="button"
          @click="switchType('expense')"
        >
          支出
        </button>
        <button
          class="pill pill-income"
          :class="{ active: type === 'income' }"
          type="button"
          @click="switchType('income')"
        >
          收入
        </button>
      </div>

      <!-- Amount -->
      <section class="card amount-card" @click="openKeypad">
        <div class="amount-row">
          <span class="currency">{{ currencyLabel(currencyCode) }}</span>
          <span class="amount-input" :class="{ 'amount-input--active': keypadOpen }">
            {{ amount || '0.00' }}<i v-if="keypadOpen" class="amount-caret" />
          </span>
        </div>
      </section>

      <!-- Wallet selector (only if multiple) -->
      <section v-if="wallets.length > 1" class="card">
        <div class="section-title">选择钱包</div>
        <select v-model="selectedWalletId" class="wallet-select">
          <option v-for="w in wallets" :key="w.id" :value="w.id">
            {{ currencyLabel(w.currencyCode) }}
          </option>
        </select>
      </section>

      <!-- Category grid -->
      <section class="card">
        <div class="section-title">选择分类</div>
        <div class="category-grid">
          <button
            v-for="c in categories"
            :key="c.id"
            class="category-item"
            :class="{ active: selectedCategoryId === c.id }"
            type="button"
            @click="selectCategory(c.id)"
          >
            <span class="category-icon" :style="{ backgroundColor: c.color || '#ccc' }">
              <img
                v-if="categoryIcon(c.icon)"
                class="category-icon__image"
                :src="categoryIcon(c.icon)"
                :alt="c.name"
              />
              <template v-else>{{ c.name.slice(0, 1) }}</template>
            </span>
            <span class="category-label">{{ c.name }}</span>
          </button>
        </div>
        <div v-if="categories.length === 0" class="empty-tip">
          暂无分类
        </div>
      </section>

      <!-- Occurred at -->
      <section class="card">
        <div class="section-title">发生时间</div>
        <input
          v-model="occurredAt"
          class="datetime-input"
          type="datetime-local"
        />
      </section>

      <!-- Note -->
      <section class="card">
        <div class="section-title">备注</div>
        <textarea
          v-model="note"
          class="note-input"
          maxlength="255"
          placeholder="可选，最多 255 字"
          rows="2"
        />
        <div class="note-count">{{ note.length }}/255</div>
      </section>

      <!-- Submit -->
      <div class="submit-wrap">
        <button
          class="submit-btn"
          :disabled="!canSubmit"
          type="button"
          @click="handleSubmit"
        >
          {{ submitting ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>

    <!-- 内置数字键盘 -->
    <div v-if="keypadOpen" class="keypad-mask" @click.self="closeKeypad">
      <div class="keypad-sheet">
        <div class="keypad-head">
          <span class="keypad-head__label">{{ currencyLabel(currencyCode) || '金额' }}</span>
          <button class="keypad-done" type="button" @click="closeKeypad">完成</button>
        </div>
        <div class="keypad">
          <button
            v-for="k in KEYS"
            :key="k"
            type="button"
            class="key"
            :class="{ 'key--del': k === '⌫' }"
            @click="onKey(k)"
          >
            <span v-if="k === '⌫'" class="key__icon">
              <svg width="24" height="18" viewBox="0 0 24 18" fill="none">
                <path d="M15.5 1.5 L22 9 L15.5 16.5 H3.5 C2.12 16.5 1 15.38 1 14 V4 C1 2.62 2.12 1.5 3.5 1.5 Z"
                  stroke="#353333" stroke-width="1.5" stroke-linejoin="round" fill="none"/>
                <line x1="8" y1="6.5" x2="13" y2="11.5" stroke="#353333" stroke-width="1.5" stroke-linecap="round"/>
                <line x1="13" y1="6.5" x2="8" y2="11.5" stroke="#353333" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </span>
            <span v-else>{{ k }}</span>
          </button>
        </div>
      </div>
    </div>
  </PageLayout>
</template>

<style scoped lang="scss">
.page {
  max-width: 390px;
  margin: 0 auto;
  padding: 8px 20px 32px;
  background: #f7f6f8;
  min-height: 100%;
  box-sizing: border-box;
}

.card {
  background: #fff;
  border: 1px solid #f0eeef;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 14px;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.section-title {
  font-size: 13px;
  color: #696565;
  margin-bottom: 12px;
  font-weight: 600;
}

/* Type toggle */
.type-toggle {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.pill {
  flex: 1;
  height: 40px;
  border-radius: 14px;
  border: 1px solid #f0eeef;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
}

.pill-expense {
  color: #696565;
  border-color: #f0eeef;
  &.active {
    background: #17b7bd;
    border-color: #17b7bd;
    color: #ffffff;
  }
}

.pill-income {
  color: #696565;
  border-color: #f0eeef;
  &.active {
    background: #17b7bd;
    border-color: #17b7bd;
    color: #ffffff;
  }
}

/* Amount */
.amount-card {
  display: flex;
  justify-content: center;
  padding: 24px 16px;
}

.amount-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.currency {
  font-size: 18px;
  color: #696565;
  font-weight: 600;
}

.amount-input {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  outline: none;
  width: 200px;
  min-height: 52px;
  text-align: center;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 44px;
  font-weight: 600;
  color: #353333;
  background: transparent;
  letter-spacing: 0.5px;
  cursor: pointer;
}

.amount-caret {
  width: 2px;
  height: 40px;
  margin-left: 2px;
  background: #17b7bd;
  animation: caret-blink 1s steps(1) infinite;
}

@keyframes caret-blink {
  0%, 49% { opacity: 1; }
  50%, 100% { opacity: 0; }
}

/* 内置数字键盘 */
.keypad-mask {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 390px;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.35);
}

.keypad-sheet {
  width: 100%;
  padding: 10px 16px calc(18px + env(safe-area-inset-bottom));
  background: #f7f6f8;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -8px 30px rgba(0, 0, 0, 0.12);
  box-sizing: border-box;
}

.keypad-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 10px;
}
.keypad-head__label {
  font-size: 14px;
  color: #696565;
}
.keypad-done {
  border: 0;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  color: #17b7bd;
  cursor: pointer;
}

.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.key {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  border: 0;
  border-radius: 8px;
  background: #ffffff;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 24px;
  font-weight: 500;
  color: #353333;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: transform 0.08s ease;
}
.key:active {
  transform: scale(0.95);
}
.key__icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Wallet select */
.wallet-select {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  border: 1px solid #f0eeef;
  padding: 0 12px;
  font-size: 15px;
  background: #ffffff;
  box-sizing: border-box;
}

/* Category grid */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px 8px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border: 2px solid transparent;
  background: transparent;
  padding: 8px 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease, background 0.15s ease;

  &.active {
    border-color: #17b7bd;
    transform: scale(1.05);
    background: rgba(23, 183, 189, 0.06);
  }
}

.category-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.category-icon__image {
  width: 26px;
  height: 26px;
  object-fit: contain;
}

.category-label {
  font-size: 12px;
  color: #696565;
  text-align: center;
  line-height: 1.2;
}

.empty-tip {
  text-align: center;
  color: #afafaf;
  font-size: 13px;
  padding: 12px 0 4px;
}

/* Datetime */
.datetime-input {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  border: 1px solid #f0eeef;
  padding: 0 12px;
  font-size: 15px;
  background: #ffffff;
  box-sizing: border-box;
  color: #353333;
}

/* Note */
.note-input {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #f0eeef;
  padding: 10px 12px;
  font-size: 14px;
  background: #ffffff;
  box-sizing: border-box;
  resize: vertical;
  color: #353333;
  font-family: inherit;
  outline: none;

  &:focus {
    border-color: #17b7bd;
  }
}

.note-count {
  text-align: right;
  font-size: 12px;
  color: #bbb;
  margin-top: 6px;
}

/* Submit */
.submit-wrap {
  margin-top: 8px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 14px;
  border: none;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  background: var(--app-primary, #17b7bd);
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.16);
  transition: opacity 0.2s ease, transform 0.15s ease;

  &:active:not(:disabled) {
    transform: scale(0.99);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
</style>
