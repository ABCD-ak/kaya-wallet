<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const menuOpen = ref(false)
const activeFaq = ref<number | null>(0)

const navItems = [
  { label: '产品功能', target: 'features' },
  { label: '安全保障', target: 'security' },
  { label: '智能客服', target: 'assistant' },
  { label: '常见问题', target: 'faq' },
]

const features = [
  {
    title: '收支，一目了然',
    description: '收入、支出、转账自动归类，用清晰的图表读懂自己的消费习惯。',
    icon: 'chart',
    className: 'feature-card--mint',
  },
  {
    title: '转账，简单安心',
    description: '常用收款人、实时手续费与完整电子回单，让每一步都有据可查。',
    icon: 'transfer',
    className: 'feature-card--blue',
  },
  {
    title: '卡片，集中管理',
    description: '银行卡状态、尾号与关联账户集中展示，冻结和管理都更方便。',
    icon: 'card',
    className: 'feature-card--lime',
  },
]

const faqs = [
  {
    question: '卡雅可以帮我做什么？',
    answer: '卡雅帮助你统一管理钱包余额、收支记录、银行卡和转账。你可以查看每月收支构成、追踪交易明细，也可以随时向 AI 客服提问。',
  },
  {
    question: '我的账户数据安全吗？',
    answer: '账户访问需要身份认证，敏感操作会进行安全校验。卡雅不会通过客服索取你的密码、验证码或完整卡号，请勿向任何人透露这些信息。',
  },
  {
    question: 'AI 客服能处理具体问题吗？',
    answer: 'AI 客服可以解答产品使用、转账、卡片和账单等常见问题。涉及具体账户或交易核查时，会引导你联系人工客服进一步处理。',
  },
  {
    question: '如何在手机上使用？',
    answer: '点击页面中的「打开 App」即可进入卡雅。产品采用移动端优先设计，也可以添加到手机主屏幕，获得接近原生 App 的体验。',
  },
]

function scrollTo(target: string) {
  menuOpen.value = false
  document.getElementById(target)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openApp() {
  router.push('/onboarding')
}

function toggleFaq(index: number) {
  activeFaq.value = activeFaq.value === index ? null : index
}

onMounted(() => {
  document.title = '卡雅 Kaya｜让每一笔收支都清晰可见'
})

onUnmounted(() => {
  document.title = '卡雅'
})
</script>

<template>
  <div class="official-site">
    <header class="site-header">
      <div class="site-container header-inner">
        <button class="brand" type="button" aria-label="返回首页" @click="scrollTo('home')">
          <span class="brand__logo">K</span>
          <span class="brand__name">卡雅 <small>KAYA</small></span>
        </button>

        <nav class="desktop-nav" aria-label="主导航">
          <button v-for="item in navItems" :key="item.target" type="button" @click="scrollTo(item.target)">
            {{ item.label }}
          </button>
        </nav>

        <div class="header-actions">
          <button class="button button--dark button--small desktop-cta" type="button" @click="openApp">
            打开 App
            <span aria-hidden="true">↗</span>
          </button>
          <button
            class="menu-toggle"
            type="button"
            :aria-expanded="menuOpen"
            aria-label="打开导航菜单"
            @click="menuOpen = !menuOpen"
          >
            <i :class="{ 'is-open': menuOpen }" /><i :class="{ 'is-open': menuOpen }" />
          </button>
        </div>
      </div>

      <nav v-if="menuOpen" class="mobile-nav" aria-label="移动端导航">
        <button v-for="item in navItems" :key="item.target" type="button" @click="scrollTo(item.target)">
          {{ item.label }} <span>→</span>
        </button>
        <button class="button button--dark" type="button" @click="openApp">打开 App</button>
      </nav>
    </header>

    <main>
      <section id="home" class="hero">
        <div class="hero__glow hero__glow--one" />
        <div class="hero__glow hero__glow--two" />
        <svg class="hero__ribbon" viewBox="0 0 1440 690" preserveAspectRatio="none" fill="none" aria-hidden="true">
          <defs>
            <linearGradient id="siteRibbon" x1="0" y1="300" x2="1440" y2="560" gradientUnits="userSpaceOnUse">
              <stop offset="0" stop-color="#17b7bd" />
              <stop offset="1" stop-color="#9deb93" />
            </linearGradient>
          </defs>
          <path d="M-120 500 C 260 280, 720 250, 1560 470" stroke="#17b7bd" stroke-opacity=".08" stroke-width="120" stroke-linecap="round" />
          <path d="M-120 556 C 280 330, 740 300, 1560 516" stroke="#9deb93" stroke-opacity=".14" stroke-width="58" stroke-linecap="round" />
          <path d="M-120 528 C 270 304, 730 274, 1560 493" stroke="url(#siteRibbon)" stroke-opacity=".55" stroke-width="2" stroke-linecap="round" />
        </svg>
        <div class="site-container hero__inner">
          <div class="hero__content">
            <div class="eyebrow"><span /> 你的智能财务伙伴</div>
            <h1>让每一笔钱，<br /><em>都清晰可见。</em></h1>
            <p class="hero__lead">
              从日常收支到转账管理，卡雅把复杂的财务信息变得简单、直观，让你更从容地做出每一个决定。
            </p>
            <div class="hero__buttons">
              <button class="button button--primary" type="button" @click="openApp">
                立即体验 <span aria-hidden="true">→</span>
              </button>
              <button class="button button--light" type="button" @click="scrollTo('features')">
                探索功能
              </button>
            </div>
            <div class="hero__trust">
              <span>✓ 安全加密</span><span>✓ 隐私保护</span><span>✓ AI 随时在线</span>
            </div>
          </div>

          <div class="hero__visual" aria-label="卡雅 App 界面演示">
            <div class="floating-card floating-card--balance">
              <span class="floating-card__icon">↗</span>
              <div><small>本月结余</small><strong>+26.8%</strong></div>
            </div>
            <div class="floating-card floating-card--ai">
              <span class="ai-pulse" /> AI 智能客服 <strong>在线</strong>
            </div>

            <div class="phone">
              <div class="phone__speaker" />
              <div class="phone__screen">
                <div class="demo-label">界面演示</div>
                <div class="demo-header"><span class="demo-logo">K</span><span>我的钱包</span><i>•••</i></div>
                <div class="balance-card">
                  <span>总余额</span>
                  <strong>¥ 12,680.00</strong>
                  <small>较上月 +¥ 1,280.00</small>
                  <div class="balance-card__actions"><span>↗ 转账</span><span>＋ 记一笔</span></div>
                </div>
                <div class="demo-section-title"><strong>收支概览</strong><span>本月</span></div>
                <div class="demo-chart">
                  <div class="demo-donut"><div><strong>68%</strong><span>储蓄率</span></div></div>
                  <div class="demo-legend">
                    <span><i class="legend-dot legend-dot--teal" />收入 <b>¥8,600</b></span>
                    <span><i class="legend-dot legend-dot--green" />支出 <b>¥2,760</b></span>
                    <span><i class="legend-dot legend-dot--gray" />转账 <b>¥1,200</b></span>
                  </div>
                </div>
                <div class="demo-section-title"><strong>最近交易</strong><span>全部</span></div>
                <div class="demo-transaction"><i>餐</i><div><strong>餐饮美食</strong><span>今天 12:30</span></div><b>-¥38.00</b></div>
                <div class="demo-transaction"><i class="income">薪</i><div><strong>工资收入</strong><span>11 月 10 日</span></div><b class="income-text">+¥8,600</b></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="value-strip">
        <div class="site-container value-strip__inner">
          <span>一个账户，管理所有收支</span><i />
          <span>清晰图表，读懂消费习惯</span><i />
          <span>智能助手，随时解答疑问</span>
        </div>
      </section>

      <section id="features" class="section site-container">
        <div class="section-heading">
          <div><span class="section-kicker">PRODUCT FEATURES</span><h2>财务管理，本该如此简单</h2></div>
          <p>少一点繁琐，多一点清晰。卡雅把你真正需要的功能，放在触手可及的地方。</p>
        </div>

        <div class="feature-grid">
          <article v-for="(feature, index) in features" :key="feature.title" class="feature-card" :class="feature.className">
            <span class="feature-card__number">0{{ index + 1 }}</span>
            <div class="feature-card__icon">
              <svg v-if="feature.icon === 'chart'" viewBox="0 0 24 24"><path d="M4 19V9m6 10V5m6 14v-7m4 7H2" /></svg>
              <svg v-else-if="feature.icon === 'transfer'" viewBox="0 0 24 24"><path d="M7 7h12l-3-3m3 3-3 3M17 17H5l3 3m-3-3 3-3" /></svg>
              <svg v-else viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="3" /><path d="M3 10h18M7 15h4" /></svg>
            </div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
            <button type="button" @click="openApp">立即体验 <span>↗</span></button>
          </article>
        </div>
      </section>

      <section id="assistant" class="assistant-section">
        <div class="site-container assistant-grid">
          <div class="assistant-demo">
            <div class="assistant-window">
              <div class="assistant-window__header"><span class="brand__logo">K</span><div><strong>卡雅 AI 客服</strong><small><i /> 随时在线</small></div><span>•••</span></div>
              <div class="chat-message chat-message--user">这个月我的支出主要在哪里？</div>
              <div class="chat-message chat-message--agent">从收支概览看，你可以按分类查看本月支出构成。点击具体分类，还能看到每一笔交易明细。</div>
              <div class="chat-message chat-message--user">怎么查看转账回单？</div>
              <div class="chat-message chat-message--agent">进入「交易记录」，选择对应转账即可查看电子回单。如果需要人工核实，我也可以为你提供联系方式。</div>
              <div class="assistant-input"><span>请输入你的问题…</span><b>发送</b></div>
            </div>
          </div>
          <div class="assistant-content">
            <span class="section-kicker section-kicker--light">AI ASSISTANT</span>
            <h2>不止回答问题，<br />更懂你的使用场景</h2>
            <p>无论是查找交易、了解功能，还是处理转账疑问，AI 客服都能结合卡雅的使用场景给出清晰指引。</p>
            <ul>
              <li><span>01</span><div><strong>自然对话</strong><p>像和朋友聊天一样，直接说出你的问题。</p></div></li>
              <li><span>02</span><div><strong>上下文理解</strong><p>连续追问也能理解，不需要反复描述。</p></div></li>
              <li><span>03</span><div><strong>人工支持</strong><p>复杂问题无缝获取人工客服联系方式。</p></div></li>
            </ul>
          </div>
        </div>
      </section>

      <section id="security" class="section site-container security-section">
        <div class="security-visual">
          <div class="security-orbit security-orbit--outer"><span>隐私</span><span>加密</span></div>
          <div class="security-orbit security-orbit--inner" />
          <div class="security-shield">
            <svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><path d="m9 12 2 2 4-5" /></svg>
          </div>
        </div>
        <div class="security-content">
          <span class="section-kicker">SECURITY BY DESIGN</span>
          <h2>你的信任，<br />值得被认真守护</h2>
          <p>从登录到每一笔操作，卡雅把安全与隐私融入产品设计的每个细节。</p>
          <div class="security-points">
            <div><span>✓</span><div><strong>身份认证保护</strong><p>账户访问与敏感操作均需进行身份校验。</p></div></div>
            <div><span>✓</span><div><strong>敏感信息提醒</strong><p>客服不会索取密码、验证码或完整卡号。</p></div></div>
            <div><span>✓</span><div><strong>隐私数据最小化</strong><p>只收集提供产品服务所必需的信息。</p></div></div>
          </div>
        </div>
      </section>

      <section id="faq" class="section faq-section">
        <div class="site-container faq-grid">
          <div class="faq-heading"><span class="section-kicker">FAQ</span><h2>你可能还想了解</h2><p>没有找到答案？AI 客服随时准备帮助你。</p><button type="button" @click="openApp">咨询智能客服 →</button></div>
          <div class="faq-list">
            <article v-for="(item, index) in faqs" :key="item.question" class="faq-item" :class="{ 'is-open': activeFaq === index }">
              <button type="button" :aria-expanded="activeFaq === index" @click="toggleFaq(index)">
                <span>{{ item.question }}</span><i>{{ activeFaq === index ? '−' : '+' }}</i>
              </button>
              <p v-if="activeFaq === index">{{ item.answer }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="site-container final-cta">
        <div class="final-cta__glow" />
        <span class="final-cta__logo">K</span>
        <h2>准备好，更从容地管理每一笔钱了吗？</h2>
        <p>现在就打开卡雅，让清晰的财务生活从今天开始。</p>
        <button class="button button--white" type="button" @click="openApp">免费开始使用 <span>→</span></button>
      </section>
    </main>

    <footer class="site-footer">
      <div class="site-container footer-main">
        <div class="footer-brand"><div class="brand"><span class="brand__logo">K</span><span class="brand__name">卡雅 <small>KAYA</small></span></div><p>让每一笔收支都清晰可见。</p></div>
        <div class="footer-links"><strong>产品</strong><button @click="scrollTo('features')">产品功能</button><button @click="scrollTo('security')">安全保障</button><button @click="openApp">打开 App</button></div>
        <div class="footer-links"><strong>支持</strong><button @click="scrollTo('assistant')">智能客服</button><button @click="scrollTo('faq')">常见问题</button><a href="mailto:support@kaya.app">联系客服</a></div>
        <div class="footer-contact"><strong>保持联系</strong><a href="mailto:support@kaya.app">support@kaya.app</a><span>服务时间 每天 9:00–22:00</span></div>
      </div>
      <div class="site-container footer-bottom"><span>© 2026 卡雅科技 保留所有权利</span><span>简单 · 清晰 · 可信赖</span></div>
    </footer>
  </div>
</template>

<style scoped>
.official-site {
  --teal: #17b7bd;
  --teal-dark: #0e8f94;
  --lime: #9deb93;
  --ink: #101f1d;
  --muted: #6f7c79;
  --paper: #f7f6f8;
  min-height: 100%;
  overflow: hidden;
  background: var(--paper);
  color: var(--ink);
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.official-site button { color: inherit; }
.site-container { width: min(1160px, calc(100% - 48px)); margin: 0 auto; }
.site-header { position: sticky; top: 0; z-index: 50; background: rgba(255,255,255,.88); border-bottom: 1px solid rgba(21,50,56,.07); backdrop-filter: blur(18px); }
.header-inner { height: 76px; display: flex; align-items: center; justify-content: space-between; }
.brand { display: inline-flex; align-items: center; gap: 11px; border: 0; background: none; padding: 0; cursor: pointer; }
.brand__logo { display: inline-flex; align-items: center; justify-content: center; width: 38px; height: 38px; border-radius: 12px; background: linear-gradient(135deg,#12aeb4 0%,#17b7bd 45%,#8ee79a 100%); color: #fff; font-size: 19px; font-weight: 700; box-shadow: 0 7px 18px rgba(23,183,189,.28); }
.brand__name { font-size: 19px; font-weight: 700; letter-spacing: .02em; }
.brand__name small { margin-left: 3px; color: #92a0a2; font-size: 10px; font-weight: 600; letter-spacing: .15em; }
.desktop-nav { display: flex; align-items: center; gap: 34px; }
.desktop-nav button { padding: 8px 0; border: 0; background: none; color: #5e7377; font-size: 13px; cursor: pointer; transition: color .2s; }
.desktop-nav button:hover { color: var(--teal); }
.header-actions { display: flex; align-items: center; }
.button { display: inline-flex; align-items: center; justify-content: center; gap: 12px; min-height: 48px; padding: 0 24px; border: 0; border-radius: 14px; font-size: 14px; font-weight: 600; cursor: pointer; transition: transform .2s, box-shadow .2s; }
.button:hover { transform: translateY(-2px); }
.button--small { min-height: 40px; padding: 0 18px; border-radius: 11px; font-size: 12px; }
.button--dark { background-image: linear-gradient(120deg,#12aeb4 0%,#17b7bd 45%,#7ee39b 100%); color: #fff !important; box-shadow: 0 10px 24px rgba(23,183,189,.30); }
.button--primary { background-image: linear-gradient(120deg,#12aeb4 0%,#17b7bd 45%,#7ee39b 100%); color: #fff !important; box-shadow: 0 12px 28px rgba(23,183,189,.30); }
.button--light { background: #fff; border: 1px solid #e8ece9; color: #475467 !important; }
.button--white { background: #fff; color: var(--ink) !important; box-shadow: 0 12px 30px rgba(0,0,0,.12); }
.menu-toggle, .mobile-nav { display: none; }

.hero { position: relative; isolation: isolate; background: radial-gradient(circle at 75% 35%,#eaf8f3 0,transparent 40%),linear-gradient(180deg,#f9faf9,var(--paper)); }
.hero::before { content: ''; position: absolute; inset: 0; z-index: -1; opacity: .35; background-image: linear-gradient(rgba(23,183,189,.06) 1px,transparent 1px),linear-gradient(90deg,rgba(23,183,189,.06) 1px,transparent 1px); background-size: 52px 52px; mask-image: linear-gradient(to right,#000,transparent 65%); }
.hero__glow { position: absolute; z-index: -1; border-radius: 50%; filter: blur(2px); }
.hero__glow--one { width: 320px; height: 320px; left: -180px; top: 140px; background: rgba(23,183,189,.10); }
.hero__glow--two { width: 240px; height: 240px; right: -100px; bottom: 20px; background: rgba(157,235,147,.14); }
.hero__ribbon { position: absolute; inset: 0; z-index: -1; width: 100%; height: 100%; pointer-events: none; }
.hero__inner { min-height: 690px; display: grid; grid-template-columns: 1fr 1fr; align-items: center; gap: 60px; padding-top: 54px; padding-bottom: 64px; }
.eyebrow { display: inline-flex; align-items: center; gap: 9px; padding: 7px 12px; border: 1px solid rgba(23,183,189,.22); border-radius: 30px; background: rgba(255,255,255,.75); color: var(--teal-dark); font-size: 11px; font-weight: 600; }
.eyebrow span { width: 6px; height: 6px; border-radius: 50%; background: var(--teal); box-shadow: 0 0 0 4px rgba(23,183,189,.14); }
.hero h1 { margin: 24px 0 20px; font-size: clamp(42px,5vw,64px); line-height: 1.16; letter-spacing: -.045em; }
.hero h1 em { font-style: normal; background: linear-gradient(100deg,#12aeb4 0%,#17b7bd 45%,#6fd9ab 100%); -webkit-background-clip: text; background-clip: text; color: transparent; }
.hero__content { min-width: 0; }
.hero__lead { max-width: 500px; margin: 0; color: var(--muted); font-size: 16px; line-height: 1.9; }
.hero__buttons { display: flex; gap: 12px; margin-top: 32px; }
.hero__trust { display: flex; flex-wrap: wrap; gap: 20px; margin-top: 26px; color: #829396; font-size: 11px; }
.hero__trust span::first-letter { color: var(--teal); }
.hero__visual { position: relative; display: flex; justify-content: center; padding: 24px 0; }
.phone { position: relative; width: 292px; padding: 9px; border: 1px solid rgba(255,255,255,.7); border-radius: 42px; background: var(--ink); box-shadow: 0 40px 80px rgba(16,24,40,.22),0 8px 24px rgba(16,24,40,.12); transform: rotate(2deg); }
.phone__speaker { position: absolute; z-index: 2; top: 16px; left: 50%; width: 76px; height: 20px; border-radius: 20px; background: var(--ink); transform: translateX(-50%); }
.phone__screen { min-height: 530px; padding: 32px 15px 18px; border-radius: 34px; background: #f7f8f8; overflow: hidden; }
.demo-label { margin-bottom: 12px; color: #a5b0b0; font-size: 8px; text-align: right; }
.demo-header { display: flex; align-items: center; gap: 8px; margin-bottom: 18px; font-size: 11px; font-weight: 600; }
.demo-header i { margin-left: auto; color: #9ba7a7; letter-spacing: 2px; }
.demo-logo { display: inline-flex; align-items: center; justify-content: center; width: 25px; height: 25px; border-radius: 8px; background: linear-gradient(135deg,#12aeb4 0%,#17b7bd 45%,#8ee79a 100%); color: #fff; font-size: 11px; }
.balance-card { padding: 19px; border-radius: 20px; background: linear-gradient(150deg,#0f3b37 0%,#0a2523 100%); color: #fff; box-shadow: 0 15px 30px rgba(11,29,27,.18); }
.balance-card > span { font-size: 9px; opacity: .8; }
.balance-card > strong { display: block; margin: 5px 0; font-size: 25px; letter-spacing: -.03em; }
.balance-card > small { font-size: 8px; opacity: .75; }
.balance-card__actions { display: flex; gap: 9px; margin-top: 17px; }
.balance-card__actions span { flex: 1; padding: 8px; border-radius: 9px; background: rgba(255,255,255,.16); font-size: 9px; text-align: center; }
.demo-section-title { display: flex; justify-content: space-between; margin: 18px 2px 10px; font-size: 9px; }
.demo-section-title span { color: #9aa6a6; font-size: 8px; }
.demo-chart { display: flex; align-items: center; gap: 19px; padding: 13px; border-radius: 15px; background: #fff; }
.demo-donut { display: grid; place-items: center; width: 75px; height: 75px; flex: none; border-radius: 50%; background: conic-gradient(var(--teal) 0 68%,var(--lime) 68% 88%,#e8eeee 88%); }
.demo-donut::before { content: ''; grid-area: 1/1; width: 51px; height: 51px; border-radius: 50%; background: #fff; }
.demo-donut div { z-index: 1; grid-area: 1/1; text-align: center; }
.demo-donut strong { display: block; font-size: 13px; }
.demo-donut span { color: #9aa6a6; font-size: 7px; }
.demo-legend { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.demo-legend > span { display: flex; align-items: center; color: #879595; font-size: 7px; }
.demo-legend b { margin-left: auto; color: #40575b; font-size: 8px; }
.legend-dot { width: 5px; height: 5px; margin-right: 5px; border-radius: 50%; }
.legend-dot--teal { background: var(--teal); }.legend-dot--green { background: var(--lime); }.legend-dot--gray { background: #e8eeee; }
.demo-transaction { display: flex; align-items: center; gap: 9px; padding: 9px 3px; border-bottom: 1px solid #edf0ef; }
.demo-transaction > i { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 9px; background: #fff1e8; color: #e99862; font-size: 9px; font-style: normal; }
.demo-transaction > i.income { background: #e8f8ef; color: #4fb982; }
.demo-transaction div { display: flex; flex-direction: column; gap: 2px; }
.demo-transaction strong { font-size: 9px; }.demo-transaction span { color: #a3aeae; font-size: 7px; }
.demo-transaction > b { margin-left: auto; font-size: 9px; }.income-text { color: #42af7a; }
.floating-card { position: absolute; z-index: 3; display: flex; align-items: center; gap: 10px; padding: 12px 15px; border: 1px solid rgba(255,255,255,.9); border-radius: 15px; background: rgba(255,255,255,.88); box-shadow: 0 15px 35px rgba(32,76,75,.12); backdrop-filter: blur(12px); }
.floating-card--balance { top: 90px; left: -10px; }
.floating-card__icon { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 10px; background: #e7f8ef; color: #3fb780; }
.floating-card small { display: block; color: #8b9a9c; font-size: 8px; }.floating-card strong { color: #35ad76; font-size: 14px; }
.floating-card--ai { right: -12px; bottom: 80px; color: #52696d; font-size: 9px; }
.floating-card--ai strong { margin-left: 5px; color: var(--teal); font-size: 9px; }
.ai-pulse { width: 7px; height: 7px; border-radius: 50%; background: #48ca8c; box-shadow: 0 0 0 5px rgba(72,202,140,.12); }
.value-strip { border-top: 1px solid #edf2f0; border-bottom: 1px solid #edf2f0; background: #fff; }
.value-strip__inner { min-height: 76px; display: flex; align-items: center; justify-content: center; gap: 36px; color: #6f8285; font-size: 12px; }
.value-strip__inner i { width: 4px; height: 4px; border-radius: 50%; background: var(--teal); }
.section { padding-top: 110px; padding-bottom: 110px; scroll-margin-top: 70px; }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 60px; margin-bottom: 42px; }
.section-kicker { color: var(--teal); font-size: 10px; font-weight: 700; letter-spacing: .18em; }
.section-heading h2, .security-content h2, .faq-heading h2 { margin: 12px 0 0; font-size: clamp(30px,3.5vw,42px); line-height: 1.25; letter-spacing: -.035em; }
.section-heading > p { max-width: 400px; margin: 0 0 5px; color: var(--muted); font-size: 13px; line-height: 1.9; }
.feature-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 20px; }
.feature-card { position: relative; min-height: 330px; padding: 32px; border: 1px solid #e8ece9; border-radius: 25px; overflow: hidden; background: #fff; transition: transform .25s,box-shadow .25s; }
.feature-card:hover { transform: translateY(-6px); box-shadow: 0 24px 45px rgba(16,24,40,.08); }
.feature-card--mint { background: #fff; }.feature-card--blue { background: #fff; }.feature-card--lime { background: #fff; }
.feature-card__number { position: absolute; top: 25px; right: 28px; color: rgba(21,50,56,.13); font-size: 12px; font-weight: 600; }
.feature-card__icon { display: grid; place-items: center; width: 58px; height: 58px; margin-bottom: 48px; border-radius: 18px; background: #fff; color: var(--teal); box-shadow: 0 10px 25px rgba(29,88,81,.08); }
.feature-card__icon svg { width: 27px; height: 27px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.feature-card h3 { margin: 0 0 12px; font-size: 21px; }
.feature-card p { margin: 0; color: #718487; font-size: 12px; line-height: 1.85; }
.feature-card button { margin-top: 24px; padding: 0; border: 0; background: none; color: var(--teal-dark); font-size: 11px; font-weight: 600; cursor: pointer; }
.feature-card button span { margin-left: 7px; }
.assistant-section { padding: 110px 0; scroll-margin-top: 70px; background: var(--ink); color: #fff; }
.assistant-grid { display: grid; grid-template-columns: 1fr 1fr; align-items: center; gap: 90px; }
.assistant-demo { position: relative; }
.assistant-demo::before { content: ''; position: absolute; inset: 10% -10%; background: radial-gradient(circle,rgba(70,211,185,.18),transparent 65%); }
.assistant-window { position: relative; max-width: 440px; padding: 22px; border: 1px solid rgba(255,255,255,.14); border-radius: 25px; background: rgba(255,255,255,.07); box-shadow: 0 35px 70px rgba(0,0,0,.15); backdrop-filter: blur(10px); }
.assistant-window__header { display: flex; align-items: center; gap: 11px; padding-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,.09); }
.assistant-window__header .brand__logo { width: 35px; height: 35px; font-size: 15px; }
.assistant-window__header div { display: flex; flex-direction: column; gap: 4px; }
.assistant-window__header strong { font-size: 12px; }.assistant-window__header small { color: #94b3b3; font-size: 8px; }
.assistant-window__header small i { display: inline-block; width: 5px; height: 5px; margin-right: 4px; border-radius: 50%; background: #62d9a0; }
.assistant-window__header > span:last-child { margin-left: auto; color: #8ba8a9; letter-spacing: 2px; }
.chat-message { width: fit-content; max-width: 82%; margin-top: 18px; padding: 12px 14px; font-size: 10px; line-height: 1.75; }
.chat-message--user { margin-left: auto; border-radius: 14px 14px 4px 14px; background: var(--teal); }
.chat-message--agent { border-radius: 14px 14px 14px 4px; background: rgba(255,255,255,.1); color: #d5e2e1; }
.assistant-input { display: flex; align-items: center; margin-top: 22px; padding: 7px 7px 7px 14px; border: 1px solid rgba(255,255,255,.1); border-radius: 12px; background: rgba(0,0,0,.1); color: #799b9c; font-size: 9px; }
.assistant-input b { margin-left: auto; padding: 7px 13px; border-radius: 8px; background: var(--teal); color: #fff; font-weight: 500; }
.section-kicker--light { color: #8fc9c2; }
.assistant-content h2 { margin: 14px 0 20px; font-size: clamp(30px,3.5vw,43px); line-height: 1.3; letter-spacing: -.035em; }
.assistant-content > p { margin: 0; color: #a6bdbc; font-size: 13px; line-height: 1.9; }
.assistant-content ul { margin: 30px 0 0; padding: 0; list-style: none; }
.assistant-content li { display: flex; gap: 15px; margin-top: 21px; }
.assistant-content li > span { display: grid; place-items: center; flex: none; width: 32px; height: 32px; border: 1px solid rgba(143,201,194,.25); border-radius: 10px; color: #8fc9c2; font-size: 9px; }
.assistant-content li strong { font-size: 12px; }.assistant-content li p { margin: 5px 0 0; color: #90abaa; font-size: 10px; }
.security-section { display: grid; grid-template-columns: 1fr 1fr; align-items: center; gap: 100px; }
.security-visual { position: relative; display: grid; place-items: center; min-height: 390px; border-radius: 35px; background: radial-gradient(circle at center,#effaf5,#f5faf8 65%,transparent 66%); }
.security-shield { z-index: 2; display: grid; place-items: center; width: 130px; height: 130px; border-radius: 38px; background: var(--ink); box-shadow: 0 25px 50px rgba(11,29,27,.24); transform: rotate(-5deg); }
.security-shield svg { width: 70px; height: 70px; fill: none; stroke: #fff; stroke-width: 1.25; stroke-linecap: round; stroke-linejoin: round; }
.security-orbit { position: absolute; border: 1px dashed rgba(23,183,189,.26); border-radius: 50%; }
.security-orbit--outer { width: 320px; height: 320px; }.security-orbit--inner { width: 230px; height: 230px; }
.security-orbit--outer span { position: absolute; padding: 7px 12px; border-radius: 20px; background: #fff; color: #699084; font-size: 9px; box-shadow: 0 8px 20px rgba(33,75,66,.08); }
.security-orbit--outer span:first-child { top: 45px; right: 10px; }.security-orbit--outer span:last-child { left: 0; bottom: 50px; }
.security-content > p { margin: 20px 0 0; color: var(--muted); font-size: 13px; line-height: 1.9; }
.security-points { margin-top: 28px; }
.security-points > div { display: flex; gap: 13px; margin-top: 20px; }
.security-points > div > span { display: grid; place-items: center; flex: none; width: 24px; height: 24px; border-radius: 50%; background: #e9f7f2; color: var(--teal); font-size: 11px; }
.security-points strong { font-size: 13px; }.security-points p { margin: 5px 0 0; color: #839295; font-size: 11px; }
.faq-section { background: var(--paper); }
.faq-grid { display: grid; grid-template-columns: .8fr 1.2fr; gap: 100px; }
.faq-heading > p { margin: 18px 0; color: var(--muted); font-size: 12px; }
.faq-heading > button { padding: 0; border: 0; background: none; color: var(--teal-dark); font-size: 12px; font-weight: 600; cursor: pointer; }
.faq-item { border-bottom: 1px solid #dfe8e5; }
.faq-item > button { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 23px 0; border: 0; background: none; text-align: left; cursor: pointer; }
.faq-item > button span { font-size: 14px; font-weight: 600; }.faq-item > button i { display: grid; place-items: center; width: 27px; height: 27px; border-radius: 50%; background: #e8efed; color: #6f8583; font-size: 17px; font-style: normal; font-weight: 400; }
.faq-item.is-open > button i { background: var(--teal); color: #fff; }
.faq-item > p { margin: -5px 45px 23px 0; color: var(--muted); font-size: 12px; line-height: 1.9; }
.final-cta { position: relative; isolation: isolate; margin-top: 100px; margin-bottom: 100px; padding: 65px 30px; border-radius: 32px; background: var(--ink); color: #fff; text-align: center; overflow: hidden; }
.final-cta__glow { position: absolute; z-index: -1; width: 500px; height: 500px; right: -120px; top: -300px; border: 1px solid rgba(255,255,255,.15); border-radius: 50%; box-shadow: 0 0 0 60px rgba(255,255,255,.035),0 0 0 120px rgba(255,255,255,.025); }
.final-cta__logo { display: grid; place-items: center; width: 46px; height: 46px; margin: 0 auto 20px; border-radius: 14px; background: rgba(255,255,255,.18); font-size: 21px; font-weight: 700; }
.final-cta h2 { margin: 0; font-size: clamp(27px,3.5vw,39px); letter-spacing: -.035em; }.final-cta p { margin: 16px 0 28px; color: rgba(255,255,255,.78); font-size: 13px; }
.site-footer { background: var(--ink); color: #fff; }
.footer-main { display: grid; grid-template-columns: 1.7fr 1fr 1fr 1.3fr; gap: 50px; padding-top: 60px; padding-bottom: 50px; }
.footer-brand p { margin: 18px 0 0; color: #789798; font-size: 11px; }.footer-brand .brand__name small { color: #668789; }
.footer-links, .footer-contact { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
.footer-links strong, .footer-contact strong { margin-bottom: 6px; color: #d9e7e6; font-size: 11px; }
.footer-links button, .footer-links a, .footer-contact a, .footer-contact span { padding: 0; border: 0; background: none; color: #7f9c9d; font-size: 10px; cursor: pointer; }
.footer-links button:hover, .footer-links a:hover { color: #fff; }
.footer-bottom { display: flex; justify-content: space-between; padding-top: 22px; padding-bottom: 25px; border-top: 1px solid rgba(255,255,255,.07); color: #5f7e80; font-size: 9px; }

@media (max-width: 900px) {
  .desktop-nav, .desktop-cta { display: none; }
  .menu-toggle { display: flex; flex-direction: column; justify-content: center; gap: 6px; width: 40px; height: 40px; padding: 10px; border: 0; background: none; }
  .menu-toggle i { width: 20px; height: 1.5px; background: var(--ink); transition: transform .2s; }.menu-toggle i.is-open:first-child { transform: translateY(3.75px) rotate(45deg); }.menu-toggle i.is-open:last-child { transform: translateY(-3.75px) rotate(-45deg); }
  .mobile-nav { display: flex; flex-direction: column; gap: 3px; padding: 10px 24px 20px; border-top: 1px solid #edf2f0; background: #fff; }
  .mobile-nav > button:not(.button) { display: flex; justify-content: space-between; padding: 14px 0; border: 0; border-bottom: 1px solid #eef2f1; background: none; font-size: 13px; text-align: left; }
  .mobile-nav .button { margin-top: 12px; }
  .hero__inner { grid-template-columns: minmax(0,1fr); gap: 45px; padding-top: 70px; text-align: center; }
  .hero__content { display: flex; flex-direction: column; align-items: center; width: 100%; }.hero__lead { width: 100%; max-width: 580px; }.hero__trust { justify-content: center; }
  .hero__visual { width: min(100%,500px); min-width: 0; max-width: 100%; margin: 0 auto; }.floating-card--balance { left: 0; }.floating-card--ai { right: 0; }
  .section-heading { align-items: flex-start; flex-direction: column; gap: 18px; }.section-heading > p { max-width: 580px; }
  .feature-grid { grid-template-columns: 1fr; }.feature-card { min-height: 0; }.feature-card__icon { margin-bottom: 30px; }
  .assistant-grid, .security-section { grid-template-columns: 1fr; gap: 55px; }.assistant-window { margin: 0 auto; }.security-content { order: -1; }
  .faq-grid { grid-template-columns: 1fr; gap: 40px; }.footer-main { grid-template-columns: 1.5fr 1fr 1fr; }.footer-contact { grid-column: 1/-1; }
}

@media (max-width: 560px) {
  .site-container { width: min(100% - 32px,1160px); }.header-inner { height: 66px; }.brand__logo { width: 34px; height: 34px; border-radius: 10px; }.brand__name { font-size: 17px; }
  .hero__inner { min-height: 0; padding-top: 54px; padding-bottom: 45px; }.hero h1 { margin-top: 20px; font-size: 40px; }.hero__lead { font-size: 14px; line-height: 1.8; }.hero__buttons { width: 100%; }.hero__buttons .button { flex: 1; padding: 0 15px; }.hero__trust { gap: 11px; font-size: 9px; }
  .hero__visual { transform: scale(.88); margin: -25px auto -35px; }.floating-card--balance { left: -14px; }.floating-card--ai { right: -15px; }
  .value-strip__inner { min-height: 65px; gap: 12px; font-size: 9px; }.value-strip__inner span { max-width: 90px; text-align: center; }
  .section { padding-top: 75px; padding-bottom: 75px; }.section-heading h2, .security-content h2, .faq-heading h2 { font-size: 30px; }.feature-card { padding: 26px; }
  .assistant-section { padding: 75px 0; }.assistant-grid { gap: 45px; }.assistant-window { padding: 16px; }.assistant-content h2 { font-size: 31px; }
  .security-visual { min-height: 310px; transform: scale(.9); }.security-section { gap: 25px; }
  .final-cta { width: calc(100% - 24px); margin-top: 65px; margin-bottom: 65px; padding: 48px 22px; border-radius: 25px; }.final-cta h2 { font-size: 27px; line-height: 1.45; }.final-cta p { font-size: 11px; line-height: 1.8; }
  .footer-main { grid-template-columns: 1fr 1fr; gap: 35px; }.footer-brand { grid-column: 1/-1; }.footer-contact { grid-column: 1/-1; }.footer-bottom { gap: 15px; flex-direction: column; }
}
</style>
