<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import PageLayout from '@/components/PageLayout.vue'
import { useAuthStore } from '@/stores/auth'
import * as usersApi from '@/api/users'
import type { UpdateProfilePayload } from '@/api/types'

const router = useRouter()
const auth = useAuthStore()

const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const form = reactive({
  displayName: '',
  username: '',
  gender: '' as '' | 'male' | 'female' | 'other',
  birthday: '',
})

// ========== 派生 ==========
const initial = computed(() => {
  const name = auth.user?.displayName || auth.user?.email || '卡'
  return name.slice(0, 1).toUpperCase()
})

function selectGender(value: 'male' | 'female' | 'other') {
  form.gender = value
}

// ========== 头像 ==========
function pickAvatar() {
  if (uploading.value) return
  fileInput.value?.click()
}

async function onAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!/^image\/(jpeg|jpg|png|webp)$/.test(file.type)) {
    ElMessage.warning('仅支持 JPG/PNG/WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return
  }

  uploading.value = true
  try {
    const res = await usersApi.uploadAvatar(file)
    if (auth.user) {
      auth.setUser({ ...auth.user, avatarUrl: res.avatarUrl })
    }
    ElMessage.success('头像已更新')
  } catch {
    // 请求层已处理
  } finally {
    uploading.value = false
  }
}

function validate(): string | null {
  const displayName = form.displayName.trim()
  const username = form.username.trim()

  if (!displayName) return '请输入昵称'
  if (displayName.length > 64) return '昵称不能超过 64 个字符'
  if (!username) return '请输入用户名'
  if (!/^[a-zA-Z0-9_]{3,32}$/.test(username)) return '用户名需为 3–32 位字母、数字或下划线'
  return null
}

async function onSave() {
  const err = validate()
  if (err) {
    ElMessage.warning(err)
    return
  }

  saving.value = true
  const payload: UpdateProfilePayload = {
    displayName: form.displayName.trim(),
    username: form.username.trim(),
    gender: form.gender || undefined,
    birthday: form.birthday || undefined,
  }
  try {
    // 同时更新后端和 store
    await auth.updateProfile(payload)
    ElMessage.success('资料已保存')
    router.back()
  } catch {
    // request 层会展示错误；回退方案
    try {
      await usersApi.updateMe(payload)
      ElMessage.success('资料已保存')
      router.back()
    } catch {
      // 错误已由 request 拦截器展示
    }
  } finally {
    saving.value = false
  }
}

// ========== 初始化 ==========
onMounted(async () => {
  // 每次进入都拉取最新资料，确保头像等字段是最新的
  try {
    await auth.fetchMe()
  } catch {
    // 401 跳转登录
  }
  const u = auth.user
  if (u) {
    form.displayName = u.displayName ?? ''
    form.username = u.username ?? ''
    form.gender = (u.gender as typeof form.gender) ?? ''
    form.birthday = u.birthday ?? ''
  }
})
</script>

<template>
  <div class="account-edit-page">
    <PageLayout title="编辑资料" @back="router.back()">
      <form class="edit-form" novalidate @submit.prevent="onSave">
        <!-- Avatar row -->
        <div class="edit-row edit-row--avatar">
          <img
            v-if="auth.user?.avatarUrl"
            class="edit-row__avatar edit-row__avatar--img"
            :src="auth.user.avatarUrl"
            alt="头像"
          />
          <span v-else class="edit-row__avatar">{{ initial }}</span>
          <button type="button" class="edit-row__photo" :disabled="uploading" @click="pickAvatar">
            {{ uploading ? '上传中…' : '更换头像' }}
          </button>
          <input
            ref="fileInput"
            class="edit-row__file"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            @change="onAvatarChange"
          />
        </div>

        <!-- Display Name -->
        <label class="edit-field">
          <span class="edit-field__label">昵称</span>
          <input
            v-model="form.displayName"
            type="text"
            class="edit-field__input"
            maxlength="64"
            autocomplete="nickname"
            placeholder="请输入昵称"
          />
        </label>

        <!-- Username -->
        <label class="edit-field">
          <span class="edit-field__label">用户名</span>
          <input
            v-model="form.username"
            type="text"
            class="edit-field__input"
            maxlength="32"
            autocomplete="username"
            placeholder="请设置用户名"
          />
        </label>

        <!-- Gender (segmented) -->
        <div class="edit-field">
          <span class="edit-field__label">性别</span>
          <div class="segmented">
            <button
              v-for="g in (['male', 'female', 'other'] as const)"
              :key="g"
              type="button"
              class="segmented__btn"
              :class="{ active: form.gender === g }"
              @click="selectGender(g)"
            >
              {{ g === 'male' ? '男' : g === 'female' ? '女' : '其他' }}
            </button>
          </div>
        </div>

        <!-- Birthday -->
        <label class="edit-field">
          <span class="edit-field__label">出生日期</span>
          <input
            v-model="form.birthday"
            type="date"
            class="edit-field__input"
          />
        </label>

        <!-- Email (disabled) -->
        <div class="edit-field">
          <span class="edit-field__label">邮箱</span>
          <input
            :value="auth.user?.email || ''"
            type="email"
            class="edit-field__input edit-field__input--disabled"
            disabled
            readonly
          />
        </div>

        <!-- Spacer -->
        <div class="edit-spacer" />

        <!-- Save button -->
        <button
          type="submit"
          class="edit-save"
          :disabled="saving"
        >
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </form>
    </PageLayout>
  </div>
</template>

<style scoped>
.account-edit-page {
  width: 100%;
  height: 100%;
}

/* ============ 表单 ============ */
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

/* ============ Avatar row ============ */
.edit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 16px;
}

.edit-row--avatar {
  margin-top: 0;
}

.edit-row__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #17b7bd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
}

.edit-row__avatar--img {
  object-fit: cover;
  flex-shrink: 0;
}

.edit-row__file {
  display: none;
}

.edit-row__photo {
  border: 0;
  background: none;
  cursor: pointer;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: #17b7bd;
  padding: 4px 6px;
}

/* ============ 字段 ============ */
.edit-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0;
}

.edit-field__label {
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: #696565;
  letter-spacing: -0.18px;
}

.edit-field__input {
  width: 100%;
  height: 44px;
  padding: 12px 14px;
  border: 1px solid var(--app-line, #f0eeef);
  border-radius: 16px;
  background: #ffffff;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  font-weight: 400;
  color: #353333;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  box-sizing: border-box;
}

.edit-field__input:focus {
  border-color: #17b7bd;
  box-shadow: 0 0 0 3px rgba(23, 183, 189, 0.15);
}

.edit-field__input::placeholder {
  color: #b5b0b3;
}

.edit-field__input--disabled {
  background: #f7f6f8;
  color: #afafaf;
  cursor: not-allowed;
  border-color: #f0eeef;
}

/* ============ Segmented ============ */
.segmented {
  display: flex;
  background: #f4f6f4;
  border-radius: 8px;
  padding: 4px;
  gap: 4px;
}

.segmented__btn {
  flex: 1;
  height: 36px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #696565;
  transition: all 0.15s ease;
}

.segmented__btn.active {
  background: #ffffff;
  color: #17b7bd;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* ============ Save ============ */
.edit-spacer {
  flex: 1;
  min-height: 16px;
}

.edit-save {
  width: 100%;
  height: 52px;
  border: 0;
  border-radius: 14px;
  background: var(--app-primary, #17b7bd);
  cursor: pointer;
  font-family: 'Poppins', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 600;
  line-height: 34px;
  letter-spacing: -0.2px;
  color: #ffffff;
  box-shadow: 0 10px 22px rgba(23, 183, 189, 0.16);
  margin-top: 12px;
}

.edit-save:active {
  transform: scale(0.99);
}

.edit-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
