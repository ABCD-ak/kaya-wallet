<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import PageLayout from '@/components/PageLayout.vue'
import request from '@/utils/request'

interface ChatMessage {
  id: number
  role: 'agent' | 'user'
  text: string
  time: string
}

const router = useRouter()
const STORAGE_KEY = 'kaya-support-chat'

const messages = ref<ChatMessage[]>([])
const input = ref('')
const sending = ref(false)
/** 免费 AI 通道限流后的冷却剩余秒数，>0 时禁止再次发送 */
const cooldown = ref(0)
const listRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
let seq = 0
let abortController: AbortController | null = null
let cooldownTimer: number | null = null

const quickQuestions = ['转账未到账', '提升转账额度', '卡片丢失', '联系人工客服']

function now(): string {
  const d = new Date()
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function push(role: ChatMessage['role'], text: string) {
  messages.value.push({ id: ++seq, role, text, time: now() })
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

interface AiReply {
  reply: string
  /** 免费通道限流时，后端返回还需等待的秒数 */
  retryAfter?: number
}

/** 经项目后端调用免费 AI，无需 API Key，也避免浏览器跨域限制 */
async function requestAiReply(): Promise<AiReply> {
  abortController = new AbortController()
  const history = messages.value.slice(-10).map((msg) => ({
    role: msg.role === 'user' ? 'user' : 'assistant',
    content: msg.text,
  }))

  try {
    // 上游首次回答可能排队 40s 以上，前端超时必须大于后端 45s 的单次等待
    const data = await request.post<AiReply>(
      '/ai/chat',
      { messages: history },
      { timeout: 75_000, signal: abortController.signal },
    )
    if (!data.reply?.trim()) throw new Error('AI 返回内容为空')
    return { reply: data.reply.trim(), retryAfter: data.retryAfter }
  } finally {
    abortController = null
  }
}

function startCooldown(seconds?: number) {
  if (!seconds || seconds <= 0) return
  cooldown.value = Math.ceil(seconds)
  if (cooldownTimer) window.clearInterval(cooldownTimer)
  cooldownTimer = window.setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      cooldown.value = 0
      if (cooldownTimer) window.clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

async function send(text?: string) {
  const content = (text ?? input.value).trim()
  if (!content || sending.value || cooldown.value > 0) return

  push('user', content)
  input.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const { reply, retryAfter } = await requestAiReply()
    push('agent', reply)
    startCooldown(retryAfter)
  } catch (error) {
    const errorName = (error as { name?: string })?.name
    const timedOut = errorName === 'AbortError' || errorName === 'CanceledError'
    push(
      'agent',
      timedOut
        ? 'AI 响应超时了，请稍后重新发送。如需人工协助，可拨打 400-800-1234。'
        : 'AI 暂时无法连接，请检查网络后重试。请勿发送密码、验证码或完整卡号。',
    )
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function goBack() {
  // 直接打开聊天页时没有应用内上一页，避免浏览器退回 about:blank
  if (window.history.state?.back) {
    router.back()
  } else {
    router.replace('/support')
  }
}

// 输入框获得焦点（移动端弹出键盘）时，保持最新消息与输入区可见
function onFocusInput() {
  setTimeout(scrollToBottom, 300)
}

function onViewportResize() {
  scrollToBottom()
}

onMounted(() => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const saved = JSON.parse(raw) as ChatMessage[]
      if (Array.isArray(saved) && saved.length) {
        messages.value = saved
        seq = saved.reduce((max, m) => Math.max(max, m.id), 0)
      }
    }
  } catch {
    // 忽略损坏的本地记录
  }
  if (!messages.value.length) {
    push('agent', '你好，我是卡雅 AI 客服，已经可以和你自由对话。请输入问题，或点击下方常见问题。')
  }
  scrollToBottom()
  // 进入会话即自动聚焦，移动端会直接弹出键盘
  setTimeout(() => inputRef.value?.focus(), 350)
  window.visualViewport?.addEventListener('resize', onViewportResize)
})

onUnmounted(() => {
  abortController?.abort()
  if (cooldownTimer) window.clearInterval(cooldownTimer)
  window.visualViewport?.removeEventListener('resize', onViewportResize)
})

watch(
  messages,
  (value) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(value.slice(-50)))
    } catch {
      // 存储失败不影响使用
    }
  },
  { deep: true },
)
</script>

<template>
  <PageLayout title="在线客服" @back="goBack">
    <div class="chat-page">
      <div ref="listRef" class="chat-list">
        <p class="chat-notice">免费 AI 提供回答 · 请勿发送密码、验证码或完整卡号</p>
        <div v-for="msg in messages" :key="msg.id" class="chat-row" :class="`chat-row--${msg.role}`">
          <span v-if="msg.role === 'agent'" class="chat-avatar">K</span>
          <div class="chat-bubble">
            <p class="chat-text">{{ msg.text }}</p>
            <span class="chat-time">{{ msg.time }}</span>
          </div>
        </div>
        <div v-if="sending" class="chat-row chat-row--agent">
          <span class="chat-avatar">K</span>
          <div class="chat-bubble chat-bubble--typing" aria-label="AI 正在思考">
            <i /><i /><i />
            <span>AI 正在思考</span>
          </div>
        </div>
      </div>

      <div class="chat-composer">
        <div class="chat-quick">
          <button
            v-for="q in quickQuestions"
            :key="q"
            type="button"
            class="chat-quick__chip"
            :disabled="sending || cooldown > 0"
            @click="send(q)"
          >
            {{ q }}
          </button>
        </div>

        <div class="chat-input">
          <input
            ref="inputRef"
            v-model="input"
            type="text"
            placeholder="请输入你的问题…"
            maxlength="200"
            enterkeyhint="send"
            :disabled="sending"
            @focus="onFocusInput"
            @keyup.enter="send()"
          />
          <button
            type="button"
            class="chat-send"
            :disabled="sending || cooldown > 0 || !input.trim()"
            @click="send()"
          >
            {{ sending ? '等待中' : cooldown > 0 ? `${cooldown}s 后再试` : '发送' }}
          </button>
        </div>
        <p v-if="cooldown > 0" class="chat-cooldown">
          免费 AI 通道每 45 秒限 1 次，{{ cooldown }} 秒后可继续提问
        </p>
      </div>
    </div>
  </PageLayout>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.chat-row--user {
  justify-content: flex-end;
}

.chat-avatar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #17b7bd;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
}

.chat-bubble {
  max-width: 74%;
  padding: 10px 12px 6px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(24, 39, 43, 0.05);
}

.chat-row--user .chat-bubble {
  background: #17b7bd;
  box-shadow: none;
}

.chat-text {
  margin: 0;
  font-size: 13px;
  line-height: 20px;
  color: #353333;
  word-break: break-word;
}

.chat-row--user .chat-text {
  color: #ffffff;
}

.chat-time {
  display: block;
  margin-top: 4px;
  font-size: 10px;
  color: #afafaf;
  text-align: right;
}

.chat-row--user .chat-time {
  color: rgba(255, 255, 255, 0.75);
}

.chat-notice {
  margin: 0 0 2px;
  font-size: 10px;
  line-height: 16px;
  color: #afafaf;
  text-align: center;
}

.chat-bubble--typing {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 10px 12px;
  color: #696565;
  font-size: 11px;
}

.chat-bubble--typing i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #17b7bd;
  animation: typing-dot 1s infinite ease-in-out;
}

.chat-bubble--typing i:nth-child(2) {
  animation-delay: 0.15s;
}

.chat-bubble--typing i:nth-child(3) {
  animation-delay: 0.3s;
}

.chat-bubble--typing span {
  margin-left: 4px;
}

@keyframes typing-dot {
  0%, 60%, 100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.chat-composer {
  flex: none;
  padding: 8px 0 4px;
  background: transparent;
}

.chat-quick {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.chat-quick__chip {
  flex: none;
  padding: 5px 12px;
  border: 1px solid rgba(23, 183, 189, 0.35);
  border-radius: 14px;
  background: #ffffff;
  font-size: 12px;
  color: #17b7bd;
  cursor: pointer;
}

.chat-input {
  display: flex;
  gap: 8px;
}

.chat-input input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  border: 1px solid #f0eeef;
  border-radius: 20px;
  background: #ffffff;
  font-family: inherit;
  font-size: 13px;
  color: #353333;
  outline: none;
}

.chat-input input:focus {
  border-color: #17b7bd;
}

.chat-send {
  flex: none;
  min-width: 64px;
  height: 40px;
  padding: 0 12px;
  border: 0;
  border-radius: 20px;
  background: var(--app-primary, #17b7bd);
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  cursor: pointer;
}

.chat-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-cooldown {
  margin: 6px 2px 0;
  font-size: 10px;
  line-height: 16px;
  color: #afafaf;
}
</style>
