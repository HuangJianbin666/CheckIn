<template>
  <view class="page">
    <view class="card">
      <view class="brand">新建打卡项</view>
      <view class="subtitle">给想坚持的事起个名字</view>

      <view class="field">
        <input
          v-model="name"
          class="input"
          placeholder="例如：阅读 30 分钟（1-50 字）"
          placeholder-class="placeholder"
        />
      </view>

      <button
        class="primary-btn"
        :loading="loading"
        :disabled="loading"
        @click="handleCreate"
      >
        创建
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { createHabit } from '../../api/habit'

const name = ref('')
const loading = ref(false)

async function handleCreate() {
  if (loading.value) return

  const trimmed = name.value.trim()
  if (trimmed.length < 1 || trimmed.length > 50) {
    uni.showToast({ title: '名称需为 1-50 字', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await createHabit(trimmed)
    // 返回列表页，列表 onShow 会自动重新拉取
    uni.navigateBack()
  } catch (e) {
    // 创建失败由 request.js 统一 toast
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

.input {
  height: 88rpx;
  padding: 0 28rpx;
  background: #f3f4f6;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #1f2937;
}

.placeholder {
  color: #9ca3af;
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