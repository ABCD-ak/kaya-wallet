<script setup lang="ts">
defineProps<{
  count: number
  current: number
}>()

const emit = defineEmits<{ select: [index: number] }>()
</script>

<template>
  <div class="progress-dots" role="tablist" aria-label="引导页进度">
    <button
      v-for="index in count"
      :key="index"
      type="button"
      class="progress-dots__item"
      :class="{ 'is-active': index - 1 === current }"
      role="tab"
      :aria-selected="index - 1 === current"
      :aria-label="`第 ${index} 页`"
      @click="emit('select', index - 1)"
    />
  </div>
</template>

<style scoped>
.progress-dots {
  display: flex;
  flex-shrink: 0;
  gap: 5px;
  align-items: center;
  height: 5px;
  margin-top: 30px;
  padding: 0 30px;
}

.progress-dots__item {
  width: 10px;
  height: 5px;
  padding: 0;
  border: 0;
  border-radius: 2.5px;
  background: #afafaf;
  cursor: pointer;
  transition: width 0.3s ease, background 0.3s ease;
}

.progress-dots__item.is-active {
  width: 20px;
  background: #17b7bd;
}
</style>
