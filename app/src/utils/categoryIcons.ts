// 交易分类图标资源映射，供记账与交易列表共用
const CATEGORY_ICON_ASSETS: Record<string, string> = {
  'ic-food-drink': '/assets/ic-food-drink-j-8.png',
  'ic-fashion': '/assets/ic-fashion-g-9.png',
  'ic-travelling': '/assets/ic-travelling-j-a.png',
  'ic-house': '/assets/ic-house-e-7.png',
  'ic-see-more': '/assets/ic-see-more-h-y.png',
  'ic-invoice-payment': '/assets/ic-invoice-payment-o-t.png',
  'ic-address': '/assets/ic-address-g-o.png',
  'ic-income': '/assets/ic-income-f-u.png',
  'ic-add-income': '/assets/ic-add-income-j-1b.png',
  'ic-correct': '/assets/ic-correct-g-1d.png',
}

export function categoryIcon(icon?: string): string {
  return CATEGORY_ICON_ASSETS[icon ?? ''] ?? ''
}
