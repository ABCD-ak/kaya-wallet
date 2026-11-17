<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

withDefaults(
  defineProps<{
    showIcons?: boolean
    /** light：白底深色字；onGradient：透明底白字（用于渐变头部） */
    variant?: 'light' | 'onGradient'
  }>(),
  { showIcons: true, variant: 'light' },
)

const time = ref('9:41')
let timer: number | undefined

function updateTime() {
  const now = new Date()
  time.value = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 30_000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="status-bar" :class="`status-bar--${variant}`">
    <span class="status-bar__time">{{ time }}</span>
    <div v-if="showIcons" class="status-bar__icons" aria-hidden="true">
      <img class="status-bar__signal" src="/assets/cellular-connection-p-4.png" alt="" />
      <img class="status-bar__wifi" src="/assets/wifi-a-3.png" alt="" />
      <img class="status-bar__battery" src="/assets/battery-d-2.png" alt="" />
    </div>
  </div>
</template>

<style scoped>
.status-bar {
  position: relative;
  z-index: 2;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: calc(44px + env(safe-area-inset-top));
  padding: env(safe-area-inset-top) 14px 0 21px;
  background: var(--app-bg, #f7f6f8);
}

.status-bar__time {
  width: 54px;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 500;
  line-height: 23px;
  color: var(--app-ink, #353333);
  text-align: center;
}

/* 渐变头部：状态栏融入渐变，文字转白 */
.status-bar--onGradient {
  background: transparent;
}

.status-bar--onGradient .status-bar__time {
  color: #ffffff;
}

.status-bar--onGradient .status-bar__icons img {
  filter: none;
}

.status-bar__icons {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 状态栏图标为白色导出资源，浅色背景需反色 */
.status-bar__icons img {
  filter: brightness(0) opacity(0.72);
}

.status-bar__signal {
  width: 17px;
  height: 11px;
}

.status-bar__wifi {
  width: 15px;
  height: 11px;
}

.status-bar__battery {
  width: 25px;
  height: 12px;
}
</style>
