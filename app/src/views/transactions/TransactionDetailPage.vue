<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import { getTransaction } from '@/api/transactions'
import type { TransactionResponse } from '@/api/types'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const notFound = ref(false)
const errMsg = ref('')
const tx = ref<TransactionResponse | null>(null)

onMounted(async () => {
  const id = typeof route.params.id === 'string' ? route.params.id : ''
  if (!id) {
    notFound.value = true
    loading.value = false
    return
  }
  try {
    tx.value = await getTransaction(id)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : ''
    if (msg.includes('404') || msg.toLowerCase().includes('not found')) {
      notFound.value = true
    } else {
      errMsg.value = '加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
})

function goBack() {
  router.back()
}

// 方向徽章样式
const directionBadge = computed(() => {
  if (!tx.value) return { text: '', cls: '' }
  const t = tx.value.type
  const d = tx.value.direction
  if (d === 'credit') {
    if (t === 'transfer_in') return { text: '转入', cls: 'dir--blue' }
    return { text: '收入', cls: 'dir--green' }
  }
  if (t === 'transfer_out') return { text: '转出', cls: 'dir--orange' }
  return { text: '支出', cls: 'dir--red' }
})

// 金额显示
const amountDisplay = computed(() => {
  if (!tx.value) return ''
  const sign = tx.value.direction === 'credit' ? '+' : '-'
  const n = Number(tx.value.amount)
  const formatted = Number.isNaN(n) ? tx.value.amount : n.toFixed(2)
  return `${sign}${formatted}`
})

const amountClass = computed(() => {
  if (!tx.value) return ''
  return tx.value.direction === 'credit' ? 'amount--credit' : 'amount--debit'
})

// 手续费与合计扣款（转出交易含手续费）
const feeNum = computed(() => Number(tx.value?.feeAmount ?? 0) || 0)
const totalDisplay = computed(() => {
  if (!tx.value) return ''
  const sign = tx.value.direction === 'credit' ? '+' : '-'
  return `${sign}${(Number(tx.value.amount) + feeNum.value).toFixed(2)}`
})

// 日期格式化
function formatDate(dateStr: string): string {
  try {
    const d = new Date(dateStr)
    if (Number.isNaN(d.getTime())) return dateStr
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mm = String(d.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${day} ${hh}:${mm}`
  } catch {
    return dateStr
  }
}

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

// 状态徽章
const statusLabel = computed(() => {
  if (!tx.value) return ''
  const map: Record<string, string> = {
    pending: '处理中',
    success: '成功',
    failed: '失败',
    cancelled: '已取消',
  }
  return map[tx.value.status] ?? '未知状态'
})

const statusClass = computed(() => {
  if (!tx.value) return ''
  const s = tx.value.status
  if (s === 'success') return 'status--success'
  if (s === 'pending') return 'status--pending'
  if (s === 'failed') return 'status--failed'
  return 'status--cancelled'
})

function copyRefNo() {
  if (!tx.value?.referenceNo) return
  void navigator.clipboard
    .writeText(tx.value.referenceNo)
    .then(() => ElMessage.success('已复制参考号'))
    .catch(() => ElMessage.warning('复制失败'))
}
</script>

<template>
  <PageLayout title="交易详情" @back="goBack">
    <div class="detail">
      <!-- Loading Skeleton -->
      <div v-if="loading" class="skeleton">
        <div class="skeleton__badge" />
        <div class="skeleton__amount" />
        <div class="skeleton__row" />
        <div class="skeleton__row" />
        <div class="skeleton__row" />
        <div class="skeleton__row" />
      </div>

      <!-- Not Found -->
      <div v-else-if="notFound" class="state">
        <div class="state__icon">🔍</div>
        <p class="state__title">交易不存在</p>
        <p class="state__hint">该交易可能已被删除或链接无效</p>
        <button class="state__btn" type="button" @click="goBack">返回</button>
      </div>

      <!-- Error -->
      <div v-else-if="errMsg" class="state">
        <div class="state__icon">⚠️</div>
        <p class="state__title">加载失败</p>
        <p class="state__hint">{{ errMsg }}</p>
        <button class="state__btn" type="button" @click="goBack">返回</button>
      </div>

      <!-- Detail -->
      <div v-else-if="tx" class="card">
        <!-- 顶部徽章 -->
        <div class="top-row">
          <span class="dir-badge" :class="directionBadge.cls">
            {{ directionBadge.text }}
          </span>
          <span class="status-badge" :class="statusClass">
            {{ statusLabel }}
          </span>
        </div>

        <!-- 大金额 -->
        <div class="amount-wrap">
          <span class="amount" :class="amountClass">
            {{ amountDisplay }}
          </span>
          <span class="currency">{{ currencyLabel(tx.currencyCode) }}</span>
        </div>

        <!-- 分割线 -->
        <div class="divider" />

        <!-- 详情列表 -->
        <div class="info-list">
          <!-- 对方 -->
          <div v-if="tx.counterparty?.name" class="info-row">
            <span class="info-row__label">对方</span>
            <span class="info-row__value">
              {{ tx.counterparty.name }}
              <template v-if="tx.counterparty.accountNo">
                <span class="info-row__sub">· {{ tx.counterparty.accountNo }}</span>
              </template>
            </span>
          </div>

          <!-- 分类 -->
          <div class="info-row">
            <span class="info-row__label">分类</span>
            <span class="info-row__value">
              <span
                class="cat-dot"
                :style="{ background: tx.category?.color || '#d3cbcb' }"
              />
              {{ tx.category?.name || '未分类' }}
            </span>
          </div>

          <!-- 手续费 -->
          <div v-if="feeNum > 0" class="info-row">
            <span class="info-row__label">手续费</span>
            <span class="info-row__value">{{ feeNum.toFixed(2) }}</span>
          </div>

          <!-- 合计扣款（含手续费） -->
          <div v-if="feeNum > 0" class="info-row">
            <span class="info-row__label">合计扣款</span>
            <span class="info-row__value info-row__value--total">{{ totalDisplay }}</span>
          </div>

          <!-- 备注 -->
          <div v-if="tx.note" class="info-row">
            <span class="info-row__label">备注</span>
            <span class="info-row__value">{{ tx.note }}</span>
          </div>

          <!-- 发生时间 -->
          <div class="info-row">
            <span class="info-row__label">发生时间</span>
            <span class="info-row__value">{{ formatDate(tx.occurredAt) }}</span>
          </div>

          <!-- 参考号 -->
          <div class="info-row info-row--copy" @click="copyRefNo">
            <span class="info-row__label">参考号</span>
            <span class="info-row__value info-row__value--mono">
              {{ tx.referenceNo }}
              <span class="copy-hint">复制</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  </PageLayout>
</template>

<style scoped>
.detail {
  padding-bottom: 24px;
}

/* === Skeleton === */
.skeleton {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 0;
}

.skeleton__badge {
  width: 70px;
  height: 24px;
  border-radius: 12px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

.skeleton__amount {
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
  margin: 4px 0 8px;
}

.skeleton__row {
  height: 20px;
  border-radius: 6px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* === State === */
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 24px;
  text-align: center;
}

.state__icon {
  font-size: 52px;
  margin-bottom: 14px;
}

.state__title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: #353333;
}

.state__hint {
  margin: 0 0 20px;
  font-size: 13px;
  color: #696565;
}

.state__btn {
  padding: 10px 28px;
  border: 0;
  border-radius: 20px;
  background: #17b7bd;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}

/* === Card === */
.card {
  padding: 22px 20px;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #fff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.dir-badge,
.status-badge {
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
}

.dir--green { background: rgba(46, 204, 113, 0.12); color: #27ae60; }
.dir--red   { background: rgba(231, 76, 60, 0.12); color: #e74c3c; }
.dir--blue  { background: rgba(52, 152, 219, 0.12); color: #2980b9; }
.dir--orange{ background: rgba(230, 126, 34, 0.14); color: #d35400; }

.status--success  { background: rgba(46, 204, 113, 0.1); color: #27ae60; }
.status--pending  { background: rgba(241, 196, 15, 0.15); color: #b7791f; }
.status--failed   { background: rgba(231, 76, 60, 0.1); color: #e74c3c; }
.status--cancelled{ background: rgba(0, 0, 0, 0.08); color: #696565; }

.amount-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 6px 0 12px;
}

.amount {
  font-family: 'Poppins', sans-serif;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.amount--credit { color: #27ae60; }
.amount--debit  { color: #e74c3c; }

.currency {
  font-size: 13px;
  color: #696565;
}

.divider {
  height: 1px;
  margin: 8px 0 14px;
  background: rgba(0, 0, 0, 0.06);
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.info-row:last-child {
  border-bottom: 0;
}

.info-row--copy {
  cursor: pointer;
}

.info-row--copy:active {
  opacity: 0.7;
}

.info-row__label {
  flex-shrink: 0;
  font-size: 13px;
  color: #696565;
}

.info-row__value {
  text-align: right;
  font-size: 13px;
  color: #353333;
  display: flex;
  align-items: center;
  gap: 6px;
}

.info-row__sub {
  font-size: 12px;
  color: #afafaf;
}

.info-row__value--mono {
  font-family: 'Poppins', monospace;
  font-size: 12px;
  color: #696565;
}

.info-row__value--total {
  font-weight: 700;
  color: #17b7bd;
}

.cat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.copy-hint {
  font-size: 11px;
  color: #17b7bd;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(23, 183, 189, 0.08);
}
</style>
