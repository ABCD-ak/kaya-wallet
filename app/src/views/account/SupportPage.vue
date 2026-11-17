<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'

const router = useRouter()

const openFaq = ref<number | null>(0)

const faqs = [
  {
    q: '转账多久能到账？',
    a: '同行转账通常实时到账；跨行转账一般在 2 小时内完成，节假日可能顺延。',
  },
  {
    q: '如何提升转账额度？',
    a: '在「我的账户 - 安全验证」中完成身份认证与手机认证后，额度会自动提升。',
  },
  {
    q: '卡片丢失了怎么办？',
    a: '请立即在「我的账户 - 银行卡」中冻结或注销该卡，并联系客服补办新卡。',
  },
  {
    q: '交易记录可以导出吗？',
    a: '目前支持按月份查看收支概览与明细，需要导出账单请联系客服获取。',
  },
]

function goBack() {
  router.back()
}

function toggleFaq(index: number) {
  openFaq.value = openFaq.value === index ? null : index
}

function onChat() {
  router.push('/support/chat')
}

async function onEmail() {
  await copyText('support@kaya.app', '客服邮箱已复制')
}

async function onPhone() {
  await copyText('400-800-1234', '客服热线已复制')
}

async function copyText(text: string, tip: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(tip)
  } catch {
    ElMessage.info(text)
  }
}
</script>

<template>
  <PageLayout title="联系客服" @back="goBack">
    <div class="support-page">
      <section class="support-hero">
        <div class="support-hero__avatar">K</div>
        <p class="support-hero__title">卡雅客服中心</p>
        <p class="support-hero__desc">服务时间：每天 9:00 - 22:00</p>
      </section>

      <section class="support-card">
        <button type="button" class="channel" @click="onChat">
          <span class="channel__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="#17b7bd" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
            </svg>
          </span>
          <span class="channel__body">
            <span class="channel__label">在线客服</span>
            <span class="channel__value">点击发起会话</span>
          </span>
          <svg class="channel__chevron" viewBox="0 0 24 24" fill="none" stroke="#c7c4cc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>

        <a class="channel" href="mailto:support@kaya.app" @click.prevent="onEmail">
          <span class="channel__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="#17b7bd" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="5" width="18" height="14" rx="2" />
              <path d="M3 7l9 6 9-6" />
            </svg>
          </span>
          <span class="channel__body">
            <span class="channel__label">客服邮箱</span>
            <span class="channel__value">support@kaya.app</span>
          </span>
          <svg class="channel__chevron" viewBox="0 0 24 24" fill="none" stroke="#c7c4cc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </a>

        <a class="channel" href="tel:4008001234" @click.prevent="onPhone">
          <span class="channel__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="#17b7bd" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.2a2 2 0 0 1 2.1-.5c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z" />
            </svg>
          </span>
          <span class="channel__body">
            <span class="channel__label">客服热线</span>
            <span class="channel__value">400-800-1234</span>
          </span>
          <svg class="channel__chevron" viewBox="0 0 24 24" fill="none" stroke="#c7c4cc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </a>
      </section>

      <section class="support-faq">
        <h3 class="support-faq__title">常见问题</h3>
        <ul class="support-faq__list">
          <li v-for="(item, index) in faqs" :key="item.q" class="faq-item">
            <button type="button" class="faq-item__head" @click="toggleFaq(index)">
              <span class="faq-item__q">{{ item.q }}</span>
              <svg
                class="faq-item__arrow"
                :class="{ 'is-open': openFaq === index }"
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
            <p v-if="openFaq === index" class="faq-item__a">{{ item.a }}</p>
          </li>
        </ul>
      </section>
    </div>
  </PageLayout>
</template>

<style scoped>
.support-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 24px;
}

.support-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 22px 16px;
  border-radius: 16px;
  background: #e9f9f5;
}

.support-hero__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #17b7bd;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
}

.support-hero__title {
  margin: 10px 0 0;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
}

.support-hero__desc {
  margin: 4px 0 0;
  font-size: 12px;
  color: #696565;
}

.support-card,
.support-faq {
  padding: 4px 16px;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.channel {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  background: none;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}

.channel:last-child {
  border-bottom: 0;
}

.channel__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #eef8f7;
}

.channel__icon svg {
  width: 18px;
  height: 18px;
}

.channel__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.channel__label {
  font-size: 14px;
  color: #353333;
}

.channel__value {
  font-size: 12px;
  color: #afafaf;
}

.channel__chevron {
  flex: none;
  width: 16px;
  height: 16px;
}

.support-faq {
  padding: 16px;
}

.support-faq__title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #353333;
}

.support-faq__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.faq-item {
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.faq-item:last-child {
  border-bottom: 0;
}

.faq-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  padding: 12px 0;
  border: 0;
  background: none;
  text-align: left;
  cursor: pointer;
}

.faq-item__q {
  font-size: 13px;
  color: #353333;
}

.faq-item__arrow {
  flex: none;
  width: 15px;
  height: 15px;
  transition: transform 0.2s ease;
}

.faq-item__arrow.is-open {
  transform: rotate(90deg);
}

.faq-item__a {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 18px;
  color: #696565;
}
</style>
