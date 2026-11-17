<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

import PageLayout from '@/components/PageLayout.vue'
import { listCards, createCard, removeCard } from '@/api/cards'
import { listWallets } from '@/api/wallets'
import { getTransactionSummary } from '@/api/transactions'
import type {
  CardResponse,
  CardBrand,
  CardType,
  CreateCardPayload,
} from '@/api/types'

const router = useRouter()

const cards = ref<CardResponse[]>([])
const loading = ref(false)
const submitting = ref(false)
const showAdd = ref(false)
const monthSpent = ref('0')

// 新增表单
const addForm = ref<{
  cardType: CardType
  brand: CardBrand
  last4: string
  holderName: string
  expiryMonth: number
  expiryYear: number
  currencyCode: string
  isPrimary?: boolean
}>({
  cardType: 'debit',
  brand: 'visa',
  last4: '',
  holderName: '',
  expiryMonth: new Date().getMonth() + 1,
  expiryYear: new Date().getFullYear(),
  currencyCode: 'CNY',
})

const expiryYears = computed(() => {
  const start = new Date().getFullYear()
  return Array.from({ length: 11 }, (_, i) => start + i)
})

// 中国银行卡优先展示，其余保持原顺序
function putBocFirst(list: CardResponse[]): CardResponse[] {
  const index = list.findIndex((card) => card.brand === 'boc')
  if (index <= 0) return list
  const next = [...list]
  const [boc] = next.splice(index, 1)
  next.unshift(boc)
  return next
}

async function loadCards() {
  loading.value = true
  try {
    cards.value = putBocFirst(await listCards())
    // 进入页面默认全部收起
    collapsedIds.value = cards.value.map((c) => c.id)
  } catch {
    // 请求层已处理
  } finally {
    loading.value = false
  }
}

// 本月累计支出
async function loadMonthSpent() {
  try {
    const wallets = await listWallets()
    const walletId = wallets[0]?.id
    if (!walletId) return
    const now = new Date()
    const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    const summary = await getTransactionSummary(month, walletId)
    monthSpent.value = summary.totalSpent ?? '0'
  } catch {
    // 请求层已处理
  }
}

onMounted(() => {
  loadCards()
  loadMonthSpent()
})

function goBack() {
  router.back()
}

function brandLabel(b: CardBrand): string {
  const map: Record<CardBrand, string> = {
    visa: '威士',
    mastercard: '万事达',
    unionpay: '银联',
    icbc: '中国工商银行',
    abc: '中国农业银行',
    boc: '中国银行',
    ccb: '中国建设银行',
    bocom: '交通银行',
    psbc: '中国邮政储蓄银行',
  }
  return map[b]
}

function brandInitial(b: CardBrand): string {
  return brandLabel(b).slice(0, 1)
}

// 卡面缩略图标记：中国银行使用长城卡的「长」字印记
function chipMark(b: CardBrand): string {
  return b === 'boc' ? '长' : brandInitial(b)
}

function cardTypeLabel(t: CardType): string {
  return t === 'debit' ? '储蓄卡' : '信用卡'
}

function statusLabel(s: string): string {
  const map: Record<string, string> = {
    active: '正常',
    expired: '已过期',
    frozen: '已冻结',
    removed: '已注销',
  }
  return map[s] ?? '未知状态'
}

function expLabel(c: CardResponse): string {
  return `${String(c.expiryMonth).padStart(2, '0')}/${String(c.expiryYear).slice(-2)}`
}

function dateLabel(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}-${d.getDate()}`
}

function fmtBalance(raw?: string): string {
  const n = Number(raw ?? 0)
  return `¥${n.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

// 依据卡 ID 生成稳定的完整卡号（与钱包页展示逻辑一致）
function fullCardNumber(card: CardResponse): string {
  const prefixMap: Record<string, string> = { visa: '4231', mastercard: '5412', unionpay: '6222' }
  const prefix = prefixMap[card.brand ?? ''] ?? '0000'
  let hash = 0
  for (const ch of card.id) hash = (hash * 31 + ch.charCodeAt(0)) % 100000000
  const middle = String(hash).padStart(8, '0')
  const last4 = (card.last4 || '').padStart(4, '0')
  const digits = (prefix + middle + last4).slice(0, 16)
  return digits.replace(/(.{4})/g, '$1 ').trim()
}

function showBalance(c: CardResponse) {
  ElMessage.success(`${brandLabel(c.brand)} **** ${c.last4} 可用余额 ${fmtBalance(c.balance)}`)
}

function showNumber(c: CardResponse) {
  ElMessage.info(fullCardNumber(c))
}

function goBills() {
  router.push('/transactions')
}

function onNote() {
  ElMessage.info('备注功能暂未开放')
}

function toggleAdd() {
  showAdd.value = !showAdd.value
  resetAddForm()
}

// ========== 展开 / 收起 ==========
const collapsedIds = ref<string[]>([])

function isCollapsed(id: string): boolean {
  return collapsedIds.value.includes(id)
}

function toggleGroup(id: string) {
  collapsedIds.value = collapsedIds.value.includes(id)
    ? collapsedIds.value.filter((x) => x !== id)
    : [...collapsedIds.value, id]
}

const allCollapsed = computed(
  () => cards.value.length > 0 && cards.value.every((c) => collapsedIds.value.includes(c.id)),
)

function toggleAll() {
  collapsedIds.value = allCollapsed.value ? [] : cards.value.map((c) => c.id)
}

async function confirmDelete(card: CardResponse) {
  try {
    await ElMessageBox.confirm(
      `确定要解绑银行卡 **** ${card.last4} 吗？`,
      '解绑银行卡',
      {
        confirmButtonText: '解绑',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    await removeCard(card.id)
    ElMessage.success('已解绑')
    await loadCards()
  } catch {
    // 请求层已处理
  }
}

function resetAddForm() {
  const now = new Date()
  addForm.value = {
    cardType: 'debit',
    brand: 'visa',
    last4: '',
    holderName: '',
    expiryMonth: now.getMonth() + 1,
    expiryYear: now.getFullYear(),
    currencyCode: 'CNY',
  }
}

async function submitAdd() {
  const f = addForm.value

  if (!/^\d{4}$/.test(f.last4)) {
    ElMessage.warning('卡号后 4 位需为 4 位数字')
    return
  }
  if (!f.holderName.trim()) {
    ElMessage.warning('请输入持卡人姓名')
    return
  }
  if (!/^[A-Z]{3}$/.test(f.currencyCode)) {
    ElMessage.warning('币种应为 3 位大写字母')
    return
  }

  const payload: CreateCardPayload = {
    cardType: f.cardType,
    brand: f.brand,
    last4: f.last4,
    holderName: f.holderName.trim(),
    expiryMonth: Number(f.expiryMonth),
    expiryYear: Number(f.expiryYear),
    currencyCode: f.currencyCode,
    isPrimary: f.isPrimary,
  }

  submitting.value = true
  try {
    await createCard(payload)
    ElMessage.success('绑卡成功')
    showAdd.value = false
    resetAddForm()
    await loadCards()
  } catch {
    // 请求层已处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="cards-root">
    <PageLayout title="银行卡" @back="goBack">
      <template #actions>
        <button class="add-card-btn" type="button" @click="toggleAdd">
          <span class="add-card-btn__plus">＋</span> 添加卡
        </button>
      </template>

      <div class="cards-page">
        <!-- 汇总 -->
        <section class="summary">
          <div class="summary__left">
            <p class="summary__count">
              <strong>{{ cards.length }}</strong>
              <span>张银行卡</span>
            </p>
            <p class="summary__meta">
              储蓄卡可查余额
              <button class="summary__link" type="button" @click="goBills">查看账单</button>
            </p>
          </div>
          <div class="summary__right">
            <span class="summary__label">本月累计支出</span>
            <span class="summary__amount">{{ fmtBalance(monthSpent) }}</span>
          </div>
        </section>

        <!-- 备注提示 -->
        <div v-if="!showAdd" class="note-banner">
          <span class="note-banner__text">备注为「常用」卡，付款时能快速找到</span>
          <button class="note-banner__btn" type="button" @click="onNote">去备注</button>
        </div>

        <!-- 加载骨架 -->
        <div v-if="loading" class="skeleton">
          <div v-for="i in 2" :key="i" class="skeleton__card" />
        </div>

        <!-- 空状态 -->
        <div v-else-if="cards.length === 0 && !showAdd" class="empty">
          <div class="empty__icon">💳</div>
          <p class="empty__title">还没有绑定银行卡</p>
          <p class="empty__hint">点击右上角「添加卡」，绑定第一张银行卡吧</p>
        </div>

        <!-- 银行卡列表 -->
        <div v-else-if="!showAdd" class="card-list">
          <section v-for="c in cards" :key="c.id" class="bank-group">
            <header class="bank-group__head" @click="toggleGroup(c.id)">
              <span class="bank-chip" :class="`bank-chip--${c.brand}`" aria-hidden="true">
                <span v-if="c.brand === 'visa'" class="bank-chip__visa">VISA</span>
                <span v-else-if="c.brand === 'mastercard'" class="bank-chip__mc"><i /><i /></span>
                <span v-else-if="c.brand === 'unionpay'" class="bank-chip__up"><i /><i /><i /></span>
                <span v-else class="bank-chip__seal">{{ chipMark(c.brand) }}</span>
              </span>
              <span class="bank-group__name">{{ brandLabel(c.brand) }}({{ c.last4 }})</span>
              <span v-if="c.isPrimary" class="tag tag--primary">主卡</span>
              <span v-if="c.status !== 'active'" class="tag">{{ statusLabel(c.status) }}</span>
              <span class="bank-group__spacer" />
              <span class="bank-group__date">{{ dateLabel(c.createdAt) }}</span>
              <svg
                class="bank-group__chevron"
                :class="{ 'bank-group__chevron--open': !isCollapsed(c.id) }"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M9 6l6 6-6 6" />
              </svg>
              <button
                v-if="c.status !== 'removed'"
                class="bank-group__unbind"
                type="button"
                @click.stop="confirmDelete(c)"
              >
                解绑
              </button>
            </header>

            <div v-if="!isCollapsed(c.id)" class="bank-body">
              <div class="bank-visual" :class="`bank-visual--${c.brand}`">
                <span class="bank-visual__glow" />
                <svg
                  v-if="c.brand === 'boc'"
                  class="bank-visual__wall"
                  viewBox="0 0 335 120"
                  preserveAspectRatio="none"
                  aria-hidden="true"
                >
                  <path d="M0 82 L34 62 L60 72 L94 48 L126 64 L160 42 L196 60 L230 44 L264 58 L300 42 L335 52" />
                  <path d="M0 112 L0 98 L10 98 L10 91 L18 91 L18 98 L30 98 L30 90 L38 90 L38 98 L50 98 L50 92 L58 92 L58 100 L74 96 L74 89 L84 89 L84 98 L96 98 L96 91 L106 91 L106 98 L118 98 L118 90 L128 90 L128 98 L142 94 L142 87 L152 87 L152 94 L164 94 L164 86 L174 86 L174 94 L188 94 L188 88 L198 88 L198 94 L212 90 L212 82 L222 82 L222 90 L236 90 L236 83 L246 83 L246 90 L260 86 L260 78 L270 78 L270 86 L284 86 L284 79 L294 79 L294 86 L308 82 L308 74 L318 74 L318 82 L335 78 L335 112 Z" />
                  <path d="M84 98 V62 H96 V98 M87 62 V55 H93 V62" />
                  <path d="M222 90 V54 H236 V90 M225 54 V47 H233 V54" />
                </svg>
                <svg
                  v-else-if="c.brand === 'unionpay'"
                  class="bank-visual__mountains"
                  viewBox="0 0 335 110"
                  preserveAspectRatio="none"
                  aria-hidden="true"
                >
                  <path d="M0 96 L28 60 L48 76 L78 42 L108 70 L140 50 L170 78 L205 52 L235 74 L270 46 L300 70 L335 58" />
                  <path d="M0 104 L28 68 L48 84 L78 50 L108 78 L140 58 L170 86 L205 60 L235 82 L270 54 L300 78 L335 66" />
                </svg>
                <div class="bank-visual__row">
                  <span class="bank-visual__brand">{{ brandLabel(c.brand) }}</span>
                  <span class="bank-visual__type">{{ cardTypeLabel(c.cardType) }} {{ c.last4 }}</span>
                </div>
                <div class="bank-visual__row bank-visual__row--bottom">
                  <span class="bank-visual__holder">{{ c.holderName }}</span>
                  <span class="bank-visual__expiry">{{ expLabel(c) }}</span>
                </div>
              </div>

              <div class="bank-actions">
                <button type="button" @click="showBalance(c)">查余额</button>
                <button type="button" @click="showNumber(c)">查卡号</button>
                <button type="button" @click="goBills">查账单</button>
              </div>
            </div>
          </section>

          <!-- 展开 / 收起全部 -->
          <button class="expand-all" type="button" @click="toggleAll">
            <span>{{ allCollapsed ? `展开全部(${cards.length})` : '收起全部' }}</span>
            <svg
              class="expand-all__chevron"
              :class="{ 'expand-all__chevron--up': allCollapsed }"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M6 9l6 6 6-6" />
            </svg>
          </button>
        </div>

        <!-- 新增表单 -->
        <section v-if="showAdd" class="card--form">
          <h3 class="card__title">绑定新卡</h3>

          <div class="form-grid">
            <label class="field">
              <span class="field__label">卡类型</span>
              <select v-model="addForm.cardType" :disabled="submitting">
                <option value="debit">借记卡</option>
                <option value="credit">信用卡</option>
              </select>
            </label>

            <label class="field">
              <span class="field__label">品牌</span>
              <select v-model="addForm.brand" :disabled="submitting">
                <option value="visa">威士</option>
                <option value="mastercard">万事达</option>
                <option value="unionpay">银联</option>
                <option value="icbc">中国工商银行</option>
                <option value="abc">中国农业银行</option>
                <option value="boc">中国银行</option>
                <option value="ccb">中国建设银行</option>
                <option value="bocom">交通银行</option>
                <option value="psbc">中国邮政储蓄银行</option>
              </select>
            </label>

            <label class="field">
              <span class="field__label">卡号后 4 位</span>
              <input
                v-model="addForm.last4"
                type="text"
                maxlength="4"
                inputmode="numeric"
                placeholder="0000"
                :disabled="submitting"
              />
            </label>

            <label class="field">
              <span class="field__label">持卡人</span>
              <input
                v-model="addForm.holderName"
                type="text"
                placeholder="请输入持卡人姓名"
                :disabled="submitting"
              />
            </label>

            <label class="field">
              <span class="field__label">到期月</span>
              <select v-model.number="addForm.expiryMonth" :disabled="submitting">
                <option v-for="m in 12" :key="m" :value="m">
                  {{ String(m).padStart(2, '0') }}
                </option>
              </select>
            </label>

            <label class="field">
              <span class="field__label">到期年</span>
              <select v-model.number="addForm.expiryYear" :disabled="submitting">
                <option v-for="y in expiryYears" :key="y" :value="y">
                  {{ y }}
                </option>
              </select>
            </label>

            <label class="field">
              <span class="field__label">币种</span>
              <select v-model="addForm.currencyCode" :disabled="submitting">
                <option value="CNY">人民币</option>
                <option value="USD">美元</option>
                <option value="EUR">欧元</option>
                <option value="JPY">日元</option>
                <option value="GBP">英镑</option>
                <option value="HKD">港币</option>
              </select>
            </label>

            <label class="field field--check">
              <input type="checkbox" v-model="addForm.isPrimary" :disabled="submitting" />
              <span class="field__label">设为主卡</span>
            </label>
          </div>

          <div class="form-actions">
            <button
              type="button"
              class="form-actions__cancel"
              :disabled="submitting"
              @click="showAdd = false; resetAddForm()"
            >
              取消
            </button>
            <button
              type="button"
              class="form-actions__save"
              :disabled="submitting"
              @click="submitAdd"
            >
              {{ submitting ? '提交中…' : '确认绑定' }}
            </button>
          </div>
        </section>
      </div>
    </PageLayout>
  </div>
</template>

<style scoped>
/* ========== 深色底 ========== */
.cards-root {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-width: 390px;
  margin: 0 auto;
}

.cards-root :deep(.page-layout) {
  background: #f7f6f8;
}

.cards-root :deep(.page-layout__header) {
  background: #f7f6f8;
}

.cards-root :deep(.page-layout__main) {
  padding: 0;
  background: #f7f6f8;
}

.cards-root :deep(.page-layout__home span) {
  background: rgba(0, 0, 0, 0.12);
}

.cards-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 20px 28px;
}

/* ========== 添加卡按钮 ========== */
.add-card-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 32px;
  padding: 0 13px;
  border: 1px solid #f0eeef;
  border-radius: 100px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.04);
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 13px;
  color: #353333;
  cursor: pointer;
}

.add-card-btn__plus {
  font-size: 14px;
}

/* ========== 汇总 ========== */
.summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 6px 4px 2px;
}

.summary__count {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 0;
}

.summary__count strong {
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 40px;
  font-weight: 600;
  line-height: 1;
  color: #353333;
}

.summary__count span {
  font-size: 14px;
  color: #696565;
}

.summary__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 0;
  font-size: 12px;
  color: #afafaf;
}

.summary__link {
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 12px;
  color: #17b7bd;
  cursor: pointer;
}

.summary__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  padding-top: 8px;
}

.summary__label {
  font-size: 12px;
  color: #afafaf;
}

.summary__amount {
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: #353333;
}

/* ========== 备注提示 ========== */
.note-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 16px;
  background: #e9f9f5;
}

.note-banner__text {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 18px;
  color: #475467;
}

.note-banner__btn {
  flex-shrink: 0;
  padding: 5px 12px;
  border: 0;
  border-radius: 100px;
  background: #17b7bd;
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  color: #ffffff;
  cursor: pointer;
}

/* ========== 卡组 ========== */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bank-group {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 16px;
  border: 1px solid var(--app-line, #f0eeef);
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.bank-group__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
}

/* 卡面缩略图：与钱包首页的卡面主题保持一致 */
.bank-chip {
  position: relative;
  flex-shrink: 0;
  width: 52px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 7px;
  border: 1px solid rgba(26, 41, 41, 0.08);
  box-shadow: 0 2px 6px rgba(26, 41, 41, 0.08);
}

.bank-chip--visa {
  background: linear-gradient(135deg, #f7f8f6 0%, #e3e7e4 45%, #fafbfa 70%, #dfe4e1 100%);
}

.bank-chip--mastercard {
  background: linear-gradient(135deg, #0b0b0d 0%, #1c1c21 50%, #0a0a0c 100%);
  border-color: rgba(232, 194, 122, 0.28);
}

.bank-chip--unionpay {
  background: linear-gradient(135deg, #f5f7f2 0%, #e4ece5 45%, #f8faf6 70%, #d5e2da 100%);
}

.bank-chip--icbc {
  background: url('/assets/icbc-palace-autumn-front.jpg') center / cover no-repeat;
  border-color: rgba(232, 194, 122, 0.28);
}

.bank-chip--abc {
  background: linear-gradient(135deg, #062b1d 0%, #0f6b49 50%, #042116 100%);
}

.bank-chip--boc {
  background: linear-gradient(135deg, #f7ecd8 0%, #e6d0a8 45%, #fbf5e9 70%, #d9c193 100%);
  border-color: rgba(138, 106, 47, 0.32);
}

.bank-chip--ccb {
  background: linear-gradient(135deg, #06251c 0%, #0f4a37 55%, #041a13 100%);
}

.bank-chip--bocom {
  background: linear-gradient(135deg, #f6efe4 0%, #eadbc6 45%, #fbf7ef 70%, #e2d2ba 100%);
}

.bank-chip--psbc {
  background: linear-gradient(135deg, #f7f9f8 0%, #e4ecef 45%, #fafcfd 70%, #dbe6eb 100%);
}

.bank-chip__visa {
  font-family: 'Poppins', sans-serif;
  font-size: 11px;
  font-style: italic;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: #1434cb;
}

.bank-chip__mc {
  display: flex;
  align-items: center;
}

.bank-chip__mc i {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: #eb001b;
}

.bank-chip__mc i + i {
  margin-left: -6px;
  background: #f79e1b;
  opacity: 0.92;
}

.bank-chip__up {
  display: flex;
  align-items: center;
  gap: 2px;
}

.bank-chip__up i {
  width: 3px;
  height: 13px;
  border-radius: 1px;
  transform: skewX(-12deg);
}

.bank-chip__up i:nth-child(1) {
  background: #e21836;
}

.bank-chip__up i:nth-child(2) {
  background: #00447c;
}

.bank-chip__up i:nth-child(3) {
  background: #007b84;
}

.bank-chip__seal {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 19px;
  height: 19px;
  border-radius: 50%;
  font-family: 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.bank-chip--icbc .bank-chip__seal {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(232, 194, 122, 0.7);
  color: #f3e2b8;
}

.bank-chip--abc .bank-chip__seal {
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(232, 194, 122, 0.6);
  color: #e8c27a;
}

.bank-chip--boc .bank-chip__seal {
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(138, 106, 47, 0.4);
  color: #8a6a2f;
}

.bank-chip--ccb .bank-chip__seal {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(232, 194, 122, 0.55);
  color: #e8c27a;
}

.bank-chip--bocom .bank-chip__seal {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(14, 116, 144, 0.3);
  color: #0e7490;
}

.bank-chip--psbc .bank-chip__seal {
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(14, 90, 138, 0.28);
  color: #0e5a8a;
}

.bank-group__name {
  flex: 0 1 auto;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: #353333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 4px;
  background: #f4f6f4;
  font-size: 10px;
  color: #7a7a7a;
}

.tag--primary {
  background: var(--app-primary, #17b7bd);
  color: #ffffff;
}

.bank-group__spacer {
  flex: 1;
}

.bank-group__date {
  flex-shrink: 0;
  font-size: 12px;
  color: #afafaf;
}

.bank-group__unbind {
  flex-shrink: 0;
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 12px;
  color: #afafaf;
  cursor: pointer;
}

.bank-group__chevron {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  color: #afafaf;
  transition: transform 0.2s ease;
}

.bank-group__chevron--open {
  transform: rotate(90deg);
}

.bank-body {
  padding-bottom: 12px;
}

/* ========== 卡面 ========== */
.bank-visual {
  --visual-ink: #ffffff;
  --visual-muted: rgba(255, 255, 255, 0.88);
  --visual-shadow: 0 10px 24px rgba(26, 41, 41, 0.18);
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 150px;
  margin: 0 12px;
  padding: 16px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--visual-shadow);
}

/* 与钱包首页一致的卡面主题 */
.bank-visual--visa {
  --visual-ink: #1434cb;
  --visual-muted: #475467;
  --visual-shadow: 0 8px 18px rgba(26, 41, 41, 0.08);
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f7f8f6 0%, #e3e7e4 45%, #fafbfa 70%, #dfe4e1 100%);
}

.bank-visual--mastercard {
  --visual-ink: #f3e2b8;
  --visual-muted: rgba(243, 226, 184, 0.75);
  background: linear-gradient(135deg, #0b0b0d 0%, #1c1c21 50%, #0a0a0c 100%);
}

.bank-visual--unionpay {
  --visual-ink: #14532d;
  --visual-muted: #4f6a60;
  --visual-shadow: 0 8px 18px rgba(26, 41, 41, 0.08);
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f5f7f2 0%, #e4ece5 45%, #f8faf6 70%, #d5e2da 100%);
}

.bank-visual--icbc {
  background:
    linear-gradient(rgba(3, 8, 18, 0.45), rgba(3, 8, 18, 0.45)),
    url('/assets/icbc-palace-autumn-front.jpg') center / cover no-repeat;
}

.bank-visual--abc {
  --visual-ink: #f3e2b8;
  --visual-muted: rgba(243, 226, 184, 0.8);
  background:
    radial-gradient(90% 120% at 88% -10%, rgba(23, 160, 106, 0.4) 0%, rgba(23, 160, 106, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #062b1d 0%, #0f6b49 50%, #042116 100%);
}

.bank-visual--boc {
  --visual-ink: #3b2a16;
  --visual-muted: #8a6a2f;
  --visual-shadow: 0 8px 18px rgba(90, 60, 20, 0.12);
  background:
    radial-gradient(80% 120% at 88% -10%, rgba(232, 194, 122, 0.28) 0%, rgba(232, 194, 122, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f7ecd8 0%, #e6d0a8 45%, #fbf5e9 70%, #d9c193 100%);
}

.bank-visual--ccb {
  --visual-ink: #f3e2b8;
  --visual-muted: rgba(243, 226, 184, 0.8);
  background:
    repeating-linear-gradient(90deg, rgba(232, 194, 122, 0.06) 0 1px, rgba(232, 194, 122, 0) 1px 18px),
    radial-gradient(90% 120% at 88% -10%, rgba(232, 194, 122, 0.16) 0%, rgba(232, 194, 122, 0) 55%),
    linear-gradient(135deg, #06251c 0%, #0f4a37 55%, #041a13 100%);
}

.bank-visual--bocom {
  --visual-ink: #1f2937;
  --visual-muted: #696565;
  --visual-shadow: 0 8px 18px rgba(26, 41, 41, 0.08);
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f6efe4 0%, #eadbc6 45%, #fbf7ef 70%, #e2d2ba 100%);
}

.bank-visual--psbc {
  --visual-ink: #1f2937;
  --visual-muted: #696565;
  --visual-shadow: 0 8px 18px rgba(26, 41, 41, 0.08);
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f7f9f8 0%, #e4ecef 45%, #fafcfd 70%, #dbe6eb 100%);
}

.bank-visual__glow {
  position: absolute;
  top: -60px;
  right: -40px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.28) 0%, rgba(255, 255, 255, 0) 68%);
}

.bank-visual__wall {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  width: 100%;
  height: 88px;
  color: #8a6a2f;
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
  stroke-opacity: 0.3;
  pointer-events: none;
}

.bank-visual__mountains {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  color: #0f766e;
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
  stroke-opacity: 0.16;
  pointer-events: none;
}

.bank-visual__wall path,
.bank-visual__mountains path {
  fill: none;
  stroke: currentColor;
}

.bank-visual__row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bank-visual__row--bottom {
  align-items: flex-end;
}

.bank-visual__brand {
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--visual-ink);
}

.bank-visual__type {
  font-size: 12px;
  color: var(--visual-muted);
}

.bank-visual__holder {
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--visual-muted);
}

.bank-visual__expiry {
  font-family: 'Poppins', monospace;
  font-size: 12px;
  color: var(--visual-muted);
}

/* ========== 卡面操作 ========== */
.bank-actions {
  display: flex;
  margin: 10px 12px 12px;
  border-radius: 10px;
  overflow: hidden;
}

.bank-actions button {
  flex: 1;
  height: 40px;
  border: 0;
  border-left: 1px solid rgba(0, 0, 0, 0.05);
  background: #f7f8f9;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 13px;
  color: #475467;
  cursor: pointer;
}

.bank-actions button:first-child {
  border-left: 0;
}

.bank-actions button:active {
  background: #eceff1;
}

/* ========== 展开 / 收起全部 ========== */
.expand-all {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 40px;
  border: 0;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.05);
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 13px;
  color: #0f9ea3;
  cursor: pointer;
}

.expand-all__chevron {
  width: 14px;
  height: 14px;
  transition: transform 0.2s ease;
}

.expand-all__chevron--up {
  transform: rotate(180deg);
}

/* ========== 骨架 / 空状态 ========== */
.skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton__card {
  height: 190px;
  border-radius: 14px;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.04) 25%, rgba(0, 0, 0, 0.08) 37%, rgba(0, 0, 0, 0.04) 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 24px;
  text-align: center;
}

.empty__icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.empty__title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #353333;
}

.empty__hint {
  margin: 0;
  font-size: 13px;
  color: #afafaf;
}

/* ========== 新增表单 ========== */
.card--form {
  padding: 18px 16px;
  border-radius: 14px;
  background: #ffffff;
}

.card__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 700;
  color: #353333;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field--check {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.field--check input {
  width: 16px;
  height: 16px;
}

.field__label {
  font-size: 12px;
  color: #696565;
}

.field input,
.field select {
  width: 100%;
  height: 40px;
  padding: 0 10px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  outline: none;
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  color: #353333;
  box-sizing: border-box;
}

.field input:focus,
.field select:focus {
  border-color: #17b7bd;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.form-actions button {
  flex: 1;
  height: 46px;
  border-radius: 10px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.form-actions__cancel {
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  color: #696565;
}

.form-actions__save {
  border: 0;
  background: #17b7bd;
  color: #fff;
}

.form-actions__cancel:disabled,
.form-actions__save:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
