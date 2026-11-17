<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import { listTransactions } from '@/api/transactions'
import type { TransactionResponse } from '@/api/types'

const route = useRoute()
const router = useRouter()

// 从路由读取类别信息
const categoryId = computed<string>(() => {
  const p = route.params.categoryId
  return typeof p === 'string' ? p : ''
})
const categoryName = computed<string>(() => {
  const q = route.query.name
  return typeof q === 'string' ? q : '分类'
})
const categoryColor = computed<string>(() => {
  const q = route.query.color
  return typeof q === 'string' ? q : '#17b7bd'
})
const categoryDescription = computed<string>(() => {
  const q = route.query.description
  return typeof q === 'string' ? q : ''
})

// 预算来自 query（可选），默认为示例
const budgetAmount = computed<number>(() => {
  const q = route.query.budget
  const n = Number(q)
  return Number.isFinite(n) && n > 0 ? n : 250
})

// 状态
const loading = ref(true)
const txList = ref<TransactionResponse[]>([])

async function loadTransactions() {
  if (!categoryId.value) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const page = await listTransactions({
      categoryId: categoryId.value,
      pageSize: 50,
    })
    txList.value = page.items.sort(
      (a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime(),
    )
  } catch {
    txList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadTransactions)

function goBack() {
  router.back()
}

function openTransaction(id: string) {
  router.push(`/transactions/${id}`)
}

// ========== 计算属性 ==========
function txAmountNum(raw: string): number {
  const n = Number(raw)
  return Number.isFinite(n) ? n : 0
}

function isExpense(tx: TransactionResponse): boolean {
  return tx.direction === 'debit'
}

// 仅统计支出方向（通常分类详情用于支出分析）
const monthSpent = computed<number>(() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  return txList.value.reduce((sum, tx) => {
    if (!isExpense(tx)) return sum
    const d = new Date(tx.occurredAt)
    if (d.getFullYear() === y && d.getMonth() === m) {
      return sum + txAmountNum(tx.amount)
    }
    return sum
  }, 0)
})

const progressPercent = computed<number>(() => {
  if (budgetAmount.value <= 0) return 0
  const p = (monthSpent.value / budgetAmount.value) * 100
  return Math.min(100, Math.max(0, p))
})

const budgetStatus = computed<{ used: number; color: string }>(() => {
  if (progressPercent.value >= 100) return { used: 100, color: '#f26b6b' }
  if (progressPercent.value >= 80) return { used: progressPercent.value, color: '#f59e0b' }
  return { used: progressPercent.value, color: '#17b7bd' }
})

// ========== 格式化 ==========
function fmtMoney(n: number): string {
  return `¥ ${n.toFixed(2)}`
}

function fmtDate(iso: string): string {
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    const now = new Date()
    const diff = (now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24)
    if (diff < 1 && d.toDateString() === now.toDateString()) {
      const hh = String(d.getHours()).padStart(2, '0')
      const mm = String(d.getMinutes()).padStart(2, '0')
      return `今天 ${hh}:${mm}`
    }
    const yesterday = new Date(now.getTime() - 86400000)
    if (d.toDateString() === yesterday.toDateString()) return '昨天'
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${m}-${day}`
  } catch {
    return iso
  }
}

function txTitle(tx: TransactionResponse): string {
  if (tx.counterparty?.name) return tx.counterparty.name
  if (tx.note) return tx.note
  if (tx.type === 'transfer_in') return '转入'
  if (tx.type === 'transfer_out') return '转出'
  return isExpense(tx) ? '支出' : '收入'
}

function txDirectionLabel(tx: TransactionResponse): string {
  if (tx.type === 'transfer_out') return '转出'
  if (tx.type === 'transfer_in') return '转入'
  return isExpense(tx) ? '支出' : '收入'
}

function txAmountDisplay(tx: TransactionResponse): string {
  const sign = isExpense(tx) ? '-' : '+'
  const symbol = tx.currencyCode === 'CNY' ? '¥' : (tx.currencyCode === 'USD' ? '$' : tx.currencyCode ? `${tx.currencyCode} ` : '¥')
  return `${sign}${symbol}${tx.amount}`
}

const isEmpty = computed(() => !loading.value && txList.value.length === 0)
</script>

<template>
  <PageLayout :title="categoryName" @back="goBack">
    <div class="cat-detail">
      <!-- 类别头部 -->
      <section class="cat-header">
        <div
          class="cat-header__icon"
          :style="{ background: categoryColor }"
        >
          <svg
            v-if="categoryColor === '#8b5cf6'"
            viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
          >
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 1 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          <svg
            v-else
            viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="9" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <h2 class="cat-header__name">{{ categoryName }}</h2>
        <p v-if="categoryDescription" class="cat-header__desc">{{ categoryDescription }}</p>
      </section>

      <!-- 月度消费卡 -->
      <section v-if="!loading" class="budget-card">
        <div class="budget-card__head">
          <span class="budget-card__label">本月支出</span>
          <span class="budget-card__amount">{{ fmtMoney(monthSpent) }}</span>
        </div>

        <div class="budget-bar">
          <div
            class="budget-bar__fill"
            :style="{ width: `${progressPercent}%`, background: budgetStatus.color }"
          />
        </div>

        <div class="budget-card__foot">
          <span class="budget-card__budget">预算 {{ fmtMoney(budgetAmount) }}</span>
          <span class="budget-card__used" :style="{ color: budgetStatus.color }">
            已使用 {{ budgetStatus.used.toFixed(0) }}%
          </span>
        </div>
      </section>

      <!-- 骨架 -->
      <div v-if="loading" class="skeleton">
        <div class="skeleton__card" />
        <div v-for="i in 3" :key="i" class="skeleton__row" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="isEmpty" class="empty">
        <div class="empty__illu" aria-hidden="true">
          <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="60" cy="60" r="50" fill="#eef5f5" />
            <path d="M42 56h36M54 72h12" stroke="#17b7bd" stroke-width="3" stroke-linecap="round" />
            <circle cx="60" cy="38" r="4" fill="#17b7bd" />
            <rect x="32" y="44" width="56" height="44" rx="10" fill="#fff" stroke="#d8d4d4" stroke-width="1.5" />
          </svg>
        </div>
        <p class="empty__title">{{ categoryName }}分类下暂无交易</p>
      </div>

      <!-- 交易列表 -->
      <section v-else class="tx-section">
        <h3 class="tx-section__title">最近交易</h3>
        <ul class="tx-list">
          <li
            v-for="tx in txList"
            :key="tx.id"
            class="tx-item"
            :class="{ 'tx-item--expense': isExpense(tx) }"
            @click="openTransaction(tx.id)"
          >
            <div class="tx-item__icon" :style="{ background: categoryColor + '22' }">
              <span class="tx-item__arrow">
                <svg v-if="isExpense(tx)" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#f26b6b" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 17h10M17 7L7 17" />
                </svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#1dbf71" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 7H7M7 7l10 10" />
                </svg>
              </span>
            </div>
            <div class="tx-item__body">
              <span class="tx-item__title">{{ txTitle(tx) }}</span>
              <span class="tx-item__sub">
                {{ txDirectionLabel(tx) }} · {{ fmtDate(tx.occurredAt) }}
              </span>
              <span v-if="tx.counterparty?.name && tx.counterparty.name !== txTitle(tx)" class="tx-item__cp">
                {{ tx.counterparty.name }}
              </span>
            </div>
            <span
              class="tx-item__amount"
              :class="{ 'tx-item__amount--expense': isExpense(tx) }"
            >
              {{ txAmountDisplay(tx) }}
            </span>
          </li>
        </ul>
      </section>
    </div>
  </PageLayout>
</template>

<style scoped>
.cat-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-bottom: 24px;
}

/* ========== Category Header ========== */
.cat-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 0;
  text-align: center;
}

.cat-header__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.cat-header__name {
  margin: 14px 0 2px;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #353333;
}

.cat-header__desc {
  margin: 0;
  font-size: 13px;
  color: #696565;
}

/* ========== Budget Card ========== */
.budget-card {
  padding: 18px 18px 16px;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.budget-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.budget-card__label {
  font-size: 13px;
  color: #696565;
}

.budget-card__amount {
  font-family: 'Poppins', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: #353333;
}

.budget-bar {
  position: relative;
  width: 100%;
  height: 8px;
  margin-top: 14px;
  border-radius: 999px;
  background: #f6f6f6;
  overflow: hidden;
}

.budget-bar__fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s ease, background 0.3s ease;
}

.budget-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.budget-card__budget {
  font-size: 12px;
  color: #696565;
}

.budget-card__used {
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  font-weight: 600;
}

/* ========== Skeleton ========== */
.skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton__card {
  height: 110px;
  border-radius: 12px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

.skeleton__row {
  height: 60px;
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
  padding: 32px 16px;
}

.empty__illu {
  width: 110px;
  height: 110px;
  margin-bottom: 14px;
}

.empty__title {
  margin: 0;
  font-size: 14px;
  color: #696565;
  text-align: center;
}

/* ========== Transaction List ========== */
.tx-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tx-section__title {
  margin: 2px 4px 6px;
  font-size: 14px;
  font-weight: 600;
  color: #696565;
}

.tx-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #fff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
  overflow: hidden;
}

.tx-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 60px;
  padding: 10px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: background 0.1s ease;
}

.tx-item:first-child {
  border-top: 0;
}

.tx-item:active {
  background: rgba(23, 183, 189, 0.04);
}

.tx-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
}

.tx-item__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.tx-item__title {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tx-item__sub {
  font-size: 12px;
  color: #afafaf;
}

.tx-item__cp {
  font-size: 11px;
  color: #afafaf;
}

.tx-item__amount {
  flex-shrink: 0;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #1dbf71;
}

.tx-item__amount--expense {
  color: #f26b6b;
}
</style>
