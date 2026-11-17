<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'

const router = useRouter()
const route = useRoute()

// 从 route query 获取数据 (AddAmountPage push 过来的)
const transferId = computed(() => route.query.transferId as string | undefined)
const referenceNo = computed(() => route.query.referenceNo as string | undefined)
const amount = computed(() => route.query.amount as string | undefined)
const recipient = computed(() => route.query.recipient as string | undefined)

function goBack() {
  // 成功页不应该再 back，直接回主页
  router.replace('/wallet')
}

function goToReceipt() {
  if (transferId.value) {
    router.push(`/receipts/${transferId.value}`)
  }
}

function goToHome() {
  router.replace('/wallet')
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

function shortRef(ref: string | undefined): string {
  if (!ref) return '—'
  if (ref.length <= 16) return ref
  return ref.slice(0, 8) + '…' + ref.slice(-4)
}
</script>

<template>
  <PageLayout title="转账成功！" @back="goBack">
    <div class="money-sent">
      <!-- 成功插画 -->
      <div class="success-wrap">
        <div class="check-circle">
          <svg width="72" height="72" viewBox="0 0 72 72" fill="none">
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
      </div>

      <!-- 收款方 -->
      <div class="recipient-line" v-if="recipient">
        {{ recipient }}
      </div>

      <!-- 大金额 -->
      <div class="amount-big">
        $ {{ formatMoney(amount) }}
      </div>

      <!-- 副标题 -->
      <div class="subtitle" v-if="recipient && amount">
        你已向 {{ recipient }} 转账 $ {{ formatMoney(amount) }}
      </div>

      <!-- 引用号 -->
      <div class="ref-row" v-if="referenceNo">
        <span class="ref-row__label">交易单号</span>
        <span class="ref-row__value">{{ shortRef(referenceNo) }}</span>
      </div>

      <!-- 主按钮 See Receipt -->
      <button
        v-if="transferId"
        class="primary-btn primary-btn--outline"
        type="button"
        @click="goToReceipt"
      >
        查看回执
      </button>

      <!-- 次链接 -->
      <button class="home-link" type="button" @click="goToHome">
        返回首页
      </button>
    </div>
  </PageLayout>
</template>

<style scoped>
.money-sent {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 24px 0 16px;
}

/* 成功图标 */
.success-wrap {
  padding: 16px 0 24px;
}
.check-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--app-primary, #17b7bd);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 28px rgba(23, 183, 189, 0.2);
}

/* 收款方 */
.recipient-line {
  font-family: 'Poppins', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #353333;
  letter-spacing: -0.3px;
}

/* 大金额 */
.amount-big {
  font-family: 'Poppins', sans-serif;
  font-size: 28px;
  font-weight: 700;
  color: #353333;
  margin: 6px 0;
  letter-spacing: -0.5px;
}

/* 副标题 */
.subtitle {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: #696565;
  letter-spacing: -0.23px;
}

/* 引用号 */
.ref-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 315px;
  margin: 16px 0 24px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.05);
}
.ref-row__label {
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: #696565;
}
.ref-row__value {
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #696565;
  text-align: right;
  letter-spacing: -0.2px;
}

/* 主按钮 */
.primary-btn {
  width: 100%;
  max-width: 315px;
  height: 52px;
  margin: 4px 0 12px;
  border: 0;
  border-radius: 24px;
  background: #17b7bd;
  font-family: 'Poppins', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.27px;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(23, 183, 189, 0.3);
  transition: opacity 0.15s, transform 0.1s;
}
.primary-btn:active {
  transform: scale(0.98);
}

.primary-btn--outline {
  background: rgba(23, 183, 189, 0.08);
  color: #17b7bd;
  box-shadow: none;
  border: 1px solid rgba(23, 183, 189, 0.3);
}

/* 返回主页链接 */
.home-link {
  border: 0;
  background: transparent;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #696565;
  letter-spacing: -0.23px;
  cursor: pointer;
  padding: 8px 16px;
  transition: color 0.15s;
}
.home-link:active {
  color: #17b7bd;
}
</style>
