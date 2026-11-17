<script setup lang="ts">
import { computed } from 'vue'

import type { TransactionResponse } from '@/api/types'
import { categoryIcon } from '@/utils/categoryIcons'

const props = withDefaults(
  defineProps<{
    tx: TransactionResponse
    size?: number
  }>(),
  { size: 38 },
)

const iconUrl = computed(() => categoryIcon(props.tx.category?.icon))
const isTransfer = computed(
  () => props.tx.type === 'transfer_in' || props.tx.type === 'transfer_out',
)
const background = computed(
  () =>
    props.tx.category?.color ||
    (props.tx.direction === 'credit' ? '#22c55e' : '#ef6874'),
)
</script>

<template>
  <span
    class="tx-icon"
    :style="{
      width: `${size}px`,
      height: `${size}px`,
      backgroundColor: background,
    }"
  >
    <img v-if="iconUrl" class="tx-icon__img" :src="iconUrl" alt="" />

    <!-- 转账：双向箭头 -->
    <svg
      v-else-if="isTransfer"
      class="tx-icon__svg tx-icon__svg--transfer"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#fff"
      stroke-width="1.9"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <path d="M4 9h13" />
      <polyline points="14 6 17 9 14 12" />
      <path d="M20 15H7" />
      <polyline points="10 12 7 15 10 18" />
    </svg>

    <!-- 无分类图标时的收支方向 -->
    <svg
      v-else-if="tx.direction === 'credit'"
      class="tx-icon__svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#fff"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <line x1="19" y1="12" x2="5" y2="12" />
      <polyline points="12 19 5 12 12 5" />
    </svg>
    <svg
      v-else
      class="tx-icon__svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#fff"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <line x1="5" y1="12" x2="19" y2="12" />
      <polyline points="12 5 19 12 12 19" />
    </svg>
  </span>
</template>

<style scoped>
.tx-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
}

.tx-icon__img {
  width: 52%;
  height: 52%;
  object-fit: contain;
}

.tx-icon__svg {
  width: 48%;
  height: 48%;
}

.tx-icon__svg--transfer {
  width: 58%;
  height: 58%;
}
</style>
