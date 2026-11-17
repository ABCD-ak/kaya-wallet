<script setup lang="ts">
import StatusBar from '@/components/StatusBar.vue'

withDefaults(
  defineProps<{
    title: string
    showBack?: boolean
  }>(),
  { showBack: true },
)

const emit = defineEmits<{ back: [] }>()
</script>

<template>
  <div class="page-layout">
    <StatusBar />
    <header class="page-layout__header">
      <button
        v-if="showBack"
        class="page-layout__back"
        type="button"
        @click="emit('back')"
      >
        ←
      </button>
      <h1 class="page-layout__title">{{ title }}</h1>
      <span class="page-layout__spacer" />
      <slot name="actions" />
    </header>
    <main class="page-layout__main">
      <slot />
    </main>
    <div class="page-layout__home"><span /></div>
  </div>
</template>

<style scoped>
.page-layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 390px;
  margin: 0 auto;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--app-bg, #f7f6f8);
}

.page-layout__header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  height: 56px;
  padding: 0 20px;
}

.page-layout__back {
  width: 32px;
  height: 32px;
  border: 0;
  background: none;
  font-size: 22px;
  line-height: 1;
  color: var(--app-ink, #353333);
  cursor: pointer;
}

.page-layout__title {
  flex: 0 1 auto;
  min-width: 0;
  margin: 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  letter-spacing: -0.4px;
  color: var(--app-ink, #353333);
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.page-layout__spacer {
  flex: 1;
}

.page-layout__main {
  flex: 1;
  overflow-y: auto;
  padding: 8px 20px 24px;
}

.page-layout__home {
  display: flex;
  flex-shrink: 0;
  justify-content: center;
  padding-bottom: calc(9px + env(safe-area-inset-bottom));
}

.page-layout__home span {
  width: 134px;
  height: 5px;
  border-radius: 100px;
  background: #d3cbcb;
}

@media (min-width: 420px) {
  .page-layout {
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.12);
  }
}
</style>
