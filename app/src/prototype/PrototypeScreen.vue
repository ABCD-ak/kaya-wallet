<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ScreenRenderer from './ScreenRenderer.vue'
import { screens, screenOrder } from './screens'

const route = useRoute()
const router = useRouter()

const spec = computed(() => {
  const slug = String(route.params.slug ?? '')
  return screens[slug] ?? screens[screenOrder[0]]
})

const keypadValue = ref('')
watchEffect(() => {
  if (route.params.slug === 'add-amount' && !keypadValue.value) keypadValue.value = '150.00'
})

function onNavigate(to: string) {
  if (to.startsWith('keypad:')) {
    const k = to.slice(7)
    if (k === 'del') keypadValue.value = keypadValue.value.length > 1 ? keypadValue.value.slice(0, -1) : '0'
    else if (keypadValue.value.length < 9) keypadValue.value = keypadValue.value === '0' ? k : keypadValue.value + k
    return
  }
  keypadValue.value = ''
  router.push(to)
}

/** 返回上一屏；直接打开链接时回到引导页 */
function goBack() {
  if (window.history.state?.back) router.back()
  else router.replace('/onboarding')
}

// 375x812 按容器可视区域自适应缩放
const scale = ref(1)
const wrapRef = ref<HTMLElement | null>(null)
let resizeObserver: ResizeObserver | undefined

function fit(el: HTMLElement) {
  const pad = 32
  scale.value = Math.min((el.clientWidth - pad) / 375, (el.clientHeight - pad) / 812, 1)
}

onMounted(() => {
  const el = wrapRef.value
  if (!el) return
  fit(el)
  resizeObserver = new ResizeObserver(() => fit(el))
  resizeObserver.observe(el)
})

onBeforeUnmount(() => resizeObserver?.disconnect())

const sideIndex = computed(() => screenOrder.indexOf(spec.value.slug))
</script>

<template>
  <div class="proto-stage">
    <aside class="proto-side">
      <div class="proto-title">Kaya 原型</div>
      <div class="proto-actions">
        <button type="button" class="proto-action" @click="goBack">← 返回上一屏</button>
        <button type="button" class="proto-action" @click="router.push('/onboarding')">引导页（正式）</button>
      </div>
      <a
        v-for="(s, i) in screenOrder"
        :key="s"
        class="proto-link"
        :class="{ active: i === sideIndex }"
        @click="router.push('/p/' + s)"
      >{{ s }}</a>
    </aside>

    <div class="proto-main">
      <div class="proto-topbar">
        <button type="button" class="proto-topbar__btn" @click="goBack">← 返回</button>
        <span class="proto-topbar__name">{{ spec.name }}</span>
        <button type="button" class="proto-topbar__btn" @click="router.push('/onboarding')">引导页</button>
      </div>
      <div ref="wrapRef" class="proto-device-wrap">
        <div class="proto-device" :style="{ transform: `scale(${scale})` }">
          <ScreenRenderer
            :spec="spec"
            :keypad-value="keypadValue"
            keypad-target="150.00"
            @navigate="onNavigate"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.proto-stage {
  display: flex;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: #17181c;
}
.proto-side {
  width: 180px;
  padding: 16px 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}
.proto-title {
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 10px;
}
.proto-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}
.proto-action {
  padding: 6px 8px;
  border: 0;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.8);
  font-family: inherit;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}
.proto-action:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}
.proto-link {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
  padding: 5px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.proto-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
.proto-link.active {
  color: #fff;
  background: rgba(23, 183, 189, 0.35);
}
.proto-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.proto-topbar {
  display: none;
}
.proto-device-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 16px;
}
.proto-device {
  flex: none;
  width: 375px;
  height: 812px;
  border-radius: 40px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6), 0 0 0 10px #1b1c20, 0 0 0 12px #33353b;
  overflow: hidden;
  transform-origin: center center;
}
@media (max-width: 700px) {
  .proto-side {
    display: none;
  }
  .proto-topbar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    height: calc(46px + env(safe-area-inset-top));
    padding: env(safe-area-inset-top) 8px 0;
    background: #17181c;
  }
  .proto-topbar__btn {
    padding: 8px 6px;
    border: 0;
    background: transparent;
    color: rgba(255, 255, 255, 0.82);
    font-family: inherit;
    font-size: 13px;
    cursor: pointer;
  }
  .proto-topbar__btn:hover {
    color: #fff;
  }
  .proto-topbar__name {
    min-width: 0;
    overflow: hidden;
    color: rgba(255, 255, 255, 0.5);
    font-family: inherit;
    font-size: 12px;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .proto-device-wrap {
    padding: 12px 16px;
  }
  .proto-device {
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
