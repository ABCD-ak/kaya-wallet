<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import { listCards } from '@/api/cards'
import type { CardBrand, CardResponse } from '@/api/types'

const router = useRouter()

const loading = ref(true)
const cards = ref<CardResponse[]>([])
const activeIndex = ref(0)

async function loadCards() {
  loading.value = true
  try {
    const all = await listCards()
    // 只展示信用卡，没绑定时 all 为空
    cards.value = all
      .filter((c) => c.cardType === 'credit' || c.cardType === 'debit')
      .sort((a, b) => Number(b.isPrimary) - Number(a.isPrimary))
  } catch {
    cards.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadCards)

function goBack() {
  router.back()
}

function goBindCard() {
  router.push('/cards')
}

function selectCard(index: number) {
  activeIndex.value = index
}

const activeCard = computed<CardResponse | null>(() => {
  if (cards.value.length === 0) return null
  const idx = Math.min(activeIndex.value, cards.value.length - 1)
  return cards.value[idx]
})

const brandPalette: Record<CardBrand, { label: string; accent: string; strip: string }> = {
  visa: { label: '威士', accent: '#1a1f71', strip: 'linear-gradient(90deg, #1a1f71 0%, #4b5bbf 100%)' },
  mastercard: { label: '万事达', accent: '#000', strip: 'linear-gradient(90deg, #eb001b 0%, #f79e1b 100%)' },
  unionpay: { label: '银联', accent: '#e21836', strip: 'linear-gradient(90deg, #e21836 0%, #ff6a7a 100%)' },
  icbc: { label: '中国工商银行', accent: '#c8161d', strip: 'linear-gradient(90deg, #c8161d 0%, #e76a6f 100%)' },
  abc: { label: '中国农业银行', accent: '#008042', strip: 'linear-gradient(90deg, #008042 0%, #4caf7c 100%)' },
  boc: { label: '中国银行', accent: '#b71d2a', strip: 'linear-gradient(90deg, #b71d2a 0%, #e76a75 100%)' },
  ccb: { label: '中国建设银行', accent: '#0066b3', strip: 'linear-gradient(90deg, #0066b3 0%, #4a9fd8 100%)' },
  bocom: { label: '交通银行', accent: '#005bac', strip: 'linear-gradient(90deg, #005bac 0%, #4a91d6 100%)' },
  psbc: { label: '中国邮政储蓄银行', accent: '#00703c', strip: 'linear-gradient(90deg, #00703c 0%, #47a977 100%)' },
}

function maskCard(last4: string): string {
  const padded = (last4 || '').padStart(4, '0')
  return `**** **** **** ${padded}`
}

function formatExpiry(mo: number, yr: number): string {
  return `${String(mo).padStart(2, '0')}/${String(yr).slice(-2)}`
}

function currencySymbol(code?: string): string {
  const map: Record<string, string> = { CNY: '¥', USD: '$', EUR: '€', JPY: '¥', GBP: '£', HKD: '港币 ' }
  if (!code) return '¥'
  return map[code] ?? `${code} `
}

function safeNumber(raw?: string): number {
  const n = Number(raw)
  return Number.isFinite(n) ? n : 0
}

const creditLimit = computed((): number | null => {
  if (!activeCard.value) return null
  if (activeCard.value.cardType !== 'credit') return null
  return safeNumber(activeCard.value.creditLimit)
})

const currentBalance = computed((): number => {
  return safeNumber(activeCard.value?.balance)
})

const availableCredit = computed((): number | null => {
  if (creditLimit.value === null) return null
  const avail = creditLimit.value - currentBalance.value
  return Math.max(0, avail)
})

function openCardDetail(id: string) {
  router.push(`/cards/${id}`)
}
</script>

<template>
  <PageLayout title="银行卡" @back="goBack">
    <div class="credit-page">
      <!-- 加载骨架 -->
      <div v-if="loading" class="skeleton">
        <div class="skeleton__card" />
        <div class="skeleton__rows" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="cards.length === 0" class="empty">
        <div class="empty__illu" aria-hidden="true">
          <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="18" y="34" width="84" height="56" rx="10" fill="#fff" stroke="#d8d4d4" stroke-width="1.5" />
            <rect x="18" y="50" width="84" height="10" fill="#f6f6f6" />
            <rect x="28" y="72" width="28" height="4" rx="2" fill="#e7e3e3" />
            <rect x="28" y="80" width="18" height="4" rx="2" fill="#efebeb" />
            <circle cx="60" cy="24" r="16" fill="rgba(23,183,189,0.14)" />
            <path d="M54 24h12M60 18v12" stroke="#17b7bd" stroke-width="2.4" stroke-linecap="round" />
          </svg>
        </div>
        <p class="empty__title">暂无银行卡</p>
        <button class="empty__link" type="button" @click="goBindCard">绑定新卡 →</button>
      </div>

      <!-- 有卡状态 -->
      <template v-else-if="activeCard">
        <!-- 信用卡视觉 -->
        <section
          class="credit-card"
          :style="{ '--brand-accent': brandPalette[activeCard.brand].accent }"
        >
          <div class="credit-card__brand">
            <span class="brand-label">{{ brandPalette[activeCard.brand].label }}</span>
          </div>

          <div class="credit-card__number">
            {{ maskCard(activeCard.last4) }}
          </div>

          <div class="credit-card__limit">
            <template v-if="activeCard.cardType === 'credit'">
              <span class="limit-text">信用额度</span>
              <span class="limit-value">
                {{ currencySymbol(activeCard.currencyCode) }}{{ creditLimit ?? 0 }}
              </span>
            </template>
            <template v-else>
              <span class="limit-text">账户余额</span>
              <span class="limit-value">
                {{ currencySymbol(activeCard.currencyCode) }}{{ activeCard.balance }}
              </span>
            </template>
          </div>

          <div class="credit-card__bottom">
            <div class="credit-card__holder">
              <span class="credit-card__meta">持卡人</span>
              <span class="credit-card__holder-name">{{ activeCard.holderName }}</span>
            </div>
            <div class="credit-card__expiry">
              <span class="credit-card__meta">有效期</span>
              <span class="credit-card__expiry-value">
                {{ formatExpiry(activeCard.expiryMonth, activeCard.expiryYear) }}
              </span>
            </div>
          </div>

          <div class="credit-card__strip" :style="{ background: brandPalette[activeCard.brand].strip }" />
        </section>

        <!-- 卡选择器（多张时显示） -->
        <div v-if="cards.length > 1" class="card-strip">
          <button
            v-for="(c, idx) in cards"
            :key="c.id"
            class="card-strip__thumb"
            :class="{ 'card-strip__thumb--active': idx === activeIndex }"
            type="button"
            @click="selectCard(idx)"
          >
            <span class="mini-brand" :style="{ background: brandPalette[c.brand].accent }">
              {{ brandPalette[c.brand].label.slice(0, 4) }}
            </span>
            <span class="mini-number">**** {{ c.last4 }}</span>
          </button>
        </div>

        <!-- 详情信息 -->
        <section class="info-card">
          <div v-if="activeCard.cardType === 'credit'" class="info-row" @click="openCardDetail(activeCard.id)">
            <span class="info-row__label">可用额度</span>
            <span class="info-row__value">
              {{ currencySymbol(activeCard.currencyCode) }}{{ availableCredit ?? 0 }}
            </span>
          </div>
          <div class="info-row" @click="openCardDetail(activeCard.id)">
            <span class="info-row__label">当前余额</span>
            <span class="info-row__value">{{ currencySymbol(activeCard.currencyCode) }}{{ activeCard.balance }}</span>
          </div>
          <div v-if="activeCard.cardType === 'credit'" class="info-row">
            <span class="info-row__label">账单周期</span>
            <span class="info-row__value">每月</span>
          </div>
          <div class="info-row info-row--status">
            <span class="info-row__label">状态</span>
            <span class="status-pill" :class="`status-pill--${activeCard.status}`">
              {{ activeCard.status === 'active' ? '正常' : activeCard.status === 'expired' ? '已过期' : activeCard.status === 'frozen' ? '已冻结' : '已注销' }}
            </span>
          </div>
        </section>

        <!-- 添加新卡 -->
        <button class="add-card-btn" type="button" @click="goBindCard">
          <span class="add-card-btn__icon">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </span>
          Add new card
        </button>
      </template>
    </div>
  </PageLayout>
</template>

<style scoped>
.credit-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-bottom: 24px;
}

/* ========== Skeleton ========== */
.skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton__card {
  width: 100%;
  height: 144px;
  border-radius: 12px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
  box-shadow: 0 4px 16px rgba(24, 39, 43, 0.06);
}

.skeleton__rows {
  height: 180px;
  border-radius: 12px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* ========== Empty ========== */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.empty__illu {
  width: 120px;
  height: 120px;
  margin-bottom: 14px;
}

.empty__title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 600;
  color: #353333;
}

.empty__link {
  padding: 0;
  border: 0;
  background: none;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #17b7bd;
  cursor: pointer;
}

/* ========== Credit Card Visual ========== */
.credit-card {
  position: relative;
  width: 100%;
  max-width: 325px;
  height: 144px;
  margin: 6px auto 0;
  padding: 16px 18px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(24, 39, 43, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.credit-card__strip {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0.9;
}

.credit-card__brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
}

.brand-label {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.6px;
  color: var(--brand-accent);
}

.credit-card__number {
  position: relative;
  z-index: 1;
  margin-top: auto;
  font-family: 'Poppins', monospace;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 3px;
  color: #353333;
}

.credit-card__limit {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}

.limit-text {
  font-size: 10px;
  font-weight: 500;
  color: #696565;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.limit-value {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #353333;
}

.credit-card__bottom {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 12px;
  padding-bottom: 4px;
}

.credit-card__holder,
.credit-card__expiry {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.credit-card__meta {
  font-size: 9px;
  font-weight: 600;
  color: #afafaf;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.credit-card__holder-name {
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  font-weight: 600;
  color: #353333;
  text-transform: uppercase;
}

.credit-card__expiry-value {
  font-family: 'Poppins', monospace;
  font-size: 12px;
  font-weight: 600;
  color: #353333;
}

/* ========== Card Strip (multiple cards) ========== */
.card-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 2px 4px;
  scrollbar-width: none;
}

.card-strip::-webkit-scrollbar {
  display: none;
}

.card-strip__thumb {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 92px;
  padding: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.card-strip__thumb--active {
  border-color: #17b7bd;
  box-shadow: 0 4px 14px rgba(23, 183, 189, 0.18);
}

.mini-brand {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  text-align: center;
  letter-spacing: 0.5px;
}

.mini-number {
  font-family: 'Poppins', monospace;
  font-size: 11px;
  font-weight: 500;
  color: #353333;
  letter-spacing: 1px;
}

/* ========== Info Card ========== */
.info-card {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(24, 39, 43, 0.05);
  overflow: hidden;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 62px;
  padding: 0 18px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: background 0.1s ease;
}

.info-row:first-child {
  border-top: 0;
}

.info-row:active {
  background: rgba(23, 183, 189, 0.04);
}

.info-row__label {
  font-size: 13px;
  color: #696565;
}

.info-row__value {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #353333;
}

.status-pill {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.status-pill--active  { background: rgba(46, 204, 113, 0.12); color: #27ae60; }
.status-pill--expired { background: rgba(231, 76, 60, 0.12); color: #e74c3c; }
.status-pill--frozen  { background: rgba(241, 196, 15, 0.15); color: #b7791f; }
.status-pill--removed { background: rgba(0, 0, 0, 0.08); color: #696565; }

/* ========== Add Card Button ========== */
.add-card-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  border: 2px dashed rgba(23, 183, 189, 0.5);
  border-radius: 12px;
  background: rgba(23, 183, 189, 0.06);
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #17b7bd;
  cursor: pointer;
  transition: background 0.15s ease;
}

.add-card-btn:active {
  background: rgba(23, 183, 189, 0.1);
}
</style>
