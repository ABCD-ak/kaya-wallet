// 从 MCP 导出的完整设计树生成原型渲染规格
// 用法: node scripts/flatten.mjs <tree.json> <outdir>
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { join } from 'node:path'

const [treePath, outDir] = process.argv.slice(2)
const tree = JSON.parse(readFileSync(treePath, 'utf8'))

const SCREEN_SKIP = new Set(['iPhone XS', '有料素材 uisheji.taobao.com 9.9加入会员全店免费下载-更新'])
const SCREEN_SLUGS = {
  '1000 Onboarding 1': 'onboarding-1',
  '1001 Onboarding 2': 'onboarding-2',
  '1002 Onboarding 3': 'onboarding-3',
  '2000 Login or Register': 'login-or-register',
  '2001 Login Form': 'login',
  '2002 Login Form (All Filled)': 'login-filled',
  '2003 Login Form (Password Error)': 'login-error',
  '2004 Forgot Password': 'forgot-password',
  '2005 Register Form': 'register',
  '3000 My Wallet': 'wallet',
  '3001 My Wallet - Add': 'wallet-add',
  '3002 My Wallet - Input Expense': 'wallet-expense',
  '3003 My Wallet - Send Money': 'send-money',
  '3004 My Wallet - Add Amount': 'add-amount',
  '3005 My Wallet - Money Sent': 'money-sent',
  '3006 My Wallet - Payment Receipt': 'receipt',
  '3007 My Wallet - Payment Receipt Download': 'receipt-download',
  '4000 Notifications': 'notifications',
  '5000 Account': 'account',
  '5001 Edit Account': 'account-edit',
  '5002 Credit Card': 'credit-card',
  '6000 Transactions': 'transactions',
  '6001 Transactions - Tips': 'transactions-tips',
}
const SCREENS = new Set(Object.keys(SCREEN_SLUGS))

const VECTOR_TYPES = new Set(['VECTOR', 'BOOLEAN_OPERATION', 'STAR', 'POLYGON'])
const CSS_SHAPE_TYPES = new Set(['RECTANGLE', 'ELLIPSE', 'LINE'])

const hex = (c) => {
  if (!c) return null
  const to = (v) => Math.round(v * 255).toString(16).padStart(2, '0')
  return `#${to(c.r)}${to(c.g)}${to(c.b)}`
}
const fillAlpha = (f) => (f.opacity ?? 1) * (f.visible === false ? 0 : 1)
const fontWeight = (styleName = '') => {
  const s = styleName.toLowerCase()
  if (s.includes('black')) return 900
  if (s.includes('extrabold') || s.includes('extra bold')) return 800
  if (s.includes('bold')) return 700
  if (s.includes('semibold') || s.includes('semi bold')) return 600
  if (s.includes('medium')) return 500
  return 400
}

const strings = new Set()
const assets = new Map() // key -> {id, name, w, h, file}

function hasTextDescendant(n) {
  if (n.type === 'TEXT') return true
  return (n.children ?? []).some(hasTextDescendant)
}
function hasVectorDescendant(n) {
  if (VECTOR_TYPES.has(n.type)) return true
  return (n.children ?? []).some(hasVectorDescendant)
}
function imageFill(n) {
  return (n.fills ?? []).some((f) => f.type === 'IMAGE' && f.visible !== false)
}

function solidFill(fills) {
  if (!fills?.length) return null
  const f = fills.find((x) => x.visible !== false)
  if (!f) return null
  if (f.type === 'SOLID') {
    const h = hex(f.color)
    return f.opacity != null && f.opacity < 1 ? { color: h, alpha: f.opacity } : h
  }
  if (f.type === 'GRADIENT_LINEAR' || f.type === 'GRADIENT_RADIAL') {
    const stops = (f.gradientStops ?? []).map((s) => `${hex(s.color)} ${(s.position * 100).toFixed(1)}%`)
    const t = f.gradientTransform ?? [[1, 0, 0], [0, 1, 0]]
    // 渐变方向:矩阵第二行的 xy 决定 y 轴方向向量
    const dx = t[1]?.[0] ?? 0
    const dy = t[1]?.[1] ?? 1
    let deg = Math.round((Math.atan2(dy, dx) * 180) / Math.PI + 90)
    return {
      gradient: `${f.type === 'GRADIENT_RADIAL' ? 'radial' : 'linear'}|${deg}`,
      stops,
    }
  }
  return null
}

function shadowCss(effects) {
  const s = (effects ?? []).find((e) => e.type === 'DROP_SHADOW' && e.visible !== false)
  if (!s) return null
  const c = s.color ?? { r: 0, g: 0, b: 0, a: 0 }
  const to = (v) => Math.round(v * 255).toString(16).padStart(2, '0')
  return {
    x: Math.round(s.offset?.x ?? 0),
    y: Math.round(s.offset?.y ?? 0),
    blur: Math.round(s.radius ?? 0),
    spread: Math.round(s.spread ?? 0),
    color: `#${to(c.r)}${to(c.g)}${to(c.b)}${Math.round((c.a ?? 1) * 255).toString(16).padStart(2, '0')}`,
  }
}

function regAsset(node) {
  const key = `${node.name}|${Math.round(node.width)}x${Math.round(node.height)}`
  if (assets.has(key)) return assets.get(key).file
  const slug = node.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60) || 'asset'
  const file = `assets/${slug}-${key.length.toString(36)}-${assets.size.toString(36)}.png`
  assets.set(key, { id: node.id, name: node.name, w: node.width, h: node.height, file })
  return file
}

function emit(out, node, dx, dy, insideClip) {
  if (node.visible === false) return
  const x = node.x + dx
  const y = node.y + dy
  const t = node.type
  const frameLike = t === 'FRAME' || t === 'GROUP' || t === 'COMPONENT' || t === 'INSTANCE'

  // 纯图形子树 → 整体导出 PNG
  if (frameLike && !hasTextDescendant(node) && hasVectorDescendant(node)) {
    out.push({ t: 'img', x, y, w: node.width, h: node.height, src: regAsset(node), o: node.opacity ?? 1, rot: node.rotation || 0 })
    return
  }
  if (VECTOR_TYPES.has(t) || imageFill(node)) {
    out.push({ t: 'img', x, y, w: node.width, h: node.height, src: regAsset(node), o: node.opacity ?? 1, rot: node.rotation || 0 })
    return
  }
  if (t === 'TEXT') {
    const f = solidFill(node.fills)
    strings.add(node.characters)
    out.push({
      t: 'text', x, y, w: node.width, h: node.height,
      ch: node.characters,
      fs: node.fontSize,
      ff: node.fontName?.family ?? '',
      fw: fontWeight(node.fontName?.style),
      col: typeof f === 'object' && f !== null && !('gradient' in f) ? f.color : (f ?? '#000000'),
      colA: typeof f === 'object' && f !== null && !('gradient' in f) ? f.alpha : 1,
      ls: node.letterSpacing?.unit === 'PIXELS' ? +(node.letterSpacing.value.toFixed(2)) : undefined,
      lh: node.lineHeight?.unit === 'PIXELS' ? +(node.lineHeight.value.toFixed(1)) : undefined,
      ta: node.textAlignHorizontal !== 'LEFT' ? node.textAlignHorizontal : undefined,
      tt: node.textCase === 'UPPER' ? 'uppercase' : node.textCase === 'LOWER' ? 'lowercase' : undefined,
      o: node.opacity ?? 1, rot: node.rotation || 0,
    })
    return
  }
  if (CSS_SHAPE_TYPES.has(t) || frameLike) {
    const f = solidFill(node.fills)
    const item = {
      t: 'box', x, y, w: node.width, h: node.height, o: node.opacity ?? 1, rot: node.rotation || 0,
      f: typeof f === 'string' ? f : undefined,
      fa: typeof f === 'object' && f !== null && !('gradient' in f) ? f.alpha : undefined,
      g: f && typeof f === 'object' && 'gradient' in f ? f : undefined,
      st: node.strokes?.length && node.strokeWeight > 0
        ? { c: hex(node.strokes.find((s) => s.visible !== false)?.color) ?? '#000', w: node.strokeWeight, a: node.strokes.find((s) => s.visible !== false)?.opacity ?? 1 }
        : undefined,
      sh: shadowCss(node.effects),
      r: node.cornerRadius || (node.topLeftRadius || node.topRightRadius || node.bottomLeftRadius || node.bottomRightRadius
        ? [node.topLeftRadius ?? 0, node.topRightRadius ?? 0, node.bottomRightRadius ?? 0, node.bottomLeftRadius ?? 0]
        : undefined),
      clip: t === 'FRAME' && node.clipsContent ? true : undefined,
    }
    const isPlain = !node.children?.length
    if (isPlain || t === 'RECTANGLE' || t === 'ELLIPSE') {
      if (t === 'ELLIPSE' && !item.r) item.r = '50%'
      out.push(item)
      if (!node.children?.length) return
    } else {
      out.push(item)
    }
    const off = t === 'FRAME' || t === 'COMPONENT' || t === 'INSTANCE' ? { dx: x, dy: y } : { dx, dy } // GROUP 不改坐标系
    const prevClip = insideClip
    for (const c of node.children ?? []) emit(out, c, off.dx, off.dy, prevClip || item.clip)
    return
  }
  // 其他类型(GROUP 已处理)递归兜底
  for (const c of node.children ?? []) emit(out, c, dx, dy, insideClip)
}

mkdirSync(outDir, { recursive: true })
mkdirSync(join(outDir, 'specs'), { recursive: true })

const manifest = []
for (const frame of tree.children ?? []) {
  if (!SCREENS.has(frame.name)) continue
  const nodes = []
  emit(nodes, frame, -frame.x, -frame.y, false)
  const slug = SCREEN_SLUGS[frame.name]
  const spec = { slug, name: frame.name, w: frame.width, h: frame.height, nodes }
  writeFileSync(join(outDir, 'specs', `${slug}.json`), JSON.stringify(spec))
  manifest.push({ slug, name: frame.name, id: frame.id, count: nodes.length })
  console.log(`${slug}: ${nodes.length} nodes`)
}

const assetList = [...assets.values()]
writeFileSync(join(outDir, 'assets.json'), JSON.stringify(assetList, null, 1))
writeFileSync(join(outDir, 'strings.json'), JSON.stringify([...strings], null, 1))
writeFileSync(join(outDir, 'manifest.json'), JSON.stringify(manifest, null, 1))
console.log(`\nassets: ${assetList.length}, unique strings: ${strings.size}`)
