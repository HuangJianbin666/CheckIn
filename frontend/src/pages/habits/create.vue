<template>
  <view class="page">
    <view class="card">
      <view class="brand">{{ isEdit ? '编辑打卡项' : '新建打卡项' }}</view>
      <view class="subtitle">{{ isEdit ? '修改名称或目标时长' : '给想坚持的事起个名字' }}</view>

      <view class="field">
        <uni-easyinput
          v-model="name"
          prefixIcon="compose"
          placeholder="例如：阅读 30 分钟（1-50 字）"
          primaryColor="#2563eb"
          :clearable="false"
        />
      </view>

      <view class="field">
        <view class="duration-label">目标时长（可选）</view>
        <view class="duration-chips">
          <view
            class="duration-chip"
            :class="{ active: durationMode === 'none' }"
            @click="selectNone"
          >不设置</view>
          <view
            v-for="preset in DURATION_PRESETS"
            :key="preset.value"
            class="duration-chip"
            :class="{ active: durationMode === 'preset' && presetValue === preset.value }"
            @click="selectPreset(preset.value)"
          >{{ preset.label }}</view>
          <view
            class="duration-chip"
            :class="{ active: durationMode === 'custom' }"
            @click="selectCustom"
          >自定义</view>
        </view>

        <view v-if="durationMode === 'custom'" class="duration-custom">
          <uni-easyinput
            v-model="customMinutes"
            type="number"
            placeholder="请输入分钟数（1-1440）"
            primaryColor="#2563eb"
            :clearable="false"
          />
        </view>
      </view>

      <button
        class="primary-btn"
        :loading="loading"
        :disabled="loading"
        @click="handleSubmit"
      >
        {{ isEdit ? '保存' : '创建' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createHabit, updateHabit, getHabitDetail } from '../../api/habit'

// 预设档位（分钟）
const DURATION_PRESETS = [
  { label: '15 分钟', value: 15 },
  { label: '30 分钟', value: 30 },
  { label: '45 分钟', value: 45 },
  { label: '60 分钟', value: 60 },
  { label: '90 分钟', value: 90 },
  { label: '120 分钟', value: 120 },
]

const name = ref('')
const loading = ref(false)
const habitId = ref(null)
const isEdit = ref(false)

// 时长状态：none（不设置）/ preset（命中档位）/ custom（自定义）
const durationMode = ref('none')
const presetValue = ref(null)
const customMinutes = ref('')

onLoad((query) => {
  if (query && query.habitId) {
    habitId.value = query.habitId
    isEdit.value = true
    uni.setNavigationBarTitle({ title: '编辑打卡项' })
    loadDetail()
  }
})

async function loadDetail() {
  try {
    const detail = await getHabitDetail(habitId.value)
    name.value = detail.name
    applyDuration(detail.durationMinutes)
  } catch (e) {
    // 详情加载失败已由 request.js 提示，返回列表
    uni.navigateBack()
  }
}

// 将后端分钟数映射回前端选择状态
function applyDuration(minutes) {
  if (minutes == null) {
    durationMode.value = 'none'
    presetValue.value = null
    customMinutes.value = ''
    return
  }
  const hit = DURATION_PRESETS.find((p) => p.value === minutes)
  if (hit) {
    durationMode.value = 'preset'
    presetValue.value = hit.value
    customMinutes.value = ''
  } else {
    durationMode.value = 'custom'
    presetValue.value = null
    customMinutes.value = String(minutes)
  }
}

function selectNone() {
  durationMode.value = 'none'
  presetValue.value = null
  customMinutes.value = ''
}

function selectPreset(value) {
  durationMode.value = 'preset'
  presetValue.value = value
  customMinutes.value = ''
}

function selectCustom() {
  durationMode.value = 'custom'
  presetValue.value = null
}

// 计算最终提交的分钟数；校验失败返回 null 并 toast
function resolveDuration() {
  if (durationMode.value === 'none') return null
  if (durationMode.value === 'preset') return presetValue.value

  const num = Number(customMinutes.value)
  if (!Number.isInteger(num) || num < 1 || num > 1440) {
    uni.showToast({ title: '自定义时长需为 1-1440 的整数分钟', icon: 'none' })
    return undefined
  }
  return num
}

async function handleSubmit() {
  if (loading.value) return

  const trimmed = name.value.trim()
  if (trimmed.length < 1 || trimmed.length > 50) {
    uni.showToast({ title: '名称需为 1-50 字', icon: 'none' })
    return
  }

  const durationMinutes = resolveDuration()
  if (durationMinutes === undefined) return // 校验失败，已 toast

  loading.value = true
  try {
    if (isEdit.value) {
      await updateHabit(habitId.value, { name: trimmed, durationMinutes })
    } else {
      await createHabit({ name: trimmed, durationMinutes })
    }
    // 返回列表页，列表 onShow 会自动重新拉取
    uni.navigateBack()
  } catch (e) {
    // 失败由 request.js 统一 toast
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 40rpx;
  box-sizing: border-box;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 60rpx 48rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.06);
}

.brand {
  font-size: 44rpx;
  font-weight: 600;
  color: #1f2937;
  text-align: center;
}

.subtitle {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #9ca3af;
  text-align: center;
}

.field {
  margin-top: 40rpx;
}

.field :deep(.uni-easyinput__content) {
  height: 88rpx;
  border-radius: 16rpx;
  border: 1px solid #e5e7eb;
  background: #f3f4f6;
  padding: 0 24rpx;
}

.field :deep(.uni-easyinput__content-input) {
  font-size: 30rpx;
  color: #1f2937;
}

.field :deep(.uni-easyinput__placeholder-class) {
  color: #9ca3af;
  font-size: 28rpx;
}

.duration-label {
  font-size: 28rpx;
  color: #6b7280;
  margin-bottom: 20rpx;
}

.duration-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.duration-chip {
  padding: 0 28rpx;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  color: #374151;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
}

.duration-chip.active {
  color: #ffffff;
  background: #2563eb;
  border-color: #2563eb;
}

.duration-custom {
  margin-top: 20rpx;
}

.primary-btn {
  margin-top: 48rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: #2563eb;
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 32rpx;
}

.primary-btn[disabled] {
  background: #93c5fd;
  color: #ffffff;
}
</style>