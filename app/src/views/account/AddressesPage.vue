<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

import PageLayout from '@/components/PageLayout.vue'
import {
  createAddress,
  listAddresses,
  removeAddress,
  updateAddress,
  type AddressPayload,
  type AddressResponse,
} from '@/api/addresses'

const router = useRouter()

const addresses = ref<AddressResponse[]>([])
const loading = ref(true)
const submitting = ref(false)
const showForm = ref(false)
const editingId = ref<string | null>(null)
const recipientRef = ref<HTMLInputElement | null>(null)

const COUNTRIES = [
  { code: 'CN', name: '中国' },
  { code: 'HK', name: '中国香港' },
  { code: 'US', name: '美国' },
  { code: 'JP', name: '日本' },
  { code: 'GB', name: '英国' },
]

const form = ref<AddressPayload>(emptyForm())

function emptyForm(): AddressPayload {
  return {
    label: '',
    recipientName: '',
    phone: '',
    countryCode: 'CN',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    postalCode: '',
    isDefault: false,
  }
}

onMounted(loadAddresses)

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await listAddresses()
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  showForm.value = true
  // 打开表单后自动聚焦，移动端直接弹出键盘
  nextTick(() => recipientRef.value?.focus())
}

function openEdit(address: AddressResponse) {
  editingId.value = address.id
  form.value = {
    label: address.label ?? '',
    recipientName: address.recipientName,
    phone: address.phone,
    countryCode: address.countryCode || 'CN',
    province: address.province ?? '',
    city: address.city ?? '',
    district: address.district ?? '',
    detailAddress: address.detailAddress,
    postalCode: address.postalCode ?? '',
    isDefault: address.isDefault,
  }
  showForm.value = true
  nextTick(() => recipientRef.value?.focus())
}

function closeForm() {
  showForm.value = false
  editingId.value = null
  form.value = emptyForm()
}

function fullAddress(address: AddressResponse): string {
  return [address.province, address.city, address.district, address.detailAddress]
    .filter((part) => !!part)
    .join(' ')
}

function countryName(code: string): string {
  return COUNTRIES.find((c) => c.code === code)?.name ?? code
}

async function submit() {
  const data = form.value
  if (!data.recipientName.trim()) {
    ElMessage.warning('请输入收件人')
    return
  }
  if (!data.phone.trim()) {
    ElMessage.warning('请输入联系电话')
    return
  }
  if (!data.detailAddress.trim()) {
    ElMessage.warning('请输入详细地址')
    return
  }

  const payload: AddressPayload = {
    label: data.label?.trim() || undefined,
    recipientName: data.recipientName.trim(),
    phone: data.phone.trim(),
    countryCode: data.countryCode,
    province: data.province?.trim() || undefined,
    city: data.city?.trim() || undefined,
    district: data.district?.trim() || undefined,
    detailAddress: data.detailAddress.trim(),
    postalCode: data.postalCode?.trim() || undefined,
    isDefault: data.isDefault,
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await updateAddress(editingId.value, payload)
      ElMessage.success('地址已更新')
    } else {
      await createAddress(payload)
      ElMessage.success('地址已添加')
    }
    closeForm()
    await loadAddresses()
  } catch {
    // 请求层已提示
  } finally {
    submitting.value = false
  }
}

async function setDefault(address: AddressResponse) {
  if (address.isDefault) return
  try {
    await updateAddress(address.id, { isDefault: true })
    ElMessage.success('已设为默认地址')
    await loadAddresses()
  } catch {
    // 请求层已提示
  }
}

async function confirmRemove(address: AddressResponse) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${address.recipientName}」的地址吗？`,
      '删除地址',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  try {
    await removeAddress(address.id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch {
    // 请求层已提示
  }
}
</script>

<template>
  <PageLayout title="地址管理" @back="goBack">
    <div class="addr-page">
      <!-- 新增 / 编辑表单 -->
      <section v-if="showForm" class="addr-card">
        <h3 class="addr-card__title">{{ editingId ? '编辑地址' : '新增地址' }}</h3>

        <div class="addr-form">
          <label class="field">
            <span class="field__label">地址标签</span>
            <input
              v-model="form.label"
              type="text"
              maxlength="32"
              placeholder="家 / 公司（可选）"
              :disabled="submitting"
            />
          </label>

          <label class="field">
            <span class="field__label">收件人</span>
            <input
              ref="recipientRef"
              v-model="form.recipientName"
              type="text"
              maxlength="64"
              placeholder="请输入收件人姓名"
              autocomplete="name"
              enterkeyhint="next"
              :disabled="submitting"
            />
          </label>

          <label class="field">
            <span class="field__label">联系电话</span>
            <input
              v-model="form.phone"
              type="tel"
              maxlength="32"
              placeholder="请输入联系电话"
              :disabled="submitting"
            />
          </label>

          <label class="field">
            <span class="field__label">国家 / 地区</span>
            <select v-model="form.countryCode" :disabled="submitting">
              <option v-for="c in COUNTRIES" :key="c.code" :value="c.code">
                {{ c.name }}
              </option>
            </select>
          </label>

          <div class="field-row">
            <label class="field">
              <span class="field__label">省份</span>
              <input v-model="form.province" type="text" maxlength="64" :disabled="submitting" />
            </label>
            <label class="field">
              <span class="field__label">城市</span>
              <input v-model="form.city" type="text" maxlength="64" :disabled="submitting" />
            </label>
          </div>

          <div class="field-row">
            <label class="field">
              <span class="field__label">区 / 县</span>
              <input v-model="form.district" type="text" maxlength="64" :disabled="submitting" />
            </label>
            <label class="field">
              <span class="field__label">邮编</span>
              <input
                v-model="form.postalCode"
                type="text"
                maxlength="16"
                inputmode="numeric"
                :disabled="submitting"
              />
            </label>
          </div>

          <label class="field">
            <span class="field__label">详细地址</span>
            <input
              v-model="form.detailAddress"
              type="text"
              maxlength="255"
              placeholder="街道、门牌号等"
              :disabled="submitting"
            />
          </label>

          <label class="field field--check">
            <input v-model="form.isDefault" type="checkbox" :disabled="submitting" />
            <span class="field__label">设为默认地址</span>
          </label>
        </div>

        <div class="addr-form__actions">
          <button type="button" class="btn btn--ghost" :disabled="submitting" @click="closeForm">
            取消
          </button>
          <button type="button" class="btn btn--primary" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中…' : '保存' }}
          </button>
        </div>
      </section>

      <!-- 地址列表 -->
      <template v-else>
        <div v-if="loading" class="addr-skeleton">
          <div class="addr-skeleton__item" />
          <div class="addr-skeleton__item" />
        </div>

        <section v-else-if="!addresses.length" class="addr-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="#17b7bd" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          <p class="addr-empty__title">还没有收货地址</p>
          <p class="addr-empty__desc">添加地址后，转账收款和寄送卡片会更方便</p>
        </section>

        <ul v-else class="addr-list">
          <li v-for="item in addresses" :key="item.id" class="addr-card">
            <div class="addr-card__head">
              <span v-if="item.label" class="addr-card__label">{{ item.label }}</span>
              <span v-if="item.isDefault" class="addr-card__default">默认</span>
            </div>
            <p class="addr-card__person">
              {{ item.recipientName }}
              <span class="addr-card__phone">{{ item.phone }}</span>
            </p>
            <p class="addr-card__detail">
              {{ countryName(item.countryCode) }} {{ fullAddress(item) }}
            </p>

            <div class="addr-card__actions">
              <button
                v-if="!item.isDefault"
                type="button"
                class="link-btn"
                @click="setDefault(item)"
              >
                设为默认
              </button>
              <button type="button" class="link-btn" @click="openEdit(item)">编辑</button>
              <button type="button" class="link-btn link-btn--danger" @click="confirmRemove(item)">
                删除
              </button>
            </div>
          </li>
        </ul>

        <button type="button" class="add-btn" @click="openCreate">+ 新增地址</button>
      </template>
    </div>
  </PageLayout>
</template>

<style scoped>
.addr-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 24px;
}

/* ===== 卡片 ===== */
.addr-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #f0eeef;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(26, 41, 41, 0.03);
}

.addr-card__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #353333;
}

/* ===== 列表 ===== */
.addr-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.addr-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 18px;
}

.addr-card__label {
  padding: 1px 8px;
  border-radius: 9px;
  background: #eef8f7;
  font-size: 11px;
  line-height: 16px;
  color: #17b7bd;
}

.addr-card__default {
  padding: 1px 8px;
  border-radius: 9px;
  background: #17b7bd;
  font-size: 11px;
  line-height: 16px;
  color: #ffffff;
}

.addr-card__person {
  margin: 8px 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #353333;
}

.addr-card__phone {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: #afafaf;
}

.addr-card__detail {
  margin: 0;
  font-size: 12px;
  line-height: 18px;
  color: #696565;
}

.addr-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
}

.link-btn {
  padding: 0;
  border: 0;
  background: none;
  font-size: 12px;
  color: #17b7bd;
  cursor: pointer;
}

.link-btn--danger {
  color: #ef4444;
}

/* ===== 空态 ===== */
.addr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  border-radius: 16px;
  background: #ffffff;
  text-align: center;
}

.addr-empty svg {
  width: 42px;
  height: 42px;
}

.addr-empty__title {
  margin: 12px 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #353333;
}

.addr-empty__desc {
  margin: 0;
  font-size: 12px;
  line-height: 18px;
  color: #afafaf;
}

/* ===== 骨架 ===== */
.addr-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.addr-skeleton__item {
  height: 96px;
  border-radius: 12px;
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 37%, #eee 63%);
  background-size: 400% 100%;
  animation: addr-loading 1.4s ease infinite;
}

@keyframes addr-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ===== 新增按钮 ===== */
.add-btn {
  height: 48px;
  border: 0;
  border-radius: 14px;
  background: var(--app-primary, #17b7bd);
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.16);
}

.add-btn:active {
  opacity: 0.9;
}

/* ===== 表单 ===== */
.addr-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
  flex: 1;
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

.field-row {
  display: flex;
  gap: 10px;
}

.field--check {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.field--check input {
  width: 16px;
  height: 16px;
  accent-color: #17b7bd;
}

.addr-form__actions {
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
</style>
