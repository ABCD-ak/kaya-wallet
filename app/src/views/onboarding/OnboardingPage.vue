<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import StatusBar from '@/components/StatusBar.vue'
import OnboardingSlide from './components/OnboardingSlide.vue'
import ProgressDots from './components/ProgressDots.vue'
import { onboardingSlides } from './slides'

const router = useRouter()
const current = ref(0)
const isLast = computed(() => current.value === onboardingSlides.length - 1)

function goTo(index: number) {
  current.value = Math.min(Math.max(index, 0), onboardingSlides.length - 1)
}

function next() {
  if (isLast.value) finish()
  else goTo(current.value + 1)
}

function prev() {
  goTo(current.value - 1)
}

/** 引导结束直接进入登录页（注册入口在登录页底部） */
function finish() {
  router.replace('/auth')
}

// 横向滑动翻页；滑动后 300ms 内忽略点击，避免误触
let touchStartX = 0
let touchStartY = 0
let didSwipe = false

function onTouchStart(event: TouchEvent) {
  const touch = event.touches[0]
  touchStartX = touch.clientX
  touchStartY = touch.clientY
}

function onTouchEnd(event: TouchEvent) {
  const touch = event.changedTouches[0]
  const deltaX = touch.clientX - touchStartX
  const deltaY = touch.clientY - touchStartY
  if (Math.abs(deltaX) <= 50 || Math.abs(deltaX) <= Math.abs(deltaY)) return

  didSwipe = true
  if (deltaX < 0) next()
  else prev()
  window.setTimeout(() => {
    didSwipe = false
  }, 300)
}

function onTap() {
  if (didSwipe) return
  next()
}
</script>

<template>
  <div class="onboarding">
    <StatusBar />

    <div class="onboarding__top">
      <el-button v-if="!isLast" class="onboarding__skip" text @click="finish">跳过</el-button>
    </div>

    <div
      class="onboarding__viewport"
      @click="onTap"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <div
        class="onboarding__track"
        :style="{ transform: `translate3d(-${current * 100}%, 0, 0)` }"
      >
        <OnboardingSlide v-for="slide in onboardingSlides" :key="slide.key" :slide="slide" />
      </div>
    </div>

    <ProgressDots :count="onboardingSlides.length" :current="current" @select="goTo" />

    <div class="onboarding__home"><span /></div>
  </div>
</template>

<style scoped>
.onboarding {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--app-bg, #f7f6f8);
  user-select: none;
  -webkit-user-select: none;
}

.onboarding__top {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  height: 53px;
  padding: 30px 30px 0;
}

.onboarding__skip.el-button {
  height: 23px;
  padding: 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 400;
  line-height: 23px;
  letter-spacing: -0.23px;
  color: #696565;
  opacity: 0.5;
  background: transparent;
}

.onboarding__skip.el-button:hover {
  color: #353333;
  opacity: 0.8;
  background: transparent;
}

.onboarding__viewport {
  flex-shrink: 0;
  overflow: hidden;
  touch-action: pan-y;
  cursor: pointer;
}

.onboarding__track {
  display: flex;
  width: 100%;
  transition: transform 0.45s cubic-bezier(0.22, 0.61, 0.36, 1);
  will-change: transform;
}

.onboarding__home {
  display: flex;
  flex-shrink: 0;
  justify-content: center;
  margin-top: auto;
  padding-bottom: calc(9px + env(safe-area-inset-bottom));
}

.onboarding__home span {
  width: 134px;
  height: 5px;
  border-radius: 100px;
  background: #d3cbcb;
}

@media (prefers-reduced-motion: reduce) {
  .onboarding__track {
    transition: none;
  }
}

@media (min-width: 600px) {
  .onboarding {
    max-width: 390px;
    margin: 0 auto;
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.12);
  }
}
</style>
