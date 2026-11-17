<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import {
  listVerifications,
  submitVerification,
  type VerificationResponse,
  type VerificationType,
} from '@/api/verifications'

const router = useRouter()

const TYPES: Array<{ key: VerificationType; label: string; desc: string }> = [
  { key: 'identity', label: '身份认证', desc: '提交证件信息，提升账户安全等级' },
  { key: 'address', label: '地址认证', desc: '验证你的常住地址信息' },
  { key: 'phone', label: '手机认证', desc: '绑定并验证常用手机号' },
  { key: 'email', label: '邮箱认证', desc: '验证常用邮箱，接收账户通知' },
]

const DOCUMENT_TYPES = [
  { value: 'id_card', label: '身份证' },
  { value: 'passport', label: '护照' },
  { value: 'driver_license', label: '驾驶证' },
]

const records = ref<VerificationResponse[]>([])
const loading = ref(true)
const submitting = ref(false)
const selectedType = ref<VerificationType | null>(null)
const documentType = ref('id_card')
const documentNo = ref('')

const recordMap = computed(() => {
  const map: Partial<Record<VerificationType, VerificationResponse>> = {}
  for (const record of records.value) {
    map[record.type] = record
  }
  return map
})

onMounted(loadVerifications)

async function loadVerifications() {
  loading.value = true
  try {
    records.value = await listVerifications()
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    pending: '审核中',
    verified: '已认证',
    rejected: '未通过',
  }
  return status ? (map[status] ?? status) : '未认证'
}

function statusClass(status?: string): string {
  if (status === 'pending') return 'badge--pending'
  if (status === 'verified') return 'badge--verified'
  if (status === 'rejected') return 'badge--rejected'
  return 'badge--idle'
}

function openForm(type: VerificationType) {
  selectedType.value = type
  documentType.value = 'id_card'
  documentNo.value = ''
}

function closeForm() {
  selectedType.value = null
  documentNo.value = ''
}

async function submit() {
  if (!selectedType.value) return
  const isIdentity = selectedType.value === 'identity'
  if (isIdentity && !documentNo.value.trim()) {
    ElMessage.warning('请输入证件号')
    return
  }

  submitting.value = true
  try {
    await submitVerification({
      type: selectedType.value,
      documentType: isIdentity ? documentType.value : undefined,
      documentNo: isIdentity ? documentNo.value.trim() : undefined,
    })
    ElMessage.success('认证已提交，审核结果会在消息通知中告知')
    closeForm()
    await loadVerifications()
  } catch {
    // 请求层已提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <PageLayout title="安全验证" @back="goBack">
    <div class="verify-page">
      <!-- 说明 -->
      <section class="verify-intro">
        <svg viewBox="0 0 24 24" fill="none" stroke="#17b7bd" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 12l2 2 4-4" />
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
        <div>
          <p class="verify-intro__title">保护你的账户安全</p>
          <p class="verify-intro__desc">完成认证后可提升转账额度，并保障账户与资金安全</p>
        </div>
      </section>

      <!-- 提交表单 -->
      <section v-if="selectedType" class="verify-card">
        <h3 class="verify-card__title">
          {{ TYPES.find((t) => t.key === selectedType)?.label }}
        </h3>

        <div v-if="selectedType === 'identity'" class="verify-form">
          <label class="field">
            <span class="field__label">证件类型</span>
            <select v-model="documentType" :disabled="submitting">
              <option v-for="doc in DOCUMENT_TYPES" :key="doc.value" :value="doc.value">
                {{ doc.label }}
              </option>
            </select>
          </label>

          <label class="field">
            <span class="field__label">证件号</span>
            <input
              v-model="documentNo"
              type="text"
              maxlength="64"
              placeholder="请输入证件号码"
              :disabled="submitting"
            />
          </label>
        </div>
        <p v-else class="verify-card__hint">
          提交后我们将通过你账户预留的{{ selectedType === 'phone' ? '手机号' : selectedType === 'email' ? '邮箱' : '地址' }}进行核验。
        </p>

        <div class="verify-form__actions">
          <button type="button" class="btn btn--ghost" :disabled="submitting" @click="closeForm">
            取消
          </button>
          <button type="button" class="btn btn--primary" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中…' : '提交认证' }}
          </button>
        </div>
      </section>

      <!-- 认证项目 -->
      <template v-else>
        <div v-if="loading" class="verify-skeleton">
          <div class="verify-skeleton__item" />
          <div class="verify-skeleton__item" />
        </div>

        <ul v-else class="verify-list">
          <li v-for="item in TYPES" :key="item.key" class="verify-card">
            <div class="verify-card__head">
              <span class="verify-card__name">{{ item.label }}</span>
              <span
                class="badge"
                :class="statusClass(recordMap[item.key]?.status)"
              >
                {{ statusLabel(recordMap[item.key]?.status) }}
              </span>
            </div>
            <p class="verify-card__desc">{{ item.desc }}</p>

            <p
              v-if="recordMap[item.key]?.status === 'rejected' && recordMap[item.key]?.rejectedReason"
              class="verify-card__reason"
            >
              未通过原因：{{ recordMap[item.key]?.rejectedReason }}
            </p>
            <p
              v-else-if="recordMap[item.key]?.documentNoLast4"
              class="verify-card__doc"
            >
              证件尾号 ****{{ recordMap[item.key]?.documentNoLast4 }}
            </p>

            <button
              v-if="recordMap[item.key]?.status !== 'pending' && recordMap[item.key]?.status !== 'verified'"
              type="button"
              class="verify-card__action"
              @click="openForm(item.key)"
            >
              {{ recordMap[item.key]?.status === 'rejected' ? '重新提交' : '去认证' }}
            </button>
          </li>
        </ul>
      </template>
    </div>
  </PageLayout>
</template>

<style scoped>
.verify-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 24px;
}

/* ===== 说明 ===== */
.verify-intro {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  background: #e9f9f5;
}

.verify-intro svg {
  flex: none;
  width: 28px;
  height: 28px;
}

.verify-intro__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #353333;
}

.verify-intro__desc {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: #696565;
}

/* ===== 卡片 ===== */
.verify-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.verify-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.verify-card__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
}

.verify-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.verify-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #353333;
}

.verify-card__desc {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: #afafaf;
}

.verify-card__doc,
.verify-card__reason {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: #696565;
}

.verify-card__reason {
  color: #ef4444;
}

.verify-card__hint {
  margin: 0;
  font-size: 13px;
  line-height: 20px;
  color: #696565;
}

.verify-card__action {
  margin-top: 12px;
  height: 34px;
  padding: 0 18px;
  border: 1px solid #17b7bd;
  border-radius: 17px;
  background: transparent;
  font-size: 13px;
  color: #17b7bd;
  cursor: pointer;
}

.verify-card__action:active {
  background: #eef8f7;
}

/* ===== 徽标 ===== */
.badge {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  line-height: 16px;
}

.badge--idle {
  background: #f4f3f5;
  color: #afafaf;
}

.badge--pending {
  background: #fff6e5;
  color: #e6a23c;
}

.badge--verified {
  background: #eef8f7;
  color: #17b7bd;
}

.badge--rejected {
  background: #fdecec;
  color: #ef4444;
}

/* ===== 表单 ===== */
.verify-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field__label {
  font-size: 12px;
  color: #696565;
}

.field input,
.field select {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #ececec;
  border-radius: 8px;
  background: #fafafa;
  font-family: inherit;
  font-size: 13px;
  color: #353333;
  outline: none;
}

.field input:focus,
.field select:focus {
  border-color: #17b7bd;
  background: #ffffff;
}

.verify-form__actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.btn {
  flex: 1;
  height: 40px;
  border: 0;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn--ghost {
  background: #f4f3f5;
  color: #696565;
}

.btn--primary {
  background: var(--app-primary, #17b7bd);
  color: #ffffff;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== 骨架 ===== */
.verify-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.verify-skeleton__item {
  height: 108px;
  border-radius: 12px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: verify-loading 1.4s ease infinite;
}

@keyframes verify-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
