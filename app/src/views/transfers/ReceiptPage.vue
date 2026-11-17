<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import { getTransfer } from '@/api/transfers'
import request from '@/utils/request'
import type { TransferResponse } from '@/api/types'

const router = useRouter()
const route = useRoute()

// ----- 状态 -----
const transfer = ref<TransferResponse | null>(null)
const loading = ref(true)
const downloading = ref(false)
const error = ref<string>('')

const transferId = computed(() => route.params.id as string)

// ----- 辅助 -----
function goBack() {
  router.back()
}

function goToHome() {
  router.push('/wallet')
}

function formatMoney(val: string | undefined): string {
  if (!val) return '0.00'
  const n = Number(val)
  if (!Number.isFinite(n)) return val
  return n.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

// 币种符号：人民币 ¥、美元 $，其他币种显示代码
const currencySymbol = computed(() => {
  const code = transfer.value?.currencyCode ?? ''
  if (code === 'CNY') return '¥ '
  if (code === 'USD') return '$ '
  return code ? `${code} ` : ''
})

function formatDateTime(iso: string | undefined): string {
  if (!iso) return '—'
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    const datePart = d.toLocaleDateString('zh-CN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
    const timePart = d.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
    return `${datePart} ${timePart}`
  } catch {
    return iso
  }
}

async function downloadReceipt() {
  if (!transferId.value || downloading.value) return
  downloading.value = true
  try {
    const blob = await request.download(`/transactions/${transferId.value}/receipt/download`)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `回执-${transfer.value?.referenceNo || transferId.value}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    // 请求层已显示后端错误
  } finally {
    downloading.value = false
  }
}

// ----- 生命周期 -----
onMounted(async () => {
  if (!transferId.value) {
    error.value = '缺少转账编号'
    loading.value = false
    return
  }
  loading.value = true
  try {
    transfer.value = await getTransfer(transferId.value)
  } catch {
    error.value = '回执加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <PageLayout title="支付回执" @back="goBack">
    <div class="receipt-wrap">
      <!-- 加载骨架 -->
      <template v-if="loading">
        <div class="card card--skeleton">
          <div class="sk-circle" />
          <div class="sk-line sk-line--lg" />
          <div class="sk-line" />
          <div class="sk-line sk-line--sm" />
          <div class="sk-line" />
          <div class="sk-line sk-line--lg" />
        </div>
      </template>

      <!-- 错误 -->
      <template v-else-if="error || !transfer">
        <div class="error-state">
          <div class="error-state__title">回执加载失败</div>
          <div class="error-state__sub">{{ error }}</div>
          <button class="primary-btn" type="button" @click="goToHome">完成</button>
        </div>
      </template>

      <!-- 正常内容 -->
      <template v-else>
        <div class="card">
          <!-- 成功图标 -->
          <div class="check-circle">
            <svg width="64" height="64" viewBox="0 0 72 72" fill="none">
              <path
                d="M20 37.5 L30.5 48 L52 26.5"
                stroke="#ffffff"
                stroke-width="6"
                stroke-linecap="round"
                stroke-linejoin="round"
                fill="none"
              />
            </svg>
          </div>

          <!-- Payment successful -->
          <div class="headline">支付成功</div>

          <!-- 引用号 -->
          <div class="ref-no">{{ transfer.referenceNo }}</div>

          <!-- 大金额 -->
          <div class="amount-big">
            {{ currencySymbol }}
            {{ formatMoney(transfer.amount) }}
          </div>

          <!-- 分隔线 -->
          <div class="divider" />

          <!-- Breakdown -->
          <div class="breakdown">
            <div class="breakdown-row">
              <span class="breakdown-row__label">转账金额</span>
              <span class="breakdown-row__value">
                {{ currencySymbol }}{{ formatMoney(transfer.amount) }}
              </span>
            </div>
            <div class="breakdown-row">
              <span class="breakdown-row__label">手续费</span>
              <span class="breakdown-row__value">
                {{ currencySymbol }}{{ formatMoney(transfer.feeAmount) }}
              </span>
            </div>
            <div class="breakdown-row breakdown-row--total">
              <span class="breakdown-row__label">合计</span>
              <span class="breakdown-row__value">
                {{ currencySymbol }}{{ formatMoney(transfer.totalAmount) }}
              </span>
            </div>
            <div class="divider divider--soft" />
            <div class="breakdown-row">
              <span class="breakdown-row__label">收款人</span>
              <span class="breakdown-row__value breakdown-row__value--wrap">
                {{ transfer.recipient.name }}
                <span v-if="transfer.recipient.accountNo" class="breakdown-row__sub">
                  · {{ transfer.recipient.accountNo }}
                </span>
              </span>
            </div>
            <div class="breakdown-row">
              <span class="breakdown-row__label">时间</span>
              <span class="breakdown-row__value">
                {{ formatDateTime(transfer.occurredAt) }}
              </span>
            </div>
            <div class="breakdown-row">
              <span class="breakdown-row__label">状态</span>
              <span class="breakdown-row__value" :class="{ 'breakdown-row__value--ok': transfer.status === 'success' }">
                {{ transfer.status === 'success' ? '成功' : transfer.status === 'failed' ? '失败' : '处理中' }}
              </span>
            </div>
          </div>

          <!-- 下载按钮 -->
          <button class="download-btn" type="button" :disabled="downloading" @click="downloadReceipt">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            {{ downloading ? '下载中…' : '下载回执' }}
          </button>
        </div>

        <!-- Done 链接 -->
        <button class="done-link" type="button" @click="goToHome">完成</button>
      </template>
    </div>
  </PageLayout>
</template>

<style scoped>
.receipt-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 16px;
}

/* 卡片 */
.card {
  width: 100%;
  max-width: 315px;
  background: #ffffff;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 16px;
  padding: 28px 20px 24px;
  box-shadow: 0 4px 16px rgba(24, 39, 43, 0.04);
}

/* 加载骨架 */
.card--skeleton {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.sk-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f6f6f6 0%, #fafafa 50%, #f6f6f6 100%);
  background-size: 200% 100%;
  animation: sk-shimmer 1.4s infinite linear;
}
.sk-line {
  width: 80%;
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f6f6f6 0%, #fafafa 50%, #f6f6f6 100%);
  background-size: 200% 100%;
  animation: sk-shimmer 1.4s infinite linear;
}
.sk-line--lg { width: 60%; height: 24px; }
.sk-line--sm { width: 50%; height: 12px; }
@keyframes sk-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 20px;
  text-align: center;
}
.error-state__title {
  font-family: 'Poppins', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #353333;
}
.error-state__sub {
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: #696565;
  margin-bottom: 12px;
}
.primary-btn {
  width: 100%;
  max-width: 315px;
  height: 52px;
  border: 0;
  border-radius: 24px;
  background: #17b7bd;
  font-family: 'Poppins', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
}

/* 成功图标 */
.check-circle {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: #17b7bd;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  box-shadow: 0 6px 18px rgba(23, 183, 189, 0.25);
}

/* 标题 */
.headline {
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #353333;
  letter-spacing: -0.3px;
}

/* 引用号 */
.ref-no {
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #696565;
  margin: 4px 0;
  letter-spacing: -0.2px;
}

/* 大金额 */
.amount-big {
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: #353333;
  margin: 8px 0;
  letter-spacing: -0.5px;
}

/* 分隔线 */
.divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 12px 0 16px;
}
.divider--soft {
  margin: 12px 0;
}

/* Breakdown */
.breakdown {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.breakdown-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.breakdown-row__label {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: #696565;
  letter-spacing: -0.23px;
}
.breakdown-row__value {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #353333;
  text-align: right;
  letter-spacing: -0.23px;
}
.breakdown-row__value--wrap {
  max-width: 60%;
  word-break: break-word;
}
.breakdown-row__sub {
  font-size: 12px;
  color: #696565;
  font-weight: 400;
}
.breakdown-row--total .breakdown-row__label,
.breakdown-row--total .breakdown-row__value {
  font-weight: 700;
  font-size: 15px;
}
.breakdown-row__value--ok {
  color: #17b7bd;
  font-weight: 600;
}

/* 下载按钮 */
.download-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  margin-top: 20px;
  border: 1px solid #696565;
  border-radius: 24px;
  background: transparent;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: #353333;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.download-btn:active {
  background: rgba(23, 183, 189, 0.08);
  border-color: #17b7bd;
  color: #17b7bd;
}

/* Done 链接 */
.done-link {
  border: 0;
  background: transparent;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #696565;
  letter-spacing: -0.23px;
  cursor: pointer;
  padding: 16px;
  transition: color 0.15s;
}
.done-link:active {
  color: #17b7bd;
}
</style>
