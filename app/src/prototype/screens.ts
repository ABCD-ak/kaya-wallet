import type { ScreenSpec, TapRule, MenuZone } from './types'

const modules = import.meta.glob('./specs/*.json', { eager: true }) as Record<string, { default: ScreenSpec }>

export const screens: Record<string, ScreenSpec> = {}
for (const mod of Object.values(modules)) {
  const spec = mod.default
  screens[spec.slug] = spec
}

export const screenOrder = [
  'onboarding-1', 'onboarding-2', 'onboarding-3',
  'login-or-register', 'login', 'login-filled', 'login-error', 'forgot-password', 'register',
  'wallet', 'wallet-add', 'wallet-expense', 'send-money', 'add-amount', 'money-sent', 'receipt', 'receipt-download',
  'notifications', 'account', 'account-edit', 'credit-card',
  'transactions', 'transactions-tips',
]

/**
 * 点击导航规则：按文本（设计稿原文，trim 后精确匹配）或图片 src 片段匹配；
 * 可选 x/y 位置过滤（±24px），避免同名文案误命中。
 */
export const TAPS: Record<string, TapRule[]> = {
  'onboarding-1': [{ text: 'Skip', to: '/p/login-or-register' }],
  'onboarding-2': [{ text: 'Skip', to: '/p/login-or-register' }],
  'onboarding-3': [],
  'login-or-register': [
    { text: 'Login', to: '/p/login' },
    { text: 'Register', to: '/p/register' },
  ],
  'login': [
    { text: 'Login', to: '/p/login-filled' },
    { src: 'ic-back', to: '/p/login-or-register' },
    { text: 'Forgot password?', to: '/p/forgot-password' },
  ],
  'login-filled': [
    { text: 'Login', to: '/p/wallet' },
    { src: 'ic-back', to: '/p/login-or-register' },
    { text: 'Forgot password?', to: '/p/forgot-password' },
  ],
  'login-error': [
    { text: 'Login', to: '/p/wallet' },
    { src: 'ic-back', to: '/p/login' },
  ],
  'forgot-password': [
    { text: 'Send', to: '/p/login' },
    { src: 'ic-back', to: '/p/login' },
  ],
  'register': [
    { text: 'Register', to: '/p/wallet' },
    { src: 'ic-back', to: '/p/login-or-register' },
  ],
  'wallet': [
    { src: 'ic-notif', x: 319, y: 58, to: '/p/notifications' },
    { src: 'ic-see-more', to: '/p/transactions' },
    { text: 'Send money to', to: '/p/send-money' },
    { text: 'Add recipient', to: '/p/send-money' },
    { text: 'All', x: 40, y: 545, to: '/p/transactions' },
    { text: 'Send', x: 170, y: 545, to: '/p/transactions' },
    { text: 'Received', to: '/p/transactions' },
    { text: 'Dribbble', to: '/p/transactions-tips' },
    { text: 'Sketch App', to: '/p/transactions-tips' },
    { text: 'Playstasion Store', to: '/p/transactions-tips' },
    // 信用卡区域
    { src: 'visa-logo', x: 275, y: 139, to: '/p/credit-card' },
  ],
  'wallet-add': [
    { text: 'New Income', to: '/p/wallet-expense' },
    { text: 'New Expense', to: '/p/wallet-expense' },
    { src: 'ic-add-income', to: '/p/wallet-expense' },
    { src: 'ic-new-expense', to: '/p/wallet-expense' },
  ],
  'wallet-expense': [
    { text: 'Apply', to: '/p/wallet' },
    { src: 'ic-back', to: '/p/wallet-add' },
  ],
  'send-money': [
    { src: 'ic-back', to: '/p/wallet' },
    { text: 'Continue', to: '/p/add-amount' },
    { text: 'King M’baku', to: '/p/add-amount' },
    { text: 'Prince T’Challa', x: 94, y: 336, to: '/p/add-amount' },
    { text: 'Adam Warlock', to: '/p/add-amount' },
    { text: 'Thor Odinson', to: '/p/add-amount' },
    { text: 'Magneto Svenssen', to: '/p/add-amount' },
    { text: 'Peter Parker', to: '/p/add-amount' },
    { text: 'Eddie Brock', to: '/p/add-amount' },
  ],
  'add-amount': [
    { src: 'ic-back', to: '/p/send-money' },
    { text: 'Send money', to: '/p/money-sent' },
    { text: '100', x: 50, y: 373, to: '/p/money-sent' },
    { text: '250', x: 102, y: 373, to: '/p/money-sent' },
    { text: '500', x: 155, y: 373, to: '/p/money-sent' },
  ],
  'money-sent': [
    { text: 'See Receipt', to: '/p/receipt' },
  ],
  'receipt': [
    { src: 'ic-back', to: '/p/money-sent' },
    { text: 'Download Receipt', to: '/p/receipt-download' },
  ],
  'receipt-download': [
    { text: 'Back to homescreen', to: '/p/wallet' },
  ],
  'notifications': [
    { src: 'ic-back', to: '/p/wallet' },
  ],
  'account': [
    { src: 'ic-back', x: 20, y: 54, to: '/p/wallet' },
    { src: 'ic-pp', to: '/p/account-edit' },
    { text: 'Edit profile', to: '/p/account-edit' },
    { text: 'Credit card', to: '/p/credit-card' },
    { src: 'logo-visa', to: '/p/credit-card' },
    { text: 'Notifications', x: 74, y: 362, to: '/p/notifications' },
    { text: 'Logout', to: '/p/login-or-register' },
    { src: 'ic-logout', to: '/p/login-or-register' },
  ],
  'account-edit': [
    { src: 'ic-back', to: '/p/account' },
    { text: 'Save', to: '/p/account' },
  ],
  'credit-card': [
    { src: 'ic-back', to: '/p/account' },
  ],
  'transactions': [
    { src: 'ic-notif', to: '/p/notifications' },
    { text: 'Travelling', x: 94, y: 306, to: '/p/transactions-tips' },
    { text: 'Fashion', x: 94, y: 366, to: '/p/transactions-tips' },
    { text: 'Food & Drink', x: 94, y: 426, to: '/p/transactions-tips' },
    { text: 'House', to: '/p/transactions-tips' },
  ],
  'transactions-tips': [
    { text: 'Set password', to: '/p/wallet' },
  ],
}

/** 整屏点击目标（引导页轻触进入下一屏） */
export const SCREEN_TAP: Record<string, string> = {
  'onboarding-1': '/p/onboarding-2',
  'onboarding-2': '/p/onboarding-3',
  'onboarding-3': '/p/login-or-register',
}

/** 底部菜单热区（相对 #menu 图片左上角，#menu 为 375x96 整图） */
const MENU_ZONES: MenuZone[] = [
  { x: 0, y: 25, w: 64, h: 40, to: '/p/wallet' },
  { x: 64, y: 25, w: 74, h: 40, to: '/p/transactions' },
  { x: 138, y: 0, w: 100, h: 52, to: '/p/wallet-add' },
  { x: 238, y: 25, w: 74, h: 40, to: '/p/wallet' },
  { x: 313, y: 25, w: 62, h: 40, to: '/p/account' },
]

export function menuZonesOf(spec: ScreenSpec): MenuZone[] {
  const menu = spec.nodes.find((n) => n.t === 'img' && n.src?.includes('menu-'))
  if (!menu) return []
  return MENU_ZONES.map((z) => ({ ...z, x: z.x + menu.x, y: z.y + menu.y }))
}
