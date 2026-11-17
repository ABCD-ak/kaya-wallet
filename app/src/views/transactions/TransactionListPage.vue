<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import BottomNav from '@/components/BottomNav.vue'
import TxCategoryIcon from '@/components/TxCategoryIcon.vue'
import { listTransactions } from '@/api/transactions'
import { listWallets } from '@/api/wallets'
import type {
  PageResult,
  TransactionResponse,
  WalletResponse,
} from '@/api/types'

const router = useRouter()

// ---------- 状态 ----------
const wallets = ref<WalletResponse[]>([])
const walletId = ref<string>('')
const directionFilter = ref<string | null>(null)
const typeFilter = ref<string | null>(null)

const items = ref<TransactionResponse[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
const hasMore = computed(() => items.value.length < total.value)

const loading = ref(false)
const loadingMore = ref(false)
const initialLoaded = ref(false)

// ---------- 可视化统计 ----------
const monthTxs = ref<TransactionResponse[]>([])

const monthKey = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})()
const monthLabel = (() => {
  const [y, m] = monthKey.split('-')
  return `${y}年${Number(m)}月`
})()

const DONUT_R = 54
const DONUT_CIRC = 2 * Math.PI * DONUT_R
const VIZ_PALETTE = ['#17b7bd', '#17b7bd', '#f27473', '#e73e79', '#f2b84b', '#7c8cf8', '#afafaf']

function moneySymbol(code?: string): string {
  const map: Record<string, string> = { CNY: '¥', USD: '$', EUR: '€', JPY: '¥', GBP: '£', HKD: '港币' }
  return map[code ?? ''] ?? '¥'
}
function fmtMoney(raw?: string, code?: string): string {
  const n = Number(raw ?? 0)
  return `${moneySymbol(code)}${n.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

const vizCurrency = computed(() => wallets.value[0]?.currencyCode)

// 当前选中的筛选模块决定下方可视化内容
type VizMode = 'all' | 'credit' | 'debit' | 'transfer_in' | 'transfer_out'
const vizMode = computed<VizMode>(() => {
  const key = activeFilterKey.value
  if (key === 'credit' || key === 'debit' || key === 'transfer_in' || key === 'transfer_out') {
    return key
  }
  return 'all'
})
const vizModeLabel = computed(() => {
  const labels: Record<VizMode, string> = {
    all: '收支',
    credit: '收入',
    debit: '支出',
    transfer_in: '转入',
    transfer_out: '转出',
  }
  return labels[vizMode.value]
})
const vizCenterLabel = computed(() => {
  const labels: Record<VizMode, string> = {
    all: '收支构成',
    credit: '收入构成',
    debit: '支出构成',
    transfer_in: '转入构成',
    transfer_out: '转出构成',
  }
  return labels[vizMode.value]
})
const vizTone = computed(() => {
  if (vizMode.value === 'credit' || vizMode.value === 'transfer_in') return 'is-income'
  if (vizMode.value === 'all') return ''
  return 'is-spent'
})

// 交易实际流动金额：支出方向含手续费
function txFlow(t: TransactionResponse): number {
  return Number(t.amount) + (t.direction === 'debit' ? Number(t.feeAmount ?? 0) : 0)
}

// 按当前模块取本月对应交易；「全部」展示收入 + 支出的完整构成
const vizTxs = computed(() => {
  switch (vizMode.value) {
    case 'credit':
      return monthTxs.value.filter((t) => t.direction === 'credit')
    case 'transfer_in':
      return monthTxs.value.filter((t) => t.type === 'transfer_in')
    case 'transfer_out':
      return monthTxs.value.filter((t) => t.type === 'transfer_out')
    case 'debit':
      return monthTxs.value.filter((t) => t.direction === 'debit')
    case 'all':
    default:
      return monthTxs.value
  }
})

const vizGroups = computed(() => {
  const map = new Map<string, { id: string; name: string; color: string; amount: number }>()
  for (const t of vizTxs.value) {
    const isTransfer = t.type === 'transfer_in' || t.type === 'transfer_out'
    const key = isTransfer
      ? `transfer-${t.type}-${t.counterparty?.name ?? ''}`
      : (t.category?.id ?? t.type)
    const name = isTransfer
      ? (t.counterparty?.name || (t.type === 'transfer_in' ? '转入' : '转出'))
      : (t.category?.name || (t.direction === 'credit' ? '收入' : '支出'))
    const existing = map.get(key)
    if (existing) {
      existing.amount += txFlow(t)
      continue
    }
    map.set(key, {
      id: key,
      name,
      color: t.category?.color || VIZ_PALETTE[map.size % VIZ_PALETTE.length],
      amount: txFlow(t),
    })
  }
  const total = [...map.values()].reduce((sum, g) => sum + g.amount, 0)
  return [...map.values()]
    .sort((a, b) => b.amount - a.amount)
    .map((g) => ({ ...g, pct: total ? (g.amount / total) * 100 : 0 }))
})

const vizTotal = computed(() => vizGroups.value.reduce((sum, g) => sum + g.amount, 0))
const vizAvg = computed(() => (vizTxs.value.length ? vizTotal.value / vizTxs.value.length : 0))

// 环形图分段：按各分组金额占比计算弧长，首尾相接不留缺口
const donutSegments = computed(() => {
  const total = vizTotal.value
  if (!total) return []
  let acc = 0
  return vizGroups.value.map((g) => {
    const ratio = g.amount / total
    const start = acc
    acc += ratio
    const len = ratio * DONUT_CIRC
    return {
      id: g.id,
      color: g.color,
      dash: `${len} ${DONUT_CIRC - len}`,
      offset: -start * DONUT_CIRC,
    }
  })
})

const legendItems = computed(() => vizGroups.value)

// ---------- 图表交互 ----------
const activeGroupKey = ref<string | null>(null)
const hoverGroupKey = ref<string | null>(null)
// 悬浮优先，点击可在移动端保持选中
const focusGroupKey = computed(() => hoverGroupKey.value ?? activeGroupKey.value)
const activeGroup = computed(() =>
  vizGroups.value.find((g) => g.id === focusGroupKey.value) ?? null,
)
function toggleGroup(id: string) {
  activeGroupKey.value = activeGroupKey.value === id ? null : id
}
// 鼠标移出后完全复位（触屏点击仍可保持选中）
function clearFocus() {
  hoverGroupKey.value = null
  activeGroupKey.value = null
}
const centerLabel = computed(() => activeGroup.value?.name ?? vizCenterLabel.value)
const centerAmount = computed(() =>
  fmtMoney((activeGroup.value?.amount ?? vizTotal.value).toFixed(2), vizCurrency.value),
)
const centerPct = computed(() => (activeGroup.value ? `${activeGroup.value.pct.toFixed(0)}%` : ''))

const incomeTotal = computed(() =>
  monthTxs.value.filter((t) => t.direction === 'credit').reduce((sum, t) => sum + txFlow(t), 0),
)
const spentTotal = computed(() =>
  monthTxs.value.filter((t) => t.direction === 'debit').reduce((sum, t) => sum + txFlow(t), 0),
)
// 支出占比：用于收支比例条
const spentRatio = computed(() => {
  const sum = incomeTotal.value + spentTotal.value
  return sum === 0 ? 0 : (spentTotal.value / sum) * 100
})

async function loadMonthTxs() {
  if (!walletId.value) return
  try {
    const [y, m] = monthKey.split('-').map(Number)
    const from = new Date(y, m - 1, 1).toISOString()
    const to = new Date(y, m, 1).toISOString()
    const collected: TransactionResponse[] = []
    let pageNo = 1
    let totalCount = Number.POSITIVE_INFINITY
    // 按月拉取，最多 10 页，保证统计基于完整月度数据
    while (collected.length < totalCount && pageNo <= 10) {
      const res = await listTransactions({
        walletId: walletId.value,
        from,
        to,
        page: pageNo,
        pageSize: 100,
      })
      collected.push(...(res.items ?? []))
      totalCount = res.total ?? collected.length
      if (!res.items?.length) break
      pageNo += 1
    }
    monthTxs.value = collected
  } catch {
    // 请求层已展示错误
  }
}

// ---------- 过滤选项 ----------
// label → { direction, type }
interface FilterChip {
  label: string
  key: string
  direction?: 'credit' | 'debit'
  type?: TransactionResponse['type']
}

const filterChips: FilterChip[] = [
  { label: '全部', key: 'all' },
  { label: '收入', key: 'credit', direction: 'credit' },
  { label: '支出', key: 'debit', direction: 'debit' },
  { label: '转入', key: 'transfer_in', type: 'transfer_in' },
  { label: '转出', key: 'transfer_out', type: 'transfer_out' },
]

const activeFilterKey = computed(() => {
  if (directionFilter.value) return directionFilter.value
  if (typeFilter.value) return typeFilter.value
  return 'all'
})

function applyFilter(chip: FilterChip) {
  directionFilter.value = chip.direction ?? null
  typeFilter.value = chip.type ?? null
}

// ---------- 格式化工具 ----------
function currencyLabel(code: string): string {
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

function formatDateTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const m = d.getMonth() + 1
  const day = d.getDate()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${m}月${day}日 ${hh}:${mm}`
}

function amountColor(tx: TransactionResponse): 'income' | 'expense' {
  return tx.direction === 'credit' ? 'income' : 'expense'
}

function formatAmount(tx: TransactionResponse): string {
  const sign = tx.direction === 'credit' ? '+' : '-'
  // 支出金额含手续费，与真实扣款保持一致
  const fee = Number(tx.feeAmount ?? 0)
  const total = tx.direction === 'debit' && fee > 0
    ? (Number(tx.amount) + fee).toFixed(2)
    : tx.amount
  return `${sign}${total}`
}

function displayTitle(tx: TransactionResponse): string {
  if (tx.type === 'transfer_in' || tx.type === 'transfer_out') {
    return tx.counterparty?.name || (tx.type === 'transfer_in' ? '转入' : '转出')
  }
  return tx.category?.name || (tx.type === 'income' ? '收入' : '支出')
}

function displaySub(tx: TransactionResponse): string {
  const base = tx.note?.trim() || tx.referenceNo || ''
  const fee = Number(tx.feeAmount ?? 0)
  if (fee > 0) {
    return `${base ? base + ' · ' : ''}手续费 ${fee.toFixed(2)}`
  }
  return base
}

// ---------- 月份分组 ----------
interface MonthGroup {
  label: string
  items: TransactionResponse[]
}

const grouped = computed<MonthGroup[]>(() => {
  const groups: Record<string, TransactionResponse[]> = {}
  for (const tx of items.value) {
    const d = new Date(tx.occurredAt)
    if (Number.isNaN(d.getTime())) continue
    const key = `${d.getFullYear()}-${d.getMonth() + 1}`
    if (!groups[key]) groups[key] = []
    groups[key].push(tx)
  }
  // 按月份 key 降序
  const keys = Object.keys(groups).sort((a, b) => (a < b ? 1 : -1))
  return keys.map((k) => {
    const [y, m] = k.split('-')
    return {
      label: `${y}年${Number(m)}月`,
      items: groups[k],
    }
  })
})

// ---------- 数据加载 ----------
async function loadWallets(): Promise<string | undefined> {
  try {
    const list = await listWallets()
    wallets.value = list
    if (list.length > 0 && !walletId.value) {
      walletId.value = list[0].id
    }
    return walletId.value || undefined
  } catch {
    return undefined
  }
}

async function fetchList(reset = false) {
  if (reset) {
    page.value = 1
    items.value = []
    total.value = 0
  }
  if (loading.value || loadingMore.value) return

  const currentPage = page.value
  const isFirst = currentPage === 1

  if (isFirst) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const wid = walletId.value || (await loadWallets())
    if (!wid) return

    const params: Record<string, unknown> = {
      walletId: wid,
      page: currentPage,
      pageSize,
    }
    if (directionFilter.value) params.direction = directionFilter.value
    if (typeFilter.value) params.type = typeFilter.value

    const res = (await listTransactions(params as never)) as unknown as PageResult<TransactionResponse>
    items.value = reset
      ? res.items
      : [...items.value, ...res.items]
    total.value = res.total
    page.value = currentPage + 1
  } catch {
    // 请求层已展示错误
  } finally {
    loading.value = false
    loadingMore.value = false
    initialLoaded.value = true
  }
}

function reloadFirstPage() {
  fetchList(true)
}

function loadMore() {
  if (!hasMore.value || loadingMore.value || loading.value) return
  fetchList(false)
}

// ---------- 监听过滤变化 ----------
// 切换筛选模块时清除图表选中状态
watch(vizMode, () => {
  activeGroupKey.value = null
})

watch([directionFilter, typeFilter, walletId], () => {
  reloadFirstPage()
  loadMonthTxs()
})

// ---------- 无限滚动：IntersectionObserver ----------
const sentinel = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

function setupObserver() {
  if (!sentinel.value) return
  observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        loadMore()
      }
    }
  }, { rootMargin: '120px' })
  observer.observe(sentinel.value)
}

onMounted(async () => {
  await loadWallets()
  reloadFirstPage()
  loadMonthTxs()
  // 等 DOM 渲染后绑定 observer
  setTimeout(setupObserver, 50)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})

// ---------- 交互 ----------
function goBack() {
  router.back()
}

function openDetail(id: string) {
  router.push(`/transactions/${id}`)
}
</script>

<template>
  <div class="tx-page">
    <PageLayout title="交易记录" @back="goBack">
    <!-- 过滤模块：置于最上方，点击后下方内容随之切换 -->
    <div class="tx-filter" role="tablist" aria-label="交易类型筛选">
      <button
        v-for="chip in filterChips"
        :key="chip.key"
        type="button"
        role="tab"
        class="tx-filter__chip"
        :class="{ 'tx-filter__chip--active': activeFilterKey === chip.key }"
        @click="applyFilter(chip)"
      >
        {{ chip.label }}
      </button>
    </div>

    <!-- 可视化统计 -->
    <section v-if="initialLoaded" class="tx-viz">
      <header class="tx-viz__head">
        <h2 class="tx-viz__title">{{ monthLabel }}{{ vizModeLabel }}概览</h2>
        <span class="tx-viz__hint">悬浮或点击分类查看</span>
      </header>

      <div class="tx-viz__body">
        <div class="tx-viz__donut">
          <svg viewBox="0 0 120 120" aria-hidden="true">
            <circle class="tx-viz__ring-bg" cx="60" cy="60" :r="DONUT_R" />
            <circle
              v-for="seg in donutSegments"
              :key="seg.id"
              class="tx-viz__ring-seg"
              :class="{
                'is-active': focusGroupKey === seg.id,
                'is-dim': focusGroupKey && focusGroupKey !== seg.id,
              }"
              cx="60"
              cy="60"
              :r="DONUT_R"
              :stroke="seg.color"
              :stroke-dasharray="seg.dash"
              :stroke-dashoffset="seg.offset"
              @mouseenter="hoverGroupKey = seg.id"
              @mouseleave="clearFocus"
              @click="toggleGroup(seg.id)"
            />
          </svg>
          <div class="tx-viz__center">
            <span class="tx-viz__center-label">{{ centerLabel }}</span>
            <strong class="tx-viz__center-amount">{{ centerAmount }}</strong>
            <span v-if="centerPct" class="tx-viz__center-sub">{{ centerPct }}</span>
          </div>
        </div>

        <ul v-if="legendItems.length" class="tx-viz__legend">
          <li v-for="item in legendItems" :key="item.id">
            <button
              type="button"
              class="tx-viz__legend-row"
              :class="{ 'is-active': focusGroupKey === item.id }"
              @mouseenter="hoverGroupKey = item.id"
              @mouseleave="clearFocus"
              @click="toggleGroup(item.id)"
            >
              <i class="tx-viz__dot" :style="{ background: item.color }" />
              <span class="tx-viz__cat">{{ item.name }}</span>
              <span class="tx-viz__pct">
                {{ focusGroupKey === item.id ? fmtMoney(item.amount.toFixed(2), vizCurrency) : `${item.pct.toFixed(0)}%` }}
              </span>
            </button>
          </li>
        </ul>
        <p v-else class="tx-viz__empty">本月暂无{{ vizModeLabel }}数据</p>
      </div>

      <div v-if="vizMode === 'all'" class="tx-viz__ratio" aria-hidden="true">
        <span class="tx-viz__ratio-income" :style="{ width: `${100 - spentRatio}%` }" />
        <span class="tx-viz__ratio-spent" :style="{ width: `${spentRatio}%` }" />
      </div>

      <div class="tx-viz__totals">
        <template v-if="vizMode === 'all'">
          <div class="tx-viz__total">
            <span class="tx-viz__total-label">收入</span>
            <strong class="tx-viz__total-value is-income">+{{ fmtMoney(incomeTotal.toFixed(2), vizCurrency) }}</strong>
          </div>
          <div class="tx-viz__total">
            <span class="tx-viz__total-label">支出</span>
            <strong class="tx-viz__total-value is-spent">-{{ fmtMoney(spentTotal.toFixed(2), vizCurrency) }}</strong>
          </div>
        </template>
        <template v-else>
          <div class="tx-viz__total">
            <span class="tx-viz__total-label">{{ vizModeLabel }}合计</span>
            <strong class="tx-viz__total-value" :class="vizTone">{{ fmtMoney(vizTotal.toFixed(2), vizCurrency) }}</strong>
          </div>
          <div class="tx-viz__total">
            <span class="tx-viz__total-label">笔数</span>
            <strong class="tx-viz__total-value">{{ vizTxs.length }}</strong>
          </div>
          <div class="tx-viz__total">
            <span class="tx-viz__total-label">平均每笔</span>
            <strong class="tx-viz__total-value">{{ fmtMoney(vizAvg.toFixed(2), vizCurrency) }}</strong>
          </div>
        </template>
      </div>
    </section>

    <!-- 钱包选择器（多个时显示） -->
    <div v-if="wallets.length > 1" class="tx-wallet">
      <select v-model="walletId" class="tx-wallet__select">
        <option v-for="w in wallets" :key="w.id" :value="w.id">
          钱包 · {{ currencyLabel(w.currencyCode) }}
        </option>
      </select>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading && items.length === 0" class="tx-list tx-list--skeleton">
      <div v-for="i in 4" :key="i" class="tx-card tx-card--skeleton">
        <span class="tx-card__icon tx-card__icon--skeleton" />
        <div class="tx-card__body">
          <span class="tx-skel tx-skel--title" />
          <span class="tx-skel tx-skel--sub" />
        </div>
        <div class="tx-skel tx-skel--amount" />
      </div>
    </div>

    <!-- 空状态 -->
    <div
      v-else-if="initialLoaded && items.length === 0"
      class="tx-empty"
    >
      <div class="tx-empty__illu" aria-hidden="true">
        <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="60" cy="60" r="56" fill="#eef5f5" />
          <rect x="28" y="40" width="64" height="56" rx="10" fill="#fff" stroke="#d8d4d4" stroke-width="1.5" />
          <rect x="38" y="52" width="28" height="4" rx="2" fill="#e7e3e3" />
          <rect x="38" y="62" width="44" height="4" rx="2" fill="#efebeb" />
          <rect x="38" y="72" width="20" height="4" rx="2" fill="#efebeb" />
          <path d="M80 82 L92 94" stroke="#c7bfbf" stroke-width="3" stroke-linecap="round" />
          <circle cx="92" cy="94" r="3" fill="#c7bfbf" />
        </svg>
      </div>
      <p class="tx-empty__text">暂无交易记录</p>
    </div>

    <!-- 交易列表 -->
    <div v-else class="tx-list">
      <section
        v-for="group in grouped"
        :key="group.label"
        class="tx-group"
      >
        <h2 class="tx-group__title">{{ group.label }}</h2>
        <ul class="tx-group__list">
          <li
            v-for="tx in group.items"
            :key="tx.id"
            class="tx-card"
            :class="`tx-card--${amountColor(tx)}`"
            @click="openDetail(tx.id)"
          >
            <TxCategoryIcon :tx="tx" />
            <div class="tx-card__body">
              <p class="tx-card__title">{{ displayTitle(tx) }}</p>
              <p v-if="displaySub(tx)" class="tx-card__sub">{{ displaySub(tx) }}</p>
            </div>
            <div class="tx-card__right">
              <span class="tx-card__amount">{{ formatAmount(tx) }}</span>
              <span class="tx-card__time">{{ formatDateTime(tx.occurredAt) }}</span>
            </div>
          </li>
        </ul>
      </section>

      <!-- 加载更多 / 底部提示 -->
      <div ref="sentinel" class="tx-sentinel" />
      <div v-if="loadingMore" class="tx-footer">加载更多…</div>
      <div v-else-if="initialLoaded && !hasMore && items.length > 0" class="tx-footer">— 已加载全部 —</div>
    </div>
    </PageLayout>

    <BottomNav active="transactions" />
  </div>
</template>

<style scoped lang="scss">
.tx-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-width: 390px;
  margin: 0 auto;
  overflow: hidden;

  :deep(.page-layout) {
    flex: 1;
    height: auto;
    min-height: 0;
  }

  :deep(.page-layout__main) {
    padding-bottom: 96px;
  }

  :deep(.page-layout__home) {
    display: none;
  }
}

/* ===== 可视化统计 ===== */
.tx-viz {
  padding: 16px;
  margin-bottom: 14px;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #fff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    margin: 0;
    font-size: 13px;
    font-weight: 600;
    line-height: 18px;
    color: #696565;
  }

  &__hint {
    font-size: 11px;
    line-height: 16px;
    color: #afafaf;
  }

  &__body {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-top: 12px;
  }

  &__donut {
    position: relative;
    flex: none;
    width: 116px;
    height: 116px;
  }

  &__ring-bg {
    fill: none;
    stroke: #f0efef;
    stroke-width: 12;
  }

  &__ring-seg {
    fill: none;
    stroke-width: 12;
    transform: rotate(-90deg);
    transform-origin: 60px 60px;
    cursor: pointer;
    transition: stroke-dasharray 0.4s ease, stroke-dashoffset 0.4s ease, stroke-width 0.2s ease,
      opacity 0.2s ease;

    &.is-active {
      stroke-width: 15;
    }

    &.is-dim {
      opacity: 0.25;
    }
  }

  &__center {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
  }

  &__center-label {
    font-size: 11px;
    color: #afafaf;
  }

  &__center-amount {
    max-width: 82px;
    font-size: 13px;
    font-weight: 700;
    color: #353333;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__center-sub {
    font-size: 11px;
    font-weight: 600;
    color: #17b7bd;
    font-variant-numeric: tabular-nums;
  }

  &__legend {
    flex: 1;
    min-width: 0;
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__legend-row {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 6px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    font-family: inherit;
    font-size: 12px;
    color: #696565;
    text-align: left;
    cursor: pointer;
    transition: background 0.15s ease;

    &:hover {
      background: #f7f7f7;
    }

    &.is-active {
      background: #eef8f7;

      .tx-viz__cat {
        color: #17b7bd;
        font-weight: 600;
      }
    }
  }

  &__dot {
    flex: none;
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  &__cat {
    flex: 1;
    min-width: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__pct {
    flex: none;
    font-weight: 600;
    color: #353333;
    font-variant-numeric: tabular-nums;
  }

  &__empty {
    flex: 1;
    margin: 0;
    font-size: 12px;
    color: #afafaf;
  }

  &__ratio {
    display: flex;
    height: 6px;
    margin-top: 14px;
    border-radius: 3px;
    overflow: hidden;
    background: #f6f6f6;
  }

  &__ratio-income {
    background: #17b7bd;
  }

  &__ratio-spent {
    background: linear-gradient(180deg, #f27473 0%, #e73e79 100%);
  }

  &__totals {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }

  &__total {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    padding: 8px 6px;
    border-radius: 8px;
    background: #f7f9f8;
  }

  &__total-label {
    font-size: 11px;
    color: #afafaf;
  }

  &__total-value {
    max-width: 100%;
    font-size: 12px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.is-income {
  color: #22c55e;
}

.is-spent {
  color: #ef4444;
}

.tx-wallet {
  margin-bottom: 10px;

  &__select {
    width: 100%;
    height: 36px;
    padding: 0 12px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 10px;
    background: #fff;
    font-family: inherit;
    font-size: 13px;
    color: #353333;
  }
}

.tx-filter {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 14px;
  padding-bottom: 4px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }

  &__chip {
    flex-shrink: 0;
    height: 34px;
    padding: 0 16px;
    border: 1px solid #f0eeef;
    border-radius: 17px;
    background: #ffffff;
    font-family: inherit;
    font-size: 13px;
    font-weight: 500;
    color: #696565;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s;

    &--active {
      border-color: #17b7bd;
      background: #17b7bd;
      color: #fff;
      font-weight: 600;
    }
  }
}

.tx-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &--skeleton {
    gap: 12px;
  }
}

.tx-group {
  display: flex;
  flex-direction: column;
  gap: 8px;

  &__title {
    margin: 2px 2px 0;
    font-size: 13px;
    font-weight: 600;
    line-height: 18px;
    color: #696565;
  }

  &__list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
}

.tx-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #f0eeef;
  background: #fff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
  cursor: pointer;
  transition: transform 0.1s ease, box-shadow 0.15s ease;

  &:active {
    transform: scale(0.99);
  }

  &__icon {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;

    &--skeleton {
      width: 38px;
      height: 38px;
      background: #ecebea;
    }
  }

  &__icon-img {
    width: 20px;
    height: 20px;
    object-fit: contain;
  }

  &__icon svg {
    width: 16px;
    height: 16px;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    line-height: 21px;
    color: #353333;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__sub {
    margin: 2px 0 0;
    font-size: 12px;
    line-height: 17px;
    color: #afafaf;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    flex-shrink: 0;
  }

  &__amount {
    font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
    font-size: 15px;
    font-weight: 700;
    line-height: 21px;
  }

  &__time {
    margin-top: 2px;
    font-size: 11px;
    line-height: 16px;
    color: #afafaf;
  }

  &--income .tx-card__amount {
    color: #22c55e;
  }

  &--expense .tx-card__amount {
    color: #ef4444;
  }

  // 骨架状态下去掉点击感
  &--skeleton {
    cursor: default;

    &:active {
      transform: none;
    }
  }
}

.tx-skel {
  display: block;
  background: linear-gradient(90deg, #f0efef 25%, #e7e6e6 50%, #f0efef 75%);
  background-size: 200% 100%;
  border-radius: 4px;
  animation: tx-skel 1.2s ease-in-out infinite;

  &--title {
    width: 55%;
    height: 14px;
    margin-bottom: 8px;
  }

  &--sub {
    width: 80%;
    height: 10px;
  }

  &--amount {
    width: 64px;
    height: 16px;
    border-radius: 4px;
  }
}

@keyframes tx-skel {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.tx-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;

  &__illu {
    width: 120px;
    height: 120px;
    margin-bottom: 16px;
  }

  &__text {
    margin: 0;
    font-size: 14px;
    color: #696565;
  }
}

.tx-sentinel {
  height: 1px;
}

.tx-footer {
  padding: 16px 0 8px;
  text-align: center;
  font-size: 12px;
  color: #afafaf;
}
</style>
