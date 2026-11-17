<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import StatusBar from '@/components/StatusBar.vue'
import BottomNav from '@/components/BottomNav.vue'
import {
  getUnreadCount,
  listNotifications,
  markAllAsRead,
  markAsRead,
  type NotificationResponse,
} from '@/api/notifications'

const router = useRouter()

// ========== 常量 ==========
/** 24 小时内的通知归入“最近” */
const RECENT_WINDOW_MS = 24 * 60 * 60 * 1000
/** “最近”最多展示条数 */
const RECENT_LIMIT = 5
/** 单页条数 */
const PAGE_SIZE = 30

// ========== 状态 ==========
const loading = ref(true)
const loadingMore = ref(false)
const loadError = ref(false)
const markingAll = ref(false)
const notifications = ref<NotificationResponse[]>([])
const unreadCount = ref(0)
const total = ref(0)

// ========== 分组 ==========
function timeOf(item: NotificationResponse): number {
  const t = Date.parse(item.createdAt)
  return Number.isNaN(t) ? 0 : t
}

const recent = computed(() => {
  const now = Date.now()
  return notifications.value
    .filter((item) => now - timeOf(item) <= RECENT_WINDOW_MS)
    .slice(0, RECENT_LIMIT)
})

const older = computed(() => {
  const recentIds = new Set(recent.value.map((item) => item.id))
  return notifications.value.filter((item) => !recentIds.has(item.id))
})

/** 渲染用分组：空组不展示 */
const groups = computed(() =>
  [
    { key: 'recent', title: '最近', items: recent.value },
    { key: 'older', title: '更早的通知', items: older.value },
  ].filter((group) => group.items.length > 0),
)

// ========== 图标 ==========
const iconMap: Record<string, string> = {
  transfer_received: '/assets/ic-share-e-r.png',
  transfer_sent: '/assets/ic-share-e-r.png',
  payment_received: '/assets/ic-invoice-payment-o-t.png',
  budget_alert: '/assets/ic-expense-g-s.png',
  income: '/assets/ic-income-f-u.png',
}

function iconOf(type: string): string {
  return iconMap[type] ?? ''
}

function textOf(item: NotificationResponse): string {
  return item.body || item.title || ''
}

// ========== 数据加载 ==========
async function load() {
  loading.value = true
  loadError.value = false
  try {
    const [page, unread] = await Promise.all([
      listNotifications({ page: 1, pageSize: PAGE_SIZE }),
      getUnreadCount(),
    ])
    notifications.value = page?.items ?? []
    total.value = page?.total ?? notifications.value.length
    unreadCount.value = unread?.count ?? 0
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 追加下一页 */
async function loadMore() {
  if (loadingMore.value || notifications.value.length >= total.value) return
  loadingMore.value = true
  try {
    const next = Math.floor(notifications.value.length / PAGE_SIZE) + 1
    const page = await listNotifications({ page: next, pageSize: PAGE_SIZE })
    const loaded = new Set(notifications.value.map((item) => item.id))
    notifications.value.push(...(page?.items ?? []).filter((item) => !loaded.has(item.id)))
    total.value = page?.total ?? notifications.value.length
  } finally {
    loadingMore.value = false
  }
}

// ========== 交互 ==========
function goBack() {
  if (window.history.state?.back) router.back()
  else router.push('/wallet')
}

/** 点开一条通知即标记已读，并跳转到关联的交易详情（若有） */
async function onOpen(item: NotificationResponse) {
  if (!item.isRead) {
    item.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    try {
      await markAsRead(item.id)
    } catch {
      item.isRead = false
      unreadCount.value += 1
    }
  }
  if (item.relatedTransactionId) {
    router.push(`/transactions/${item.relatedTransactionId}`)
  }
}

async function onMarkAll() {
  if (markingAll.value || unreadCount.value === 0) return
  markingAll.value = true
  try {
    const result = await markAllAsRead()
    notifications.value.forEach((item) => {
      item.isRead = true
    })
    unreadCount.value = 0
    ElMessage.success(
      result?.updatedCount ? `已将 ${result.updatedCount} 条通知标为已读` : '已全部标为已读',
    )
  } finally {
    markingAll.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="notif-page">
    <!-- ========== 头部 ========== -->
    <header class="notif-header">
      <StatusBar />
      <div class="notif-header__bar">
        <button class="notif-header__back" type="button" aria-label="返回" @click="goBack">
          <img src="/assets/ic-back-d-h.png" alt="" />
        </button>
        <h1 class="notif-header__title">消息通知</h1>
        <button
          v-if="unreadCount > 0"
          class="notif-header__action"
          type="button"
          :disabled="markingAll"
          @click="onMarkAll"
        >
          {{ markingAll ? '处理中…' : '全部已读' }}
        </button>
      </div>
    </header>

    <!-- ========== 内容区 ========== -->
    <main class="notif-main">
      <!-- 加载骨架 -->
      <section v-if="loading" class="notif-skeleton" aria-hidden="true">
        <span class="notif-skeleton__label" />
        <div v-for="i in 6" :key="i" class="notif-skeleton__row">
          <span class="notif-skeleton__icon" />
          <span class="notif-skeleton__lines">
            <span class="notif-skeleton__line" />
            <span class="notif-skeleton__line notif-skeleton__line--short" />
          </span>
        </div>
      </section>

      <!-- 加载失败 -->
      <section v-else-if="loadError" class="notif-state">
        <p class="notif-state__title">通知加载失败</p>
        <p class="notif-state__desc">请检查网络后重试</p>
        <button class="notif-state__btn" type="button" @click="load">重新加载</button>
      </section>

      <!-- 空状态 -->
      <section v-else-if="notifications.length === 0" class="notif-state">
        <span class="notif-state__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
        </span>
        <p class="notif-state__title">暂无通知</p>
        <p class="notif-state__desc">账户变动、支出提醒会出现在这里</p>
      </section>

      <template v-else>
        <template v-for="(group, index) in groups" :key="group.key">
          <div class="notif-group">
            <h2
              class="notif-section__title"
              :class="{ 'notif-section__title--older': index > 0 }"
            >
              {{ group.title }}
            </h2>
            <div class="notif-group__card">
              <button
                v-for="item in group.items"
                :key="item.id"
                type="button"
                class="notif-row"
                :class="{ 'notif-row--unread': !item.isRead }"
                @click="onOpen(item)"
              >
                <span class="notif-row__icon">
                  <img v-if="iconOf(item.type)" :src="iconOf(item.type)" alt="" />
                  <svg
                    v-else
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#fff"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  </svg>
                </span>
                <span class="notif-row__text">{{ textOf(item) }}</span>
                <span
                  class="notif-row__status"
                  :class="item.isRead ? 'notif-row__status--read' : 'notif-row__status--unread'"
                >
                  {{ item.isRead ? '已读' : '未读' }}
                </span>
              </button>
            </div>
          </div>
        </template>

        <!-- 分页 -->
        <div v-if="notifications.length < total" class="notif-more">
          <button class="notif-more__btn" type="button" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? '加载中…' : '加载更多' }}
          </button>
        </div>
      </template>
    </main>

    <BottomNav active="none" />
  </div>
</template>

<style scoped>
.notif-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--app-bg, #f7f6f8);
}

/* ========== 头部 ========== */
.notif-header {
  flex-shrink: 0;
  background: var(--app-bg, #f7f6f8);
}

.notif-header__bar {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 56px;
  padding: 0 20px;
}

.notif-header__back {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
}

.notif-header__back img {
  display: block;
  width: 32px;
  height: 32px;
  filter: brightness(0) opacity(0.8);
}

.notif-header__title {
  flex: 1;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  letter-spacing: -0.4px;
  color: var(--app-ink, #353333);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notif-header__action {
  flex-shrink: 0;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.04);
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: var(--app-ink, #353333);
  cursor: pointer;
}

.notif-header__action:disabled {
  opacity: 0.6;
  cursor: default;
}

/* ========== 列表 ========== */
.notif-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 20px 20px;
  background: var(--app-bg, #f7f6f8);
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.notif-main::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.notif-more {
  display: flex;
  justify-content: center;
  padding: 18px 0 24px;
}

.notif-more__btn {
  padding: 8px 20px;
  border: 1px solid #f0eeef;
  border-radius: 100px;
  background: #ffffff;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  color: #696565;
  cursor: pointer;
}

.notif-more__btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.notif-section__title {
  margin: 0;
  padding: 18px 2px 8px;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  letter-spacing: -0.1px;
  color: var(--app-muted, #696565);
}

.notif-section__title--older {
  padding-top: 18px;
  padding-bottom: 8px;
}

.notif-group__card {
  overflow: hidden;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 16px;
  background: #ffffff;
}

.notif-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  height: 72px;
  padding: 0 14px;
  border: 0;
  border-top: 1px solid var(--app-line-soft, #f6f6f6);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}

.notif-row:first-of-type {
  border-top: 0;
}

.notif-row--unread {
  background: var(--app-accent-soft, #e9f9f5);
}

.notif-row:active {
  background: #f6faf8;
}

.notif-row__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
}

.notif-row__icon img {
  display: block;
  width: 34px;
  height: 34px;
}

.notif-row__icon svg {
  width: 34px;
  height: 34px;
  padding: 8px;
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
}

.notif-row__text {
  flex: 1;
  min-width: 0;
  display: -webkit-box;
  overflow: hidden;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13.5px;
  font-weight: 400;
  line-height: 21px;
  letter-spacing: -0.2px;
  color: var(--app-ink, #353333);
  white-space: pre-line;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.notif-row__status {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 100px;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 11px;
  font-weight: 500;
  line-height: 16px;
}

.notif-row__status--unread {
  background: var(--app-accent-soft, #e9f9f5);
  color: var(--app-accent, #17b7bd);
}

.notif-row__status--read {
  background: #f4f6f4;
  color: var(--app-subtle, #afafaf);
}

/* ========== 状态（加载 / 空 / 错误） ========== */
.notif-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 96px 32px 0;
  text-align: center;
}

.notif-state__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  border-radius: 50%;
  background: var(--app-accent-soft, #e9f9f5);
  color: var(--app-accent, #17b7bd);
}

.notif-state__icon svg {
  width: 32px;
  height: 32px;
}

.notif-state__title {
  margin: 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
  color: #353333;
}

.notif-state__desc {
  margin: 6px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  line-height: 20px;
  color: #9a9494;
}

.notif-state__btn {
  margin-top: 20px;
  padding: 9px 22px;
  border: 0;
  border-radius: 100px;
  background: var(--app-primary, #17b7bd);
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  cursor: pointer;
}

/* ========== 骨架 ========== */
.notif-skeleton {
  padding-bottom: 20px;
}

.notif-skeleton__label {
  display: block;
  width: 48px;
  height: 14px;
  margin: 20px 20px 20px;
  border-radius: 4px;
  background: #f1eff1;
  animation: notif-pulse 1.2s ease-in-out infinite;
}

.notif-skeleton__row {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 74px;
  padding: 0 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.notif-skeleton__icon {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #f1eff1;
  animation: notif-pulse 1.2s ease-in-out infinite;
}

.notif-skeleton__lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notif-skeleton__line {
  height: 12px;
  border-radius: 4px;
  background: #f1eff1;
  animation: notif-pulse 1.2s ease-in-out infinite;
}

.notif-skeleton__line--short {
  width: 62%;
}

@keyframes notif-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}

@media (min-width: 600px) {
  .notif-page {
    max-width: 420px;
    margin: 0 auto;
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.12);
  }
}
</style>
