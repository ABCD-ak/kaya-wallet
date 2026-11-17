<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps<{ active?: 'wallet' | 'transactions' | 'transfers' | 'account' | 'none' }>()
const emit = defineEmits<{ fabClick: [] }>()

const router = useRouter()
const route = useRoute()
const attrs = useAttrs()

const currentActive = computed(() => {
  const p = String(route.path)
  if (p.startsWith('/transactions')) return 'transactions'
  if (p.startsWith('/transfers')) return 'transfers'
  if (p.startsWith('/account')) return 'account'
  return 'wallet'
})

function onFab() {
  // 页面有自定义处理时交给页面，否则统一进入「添加记录」
  if (attrs.onFabClick) emit('fabClick')
  else router.push('/wallet/add')
}
</script>

<template>
  <nav class="bn">
    <!-- 钱包 -->
    <button
      class="bn__item"
      :class="{ active: currentActive === 'wallet' }"
      aria-label="钱包"
      @click="router.push('/wallet')"
    >
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path d="M19.5 6H6.2A2.2 2.2 0 0 1 8.4 3.8h9.9c.66 0 1.2.54 1.2 1.2V6Z" />
        <path d="M3.5 8.2c0-1.22.98-2.2 2.2-2.2h13.1c1.22 0 2.2.98 2.2 2.2v8.6c0 1.22-.98 2.2-2.2 2.2H5.7c-1.22 0-2.2-.98-2.2-2.2V8.2Z" />
        <rect x="14.6" y="10.4" width="6.4" height="4.2" rx="1.2" fill="#ffffff" />
      </svg>
    </button>

    <!-- 交易统计 -->
    <button
      class="bn__item"
      :class="{ active: currentActive === 'transactions' }"
      aria-label="交易记录"
      @click="router.push('/transactions')"
    >
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path d="M11.2 3.1A9 9 0 1 0 20.9 12.8H11.2V3.1Z" />
        <path d="M13.4 2.1v7.5h7.5a9 9 0 0 0-7.5-7.5Z" opacity="0.55" />
      </svg>
    </button>

    <!-- 添加 -->
    <button class="bn__fab" type="button" @click="onFab" aria-label="添加">
      <svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round">
        <line x1="12" y1="5" x2="12" y2="19" />
        <line x1="5" y1="12" x2="19" y2="12" />
      </svg>
    </button>

    <!-- 转账 -->
    <button
      class="bn__item"
      :class="{ active: currentActive === 'transfers' }"
      aria-label="转账"
      @click="router.push('/transfers')"
    >
      <svg viewBox="0 0 24 24" fill="currentColor">
        <rect x="2.2" y="6" width="19.6" height="12" rx="2.2" />
        <circle cx="12" cy="12" r="3" fill="#ffffff" />
        <circle cx="5.6" cy="12" r="0.9" fill="#ffffff" />
        <circle cx="18.4" cy="12" r="0.9" fill="#ffffff" />
      </svg>
    </button>

    <!-- 我的账户 -->
    <button
      class="bn__item"
      :class="{ active: currentActive === 'account' }"
      aria-label="我的账户"
      @click="router.push('/account')"
    >
      <svg viewBox="0 0 24 24" fill="currentColor">
        <circle cx="12" cy="7.8" r="4.2" />
        <path d="M3.6 20.4c0-4.4 3.7-7.2 8.4-7.2s8.4 2.8 8.4 7.2c0 .77-.62 1.4-1.4 1.4H5c-.78 0-1.4-.63-1.4-1.4Z" />
      </svg>
    </button>
  </nav>
</template>

<style scoped>
.bn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 20px calc(8px + env(safe-area-inset-bottom));
  background: var(--app-surface, #ffffff);
  border-top: 1px solid var(--app-line, #f0eeef);
  flex-shrink: 0;
  position: relative;
  z-index: 20;
  overflow: visible;
}
.bn__item {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: none;
  color: #b9c0bd;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}
.bn__item svg {
  width: 24px;
  height: 24px;
}
.bn__item.active {
  color: var(--app-accent, #17b7bd);
}
.bn__fab {
  flex: none;
  position: relative;
  z-index: 30;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 0;
  background: linear-gradient(135deg, #17b7bd 0%, #9deb93 100%);
  box-shadow: 0 8px 18px rgba(23, 183, 189, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  cursor: pointer;
  padding: 0;
}
.bn__fab svg {
  width: 26px;
  height: 26px;
}
</style>
