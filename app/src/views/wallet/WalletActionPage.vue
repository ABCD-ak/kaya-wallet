<script setup lang="ts">
import { useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import BottomNav from '@/components/BottomNav.vue'

const router = useRouter()

function goNewIncome() {
  router.push({ path: '/transactions/new', query: { type: 'income' } })
}

function goNewExpense() {
  router.push({ path: '/transactions/new', query: { type: 'expense' } })
}
</script>

<template>
  <div class="action-shell">
    <PageLayout title="" :showBack="false">
    <div class="action-page">
      <div class="action-title">
        <h2>添加记录</h2>
        <p>选择今天要记录的账目</p>
      </div>

      <!-- 收入卡片 -->
      <button class="action-card action-card--income" type="button" @click="goNewIncome">
        <span class="action-card__icon action-card__icon--income">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
        </span>
        <div class="action-card__body">
          <span class="action-card__title">记一笔收入</span>
          <span class="action-card__subtitle">记录每一笔收入</span>
        </div>
        <span class="action-card__arrow">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#17b7bd" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </span>
      </button>

      <!-- 支出卡片 -->
      <button class="action-card action-card--expense" type="button" @click="goNewExpense">
        <span class="action-card__icon action-card__icon--expense">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
        </span>
        <div class="action-card__body">
          <span class="action-card__title">记一笔支出</span>
          <span class="action-card__subtitle">记录每一笔支出</span>
        </div>
        <span class="action-card__arrow">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#17b7bd" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </span>
      </button>
    </div>
    </PageLayout>

    <BottomNav active="wallet" />
  </div>
</template>

<style scoped>
.action-shell {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-width: 390px;
  margin: 0 auto;
  overflow: hidden;
}
.action-shell :deep(.page-layout) {
  flex: 1;
  height: auto;
  min-height: 0;
}
.action-shell :deep(.page-layout__home) {
  display: none;
}

.action-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 0;
}

.action-title {
  padding: 4px 2px;
  text-align: left;
}

.action-title h2 {
  margin: 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 22px;
  font-weight: 700;
  line-height: 30px;
  letter-spacing: -0.3px;
  color: #353333;
}

.action-title p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #696565;
}

.action-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  height: 128px;
  margin: 0;
  padding: 0 20px;
  border: 1px solid #f0eeef;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: transform 0.12s ease, box-shadow 0.15s ease;
  overflow: hidden;
}

.action-card:active {
  transform: scale(0.985);
  box-shadow: 0 3px 12px rgba(26, 41, 41, 0.06);
}

.action-card--income {
  background: #ffffff;
}

.action-card--expense {
  background: #ffffff;
}

.action-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 50%;
}

.action-card__icon--income {
  background: #17b7bd;
  box-shadow: 0 6px 18px rgba(23, 183, 189, 0.18);
}

.action-card__icon--expense {
  background: #b42318;
  box-shadow: 0 6px 18px rgba(180, 35, 24, 0.18);
}

.action-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.action-card__title {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
}

.action-card__subtitle {
  font-size: 12px;
  color: #696565;
}

.action-card__arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e9f9f5;
}
</style>
