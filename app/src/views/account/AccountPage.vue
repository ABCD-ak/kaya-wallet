<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import BottomNav from '@/components/BottomNav.vue'
import { useAuthStore } from '@/stores/auth'
import { listCards } from '@/api/cards'
import { getUnreadCount } from '@/api/notifications'
import { getPaymentPasswordStatus } from '@/api/users'

const router = useRouter()
const auth = useAuthStore()

const loggingOut = ref(false)
const cardCount = ref(0)
const payPwdEnabled = ref(false)
const unreadCount = ref(0)

onMounted(async () => {
  try {
    await auth.fetchMe()
  } catch {
    // ignore
  }
  try {
    cardCount.value = (await listCards()).length
  } catch {
    // 请求层已处理
  }
  try {
    payPwdEnabled.value = (await getPaymentPasswordStatus()).enabled
  } catch {
    // 请求层已处理
  }
  try {
    unreadCount.value = (await getUnreadCount()).count
  } catch {
    // 请求层已处理
  }
})

// ========== 派生 ==========
const initial = computed(() => {
  const name = auth.user?.displayName || auth.user?.email || '卡'
  return name.slice(0, 1).toUpperCase()
})

function goEditProfile() {
  router.push('/account/edit')
}

function goCards() {
  router.push('/cards')
}

function goAddresses() {
  router.push('/addresses')
}

function goNotifications() {
  router.push('/notifications')
}

function goVerifications() {
  router.push('/verifications')
}

function goPaymentPassword() {
  router.push('/account/payment-password')
}

function goSupport() {
  router.push('/support')
}

function goAbout() {
  router.push('/about')
}

async function onLogout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await auth.logout()
    ElMessage.success('已退出登录')
  } catch {
    auth.reset()
  } finally {
    loggingOut.value = false
    // 退出后回到登录/注册入口，而不是重走引导页
    await router.replace('/auth')
  }
}

// ========== 图标 SVG ==========
const iconTeal = '#17b7bd'
const iconLogout = '#ef4444'

const menuItems = computed(() => [
  {
    key: 'cards',
    label: '银行卡',
    subtitle: `已绑定 ${cardCount.value} 张银行卡`,
    color: iconTeal,
    onClick: goCards,
    svg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2.5" y="5.5" width="19" height="13" rx="2.2"/><path d="M2.5 10h19"/></svg>`,
  },
  {
    key: 'address',
    label: '地址管理',
    subtitle: '添加或移除你的地址',
    color: iconTeal,
    onClick: goAddresses,
    svg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`,
  },
  {
    key: 'notifications',
    label: '消息通知',
    subtitle: unreadCount.value > 0 ? `${unreadCount.value} 条未读消息` : '暂无未读消息',
    color: iconTeal,
    onClick: goNotifications,
    svg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>`,
  },
  {
    key: 'verifications',
    label: '安全验证',
    subtitle: '',
    color: iconTeal,
    onClick: goVerifications,
    svg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4"/><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
  },
  {
    key: 'payment-password',
    label: '支付密码',
    subtitle: payPwdEnabled.value ? '已设置，修改需验证旧密码' : '未设置，转账前需先设置',
    color: iconTeal,
    onClick: goPaymentPassword,
    svg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3.5" y="10.5" width="17" height="10" rx="2.2"/><path d="M7.5 10.5V7.8a4.5 4.5 0 0 1 9 0v2.7"/><circle cx="12" cy="15.5" r="1.4"/></svg>`,
  },
  {
    key: 'support',
    label: '联系客服',
    subtitle: '',
    color: iconTeal,
    onClick: goSupport,
    svg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>`,
  },
  {
    key: 'about',
    label: '关于我们',
    subtitle: '',
    color: iconTeal,
    onClick: goAbout,
    svg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`,
  },
])
</script>

<template>
  <div class="account-page">
    <PageLayout title="我的账户" :show-back="false">
      <section class="account-section">
        <!-- Profile row -->
        <button type="button" class="profile-row" @click="goEditProfile">
          <img
            v-if="auth.user?.avatarUrl"
            class="profile-row__avatar profile-row__avatar--img"
            :src="auth.user.avatarUrl"
            alt="头像"
          />
          <span v-else class="profile-row__avatar">{{ initial }}</span>
          <div class="profile-row__info">
            <span class="profile-row__name">{{ auth.user?.displayName || '卡雅用户' }}</span>
            <span class="profile-row__progress">
              <span class="profile-row__progress-fill" />
            </span>
          </div>
          <span class="profile-row__edit">编辑资料</span>
        </button>

        <!-- Menu list -->
        <button
          v-for="item in menuItems"
          :key="item.key"
          type="button"
          class="menu-row"
          @click="item.onClick"
        >
          <span
            class="menu-row__icon"
            :style="{ color: item.color }"
            v-html="item.svg"
          />
          <span class="menu-row__body">
            <span class="menu-row__label">{{ item.label }}</span>
            <span v-if="item.subtitle" class="menu-row__subtitle">{{ item.subtitle }}</span>
          </span>
          <span
            v-if="item.key === 'notifications' && unreadCount > 0"
            class="menu-row__badge"
          >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          <svg
            class="menu-row__chevron"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#c7c4cc"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>

        <!-- Logout -->
        <button
          type="button"
          class="menu-row menu-row--logout"
          :disabled="loggingOut"
          @click="onLogout"
        >
          <span
            class="menu-row__icon"
            :style="{ color: iconLogout }"
            v-html="`<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'/><polyline points='16 17 21 12 16 7'/><line x1='21' y1='12' x2='9' y2='12'/></svg>`"
          />
          <span class="menu-row__body">
            <span class="menu-row__label menu-row__label--logout">
              {{ loggingOut ? '退出中…' : '退出登录' }}
            </span>
          </span>
        </button>
      </section>
    </PageLayout>

    <BottomNav active="account" />
  </div>
</template>

<style scoped>
.account-page {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  max-width: 390px;
  margin: 0 auto;
  overflow: hidden;
}

/* ============ PageLayout ============ */
.account-page :deep(.page-layout) {
  flex: 1;
  height: auto;
  min-height: 0;
}

.account-page :deep(.page-layout__home) {
  display: none;
}

.account-page :deep(.page-layout__main) {
  padding: 8px 20px 96px;
  background: transparent;
}

/* ============ 内容区 ============ */
.account-section {
  overflow: hidden;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 16px;
  background: #ffffff;
}

/* ============ Profile row ============ */
.profile-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  height: 76px;
  padding: 0 16px;
  border: 0;
  background: #ffffff;
  border-top: 0;
  cursor: pointer;
  text-align: left;
}

.profile-row__avatar {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #17b7bd;
  box-shadow: 0 4px 12px rgba(23, 183, 189, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: none;
}

.profile-row__avatar--img {
  object-fit: cover;
}

.profile-row__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.profile-row__name {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 700;
  line-height: 22px;
  letter-spacing: -0.23px;
  color: #353333;
}

.profile-row__progress {
  display: block;
  width: 130px;
  height: 5px;
  border-radius: 4px;
  background: #f6f6f6;
  overflow: hidden;
}

.profile-row__progress-fill {
  display: block;
  width: 80px;
  height: 100%;
  border-radius: 4px;
  background: #17b7bd;
}

.profile-row__edit {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  font-weight: 400;
  line-height: 22px;
  letter-spacing: -0.2px;
  color: #d0d5dd;
}

/* ============ Menu rows ============ */
.menu-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  height: 64px;
  padding: 0 16px;
  border: 0;
  border-top: 1px solid var(--app-line-soft, #f6f6f6);
  background: #ffffff;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;
}

.menu-row:active {
  background: #f8faf9;
}

.menu-row__icon {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
}

.menu-row__icon::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.12;
}

.menu-row__icon :deep(svg) {
  position: relative;
  z-index: 1;
  width: 20px;
  height: 20px;
}

.menu-row__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.menu-row__label {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 400;
  line-height: 22px;
  letter-spacing: 0.23px;
  color: #353333;
}

.menu-row__label--logout {
  color: #ef4444;
}

.menu-row__subtitle {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  font-weight: 400;
  line-height: 22px;
  letter-spacing: -0.22px;
  color: #afafaf;
}

.menu-row__badge {
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  margin-right: 4px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
}

.menu-row__chevron {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
}

/* ============ Logout row ============ */
.menu-row--logout {
  border-bottom: 0;
}

.menu-row--logout:active {
  background: #fff5f5;
}

.menu-row--logout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
