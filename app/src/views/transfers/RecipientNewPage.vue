<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import { createRecipient } from '@/api/recipients'

const router = useRouter()
const displayName = ref('')
const accountNo = ref('')
const bankName = ref('')
const submitting = ref(false)

const canSubmit = computed(() =>
  Boolean(displayName.value.trim() && accountNo.value.trim()) && !submitting.value,
)

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const recipient = await createRecipient({
      displayName: displayName.value.trim(),
      accountNo: accountNo.value.trim(),
      bankName: bankName.value.trim() || undefined,
    })
    ElMessage.success('收款人已添加')
    await router.replace({
      path: '/transfers/new-amount',
      query: {
        recipient: recipient.id,
        recipientName: recipient.displayName,
      },
    })
  } catch {
    // 请求层已处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <PageLayout title="添加收款人" @back="router.back()">
    <form class="recipient-form" @submit.prevent="submit">
      <section class="form-card">
        <label class="field">
          <span class="field__label">收款人姓名</span>
          <input
            v-model="displayName"
            type="text"
            maxlength="64"
            placeholder="请输入收款人姓名"
            autocomplete="name"
          />
        </label>
        <label class="field">
          <span class="field__label">收款账号</span>
          <input
            v-model="accountNo"
            type="text"
            maxlength="64"
            placeholder="请输入收款账号"
            inputmode="numeric"
            enterkeyhint="next"
            autocomplete="off"
          />
        </label>
        <label class="field">
          <span class="field__label">银行名称（选填）</span>
          <input
            v-model="bankName"
            type="text"
            maxlength="64"
            placeholder="请输入银行名称"
          />
        </label>
      </section>

      <p class="form-tip">保存后将继续填写转账金额。</p>
      <button class="primary-btn" type="submit" :disabled="!canSubmit">
        {{ submitting ? '保存中…' : '保存并继续' }}
      </button>
    </form>
  </PageLayout>
</template>

<style scoped>
.recipient-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-card {
  padding: 4px 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 0;
  border-bottom: 1px solid #f6f6f6;
}

.field:last-child {
  border-bottom: 0;
}

.field__label {
  font-size: 14px;
  font-weight: 500;
  color: #696565;
}

.field input {
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid #e8e6e7;
  border-radius: 10px;
  outline: none;
  background: #fafafa;
  box-sizing: border-box;
  font: inherit;
  font-size: 15px;
  color: #353333;
}

.field input:focus {
  border-color: #17b7bd;
  background: #fff;
}

.form-tip {
  margin: 0;
  text-align: center;
  font-size: 13px;
  color: #696565;
}

.primary-btn {
  min-height: 52px;
  border: 0;
  border-radius: 14px;
  background: var(--app-primary, #17b7bd);
  color: #fff;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.16);
}

.primary-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
