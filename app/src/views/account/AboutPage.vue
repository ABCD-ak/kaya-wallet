<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'

const router = useRouter()

const openSection = ref<'terms' | 'privacy' | null>(null)

const APP_VERSION = '1.0.0'

const terms = [
  '卡雅为用户提供账户管理、转账汇款与卡片管理服务，请在合法合规的前提下使用。',
  '用户应妥善保管账户凭证，因个人原因导致的账户泄露与资金损失由用户自行承担。',
  '平台有权根据监管要求调整服务内容与转账额度，并通过站内消息通知用户。',
]

const privacy = [
  '我们仅收集提供服务所必需的信息，包括账户资料、交易记录与设备信息。',
  '未经你的授权，我们不会向第三方共享个人敏感信息，法律法规另有规定的除外。',
  '你可以随时在「我的账户」中查看、修改或删除个人资料与地址信息。',
]

function goBack() {
  router.back()
}

function toggleSection(section: 'terms' | 'privacy') {
  openSection.value = openSection.value === section ? null : section
}
</script>

<template>
  <PageLayout title="关于我们" @back="goBack">
    <div class="about-page">
      <section class="about-brand">
        <div class="about-brand__logo">K</div>
        <p class="about-brand__name">卡雅 Kaya</p>
        <p class="about-brand__slogan">让每一笔收支都清晰可见</p>
        <span class="about-brand__version">版本 {{ APP_VERSION }}</span>
      </section>

      <section class="about-card">
        <div class="about-row">
          <span class="about-row__label">应用版本</span>
          <span class="about-row__value">{{ APP_VERSION }}</span>
        </div>
        <a class="about-row" href="mailto:support@kaya.app">
          <span class="about-row__label">客服邮箱</span>
          <span class="about-row__value">support@kaya.app</span>
        </a>
        <RouterLink class="about-row" to="/official">
          <span class="about-row__label">官方网站</span>
          <span class="about-row__value">www.kaya.app ↗</span>
        </RouterLink>
      </section>

      <section class="about-card">
        <button type="button" class="about-row about-row--button" @click="toggleSection('terms')">
          <span class="about-row__label">用户协议</span>
          <svg
            class="about-row__arrow"
            :class="{ 'is-open': openSection === 'terms' }"
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
        <ul v-if="openSection === 'terms'" class="about-detail">
          <li v-for="line in terms" :key="line">{{ line }}</li>
        </ul>

        <button type="button" class="about-row about-row--button" @click="toggleSection('privacy')">
          <span class="about-row__label">隐私政策</span>
          <svg
            class="about-row__arrow"
            :class="{ 'is-open': openSection === 'privacy' }"
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
        <ul v-if="openSection === 'privacy'" class="about-detail">
          <li v-for="line in privacy" :key="line">{{ line }}</li>
        </ul>
      </section>

      <p class="about-copyright">© 2026 卡雅科技 保留所有权利</p>
    </div>
  </PageLayout>
</template>

<style scoped>
.about-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 24px;
}

.about-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 26px 16px 22px;
  border-radius: 16px;
  background: #e9f9f5;
}

.about-brand__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: #17b7bd;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
}

.about-brand__name {
  margin: 12px 0 0;
  font-size: 17px;
  font-weight: 700;
  color: #353333;
}

.about-brand__slogan {
  margin: 5px 0 0;
  font-size: 12px;
  color: #696565;
}

.about-brand__version {
  margin-top: 10px;
  padding: 2px 10px;
  border-radius: 10px;
  background: #ffffff;
  font-size: 11px;
  line-height: 16px;
  color: #17b7bd;
}

.about-card {
  padding: 4px 16px;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.about-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  background: none;
  text-align: left;
  text-decoration: none;
  cursor: default;
}

.about-card > .about-row:last-child,
.about-detail:last-child {
  border-bottom: 0;
}

.about-row--button {
  cursor: pointer;
}

.about-row__label {
  font-size: 14px;
  color: #353333;
}

.about-row__value {
  font-size: 12px;
  color: #afafaf;
}

.about-row__arrow {
  flex: none;
  width: 15px;
  height: 15px;
  transition: transform 0.2s ease;
}

.about-row__arrow.is-open {
  transform: rotate(90deg);
}

.about-detail {
  margin: 0;
  padding: 0 0 14px 18px;
  list-style: disc;
}

.about-detail li {
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 18px;
  color: #696565;
}

.about-copyright {
  margin: 4px 0 0;
  font-size: 11px;
  text-align: center;
  color: #afafaf;
}
</style>
