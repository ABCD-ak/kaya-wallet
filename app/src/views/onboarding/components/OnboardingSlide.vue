<script setup lang="ts">
import type { OnboardingSlideData } from '../slides'

defineProps<{ slide: OnboardingSlideData }>()
</script>

<template>
  <section class="slide">
    <div class="slide__illustration">
      <img
        :src="slide.image"
        :alt="slide.imageAlt"
        :style="{ width: `${slide.imageWidth}px` }"
        draggable="false"
      />
    </div>
    <div class="slide__text">
      <h1 class="slide__title">{{ slide.title }}</h1>
      <p class="slide__desc">{{ slide.description }}</p>
    </div>
  </section>
</template>

<style scoped>
.slide {
  flex: 0 0 100%;
  width: 100%;
  padding: 0 30px;
  background: var(--app-bg, #f7f6f8);
}

.slide__illustration {
  display: flex;
  align-items: flex-end;
  height: 260px;
  margin-top: 80px;
}

.slide__illustration img {
  max-width: 100%;
  height: auto;
  user-select: none;
}

.slide__text {
  min-height: 140px;
  margin-top: 50px;
}

.slide__title {
  margin: 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 24px;
  font-weight: 600;
  line-height: 34px;
  letter-spacing: -0.4px;
  color: var(--app-ink, #353333);
}

.slide__desc {
  margin: 10px 0 0;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 400;
  line-height: 24px;
  letter-spacing: -0.2px;
  color: var(--app-muted, #696565);
}

/* 矮屏收紧纵向留白，保证内容完整可见 */
@media (max-height: 700px) {
  .slide__illustration {
    height: 210px;
    margin-top: 40px;
  }

  .slide__text {
    min-height: 120px;
    margin-top: 30px;
  }

  .slide__desc {
    font-size: 18px;
    line-height: 27px;
  }
}
</style>
