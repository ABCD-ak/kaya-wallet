<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import BottomNav from '@/components/BottomNav.vue'
import TxCategoryIcon from '@/components/TxCategoryIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { listWallets, getWalletSummary } from '@/api/wallets'
import { listTransactions } from '@/api/transactions'
import { listCards } from '@/api/cards'
import { getUnreadCount } from '@/api/notifications'
import type { WalletResponse, WalletSummaryResponse, TransactionResponse, CardResponse } from '@/api/types'

const router = useRouter()
const auth = useAuthStore()

// ========== 业务数据 ==========
const wallets = ref<WalletResponse[]>([])
const activeWalletId = ref('')
const summary = ref<WalletSummaryResponse | null>(null)
const txs = ref<TransactionResponse[]>([])
const cards = ref<CardResponse[]>([])
const unreadCount = ref(0)
const txFilter = ref<'all' | 'send' | 'received'>('all')

// 顶部银行卡轮播：有几张卡就展示几张，无卡时展示一张占位卡
const cardSlides = computed<Array<CardResponse | null>>(() =>
  cards.value.length ? cards.value : [null],
)
const activeCardIndex = ref(0)
const cardsEl = ref<HTMLElement | null>(null)

function brandLabel(card: CardResponse | null): string {
  const brands: Record<string, string> = {
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
  return brands[card?.brand ?? ''] ?? '银行卡'
}

// 各卡组织/银行的标准色（用于品牌标识和卡面主题）
const BANK_COLORS: Record<string, string> = {
  icbc: '#c7000b',
  abc: '#009a44',
  boc: '#a6192e',
  ccb: '#0066b3',
  bocom: '#005bac',
  psbc: '#007a33',
}
const CARD_THEME_BRANDS = new Set([
  'visa',
  'mastercard',
  'unionpay',
  'icbc',
  'abc',
  'boc',
  'ccb',
  'bocom',
  'psbc',
])
function cardTheme(card: CardResponse | null): string {
  const brand = card?.brand ?? ''
  return CARD_THEME_BRANDS.has(brand) ? `balance-card--${brand}` : 'balance-card--default'
}
function bankColor(card: CardResponse | null): string {
  return BANK_COLORS[card?.brand ?? ''] ?? '#afafaf'
}
function brandSub(card: CardResponse | null): string {
  const subs: Record<string, string> = {
    abc: '金穗白金',
    ccb: '龙卡 · 电影主题',
    bocom: '海南文旅',
    psbc: '中铁联名',
  }
  return subs[card?.brand ?? ''] ?? ''
}
function expiryLabel(card: CardResponse | null): string {
  if (!card) return '--/--'
  return `${String(card.expiryMonth).padStart(2, '0')}/${String(card.expiryYear).slice(-2)}`
}
// 卡号展示模式：false = 掩码（•••• •••• •••• 1234），true = 完整卡号
const showFullNumber = ref(false)
function fullCardNumber(card: CardResponse | null): string {
  if (!card) return ''
  const prefixMap: Record<string, string> = { visa: '4231', mastercard: '5412', unionpay: '6222' }
  const prefix = prefixMap[card.brand ?? ''] ?? '0000'
  // 依据卡 ID 生成稳定的中间 8 位数字，保证同一张卡展示结果不变
  let hash = 0
  for (const ch of card.id) hash = (hash * 31 + ch.charCodeAt(0)) % 100000000
  const middle = String(hash).padStart(8, '0')
  const last4 = (card.last4 || '').padStart(4, '0')
  const digits = (prefix + middle + last4).slice(0, 16)
  return digits.replace(/(.{4})/g, '$1 ').trim()
}

function onCardsScroll() {
  const el = cardsEl.value
  if (!el || !el.clientWidth) return
  activeCardIndex.value = Math.min(
    cardSlides.value.length - 1,
    Math.round(el.scrollLeft / el.clientWidth),
  )
}
function goSlide(i: number) {
  const el = cardsEl.value
  if (!el) return
  el.scrollTo({ left: i * el.clientWidth, behavior: 'smooth' })
}

function fmtNum(raw?: string): string {
  const s = raw ?? '0.00'
  const [whole, dec] = s.split('.')
  return whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '.' + (dec ?? '00')
}
function fmtAmount(raw?: string, code?: string): string {
  const map: Record<string, string> = { CNY: '¥', USD: '$', EUR: '€', JPY: '¥', GBP: '£', HKD: '港币' }
  const sym = map[code ?? ''] ?? '¥'
  return `${sym} ${fmtNum(raw)}`
}
function txTitle(t: TransactionResponse): string {
  return t.type.startsWith('transfer') ? (t.counterparty?.name ?? '转账') : (t.category?.name ?? '未分类')
}
function txSub(t: TransactionResponse): string {
  const base =
    t.note ||
    (t.type === 'transfer_out' ? '转出' : t.type === 'transfer_in' ? '转入' : t.type === 'income' ? '收入' : t.type === 'expense' ? '支出' : '')
  const fee = Number(t.feeAmount ?? 0)
  if (fee > 0) {
    return `${base ? base + ' · ' : ''}手续费 ${fmtAmount(t.feeAmount, t.currencyCode)}`
  }
  return base
}

// 支出金额含手续费，与真实扣款保持一致
function txTotal(t: TransactionResponse): string {
  const fee = Number(t.feeAmount ?? 0)
  if (t.direction === 'debit' && fee > 0) {
    return (Number(t.amount) + fee).toFixed(2)
  }
  return t.amount
}

const filteredTxs = computed(() => {
  if (txFilter.value === 'send') return txs.value.filter((t) => t.direction === 'debit')
  if (txFilter.value === 'received') return txs.value.filter((t) => t.direction === 'credit')
  return txs.value
})
const balanceAmount = computed(() => {
  const inc = Number(summary.value?.income ?? 0)
  const sp = Number(summary.value?.spent ?? 0)
  return (inc - sp).toFixed(2)
})
const monthLabel = `${new Date().getMonth() + 1}月`

// 微信风格预览开关：当前关闭，恢复品牌卡片版首页；改为 true 可再次预览微信风格
const wechatPreview = false

const primaryCard = computed<CardResponse | null>(
  () => cards.value.find((c) => c.isPrimary) ?? cards.value[0] ?? null,
)
const walletCurrency = computed(() => wallets.value[0]?.currencyCode)

// 中国银行卡优先展示，其余保持原顺序
function putBocFirst(list: CardResponse[]): CardResponse[] {
  const index = list.findIndex((card) => card.brand === 'boc')
  if (index <= 0) return list
  const next = [...list]
  const [boc] = next.splice(index, 1)
  next.unshift(boc)
  return next
}

async function loadData() {
  try {
    auth.fetchMe().catch(() => {})
    getUnreadCount()
      .then((r) => { unreadCount.value = r.count })
      .catch(() => {})
    const w = await listWallets()
    wallets.value = w
    summary.value = null
    txs.value = []
    cards.value = []
    activeWalletId.value = w[0]?.id ?? ''
    if (!w.length) return

    const walletId = w[0].id
    const [s, t, c] = await Promise.all([
      getWalletSummary(walletId),
      listTransactions({ walletId, pageSize: 5 }),
      // 银行卡按用户维度返回，不按钱包过滤
      listCards(undefined, 'active'),
    ])
    summary.value = s
    txs.value = t.items ?? []
    cards.value = putBocFirst(c)
  } catch {
    ElMessage.error('加载失败')
  }
}
onMounted(loadData)
</script>

<template>
  <div
    class="wallet-page"
    :class="{
      'wallet-page--wechat': wechatPreview,
      'wallet-page--classic': !wechatPreview,
    }"
  >
    <PageLayout :title="wechatPreview ? '钱包' : '我的钱包'" :show-back="false">
      <template #actions>
        <button class="notif" aria-label="消息通知" @click="router.push('/notifications')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
          <span v-if="unreadCount > 0" class="notif__badge">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>
      </template>

      <!-- 微信风格预览 -->
      <template v-if="wechatPreview">
        <!-- 绿色余额卡 -->
        <section class="wx-balance">
          <div class="wx-balance__head">
            <span>零钱</span>
            <span class="wx-balance__currency">{{ walletCurrency === 'USD' ? '美元' : '人民币' }}</span>
          </div>
          <div class="wx-balance__amount">{{ fmtNum(wallets[0]?.balance) }}</div>
          <div class="wx-balance__actions">
            <button type="button" @click="router.push('/receive')">收付款</button>
            <button type="button" @click="router.push('/transfers/new')">转账</button>
            <button type="button" @click="router.push('/wallet/add')">记账</button>
          </div>
        </section>

        <!-- 常用服务 -->
        <section class="wx-card">
          <button class="wx-cell" type="button" @click="router.push('/cards')">
            <span class="wx-cell__icon wx-cell__icon--green">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2.5" y="5" width="19" height="14" rx="2.5" />
                <path d="M2.5 9.5h19" />
                <path d="M6 15h5" />
              </svg>
            </span>
            <span class="wx-cell__body">
              <span class="wx-cell__title">银行卡</span>
              <span class="wx-cell__sub">
                {{ cards.length ? `${cards.length} 张卡 · 尾号 ${primaryCard?.last4 ?? ''}` : '未绑定银行卡' }}
              </span>
            </span>
            <span class="wx-cell__arrow">›</span>
          </button>

          <button class="wx-cell" type="button" @click="router.push('/transactions')">
            <span class="wx-cell__icon wx-cell__icon--blue">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 3.5h9l4 4V20a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4.5a1 1 0 0 1 1-1Z" />
                <path d="M14.5 3.5V8H19" />
                <path d="M8.5 12h7M8.5 16h5" />
              </svg>
            </span>
            <span class="wx-cell__body">
              <span class="wx-cell__title">账单</span>
            </span>
            <span class="wx-cell__arrow">›</span>
          </button>

          <button class="wx-cell" type="button" @click="router.push('/notifications')">
            <span class="wx-cell__icon wx-cell__icon--orange">
              <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
            </span>
            <span class="wx-cell__body">
              <span class="wx-cell__title">消息通知</span>
            </span>
            <span v-if="unreadCount > 0" class="wx-cell__badge">
              {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
            <span class="wx-cell__arrow">›</span>
          </button>

          <button class="wx-cell" type="button" @click="router.push('/support')">
            <span class="wx-cell__icon wx-cell__icon--indigo">?</span>
            <span class="wx-cell__body">
              <span class="wx-cell__title">帮助与客服</span>
            </span>
            <span class="wx-cell__arrow">›</span>
          </button>
        </section>

        <!-- 本月账单 -->
        <section class="wx-card">
          <div class="wx-card__head">
            <h2>本月账单</h2>
            <span>{{ monthLabel }}</span>
          </div>
          <div class="wx-metric">
            <span>收入</span>
            <strong class="is-income">{{ fmtAmount(summary?.income, walletCurrency) }}</strong>
          </div>
          <div class="wx-metric">
            <span>支出</span>
            <strong>{{ fmtAmount(summary?.spent, walletCurrency) }}</strong>
          </div>
          <div class="wx-metric">
            <span>结余</span>
            <strong>{{ fmtAmount(balanceAmount, walletCurrency) }}</strong>
          </div>
        </section>

        <!-- 最近交易 -->
        <section class="wx-card">
          <div class="wx-card__head">
            <h2>最近交易</h2>
            <button class="wx-link" type="button" @click="router.push('/transactions')">全部账单 ›</button>
          </div>
          <div v-if="txs.length === 0" class="wx-empty">暂无交易</div>
          <button
            v-for="tx in txs"
            :key="tx.id"
            class="wx-tx"
            type="button"
            @click="router.push(`/transactions/${tx.id}`)"
          >
            <span class="wx-tx__icon" :style="{ background: tx.category?.color || '#07c160' }">
              <svg v-if="tx.direction === 'debit'" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="5" y1="12" x2="19" y2="12" /><polyline points="12 5 19 12 12 19" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
              </svg>
            </span>
            <span class="wx-tx__body">
              <span class="wx-tx__title">{{ txTitle(tx) }}</span>
              <span class="wx-tx__sub">{{ txSub(tx) }}</span>
            </span>
            <span class="wx-tx__amount" :class="{ 'is-income': tx.direction === 'credit' }">
              {{ tx.direction === 'credit' ? '+' : '-' }}{{ fmtNum(txTotal(tx)) }}
            </span>
          </button>
        </section>
      </template>

      <template v-else>
      <!-- 银行卡轮播 -->
      <div class="balance-carousel">
        <section ref="cardsEl" class="balance-cards" @scroll.passive="onCardsScroll">
        <article
          v-for="card in cardSlides"
          :key="card?.id ?? 'empty'"
          class="balance-card"
          :class="cardTheme(card)"
        >
          <!-- 联名/纪念款装饰 -->
          <span v-if="card?.brand === 'mastercard'" class="card-globe" aria-hidden="true" />
          <svg
            v-if="card?.brand === 'boc'"
            class="card-wall"
            viewBox="0 0 335 120"
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <path class="card-wall__ridge" d="M0 82 L34 62 L60 72 L94 48 L126 64 L160 42 L196 60 L230 44 L264 58 L300 42 L335 52" />
            <path class="card-wall__base" d="M0 112 L0 98 L10 98 L10 91 L18 91 L18 98 L30 98 L30 90 L38 90 L38 98 L50 98 L50 92 L58 92 L58 100 L74 96 L74 89 L84 89 L84 98 L96 98 L96 91 L106 91 L106 98 L118 98 L118 90 L128 90 L128 98 L142 94 L142 87 L152 87 L152 94 L164 94 L164 86 L174 86 L174 94 L188 94 L188 88 L198 88 L198 94 L212 90 L212 82 L222 82 L222 90 L236 90 L236 83 L246 83 L246 90 L260 86 L260 78 L270 78 L270 86 L284 86 L284 79 L294 79 L294 86 L308 82 L308 74 L318 74 L318 82 L335 78 L335 112 Z" />
            <path class="card-wall__tower" d="M84 98 V62 H96 V98 M87 62 V55 H93 V62" />
            <path class="card-wall__tower" d="M222 90 V54 H236 V90 M225 54 V47 H233 V54" />
          </svg>
          <svg
            v-if="card?.brand === 'unionpay'"
            class="card-mountains"
            viewBox="0 0 335 110"
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <path d="M0 96 L28 60 L48 76 L78 42 L108 70 L140 50 L170 78 L205 52 L235 74 L270 46 L300 70 L335 58" />
            <path d="M0 104 L28 68 L48 84 L78 50 L108 78 L140 58 L170 86 L205 60 L235 82 L270 54 L300 78 L335 66" />
          </svg>
          <svg
            v-if="card?.brand === 'bocom'"
            class="card-waves"
            viewBox="0 0 335 70"
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <path d="M0 34 Q20 24 40 34 T80 34 T120 34 T160 34 T200 34 T240 34 T280 34 T320 34" />
            <path d="M0 48 Q20 38 40 48 T80 48 T120 48 T160 48 T200 48 T240 48 T280 48 T320 48" />
            <path d="M0 62 Q20 52 40 62 T80 62 T120 62 T160 62 T200 62 T240 62 T280 62 T320 62" />
          </svg>
          <svg
            v-if="card?.brand === 'psbc'"
            class="card-rail"
            viewBox="0 0 335 90"
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <path d="M0 70 C70 70 110 34 175 34 S290 68 335 56" />
            <path d="M0 80 C70 80 110 44 175 44 S290 78 335 66" />
            <path d="M0 80 C70 80 110 44 175 44 S290 78 335 66" stroke-dasharray="2 8" />
          </svg>

          <div class="balance-card__row">
            <span class="balance-card__label">当前余额</span>
            <span class="balance-card__brand">
              <span v-if="card?.brand === 'visa'" class="brand-stack brand-stack--end">
                <span class="brand-visa">VISA</span>
                <span class="brand-sub">INFINITE</span>
              </span>
              <template v-else-if="card?.brand === 'mastercard'">
                <span class="brand-mc" aria-hidden="true"><i /><i /></span>
                <span class="brand-stack">
                  <span>万事达</span>
                  <span class="brand-sub">world elite</span>
                </span>
              </template>
              <template v-else-if="card?.brand === 'unionpay'">
                <span class="brand-up" aria-hidden="true"><i /><i /><i /></span>
                <span>银联</span>
              </template>
              <template v-else-if="card?.brand === 'icbc'">
                <span class="brand-seal" aria-hidden="true">宫</span>
                <span class="brand-stack">
                  <span>故宫联名</span>
                  <span class="brand-sub">PALACE MUSEUM</span>
                </span>
              </template>
              <template v-else-if="card?.brand === 'boc'">
                <span class="brand-boc" aria-hidden="true">中</span>
                <span class="brand-stack">
                  <span>中国银行</span>
                  <span class="brand-sub">长城卡 · GREAT WALL</span>
                </span>
              </template>
              <template v-else-if="card?.brand === 'ccb'">
                <span class="brand-seal" aria-hidden="true">龙</span>
                <span class="brand-stack">
                  <span>中国建设银行</span>
                  <span class="brand-sub">龙卡 · 电影主题</span>
                </span>
              </template>
              <template v-else>
                <span class="brand-bank" aria-hidden="true" :style="{ background: bankColor(card) }" />
                <span class="brand-stack">
                  <span>{{ brandLabel(card) }}</span>
                  <span v-if="brandSub(card)" class="brand-sub">{{ brandSub(card) }}</span>
                </span>
              </template>
            </span>
          </div>

          <div class="balance-card__row balance-card__row--amount">
            <span class="balance-card__currency">{{ wallets[0]?.currencyCode === 'USD' ? '美元' : '人民币' }}</span>
            <span class="balance-card__amount">{{ fmtNum(card?.balance ?? wallets[0]?.balance) }}</span>
          </div>

          <div class="balance-card__number">
            <span v-if="showFullNumber && card" class="balance-card__full">{{ fullCardNumber(card) }}</span>
            <template v-else>
              <span class="balance-card__group">••••</span>
              <span class="balance-card__group">••••</span>
              <span class="balance-card__group">••••</span>
              <span class="balance-card__last">{{ card?.last4 || '----' }}</span>
            </template>
            <button
              class="balance-card__eye"
              :aria-label="showFullNumber ? '隐藏卡号' : '显示完整卡号'"
              @click.stop="showFullNumber = !showFullNumber"
            >
              <svg v-if="showFullNumber" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
          </div>

          <div class="balance-card__row balance-card__row--footer">
            <span class="balance-card__holder">{{ card?.holderName || auth.user?.displayName || '' }}</span>
            <span class="balance-card__expiry">{{ expiryLabel(card) }}</span>
          </div>
        </article>
        </section>

        <button
          v-if="cardSlides.length > 1 && activeCardIndex > 0"
          class="card-nav card-nav--prev"
          type="button"
          aria-label="上一张卡"
          @click="goSlide(activeCardIndex - 1)"
        >
          ‹
        </button>
        <button
          v-if="cardSlides.length > 1 && activeCardIndex < cardSlides.length - 1"
          class="card-nav card-nav--next"
          type="button"
          aria-label="下一张卡"
          @click="goSlide(activeCardIndex + 1)"
        >
          ›
        </button>

        <div v-if="cardSlides.length > 1" class="dots">
          <span
            v-for="(_card, i) in cardSlides"
            :key="`dot-${i}`"
            class="dot"
            :class="i === activeCardIndex ? 'dot--active' : 'dot--inactive'"
            @click="goSlide(i)"
          />
        </div>
      </div>

      <!-- 本月概览 -->
      <section class="overview">
        <div class="overview__head">
          <h2 class="section-title">本月概览</h2>
          <span class="overview__month">{{ monthLabel }}</span>
        </div>
        <div class="overview__metrics">
          <div class="overview__metric">
            <span class="overview__label">收入</span>
            <strong class="overview__value overview__value--income">
              {{ fmtAmount(summary?.income, wallets[0]?.currencyCode) }}
            </strong>
          </div>
          <div class="overview__metric">
            <span class="overview__label">支出</span>
            <strong class="overview__value">
              {{ fmtAmount(summary?.spent, wallets[0]?.currencyCode) }}
            </strong>
          </div>
          <div class="overview__metric">
            <span class="overview__label">结余</span>
            <strong class="overview__value">
              {{ fmtAmount(balanceAmount, wallets[0]?.currencyCode) }}
            </strong>
          </div>
        </div>
      </section>

      <!-- 快捷操作：转账 + 收款 -->
      <section class="quick-actions">
        <button class="quick-action quick-action--primary" type="button" @click="router.push('/transfers/new')">
          <span class="quick-action__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </span>
          <span class="quick-action__label">转账</span>
        </button>
        <button class="quick-action" type="button" @click="router.push('/receive')">
          <span class="quick-action__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7" rx="1" />
              <rect x="14" y="3" width="7" height="7" rx="1" />
              <rect x="3" y="14" width="7" height="7" rx="1" />
              <path d="M14 14h3v3h-3z" />
              <path d="M21 14v3" />
              <path d="M14 21h3" />
              <path d="M21 21v.01" />
            </svg>
          </span>
          <span class="quick-action__label">收款</span>
        </button>
      </section>

      <!-- 最近交易 -->
      <div class="list-head">
        <h2 class="section-title">最近交易</h2>
        <button class="see-more" aria-label="查看全部交易" @click="router.push('/transactions')">
          查看全部 <span>›</span>
        </button>
      </div>

      <div class="chips">
        <button class="chip" :class="{ active: txFilter === 'all' }" @click="txFilter = 'all'">全部</button>
        <button class="chip" :class="{ active: txFilter === 'received' }" @click="txFilter = 'received'">转入</button>
        <button class="chip" :class="{ active: txFilter === 'send' }" @click="txFilter = 'send'">转出</button>
      </div>

      <section class="tx-list">
        <div v-if="filteredTxs.length === 0" class="tx-empty">
          <p>暂无交易</p>
        </div>
        <article
          v-for="tx in filteredTxs"
          :key="tx.id"
          class="tx-row"
          @click="router.push(`/transactions/${tx.id}`)"
        >
          <TxCategoryIcon :tx="tx" :size="36" />
          <div class="tx-row__body">
            <div class="tx-row__title">{{ txTitle(tx) }}</div>
            <div class="tx-row__sub">{{ txSub(tx) }}</div>
          </div>
          <div class="tx-row__amount" :class="{ pos: tx.direction === 'credit' }">
            {{ tx.direction === 'credit' ? '+' : '-' }}{{ fmtNum(txTotal(tx)) }}
          </div>
        </article>
      </section>
      </template>
    </PageLayout>

    <BottomNav @fab-click="router.push('/wallet/add')" />
  </div>
</template>

<style scoped lang="scss">
.wallet-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-width: 390px;
  margin: 0 auto;
  overflow: hidden;
  background: #f7f6f8;

  @media (min-width: 420px) {
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.12);
  }
}

.wallet-page :deep(.page-layout) {
  flex: 1;
  height: auto;
  min-height: 0;
  background: #f7f6f8;
}

.wallet-page :deep(.status-bar) {
  background: #f7f6f8;
}

.wallet-page :deep(.page-layout__header) {
  height: 56px;
  padding: 0 20px;
  background: #f7f6f8;
}

.wallet-page :deep(.page-layout__title) {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.4px;
  color: #353333;
}

.wallet-page :deep(.page-layout__main) {
  padding: 8px 20px 64px;
}

.wallet-page :deep(.page-layout__home) {
  display: none;
}

/* ===== 早期绿色渐变版 ===== */
.wallet-page--classic {
  background: #f7f6f8;
}

.wallet-page--classic :deep(.page-layout) {
  position: relative;
  background: #f7f6f8;
}

.wallet-page--classic :deep(.page-layout)::before {
  display: none;
}

.wallet-page--classic :deep(.status-bar) {
  background: #17b7bd;
}

.wallet-page--classic :deep(.page-layout__header) {
  height: 64px;
  background: linear-gradient(180deg, #17b7bd 0%, #56d3ac 100%);
}

.wallet-page--classic :deep(.page-layout__main) {
  padding: 0 20px 104px;
  background-image: linear-gradient(180deg, #56d3ac 0%, #86e29f 45%, #f7f6f8 100%);
  background-position: top center;
  background-size: 100% 300px;
  background-repeat: no-repeat;
  background-attachment: local;
  scrollbar-width: none;
}

.wallet-page--classic :deep(.page-layout__main)::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.wallet-page--classic :deep(.status-bar__time) {
  color: #ffffff;
}

.wallet-page--classic :deep(.status-bar__icons img) {
  filter: none;
}

.wallet-page--classic :deep(.page-layout__title) {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.4px;
  color: #ffffff;
}

.wallet-page--classic .notif {
  width: 36px;
  height: 36px;
  border: 0;
  background: transparent;
  box-shadow: none;
  color: #ffffff;
}

.wallet-page--classic .notif__badge {
  background: #e5484d;
  color: #ffffff;
  box-shadow: 0 0 0 2px #ffffff;
}

.wallet-page--classic .dot--active {
  background: #17b7bd;
}

/* 本月概览 */
.wallet-page--classic .overview {
  border: 0;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(26, 41, 41, 0.06);
}

.wallet-page--classic .section-title {
  color: #353333;
}

.wallet-page--classic .overview__month {
  color: #afafaf;
}

.wallet-page--classic .overview__metric + .overview__metric {
  border-left-color: #f1f3f8;
}

.wallet-page--classic .overview__label {
  color: #696565;
}

.wallet-page--classic .overview__value {
  color: #353333;
}

.wallet-page--classic .overview__value--income {
  color: #353333;
}

/* 转账 / 收款 */
.wallet-page--classic .quick-actions {
  gap: 0;
  padding: 4px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(26, 41, 41, 0.05);
}

.wallet-page--classic .quick-action {
  height: 48px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  box-shadow: none;
  color: #353333;
}

.wallet-page--classic .quick-action + .quick-action {
  border-left: 1px solid #f1f3f8;
  border-radius: 0;
}

.wallet-page--classic .quick-action--primary {
  background: transparent;
  color: #17b7bd;
  box-shadow: none;
}

.wallet-page--classic .quick-action__icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e6faf4;
  color: #17b7bd;
}

.wallet-page--classic .quick-action--primary .quick-action__icon {
  background: #e9f9f5;
  color: #17b7bd;
}

.wallet-page--classic .see-more {
  color: #17b7bd;
  font-size: 12px;
}

/* 交易筛选：早期为纯文字切换 */
.wallet-page--classic .chips {
  gap: 18px;
  margin: 4px 0 10px;
}

.wallet-page--classic .chip {
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  font-size: 15px;
  color: #696565;
  opacity: 0.55;
}

.wallet-page--classic .chip.active {
  border: 0;
  background: transparent;
  color: #353333;
  font-weight: 500;
  opacity: 1;
}

/* 最近交易列表 */
.wallet-page--classic .tx-list {
  padding: 0 16px;
  border: 0;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(26, 41, 41, 0.05);
}

.wallet-page--classic .tx-row {
  padding: 14px 0;
  border-top-color: #f1f3f8;
}

.wallet-page--classic .tx-row__title {
  color: #353333;
  font-size: 14px;
  font-weight: 500;
}

.wallet-page--classic .tx-row__sub {
  color: #afafaf;
  font-size: 13px;
}

.wallet-page--classic .tx-row__amount {
  color: #ef6874;
  font-weight: 700;
}

.wallet-page--classic .tx-row__amount.pos {
  color: #17b7bd;
}

/* ===== 微信风格预览 ===== */
.wallet-page--wechat {
  background: #ededed;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.wallet-page--wechat :deep(.page-layout) {
  background: #ededed;
}

.wallet-page--wechat :deep(.status-bar) {
  background: #ededed;
}

.wallet-page--wechat :deep(.page-layout__header) {
  height: 48px;
  padding: 0 12px;
  background: #ededed;
  justify-content: center;
}

.wallet-page--wechat :deep(.page-layout__title) {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-family: inherit;
  font-size: 17px;
  font-weight: 600;
  line-height: 24px;
  letter-spacing: 0;
  color: #191919;
}

.wallet-page--wechat :deep(.page-layout__main) {
  padding: 8px 12px 72px;
}

.wallet-page--wechat .notif {
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  box-shadow: none;
  color: #191919;
}

.wallet-page--wechat .notif__badge {
  background: #fa5151;
  box-shadow: 0 0 0 2px #ededed;
}

/* 底部导航切换为微信绿 */
.wallet-page--wechat :deep(.bn) {
  background: #f7f7f7;
  border-top-color: #e5e5e5;
}

.wallet-page--wechat :deep(.bn__item) {
  color: #7f7f7f;
}

.wallet-page--wechat :deep(.bn__item.active) {
  color: #07c160;
}

.wallet-page--wechat :deep(.bn__fab) {
  background: #07c160;
  box-shadow: 0 8px 18px rgba(7, 193, 96, 0.28);
}

/* 绿色余额卡 */
.wx-balance {
  padding: 18px 18px 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #07c160 0%, #06ad56 100%);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(7, 193, 96, 0.18);
}

.wx-balance__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  opacity: 0.9;
}

.wx-balance__currency {
  font-size: 11px;
  opacity: 0.75;
}

.wx-balance__amount {
  margin-top: 6px;
  font-size: 32px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.5px;
  font-variant-numeric: tabular-nums;
}

.wx-balance__actions {
  display: flex;
  margin: 18px -18px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}

.wx-balance__actions button {
  flex: 1;
  height: 44px;
  border: 0;
  background: none;
  color: #ffffff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.wx-balance__actions button + button {
  border-left: 1px solid rgba(255, 255, 255, 0.18);
}

.wx-balance__actions button:active {
  background: rgba(0, 0, 0, 0.06);
}

/* 白色分组 */
.wx-card {
  margin-top: 10px;
  border-radius: 10px;
  background: #ffffff;
  overflow: hidden;
}

.wx-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
}

.wx-card__head h2 {
  margin: 0;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  color: #191919;
}

.wx-card__head > span {
  font-size: 11px;
  color: #999999;
}

.wx-link {
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 12px;
  color: #576b95;
  cursor: pointer;
}

/* 服务列表 */
.wx-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 56px;
  padding: 10px 16px;
  border: 0;
  border-top: 1px solid #f0f0f0;
  background: #ffffff;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.wx-cell:first-child {
  border-top: 0;
}

.wx-cell:active {
  background: #f5f5f5;
}

.wx-cell__icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
}

.wx-cell__icon svg {
  width: 18px;
  height: 18px;
}

.wx-cell__icon--green {
  background: #07c160;
}

.wx-cell__icon--blue {
  background: #10aeff;
}

.wx-cell__icon--orange {
  background: #fa9d3b;
}

.wx-cell__icon--indigo {
  background: #576b95;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
}

.wx-cell__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.wx-cell__title {
  font-size: 15px;
  color: #191919;
}

.wx-cell__sub {
  font-size: 12px;
  color: #888888;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wx-cell__arrow {
  flex: none;
  padding-left: 4px;
  font-size: 20px;
  line-height: 1;
  color: #c4c4c4;
}

.wx-cell__badge {
  flex: none;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #fa5151;
  color: #ffffff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

/* 本月账单 */
.wx-metric {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 16px;
  border-top: 1px solid #f5f5f5;
}

.wx-metric span {
  font-size: 13px;
  color: #7f7f7f;
}

.wx-metric strong {
  font-size: 14px;
  font-weight: 500;
  color: #191919;
  font-variant-numeric: tabular-nums;
}

.wx-metric strong.is-income {
  color: #07c160;
}

/* 最近交易 */
.wx-empty {
  padding: 28px 0 32px;
  text-align: center;
  font-size: 13px;
  color: #999999;
}

.wx-tx {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 60px;
  padding: 10px 16px;
  border: 0;
  border-top: 1px solid #f5f5f5;
  background: #ffffff;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.wx-tx:active {
  background: #f5f5f5;
}

.wx-tx__icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.wx-tx__icon svg {
  width: 16px;
  height: 16px;
}

.wx-tx__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.wx-tx__title {
  font-size: 14px;
  color: #191919;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wx-tx__sub {
  font-size: 11px;
  color: #999999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wx-tx__amount {
  flex: none;
  font-size: 15px;
  font-weight: 500;
  color: #191919;
  font-variant-numeric: tabular-nums;
}

.wx-tx__amount.is-income {
  color: #07c160;
}

.notif {
  position: relative;
  flex: none;
  width: 36px;
  height: 36px;
  border: 1px solid #f0eeef;
  border-radius: 50%;
  background: #ffffff;
  color: #353333;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;

  svg {
    width: 18px;
    height: 18px;
  }
}

.notif__badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #e5484d;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  box-shadow: 0 0 0 2px #f7f6f8;
}

/* ===== 余额卡片 ===== */
.balance-carousel {
  position: relative;
}

.card-nav {
  position: absolute;
  /* 位于金额行与卡号行之间的空白带，避开右侧眼睛图标 */
  top: 35%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0 0 2px;
  border: 1px solid rgba(26, 41, 41, 0.08);
  border-radius: 50%;
  background: #17b7bd;
  box-shadow: 0 6px 16px rgba(23, 183, 189, 0.28);
  font-family: 'Poppins', sans-serif;
  font-size: 20px;
  line-height: 1;
  color: #ffffff;
  cursor: pointer;
  z-index: 2;
}
.card-nav--prev {
  left: -14px;
}
.card-nav--next {
  right: -14px;
}

.balance-cards {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.balance-cards::-webkit-scrollbar {
  display: none;
}

.balance-card {
  /* 默认钛银卡面 */
  --card-ink: #353333;
  --card-muted: #475467;
  --card-line: rgba(26, 41, 41, 0.1);
  --card-chip: rgba(255, 255, 255, 0.55);
  --card-eye: #475467;
  --card-sheen: rgba(255, 255, 255, 0.6);
  --card-border: rgba(255, 255, 255, 0.85);
  --card-edge: linear-gradient(90deg, #0f766e 0%, #3f938c 50%, #c5dfdd 100%);
  --card-shadow:
    0 24px 48px rgba(31, 45, 42, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);

  position: relative;
  flex: 0 0 100%;
  box-sizing: border-box;
  overflow: hidden;
  scroll-snap-align: center;
  padding: 18px;
  border: 1px solid var(--card-border);
  border-radius: 20px;
  /* 钛银金属：亮面扫光 + 冷灰基底 */
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0) 28%),
    linear-gradient(135deg, #e9ece9 0%, #d2d8d4 32%, #eff1ee 54%, #c0c8c3 78%, #dce1de 100%);
  box-shadow: var(--card-shadow);
}
.balance-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  /* 斜向高光，模拟金属卡的反光带 */
  background: linear-gradient(
    105deg,
    rgba(255, 255, 255, 0) 32%,
    var(--card-sheen) 45%,
    rgba(255, 255, 255, 0) 58%
  );
  pointer-events: none;
}
.balance-card::before {
  content: '';
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  height: 3px;
  background: var(--card-edge);
  opacity: 0.9;
}

/* ===== 各卡组织 / 银行专属卡面 ===== */
.balance-card--visa {
  --card-ink: #353333;
  --card-muted: #696565;
  --card-line: rgba(26, 41, 41, 0.1);
  --card-chip: rgba(255, 255, 255, 0.65);
  --card-eye: #475467;
  --card-sheen: rgba(255, 255, 255, 0.8);
  --card-sub: #696565;
  --card-brand: #1434cb;
  --card-border: rgba(255, 255, 255, 0.9);
  --card-edge: linear-gradient(90deg, #1434cb 0%, #6b7cff 55%, #d7dcea 100%);
  --card-shadow:
    0 24px 48px rgba(31, 45, 42, 0.16),
    inset 0 0 0 1px rgba(255, 255, 255, 0.9);
  /* Visa 2025：珍珠白 + Visa Blue */
  background:
    radial-gradient(90% 120% at 88% -10%, rgba(20, 52, 203, 0.1) 0%, rgba(20, 52, 203, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f7f8f6 0%, #e3e7e4 45%, #fafbfa 70%, #dfe4e1 100%);
}
.balance-card--mastercard {
  --card-ink: #ffffff;
  --card-muted: rgba(255, 255, 255, 0.55);
  --card-line: rgba(255, 255, 255, 0.16);
  --card-chip: rgba(255, 255, 255, 0.1);
  --card-eye: rgba(255, 255, 255, 0.68);
  --card-sheen: rgba(255, 255, 255, 0.16);
  --card-sub: #d9b56a;
  --card-border: rgba(255, 255, 255, 0.14);
  --card-edge: linear-gradient(90deg, #eb001b 0%, #ff5f00 50%, #f79e1b 100%);
  --card-shadow:
    0 24px 48px rgba(0, 0, 0, 0.34),
    inset 0 0 0 1px rgba(217, 181, 106, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  /* World Elite：黑金卡 + globe 线框 */
  background:
    radial-gradient(90% 120% at 88% -10%, rgba(235, 0, 27, 0.35) 0%, rgba(235, 0, 27, 0) 55%),
    radial-gradient(70% 100% at 0% 110%, rgba(247, 158, 27, 0.18) 0%, rgba(247, 158, 27, 0) 60%),
    linear-gradient(135deg, #0b0b0d 0%, #1c1c21 50%, #0a0a0c 100%);
}
.balance-card--unionpay {
  --card-ink: #1f2937;
  --card-muted: #696565;
  --card-line: rgba(26, 41, 41, 0.1);
  --card-chip: rgba(255, 255, 255, 0.6);
  --card-eye: #4b5563;
  --card-sheen: rgba(255, 255, 255, 0.7);
  --card-sub: #b23a34;
  --card-border: rgba(255, 255, 255, 0.9);
  --card-edge: linear-gradient(90deg, #e21836 0%, #00447c 50%, #007b84 100%);
  --card-shadow:
    0 24px 48px rgba(52, 44, 30, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  /* 银联锦绣中华·广西：山水 + 朱瑾 */
  background:
    radial-gradient(80% 120% at 90% -10%, rgba(226, 24, 54, 0.08) 0%, rgba(226, 24, 54, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f5f7f2 0%, #e4ece5 45%, #f8faf6 70%, #d5e2da 100%);
}

/* 国内银行：深色金属卡 + 各银行标准色边缘 */
.balance-card--icbc,
.balance-card--abc,
.balance-card--ccb {
  --card-ink: #ffffff;
  --card-muted: rgba(255, 255, 255, 0.58);
  --card-line: rgba(255, 255, 255, 0.16);
  --card-chip: rgba(255, 255, 255, 0.1);
  --card-eye: rgba(255, 255, 255, 0.7);
  --card-sheen: rgba(255, 255, 255, 0.15);
  --card-border: rgba(255, 255, 255, 0.14);
  --card-shadow:
    0 24px 48px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
}
.balance-card--icbc {
  --card-sub: #e8c27a;
  --card-border: rgba(232, 194, 122, 0.26);
  --card-edge: linear-gradient(90deg, #8a6a2f 0%, #e8c27a 50%, #b58a3c 100%);
  --card-shadow:
    0 24px 48px rgba(6, 18, 32, 0.34),
    inset 0 0 0 1px rgba(232, 194, 122, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  /* 工银故宫联名「秋」官方卡面：角楼 + 圆月 + 烫金 */
  background:
    linear-gradient(rgba(3, 8, 18, 0.55), rgba(3, 8, 18, 0.55)),
    url('/assets/icbc-palace-autumn-front.jpg') center 42% / cover no-repeat;
}
.balance-card--abc {
  --card-sub: #d9b56a;
  --card-edge: linear-gradient(90deg, #006b43 0%, #17a06a 55%, #d9b56a 100%);
  --card-shadow:
    0 24px 48px rgba(4, 40, 26, 0.3),
    inset 0 0 0 1px rgba(217, 181, 106, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  /* 金穗白金：青绿 + 金 */
  background:
    radial-gradient(90% 120% at 88% -10%, rgba(23, 160, 106, 0.4) 0%, rgba(23, 160, 106, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.14) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #062b1d 0%, #0f6b49 50%, #042116 100%);
}
.balance-card--boc {
  --card-ink: #3b2a16;
  --card-muted: #8a6a2f;
  --card-line: rgba(90, 60, 20, 0.14);
  --card-chip: rgba(255, 255, 255, 0.5);
  --card-eye: #6b4e1f;
  --card-sheen: rgba(255, 255, 255, 0.75);
  --card-sub: #8a6a2f;
  --card-border: rgba(255, 255, 255, 0.8);
  --card-edge: linear-gradient(90deg, #8a6a2f 0%, #e8c27a 50%, #b58a3c 100%);
  --card-shadow:
    0 24px 48px rgba(90, 60, 20, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  /* 中行长城卡：香槟金 + 长城 */
  background:
    radial-gradient(80% 120% at 88% -10%, rgba(232, 194, 122, 0.28) 0%, rgba(232, 194, 122, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f7ecd8 0%, #e6d0a8 45%, #fbf5e9 70%, #d9c193 100%);
}
.balance-card--ccb {
  --card-sub: #e8c27a;
  --card-edge: linear-gradient(90deg, #0b3b2e 0%, #1f6f56 55%, #e8c27a 100%);
  --card-shadow:
    0 24px 48px rgba(4, 20, 15, 0.34),
    inset 0 0 0 1px rgba(232, 194, 122, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  /* 龙卡龙标电影卡：深绿 + 金色胶片 */
  background:
    repeating-linear-gradient(90deg, rgba(232, 194, 122, 0.06) 0 1px, rgba(232, 194, 122, 0) 1px 18px),
    radial-gradient(90% 120% at 88% -10%, rgba(232, 194, 122, 0.16) 0%, rgba(232, 194, 122, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #06251c 0%, #0f4a37 55%, #041a13 100%);
}
.balance-card--bocom {
  --card-ink: #1f2937;
  --card-muted: #696565;
  --card-line: rgba(26, 41, 41, 0.1);
  --card-chip: rgba(255, 255, 255, 0.6);
  --card-eye: #4b5563;
  --card-sheen: rgba(255, 255, 255, 0.75);
  --card-sub: #0e7490;
  --card-border: rgba(255, 255, 255, 0.9);
  --card-edge: linear-gradient(90deg, #0e7490 0%, #38bdf8 55%, #f4ebdd 100%);
  --card-shadow:
    0 24px 48px rgba(52, 44, 30, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  /* 海南文旅：沙滩米 + 海水蓝 */
  background:
    radial-gradient(80% 120% at 90% -10%, rgba(14, 116, 144, 0.1) 0%, rgba(14, 116, 144, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f6efe4 0%, #eadbc6 45%, #fbf7ef 70%, #e2d2ba 100%);
}
.balance-card--psbc {
  --card-ink: #353333;
  --card-muted: #696565;
  --card-line: rgba(26, 41, 41, 0.1);
  --card-chip: rgba(255, 255, 255, 0.65);
  --card-eye: #475467;
  --card-sheen: rgba(255, 255, 255, 0.8);
  --card-sub: #0e5a8a;
  --card-border: rgba(255, 255, 255, 0.9);
  --card-edge: linear-gradient(90deg, #0e5a8a 0%, #57a9d9 55%, #d7e3ea 100%);
  --card-shadow:
    0 24px 48px rgba(31, 45, 42, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  /* 中铁联名：银白 + 高铁蓝 */
  background:
    radial-gradient(80% 120% at 90% -10%, rgba(14, 90, 138, 0.08) 0%, rgba(14, 90, 138, 0) 55%),
    linear-gradient(115deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0) 30%),
    linear-gradient(135deg, #f7f9f8 0%, #e4ecef 45%, #fafcfd 70%, #dbe6eb 100%);
}

/* 联名款装饰 */
.card-globe {
  position: absolute;
  right: -54px;
  bottom: -72px;
  z-index: 0;
  width: 190px;
  height: 190px;
  border: 1px solid rgba(217, 181, 106, 0.16);
  border-radius: 50%;
  pointer-events: none;
}
.card-globe::before {
  content: '';
  position: absolute;
  inset: 24px 62px;
  border: 1px solid rgba(217, 181, 106, 0.14);
  border-radius: 50%;
}
.card-globe::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
  background: rgba(217, 181, 106, 0.12);
}
.card-wall {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  width: 100%;
  height: 104px;
  pointer-events: none;
  color: rgba(138, 106, 47, 0.26);
}
.card-wall path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
}
.card-wall__ridge {
  opacity: 0.65;
}
.card-wall__base,
.card-wall__tower {
  stroke-width: 1.1;
}
.card-mountains {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  color: rgba(15, 118, 110, 0.18);
}
.card-mountains path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
}
.card-waves {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  width: 100%;
  height: 70px;
  pointer-events: none;
  color: rgba(14, 116, 144, 0.2);
}
.card-waves path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
}
.card-rail {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  width: 100%;
  height: 90px;
  pointer-events: none;
  color: rgba(14, 90, 138, 0.2);
}
.card-rail path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1;
}

.balance-card__row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.balance-card__row--amount {
  justify-content: flex-start;
  gap: 10px;
  margin-top: 10px;
}
.balance-card__row--footer {
  margin-top: 16px;
}

.balance-card__label {
  font-size: 12px;
  letter-spacing: 0.3px;
  color: var(--card-muted);
}
.balance-card__brand {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.6px;
  color: var(--card-muted);
}
.brand-visa {
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  font-style: italic;
  font-weight: 700;
  letter-spacing: 0.4px;
  line-height: 1;
  color: var(--card-brand, var(--card-ink));
}
.brand-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  line-height: 1.05;
}
.brand-stack--end {
  align-items: flex-end;
}
.brand-sub {
  font-size: 7px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--card-sub, var(--card-muted));
  opacity: 0.9;
}
.brand-seal {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(232, 194, 122, 0.62);
  border-radius: 3px;
  font-family: 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: 11px;
  line-height: 1;
  color: #e8c27a;
}
.brand-boc {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #a6192e;
  color: #ffffff;
  font-family: 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: 11px;
  line-height: 1;
}
.brand-mc {
  display: flex;
  align-items: center;

  i {
    width: 13px;
    height: 13px;
    border-radius: 50%;
    background: #eb001b;
  }
  i + i {
    margin-left: -5px;
    background: #f79e1b;
  }
}
.brand-up {
  display: flex;
  align-items: center;
  gap: 2px;

  i {
    width: 3px;
    height: 12px;
    border-radius: 1px;
    transform: skewX(-12deg);
  }
  i:nth-child(1) {
    background: #e21836;
  }
  i:nth-child(2) {
    background: #00447c;
  }
  i:nth-child(3) {
    background: #007b84;
  }
}
.brand-bank {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.28);
}

.balance-card__currency {
  flex: none;
  height: 22px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--card-line);
  border-radius: 6px;
  background: var(--card-chip);
  font-size: 11px;
  color: var(--card-ink);
}
.balance-card__amount {
  font-size: 34px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -1px;
  color: var(--card-ink);
  font-variant-numeric: tabular-nums;
}

.balance-card__number {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}
.balance-card__group {
  font-size: 20px;
  letter-spacing: 1.5px;
  color: var(--card-muted);
}
.balance-card__last {
  font-size: 20px;
  letter-spacing: 1px;
  color: var(--card-ink);
}
.balance-card__full {
  font-size: 18px;
  letter-spacing: 1.2px;
  white-space: nowrap;
  color: var(--card-ink);
  font-variant-numeric: tabular-nums;
}
.balance-card__eye {
  flex: none;
  margin-left: auto;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 0;
  background: none;
  color: var(--card-eye);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 18px;
    height: 18px;
  }
}

.balance-card__holder {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: var(--card-ink);
}
.balance-card__expiry {
  font-size: 11px;
  letter-spacing: 1.2px;
  color: var(--card-muted);
}

/* ===== 圆点 ===== */
.dots {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 14px;
}
.dot {
  width: 6px;
  height: 3px;
  border-radius: 3px;
  background: #d7dcd9;
  cursor: pointer;
  transition: width 0.2s ease, background 0.2s ease;
}
.dot--active {
  width: 18px;
  background: #17b7bd;
}

/* ===== 本月概览 ===== */
.overview {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #f0eeef;
  border-radius: 16px;
  background: #ffffff;
}
.overview__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: #353333;
}
.overview__month {
  font-size: 11px;
  color: #afafaf;
}
.overview__metrics {
  display: flex;
  margin-top: 14px;
}
.overview__metric {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.overview__metric + .overview__metric {
  padding-left: 14px;
  border-left: 1px solid #f1f3f8;
}
.overview__label {
  font-size: 11px;
  color: #afafaf;
}
.overview__value {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: #353333;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.overview__value--income {
  color: #22c55e;
}

/* ===== 快捷操作：转账 + 收款 ===== */
.quick-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}
.quick-action {
  flex: 1;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid #f0eeef;
  border-radius: 14px;
  background: #ffffff;
  color: #353333;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.quick-action:active {
  transform: scale(0.98);
}
.quick-action--primary {
  border-color: #17b7bd;
  background: #17b7bd;
  color: #ffffff;
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.24);
}
.quick-action__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: #e9f9f5;
  color: #17b7bd;
}
.quick-action--primary .quick-action__icon {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}
.quick-action__icon svg {
  width: 18px;
  height: 18px;
}

/* ===== 最近交易 ===== */
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  margin-bottom: 10px;
}
.see-more {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 11px;
  color: #17b7bd;
  cursor: pointer;

  span {
    font-size: 14px;
    line-height: 1;
  }
}

.chips {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.chip {
  height: 28px;
  padding: 0 12px;
  border: 1px solid #f0eeef;
  border-radius: 999px;
  background: #ffffff;
  font-family: inherit;
  font-size: 12px;
  color: #696565;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;

  &.active {
    border-color: #17b7bd;
    background: #17b7bd;
    color: #ffffff;
  }
}

.tx-list {
  padding: 0 14px;
  border: 1px solid #f0eeef;
  border-radius: 16px;
  background: #ffffff;
  overflow: hidden;
}
.tx-empty {
  padding: 36px 0;
  text-align: center;
  color: #afafaf;
  font-size: 12px;

  p {
    margin: 0;
  }
}
.tx-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 0;
  border-top: 1px solid #f2f4f2;
  cursor: pointer;

  &:first-of-type {
    border-top: 0;
  }
}
.tx-row__body {
  flex: 1;
  min-width: 0;
}
.tx-row__title {
  font-size: 13.5px;
  font-weight: 500;
  color: #353333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tx-row__sub {
  margin-top: 2px;
  font-size: 11px;
  color: #afafaf;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tx-row__amount {
  flex: none;
  font-size: 13.5px;
  font-weight: 600;
  color: #b42318;
  font-variant-numeric: tabular-nums;

  &.pos {
    color: #22c55e;
  }
}

/* ===== 入场动画 ===== */
.balance-carousel,
.overview,
.quick-actions,
.list-head,
.chips,
.tx-list {
  animation: rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.overview { animation-delay: 0.04s; }
.quick-actions { animation-delay: 0.08s; }
.list-head { animation-delay: 0.12s; }
.chips { animation-delay: 0.14s; }
.tx-list { animation-delay: 0.16s; }

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .balance-carousel,
  .overview,
  .quick-actions,
  .list-head,
  .chips,
  .tx-list {
    animation: none;
  }
}
</style>
