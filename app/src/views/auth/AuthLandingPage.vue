<script setup lang="ts">
import { useRouter } from 'vue-router'

import StatusBar from '@/components/StatusBar.vue'
import { authCopy } from './copy'

const router = useRouter()
</script>

<template>
  <div class="landing">
    <StatusBar />

    <!-- 品牌丝带：欢迎页斜穿上半屏 -->
    <svg class="ribbon" viewBox="0 0 390 420" fill="none" aria-hidden="true">
      <defs>
        <linearGradient
          id="kaya-ribbon-welcome"
          x1="0"
          y1="180"
          x2="390"
          y2="360"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stop-color="#17b7bd" />
          <stop offset="1" stop-color="#9deb93" />
        </linearGradient>
      </defs>
      <path
        class="ribbon__band ribbon__band--a"
        d="M-70 340 C 80 170, 240 150, 450 270"
        stroke="#17b7bd"
        stroke-opacity=".15"
        stroke-width="96"
        stroke-linecap="round"
      />
      <path
        class="ribbon__band ribbon__band--b"
        d="M-70 402 C 90 246, 250 224, 450 340"
        stroke="#7ee39b"
        stroke-opacity=".2"
        stroke-width="52"
        stroke-linecap="round"
      />
      <path
        class="ribbon__band ribbon__band--c"
        d="M-70 370 C 95 214, 250 194, 450 306"
        stroke="url(#kaya-ribbon-welcome)"
        stroke-width="2.8"
        stroke-linecap="round"
      />
      <circle cx="70" cy="332" r="3" fill="#17b7bd" fill-opacity=".55" />
      <circle cx="204" cy="248" r="2.4" fill="#3ec9b4" fill-opacity=".5" />
      <circle cx="320" cy="272" r="3" fill="#9deb93" fill-opacity=".85" />
    </svg>

    <span class="landing__wordmark">KAYA</span>

    <div class="landing__content">
      <div class="landing__brand">
        <div class="landing__mark"><span>K</span></div>
        <h1 class="landing__welcome">{{ authCopy.landing.welcome }}</h1>
        <p class="landing__tagline">{{ authCopy.landing.tagline }}</p>
      </div>

      <div class="landing__actions">
        <button
          class="landing__button landing__button--primary"
          type="button"
          @click="router.push('/login')"
        >
          登 录
        </button>
        <button
          class="landing__button landing__button--ghost"
          type="button"
          @click="router.push('/register')"
        >
          注 册
        </button>
      </div>

      <p class="landing__terms">{{ authCopy.landing.terms }}</p>
    </div>

    <div class="landing__home"><span /></div>
  </div>
</template>

<style scoped>
.landing {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background:
    radial-gradient(320px 260px at 100% 4%, rgba(23, 183, 189, 0.12), transparent 68%),
    radial-gradient(340px 300px at -4% 34%, rgba(157, 235, 147, 0.18), transparent 68%),
    var(--app-bg, #f7f6f8);
}

.ribbon {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
  width: 100%;
  height: 420px;
  pointer-events: none;
}

.landing :deep(.status-bar) {
  position: relative;
  z-index: 4;
  background: transparent;
}

.ribbon__band {
  transform-box: fill-box;
  transform-origin: center;
  will-change: transform;
}

.landing__wordmark {
  position: absolute;
  top: calc(64px + env(safe-area-inset-top));
  left: 30px;
  z-index: 3;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.36em;
  color: #a3aeab;
}

.landing__content {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 0 30px calc(46px + env(safe-area-inset-bottom));
  color: var(--app-ink, #1d2b29);
}

.landing__mark {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: linear-gradient(135deg, #12aeb4 0%, #17b7bd 42%, #8ee79a 100%);
  box-shadow: 0 14px 28px rgba(23, 183, 189, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.landing__mark span {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 26px;
  font-weight: 600;
  line-height: 1;
  color: #fff;
}

.landing__mark::after {
  content: '';
  position: absolute;
  inset: -8px;
  border: 1px solid rgba(23, 183, 189, 0.28);
  border-radius: 24px;
}

.landing__welcome {
  margin: 34px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 42px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: 0.08em;
}

.landing__tagline {
  margin: 16px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 400;
  line-height: 27px;
  letter-spacing: 0.04em;
  color: #7c8886;
  white-space: pre-line;
}

.landing__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 52px;
}

.landing__button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 54px;
  overflow: hidden;
  border-radius: 16px;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.16em;
  cursor: pointer;
  transition: transform 0.16s ease;
}

.landing__button:active {
  transform: scale(0.988);
}

.landing__button--primary {
  border: 0;
  color: #fff;
  background-image: linear-gradient(120deg, #12aeb4 0%, #17b7bd 44%, #7ee39b 100%);
  background-size: 180% 180%;
  box-shadow: 0 16px 30px rgba(23, 183, 189, 0.3);
}

.landing__button--primary::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(
    105deg,
    transparent 38%,
    rgba(255, 255, 255, 0.52) 50%,
    transparent 62%
  );
  background-size: 260% 100%;
  background-position: 132% 0;
}

.landing__button--ghost {
  border: 1px solid #e7ebea;
  background: #fff;
  color: var(--app-ink, #1d2b29);
  box-shadow: 0 6px 16px rgba(16, 40, 38, 0.04);
}

.landing__terms {
  margin: 18px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 11px;
  line-height: 20px;
  letter-spacing: 0.06em;
  color: var(--app-subtle, #b0b9b7);
  text-align: center;
}

.landing__home {
  position: absolute;
  bottom: calc(9px + env(safe-area-inset-bottom));
  left: 0;
  z-index: 3;
  display: flex;
  justify-content: center;
  width: 100%;
}

.landing__home span {
  width: 134px;
  height: 5px;
  border-radius: 100px;
  background: #d3cbcb;
}

/* 动效：只动丝带、品牌标记和主按钮 */
@keyframes ribbon-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(0deg);
  }
  50% {
    transform: translate3d(-14px, 12px, 0) rotate(-1.2deg);
  }
}

@keyframes mark-breathe {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.06);
    opacity: 0.25;
  }
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes flow {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes glow {
  0%,
  100% {
    box-shadow: 0 16px 30px rgba(23, 183, 189, 0.26);
  }
  50% {
    box-shadow: 0 20px 38px rgba(23, 183, 189, 0.4);
  }
}

@keyframes sheen {
  0% {
    background-position: 132% 0;
  }
  58%,
  100% {
    background-position: -132% 0;
  }
}

@media (prefers-reduced-motion: no-preference) {
  .ribbon__band--a {
    animation: ribbon-drift 13s ease-in-out infinite;
  }
  .ribbon__band--b {
    animation: ribbon-drift 17s ease-in-out infinite reverse;
  }
  .ribbon__band--c {
    animation: ribbon-drift 21s ease-in-out infinite;
  }
  .landing__mark::after {
    animation: mark-breathe 3.6s ease-in-out infinite;
  }
  .landing__mark {
    animation: rise-in 0.6s cubic-bezier(0.2, 1.4, 0.4, 1) both;
  }
  .landing__welcome {
    animation: rise-in 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) 0.1s both;
  }
  .landing__tagline {
    animation: rise-in 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) 0.18s both;
  }
  .landing__button--primary {
    animation: flow 7s ease-in-out infinite, glow 3.6s ease-in-out infinite;
  }
  .landing__button--primary::after {
    animation: sheen 3.8s ease-in-out infinite;
  }
  .landing__button--ghost,
  .landing__terms {
    animation: rise-in 0.5s cubic-bezier(0.2, 0.7, 0.2, 1) 0.26s both;
  }
}

@media (min-width: 600px) {
  .landing {
    max-width: 390px;
    margin: 0 auto;
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.2);
  }
}
</style>
