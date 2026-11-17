import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import type { AuthTokens } from './api/types'
import { useAuthStore } from './stores/auth'
import { AUTH_REFRESHED_EVENT, AUTH_UNAUTHORIZED_EVENT } from './utils/request'
import './styles/index.scss'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

const auth = useAuthStore(pinia)

// 请求层刷新令牌成功后，同步 Pinia 中的令牌与用户信息
window.addEventListener(AUTH_REFRESHED_EVENT, (event) => {
  const tokens = (event as CustomEvent<AuthTokens>).detail
  if (tokens) auth.applyTokens(tokens)
})

// 请求层判断登录态失效后，清理本地状态并回到登录页
window.addEventListener(AUTH_UNAUTHORIZED_EVENT, () => {
  auth.reset()
  const current = router.currentRoute.value
  if (current.name === 'Login' || current.name === 'Register') return
  void router.replace({ path: '/login', query: { redirect: current.fullPath } })
})

app.mount('#app')
