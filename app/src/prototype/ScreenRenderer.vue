<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import type { ScreenSpec, SpecNode, TapRule } from './types'
import { TAPS, SCREEN_TAP, menuZonesOf } from './screens'
import { t } from './i18n'

const props = defineProps<{
  spec: ScreenSpec
  /** 键盘输入覆盖：显示文本节点 ch === keypadTarget 的节点时改用该值 */
  keypadValue?: string
  keypadTarget?: string
}>()

const emit = defineEmits<{ navigate: [to: string] }>()

const rgba = (hexColor: string, alpha: number) => {
  const h = hexColor.replace('#', '')
  const n = parseInt(h.length === 3 ? h.split('').map((c) => c + c).join('') : h, 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`
}

const fontFamily = `'Poppins','PingFang SC','Hiragino Sans GB','Microsoft YaHei',sans-serif`

function boxStyle(n: SpecNode): CSSProperties {
  const s: CSSProperties = {
    position: 'absolute',
    left: `${n.x}px`,
    top: `${n.y}px`,
    width: `${n.w}px`,
    height: `${n.h}px`,
    opacity: n.o ?? 1,
  }
  if (n.g) {
    const [kind, deg] = n.g.gradient.split('|')
    s.background = kind === 'radial'
      ? `radial-gradient(circle, ${n.g.stops.join(', ')})`
      : `linear-gradient(${deg}deg, ${n.g.stops.join(', ')})`
  } else if (n.f || n.fa != null) {
    const color = n.f ?? '#ffffff'
    s.background = n.fa != null && n.fa < 1 ? rgba(color, n.fa) : color
  }
  if (n.r != null) {
    s.borderRadius = Array.isArray(n.r)
      ? n.r.map((v) => `${v}px`).join(' ')
      : typeof n.r === 'number' ? `${n.r}px` : n.r
  }
  if (n.st) s.border = `${n.st.w}px solid ${rgba(n.st.c, n.st.a)}`
  if (n.sh) s.boxShadow = `${n.sh.x}px ${n.sh.y}px ${n.sh.blur}px ${n.sh.spread}px ${n.sh.color}`
  if (n.clip) s.overflow = 'hidden'
  if (n.rot) s.transform = `rotate(${-n.rot}deg)`
  return s
}

/**
 * 设计稿单行短文案（金额、标签、按钮等）在中文/字体差异下容易从中间折行，
 * 这里按原文长度判定为单行，禁止换行，避免出现 "3,250.0 / 0" 这类断字。
 */
function isSingleLineLabel(n: SpecNode): boolean {
  const text = n.ch?.trim() ?? ''
  return text.length > 0 && text.length <= 14 && !text.includes('\n')
}

function textStyle(n: SpecNode): CSSProperties {
  const singleLine = isSingleLineLabel(n)
  const s: CSSProperties = {
    position: 'absolute',
    left: `${n.x}px`,
    top: `${n.y}px`,
    width: `${n.w}px`,
    minHeight: `${n.h}px`,
    margin: '0',
    fontSize: `${n.fs}px`,
    fontWeight: n.fw ?? 400,
    fontFamily,
    lineHeight: `${n.lh ?? Math.round(n.fs! * 1.5 * 10) / 10}px`,
    whiteSpace: singleLine ? 'nowrap' : 'pre-wrap',
    wordBreak: 'break-word',
    opacity: n.o ?? 1,
  }
  if (n.col) s.color = (n.colA ?? 1) < 1 ? rgba(n.col, n.colA!) : n.col
  if (n.ls != null) s.letterSpacing = `${n.ls}px`
  if (n.ta) s.textAlign = n.ta.toLowerCase() as 'left' | 'center' | 'right'
  if (n.tt) s.textTransform = n.tt
  if (n.rot) s.transform = `rotate(${-n.rot}deg)`
  return s
}

function imgStyle(n: SpecNode): CSSProperties {
  const s: CSSProperties = {
    position: 'absolute',
    left: `${n.x}px`,
    top: `${n.y}px`,
    width: `${n.w}px`,
    height: `${n.h}px`,
    opacity: n.o ?? 1,
    userSelect: 'none',
  }
  if (n.rot) s.transform = `rotate(${-n.rot}deg)`
  return s
}

function matchRule(n: SpecNode): TapRule | undefined {
  const rules = TAPS[props.spec.slug] ?? []
  return rules.find((r) => {
    if (r.text != null) {
      if (n.t !== 'text' || n.ch?.trim() !== r.text) return false
    } else if (r.src != null) {
      if (n.t !== 'img' || !n.src?.includes(r.src)) return false
    } else {
      return false
    }
    if (r.x != null && Math.abs(n.x - r.x) > 24) return false
    if (r.y != null && Math.abs(n.y - r.y) > 24) return false
    return true
  })
}

function onTap(n: SpecNode, e: MouseEvent) {
  const rule = matchRule(n)
  if (rule) {
    e.stopPropagation()
    emit('navigate', rule.to)
    return
  }
  // 键盘数字/退格（add-amount）
  if (props.spec.slug === 'add-amount') {
    if (n.t === 'text' && /^[0-9]$/.test(n.ch ?? '')) {
      e.stopPropagation()
      emit('navigate', `keypad:${n.ch}`)
    } else if (n.t === 'img' && n.src?.includes('delete')) {
      e.stopPropagation()
      emit('navigate', 'keypad:del')
    }
  }
}

function onScreenTap() {
  const to = SCREEN_TAP[props.spec.slug]
  if (to) emit('navigate', to)
}

const zones = computed(() => menuZonesOf(props.spec))

function displayText(n: SpecNode): string {
  if (props.keypadTarget && n.ch === props.keypadTarget && props.keypadValue) {
    return props.keypadValue
  }
  return t(n.ch ?? '')
}
</script>

<template>
  <div class="screen" :style="{ width: spec.w + 'px', height: spec.h + 'px' }" @click="onScreenTap">
    <template v-for="(n, i) in spec.nodes" :key="i">
      <img v-if="n.t === 'img'" :src="'/' + n.src" :style="imgStyle(n)" alt="" draggable="false" @click="onTap(n, $event)" />
      <div v-else-if="n.t === 'box'" :style="boxStyle(n)" @click="onTap(n, $event)" />
      <div v-else :style="textStyle(n)" @click="onTap(n, $event)">{{ displayText(n) }}</div>
    </template>
    <div
      v-for="(z, i) in zones"
      :key="'zone' + i"
      class="menu-zone"
      :style="{ left: z.x + 'px', top: z.y + 'px', width: z.w + 'px', height: z.h + 'px' }"
      @click.stop="emit('navigate', z.to)"
    />
  </div>
</template>

<style scoped>
.screen {
  position: relative;
  overflow: hidden;
  background: #fff;
  flex-shrink: 0;
}
.screen :deep(*) {
  box-sizing: border-box;
}
.menu-zone {
  position: absolute;
  cursor: pointer;
}
</style>
