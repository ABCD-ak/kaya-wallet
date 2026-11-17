export interface OnboardingSlideData {
  key: string
  title: string
  description: string
  image: string
  imageAlt: string
  /** 设计稿 375 宽下的插画宽度（高度按比例自适应） */
  imageWidth: number
}

export const onboardingSlides: OnboardingSlideData[] = [
  {
    key: 'secure',
    title: '安全可信',
    description: '你的每一笔资金往来都经过加密保护，随时安心管理财富。',
    image: '/assets/safety-payment-data-finance-z-1j.png',
    imageAlt: '安全支付插画',
    imageWidth: 240,
  },
  {
    key: 'invest',
    title: '投资你的财富',
    description: '让闲置资金持续增值，收益变化一目了然。',
    image: '/assets/invest-data-finance-r-1i.png',
    imageAlt: '投资数据插画',
    imageWidth: 219,
  },
  {
    key: 'earn',
    title: '边赚边买！',
    description: '赚取积分与专属收益，把心仪好物轻松带回家。',
    image: '/assets/money-buy-chart-finance-v-1h.png',
    imageAlt: '消费返利插画',
    imageWidth: 232,
  },
]
