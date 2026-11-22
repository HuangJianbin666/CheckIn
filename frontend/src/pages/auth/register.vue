<template>
  <view class="page">
    <view class="card">
      <view class="brand">注册账号</view>
      <view class="subtitle">创建你的第一个打卡项</view>

      <view class="field">
        <input
          v-model="username"
          class="input"
          placeholder="用户名（3-32 位字母/数字/下划线）"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <input
          v-model="password"
          class="input"
          password
          placeholder="密码（8-72 位）"
          placeholder-class="placeholder"
        />
      </view>

      <view class="field">
        <input
          v-model="confirmPassword"
          class="input"
          password
          placeholder="确认密码"
          placeholder-class="placeholder"
        />
      </view>

      <button
        class="primary-btn"
        :loading="loading"
        :disabled="loading"
        @click="handleRegister"
      >
        注册
      </button>

      <view class="switch" @click="goLogin">已有账号？去登录</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { register } from '../../api/auth'

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
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
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return false
  }
  return true
}

async function handleRegister() {
  if (loading.value) return
  if (!validate()) return

  loading.value = true
  try {
    await register(username.value.trim(), password.value)
    uni.showToast({ title: '注册成功，请登录', icon: 'success' })
    // 延迟跳转，确保 toast 可见
    setTimeout(() => uni.reLaunch({ url: '/pages/auth/login' }), 800)
  } catch (e) {
    // 注册失败（如 USERNAME_EXISTS 409）由 request.js 统一 toast 提示
  } finally {
    loading.value = false
  }
}

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login' })
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

.switch {
  margin-top: 32rpx;
  text-align: center;
  font-size: 26rpx;
  color: #2563eb;
}
</style>