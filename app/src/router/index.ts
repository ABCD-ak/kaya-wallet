import { createRouter, createWebHistory } from 'vue-router'

import { getToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      alias: '/official',
      name: 'OfficialSite',
      component: () => import('@/views/OfficialSitePage.vue'),
    },
    {
      path: '/onboarding',
      name: 'Onboarding',
      component: () => import('@/views/onboarding/OnboardingPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/auth',
      name: 'AuthLanding',
      component: () => import('@/views/auth/AuthLandingPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/auth/ForgotPasswordPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/reset-password',
      name: 'ResetPassword',
      component: () => import('@/views/auth/ResetPasswordPage.vue'),
    },
    {
      path: '/wallet',
      name: 'Wallet',
      component: () => import('@/views/wallet/WalletPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transactions',
      name: 'Transactions',
      component: () => import('@/views/transactions/TransactionListPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transactions/new',
      name: 'NewTransaction',
      component: () => import('@/views/transactions/NewTransactionPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transactions/:id',
      name: 'TransactionDetail',
      component: () => import('@/views/transactions/TransactionDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transfers',
      name: 'Transfer',
      component: () => import('@/views/transfers/TransferPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transfers/new',
      name: 'SendMoney',
      component: () => import('@/views/transfers/SendMoneyPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/recipients/new',
      name: 'RecipientNew',
      component: () => import('@/views/transfers/RecipientNewPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transfers/new-amount',
      name: 'AddAmount',
      component: () => import('@/views/transfers/AddAmountPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/transfers/success',
      name: 'MoneySent',
      component: () => import('@/views/transfers/MoneySentPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/receipts/:id',
      name: 'Receipt',
      component: () => import('@/views/transfers/ReceiptPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/receive',
      name: 'Receive',
      component: () => import('@/views/transfers/ReceivePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/cards',
      name: 'Cards',
      component: () => import('@/views/cards/CardsPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/cards/:id',
      name: 'CreditCardDetail',
      component: () => import('@/views/cards/CreditCardPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/wallet/add',
      name: 'WalletAction',
      component: () => import('@/views/wallet/WalletActionPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/categories/:categoryId',
      name: 'CategoryDetail',
      component: () => import('@/views/transactions/CategoryDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/notifications',
      name: 'Notifications',
      component: () => import('@/views/notifications/NotificationsPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/account',
      name: 'Account',
      component: () => import('@/views/account/AccountPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/account/edit',
      name: 'AccountEdit',
      component: () => import('@/views/account/AccountEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/addresses',
      name: 'Addresses',
      component: () => import('@/views/account/AddressesPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/verifications',
      name: 'Verifications',
      component: () => import('@/views/account/VerificationsPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/account/payment-password',
      name: 'PaymentPassword',
      component: () => import('@/views/account/PaymentPasswordPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/support',
      name: 'Support',
      component: () => import('@/views/account/SupportPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/support/chat',
      name: 'SupportChat',
      component: () => import('@/views/account/SupportChatPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'About',
      component: () => import('@/views/account/AboutPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/p/:slug',
      name: 'Prototype',
      component: () => import('@/prototype/PrototypeScreen.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/onboarding' },
  ],
})

router.beforeEach((to) => {
  const authenticated = !!getToken()

  if (to.meta.requiresAuth && !authenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && authenticated) {
    return { path: '/wallet' }
  }
  return true
})

export default router
