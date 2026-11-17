export interface SpecNode {
  t: 'box' | 'text' | 'img'
  x: number
  y: number
  w: number
  h: number
  // text
  ch?: string
  fs?: number
  ff?: string
  fw?: number
  col?: string
  colA?: number
  ls?: number
  lh?: number
  ta?: string
  tt?: string
  // box
  f?: string
  fa?: number
  g?: { gradient: string; stops: string[] }
  st?: { c: string; w: number; a: number }
  sh?: { x: number; y: number; blur: number; spread: number; color: string }
  r?: number | string | number[]
  clip?: boolean
  // img
  src?: string
  // common
  o?: number
  rot?: number
}

export interface ScreenSpec {
  slug: string
  name: string
  w: number
  h: number
  nodes: SpecNode[]
}

export interface TapRule {
  /** 匹配文本节点的 characters（trim 后精确匹配，用设计稿原文） */
  text?: string
  /** 匹配图片 src 包含的片段 */
  src?: string
  /** 位置过滤（容差 24px） */
  x?: number
  y?: number
  to: string
}

export interface MenuZone {
  x: number
  y: number
  w: number
  h: number
  to: string
}
