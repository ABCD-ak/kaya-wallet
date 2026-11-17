<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import { listRecipients } from '@/api/recipients'
import type { RecipientResponse } from '@/api/types'

const router = useRouter()
const route = useRoute()

// ----- 状态 -----
const searchQuery = ref('')
const recipients = ref<RecipientResponse[]>([])
const loading = ref(false)
const selectedRecipientId = ref<string>('')

// ----- 派生 -----
const filteredRecipients = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return recipients.value
  return recipients.value.filter(
    (r) =>
      r.displayName.toLowerCase().includes(q) ||
      (r.accountNo ?? '').toLowerCase().includes(q) ||
      (r.bankName ?? '').toLowerCase().includes(q),
  )
})

const hasRecipients = computed(() => recipients.value.length > 0)

// ----- 头像颜色 (从调色板中选) -----
const AVATAR_COLORS = [
  ['#0b1d1b', '#134e45'],
  ['#17b7bd', '#0b5e57'],
  ['#334155', '#1e293b'],
  ['#7c6a4f', '#5c4d36'],
  ['#4b6b63', '#2d4a43'],
]

function getAvatarColor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) >>> 0
  const palette = AVATAR_COLORS[hash % AVATAR_COLORS.length]
  return `linear-gradient(135deg, ${palette[0]} 0%, ${palette[1]} 100%)`
}

function getInitial(name: string): string {
  const t = name.trim()
  return t ? t.slice(0, 1).toUpperCase() : '?'
}

// ----- 生命周期 -----
onMounted(async () => {
  loading.value = true
  try {
    recipients.value = await listRecipients()
    // 从 query 预选
    const pre = route.query.recipient
    if (typeof pre === 'string') selectedRecipientId.value = pre
  } catch {
    // 请求层已处理
  } finally {
    loading.value = false
  }
})

// ----- 操作 -----
function goBack() {
  router.back()
}

function selectRecipient(id: string) {
  selectedRecipientId.value = id
}

function goToAddRecipient() {
  router.push('/recipients/new')
}

function goToTransferNew() {
  if (!selectedRecipientId.value) return
  const selected = recipients.value.find((r) => r.id === selectedRecipientId.value)
  router.push({
    path: '/transfers/new-amount',
    query: {
      recipient: selectedRecipientId.value,
      recipientName: selected?.displayName ?? '',
    },
  })
}
</script>

<template>
  <PageLayout title="转账" @back="goBack">
    <div class="send-money">
      <!-- 搜索栏 -->
      <div class="search-wrap">
        <span class="search-wrap__icon">🔍</span>
        <input
          v-model="searchQuery"
          class="search-wrap__input"
          type="text"
          placeholder="搜索收款人"
        />
      </div>

      <!-- 有联系人时 -->
      <template v-if="hasRecipients">
        <!-- 横向滚动的 chips -->
        <div class="chips-title">最近联系人</div>
        <div class="chips-scroll" role="list">
          <button
            v-for="r in recipients"
            :key="r.id"
            type="button"
            class="chip"
            :class="{ 'chip--active': selectedRecipientId === r.id }"
            @click="selectRecipient(r.id)"
          >
            <span class="chip__avatar" :style="{ background: getAvatarColor(r.displayName) }">
              {{ getInitial(r.displayName) }}
            </span>
            <span class="chip__name">{{ r.displayName }}</span>
          </button>
        </div>

        <!-- 全量联系人 -->
        <div class="list-title">全部联系人</div>
        <div class="list-wrap">
          <button
            v-for="r in filteredRecipients"
            :key="r.id"
            type="button"
            class="row"
            @click="selectRecipient(r.id)"
          >
            <span class="row__avatar" :style="{ background: getAvatarColor(r.displayName) }">
              {{ getInitial(r.displayName) }}
            </span>
            <span class="row__body">
              <span class="row__name">{{ r.displayName }}</span>
              <span v-if="r.accountNo" class="row__sub">
                {{ r.bankName ? `${r.bankName} · ` : '' }}
                **** {{ r.accountNo.slice(-4) }}
              </span>
            </span>
          </button>

          <div v-if="filteredRecipients.length === 0" class="empty-filter">
            未找到与“{{ searchQuery }}”匹配的联系人
          </div>
        </div>

        <!-- 继续按钮 -->
        <button
          class="primary-btn"
          type="button"
          :disabled="!selectedRecipientId"
          @click="goToTransferNew"
        >
          继续
        </button>
      </template>

      <!-- 空状态 -->
      <template v-else-if="!loading">
        <div class="empty-state">
          <div class="empty-state__illus" aria-hidden>
            <svg width="120" height="120" viewBox="0 0 120 120" fill="none">
              <circle cx="60" cy="60" r="52" stroke="#dfe7e3" stroke-width="4" fill="none" />
              <path d="M42 60 h36 M60 42 v36" stroke="#17b7bd" stroke-width="4" stroke-linecap="round" />
            </svg>
          </div>
          <div class="empty-state__title">暂无收款人</div>
          <div class="empty-state__sub">
            添加第一位收款人后即可开始转账。
          </div>
          <button class="primary-btn" type="button" @click="goToAddRecipient">
            添加收款人
          </button>
        </div>
      </template>

      <!-- 底部 "Add recipient +" 快速入口 -->
      <button
        v-if="hasRecipients"
        type="button"
        class="footer-card"
        @click="goToAddRecipient"
      >
        <span class="footer-card__icon">＋</span>
        <span class="footer-card__text">添加收款人 ＋</span>
      </button>
    </div>
  </PageLayout>
</template>

<style scoped>
.send-money {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 24px;
}

/* 搜索栏 */
.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
  height: 46px;
  margin-bottom: 8px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid #f0eeef;
  background: #ffffff;
}
.search-wrap__icon {
  margin-right: 10px;
  font-size: 16px;
  opacity: 0.6;
}
.search-wrap__input {
  flex: 1;
  border: 0;
  outline: none;
  background: transparent;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 14px;
  color: #353333;
}
.search-wrap__input::placeholder {
  color: #afafaf;
  opacity: 0.8;
}

/* 标题 */
.chips-title,
.list-title {
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
  letter-spacing: -0.2px;
}

/* Chips 横向滚动 */
.chips-scroll {
  display: flex;
  gap: 14px;
  margin: 8px -20px 0;
  padding: 8px 20px 14px;
  overflow-x: auto;
  scrollbar-width: none;
}
.chips-scroll::-webkit-scrollbar {
  display: none;
}

.chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 0;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 0.15s;
}
.chip--active .chip__avatar {
  box-shadow: 0 0 0 3px rgba(23, 183, 189, 0.25);
}
.chip:active {
  transform: scale(0.95);
}
.chip__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
}
.chip__name {
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #353333;
  max-width: 64px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 列表 */
.list-wrap {
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #f0eeef;
  overflow: hidden;
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  height: 64px;
  padding: 0 14px;
  border: 0;
  border-top: 1px solid #f6f6f6;
  background: #ffffff;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}
.row:first-of-type {
  border-top: 0;
}
.row:active {
  background: #f6faf8;
}

.row__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
}
.row__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.row__name {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #353333;
  letter-spacing: -0.23px;
}
.row__sub {
  font-family: 'Poppins', sans-serif;
  font-size: 12px;
  font-weight: 400;
  color: #696565;
  letter-spacing: -0.2px;
}

.empty-filter {
  padding: 20px 16px;
  text-align: center;
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  color: #696565;
}

/* 主按钮 */
.primary-btn {
  width: 100%;
  height: 52px;
  margin-top: 8px;
  border: 0;
  border-radius: 14px;
  background: var(--app-primary, #17b7bd);
  font-family: 'Poppins', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: -0.2px;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.16);
  transition: opacity 0.15s;
}
.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* 底部 "Add recipient" 卡片 */
.footer-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 52px;
  margin-top: 8px;
  border: 1px solid #f0eeef;
  border-radius: 14px;
  background: #ffffff;
  cursor: pointer;
  transition: background 0.15s;
}
.footer-card:active {
  background: #f6faf8;
}
.footer-card__icon {
  font-size: 20px;
  color: #17b7bd;
  font-weight: 500;
}
.footer-card__text {
  font-family: 'Poppins', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #17b7bd;
  letter-spacing: -0.23px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 20px;
  text-align: center;
}
.empty-state__illus {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-state__title {
  font-family: 'Poppins', sans-serif;
  font-size: 17px;
  font-weight: 600;
  color: #353333;
  letter-spacing: -0.3px;
}
.empty-state__sub {
  font-family: 'Poppins', sans-serif;
  font-size: 13px;
  font-weight: 400;
  color: #696565;
  max-width: 260px;
}
</style>
