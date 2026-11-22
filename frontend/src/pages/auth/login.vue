<template>
  <view class="page">
    <view class="card">
      <view class="brand">习惯打卡</view>
      <view class="subtitle">坚持，从今天开始</view>

      <view class="field">
        <uni-easyinput
          v-model="username"
          prefixIcon="person"
          placeholder="用户名（3-32 位字母/数字/下划线）"
          primaryColor="#2563eb"
          :clearable="false"
        />
      </view>

      <view class="field">
        <uni-easyinput
          v-model="password"
          type="password"
          prefixIcon="locked"
          placeholder="密码（8-72 位）"
          primaryColor="#2563eb"
          :clearable="false"
        />
      </view>

      <button
        class="primary-btn"
        :loading="loading"
        :disabled="loading"
        @click="handleLogin"
      >
        登录
      </button>

      <view class="switch" @click="goRegister">没有账号？去注册</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { login } from '../../api/auth'
import { setToken } from '../../api/token'

const username = ref('')
const password = ref('')
const loading = ref(false)

function validate() {
  if (!/^[A-Za-z0-9_]{3,32}$/.test(username.value.trim())) {
    uni.showToast({ title: '用户名需为 3-32 位字母、数字或下划线', icon: 'none' })
    return false
  }
  if (password.value.length < 8 || password.value.length > 72) {
    uni.showToast({ title: '密码长度需为 8-72 位', icon: 'none' })
    return false
  }
  return true
}

async function handleLogin() {
  if (loading.value) return
  if (!validate()) return

  loading.value = true
  try {
    const res = await login(username.value.trim(), password.value)
    setToken(res.token)
    uni.reLaunch({ url: '/pages/habits/index' })
  } catch (e) {
    // 登录失败（如 INVALID_CREDENTIALS）由 request.js 统一 toast 提示
  } finally {
    loading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/auth/register' })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
}

.card {
  width: 100%;
  max-width: 640rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 60rpx 48rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.06);
}

.brand {
  font-size: 48rpx;
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

.switch {
  margin-top: 32rpx;
  text-align: center;
  font-size: 26rpx;
  color: #2563eb;
}
</style>