<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { listWallets } from '@/api/wallets'
import { listCards } from '@/api/cards'
import type { CardResponse } from '@/api/types'

const router = useRouter()
const auth = useAuthStore()

const qrDataUrl = ref('')
const amount = ref('100.00')

// 收款目标
type TargetKind = 'wallet' | 'card'
const targetKind = ref<TargetKind>('wallet')
const wallets = ref<{ id: string; balance: string }[]>([])
const cards = ref<CardResponse[]>([])
const selectedCardId = ref('')

const payeeName = computed(() => auth.user?.displayName || '卡雅用户')
const walletId = computed(() => wallets.value[0]?.id ?? '')

// 切换到银行卡时默认选第一张
watch(cards, (list) => {
  if (list.length && !selectedCardId.value) {
    selectedCardId.value = list[0].id
  }
}, { immediate: true })

// 生成收款二维码内容
const payUrl = computed(() => {
  const uid = auth.user?.id ?? ''
  const name = encodeURIComponent(payeeName.value)
  const amt = amount.value
  if (targetKind.value === 'wallet') {
    return `kaya://pay?to=${uid}&name=${name}&amount=${amt}&target=wallet&walletId=${walletId.value}`
  } else {
    const cid = selectedCardId.value
    return `kaya://pay?to=${uid}&name=${name}&amount=${amt}&target=card&cardId=${cid}`
  }
})

async function generateQR() {
  try {
    qrDataUrl.value = await QRCode.toDataURL(payUrl.value, {
      width: 320,
      margin: 2,
      color: { dark: '#1a1a1a', light: '#ffffff' },
      errorCorrectionLevel: 'M',
    })
  } catch {
    ElMessage.error('二维码生成失败')
  }
}

function saveQR() {
  if (!qrDataUrl.value) return
  const a = document.createElement('a')
  a.href = qrDataUrl.value
  a.download = `卡雅收款码_${payeeName.value}.png`
  a.click()
  ElMessage.success('二维码已保存')
}

const brandNameMap: Record<string, string> = {
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
function brandName(b: string) {
  return brandNameMap[b] ?? b.toUpperCase()
}

async function loadData() {
  try {
    const [ws, cs] = await Promise.all([listWallets(), listCards()])
    wallets.value = ws.map(w => ({ id: w.id, balance: w.balance }))
    cards.value = cs.filter(c => c.status === 'active')
  } catch {
    /* 静默 */
  }
}

onMounted(async () => {
  await loadData()
  await generateQR()
})

// 任何影响二维码的状态变化都刷新
watch([targetKind, selectedCardId, amount], generateQR)
</script>

<template>
  <PageLayout title="收款" @back="router.back()">
    <div class="receive-page">
      <!-- 收款码卡片 -->
      <div class="qr-card">
        <div class="qr-card__payee">
          <span class="qr-card__name">{{ payeeName }}</span>
          <span class="qr-card__tag">向我付款</span>
        </div>

        <div class="qr-wrap">
          <img v-if="qrDataUrl" :src="qrDataUrl" alt="收款二维码" class="qr-img" />
          <div v-else class="qr-placeholder">二维码生成中...</div>
        </div>

        <div class="qr-card__amount">
          <span class="amount__symbol">¥</span>
          <input
            v-model="amount"
            type="text"
            class="amount__input"
            inputmode="decimal"
          />
        </div>

        <!-- 收款到选择 -->
        <div class="target-section">
          <div class="target-section__label">收款到</div>
          <div class="target-toggle">
            <button
              class="target-toggle__btn"
              :class="{ active: targetKind === 'wallet' }"
              type="button"
              @click="targetKind = 'wallet'"
            >
              <svg v-if="targetKind === 'wallet'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" width="14" height="14"><polyline points="20 6 9 17 4 12"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14"><circle cx="12" cy="12" r="9"/></svg>
              零钱
            </button>
            <button
              class="target-toggle__btn"
              :class="{ active: targetKind === 'card' }"
              type="button"
              :disabled="!cards.length"
              @click="targetKind = 'card'"
            >
              <svg v-if="targetKind === 'card'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" width="14" height="14"><polyline points="20 6 9 17 4 12"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14"><circle cx="12" cy="12" r="9"/></svg>
              银行卡
            </button>
          </div>

          <!-- 零钱信息 -->
          <div v-if="targetKind === 'wallet'" class="target-info">
            <div class="target-info__icon target-info__icon--wallet">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
            </div>
            <div class="target-info__main">
              <div class="target-info__title">钱包零钱</div>
              <div class="target-info__sub">可用余额 ¥{{ wallets[0]?.balance ?? '0.00' }}</div>
            </div>
          </div>

          <!-- 银行卡列表 -->
          <div v-else-if="cards.length" class="card-list">
            <button
              v-for="c in cards"
              :key="c.id"
              class="card-item"
              :class="{ active: selectedCardId === c.id }"
              type="button"
              @click="selectedCardId = c.id"
            >
              <span class="card-item__brand">{{ brandName(c.brand) }}</span>
              <span class="card-item__num">**** {{ c.last4 }}</span>
              <span class="card-item__check">
                <svg v-if="selectedCardId === c.id" viewBox="0 0 24 24" fill="none" stroke="#17b7bd" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" width="16" height="16"><polyline points="20 6 9 17 4 12"/></svg>
              </span>
            </button>
          </div>

          <div v-else class="target-info target-info--empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            暂无银行卡，去「我的银行卡」绑定
          </div>
        </div>
      </div>

      <p class="qr-tip">请使用卡雅扫描二维码</p>

      <!-- 操作按钮 -->
      <button class="save-btn" type="button" @click="saveQR">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="7 10 12 15 17 10" />
          <line x1="12" y1="15" x2="12" y2="3" />
        </svg>
        保存二维码
      </button>
    </div>
  </PageLayout>
</template>

<style scoped>
.receive-page {
  min-height: calc(100vh - 56px);
  padding: 24px 20px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #f7f6f8;
}

.qr-card {
  width: 100%;
  max-width: 360px;
  background: #ffffff;
  border: 1px solid #f0eeef;
  border-radius: 20px;
  padding: 28px 24px 24px;
  box-shadow: 0 8px 30px rgba(26, 41, 41, 0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-card__payee {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.qr-card__name { font-size: 17px; font-weight: 700; color: #1a1a1a; }
.qr-card__tag {
  font-size: 12px; color: #17b7bd;
  background: rgba(23, 183, 189, 0.1);
  padding: 3px 10px; border-radius: 12px; font-weight: 600;
}

.qr-wrap {
  width: 220px; height: 220px;
  background: #fff; border: 1px solid #eef0f0; border-radius: 16px;
  display: flex; align-items: center; justify-content: center; padding: 10px;
}
.qr-img { width: 100%; height: 100%; object-fit: contain; }
.qr-placeholder { color: #999; font-size: 14px; }

.qr-card__amount { display: flex; align-items: baseline; gap: 4px; margin-top: 16px; }
.amount__symbol { font-size: 22px; font-weight: 700; color: #1a1a1a; }
.amount__input {
  font-size: 36px; font-weight: 800; color: #1a1a1a;
  border: none; outline: none; width: 180px; text-align: center;
  background: transparent; font-family: 'Poppins', 'PingFang SC', sans-serif;
}

/* 收款到选择 */
.target-section {
  width: 100%;
  margin-top: 20px;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
.target-section__label {
  font-size: 13px; color: #999; margin-bottom: 10px; font-weight: 500;
}
.target-toggle {
  display: flex; gap: 8px; margin-bottom: 12px;
}
.target-toggle__btn {
  flex: 1;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px;
  border: 1px solid #e0e0e0; border-radius: 10px;
  background: #fafafa; color: #696565;
  font-size: 14px; font-weight: 500;
  cursor: pointer; transition: all 0.15s ease;
}
.target-toggle__btn.active {
  border-color: #17b7bd; background: rgba(23, 183, 189, 0.08);
  color: #17b7bd; font-weight: 700;
}
.target-toggle__btn:disabled { opacity: 0.5; cursor: not-allowed; }

.target-info {
  display: flex; align-items: center; gap: 12px;
  padding: 12px; background: #fafafa; border-radius: 10px;
}
.target-info__icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(23, 183, 189, 0.12); color: #17b7bd; flex-shrink: 0;
}
.target-info__main { flex: 1; min-width: 0; }
.target-info__title { font-size: 14px; font-weight: 600; color: #1a1a1a; }
.target-info__sub { font-size: 12px; color: #999; margin-top: 2px; }
.target-info--empty { color: #999; font-size: 13px; gap: 8px; justify-content: center; }

/* 银行卡列表 */
.card-list { display: flex; flex-direction: column; gap: 8px; }
.card-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px;
  background: #fafafa; border: 1.5px solid transparent;
  border-radius: 12px;
  cursor: pointer; transition: all 0.15s ease;
}
.card-item.active {
  border-color: #17b7bd;
  background: rgba(23, 183, 189, 0.08);
}
.card-item__brand {
  font-size: 11px; font-weight: 700; color: #fff;
  background: #17b7bd; padding: 3px 8px; border-radius: 4px;
  letter-spacing: 0.5px;
}
.card-item__num { flex: 1; font-size: 14px; color: #353333; font-weight: 600; }
.card-item__check { flex-shrink: 0; }

.qr-tip { margin-top: 16px; font-size: 13px; color: #999; }

.save-btn {
  margin-top: 20px;
  width: 100%; max-width: 360px; height: 52px;
  border: none; border-radius: 14px;
  background: var(--app-primary, #17b7bd);
  color: #fff; font-size: 15px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.16);
  transition: transform 0.15s ease;
}
.save-btn:active { transform: scale(0.98); }
</style>
