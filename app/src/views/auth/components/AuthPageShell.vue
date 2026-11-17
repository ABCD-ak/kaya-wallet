<script setup lang="ts">
import StatusBar from '@/components/StatusBar.vue'

withDefaults(defineProps<{ showBack?: boolean }>(), { showBack: true })

const emit = defineEmits<{ back: [] }>()
</script>

<template>
  <div class="auth-page">
    <StatusBar />

    <main class="auth-page__main">
      <button
        v-if="showBack"
        class="auth-page__back"
        type="button"
        aria-label="返回"
        @click="emit('back')"
      >
        <svg viewBox="0 0 36 36" aria-hidden="true">
          <path
            d="M21.5 8.5 12.5 18l9 9.5"
            fill="none"
            stroke="#353333"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>

      <slot />
    </main>

    <div class="auth-page__home"><span /></div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--app-bg, #f7f6f8);
}

.auth-page__main {
  flex: 1;
  overflow-y: auto;
  padding: 10px 20px 24px;
  scrollbar-width: none;
}

.auth-page__main::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.auth-page__back {
  display: block;
  width: 36px;
  height: 36px;
  margin: 0 0 32px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.auth-page__back svg {
  display: block;
  width: 36px;
  height: 36px;
}

/* 设计稿的状态栏图标是白色导出资源，白底页面需要反色才可见 */
.auth-page :deep(.status-bar__icons img) {
  filter: brightness(0) opacity(0.72);
}

.auth-page__home {
  display: flex;
  flex-shrink: 0;
  justify-content: center;
  padding-bottom: calc(9px + env(safe-area-inset-bottom));
}

.auth-page__home span {
  width: 134px;
  height: 5px;
  border-radius: 100px;
  background: #d3cbcb;
}

@media (min-width: 600px) {
  .auth-page {
    max-width: 390px;
    margin: 0 auto;
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.12);
  }
}
</style>
